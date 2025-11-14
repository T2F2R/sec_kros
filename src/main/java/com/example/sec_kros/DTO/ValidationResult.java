package com.example.sec_kros.DTO;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private boolean canAutoCreate;
    private List<ValidationCheck> checks;

    public ValidationResult() {
        this.checks = new ArrayList<>();
    }

    // Вложенный класс для проверок
    public static class ValidationCheck {
        private boolean passed;
        private String message;
        private String autoCreateAction;

        public ValidationCheck(boolean passed, String message) {
            this.passed = passed;
            this.message = message;
        }

        public ValidationCheck(boolean passed, String message, String autoCreateAction) {
            this.passed = passed;
            this.message = message;
            this.autoCreateAction = autoCreateAction;
        }

        // Геттеры и сеттеры
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getAutoCreateAction() { return autoCreateAction; }
        public void setAutoCreateAction(String autoCreateAction) { this.autoCreateAction = autoCreateAction; }
    }

    // Геттеры и сеттеры
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public boolean isCanAutoCreate() { return canAutoCreate; }
    public void setCanAutoCreate(boolean canAutoCreate) { this.canAutoCreate = canAutoCreate; }

    public List<ValidationCheck> getChecks() { return checks; }
    public void setChecks(List<ValidationCheck> checks) { this.checks = checks; }

    public void addCheck(ValidationCheck check) {
        this.checks.add(check);
    }

    // Вспомогательные методы
    public boolean hasGuardObject() {
        return checks.stream()
                .filter(check -> check.getMessage().contains("охранный объект"))
                .findFirst()
                .map(ValidationCheck::isPassed)
                .orElse(false);
    }

    public boolean hasNotifications() {
        return checks.stream()
                .filter(check -> check.getMessage().contains("уведомления"))
                .findFirst()
                .map(ValidationCheck::isPassed)
                .orElse(false);
    }
}