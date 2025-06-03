package com.charity.Donaskuy.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AdminAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        // Only handle admin login
        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            response.sendRedirect("/login"); // Redirect non-admins to regular login
            return;
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        HttpSession session = request.getSession();
        
        if (user != null) {
            // Put admin in session for backward compatibility
            session.setAttribute("admin", user);
            response.sendRedirect("/admin/dashboard");
        } else {
            response.sendRedirect("/admin/login?error");
        }
    }
}