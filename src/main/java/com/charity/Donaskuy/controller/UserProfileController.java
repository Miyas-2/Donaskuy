// filepath: d:\Semester 4\pbo\Donaskuy\src\main\java\com\charity\Donaskuy\controller\UserProfileController.java
package com.charity.Donaskuy.controller;

import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder; // Import PasswordEncoder
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/dashboard/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Inject PasswordEncoder

    @GetMapping
    public String profilePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "edit_profile";
    }

    @PostMapping
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

        // Fetch the user from DB to ensure we have the latest state, especially the password
        User userToUpdate = userRepository.findById(user.getId()).orElse(null);
        if (userToUpdate == null) {
            return "redirect:/login"; // Should not happen if session user is valid
        }


        userToUpdate.setName(name);
        userToUpdate.setPhone(phone);

        if (photo != null && !photo.isEmpty()) {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String photoName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
            photo.transferTo(new File(uploadDir + photoName));
            userToUpdate.setPhoto(photoName);
        }

        if (password != null && !password.isBlank()) {
            if (!password.equals(confirmPassword)) {
                model.addAttribute("user", userToUpdate);
                model.addAttribute("error", "Password dan konfirmasi tidak cocok!");
                return "edit_profile";
            }
            userToUpdate.setPassword(passwordEncoder.encode(password)); // Encode password
        }

        userRepository.save(userToUpdate);
        session.setAttribute("user", userToUpdate); // Update user in session

        return "redirect:/dashboard/profile?updated";
    }
}