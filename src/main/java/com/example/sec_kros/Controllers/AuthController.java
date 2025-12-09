package com.example.sec_kros.Controllers;

import com.example.sec_kros.DTO.AuthResponse;
import com.example.sec_kros.DTO.LoginRequest;
import com.example.sec_kros.DTO.RegisterRequest;
import com.example.sec_kros.Services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, HttpSession session) {
        logger.info("Dashboard access attempted");

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthenticated access to dashboard");
            return "redirect:/login";
        }

        String email = authentication.getName();
        logger.debug("User authenticated: {}", email);

        String userType = authService.determineUserType(email);
        logger.debug("User type determined: {} for email: {}", userType, email);

        if (userType != null) {
            session.setAttribute("userType", userType);
            session.setAttribute("userEmail", email);

            String redirectUrl = authService.getRedirectUrl(userType);
            logger.info("User {} redirected to: {} based on user type: {}", email, redirectUrl, userType);

            return "redirect:" + redirectUrl;
        }

        logger.error("Failed to determine user type for email: {}", email);
        return "redirect:/login?error=true";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        logger.info("Login page accessed from IP: {}", remoteAddr);

        if (!model.containsAttribute("loginRequest")) {
            logger.debug("Adding new LoginRequest to model");
        }

        if (request.getParameter("error") != null) {
            logger.warn("Login page loaded with error parameter (invalid credentials attempt)");
            model.addAttribute("error", "Неверный email или пароль");
        }
        if (request.getParameter("logout") != null) {
            logger.info("Login page loaded after successful logout");
            model.addAttribute("success", "Вы успешно вышли из системы");
        }

        if (request.getParameter("sessionExpired") != null) {
            logger.info("Login page loaded due to expired session");
            model.addAttribute("error", "Ваша сессия истекла. Пожалуйста, войдите снова.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model, HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        logger.info("Register page accessed from IP: {}", remoteAddr);

        if (!model.containsAttribute("registerRequest")) {
            logger.debug("Adding new RegisterRequest to model");
            model.addAttribute("registerRequest", new RegisterRequest());
        }

        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {

        String remoteAddr = request.getRemoteAddr();
        String clientInfo = getClientInfo(request);

        logger.info("Registration attempt from IP: {} | Email: {} | Client: {}",
                remoteAddr, registerRequest.getEmail(), clientInfo);

        if (bindingResult.hasErrors()) {
            int errorCount = bindingResult.getErrorCount();
            logger.warn("Registration validation failed for email: {} - {} errors",
                    registerRequest.getEmail(), errorCount);

            bindingResult.getAllErrors().forEach(error -> {
                logger.debug("Validation error for {}: {}", registerRequest.getEmail(), error.getDefaultMessage());
            });

            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }

        try {
            logger.debug("Processing registration for: {} {}",
                    registerRequest.getLastName(), registerRequest.getFirstName());

            AuthResponse authResponse = authService.register(registerRequest);

            if (authResponse.isSuccess()) {
                logger.info("Registration successful for: {} (Email: {})",
                        registerRequest.getEmail(), authResponse.getMessage());

                redirectAttributes.addFlashAttribute("success", authResponse.getMessage());
                return "redirect:/login";
            } else {
                logger.warn("Registration failed for: {} - Reason: {}",
                        registerRequest.getEmail(), authResponse.getMessage());

                redirectAttributes.addFlashAttribute("error", authResponse.getMessage());
                redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
                return "redirect:/register";
            }

        } catch (Exception e) {
            logger.error("Unexpected error during registration for email: {}",
                    registerRequest.getEmail(), e);

            redirectAttributes.addFlashAttribute("error",
                    "Произошла непредвиденная ошибка при регистрации. Пожалуйста, попробуйте позже.");
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }
    }

    @GetMapping("/logout-success")
    public String showLogoutSuccessPage(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        logger.info("Logout success page accessed from IP: {}", remoteAddr);

        return "logout-success";
    }

    // Вспомогательный метод для получения информации о клиенте
    private String getClientInfo(HttpServletRequest request) {
        StringBuilder clientInfo = new StringBuilder();

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            clientInfo.append("User-Agent: ").append(userAgent);
        }

        String referer = request.getHeader("Referer");
        if (referer != null) {
            if (clientInfo.length() > 0) clientInfo.append(" | ");
            clientInfo.append("Referer: ").append(referer);
        }

        return clientInfo.toString();
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        HttpServletRequest request) {

        String remoteAddr = request.getRemoteAddr();
        logger.info("Login attempt from IP: {} | Email: {}", remoteAddr, loginRequest.getEmail());

        if (bindingResult.hasErrors()) {
            logger.warn("Login validation failed for email: {}", loginRequest.getEmail());

            bindingResult.getAllErrors().forEach(error -> {
                logger.debug("Login validation error: {}", error.getDefaultMessage());
            });

            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.loginRequest", bindingResult);
            redirectAttributes.addFlashAttribute("loginRequest", loginRequest);
            return "redirect:/login";
        }

        // Note: Actual login authentication is handled by Spring Security
        // This endpoint is mainly for form validation

        return "redirect:/login";
    }

    @GetMapping("/access-denied")
    public String showAccessDeniedPage(HttpServletRequest request, Authentication authentication) {
        String remoteAddr = request.getRemoteAddr();
        String username = authentication != null ? authentication.getName() : "anonymous";

        logger.warn("Access denied for user: {} from IP: {} to URL: {}",
                username, remoteAddr, request.getRequestURI());

        if (request.getHeader("Referer") != null) {
            logger.debug("Referer: {}", request.getHeader("Referer"));
        }

        return "error/access-denied";
    }

    @GetMapping("/session-expired")
    public String showSessionExpiredPage(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        logger.info("Session expired page accessed from IP: {}", remoteAddr);

        return "redirect:/login?sessionExpired=true";
    }

    @GetMapping("/auth-error")
    public String showAuthErrorPage(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        logger.error("Authentication error page accessed from IP: {}", remoteAddr);

        String errorMessage = request.getParameter("message");
        if (errorMessage != null) {
            logger.error("Authentication error: {}", errorMessage);
        }

        return "error/auth-error";
    }
}