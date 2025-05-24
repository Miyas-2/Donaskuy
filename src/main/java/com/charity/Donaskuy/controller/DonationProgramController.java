package com.charity.Donaskuy.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.charity.Donaskuy.Model.DonationProgram;
import com.charity.Donaskuy.repository.DonationProgramRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/program")
@RequiredArgsConstructor
public class DonationProgramController {
    private final DonationProgramRepository ProgramRepository;

    @GetMapping("/donation")
    public List<DonationProgram> listPrograms() {
        return ProgramRepository.findAll();
    }
    
}
