package com.charity.Donaskuy.service;

import org.springframework.stereotype.Service;

import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User RegisterUser(User user) {
        user.setRole(User.Role.USER);
        return userRepository.save(user);
    }
}
