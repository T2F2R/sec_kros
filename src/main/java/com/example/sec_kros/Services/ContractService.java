package com.example.sec_kros.Services;

import com.example.sec_kros.DTO.ContractApprovalDTO;
import com.example.sec_kros.DTO.ContractDTO;
import com.example.sec_kros.DTO.ContractDeletionInfo;
import com.example.sec_kros.DTO.ValidationResult;
import com.example.sec_kros.Entities.*;
import com.example.sec_kros.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private GuardObjectRepository guardObjectRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService; // Добавляем EmailService

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract createContract(Long clientId, Long serviceId, Contract contract) {
        Optional<Client> client = clientRepository.findById(clientId);
        Optional<ServiceEntity> service = serviceRepository.findById(serviceId);

        if (client.isPresent() && service.isPresent()) {
            contract.setClient(client.get());
            contract.setService(service.get());
            contract.setCreatedAt(LocalDateTime.now());
            contract.setStatus("active");

            return contractRepository.save(contract);
        }
        return null;
    }

    public Contract updateContract(Long id, Contract contractDetails) {
        Optional<Contract> existingContract = contractRepository.findById(id);
        if (existingContract.isPresent()) {
            Contract contract = existingContract.get();
            contract.setStartDate(contractDetails.getStartDate());
            contract.setEndDate(contractDetails.getEndDate());
            contract.setTotalAmount(contractDetails.getTotalAmount());
            contract.setStatus(contractDetails.getStatus());

            return contractRepository.save(contract);
        }
        return null;
    }

    @Transactional
    public boolean deleteContract(Long id) {
        Optional<Contract> contractOpt = contractRepository.findById(id);
        if (contractOpt.isPresent()) {
            Contract contract = contractOpt.get();

            // Получаем все связанные объекты для информации
            List<GuardObject> guardObjects = guardObjectRepository.findByContractId(id);
            List<Schedule> schedules = new ArrayList<>();

            // Собираем все расписания связанные с объектами этого контракта
            for (GuardObject guardObject : guardObjects) {
                schedules.addAll(scheduleRepository.findByGuardObjectId(guardObject.getId()));
            }

            // Удаляем контракт (каскадно удалятся guardObjects, а через них и schedules)
            contractRepository.delete(contract);

            return true;
        }
        return false;
    }

    // Метод для получения информации о связанных данных перед удалением
    public ContractDeletionInfo getDeletionInfo(Long contractId) {
        Optional<Contract> contractOpt = contractRepository.findById(contractId);
        if (contractOpt.isEmpty()) {
            throw new RuntimeException("Контракт не найден");
        }

        Contract contract = contractOpt.get();
        List<GuardObject> guardObjects = guardObjectRepository.findByContractId(contractId);
        List<Schedule> allSchedules = new ArrayList<>();

        for (GuardObject guardObject : guardObjects) {
            allSchedules.addAll(scheduleRepository.findByGuardObjectId(guardObject.getId()));
        }

        ContractDeletionInfo info = new ContractDeletionInfo();
        info.setContract(contract);
        info.setGuardObjectsCount(guardObjects.size());
        info.setSchedulesCount(allSchedules.size());
        info.setGuardObjects(guardObjects);
        info.setSchedules(allSchedules);

        return info;
    }

    public List<Contract> getContractsByClientId(Long clientId) {
        return contractRepository.findByClientId(clientId);
    }

    public Contract createContract(ContractDTO contractDTO) {
        // Дополнительная проверка дат на сервере
        if (contractDTO.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Дата начала не может быть в прошлом");
        }
        if (contractDTO.getEndDate().isBefore(contractDTO.getStartDate().plusDays(1))) {
            throw new RuntimeException("Дата окончания должна быть после даты начала");
        }

        Client client = clientRepository.findById(contractDTO.getClientId())
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        ServiceEntity service = serviceRepository.findById(contractDTO.getServiceId())
                .orElseThrow(() -> new RuntimeException("Услуга не найдена"));

        Contract contract = new Contract();
        contract.setClient(client);
        contract.setService(service);
        contract.setStartDate(contractDTO.getStartDate());
        contract.setEndDate(contractDTO.getEndDate());
        contract.setTotalAmount(contractDTO.getTotalAmount() != null ?
                BigDecimal.valueOf(contractDTO.getTotalAmount()) : null);
        contract.setStatus(contractDTO.getStatus());
        contract.setCreatedAt(LocalDateTime.now());

        return contractRepository.save(contract);
    }

    public Contract updateContract(Long id, ContractDTO contractDTO) {
        return contractRepository.findById(id)
                .map(contract -> {
                    // Дополнительная проверка дат на сервере
                    if (contractDTO.getStartDate().isBefore(LocalDate.now())) {
                        throw new RuntimeException("Дата начала не может быть в прошлом");
                    }
                    if (contractDTO.getEndDate().isBefore(contractDTO.getStartDate().plusDays(1))) {
                        throw new RuntimeException("Дата окончания должна быть после даты начала");
                    }

                    Client client = clientRepository.findById(contractDTO.getClientId())
                            .orElseThrow(() -> new RuntimeException("Клиент не найден"));
                    ServiceEntity service = serviceRepository.findById(contractDTO.getServiceId())
                            .orElseThrow(() -> new RuntimeException("Услуга не найдена"));

                    contract.setClient(client);
                    contract.setService(service);
                    contract.setStartDate(contractDTO.getStartDate());
                    contract.setEndDate(contractDTO.getEndDate());
                    contract.setTotalAmount(contractDTO.getTotalAmount() != null ?
                            BigDecimal.valueOf(contractDTO.getTotalAmount()) : null);
                    contract.setStatus(contractDTO.getStatus());

                    return contractRepository.save(contract);
                })
                .orElse(null);
    }

    public List<Contract> getContractsByStatus(String status) {
        return contractRepository.findByStatus(status);
    }

    public Contract approveContract(Long id) {
        return contractRepository.findById(id)
                .map(contract -> {
                    contract.setStatus("active");
                    return contractRepository.save(contract);
                })
                .orElse(null);
    }

    public ValidationResult validateContractForApproval(Long contractId) {
        ValidationResult result = new ValidationResult();

        Optional<Contract> contractOpt = contractRepository.findById(contractId);
        if (contractOpt.isEmpty()) {
            result.setValid(false);
            result.setCanAutoCreate(false);
            result.addCheck(new ValidationResult.ValidationCheck(false, "Контракт не найден"));
            return result;
        }

        Contract contract = contractOpt.get();
        List<ValidationResult.ValidationCheck> checks = new ArrayList<>();

        // Только базовые проверки
        boolean hasClient = contract.getClient() != null;
        checks.add(new ValidationResult.ValidationCheck(hasClient, "Клиент привязан к контракту"));

        boolean hasService = contract.getService() != null;
        checks.add(new ValidationResult.ValidationCheck(hasService, "Услуга привязана к контракту"));

        boolean hasGuardObject = guardObjectRepository.existsByContractId(contractId);
        checks.add(new ValidationResult.ValidationCheck(hasGuardObject, "Охранный объект создан"));

        result.setChecks(checks);

        // Контракт может быть одобрен если есть клиент, услуга и охранный объект
        // Сотрудники проверяются в форме, а не здесь
        boolean canApprove = hasClient && hasService && hasGuardObject;
        result.setValid(canApprove);
        result.setCanAutoCreate(canApprove);

        return result;
    }

    @Transactional
    public Contract approveContract(Long contractId, ContractApprovalDTO approvalDTO) {
        return contractRepository.findById(contractId)
                .map(contract -> {
                    try {
                        // Базовые проверки
                        if (contract.getClient() == null) {
                            throw new RuntimeException("Клиент не привязан к контракту");
                        }
                        if (contract.getService() == null) {
                            throw new RuntimeException("Услуга не привязана к контракту");
                        }

                        // Проверка охранного объекта
                        List<GuardObject> guardObjects = guardObjectRepository.findByContractId(contract.getId());
                        if (guardObjects.isEmpty()) {
                            throw new RuntimeException("Не найден охранный объект для контракта. Сначала создайте охранный объект.");
                        }

                        // Проверка выбранного сотрудника (из формы)
                        if (approvalDTO.getSecurityEmployeeId() == null) {
                            throw new RuntimeException("Сотрудник охраны не выбран");
                        }

                        Employee employee = employeeRepository.findById(approvalDTO.getSecurityEmployeeId())
                                .orElseThrow(() -> new RuntimeException("Выбранный сотрудник не найден"));

                        // Проверка времени смены
                        if (approvalDTO.getShiftStartTime() == null) {
                            throw new RuntimeException("Время начала смены не указано");
                        }
                        if (approvalDTO.getShiftEndTime() == null) {
                            throw new RuntimeException("Время окончания смены не указано");
                        }

                        // Создание расписания охраны
                        createSecuritySchedule(contract, approvalDTO);

                        // Создание уведомлений
                        createApprovalNotifications(contract, approvalDTO);

                        // Отправка email уведомлений
                        sendApprovalEmails(contract, approvalDTO, guardObjects.get(0));

                        // Обновление статуса контракта
                        contract.setStatus("active");
                        Contract savedContract = contractRepository.save(contract);

                        return savedContract;

                    } catch (Exception e) {
                        throw new RuntimeException("Ошибка при одобрении контракта: " + e.getMessage());
                    }
                })
                .orElseThrow(() -> new RuntimeException("Контракт не найден"));
    }

    private void createSecuritySchedule(Contract contract, ContractApprovalDTO approvalDTO) {
        Employee employee = employeeRepository.findById(approvalDTO.getSecurityEmployeeId())
                .orElseThrow(() -> new RuntimeException("Выбранный сотрудник не найден"));

        // Ищем существующий охранный объект
        List<GuardObject> guardObjects = guardObjectRepository.findByContractId(contract.getId());
        GuardObject guardObject;

        if (guardObjects.isEmpty()) {
            throw new RuntimeException("Не найден охранный объект для контракта. Сначала создайте охранный объект.");
        } else {
            guardObject = guardObjects.get(0);
        }

        // Используем дату начала контракта как дату начала охраны
        LocalDate startDate = contract.getStartDate();

        // Создаем расписание на первую неделю (7 дней)
        for (int i = 0; i < 7; i++) {
            LocalDate scheduleDate = startDate.plusDays(i);

            // Проверяем, не выходит ли дата за пределы контракта
            if (scheduleDate.isAfter(contract.getEndDate())) {
                break;
            }

            Schedule schedule = new Schedule();
            schedule.setEmployee(employee);
            schedule.setGuardObject(guardObject);
            schedule.setDate(scheduleDate);
            schedule.setStartTime(approvalDTO.getShiftStartTime());
            schedule.setEndTime(approvalDTO.getShiftEndTime());
            schedule.setNotes("Охрана по контракту №" + contract.getId() +
                    (approvalDTO.getNotes() != null ? ". " + approvalDTO.getNotes() : ""));

            scheduleRepository.save(schedule);
        }
    }

    private void createApprovalNotifications(Contract contract, ContractApprovalDTO approvalDTO) {
        // Уведомление для клиента
        Notification clientNotification = new Notification();
        clientNotification.setClient(contract.getClient());
        clientNotification.setTitle("Контракт одобрен");
        clientNotification.setMessage("Ваш контракт №" + contract.getId() + " на услугу '" +
                contract.getService().getName() + "' был одобрен. Охрана начинается с " +
                contract.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        notificationRepository.save(clientNotification);

        // Уведомление для сотрудника
        Employee employee = employeeRepository.findById(approvalDTO.getSecurityEmployeeId()).orElse(null);
        if (employee != null) {
            Notification employeeNotification = new Notification();
            employeeNotification.setEmployee(employee);
            employeeNotification.setTitle("Новое задание охраны");
            employeeNotification.setMessage("Вам назначена охрана объекта по контракту №" + contract.getId() +
                    ". Начало: " + contract.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                    ". Время смены: " + approvalDTO.getShiftStartTime() + " - " + approvalDTO.getShiftEndTime());
            notificationRepository.save(employeeNotification);
        }
    }

    private void sendApprovalEmails(Contract contract, ContractApprovalDTO approvalDTO, GuardObject guardObject) {
        try {
            // Получаем данные для email
            Client client = contract.getClient();
            Employee employee = employeeRepository.findById(approvalDTO.getSecurityEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

            // Форматируем даты для читаемого отображения
            String startDateFormatted = contract.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String endDateFormatted = contract.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            String shiftTimeFormatted = approvalDTO.getShiftStartTime() + " - " + approvalDTO.getShiftEndTime();

            // Отправляем email клиенту
            emailService.sendContractApprovalEmailToClient(
                    client.getEmail(),
                    client.getFirstName() + " " + client.getLastName(),
                    contract.getId().toString(),
                    startDateFormatted,
                    endDateFormatted,
                    guardObject.getName(),
                    guardObject.getAddress(),
                    employee.getFirstName() + " " + employee.getLastName(),
                    shiftTimeFormatted
            );

            // Отправляем email сотруднику
            emailService.sendContractApprovalEmailToEmployee(
                    employee.getEmail(),
                    employee.getFirstName() + " " + employee.getLastName(),
                    contract.getId().toString(),
                    client.getFirstName() + " " + client.getLastName(),
                    guardObject.getName(),
                    guardObject.getAddress(),
                    shiftTimeFormatted,
                    startDateFormatted,
                    endDateFormatted,
                    approvalDTO.getNotes() != null ? approvalDTO.getNotes() : "Дополнительные указания отсутствуют"
            );

        } catch (Exception e) {
            // Логируем ошибку, но не прерываем выполнение
            System.err.println("Ошибка при отправке email уведомлений: " + e.getMessage());
            // Можно добавить логирование в файл или использовать Logger
        }
    }
}