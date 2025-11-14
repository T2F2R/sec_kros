package com.example.sec_kros.DTO;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class ContractDTO {
    private Long id;

    @NotNull(message = "Клиент обязателен")
    private Long clientId;

    @NotNull(message = "Услуга обязательна")
    private Long serviceId;

    @NotNull(message = "Дата начала обязательна")
    @FutureOrPresent(message = "Дата начала должна быть сегодня или в будущем")
    private LocalDate startDate;

    @NotNull(message = "Дата окончания обязательна")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDate endDate;

    @DecimalMin(value = "0.0", message = "Сумма не может быть отрицательной")
    private Double totalAmount;

    @NotBlank(message = "Статус обязателен")
    private String status;

    // Конструкторы
    public ContractDTO() {}

    public ContractDTO(Long id, Long clientId, Long serviceId, LocalDate startDate,
                       LocalDate endDate, Double totalAmount, String status) {
        this.id = id;
        this.clientId = clientId;
        this.serviceId = serviceId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Валидация: дата окончания должна быть после даты начала
    @AssertTrue(message = "Дата окончания должна быть после даты начала")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // Пусть другие валидаторы обрабатывают null
        }
        return endDate.isAfter(startDate);
    }
}