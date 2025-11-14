package com.example.sec_kros.DTO;

import com.example.sec_kros.Entities.Contract;
import com.example.sec_kros.Entities.GuardObject;
import com.example.sec_kros.Entities.Schedule;

import java.util.List;

public class ContractDeletionInfo {
    private Contract contract;
    private int guardObjectsCount;
    private int schedulesCount;
    private List<GuardObject> guardObjects;
    private List<Schedule> schedules;

    // Геттеры и сеттеры
    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public int getGuardObjectsCount() {
        return guardObjectsCount;
    }

    public void setGuardObjectsCount(int guardObjectsCount) {
        this.guardObjectsCount = guardObjectsCount;
    }

    public int getSchedulesCount() {
        return schedulesCount;
    }

    public void setSchedulesCount(int schedulesCount) {
        this.schedulesCount = schedulesCount;
    }

    public List<GuardObject> getGuardObjects() {
        return guardObjects;
    }

    public void setGuardObjects(List<GuardObject> guardObjects) {
        this.guardObjects = guardObjects;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }
}