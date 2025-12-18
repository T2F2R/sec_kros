package com.example.sec_kros.services;

import com.example.sec_kros.Services.PasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Test
    void hashPassword_ShouldReturnHashedPassword() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String plainPassword = "mySecurePassword123";

        // Act
        String hashedPassword = passwordService.hashPassword(plainPassword);

        // Assert
        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword).isNotBlank();
        assertThat(hashedPassword).isNotEqualTo(plainPassword);
        // BCrypt hash начинается с $2a$ или $2b$
        assertThat(hashedPassword).startsWith("$2");
        // Длина BCrypt hash обычно 60 символов
        assertThat(hashedPassword).hasSize(60);
    }

    @Test
    void hashPassword_ShouldReturnDifferentHashes_ForSamePassword() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String plainPassword = "samePassword";

        // Act
        String hash1 = passwordService.hashPassword(plainPassword);
        String hash2 = passwordService.hashPassword(plainPassword);

        // Assert
        assertThat(hash1).isNotEqualTo(hash2); // Разные соли дают разные хэши
        assertThat(hash1).isNotNull();
        assertThat(hash2).isNotNull();
    }

    @Test
    void hashPassword_ShouldHandleEmptyPassword() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String emptyPassword = "";

        // Act
        String hashedPassword = passwordService.hashPassword(emptyPassword);

        // Assert
        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword).isNotBlank();
        assertThat(hashedPassword).startsWith("$2");
    }

    @Test
    void hashPassword_ShouldThrowException_WhenPasswordIsNull() {
        // Arrange
        PasswordService passwordService = new PasswordService();

        // Act & Assert
        assertThatThrownBy(() -> passwordService.hashPassword(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароль не может быть null");
    }

    @Test
    void hashPassword_ShouldThrowException_WhenPasswordTooLong() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String longPassword = "a".repeat(73); // 73 символа > 72

        // Act & Assert
        assertThatThrownBy(() -> passwordService.hashPassword(longPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароль не может быть длиннее 72 символов");
    }

    @Test
    void hashPassword_ShouldHandle72CharacterPassword() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String maxLengthPassword = "a".repeat(72); // Ровно 72 символа

        // Act
        String hashedPassword = passwordService.hashPassword(maxLengthPassword);

        // Assert
        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword).isNotBlank();
        assertThat(hashedPassword).startsWith("$2");
    }

    @Test
    void hashPassword_ShouldHandleSpecialCharacters() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String passwordWithSpecialChars = "P@ssw0rd!123#";

        // Act
        String hashedPassword = passwordService.hashPassword(passwordWithSpecialChars);

        // Assert
        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword).isNotEqualTo(passwordWithSpecialChars);
        assertThat(hashedPassword).startsWith("$2");
    }

    @Test
    void hashPassword_ShouldHandleWhitespacePassword() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String whitespacePassword = "   ";

        // Act
        String hashedPassword = passwordService.hashPassword(whitespacePassword);

        // Assert
        assertThat(hashedPassword).isNotNull();
        assertThat(hashedPassword).isNotBlank();
        assertThat(hashedPassword).startsWith("$2");
    }

    @Test
    void hashPassword_ShouldReturnValidBCryptFormat() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String password = "testPassword";

        // Act
        String hashedPassword = passwordService.hashPassword(password);

        // Assert
        // Проверяем формат BCrypt: $2a$10$... (версия, стоимость, соль+хэш)
        assertThat(hashedPassword).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    @Test
    void matches_ShouldReturnTrue_ForCorrectPassword() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String plainPassword = "correctPassword";
        String hashedPassword = passwordService.hashPassword(plainPassword);

        // Act
        boolean result = passwordService.matches(plainPassword, hashedPassword);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void matches_ShouldReturnFalse_ForIncorrectPassword() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String correctPassword = "correctPassword";
        String wrongPassword = "wrongPassword";
        String hashedPassword = passwordService.hashPassword(correctPassword);

        // Act
        boolean result = passwordService.matches(wrongPassword, hashedPassword);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void matches_ShouldReturnFalse_WhenRawPasswordIsNull() {
        // Arrange
        PasswordService passwordService = new PasswordService();
        String hashedPassword = passwordService.hashPassword("test");

        // Act
        boolean result = passwordService.matches(null, hashedPassword);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void matches_ShouldReturnFalse_WhenEncodedPasswordIsNull() {
        // Arrange
        PasswordService passwordService = new PasswordService();

        // Act
        boolean result = passwordService.matches("test", null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void matches_ShouldReturnFalse_WhenBothNull() {
        // Arrange
        PasswordService passwordService = new PasswordService();

        // Act
        boolean result = passwordService.matches(null, null);

        // Assert
        assertThat(result).isFalse();
    }
}