package com.charity.Donaskuy.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        HttpSession session = request.getSession();
        
        if (user != null) {
            // Check if user is admin
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                session.setAttribute("admin", user);
                response.sendRedirect("/admin/dashboard");
                return;
            } else {
                session.setAttribute("user", user);
                response.sendRedirect("/dashboard");
                return;
            }
        }
        
        super.onAuthenticationSuccess(request, response, authentication);
    }
}