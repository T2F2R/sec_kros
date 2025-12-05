package com.example.sec_kros.DTO;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class ContractCreateDTO {

    @NotNull(message = "Услуга обязательна")
    private Long serviceId;

    @NotNull(message = "Дата начала обязательна")
    @FutureOrPresent(message = "Дата начала должна быть сегодня или в будущем")
    private LocalDate startDate;

    @NotNull(message = "Дата окончания обязательна")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDate endDate;

    // Поле необязательное, будет рассчитываться автоматически
    private Double totalAmount;

    // Конструкторы
    public ContractCreateDTO() {}

    // Геттеры и сеттеры
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    @AssertTrue(message = "Дата окончания должна быть после даты начала")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}