package com.example.sec_kros.DTO;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ServiceDTO {
    private Long id;

    @NotBlank(message = "Название услуги обязательно")
    @Size(max = 100, message = "Название услуги не должно превышать 100 символов")
    private String name;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше 0")
    @Digits(integer = 10, fraction = 2, message = "Неверный формат цены")
    private BigDecimal price;

    // Конструкторы
    public ServiceDTO() {}

    public ServiceDTO(Long id, String name, String description, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}