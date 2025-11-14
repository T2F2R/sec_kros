package com.example.sec_kros.DTO;

import jakarta.validation.constraints.*;

public class EmployeeDTO {
    private Long id;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
    private String lastName;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 50, message = "Имя не должно превышать 50 символов")
    private String firstName;

    @Size(max = 50, message = "Отчество не должно превышать 50 символов")
    private String patronymic;

    @NotNull(message = "Серия паспорта обязательна")
    @Min(value = 1000, message = "Серия паспорта должна быть 4 цифры")
    @Max(value = 9999, message = "Серия паспорта должна быть 4 цифры")
    private Integer passportSeries;

    @NotNull(message = "Номер паспорта обязателен")
    @Min(value = 100000, message = "Номер паспорта должен быть 6 цифр")
    @Max(value = 999999, message = "Номер паспорта должен быть 6 цифр")
    private Integer passportNumber;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+?[78][-\\(]?\\d{3}\\)?-?\\d{3}-?\\d{2}-?\\d{2}$",
            message = "Неверный формат телефона")
    private String phone;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Неверный формат email")
    private String email;

    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 50, message = "Логин должен быть от 3 до 50 символов")
    private String login;

    @NotBlank(message = "Должность обязательна")
    private String position;

    private Boolean isAdmin = false;

    private String password;

    // Конструкторы
    public EmployeeDTO() {}

    public EmployeeDTO(Long id, String lastName, String firstName, String patronymic,
                       Integer passportSeries, Integer passportNumber, String phone,
                       String email, String login, String position, Boolean isAdmin) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.patronymic = patronymic;
        this.passportSeries = passportSeries;
        this.passportNumber = passportNumber;
        this.phone = phone;
        this.email = email;
        this.login = login;
        this.position = position;
        this.isAdmin = isAdmin;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }

    public Integer getPassportSeries() { return passportSeries; }
    public void setPassportSeries(Integer passportSeries) { this.passportSeries = passportSeries; }

    public Integer getPassportNumber() { return passportNumber; }
    public void setPassportNumber(Integer passportNumber) { this.passportNumber = passportNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}