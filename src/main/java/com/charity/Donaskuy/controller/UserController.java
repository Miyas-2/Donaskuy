package com.charity.Donaskuy.controller;

import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    
    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/ping")
    public String ping() {
        return "Service OK";
    }
    
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        System.out.println(">>> REGISTER HIT <<<");
        return userRepository.save(user);
    }

    @PostMapping("/test")
    public String testPost(@RequestBody String test) {
        return "Received: " + test;
    }
}
