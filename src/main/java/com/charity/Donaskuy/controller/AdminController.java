package com.charity.Donaskuy.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // Import Optional

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

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long categoryId,
                            HttpSession session,
                            Model model) {
        // Dengan Spring Security, kita tidak perlu memeriksa admin di session, tapi tetap ada untuk kompatibilitas
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            // Sebaiknya redirect ke halaman login atau unauthenticated error jika admin tidak ada
            // Untuk aplikasi yang lebih robust, gunakan Spring Security untuk otentikasi & otorisasi
            return "redirect:/login"; 
        }

        List<DonationProgram> programs;
        if (categoryId != null) {
            programs = programRepo.findByCategory_Id(categoryId);
        } else {
            programs = programRepo.findAll();
        }

        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("programs", programs);
        model.addAttribute("documents", documentRepo.findAll());
        model.addAttribute("selectedCategoryId", categoryId);

        // --- Data untuk Summary Cards dan Progress Bar ---
        // 1. Total Programs
        long totalPrograms = programRepo.count();
        model.addAttribute("totalPrograms", totalPrograms);

        // 2. Total Users
        long totalUsers = userRepo.count(); // Menggunakan metode count() dari JpaRepository
        model.addAttribute("totalUsers", totalUsers);

        // 3. Total Categories
        long totalCategories = categoryRepo.count(); // Menggunakan metode count() dari JpaRepository
        model.addAttribute("totalCategories", totalCategories);

        // 4. Total Collected Amount dan Overall Target Amount
        double totalCollectedAmount = 0.0;
        double overallTargetAmount = 0.0;

        // Mengasumsikan DonationProgram memiliki getCollectedAmount() dan getTargetAmount()
        // PENTING: Pastikan DonationProgram.java memiliki field ini
        for (DonationProgram program : programRepo.findAll()) {
            if (program.getCollectedAmount() != null) {
                totalCollectedAmount += program.getCollectedAmount();
            }
            if (program.getTargetAmount() != null) {
                overallTargetAmount += program.getTargetAmount();
            }
        }
        model.addAttribute("totalCollectedAmount", totalCollectedAmount);
        // Hindari pembagian nol jika tidak ada target
        model.addAttribute("overallTargetAmount", overallTargetAmount == 0 ? 1 : overallTargetAmount); // Set to 1 to avoid division by zero in Thymeleaf

        return "admin_dashboard"; // Pastikan ini sesuai dengan nama file Thymeleaf Anda (misal: admin_dashboard.html)
    }

    @GetMapping("/category")
    public String category(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/login";
        }
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("programs", programRepo.findAll());
        model.addAttribute("documents", documentRepo.findAll());
        return "admin_category";
    }

    @GetMapping("/addProgram")
    public String addProgram(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/login";
        }
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("programs", programRepo.findAll());
        model.addAttribute("documents", documentRepo.findAll());
        return "admin_dashboard_program_add";
    }

    @GetMapping("/verifDok")
    public String verifDok(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/login";
        }
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("programs", programRepo.findAll());
        model.addAttribute("documents", documentRepo.findAll());
        return "admin_verifikasi_dok";
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
        Optional<Category> categoryOptional = categoryRepo.findById(categoryId); // Menggunakan Optional
        User admin = (User) session.getAttribute("admin");

        if (categoryOptional.isPresent() && admin != null) { // Memeriksa keberadaan kategori
            Category category = categoryOptional.get();

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
            // Inisialisasi collectedAmount ke 0 saat membuat program baru
            prog.setCollectedAmount(0.0); 
            programRepo.save(prog);
        } else {
            // Handle jika kategori tidak ditemukan atau admin tidak login
            System.out.println("Error: Category not found or Admin not logged in for adding program.");
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/program/delete")
    public String deleteProgram(@RequestParam Long id) {
        programRepo.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    // --- Edit Category ---
    @PostMapping("/category/edit")
    public String editCategory(@RequestParam Long id,
                               @RequestParam String name,
                               @RequestParam String description) {
        Optional<Category> catOptional = categoryRepo.findById(id); // Menggunakan Optional
        if (catOptional.isPresent()) {
            Category cat = catOptional.get();
            cat.setName(name);
            cat.setDescription(description);
            categoryRepo.save(cat);
        } else {
            System.out.println("Error: Category with ID " + id + " not found for editing.");
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
        Optional<DonationProgram> progOptional = programRepo.findById(id); // Menggunakan Optional
        Optional<Category> categoryOptional = categoryRepo.findById(categoryId); // Menggunakan Optional

        if (progOptional.isPresent() && categoryOptional.isPresent()) {
            DonationProgram prog = progOptional.get();
            Category category = categoryOptional.get();

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
        } else {
            System.out.println("Error: Program with ID " + id + " or Category with ID " + categoryId + " not found for editing.");
        }
        return "redirect:/admin/dashboard";
    }

    // --- Verifikasi Dokumen User ---
    @PostMapping("/document/verify")
    public String verifyDocument(@RequestParam Long docId, @RequestParam("status") UserDocument.DocumentStatus status) {
        Optional<UserDocument> docOptional = documentRepo.findById(docId); // Menggunakan Optional
        if (docOptional.isPresent()) {
            UserDocument doc = docOptional.get();
            doc.setStatus(status);
            documentRepo.save(doc);
            // Jika status VERIFIED, set user jadi verified
            if (status == UserDocument.DocumentStatus.VERIFIED) {
                User user = doc.getUser();
                user.setIsVerified(true);
                userRepo.save(user);
            }
        } else {
            System.out.println("Error: Document with ID " + docId + " not found for verification.");
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/program/{id}")
    public String programDetail(@PathVariable Long id, Model model, HttpSession session) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/login";
        }
        Optional<DonationProgram> progOptional = programRepo.findById(id); // Menggunakan Optional
        if (progOptional.isEmpty()) {
            return "redirect:/admin/dashboard"; // Atau halaman error 404
        }
        DonationProgram prog = progOptional.get();
        model.addAttribute("program", prog);
        model.addAttribute("donations", prog.getDonations());
        return "admin_program_detail";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @PostMapping("/program/status")
    public String updateStatus(
            @RequestParam Long id,
            @RequestParam("status") DonationProgram.ProgramStatus status,
            @RequestParam("donationStatus") DonationProgram.DonationStatus donationStatus) {
        Optional<DonationProgram> progOptional = programRepo.findById(id); // Menggunakan Optional
        if (progOptional.isPresent()) {
            DonationProgram prog = progOptional.get();
            prog.setStatus(status);
            prog.setDonationStatus(donationStatus);
            programRepo.save(prog);
        } else {
            System.out.println("Error: Program with ID " + id + " not found for status update.");
        }
        return "redirect:/admin/dashboard";
    }
}
