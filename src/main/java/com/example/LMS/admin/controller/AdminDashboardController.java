package com.example.LMS.admin.controller;

import com.example.LMS.admin.service.AdminDashboardService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "daily") String period,
                            Authentication authentication,
                            Model model) {
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        boolean isAdmin = isAuthenticated && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 접근 가능합니다.");
        }

        AdminDashboardService.DashboardData data = dashboardService.load(period);
        model.addAttribute("isAuthenticated", true);
        model.addAttribute("dashboard", data);
        return "admin/dashboard";
    }
}
