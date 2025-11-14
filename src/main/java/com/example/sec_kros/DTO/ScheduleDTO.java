package com.example.sec_kros.DTO;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleDTO {
    private Long id;

    @NotNull(message = "Сотрудник обязателен")
    private Long employeeId;

    @NotNull(message = "Объект обязателен")
    private Long guardObjectId;

    @NotNull(message = "Дата обязательна")
    private LocalDate date;

    @NotNull(message = "Время начала обязательно")
    private LocalTime startTime;

    @NotNull(message = "Время окончания обязательно")
    private LocalTime endTime;

    private String notes;

    // Конструкторы
    public ScheduleDTO() {}

    public ScheduleDTO(Long id, Long employeeId, Long guardObjectId, LocalDate date,
                       LocalTime startTime, LocalTime endTime, String notes) {
        this.id = id;
        this.employeeId = employeeId;
        this.guardObjectId = guardObjectId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public Long getGuardObjectId() { return guardObjectId; }
    public void setGuardObjectId(Long guardObjectId) { this.guardObjectId = guardObjectId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}