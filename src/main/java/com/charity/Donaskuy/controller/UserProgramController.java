// filepath: d:\Semester 4\pbo\Donaskuy\src\main\java\com\charity\Donaskuy\controller\UserProgramController.java
package com.charity.Donaskuy.controller;

import com.charity.Donaskuy.Model.Category;
import com.charity.Donaskuy.Model.DonationProgram;
import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.Model.UserDocument;
import com.charity.Donaskuy.repository.CategoryRepository;
import com.charity.Donaskuy.repository.DonationProgramRepository;
import com.charity.Donaskuy.repository.UserDocumentRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class UserProgramController {

    private final DonationProgramRepository programRepository;
    private final CategoryRepository categoryRepository;
    private final UserDocumentRepository documentRepository; // Added for docStatus in add/edit form

    @GetMapping("/program/{id}")
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

    @GetMapping("/program/add")
    public String showAddProgram(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", false);
        var docOpt = documentRepository.findTopByUserOrderByIdDesc(user);
        model.addAttribute("docStatus", docOpt.map(UserDocument::getStatus).orElse(null));
        return "dashboard_program_add";
    }

// ...existing code...
    @PostMapping("/program/add")
    public String addUserProgram(@RequestParam String title,
            @RequestParam String description,
            @RequestParam Double targetAmount,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Long categoryId,
            @RequestParam("photo") MultipartFile photo,
            HttpSession session) throws IOException {

        System.out.println("=== DEBUG: addUserProgram method called ===");
        System.out.println("Title: " + title);
        System.out.println("Description: " + description);
        System.out.println("Target Amount: " + targetAmount);
        System.out.println("Category ID: " + categoryId);
        System.out.println("Photo: " + (photo != null ? photo.getOriginalFilename() : "null"));

        try {
            // Validasi user
            User user = (User) session.getAttribute("user");
            if (user == null) {
                System.out.println("ERROR: User tidak ditemukan di session");
                return "redirect:/login";
            }
            System.out.println("User found: " + user.getName() + " (ID: " + user.getId() + ")");

            // Validasi verifikasi user
            if (user.getIsVerified() == null || !user.getIsVerified()) {
                System.out.println("ERROR: User belum diverifikasi: " + user.getIsVerified());
                return "redirect:/dashboard?notverified";
            }
            System.out.println("User verified: " + user.getIsVerified());

            // Validasi category
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category == null) {
                System.out.println("ERROR: Category not found with ID: " + categoryId);
                return "redirect:/dashboard/program/add?error=category";
            }
            System.out.println("Category found: " + category.getName());

            // Handle file upload
            String photoName = null;
            if (photo != null && !photo.isEmpty()) {
                String uploadDir = System.getProperty("user.dir") + "/uploads/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    System.out.println("Upload directory created: " + created);
                }

                photoName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
                photo.transferTo(new File(uploadDir + photoName));
                System.out.println("Photo uploaded: " + photoName);
            } else {
                System.out.println("ERROR: No photo uploaded");
                return "redirect:/dashboard/program/add?error=photo";
            }

            // Buat program baru
            DonationProgram prog = new DonationProgram();
            prog.setTitle(title);
            prog.setDescription(description);
            prog.setTargetAmount(targetAmount);
            prog.setStartDate(LocalDate.parse(startDate));
            prog.setEndDate(LocalDate.parse(endDate));
            prog.setPhoto(photoName);
            prog.setUser(user);
            prog.setCategory(category);
            prog.setStatus(DonationProgram.ProgramStatus.PENDING);

            // Set default values
            prog.setCollectedAmount(0.0);
            prog.setDonationStatus(DonationProgram.DonationStatus.INACTIVE);

            System.out.println("Program object created:");
            System.out.println("- Title: " + prog.getTitle());
            System.out.println("- Target: " + prog.getTargetAmount());
            System.out.println("- User ID: " + prog.getUser().getId());
            System.out.println("- Category ID: " + prog.getCategory().getId());

            // Simpan ke database
            DonationProgram savedProgram = programRepository.save(prog);
            System.out.println("Program SAVED SUCCESSFULLY with ID: " + savedProgram.getId());

            return "redirect:/dashboard/programs/mine?submitted=true";

        } catch (Exception e) {
            System.err.println("ERROR saving program: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/dashboard/program/add?error=save";
        }
    }

// ...existing code...
    @GetMapping("/programs/mine")
    public String myPrograms(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        List<DonationProgram> myPrograms = programRepository.findByUser(user);
        model.addAttribute("myPrograms", myPrograms);
        return "dashboard_my_programs";
    }

    @GetMapping("/program/edit/{id}")
    public String editProgram(@PathVariable Long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        DonationProgram program = programRepository.findById(id).orElse(null);
        if (program == null || !program.getUser().getId().equals(user.getId())) {
            return "redirect:/dashboard/programs/mine";
        }

        if (program.getStatus() != DonationProgram.ProgramStatus.PENDING) {
            return "redirect:/dashboard/programs/mine?error=cantedit";
        }

        model.addAttribute("program", program);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("isEdit", true);
        // Assuming if they can edit, their doc is fine or it's handled by PENDING status logic
        var docOpt = documentRepository.findTopByUserOrderByIdDesc(user);
        model.addAttribute("docStatus", docOpt.map(UserDocument::getStatus).orElse(UserDocument.DocumentStatus.VERIFIED));

        return "dashboard_program_add";
    }

    @PostMapping("/program/edit/{id}")
    public String updateProgram(@PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Double targetAmount,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Long categoryId,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            HttpSession session) throws IOException {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        DonationProgram program = programRepository.findById(id).orElse(null);
        if (program == null || !program.getUser().getId().equals(user.getId())) {
            return "redirect:/dashboard/programs/mine";
        }

        if (program.getStatus() != DonationProgram.ProgramStatus.PENDING) {
            return "redirect:/dashboard/programs/mine?error=cantedit";
        }

        program.setTitle(title);
        program.setDescription(description);
        program.setTargetAmount(targetAmount);
        program.setStartDate(LocalDate.parse(startDate));
        program.setEndDate(LocalDate.parse(endDate));

        Category category = categoryRepository.findById(categoryId).orElse(null);
        program.setCategory(category);

        if (photo != null && !photo.isEmpty()) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String photoName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
            photo.transferTo(new File(uploadDir + photoName));
            program.setPhoto(photoName);
        }

        programRepository.save(program);
        return "redirect:/dashboard/programs/mine?updated";
    }

    @GetMapping("/program/delete/{id}")
    public String deleteProgram(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        DonationProgram program = programRepository.findById(id).orElse(null);
        if (program == null || !program.getUser().getId().equals(user.getId())) {
            return "redirect:/dashboard/programs/mine";
        }

        if (program.getStatus() != DonationProgram.ProgramStatus.PENDING) {
            return "redirect:/dashboard/programs/mine?error=cantdelete";
        }

        if (program.getPhoto() != null) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File photoFile = new File(uploadDir + program.getPhoto());
            if (photoFile.exists()) {
                photoFile.delete();
            }
        }

        programRepository.delete(program);
        return "redirect:/dashboard/programs/mine?deleted";
    }
}
