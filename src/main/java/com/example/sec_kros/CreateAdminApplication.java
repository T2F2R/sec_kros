package com.example.sec_kros;

import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Repositories.EmployeeRepository;
import com.example.sec_kros.Services.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CreateAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreateAdminApplication.class, args);
    }

    @Bean
    public CommandLineRunner createDefaultAdmin(EmployeeRepository employeeRepository,
                                                PasswordService passwordService) {
        return args -> {
            if (!employeeRepository.findByLogin("admin").isPresent()) {
                Employee admin = new Employee();
                admin.setLastName("Иваонов");
                admin.setFirstName("Иван");
                admin.setPatronymic("Иванович");
                admin.setPassportSeries(1234);
                admin.setPassportNumber(567890);
                admin.setPhone("+79991234567");
                admin.setEmail("admin@test.ru");
                admin.setLogin("admin");
                admin.setPosition("Администратор");
                admin.setPasswordHash(passwordService.hashPassword("123456"));
                admin.setAdmin(true);

                employeeRepository.save(admin);
                System.out.println("Администратор успешно создан!");
            } else {
                System.out.println("Администратор уже существует");
            }
        };
    }
}