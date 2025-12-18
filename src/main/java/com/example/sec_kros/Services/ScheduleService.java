package com.example.sec_kros.Services;

import com.example.sec_kros.Entities.Schedule;
import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Entities.GuardObject;
import com.example.sec_kros.DTO.ScheduleDTO;
import com.example.sec_kros.Repositories.ScheduleRepository;
import com.example.sec_kros.Repositories.EmployeeRepository;
import com.example.sec_kros.Repositories.GuardObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private GuardObjectRepository guardObjectRepository;

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public Optional<Schedule> getScheduleById(Long id) {
        return scheduleRepository.findById(id);
    }

    public Schedule createSchedule(ScheduleDTO scheduleDTO) {
        Employee employee = employeeRepository.findById(scheduleDTO.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
        GuardObject guardObject = guardObjectRepository.findById(scheduleDTO.getGuardObjectId())
                .orElseThrow(() -> new RuntimeException("Объект охраны не найден"));

        Schedule schedule = new Schedule();
        schedule.setEmployee(employee);
        schedule.setGuardObject(guardObject);
        schedule.setDate(scheduleDTO.getDate());
        schedule.setStartTime(scheduleDTO.getStartTime());
        schedule.setEndTime(scheduleDTO.getEndTime());
        schedule.setNotes(scheduleDTO.getNotes());

        return scheduleRepository.save(schedule);
    }

    public Schedule updateSchedule(Long id, ScheduleDTO scheduleDTO) {
        return scheduleRepository.findById(id)
                .map(schedule -> {
                    Employee employee = employeeRepository.findById(scheduleDTO.getEmployeeId())
                            .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));
                    GuardObject guardObject = guardObjectRepository.findById(scheduleDTO.getGuardObjectId())
                            .orElseThrow(() -> new RuntimeException("Объект охраны не найден"));

                    schedule.setEmployee(employee);
                    schedule.setGuardObject(guardObject);
                    schedule.setDate(scheduleDTO.getDate());
                    schedule.setStartTime(scheduleDTO.getStartTime());
                    schedule.setEndTime(scheduleDTO.getEndTime());
                    schedule.setNotes(scheduleDTO.getNotes());

                    return scheduleRepository.save(schedule);
                })
                .orElse(null);
    }

    public boolean deleteSchedule(Long id) {
        if (scheduleRepository.existsById(id)) {
            scheduleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Schedule> getSchedulesByContractId(Long contractId) {
        return scheduleRepository.findByGuardObject_Contract_Id(contractId);
    }
}