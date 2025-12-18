package com.example.sec_kros.Services;

import com.example.sec_kros.DTO.AuthResponse;
import com.example.sec_kros.DTO.RegisterRequest;
import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Repositories.ClientRepository;
import com.example.sec_kros.Repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordService passwordService;

    public String determineUserType(String email) {
        // Теперь пароль проверяется Spring Security, мы только определяем тип пользователя
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        if (employeeOpt.isPresent()) {
            Employee employee = employeeOpt.get();
            return Boolean.TRUE.equals(employee.getIsAdmin()) ? "admin" : "employee";
        }

        Optional<Client> clientOpt = clientRepository.findByEmail(email);
        if (clientOpt.isPresent()) {
            return "client";
        }

        return null;
    }

    public String getRedirectUrl(String userType) {
        if (userType == null) {
            return "/login?error=true";
        }

        switch (userType) {
            case "admin":
                return "/admin/dashboard";
            case "employee":
                return "/employee/dashboard";
            case "client":
                return "/client/dashboard";
            default:
                return "/login?error=true";
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // Проверяем, что пароли совпадают
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            return new AuthResponse(false, "Пароли не совпадают", null, null);
        }

        // Проверяем, нет ли уже пользователя с таким email
        if (clientRepository.existsByEmail(registerRequest.getEmail()) ||
                employeeRepository.existsByEmail(registerRequest.getEmail())) {
            return new AuthResponse(false, "Пользователь с таким email уже существует", null, null);
        }

        // Создаем нового клиента
        Client client = new Client();
        client.setLastName(registerRequest.getLastName());
        client.setFirstName(registerRequest.getFirstName());
        client.setPatronymic(registerRequest.getPatronymic());
        client.setPhone(registerRequest.getPhone());
        client.setEmail(registerRequest.getEmail());
        client.setAddress(registerRequest.getAddress());
        client.setPasswordHash(passwordService.hashPassword(registerRequest.getPassword()));
        client.setCreatedAt(LocalDateTime.now());

        try {
            clientRepository.save(client);
            return new AuthResponse(true, "Регистрация успешна! Теперь вы можете войти.", "client", "/login");
        } catch (Exception e) {
            return new AuthResponse(false, "Ошибка при регистрации: " + e.getMessage(), null, null);
        }
    }
}