package com.charity.Donaskuy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// import ch.qos.logback.core.model.Model;
@Controller
public class ViewController {
    @GetMapping("/")
    public String getLandingPage() {
        return "index";
    }
}
