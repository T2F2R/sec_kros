package com.example.sec_kros.Services;

import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.DTO.EmployeeDTO;
import com.example.sec_kros.Repositories.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public Employee createEmployee(EmployeeDTO employeeDTO) {
        if (employeeRepository.existsByEmail(employeeDTO.getEmail())) {
            throw new RuntimeException("Сотрудник с таким email уже существует");
        }
        if (employeeRepository.existsByLogin(employeeDTO.getLogin())) {
            throw new RuntimeException("Сотрудник с таким логином уже существует");
        }

        Employee employee = new Employee();
        employee.setLastName(employeeDTO.getLastName());
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setPatronymic(employeeDTO.getPatronymic());
        employee.setPassportSeries(employeeDTO.getPassportSeries());
        employee.setPassportNumber(employeeDTO.getPassportNumber());
        employee.setPhone(employeeDTO.getPhone());
        employee.setEmail(employeeDTO.getEmail());
        employee.setLogin(employeeDTO.getLogin());
        employee.setPosition(employeeDTO.getPosition());
        employee.setIsAdmin(employeeDTO.getIsAdmin() != null ? employeeDTO.getIsAdmin() : false);

        if (employeeDTO.getPassword() != null && !employeeDTO.getPassword().isEmpty()) {
            String hashedPassword = passwordEncoder.encode(employeeDTO.getPassword());
            employee.setPasswordHash(hashedPassword);
        } else {
            throw new RuntimeException("Пароль обязателен для создания сотрудника");
        }

        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(Long id, EmployeeDTO employeeDTO) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setLastName(employeeDTO.getLastName());
                    employee.setFirstName(employeeDTO.getFirstName());
                    employee.setPatronymic(employeeDTO.getPatronymic());
                    employee.setPassportSeries(employeeDTO.getPassportSeries());
                    employee.setPassportNumber(employeeDTO.getPassportNumber());
                    employee.setPhone(employeeDTO.getPhone());
                    employee.setEmail(employeeDTO.getEmail());
                    employee.setLogin(employeeDTO.getLogin());
                    employee.setPosition(employeeDTO.getPosition());
                    employee.setIsAdmin(employeeDTO.getIsAdmin() != null ? employeeDTO.getIsAdmin() : false);

                    if (employeeDTO.getPassword() != null && !employeeDTO.getPassword().isEmpty()) {
                        String hashedPassword = passwordEncoder.encode(employeeDTO.getPassword());
                        employee.setPasswordHash(hashedPassword);
                    }

                    return employeeRepository.save(employee);
                })
                .orElse(null);
    }

    public boolean deleteEmployee(Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByEmail(String email) {
        return employeeRepository.existsByEmail(email);
    }

    public boolean existsByLogin(String login) {
        return employeeRepository.existsByLogin(login);
    }

    public Optional<Employee> getEmployeeByLogin(String login) {
        return employeeRepository.findByLogin(login);
    }

    public List<Employee> getEmployeesByPositionContaining(String position) {
        return employeeRepository.findByPositionContaining(position);
    }

    public List<Employee> getSecurityEmployees() {
        return employeeRepository.findByPositionContaining("Охранник");
    }

    public long countSecurityEmployees() {
        return employeeRepository.countByPositionContaining("охран");
    }

    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElse(null);
    }
}