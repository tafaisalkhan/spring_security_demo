package com.spring.security.demo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AudienceController {
    @GetMapping("/admin/status")
    public String adminStatus() {
        return "admin ready";
    }

    @GetMapping("/mobile/status")
    public String mobileStatus() {
        return "mobile ready";
    }
}
