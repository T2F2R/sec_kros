package com.example.sec_kros.Controllers;

import com.example.sec_kros.Entities.*;
import com.example.sec_kros.DTO.*;
import com.example.sec_kros.Services.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private GuardObjectService guardObjectService;

    @Autowired
    private ServiceService serviceService;

    // ==================== ДАШБОРД ====================

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        long clientsCount = clientService.getAllClients().size();
        long employeesCount = employeeService.getAllEmployees().size();
        long contractsCount = contractService.getAllContracts().size();
        long objectsCount = guardObjectService.getAllGuardObjects().size();
        long schedulesCount = scheduleService.getAllSchedules().size();

        // Получаем контракты на одобрение (со статусом inactive)
        List<Contract> pendingContracts = contractService.getContractsByStatus("inactive");

        model.addAttribute("clientsCount", clientsCount);
        model.addAttribute("employeesCount", employeesCount);
        model.addAttribute("contractsCount", contractsCount);
        model.addAttribute("objectsCount", objectsCount);
        model.addAttribute("schedulesCount", schedulesCount);
        model.addAttribute("pendingContracts", pendingContracts);

        return "admin/dashboard";
    }

// ==================== ОДОБРЕНИЕ КОНТРАКТОВ ====================

    @GetMapping("/contracts/approve/{id}")
    public String showApproveContractForm(@PathVariable Long id, Model model) {
        Optional<Contract> contract = contractService.getContractById(id);
        if (contract.isPresent() && "inactive".equals(contract.get().getStatus())) {
            // Получаем всех сотрудников для выбора
            List<Employee> securityEmployees = employeeService.getSecurityEmployees();

            // Проверяем готовность контракта
            ValidationResult validationResult = contractService.validateContractForApproval(id);

            model.addAttribute("contract", contract.get());
            model.addAttribute("securityEmployees", securityEmployees);
            model.addAttribute("approvalDTO", new ContractApprovalDTO());
            model.addAttribute("validationResult", validationResult);

            return "admin/contracts/approve";
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/contracts/approve/{id}")
    public String approveContract(@PathVariable Long id,
                                  @Valid @ModelAttribute("approvalDTO") ContractApprovalDTO approvalDTO,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {

        // Получаем контракт для отображения формы
        Optional<Contract> contract = contractService.getContractById(id);
        if (contract.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Контракт не найден");
            return "redirect:/admin/dashboard";
        }

        if (bindingResult.hasErrors()) {
            // Возвращаем форму с ошибками
            List<Employee> securityEmployees = employeeService.getSecurityEmployees();
            ValidationResult validationResult = contractService.validateContractForApproval(id);

            model.addAttribute("contract", contract.get());
            model.addAttribute("securityEmployees", securityEmployees);
            model.addAttribute("validationResult", validationResult);

            return "admin/contracts/approve";
        }

        try {
            Contract approved = contractService.approveContract(id, approvalDTO);
            if (approved != null) {
                redirectAttributes.addFlashAttribute("success", "Контракт успешно одобрен и расписание создано");
            } else {
                redirectAttributes.addFlashAttribute("error", "Контракт не найден");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            // При ошибке также возвращаем форму с данными
            List<Employee> securityEmployees = employeeService.getSecurityEmployees();
            ValidationResult validationResult = contractService.validateContractForApproval(id);

            model.addAttribute("contract", contract.get());
            model.addAttribute("securityEmployees", securityEmployees);
            model.addAttribute("validationResult", validationResult);
            model.addAttribute("approvalDTO", approvalDTO);

            return "admin/contracts/approve";
        }

        return "redirect:/admin/dashboard";
    }

    // ==================== КЛИЕНТЫ ====================

    @GetMapping("/clients")
    public String clientsList(Model model) {
        List<Client> clients = clientService.getAllClients();
        model.addAttribute("clients", clients);
        return "admin/clients/list";
    }

    @GetMapping("/clients/create")
    public String showCreateClientForm(Model model) {
        model.addAttribute("clientDTO", new ClientDTO());
        return "admin/clients/create";
    }

    @PostMapping("/clients/create")
    public String createClient(@Valid @ModelAttribute ClientDTO clientDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/clients/create";
        }

        if (clientService.existsByEmail(clientDTO.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Клиент с таким email уже существует");
            return "redirect:/admin/clients/create";
        }

        clientService.createClient(clientDTO);
        redirectAttributes.addFlashAttribute("success", "Клиент успешно создан");
        return "redirect:/admin/clients";
    }

    @GetMapping("/clients/edit/{id}")
    public String showEditClientForm(@PathVariable Long id, Model model) {
        Optional<Client> client = clientService.getClientById(id);
        if (client.isPresent()) {
            Client c = client.get();
            ClientDTO clientDTO = new ClientDTO();
            clientDTO.setId(c.getId());
            clientDTO.setLastName(c.getLastName());
            clientDTO.setFirstName(c.getFirstName());
            clientDTO.setPatronymic(c.getPatronymic());
            clientDTO.setPhone(c.getPhone());
            clientDTO.setEmail(c.getEmail());
            clientDTO.setAddress(c.getAddress());

            model.addAttribute("clientDTO", clientDTO);
            return "admin/clients/edit";
        }
        return "redirect:/admin/clients";
    }

    @PostMapping("/clients/edit/{id}")
    public String updateClient(@PathVariable Long id,
                               @Valid @ModelAttribute ClientDTO clientDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/clients/edit";
        }

        Client updated = clientService.updateClient(id, clientDTO);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("success", "Клиент успешно обновлен");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении клиента");
        }
        return "redirect:/admin/clients";
    }

    @PostMapping("/clients/delete/{id}")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (clientService.deleteClient(id)) {
            redirectAttributes.addFlashAttribute("success", "Клиент успешно удален");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении клиента");
        }
        return "redirect:/admin/clients";
    }

    // ==================== СОТРУДНИКИ ====================

    @GetMapping("/employees")
    public String employeesList(Model model) {
        List<Employee> employees = employeeService.getAllEmployees();
        model.addAttribute("employees", employees);
        return "admin/employees/list";
    }

    @GetMapping("/employees/create")
    public String showCreateEmployeeForm(Model model) {
        model.addAttribute("employeeDTO", new EmployeeDTO());
        return "admin/employees/create";
    }

    @PostMapping("/employees/create")
    public String createEmployee(@Valid @ModelAttribute EmployeeDTO employeeDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/employees/create";
        }

        if (employeeService.existsByEmail(employeeDTO.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Сотрудник с таким email уже существует");
            return "redirect:/admin/employees/create";
        }

        if (employeeService.existsByLogin(employeeDTO.getLogin())) {
            redirectAttributes.addFlashAttribute("error", "Сотрудник с таким логином уже существует");
            return "redirect:/admin/employees/create";
        }

        employeeService.createEmployee(employeeDTO);
        redirectAttributes.addFlashAttribute("success", "Сотрудник успешно создан");
        return "redirect:/admin/employees";
    }

    @GetMapping("/employees/edit/{id}")
    public String showEditEmployeeForm(@PathVariable Long id, Model model) {
        Optional<Employee> employee = employeeService.getEmployeeById(id);
        if (employee.isPresent()) {
            Employee e = employee.get();
            EmployeeDTO employeeDTO = new EmployeeDTO();
            employeeDTO.setId(e.getId());
            employeeDTO.setLastName(e.getLastName());
            employeeDTO.setFirstName(e.getFirstName());
            employeeDTO.setPatronymic(e.getPatronymic());
            employeeDTO.setPassportSeries(e.getPassportSeries());
            employeeDTO.setPassportNumber(e.getPassportNumber());
            employeeDTO.setPhone(e.getPhone());
            employeeDTO.setEmail(e.getEmail());
            employeeDTO.setLogin(e.getLogin());
            employeeDTO.setPosition(e.getPosition());
            employeeDTO.setIsAdmin(e.getIsAdmin());

            model.addAttribute("employeeDTO", employeeDTO);
            return "admin/employees/edit";
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/edit/{id}")
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute EmployeeDTO employeeDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/employees/edit";
        }

        Employee updated = employeeService.updateEmployee(id, employeeDTO);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("success", "Сотрудник успешно обновлен");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении сотрудника");
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (employeeService.deleteEmployee(id)) {
            redirectAttributes.addFlashAttribute("success", "Сотрудник успешно удален");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении сотрудника");
        }
        return "redirect:/admin/employees";
    }

    // ==================== ДОГОВОРЫ ====================

    @GetMapping("/contracts")
    public String contractsList(Model model) {
        List<Contract> contracts = contractService.getAllContracts();
        model.addAttribute("contracts", contracts);
        return "admin/contracts/list";
    }

    @GetMapping("/contracts/create")
    public String showCreateContractForm(Model model) {
        List<Client> clients = clientService.getAllClients();
        List<ServiceEntity> services = serviceService.getAllServices();

        model.addAttribute("contractDTO", new ContractDTO());
        model.addAttribute("clients", clients);
        model.addAttribute("services", services);
        return "admin/contracts/create";
    }

    @PostMapping("/contracts/create")
    public String createContract(@Valid @ModelAttribute ContractDTO contractDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (bindingResult.hasErrors()) {
            // Добавляем списки обратно в модель для отображения формы с ошибками
            List<Client> clients = clientService.getAllClients();
            List<ServiceEntity> services = serviceService.getAllServices();
            model.addAttribute("clients", clients);
            model.addAttribute("services", services);
            return "admin/contracts/create";
        }

        Contract created = contractService.createContract(contractDTO);
        if (created != null) {
            redirectAttributes.addFlashAttribute("success", "Договор успешно создан");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании договора");
        }
        return "redirect:/admin/contracts";
    }

    @GetMapping("/contracts/edit/{id}")
    public String showEditContractForm(@PathVariable Long id, Model model) {
        Optional<Contract> contract = contractService.getContractById(id);
        if (contract.isPresent()) {
            Contract c = contract.get();
            ContractDTO contractDTO = new ContractDTO();
            contractDTO.setId(c.getId());
            contractDTO.setClientId(c.getClient().getId());
            contractDTO.setServiceId(c.getService().getId());
            contractDTO.setStartDate(c.getStartDate());
            contractDTO.setEndDate(c.getEndDate());
            contractDTO.setTotalAmount(c.getTotalAmount() != null ? c.getTotalAmount().doubleValue() : null);
            contractDTO.setStatus(c.getStatus());

            List<Client> clients = clientService.getAllClients();
            List<ServiceEntity> services = serviceService.getAllServices();

            model.addAttribute("contractDTO", contractDTO);
            model.addAttribute("clients", clients);
            model.addAttribute("services", services);
            return "admin/contracts/edit";
        }
        return "redirect:/admin/contracts";
    }

    @PostMapping("/contracts/edit/{id}")
    public String updateContract(@PathVariable Long id,
                                 @Valid @ModelAttribute ContractDTO contractDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (bindingResult.hasErrors()) {
            // Добавляем списки обратно в модель для отображения формы с ошибками
            List<Client> clients = clientService.getAllClients();
            List<ServiceEntity> services = serviceService.getAllServices();
            model.addAttribute("clients", clients);
            model.addAttribute("services", services);
            return "admin/contracts/edit";
        }

        Contract updated = contractService.updateContract(id, contractDTO);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("success", "Договор успешно обновлен");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении договора");
        }
        return "redirect:/admin/contracts";
    }

    @GetMapping("/contracts/delete/{id}")
    public String showDeleteContractConfirmation(@PathVariable Long id, Model model) {
        try {
            ContractDeletionInfo deletionInfo = contractService.getDeletionInfo(id);
            model.addAttribute("deletionInfo", deletionInfo);
            return "admin/contracts/delete-confirm";
        } catch (RuntimeException e) {
            return "redirect:/admin/contracts";
        }
    }

    @PostMapping("/contracts/delete/{id}")
    public String deleteContract(@PathVariable Long id,
                                 @RequestParam(defaultValue = "false") boolean confirm,
                                 RedirectAttributes redirectAttributes) {
        if (!confirm) {
            redirectAttributes.addFlashAttribute("error", "Удаление отменено. Подтвердите удаление.");
            return "redirect:/admin/contracts/delete/" + id;
        }

        try {
            if (contractService.deleteContract(id)) {
                redirectAttributes.addFlashAttribute("success", "Контракт и все связанные данные успешно удалены");
            } else {
                redirectAttributes.addFlashAttribute("error", "Контракт не найден");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении контракта: " + e.getMessage());
        }

        return "redirect:/admin/contracts";
    }


    // ==================== ОХРАНЯЕМЫЕ ОБЪЕКТЫ ====================

    @GetMapping("/objects")
    public String objectsList(Model model) {
        List<GuardObject> objects = guardObjectService.getAllGuardObjects();
        model.addAttribute("objects", objects);

        // Создаем Map с информацией о возможности удаления для каждого объекта
        Map<Long, Boolean> canDelete = new HashMap<>();
        for (GuardObject object : objects) {
            canDelete.put(object.getId(), guardObjectService.canDeleteGuardObject(object.getId()));
        }
        model.addAttribute("canDelete", canDelete);

        return "admin/objects/list";
    }

    @GetMapping("/objects/create")
    public String showCreateObjectForm(Model model) {
        List<Client> clients = clientService.getAllClients();
        List<Contract> contracts = contractService.getAllContracts();

        model.addAttribute("guardObjectDTO", new GuardObjectDTO());
        model.addAttribute("clients", clients);
        model.addAttribute("contracts", contracts);
        return "admin/objects/create";
    }

    @PostMapping("/objects/create")
    public String createObject(@Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (bindingResult.hasErrors()) {
            // Добавляем списки обратно в модель для отображения формы с ошибками
            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/create";
        }

        try {
            GuardObject created = guardObjectService.createGuardObject(guardObjectDTO);
            if (created != null) {
                redirectAttributes.addFlashAttribute("success", "Объект успешно создан");
            } else {
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании объекта");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            // Возвращаем списки для повторного отображения формы
            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/create";
        }

        return "redirect:/admin/objects";
    }

    @GetMapping("/objects/edit/{id}")
    public String showEditObjectForm(@PathVariable Long id, Model model) {
        Optional<GuardObject> guardObject = guardObjectService.getGuardObjectById(id);
        if (guardObject.isPresent()) {
            GuardObject obj = guardObject.get();
            GuardObjectDTO guardObjectDTO = new GuardObjectDTO();
            guardObjectDTO.setId(obj.getId());
            guardObjectDTO.setClientId(obj.getClient().getId());
            guardObjectDTO.setContractId(obj.getContract().getId());
            guardObjectDTO.setName(obj.getName());
            guardObjectDTO.setAddress(obj.getAddress());
            guardObjectDTO.setLatitude(obj.getLatitude());
            guardObjectDTO.setLongitude(obj.getLongitude());
            guardObjectDTO.setDescription(obj.getDescription());

            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();

            model.addAttribute("guardObjectDTO", guardObjectDTO);
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/edit";
        }
        return "redirect:/admin/objects";
    }

    @PostMapping("/objects/edit/{id}")
    public String updateObject(@PathVariable Long id,
                               @Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        if (bindingResult.hasErrors()) {
            // Добавляем списки обратно в модель для отображения формы с ошибками
            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/edit";
        }

        try {
            GuardObject updated = guardObjectService.updateGuardObject(id, guardObjectDTO);
            if (updated != null) {
                redirectAttributes.addFlashAttribute("success", "Объект успешно обновлен");
            } else {
                redirectAttributes.addFlashAttribute("error", "Объект не найден");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            // Возвращаем списки для повторного отображения формы
            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/edit";
        }

        return "redirect:/admin/objects";
    }

    @PostMapping("/objects/delete/{id}")
    public String deleteObject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Проверяем, можно ли удалить объект
        boolean canDelete = guardObjectService.canDeleteGuardObject(id);

        if (!canDelete) {
            redirectAttributes.addFlashAttribute("error",
                    "Невозможно удалить объект: существуют связанные контракты или расписания");
            return "redirect:/admin/objects";
        }

        if (guardObjectService.deleteGuardObject(id)) {
            redirectAttributes.addFlashAttribute("success", "Объект успешно удален");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении объекта");
        }
        return "redirect:/admin/objects";
    }

    // ==================== УСЛУГИ ====================

    @GetMapping("/services")
    public String servicesList(Model model) {
        List<ServiceEntity> services = serviceService.getAllServices();
        model.addAttribute("services", services);
        return "admin/services/list";
    }

    @GetMapping("/services/create")
    public String showCreateServiceForm(Model model) {
        model.addAttribute("serviceDTO", new ServiceDTO());
        return "admin/services/create";
    }

    @PostMapping("/services/create")
    public String createService(@Valid @ModelAttribute ServiceDTO serviceDTO,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/services/create";
        }

        try {
            // Проверка уникальности названия услуги
            if (serviceService.existsByName(serviceDTO.getName())) {
                redirectAttributes.addFlashAttribute("error", "Услуга с таким названием уже существует");
                return "redirect:/admin/services/create";
            }

            serviceService.createService(serviceDTO);
            redirectAttributes.addFlashAttribute("success", "Услуга успешно создана");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/services/create";
        }

        return "redirect:/admin/services";
    }

    @GetMapping("/services/edit/{id}")
    public String showEditServiceForm(@PathVariable Long id, Model model) {
        Optional<ServiceEntity> service = serviceService.getServiceById(id);
        if (service.isPresent()) {
            ServiceEntity s = service.get();
            ServiceDTO serviceDTO = new ServiceDTO();
            serviceDTO.setId(s.getId());
            serviceDTO.setName(s.getName());
            serviceDTO.setDescription(s.getDescription());
            serviceDTO.setPrice(s.getPrice());

            model.addAttribute("serviceDTO", serviceDTO);
            return "admin/services/edit";
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/services/edit/{id}")
    public String updateService(@PathVariable Long id,
                                @Valid @ModelAttribute ServiceDTO serviceDTO,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/services/edit";
        }

        try {
            // Проверка уникальности названия услуги (исключая текущую услугу)
            Optional<ServiceEntity> existingService = serviceService.getServiceById(id);
            if (existingService.isPresent()) {
                ServiceEntity currentService = existingService.get();
                if (!currentService.getName().equals(serviceDTO.getName()) &&
                        serviceService.existsByName(serviceDTO.getName())) {
                    redirectAttributes.addFlashAttribute("error", "Услуга с таким названием уже существует");
                    return "redirect:/admin/services/edit/" + id;
                }
            }

            ServiceEntity updated = serviceService.updateService(id, serviceDTO);
            if (updated != null) {
                redirectAttributes.addFlashAttribute("success", "Услуга успешно обновлена");
            } else {
                redirectAttributes.addFlashAttribute("error", "Услуга не найдена");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/services/edit/" + id;
        }

        return "redirect:/admin/services";
    }

    @PostMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (serviceService.deleteService(id)) {
                redirectAttributes.addFlashAttribute("success", "Услуга успешно удалена");
            } else {
                redirectAttributes.addFlashAttribute("error", "Услуга не найдена");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении услуги: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @Autowired
    private ScheduleService scheduleService;

// ==================== ГРАФИКИ РАБОТЫ ====================

    @GetMapping("/schedules")
    public String schedulesList(Model model) {
        List<Schedule> schedules = scheduleService.getAllSchedules();
        model.addAttribute("schedules", schedules);
        return "admin/schedules/list";
    }

    @GetMapping("/schedules/create")
    public String showCreateScheduleForm(Model model) {
        List<Employee> employees = employeeService.getAllEmployees();
        List<GuardObject> guardObjects = guardObjectService.getAllGuardObjects();

        model.addAttribute("scheduleDTO", new ScheduleDTO());
        model.addAttribute("employees", employees);
        model.addAttribute("guardObjects", guardObjects);
        return "admin/schedules/create";
    }

    @PostMapping("/schedules/create")
    public String createSchedule(@Valid @ModelAttribute ScheduleDTO scheduleDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (bindingResult.hasErrors()) {
            // Добавляем списки обратно в модель для отображения формы с ошибками
            List<Employee> employees = employeeService.getAllEmployees();
            List<GuardObject> guardObjects = guardObjectService.getAllGuardObjects();
            model.addAttribute("employees", employees);
            model.addAttribute("guardObjects", guardObjects);
            return "admin/schedules/create";
        }

        try {
            Schedule created = scheduleService.createSchedule(scheduleDTO);
            if (created != null) {
                redirectAttributes.addFlashAttribute("success", "График работы успешно создан");
            } else {
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании графика работы");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/schedules/create";
        }

        return "redirect:/admin/schedules";
    }

    @GetMapping("/schedules/edit/{id}")
    public String showEditScheduleForm(@PathVariable Long id, Model model) {
        Optional<Schedule> schedule = scheduleService.getScheduleById(id);
        if (schedule.isPresent()) {
            Schedule s = schedule.get();
            ScheduleDTO scheduleDTO = new ScheduleDTO();
            scheduleDTO.setId(s.getId());
            scheduleDTO.setEmployeeId(s.getEmployee().getId());
            scheduleDTO.setGuardObjectId(s.getGuardObject().getId());
            scheduleDTO.setDate(s.getDate());
            scheduleDTO.setStartTime(s.getStartTime());
            scheduleDTO.setEndTime(s.getEndTime());
            scheduleDTO.setNotes(s.getNotes());

            List<Employee> employees = employeeService.getAllEmployees();
            List<GuardObject> guardObjects = guardObjectService.getAllGuardObjects();

            model.addAttribute("scheduleDTO", scheduleDTO);
            model.addAttribute("employees", employees);
            model.addAttribute("guardObjects", guardObjects);
            return "admin/schedules/edit";
        }
        return "redirect:/admin/schedules";
    }

    @PostMapping("/schedules/edit/{id}")
    public String updateSchedule(@PathVariable Long id,
                                 @Valid @ModelAttribute ScheduleDTO scheduleDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        if (bindingResult.hasErrors()) {
            // Добавляем списки обратно в модель для отображения формы с ошибками
            List<Employee> employees = employeeService.getAllEmployees();
            List<GuardObject> guardObjects = guardObjectService.getAllGuardObjects();
            model.addAttribute("employees", employees);
            model.addAttribute("guardObjects", guardObjects);
            return "admin/schedules/edit";
        }

        try {
            Schedule updated = scheduleService.updateSchedule(id, scheduleDTO);
            if (updated != null) {
                redirectAttributes.addFlashAttribute("success", "График работы успешно обновлен");
            } else {
                redirectAttributes.addFlashAttribute("error", "График работы не найден");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/schedules/edit/" + id;
        }

        return "redirect:/admin/schedules";
    }

    @PostMapping("/schedules/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (scheduleService.deleteSchedule(id)) {
            redirectAttributes.addFlashAttribute("success", "График работы успешно удален");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении графика работы");
        }
        return "redirect:/admin/schedules";
    }

    @Autowired
    private ReportService reportService;

    // ==================== ОТЧЕТЫ ====================

    @PostMapping("/reports/revenue")
    public ResponseEntity<byte[]> generateRevenueReport(@RequestParam LocalDate startDate,
                                                        @RequestParam LocalDate endDate) {
        try {
            byte[] excelBytes = reportService.generateRevenueReport(startDate, endDate);

            String filename = String.format("revenue_report_%s_%s.xlsx",
                    startDate, endDate);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/reports/contracts")
    public ResponseEntity<byte[]> generateContractsReport(@RequestParam LocalDate startDate,
                                                          @RequestParam LocalDate endDate) {
        try {
            byte[] excelBytes = reportService.generateContractsReport(startDate, endDate);

            String filename = String.format("contracts_report_%s_%s.xlsx",
                    startDate, endDate);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/reports/clients")
    public ResponseEntity<byte[]> generateClientsReport(@RequestParam LocalDate startDate,
                                                        @RequestParam LocalDate endDate) {
        try {
            byte[] excelBytes = reportService.generateClientsReport(startDate, endDate);

            String filename = String.format("clients_report_%s_%s.xlsx",
                    startDate, endDate);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}