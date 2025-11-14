package com.example.sec_kros.DTO;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class GuardObjectDTO {
    private Long id;

    @NotNull(message = "Клиент обязателен")
    private Long clientId;

    @NotNull(message = "Договор обязателен")
    private Long contractId;

    @NotBlank(message = "Название объекта обязательно")
    @Size(max = 100, message = "Название объекта не должно превышать 100 символов")
    private String name;

    @NotBlank(message = "Адрес обязателен")
    @Size(max = 255, message = "Адрес не должен превышать 255 символов")
    private String address;

    @DecimalMin(value = "-90.0", message = "Широта должна быть в диапазоне от -90 до 90")
    @DecimalMax(value = "90.0", message = "Широта должна быть в диапазоне от -90 до 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Долгота должна быть в диапазоне от -180 до 180")
    @DecimalMax(value = "180.0", message = "Долгота должна быть в диапазоне от -180 до 180")
    private BigDecimal longitude;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    // Конструкторы
    public GuardObjectDTO() {}

    public GuardObjectDTO(Long id, Long clientId, Long contractId, String name,
                          String address, BigDecimal latitude, BigDecimal longitude, String description) {
        this.id = id;
        this.clientId = clientId;
        this.contractId = contractId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}