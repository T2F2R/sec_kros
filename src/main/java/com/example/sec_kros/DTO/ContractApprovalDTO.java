package com.example.sec_kros.DTO;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class ContractApprovalDTO {

    @NotNull(message = "Сотрудник охраны обязателен")
    private Long securityEmployeeId;

    // Убрали securityStartDate - будет автоматически браться из контракта
    // private LocalDate securityStartDate;

    @NotNull(message = "Время начала смены обязательно")
    private LocalTime shiftStartTime;

    @NotNull(message = "Время окончания смены обязательно")
    private LocalTime shiftEndTime;

    private String notes;

    // Конструкторы
    public ContractApprovalDTO() {}

    public ContractApprovalDTO(Long securityEmployeeId, LocalTime shiftStartTime,
                               LocalTime shiftEndTime, String notes) {
        this.securityEmployeeId = securityEmployeeId;
        this.shiftStartTime = shiftStartTime;
        this.shiftEndTime = shiftEndTime;
        this.notes = notes;
    }

    // Геттеры и сеттеры
    public Long getSecurityEmployeeId() { return securityEmployeeId; }
    public void setSecurityEmployeeId(Long securityEmployeeId) { this.securityEmployeeId = securityEmployeeId; }

    public LocalTime getShiftStartTime() { return shiftStartTime; }
    public void setShiftStartTime(LocalTime shiftStartTime) { this.shiftStartTime = shiftStartTime; }

    public LocalTime getShiftEndTime() { return shiftEndTime; }
    public void setShiftEndTime(LocalTime shiftEndTime) { this.shiftEndTime = shiftEndTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}