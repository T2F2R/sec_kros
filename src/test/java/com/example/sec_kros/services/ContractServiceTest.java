package com.example.sec_kros.services;

import com.example.sec_kros.DTO.ContractApprovalDTO;
import com.example.sec_kros.DTO.ContractDTO;
import com.example.sec_kros.DTO.ContractDeletionInfo;
import com.example.sec_kros.DTO.ValidationResult;
import com.example.sec_kros.Entities.*;
import com.example.sec_kros.Repositories.*;
import com.example.sec_kros.Services.ContractService;
import com.example.sec_kros.Services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock private ContractRepository contractRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private GuardObjectRepository guardObjectRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;

    @InjectMocks private ContractService contractService;

    private Client testClient;
    private ServiceEntity testService;
    private Contract testContract;
    private ContractDTO testContractDTO;
    private Employee testEmployee;
    private GuardObject testGuardObject;

    @BeforeEach
    void setUp() {
        testClient = new Client();
        testClient.setId(1L);
        testClient.setFirstName("Иван");
        testClient.setLastName("Иванов");
        testClient.setEmail("ivanov@example.com");

        testService = new ServiceEntity();
        testService.setId(1L);
        testService.setName("Охрана офиса");

        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setFirstName("Петр");
        testEmployee.setLastName("Петров");
        testEmployee.setEmail("petrov@example.com");

        testGuardObject = new GuardObject();
        testGuardObject.setId(1L);
        testGuardObject.setName("Офисный комплекс");
        testGuardObject.setAddress("ул. Ленина, 1");

        testContract = new Contract();
        testContract.setId(1L);
        testContract.setClient(testClient);
        testContract.setService(testService);
        testContract.setStartDate(LocalDate.now().plusDays(1));
        testContract.setEndDate(LocalDate.now().plusDays(31));
        testContract.setTotalAmount(BigDecimal.valueOf(10000));
        testContract.setStatus("draft");
        testContract.setCreatedAt(LocalDateTime.now());

        testContractDTO = new ContractDTO();
        testContractDTO.setClientId(1L);
        testContractDTO.setServiceId(1L);
        testContractDTO.setStartDate(LocalDate.now().plusDays(1));
        testContractDTO.setEndDate(LocalDate.now().plusDays(31));
        testContractDTO.setTotalAmount(10000.0);
        testContractDTO.setStatus("draft");
    }

    @Test
    void getAllContracts_ShouldReturnAllContracts() {
        when(contractRepository.findAll()).thenReturn(Arrays.asList(testContract));
        List<Contract> result = contractService.getAllContracts();
        assertThat(result).hasSize(1).contains(testContract);
        verify(contractRepository).findAll();
    }

    @Test
    void getContractById_ShouldReturnContract() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        Optional<Contract> result = contractService.getContractById(1L);
        assertThat(result).isPresent().contains(testContract);
        verify(contractRepository).findById(1L);
    }

    @Test
    void createContract_ShouldCreateContract() {
        Contract newContract = new Contract();
        newContract.setStartDate(LocalDate.now().plusDays(1));
        newContract.setEndDate(LocalDate.now().plusDays(31));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
            Contract saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Contract result = contractService.createContract(1L, 1L, newContract);

        assertThat(result).isNotNull();
        assertThat(result.getClient()).isEqualTo(testClient);
        assertThat(result.getService()).isEqualTo(testService);
        assertThat(result.getStatus()).isEqualTo("active");

        verify(clientRepository).findById(1L);
        verify(serviceRepository).findById(1L);
        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    void createContract_ShouldReturnNull_WhenClientNotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        Contract result = contractService.createContract(1L, 1L, new Contract());

        assertThat(result).isNull();
        verify(clientRepository).findById(1L);
        // Теперь serviceRepository.findById не должен вызываться
        verify(serviceRepository, never()).findById(anyLong());
        verify(contractRepository, never()).save(any());
    }

    @Test
    void createContract_ShouldReturnNull_WhenServiceNotFound() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        Contract result = contractService.createContract(1L, 1L, new Contract());

        assertThat(result).isNull();
        verify(clientRepository).findById(1L);
        verify(serviceRepository).findById(1L);
        verify(contractRepository, never()).save(any());
    }

    @Test
    void deleteContract_ShouldReturnTrueAndDelete() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        when(guardObjectRepository.findByContractId(1L)).thenReturn(Collections.emptyList());
        doNothing().when(contractRepository).delete(testContract);

        boolean result = contractService.deleteContract(1L);

        assertThat(result).isTrue();
        verify(contractRepository).findById(1L);
        verify(contractRepository).delete(testContract);
    }

    @Test
    void createContract_WithDTO_ShouldCreateContract() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(testService));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
            Contract saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        Contract result = contractService.createContract(testContractDTO);

        assertThat(result).isNotNull();
        verify(clientRepository).findById(1L);
        verify(serviceRepository).findById(1L);
        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    void createContract_WithDTO_ShouldThrowException_WhenStartDateInPast() {
        testContractDTO.setStartDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> contractService.createContract(testContractDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Дата начала не может быть в прошлом");
    }

    @Test
    void validateContractForApproval_ShouldReturnValidResult() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        when(guardObjectRepository.existsByContractId(1L)).thenReturn(true);

        ValidationResult result = contractService.validateContractForApproval(1L);

        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void approveContract_WithDTO_ShouldApproveAndCreateSchedule() {
        ContractApprovalDTO approvalDTO = new ContractApprovalDTO();
        approvalDTO.setSecurityEmployeeId(1L);
        approvalDTO.setShiftStartTime(LocalTime.of(8, 0));
        approvalDTO.setShiftEndTime(LocalTime.of(20, 0));
        approvalDTO.setNotes("Особые указания");

        testContract.setStatus("draft");

        // Настраиваем моки с учетом однократных вызовов
        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findByContractId(1L)).thenReturn(List.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());
        when(contractRepository.save(any(Contract.class))).thenReturn(testContract);

        // Email моки
        doNothing().when(emailService).sendContractApprovalEmailToClient(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        doNothing().when(emailService).sendContractApprovalEmailToEmployee(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        // Act
        Contract result = contractService.approveContract(1L, approvalDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("active");

        // Проверяем вызовы (теперь каждый метод вызывается 1 раз)
        verify(employeeRepository, times(1)).findById(1L);
        verify(guardObjectRepository, times(1)).findByContractId(1L);
        verify(contractRepository).findById(1L);
        verify(scheduleRepository, times(7)).save(any(Schedule.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailService, times(1)).sendContractApprovalEmailToClient(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(emailService, times(1)).sendContractApprovalEmailToEmployee(anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(contractRepository).save(testContract);
    }

    @Test
    void approveContract_WithDTO_ShouldThrowException_WhenNoGuardObject() {
        ContractApprovalDTO approvalDTO = new ContractApprovalDTO();
        approvalDTO.setSecurityEmployeeId(1L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        when(guardObjectRepository.findByContractId(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> contractService.approveContract(1L, approvalDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Не найден охранный объект");
    }

    @Test
    void approveContract_WithDTO_ShouldThrowException_WhenEmployeeNotFound() {
        ContractApprovalDTO approvalDTO = new ContractApprovalDTO();
        approvalDTO.setSecurityEmployeeId(999L);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        when(guardObjectRepository.findByContractId(1L)).thenReturn(List.of(testGuardObject));
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.approveContract(1L, approvalDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Выбранный сотрудник не найден");
    }

    @Test
    void approveContract_WithDTO_ShouldHandleEmailException() {
        ContractApprovalDTO approvalDTO = new ContractApprovalDTO();
        approvalDTO.setSecurityEmployeeId(1L);
        approvalDTO.setShiftStartTime(LocalTime.of(8, 0));
        approvalDTO.setShiftEndTime(LocalTime.of(20, 0));

        testContract.setStatus("draft");

        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findByContractId(1L)).thenReturn(List.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(new Schedule());
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // Бросаем исключение при отправке email
        doThrow(new RuntimeException("SMTP error")).when(emailService).sendContractApprovalEmailToClient(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());

        when(contractRepository.save(any(Contract.class))).thenReturn(testContract);

        Contract result = contractService.approveContract(1L, approvalDTO);

        // Должен успешно завершиться даже при ошибке email
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("active");
    }

    @Test
    void approveContract_WithDTO_ShouldCreateCorrectSchedule() {
        ContractApprovalDTO approvalDTO = new ContractApprovalDTO();
        approvalDTO.setSecurityEmployeeId(1L);
        approvalDTO.setShiftStartTime(LocalTime.of(8, 0));
        approvalDTO.setShiftEndTime(LocalTime.of(20, 0));
        approvalDTO.setNotes("Особые указания");

        // Контракт на 5 дней
        testContract.setStartDate(LocalDate.now().plusDays(1));
        testContract.setEndDate(LocalDate.now().plusDays(5));
        testContract.setStatus("draft");

        when(contractRepository.findById(1L)).thenReturn(Optional.of(testContract));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(guardObjectRepository.findByContractId(1L)).thenReturn(List.of(testGuardObject));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());
        when(contractRepository.save(any(Contract.class))).thenReturn(testContract);
        doNothing().when(emailService).sendContractApprovalEmailToClient(any(), any(), any(), any(), any(), any(), any(), any(), any());
        doNothing().when(emailService).sendContractApprovalEmailToEmployee(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        // Act
        contractService.approveContract(1L, approvalDTO);

        // Assert - должно быть создано 5 расписаний
        verify(scheduleRepository, times(5)).save(any(Schedule.class));
    }
}