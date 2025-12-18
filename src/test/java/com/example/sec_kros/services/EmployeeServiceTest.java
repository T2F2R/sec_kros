package com.example.sec_kros.services;

import com.example.sec_kros.DTO.EmployeeDTO;
import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Repositories.EmployeeRepository;
import com.example.sec_kros.Services.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void getAllEmployees_ShouldReturnAllEmployees() {
        // Arrange
        List<Employee> employees = Arrays.asList(
                createEmployee(1L, "employee1@example.com", "login1"),
                createEmployee(2L, "employee2@example.com", "login2")
        );
        when(employeeRepository.findAll()).thenReturn(employees);

        // Act
        List<Employee> result = employeeService.getAllEmployees();

        // Assert
        assertThat(result).hasSize(2).containsAll(employees);
        verify(employeeRepository).findAll();
    }

    @Test
    void getEmployeeById_ShouldReturnEmployee() {
        // Arrange
        Long employeeId = 1L;
        Employee employee = createEmployee(employeeId, "test@example.com", "login");
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act
        Optional<Employee> result = employeeService.getEmployeeById(employeeId);

        // Assert
        assertThat(result).isPresent().contains(employee);
        verify(employeeRepository).findById(employeeId);
    }

    @Test
    void createEmployee_ShouldCreateEmployee_WhenValidData() {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        String encodedPassword = "encoded_password_hash";

        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(false);
        when(employeeRepository.existsByLogin(employeeDTO.getLogin())).thenReturn(false);
        when(passwordEncoder.encode(employeeDTO.getPassword())).thenReturn(encodedPassword);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Employee result = employeeService.createEmployee(employeeDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(employeeDTO.getEmail());
        assertThat(result.getLogin()).isEqualTo(employeeDTO.getLogin());
        assertThat(result.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(result.getIsAdmin()).isEqualTo(employeeDTO.getIsAdmin());

        verify(employeeRepository).existsByEmail(employeeDTO.getEmail());
        verify(employeeRepository).existsByLogin(employeeDTO.getLogin());
        verify(passwordEncoder).encode(employeeDTO.getPassword());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void createEmployee_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(employeeDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Сотрудник с таким email уже существует");

        verify(employeeRepository).existsByEmail(employeeDTO.getEmail());
        verify(employeeRepository, never()).existsByLogin(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_ShouldThrowException_WhenLoginAlreadyExists() {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(false);
        when(employeeRepository.existsByLogin(employeeDTO.getLogin())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(employeeDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Сотрудник с таким логином уже существует");

        verify(employeeRepository).existsByEmail(employeeDTO.getEmail());
        verify(employeeRepository).existsByLogin(employeeDTO.getLogin());
        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_ShouldThrowException_WhenPasswordIsNull() {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        employeeDTO.setPassword(null);

        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(false);
        when(employeeRepository.existsByLogin(employeeDTO.getLogin())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(employeeDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Пароль обязателен для создания сотрудника");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createEmployee_ShouldThrowException_WhenPasswordIsEmpty(String password) {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        employeeDTO.setPassword(password);

        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(false);
        when(employeeRepository.existsByLogin(employeeDTO.getLogin())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(employeeDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Пароль обязателен для создания сотрудника");
    }

    @Test
    void createEmployee_ShouldSetDefaultIsAdmin_WhenNull() {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        employeeDTO.setIsAdmin(null);
        String encodedPassword = "encoded_hash";

        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(false);
        when(employeeRepository.existsByLogin(employeeDTO.getLogin())).thenReturn(false);
        when(passwordEncoder.encode(employeeDTO.getPassword())).thenReturn(encodedPassword);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Employee result = employeeService.createEmployee(employeeDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsAdmin()).isFalse(); // Должно быть false по умолчанию
    }

    @Test
    void createEmployee_ShouldSetIsAdmin_WhenProvided() {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        employeeDTO.setIsAdmin(true);
        String encodedPassword = "encoded_hash";

        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(false);
        when(employeeRepository.existsByLogin(employeeDTO.getLogin())).thenReturn(false);
        when(passwordEncoder.encode(employeeDTO.getPassword())).thenReturn(encodedPassword);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Employee result = employeeService.createEmployee(employeeDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsAdmin()).isTrue();
    }

    @Test
    void updateEmployee_ShouldUpdateEmployee_WhenExists() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = createEmployee(employeeId, "old@example.com", "old_login");
        EmployeeDTO updateDTO = createValidEmployeeDTO();
        updateDTO.setPassword("new_password");
        String encodedPassword = "encoded_new_password";

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(passwordEncoder.encode(updateDTO.getPassword())).thenReturn(encodedPassword);
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        // Act
        Employee result = employeeService.updateEmployee(employeeId, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(updateDTO.getEmail());
        assertThat(result.getLogin()).isEqualTo(updateDTO.getLogin());
        assertThat(result.getPasswordHash()).isEqualTo(encodedPassword);

        verify(employeeRepository).findById(employeeId);
        verify(passwordEncoder).encode(updateDTO.getPassword());
        verify(employeeRepository).save(existingEmployee);
    }

    @Test
    void updateEmployee_ShouldReturnNull_WhenEmployeeNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        EmployeeDTO updateDTO = new EmployeeDTO();
        when(employeeRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Employee result = employeeService.updateEmployee(nonExistentId, updateDTO);

        // Assert
        assertThat(result).isNull();
        verify(employeeRepository).findById(nonExistentId);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateEmployee_ShouldNotUpdatePassword_WhenPasswordIsNull() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = createEmployee(employeeId, "test@example.com", "login");
        existingEmployee.setPasswordHash("old_hash");

        EmployeeDTO updateDTO = createValidEmployeeDTO();
        updateDTO.setPassword(null); // Пароль null

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        // Act
        Employee result = employeeService.updateEmployee(employeeId, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPasswordHash()).isEqualTo("old_hash"); // Старый пароль не изменился

        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository).save(existingEmployee);
    }

    @Test
    void updateEmployee_ShouldNotUpdatePassword_WhenPasswordIsEmpty() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = createEmployee(employeeId, "test@example.com", "login");
        existingEmployee.setPasswordHash("old_hash");

        EmployeeDTO updateDTO = createValidEmployeeDTO();
        updateDTO.setPassword(""); // Пустой пароль

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        // Act
        Employee result = employeeService.updateEmployee(employeeId, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPasswordHash()).isEqualTo("old_hash"); // Старый пароль не изменился

        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository).save(existingEmployee);
    }

    @Test
    void updateEmployee_ShouldSetDefaultIsAdmin_WhenNull() {
        // Arrange
        Long employeeId = 1L;
        Employee existingEmployee = createEmployee(employeeId, "test@example.com", "login");
        existingEmployee.setIsAdmin(true);

        EmployeeDTO updateDTO = createValidEmployeeDTO();
        updateDTO.setIsAdmin(null);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existingEmployee);

        // Act
        Employee result = employeeService.updateEmployee(employeeId, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsAdmin()).isFalse(); // Должно стать false
    }

    @Test
    void deleteEmployee_ShouldReturnTrueAndDelete_WhenEmployeeExists() {
        // Arrange
        Long employeeId = 1L;
        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        doNothing().when(employeeRepository).deleteById(employeeId);

        // Act
        boolean result = employeeService.deleteEmployee(employeeId);

        // Assert
        assertThat(result).isTrue();
        verify(employeeRepository).existsById(employeeId);
        verify(employeeRepository).deleteById(employeeId);
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        // Arrange
        String email = "existing@example.com";
        when(employeeRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean result = employeeService.existsByEmail(email);

        // Assert
        assertThat(result).isTrue();
        verify(employeeRepository).existsByEmail(email);
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailNotExists() {
        // Arrange
        String email = "nonexistent@example.com";
        when(employeeRepository.existsByEmail(email)).thenReturn(false);

        // Act
        boolean result = employeeService.existsByEmail(email);

        // Assert
        assertThat(result).isFalse();
        verify(employeeRepository).existsByEmail(email);
    }

    @Test
    void existsByLogin_ShouldReturnTrue_WhenLoginExists() {
        // Arrange
        String login = "existing_login";
        when(employeeRepository.existsByLogin(login)).thenReturn(true);

        // Act
        boolean result = employeeService.existsByLogin(login);

        // Assert
        assertThat(result).isTrue();
        verify(employeeRepository).existsByLogin(login);
    }

    @Test
    void getEmployeeByLogin_ShouldReturnEmployee_WhenLoginExists() {
        // Arrange
        String login = "test_login";
        Employee employee = createEmployee(1L, "test@example.com", login);
        when(employeeRepository.findByLogin(login)).thenReturn(Optional.of(employee));

        // Act
        Optional<Employee> result = employeeService.getEmployeeByLogin(login);

        // Assert
        assertThat(result).isPresent().contains(employee);
        verify(employeeRepository).findByLogin(login);
    }

    @Test
    void getEmployeesByPositionContaining_ShouldReturnEmployees() {
        // Arrange
        String position = "Охранник";
        List<Employee> employees = Arrays.asList(
                createEmployee(1L, "guard1@example.com", "guard1"),
                createEmployee(2L, "guard2@example.com", "guard2")
        );
        when(employeeRepository.findByPositionContaining(position)).thenReturn(employees);

        // Act
        List<Employee> result = employeeService.getEmployeesByPositionContaining(position);

        // Assert
        assertThat(result).hasSize(2).containsAll(employees);
        verify(employeeRepository).findByPositionContaining(position);
    }

    @Test
    void getSecurityEmployees_ShouldReturnSecurityEmployees() {
        // Arrange
        List<Employee> securityEmployees = Arrays.asList(
                createEmployee(1L, "guard1@example.com", "guard1"),
                createEmployee(2L, "guard2@example.com", "guard2")
        );
        when(employeeRepository.findByPositionContaining("Охранник")).thenReturn(securityEmployees);

        // Act
        List<Employee> result = employeeService.getSecurityEmployees();

        // Assert
        assertThat(result).hasSize(2).containsAll(securityEmployees);
        verify(employeeRepository).findByPositionContaining("Охранник");
    }

    @Test
    void countSecurityEmployees_ShouldReturnCount() {
        // Arrange
        long expectedCount = 5L;
        when(employeeRepository.countByPositionContaining("охран")).thenReturn(expectedCount);

        // Act
        long result = employeeService.countSecurityEmployees();

        // Assert
        assertThat(result).isEqualTo(expectedCount);
        verify(employeeRepository).countByPositionContaining("охран");
    }

    @Test
    void findByEmail_ShouldReturnEmployee_WhenEmailExists() {
        // Arrange
        String email = "test@example.com";
        Employee employee = createEmployee(1L, email, "login");
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        Employee result = employeeService.findByEmail(email);

        // Assert
        assertThat(result).isEqualTo(employee);
        verify(employeeRepository).findByEmail(email);
    }

    @Test
    void findByEmail_ShouldReturnNull_WhenEmailNotExists() {
        // Arrange
        String email = "nonexistent@example.com";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Employee result = employeeService.findByEmail(email);

        // Assert
        assertThat(result).isNull();
        verify(employeeRepository).findByEmail(email);
    }

    @Test
    void createEmployee_ShouldSetAllFieldsCorrectly() {
        // Arrange
        EmployeeDTO employeeDTO = createValidEmployeeDTO();
        String encodedPassword = "encoded_password_hash";

        when(employeeRepository.existsByEmail(employeeDTO.getEmail())).thenReturn(false);
        when(employeeRepository.existsByLogin(employeeDTO.getLogin())).thenReturn(false);
        when(passwordEncoder.encode(employeeDTO.getPassword())).thenReturn(encodedPassword);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        when(employeeRepository.save(employeeCaptor.capture())).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        employeeService.createEmployee(employeeDTO);

        // Assert
        Employee capturedEmployee = employeeCaptor.getValue();
        assertThat(capturedEmployee.getLastName()).isEqualTo(employeeDTO.getLastName());
        assertThat(capturedEmployee.getFirstName()).isEqualTo(employeeDTO.getFirstName());
        assertThat(capturedEmployee.getPatronymic()).isEqualTo(employeeDTO.getPatronymic());
        assertThat(capturedEmployee.getPassportSeries()).isEqualTo(employeeDTO.getPassportSeries());
        assertThat(capturedEmployee.getPassportNumber()).isEqualTo(employeeDTO.getPassportNumber());
        assertThat(capturedEmployee.getPhone()).isEqualTo(employeeDTO.getPhone());
        assertThat(capturedEmployee.getEmail()).isEqualTo(employeeDTO.getEmail());
        assertThat(capturedEmployee.getLogin()).isEqualTo(employeeDTO.getLogin());
        assertThat(capturedEmployee.getPosition()).isEqualTo(employeeDTO.getPosition());
        assertThat(capturedEmployee.getIsAdmin()).isEqualTo(employeeDTO.getIsAdmin());
        assertThat(capturedEmployee.getPasswordHash()).isEqualTo(encodedPassword);
    }

    // Вспомогательные методы
    private Employee createEmployee(Long id, String email, String login) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setLastName("Иванов");
        employee.setFirstName("Иван");
        employee.setPatronymic("Иванович");
        employee.setEmail(email);
        employee.setLogin(login);
        employee.setPasswordHash("hashed_password");
        employee.setIsAdmin(false);
        return employee;
    }

    private EmployeeDTO createValidEmployeeDTO() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setLastName("Петров");
        dto.setFirstName("Петр");
        dto.setPatronymic("Петрович");
        dto.setPassportSeries(1234); // Integer вместо String
        dto.setPassportNumber(567890); // Integer вместо String
        dto.setPhone("+79991234567");
        dto.setEmail("petrov@example.com");
        dto.setLogin("petrov_login");
        dto.setPosition("Охранник");
        dto.setIsAdmin(false);
        dto.setPassword("password123");
        return dto;
    }
}