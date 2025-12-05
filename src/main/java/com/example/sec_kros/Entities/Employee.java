package com.example.sec_kros.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "patronymic", length = 50)
    private String patronymic;

    @Column(name = "passport_series", nullable = false)
    private Integer passportSeries;

    @Column(name = "passport_number", nullable = false)
    private Integer passportNumber;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "login", length = 50, unique = true)
    private String login;

    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    public Employee() {}

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

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }

    public String getFullName() {
        if (patronymic != null && !patronymic.isEmpty()) {
            return lastName + " " + firstName + " " + patronymic;
        }
        return lastName + " " + firstName;
    }

    public String getPassportInfo() {
        return passportSeries + " " + passportNumber;
    }
}