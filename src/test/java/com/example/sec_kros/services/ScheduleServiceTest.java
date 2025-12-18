package com.example.sec_kros.services;

import com.example.sec_kros.Entities.Employee;
import com.example.sec_kros.Entities.GuardObject;
import com.example.sec_kros.Entities.Schedule;
import com.example.sec_kros.DTO.ScheduleDTO;
import com.example.sec_kros.Repositories.ScheduleRepository;
import com.example.sec_kros.Repositories.EmployeeRepository;
import com.example.sec_kros.Repositories.GuardObjectRepository;
import com.example.sec_kros.Services.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private GuardObjectRepository guardObjectRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private Employee testEmployee;
    private GuardObject testGuardObject;
    private Schedule testSchedule;
    private ScheduleDTO testScheduleDTO;

    @BeforeEach
    void setUp() {
        // Создаем тестового сотрудника с минимальным набором полей
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setFirstName("Иван");
        testEmployee.setLastName("Иванов");
        // Только обязательные поля - остальные могут быть null

        // Создаем тестовый объект охраны с минимальным набором полей
        testGuardObject = new GuardObject();
        testGuardObject.setId(1L);
        testGuardObject.setName("Офисный центр 'Сити'");
        // Только обязательные поля - остальные могут быть null

        // Создаем тестовое расписание
        testSchedule = new Schedule();
        testSchedule.setId(1L);
        testSchedule.setEmployee(testEmployee);
        testSchedule.setGuardObject(testGuardObject);
        testSchedule.setDate(LocalDate.of(2024, 1, 15));
        testSchedule.setStartTime(LocalTime.of(8, 0));
        testSchedule.setEndTime(LocalTime.of(20, 0));
        testSchedule.setNotes("Дежурство в главном холле");

        // Создаем тестовый DTO
        testScheduleDTO = new ScheduleDTO();
        testScheduleDTO.setEmployeeId(1L);
        testScheduleDTO.setGuardObjectId(1L);
        testScheduleDTO.setDate(LocalDate.of(2024, 1, 15));
        testScheduleDTO.setStartTime(LocalTime.of(8, 0));
        testScheduleDTO.setEndTime(LocalTime.of(20, 0));
        testScheduleDTO.setNotes("Дежурство в главном холле");
    }

    @Test
    void getAllSchedules_ShouldReturnAllSchedules() {
        // Arrange
        Schedule schedule2 = new Schedule();
        schedule2.setId(2L);
        schedule2.setEmployee(testEmployee);
        schedule2.setGuardObject(testGuardObject);
        schedule2.setDate(LocalDate.of(2024, 1, 16));
        schedule2.setStartTime(LocalTime.of(20, 0));
        schedule2.setEndTime(LocalTime.of(8, 0));

        List<Schedule> schedules = Arrays.asList(testSchedule, schedule2);
        when(scheduleRepository.findAll()).thenReturn(schedules);

        // Act
        List<Schedule> result = scheduleService.getAllSchedules();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(scheduleRepository, times(1)).findAll();
    }

    @Test
    void getAllSchedules_ShouldReturnEmptyList_WhenNoSchedules() {
        // Arrange
        when(scheduleRepository.findAll()).thenReturn(List.of());

        // Act
        List<Schedule> result = scheduleService.getAllSchedules();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(scheduleRepository, times(1)).findAll();
    }

    @Test
    void getScheduleById_ShouldReturnSchedule_WhenExists() {
        // Arrange
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

        // Act
        Optional<Schedule> result = scheduleService.getScheduleById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getEmployee().getFirstName()).isEqualTo("Иван");
        assertThat(result.get().getGuardObject().getName()).isEqualTo("Офисный центр 'Сити'");
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 1, 15));

        verify(scheduleRepository, times(1)).findById(1L);
    }

    @Test
    void getScheduleById_ShouldReturnEmptyOptional_WhenNotExists() {
        // Arrange
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Schedule> result = scheduleService.getScheduleById(999L);

        // Assert
        assertThat(result).isEmpty();

        verify(scheduleRepository, times(1)).findById(999L);
    }

    @Test
    void createSchedule_ShouldCreateScheduleSuccessfully() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findById(1L)).thenReturn(Optional.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(testSchedule);

        // Act
        Schedule result = scheduleService.createSchedule(testScheduleDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmployee()).isEqualTo(testEmployee);
        assertThat(result.getGuardObject()).isEqualTo(testGuardObject);
        assertThat(result.getDate()).isEqualTo(testScheduleDTO.getDate());
        assertThat(result.getStartTime()).isEqualTo(testScheduleDTO.getStartTime());
        assertThat(result.getEndTime()).isEqualTo(testScheduleDTO.getEndTime());
        assertThat(result.getNotes()).isEqualTo(testScheduleDTO.getNotes());

        verify(employeeRepository, times(1)).findById(1L);
        verify(guardObjectRepository, times(1)).findById(1L);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    void createSchedule_ShouldThrowException_WhenEmployeeNotFound() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        ScheduleDTO dto = new ScheduleDTO();
        dto.setEmployeeId(999L);
        dto.setGuardObjectId(1L);
        dto.setDate(LocalDate.now());
        dto.setStartTime(LocalTime.now());
        dto.setEndTime(LocalTime.now().plusHours(8));

        // Act & Assert
        assertThatThrownBy(() -> scheduleService.createSchedule(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Сотрудник не найден");

        verify(employeeRepository, times(1)).findById(999L);
        verify(guardObjectRepository, never()).findById(anyLong());
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createSchedule_ShouldThrowException_WhenGuardObjectNotFound() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findById(999L)).thenReturn(Optional.empty());

        ScheduleDTO dto = new ScheduleDTO();
        dto.setEmployeeId(1L);
        dto.setGuardObjectId(999L);
        dto.setDate(LocalDate.now());
        dto.setStartTime(LocalTime.now());
        dto.setEndTime(LocalTime.now().plusHours(8));

        // Act & Assert
        assertThatThrownBy(() -> scheduleService.createSchedule(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Объект охраны не найден");

        verify(employeeRepository, times(1)).findById(1L);
        verify(guardObjectRepository, times(1)).findById(999L);
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void updateSchedule_ShouldUpdateScheduleSuccessfully() {
        // Arrange
        ScheduleDTO updateDTO = new ScheduleDTO();
        updateDTO.setEmployeeId(1L);
        updateDTO.setGuardObjectId(1L);
        updateDTO.setDate(LocalDate.of(2024, 1, 16));
        updateDTO.setStartTime(LocalTime.of(12, 0));
        updateDTO.setEndTime(LocalTime.of(0, 0));
        updateDTO.setNotes("Ночное дежурство");

        Schedule updatedSchedule = new Schedule();
        updatedSchedule.setId(1L);
        updatedSchedule.setEmployee(testEmployee);
        updatedSchedule.setGuardObject(testGuardObject);
        updatedSchedule.setDate(updateDTO.getDate());
        updatedSchedule.setStartTime(updateDTO.getStartTime());
        updatedSchedule.setEndTime(updateDTO.getEndTime());
        updatedSchedule.setNotes(updateDTO.getNotes());

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findById(1L)).thenReturn(Optional.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(updatedSchedule);

        // Act
        Schedule result = scheduleService.updateSchedule(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 1, 16));
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(0, 0));
        assertThat(result.getNotes()).isEqualTo("Ночное дежурство");

        verify(scheduleRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).findById(1L);
        verify(guardObjectRepository, times(1)).findById(1L);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    void updateSchedule_ShouldReturnNull_WhenScheduleNotFound() {
        // Arrange
        when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Schedule result = scheduleService.updateSchedule(999L, testScheduleDTO);

        // Assert
        assertThat(result).isNull();

        verify(scheduleRepository, times(1)).findById(999L);
        verify(employeeRepository, never()).findById(anyLong());
        verify(guardObjectRepository, never()).findById(anyLong());
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void updateSchedule_ShouldThrowException_WhenEmployeeNotFound() {
        // Arrange
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        ScheduleDTO dto = new ScheduleDTO();
        dto.setEmployeeId(999L);
        dto.setGuardObjectId(1L);
        dto.setDate(LocalDate.now());
        dto.setStartTime(LocalTime.now());
        dto.setEndTime(LocalTime.now().plusHours(8));

        // Act & Assert
        assertThatThrownBy(() -> scheduleService.updateSchedule(1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Сотрудник не найден");

        verify(scheduleRepository, times(1)).findById(1L);
        verify(employeeRepository, times(1)).findById(999L);
        verify(guardObjectRepository, never()).findById(anyLong());
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void deleteSchedule_ShouldReturnTrue_WhenScheduleExists() {
        // Arrange
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        doNothing().when(scheduleRepository).deleteById(1L);

        // Act
        boolean result = scheduleService.deleteSchedule(1L);

        // Assert
        assertThat(result).isTrue();

        verify(scheduleRepository, times(1)).existsById(1L);
        verify(scheduleRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteSchedule_ShouldReturnFalse_WhenScheduleNotExists() {
        // Arrange
        when(scheduleRepository.existsById(999L)).thenReturn(false);

        // Act
        boolean result = scheduleService.deleteSchedule(999L);

        // Assert
        assertThat(result).isFalse();

        verify(scheduleRepository, times(1)).existsById(999L);
        verify(scheduleRepository, never()).deleteById(anyLong());
    }

    @Test
    void createSchedule_ShouldHandleNullNotes() {
        // Arrange
        ScheduleDTO dtoWithoutNotes = new ScheduleDTO();
        dtoWithoutNotes.setEmployeeId(1L);
        dtoWithoutNotes.setGuardObjectId(1L);
        dtoWithoutNotes.setDate(LocalDate.now());
        dtoWithoutNotes.setStartTime(LocalTime.now());
        dtoWithoutNotes.setEndTime(LocalTime.now().plusHours(8));

        Schedule scheduleWithoutNotes = new Schedule();
        scheduleWithoutNotes.setId(2L);
        scheduleWithoutNotes.setEmployee(testEmployee);
        scheduleWithoutNotes.setGuardObject(testGuardObject);
        scheduleWithoutNotes.setDate(dtoWithoutNotes.getDate());
        scheduleWithoutNotes.setStartTime(dtoWithoutNotes.getStartTime());
        scheduleWithoutNotes.setEndTime(dtoWithoutNotes.getEndTime());
        scheduleWithoutNotes.setNotes(null);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findById(1L)).thenReturn(Optional.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(scheduleWithoutNotes);

        // Act
        Schedule result = scheduleService.createSchedule(dtoWithoutNotes);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNotes()).isNull();

        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    void updateSchedule_ShouldHandleEmptyNotes() {
        // Arrange
        ScheduleDTO dtoWithEmptyNotes = new ScheduleDTO();
        dtoWithEmptyNotes.setEmployeeId(1L);
        dtoWithEmptyNotes.setGuardObjectId(1L);
        dtoWithEmptyNotes.setDate(LocalDate.now());
        dtoWithEmptyNotes.setStartTime(LocalTime.now());
        dtoWithEmptyNotes.setEndTime(LocalTime.now().plusHours(8));
        dtoWithEmptyNotes.setNotes("");

        Schedule scheduleWithEmptyNotes = new Schedule();
        scheduleWithEmptyNotes.setId(1L);
        scheduleWithEmptyNotes.setEmployee(testEmployee);
        scheduleWithEmptyNotes.setGuardObject(testGuardObject);
        scheduleWithEmptyNotes.setDate(dtoWithEmptyNotes.getDate());
        scheduleWithEmptyNotes.setStartTime(dtoWithEmptyNotes.getStartTime());
        scheduleWithEmptyNotes.setEndTime(dtoWithEmptyNotes.getEndTime());
        scheduleWithEmptyNotes.setNotes("");

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findById(1L)).thenReturn(Optional.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(scheduleWithEmptyNotes);

        // Act
        Schedule result = scheduleService.updateSchedule(1L, dtoWithEmptyNotes);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNotes()).isEmpty();

        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    void createSchedule_ShouldHandleCrossMidnightSchedule() {
        // Arrange
        ScheduleDTO crossMidnightDTO = new ScheduleDTO();
        crossMidnightDTO.setEmployeeId(1L);
        crossMidnightDTO.setGuardObjectId(1L);
        crossMidnightDTO.setDate(LocalDate.of(2024, 1, 15));
        crossMidnightDTO.setStartTime(LocalTime.of(20, 0));
        crossMidnightDTO.setEndTime(LocalTime.of(8, 0));
        crossMidnightDTO.setNotes("Ночное дежурство");

        Schedule crossMidnightSchedule = new Schedule();
        crossMidnightSchedule.setId(3L);
        crossMidnightSchedule.setEmployee(testEmployee);
        crossMidnightSchedule.setGuardObject(testGuardObject);
        crossMidnightSchedule.setDate(crossMidnightDTO.getDate());
        crossMidnightSchedule.setStartTime(crossMidnightDTO.getStartTime());
        crossMidnightSchedule.setEndTime(crossMidnightDTO.getEndTime());
        crossMidnightSchedule.setNotes(crossMidnightDTO.getNotes());

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findById(1L)).thenReturn(Optional.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(crossMidnightSchedule);

        // Act
        Schedule result = scheduleService.createSchedule(crossMidnightDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(result.getEndTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void updateSchedule_ShouldUpdateWithDifferentEmployeeAndGuardObject() {
        // Arrange
        Employee newEmployee = new Employee();
        newEmployee.setId(2L);
        newEmployee.setFirstName("Петр");
        newEmployee.setLastName("Петров");

        GuardObject newGuardObject = new GuardObject();
        newGuardObject.setId(2L);
        newGuardObject.setName("Торговый центр 'Молл'");

        ScheduleDTO updateDTO = new ScheduleDTO();
        updateDTO.setEmployeeId(2L);
        updateDTO.setGuardObjectId(2L);
        updateDTO.setDate(LocalDate.now());
        updateDTO.setStartTime(LocalTime.of(9, 0));
        updateDTO.setEndTime(LocalTime.of(17, 0));

        Schedule updatedSchedule = new Schedule();
        updatedSchedule.setId(1L);
        updatedSchedule.setEmployee(newEmployee);
        updatedSchedule.setGuardObject(newGuardObject);
        updatedSchedule.setDate(updateDTO.getDate());
        updatedSchedule.setStartTime(updateDTO.getStartTime());
        updatedSchedule.setEndTime(updateDTO.getEndTime());

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(newEmployee));
        when(guardObjectRepository.findById(2L)).thenReturn(Optional.of(newGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(updatedSchedule);

        // Act
        Schedule result = scheduleService.updateSchedule(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmployee().getId()).isEqualTo(2L);
        assertThat(result.getEmployee().getFirstName()).isEqualTo("Петр");
        assertThat(result.getGuardObject().getId()).isEqualTo(2L);
        assertThat(result.getGuardObject().getName()).isEqualTo("Торговый центр 'Молл'");
    }

    @Test
    void createSchedule_ShouldSaveScheduleWithoutOptionalFields() {
        // Arrange
        ScheduleDTO minimalDTO = new ScheduleDTO();
        minimalDTO.setEmployeeId(1L);
        minimalDTO.setGuardObjectId(1L);
        minimalDTO.setDate(LocalDate.now());
        minimalDTO.setStartTime(LocalTime.of(9, 0));
        minimalDTO.setEndTime(LocalTime.of(17, 0));
        // Не устанавливаем notes

        Schedule minimalSchedule = new Schedule();
        minimalSchedule.setId(4L);
        minimalSchedule.setEmployee(testEmployee);
        minimalSchedule.setGuardObject(testGuardObject);
        minimalSchedule.setDate(minimalDTO.getDate());
        minimalSchedule.setStartTime(minimalDTO.getStartTime());
        minimalSchedule.setEndTime(minimalDTO.getEndTime());
        // notes остается null

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findById(1L)).thenReturn(Optional.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(minimalSchedule);

        // Act
        Schedule result = scheduleService.createSchedule(minimalDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getNotes()).isNull();
    }

    @Test
    void getAllSchedules_ShouldReturnSchedulesSortedByDate() {
        // Arrange
        Schedule schedule1 = new Schedule();
        schedule1.setId(1L);
        schedule1.setEmployee(testEmployee);
        schedule1.setGuardObject(testGuardObject);
        schedule1.setDate(LocalDate.of(2024, 1, 16));
        schedule1.setStartTime(LocalTime.of(8, 0));

        Schedule schedule2 = new Schedule();
        schedule2.setId(2L);
        schedule2.setEmployee(testEmployee);
        schedule2.setGuardObject(testGuardObject);
        schedule2.setDate(LocalDate.of(2024, 1, 15));
        schedule2.setStartTime(LocalTime.of(8, 0));

        Schedule schedule3 = new Schedule();
        schedule3.setId(3L);
        schedule3.setEmployee(testEmployee);
        schedule3.setGuardObject(testGuardObject);
        schedule3.setDate(LocalDate.of(2024, 1, 17));
        schedule3.setStartTime(LocalTime.of(8, 0));

        List<Schedule> schedules = Arrays.asList(schedule1, schedule2, schedule3);
        when(scheduleRepository.findAll()).thenReturn(schedules);

        // Act
        List<Schedule> result = scheduleService.getAllSchedules();

        // Assert
        assertThat(result).hasSize(3);
        // Проверяем, что данные возвращаются в том порядке, в котором их вернул репозиторий
        assertThat(result.get(0).getId()).isEqualTo(1L); // 16 января
        assertThat(result.get(1).getId()).isEqualTo(2L); // 15 января
        assertThat(result.get(2).getId()).isEqualTo(3L); // 17 января
    }
}