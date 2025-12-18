package com.example.sec_kros.services;

import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Repositories.ClientRepository;
import com.example.sec_kros.Repositories.EmployeeRepository;
import com.example.sec_kros.Services.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_ShouldReturnAdminUser_WhenAdminEmployeeExists() {
        // Arrange
        String email = "admin@example.com";
        Employee adminEmployee = new Employee();
        adminEmployee.setEmail(email);
        adminEmployee.setPasswordHash("hashed_password");
        adminEmployee.setIsAdmin(true);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(adminEmployee));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password");

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");

        verify(employeeRepository).findByEmail(email);
        verify(clientRepository, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsername_ShouldReturnEmployeeUser_WhenNonAdminEmployeeExists() {
        // Arrange
        String email = "employee@example.com";
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPasswordHash("hashed_password");
        employee.setIsAdmin(false);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password");

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_EMPLOYEE");

        verify(employeeRepository).findByEmail(email);
        verify(clientRepository, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsername_ShouldReturnClientUser_WhenClientExists() {
        // Arrange
        String email = "client@example.com";
        Client client = new Client();
        client.setEmail(email);
        client.setPasswordHash("hashed_password");

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password");

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_CLIENT");

        verify(employeeRepository).findByEmail(email);
        verify(clientRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_ShouldCheckEmployeeFirst_ThenClient() {
        // Arrange
        String email = "user@example.com";
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPasswordHash("employee_password");
        employee.setIsAdmin(false);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("employee_password");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_EMPLOYEE");

        verify(employeeRepository).findByEmail(email);
        verify(clientRepository, never()).findByEmail(anyString());
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        String email = "nonexistent@example.com";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Пользователь с email " + email + " не найден");

        verify(employeeRepository).findByEmail(email);
        verify(clientRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_ShouldHandleNullEmployeeAdminField() {
        // Arrange
        String email = "employee@example.com";
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPasswordHash("hashed_password");
        employee.setIsAdmin(null);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_EMPLOYEE");
    }

    @Test
    void loadUserByUsername_ShouldHandleEmptyEmail() {
        // Arrange
        String email = "";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Пользователь с email " + email + " не найден");
    }

    @Test
    void loadUserByUsername_ShouldHandleNullEmail() {
        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Пользователь с email null не найден");
    }

    @Test
    void loadUserByUsername_ShouldCreateUserWithCorrectProperties() {
        // Arrange
        String email = "test@example.com";
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPasswordHash("$2a$10$hashedPassword123");
        employee.setIsAdmin(true);

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$hashedPassword123");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        verify(employeeRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_ShouldReturnClientWithCorrectProperties() {
        // Arrange
        String email = "client@example.com";
        Client client = new Client();
        client.setEmail(email);
        client.setPasswordHash("client_hashed_password");

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("client_hashed_password");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void loadUserByUsername_ShouldHandleEmployeeWithAllFields() {
        // Arrange
        String email = "full.employee@example.com";
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Иван");
        employee.setLastName("Иванов");
        employee.setEmail(email);
        employee.setPhone("+79991234567");
        employee.setPosition("Охранник");
        employee.setPasswordHash("hashed123");
        employee.setIsAdmin(false);
        // createdAt может отсутствовать

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("hashed123");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void loadUserByUsername_ShouldHandleClientWithAllFields() {
        // Arrange
        String email = "full.client@example.com";
        Client client = new Client();
        client.setId(1L);
        client.setFirstName("Петр");
        client.setLastName("Петров");
        client.setPatronymic("Петрович");
        client.setEmail(email);
        client.setPhone("+79997654321");
        client.setAddress("ул. Примерная, 1");
        client.setPasswordHash("client_hash");
        // createdAt может отсутствовать или быть по-другому

        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(client));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("client_hash");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenEmailIsBlank() {
        // Arrange
        String email = "   ";
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Пользователь с email " + email + " не найден");
    }

    @Test
    void loadUserByUsername_ShouldHandleCaseSensitiveEmail() {
        // Arrange
        String email = "Test@Example.COM";
        Employee employee = new Employee();
        employee.setEmail("test@example.com"); // другой регистр
        employee.setPasswordHash("hash");
        employee.setIsAdmin(false);

        // Репозиторий чувствителен к регистру
        when(employeeRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(clientRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(employeeRepository).findByEmail(email);
        verify(clientRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_ShouldWorkWithDifferentEmailFormats() {
        // Arrange
        String[] emails = {
                "test@example.com",
                "test.user@example.com",
                "test_user@example.com",
                "test-user@example.com",
                "test123@example.com",
                "тест@пример.рф", // кириллический email
                "test+tag@example.com"
        };

        for (String email : emails) {
            Employee employee = new Employee();
            employee.setEmail(email);
            employee.setPasswordHash("hash");
            employee.setIsAdmin(false);

            when(employeeRepository.findByEmail(email)).thenReturn(Optional.of(employee));

            // Act
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Assert
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(email);

            // Reset mock для следующей итерации
            reset(employeeRepository);
        }
    }
}