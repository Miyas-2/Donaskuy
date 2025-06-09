package com.charity.Donaskuy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminLoginController {

    @GetMapping("/login")
    public String showAdminLoginPage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // If already authenticated as admin, redirect to dashboard
        if (auth != null && auth.isAuthenticated() && 
                auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }
        
        return "login";
    }
}