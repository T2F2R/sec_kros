package com.example.sec_kros.Controllers;

import com.example.sec_kros.DTO.AuthResponse;
import com.example.sec_kros.DTO.LoginRequest;
import com.example.sec_kros.DTO.RegisterRequest;
import com.example.sec_kros.Services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        String userType = authService.determineUserType(email);

        if (userType != null) {
            session.setAttribute("userType", userType);
            session.setAttribute("userEmail", email);
            return "redirect:" + authService.getRedirectUrl(userType);
        }

        return "redirect:/login?error=true";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, HttpServletRequest request) {
        // Добавляем пустой объект для формы, если его нет
        if (!model.containsAttribute("loginRequest")) {
            // Не нужно добавлять loginRequest, так как Spring Security обрабатывает форму
        }

        // Проверяем параметры ошибки
        if (request.getParameter("error") != null) {
            model.addAttribute("error", "Неверный email или пароль");
        }
        if (request.getParameter("logout") != null) {
            model.addAttribute("success", "Вы успешно вышли из системы");
        }

        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }

        AuthResponse authResponse = authService.register(registerRequest);

        if (authResponse.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", authResponse.getMessage());
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", authResponse.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }
    }
}