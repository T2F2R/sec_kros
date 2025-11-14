package com.example.sec_kros.DTO;

public class AuthResponse {
    private boolean success;
    private String message;
    private String userType; // "admin", "client"
    private String redirectUrl;

    // Конструкторы
    public AuthResponse() {}

    public AuthResponse(boolean success, String message, String userType, String redirectUrl) {
        this.success = success;
        this.message = message;
        this.userType = userType;
        this.redirectUrl = redirectUrl;
    }

    // Геттеры и сеттеры
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
}