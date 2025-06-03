// filepath: d:\Semester 4\pbo\Donaskuy\src\main\java\com\charity\Donaskuy\controller\DashboardHomeController.java
package com.charity.Donaskuy.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.charity.Donaskuy.Model.DonationProgram;
import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.Model.UserDocument;
import com.charity.Donaskuy.repository.CategoryRepository;
import com.charity.Donaskuy.repository.DonationProgramRepository;
import com.charity.Donaskuy.repository.UserDocumentRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardHomeController {

    private final DonationProgramRepository programRepository;
    private final CategoryRepository categoryRepository;
    private final UserDocumentRepository documentRepository;

    @GetMapping
    public String dashboard(@RequestParam(required = false) Long categoryId,
                            HttpSession session,
                            Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<DonationProgram> programs;
        if (categoryId != null) {
            programs = programRepository.findByStatusAndDonationStatusAndCategoryId(
                    DonationProgram.ProgramStatus.APPROVED,
                    DonationProgram.DonationStatus.ACTIVE,
                    categoryId
            );
        } else {
            programs = programRepository.findByStatusAndDonationStatus(
                    DonationProgram.ProgramStatus.APPROVED,
                    DonationProgram.DonationStatus.ACTIVE
            );
        }

        model.addAttribute("user", user);
        model.addAttribute("programs", programs);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("selectedCategoryId", categoryId);

        var docOpt = documentRepository.findTopByUserOrderByIdDesc(user);
        model.addAttribute("docStatus", docOpt.map(UserDocument::getStatus).orElse(null));

        return "dashboard_home";
    }
}