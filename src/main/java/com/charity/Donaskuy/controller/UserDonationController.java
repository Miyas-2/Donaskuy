// filepath: d:\Semester 4\pbo\Donaskuy\src\main\java\com\charity\Donaskuy\controller\UserDonationController.java
package com.charity.Donaskuy.controller;

import com.charity.Donaskuy.Model.Donation;
import com.charity.Donaskuy.Model.DonationProgram;
import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.repository.DonationProgramRepository;
import com.charity.Donaskuy.repository.DonationRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class UserDonationController {

    private final DonationRepository donationRepository;
    private final DonationProgramRepository programRepository;

    @PostMapping("/program/{id}/donate")
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

        if (prog.getDonationStatus() != DonationProgram.DonationStatus.ACTIVE) {
            return "redirect:/dashboard/program/" + id + "?error=inactive";
        }
        Donation donation = new Donation();
        donation.setUser(user);
        donation.setDonationProgram(prog);
        donation.setAmount(amount);
        donation.setMessage(message);
        donation.setPaymentStatus(Donation.PaymentStatus.PENDING);
        donationRepository.save(donation);

        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
                donation.setPaymentStatus(Donation.PaymentStatus.COMPLETED);
                donationRepository.save(donation);

                DonationProgram progToUpdate = programRepository.findById(id).orElse(null);
                if (progToUpdate != null) {
                    Double current = progToUpdate.getCollectedAmount() != null ? progToUpdate.getCollectedAmount() : 0.0;
                    progToUpdate.setCollectedAmount(current + donation.getAmount());
                    programRepository.save(progToUpdate);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt(); // Restore interruption status
            }
        });

        return "redirect:/dashboard/program/" + id + "?donated";
    }

    @GetMapping("/donasi")
    public String donasiSaya(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Donation> myDonations = donationRepository.findByUserId(user.getId());
        model.addAttribute("myDonations", myDonations);

        double totalDonations = myDonations.stream()
                .filter(d -> d.getPaymentStatus() == Donation.PaymentStatus.COMPLETED)
                .mapToDouble(Donation::getAmount)
                .sum();
        model.addAttribute("totalDonations", totalDonations);

        long supportedPrograms = myDonations.stream()
                .filter(d -> d.getPaymentStatus() == Donation.PaymentStatus.COMPLETED)
                .map(d -> d.getDonationProgram().getId())
                .distinct()
                .count();
        model.addAttribute("supportedPrograms", supportedPrograms);

        long impactCount = myDonations.stream()
                .filter(d -> d.getPaymentStatus() == Donation.PaymentStatus.COMPLETED)
                .count() * 2;
        model.addAttribute("impactCount", impactCount);

        return "dashboard_donasi";
    }
}