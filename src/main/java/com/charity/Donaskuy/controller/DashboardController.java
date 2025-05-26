package com.charity.Donaskuy.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.charity.Donaskuy.Model.Category;
import com.charity.Donaskuy.Model.Donation;
import com.charity.Donaskuy.Model.DonationProgram;
import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.Model.UserDocument;
import com.charity.Donaskuy.repository.DonationProgramRepository;
import com.charity.Donaskuy.repository.DonationRepository;
import com.charity.Donaskuy.repository.UserDocumentRepository;
import com.charity.Donaskuy.repository.UserRepository;
import com.charity.Donaskuy.repository.CategoryRepository;

import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DonationProgramRepository programRepository;
    private final UserDocumentRepository documentRepository;
    private final DonationRepository donationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        List<DonationProgram> programs = programRepository.findByStatus(DonationProgram.ProgramStatus.APPROVED);
        model.addAttribute("user", user);
        model.addAttribute("programs", programs);

        model.addAttribute("categories", categoryRepository.findAll()); // Ambil status dokumen terbaru user
        var docOpt = documentRepository.findTopByUserOrderByIdDesc(user);
        model.addAttribute("docStatus", docOpt.map(UserDocument::getStatus).orElse(null));

        return "dashboard";
    }

    @GetMapping("/dashboard/program/{id}")
    public String programDetail(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        DonationProgram prog = programRepository.findById(id).orElse(null);
        if (prog == null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("program", prog);
        model.addAttribute("donations", prog.getDonations());
        return "program_detail";
    }

    @PostMapping("/dashboard/program/{id}/donate")
    public String donate(@PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam(required = false) String message,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        DonationProgram prog = programRepository.findById(id).orElse(null);
        if (prog == null) {
            return "redirect:/dashboard";
        }
        Donation donation = new Donation();
        donation.setUser(user);
        donation.setDonationProgram(prog);
        donation.setAmount(amount);
        donation.setMessage(message);
        donation.setPaymentStatus(Donation.PaymentStatus.PENDING);
        donationRepository.save(donation);

        // Mock payment: set status to COMPLETED after 10 seconds
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
                donation.setPaymentStatus(Donation.PaymentStatus.COMPLETED);
                donationRepository.save(donation);

                // Tambahkan ke collectedAmount
                DonationProgram progToUpdate = programRepository.findById(id).orElse(null);
                if (progToUpdate != null) {
                    Double current = progToUpdate.getCollectedAmount() != null ? progToUpdate.getCollectedAmount() : 0.0;
                    progToUpdate.setCollectedAmount(current + donation.getAmount());
                    programRepository.save(progToUpdate);
                }
            } catch (InterruptedException ignored) {
            }
        });

        return "redirect:/dashboard/program/" + id + "?donated";
    }

    @PostMapping("/dashboard/program/add")
    public String addUserProgram(@RequestParam String title,
            @RequestParam String description,
            @RequestParam Double targetAmount,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Long categoryId,
            @RequestParam("photo") MultipartFile photo,
            HttpSession session) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getIsVerified() == null || !user.getIsVerified()) {
            return "redirect:/dashboard?notverified";
        }
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String photoName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
        photo.transferTo(new File(uploadDir + photoName));

        Category category = categoryRepository.findById(categoryId).orElse(null);

        DonationProgram prog = new DonationProgram();
        prog.setTitle(title);
        prog.setDescription(description);
        prog.setTargetAmount(targetAmount);
        prog.setStartDate(LocalDate.parse(startDate));
        prog.setEndDate(LocalDate.parse(endDate));
        prog.setPhoto(photoName);
        prog.setUser(user);
        prog.setCategory(category); // set kategori
        prog.setStatus(DonationProgram.ProgramStatus.PENDING);
        programRepository.save(prog);

        return "redirect:/dashboard?submitted";
    }

    @GetMapping("/dashboard/profile")
    public String profilePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "edit_profile";
    }

    @PostMapping("/dashboard/profile")
    public String updateProfile(@RequestParam String name,
            @RequestParam String phone,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            HttpSession session,
            Model model) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        user.setName(name);
        user.setPhone(phone);

        if (photo != null && !photo.isEmpty()) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String photoName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
            photo.transferTo(new File(uploadDir + photoName));
            user.setPhoto(photoName);
        }

        // Ganti password jika diisi dan konfirmasi cocok
        if (password != null && !password.isBlank()) {
            if (!password.equals(confirmPassword)) {
                model.addAttribute("user", user);
                model.addAttribute("error", "Password dan konfirmasi tidak cocok!");
                return "edit_profile";
            }
            user.setPassword(password);
        }

        userRepository.save(user);
        session.setAttribute("user", user);

        return "redirect:/dashboard";
    }
}
