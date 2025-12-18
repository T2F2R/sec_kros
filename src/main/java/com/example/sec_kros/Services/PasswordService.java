package com.example.sec_kros.Services;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Пароль не может быть null");
        }
        if (plainPassword.length() > 72) {
            throw new IllegalArgumentException("Пароль не может быть длиннее 72 символов");
        }
        return passwordEncoder.encode(plainPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}