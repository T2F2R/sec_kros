package com.example.sec_kros.services;

import com.example.sec_kros.DTO.AuthResponse;
import com.example.sec_kros.DTO.RegisterRequest;
import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Repositories.ClientRepository;
import com.example.sec_kros.Repositories.EmployeeRepository;
import com.example.sec_kros.Services.AuthService;
import com.example.sec_kros.Services.PasswordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private AuthService authService;

    // ========== Тесты для determineUserType() ==========

    @Test
    void determineUserType_ShouldReturnAdmin_WhenAdminEmployeeExists() {
        // Arrange
        String email = "admin@example.com";
        Employee adminEmployee = new Employee();
        adminEmployee.setEmail(email);
        adminEmployee.setIsAdmin(true);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(adminEmployee));

        // Act
        String result = authService.determineUserType(email);

        // Assert
        assertThat(result).isEqualTo("admin");
        verify(employeeRepository).findByEmail(email);
        verifyNoInteractions(clientRepository);
    }

    @Test
    void determineUserType_ShouldReturnEmployee_WhenNonAdminEmployeeExists() {
        // Arrange
        String email = "employee@example.com";
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setIsAdmin(false);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        String result = authService.determineUserType(email);

        // Assert
        assertThat(result).isEqualTo("employee");
        verify(employeeRepository).findByEmail(email);
        verifyNoInteractions(clientRepository);
    }

    @Test
    void determineUserType_ShouldReturnClient_WhenClientExists() {
        // Arrange
        String email = "client@example.com";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());

        Client client = new Client();
        client.setEmail(email);
        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));

        // Act
        String result = authService.determineUserType(email);

        // Assert
        assertThat(result).isEqualTo("client");
        verify(employeeRepository).findByEmail(email);
        verify(clientRepository).findByEmail(email);
    }

    @Test
    void determineUserType_ShouldReturnNull_WhenUserNotFound() {
        // Arrange
        String email = "nonexistent@example.com";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        String result = authService.determineUserType(email);

        // Assert
        assertThat(result).isNull();
        verify(employeeRepository).findByEmail(email);
        verify(clientRepository).findByEmail(email);
    }

    // ========== Тесты для getRedirectUrl() ==========

    @ParameterizedTest
    @MethodSource("provideUserTypesAndUrls")
    void getRedirectUrl_ShouldReturnCorrectUrl_ForGivenUserType(String userType, String expectedUrl) {
        // Act
        String result = authService.getRedirectUrl(userType);

        // Assert
        assertThat(result).isEqualTo(expectedUrl);
    }

    private static Stream<Arguments> provideUserTypesAndUrls() {
        return Stream.of(
                Arguments.of("admin", "/admin/dashboard"),
                Arguments.of("employee", "/employee/dashboard"),
                Arguments.of("client", "/client/dashboard"),
                Arguments.of("unknown", "/login?error=true"),
                Arguments.of(null, "/login?error=true"),
                Arguments.of("", "/login?error=true")
        );
    }

    // ========== Тесты для register() ==========

    @Test
    void register_ShouldReturnSuccessResponse_WhenValidClientRegistration() {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();
        String hashedPassword = "hashed_password_123";

        when(clientRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordService.hashPassword(request.getPassword())).thenReturn(hashedPassword);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client savedClient = invocation.getArgument(0);
            savedClient.setId(1L); // Симулируем сохранение с ID
            return savedClient;
        });

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Регистрация успешна! Теперь вы можете войти.");
        assertThat(response.getUserType()).isEqualTo("client");
        assertThat(response.getRedirectUrl()).isEqualTo("/login");

        verify(clientRepository).existsByEmail(request.getEmail());
        verify(employeeRepository).existsByEmail(request.getEmail());
        verify(passwordService).hashPassword(request.getPassword());
        verify(clientRepository).save(any(Client.class));

        // Проверяем, что сохраненный клиент имеет правильные данные
        verify(clientRepository).save(argThat(client ->
                client.getEmail().equals(request.getEmail()) &&
                        client.getPasswordHash().equals(hashedPassword) &&
                        client.getFirstName().equals(request.getFirstName()) &&
                        client.getLastName().equals(request.getLastName()) &&
                        client.getPatronymic().equals(request.getPatronymic()) &&
                        client.getPhone().equals(request.getPhone()) &&
                        client.getAddress().equals(request.getAddress()) &&
                        client.getCreatedAt() != null
        ));
    }

    @Test
    void register_ShouldReturnErrorResponse_WhenPasswordsDoNotMatch() {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();
        request.setConfirmPassword("different_password");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Пароли не совпадают");
        assertThat(response.getUserType()).isNull();
        assertThat(response.getRedirectUrl()).isNull();

        verifyNoInteractions(clientRepository, employeeRepository, passwordService);
    }

    @Test
    void register_ShouldReturnErrorResponse_WhenClientEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();

        when(clientRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Пользователь с таким email уже существует");
        assertThat(response.getUserType()).isNull();
        assertThat(response.getRedirectUrl()).isNull();

        verify(clientRepository).existsByEmail(request.getEmail());
        verifyNoInteractions(employeeRepository, passwordService);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void register_ShouldReturnErrorResponse_WhenEmployeeEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();

        when(clientRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Пользователь с таким email уже существует");
        assertThat(response.getUserType()).isNull();
        assertThat(response.getRedirectUrl()).isNull();

        verify(clientRepository).existsByEmail(request.getEmail());
        verify(employeeRepository).existsByEmail(request.getEmail());
        verifyNoInteractions(passwordService);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void register_ShouldReturnErrorResponse_WhenRepositorySaveFails() {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();
        String hashedPassword = "hashed_password_123";

        when(clientRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordService.hashPassword(request.getPassword())).thenReturn(hashedPassword);
        when(clientRepository.save(any(Client.class))).thenThrow(new RuntimeException("Database connection failed"));

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Ошибка при регистрации");
        assertThat(response.getUserType()).isNull();
        assertThat(response.getRedirectUrl()).isNull();

        verify(clientRepository).existsByEmail(request.getEmail());
        verify(employeeRepository).existsByEmail(request.getEmail());
        verify(passwordService).hashPassword(request.getPassword());
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void register_ShouldSetCorrectTimestamp_WhenCreatingClient() {
        // Arrange
        RegisterRequest request = createValidRegisterRequest();
        String hashedPassword = "hashed_password_123";

        when(clientRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordService.hashPassword(request.getPassword())).thenReturn(hashedPassword);

        LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0);
        try (var mockedLocalDateTime = mockStatic(LocalDateTime.class)) {
            mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fixedTime);

            when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
                Client savedClient = invocation.getArgument(0);
                savedClient.setId(1L);
                return savedClient;
            });

            // Act
            authService.register(request);

            // Assert
            verify(clientRepository).save(argThat(client ->
                    client.getCreatedAt().equals(fixedTime)
            ));
        }
    }

    // ========== Вспомогательные методы ==========

    private RegisterRequest createValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setLastName("Иванов");
        request.setFirstName("Иван");
        request.setPatronymic("Иванович");
        request.setPhone("+79991234567");
        request.setEmail("ivanov@example.com");
        request.setAddress("г. Москва, ул. Примерная, д. 1");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        return request;
    }

    // ========== Дополнительные edge-case тесты ==========

    @Test
    void determineUserType_ShouldCheckEmployeeFirst_ThenClient() {
        // Arrange
        String email = "user@example.com";
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setIsAdmin(false);

        // Оба репозитория вернут пользователя, но должен вернуться employee
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));
        // ClientRepository не должен вызываться, т.к. employee уже найден

        // Act
        String result = authService.determineUserType(email);

        // Assert
        assertThat(result).isEqualTo("employee");
        verify(employeeRepository).findByEmail(email);
        verify(clientRepository, never()).findByEmail(anyString());
    }

    @Test
    void register_ShouldHandleNullFieldsGracefully() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("pass");
        request.setConfirmPassword("pass");
        // Остальные поля null

        when(clientRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordService.hashPassword(request.getPassword())).thenReturn("hashed");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        verify(clientRepository).save(argThat(client ->
                client.getLastName() == null &&
                        client.getFirstName() == null &&
                        client.getPatronymic() == null &&
                        client.getPhone() == null &&
                        client.getAddress() == null
        ));
    }
}