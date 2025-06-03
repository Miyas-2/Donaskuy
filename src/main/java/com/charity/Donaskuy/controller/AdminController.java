package com.charity.Donaskuy.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.charity.Donaskuy.Model.Category;
import com.charity.Donaskuy.Model.DonationProgram;
import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.Model.UserDocument;
import com.charity.Donaskuy.repository.CategoryRepository;
import com.charity.Donaskuy.repository.DonationProgramRepository;
import com.charity.Donaskuy.repository.UserDocumentRepository;
import com.charity.Donaskuy.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepo;
    private final UserDocumentRepository documentRepo;
    private final CategoryRepository categoryRepo;
    private final DonationProgramRepository programRepo;

// Only update the dashboard method - the login methods will be handled by Spring Security
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // With Spring Security, we don't need to check for admin in session, but keep it for compatibility
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("programs", programRepo.findAll());
        model.addAttribute("documents", documentRepo.findAll());
        return "admin_dashboard";
    }

    // --- CRUD Category ---
    @PostMapping("/category/add")
    public String addCategory(@RequestParam String name, @RequestParam String description) {
        categoryRepo.save(new Category(null, name, description, null));
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/category/delete")
    public String deleteCategory(@RequestParam Long id) {
        categoryRepo.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    // --- CRUD Program Donasi ---
    @PostMapping("/program/add")
    public String addProgram(@RequestParam String title,
            @RequestParam String description,
            @RequestParam Double targetAmount,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Long categoryId,
            @RequestParam("photo") MultipartFile photo,
            HttpSession session) throws IOException {
        Category category = categoryRepo.findById(categoryId).orElse(null);
        User admin = (User) session.getAttribute("admin");
        if (category != null && admin != null) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String photoName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
            photo.transferTo(new File(uploadDir + photoName));

            DonationProgram prog = new DonationProgram();
            prog.setTitle(title);
            prog.setDescription(description);
            prog.setTargetAmount(targetAmount);
            prog.setStartDate(LocalDate.parse(startDate));
            prog.setEndDate(LocalDate.parse(endDate));
            prog.setCategory(category);
            prog.setUser(admin);
            prog.setStatus(DonationProgram.ProgramStatus.APPROVED);
            prog.setPhoto(photoName);
            programRepo.save(prog);
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/program/delete")
    public String deleteProgram(@RequestParam Long id) {
        programRepo.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    // ...existing code...
// --- Edit Category ---
    @PostMapping("/category/edit")
    public String editCategory(@RequestParam Long id,
            @RequestParam String name,
            @RequestParam String description) {
        Category cat = categoryRepo.findById(id).orElse(null);
        if (cat != null) {
            cat.setName(name);
            cat.setDescription(description);
            categoryRepo.save(cat);
        }
        return "redirect:/admin/dashboard";
    }

// --- Edit Program Donasi ---
    @PostMapping("/program/edit")
    public String editProgram(@RequestParam Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Double targetAmount,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Long categoryId,
            @RequestParam(value = "photo", required = false) MultipartFile photo) throws IOException {
        DonationProgram prog = programRepo.findById(id).orElse(null);
        Category category = categoryRepo.findById(categoryId).orElse(null);
        if (prog != null && category != null) {
            prog.setTitle(title);
            prog.setDescription(description);
            prog.setTargetAmount(targetAmount);
            prog.setStartDate(LocalDate.parse(startDate));
            prog.setEndDate(LocalDate.parse(endDate));
            prog.setCategory(category);

            // Jika ada file foto baru, simpan dan update
            if (photo != null && !photo.isEmpty()) {
                String uploadDir = System.getProperty("user.dir") + "/uploads/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String photoName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
                photo.transferTo(new File(uploadDir + photoName));
                prog.setPhoto(photoName);
            }

            programRepo.save(prog);
        }
        return "redirect:/admin/dashboard";
    }

    // --- Verifikasi Dokumen User ---
    @PostMapping("/document/verify")
    public String verifyDocument(@RequestParam Long docId, @RequestParam("status") UserDocument.DocumentStatus status) {
        UserDocument doc = documentRepo.findById(docId).orElse(null);
        if (doc != null) {
            doc.setStatus(status);
            documentRepo.save(doc);
            // Jika status VERIFIED, set user jadi verified
            if (status == UserDocument.DocumentStatus.VERIFIED) {
                User user = doc.getUser();
                user.setIsVerified(true);
                userRepo.save(user);
            }
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/program/{id}")
    public String programDetail(@PathVariable Long id, Model model, HttpSession session) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        DonationProgram prog = programRepo.findById(id).orElse(null);
        if (prog == null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("program", prog);
        model.addAttribute("donations", prog.getDonations());
        return "admin_program_detail";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    @PostMapping("/program/status")
    public String updateStatus(
            @RequestParam Long id,
            @RequestParam("status") DonationProgram.ProgramStatus status,
            @RequestParam("donationStatus") DonationProgram.DonationStatus donationStatus) {
        DonationProgram prog = programRepo.findById(id).orElse(null);
        if (prog != null) {
            prog.setStatus(status);
            prog.setDonationStatus(donationStatus);
            programRepo.save(prog);
        }
        return "redirect:/admin/dashboard";
    }
}
