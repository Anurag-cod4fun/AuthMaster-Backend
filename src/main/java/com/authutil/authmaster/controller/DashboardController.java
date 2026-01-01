package com.authutil.authmaster.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @GetMapping("/dashboard")
    public Object dashboard(@AuthenticationPrincipal UserDetails user) {
        // example payload
        return Map.of(
            "message", "Welcome to your dashboard",
            "user", user.getUsername(),
            "roles", user.getAuthorities()
        );
    }

    // example role-protected route
    @GetMapping("/admin/overview")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public String adminOverview() {
        return "admin only data";
    }
}