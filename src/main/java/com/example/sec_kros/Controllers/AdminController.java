package com.example.sec_kros.Controllers;

import com.example.sec_kros.Entities.*;
import com.example.sec_kros.DTO.*;
import com.example.sec_kros.Services.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

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

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ReportService reportService;

    private Employee getCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return employeeService.findByEmail(username);
    }

    // ==================== ДАШБОРД ====================

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        logger.info("Admin dashboard accessed");

        Employee employee = getCurrentEmployee();
        if (employee == null) {
            logger.warn("Attempt to access dashboard without authentication");
            return "redirect:/login";
        }

        try {
            long clientsCount = clientService.getAllClients().size();
            long employeesCount = employeeService.getAllEmployees().size();
            long contractsCount = contractService.getAllContracts().size();
            long objectsCount = guardObjectService.getAllGuardObjects().size();
            long schedulesCount = scheduleService.getAllSchedules().size();

            List<Contract> pendingContracts = contractService.getContractsByStatus("inactive");

            model.addAttribute("employee", employee);
            model.addAttribute("clientsCount", clientsCount);
            model.addAttribute("employeesCount", employeesCount);
            model.addAttribute("contractsCount", contractsCount);
            model.addAttribute("objectsCount", objectsCount);
            model.addAttribute("schedulesCount", schedulesCount);
            model.addAttribute("pendingContracts", pendingContracts);

            logger.info("Dashboard loaded successfully for admin: {}", employee.getEmail());
            return "admin/dashboard";

        } catch (Exception e) {
            logger.error("Error loading admin dashboard for user: {}", employee.getEmail(), e);
            model.addAttribute("error", "Произошла ошибка при загрузке данных");
            return "admin/dashboard";
        }
    }

    // ==================== ОДОБРЕНИЕ КОНТРАКТОВ ====================

    @GetMapping("/contracts/approve/{id}")
    public String showApproveContractForm(@PathVariable Long id, Model model) {
        logger.info("Showing approve contract form for contract ID: {}", id);

        try {
            Optional<Contract> contract = contractService.getContractById(id);
            if (contract.isPresent() && "inactive".equals(contract.get().getStatus())) {
                List<Employee> securityEmployees = employeeService.getSecurityEmployees();
                ValidationResult validationResult = contractService.validateContractForApproval(id);

                model.addAttribute("contract", contract.get());
                model.addAttribute("securityEmployees", securityEmployees);
                model.addAttribute("approvalDTO", new ContractApprovalDTO());
                model.addAttribute("validationResult", validationResult);

                logger.info("Approve contract form loaded successfully for contract ID: {}", id);
                return "admin/contracts/approve";
            } else {
                logger.warn("Contract not found or not inactive: {}", id);
                return "redirect:/admin/dashboard";
            }
        } catch (Exception e) {
            logger.error("Error loading approve contract form for contract ID: {}", id, e);
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/contracts/approve/{id}")
    public String approveContract(@PathVariable Long id,
                                  @Valid @ModelAttribute("approvalDTO") ContractApprovalDTO approvalDTO,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to approve contract ID: {}",
                currentEmployee.getEmail(), id);

        Optional<Contract> contract = contractService.getContractById(id);
        if (contract.isEmpty()) {
            logger.warn("Contract not found for approval: {}", id);
            redirectAttributes.addFlashAttribute("error", "Контракт не найден");
            return "redirect:/admin/dashboard";
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in contract approval for contract ID: {}", id);
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
                logger.info("Contract ID: {} approved successfully by admin: {}",
                        id, currentEmployee.getEmail());
                redirectAttributes.addFlashAttribute("success", "Контракт успешно одобрен и расписание создано");
            } else {
                logger.error("Failed to approve contract ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Контракт не найден");
            }
        } catch (RuntimeException e) {
            logger.error("Error approving contract ID: {} by admin: {}",
                    id, currentEmployee.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());

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
        logger.info("Accessing clients list");

        try {
            List<Client> clients = clientService.getAllClients();
            model.addAttribute("clients", clients);
            logger.info("Loaded {} clients", clients.size());
            return "admin/clients/list";
        } catch (Exception e) {
            logger.error("Error loading clients list", e);
            model.addAttribute("error", "Ошибка при загрузке списка клиентов");
            return "admin/clients/list";
        }
    }

    @GetMapping("/clients/create")
    public String showCreateClientForm(Model model) {
        logger.info("Showing create client form");
        model.addAttribute("clientDTO", new ClientDTO());
        return "admin/clients/create";
    }

    @PostMapping("/clients/create")
    public String createClient(@Valid @ModelAttribute ClientDTO clientDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to create client: {}",
                currentEmployee.getEmail(), clientDTO.getEmail());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in client creation for email: {}", clientDTO.getEmail());
            return "admin/clients/create";
        }

        if (clientService.existsByEmail(clientDTO.getEmail())) {
            logger.warn("Client with email already exists: {}", clientDTO.getEmail());
            redirectAttributes.addFlashAttribute("error", "Клиент с таким email уже существует");
            return "redirect:/admin/clients/create";
        }

        try {
            clientService.createClient(clientDTO);
            logger.info("Client created successfully: {}", clientDTO.getEmail());
            redirectAttributes.addFlashAttribute("success", "Клиент успешно создан");
        } catch (Exception e) {
            logger.error("Error creating client: {}", clientDTO.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании клиента");
        }

        return "redirect:/admin/clients";
    }

    @GetMapping("/clients/edit/{id}")
    public String showEditClientForm(@PathVariable Long id, Model model) {
        logger.info("Showing edit client form for ID: {}", id);

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
            logger.info("Edit client form loaded for ID: {}", id);
            return "admin/clients/edit";
        }

        logger.warn("Client not found for editing: {}", id);
        return "redirect:/admin/clients";
    }

    @PostMapping("/clients/edit/{id}")
    public String updateClient(@PathVariable Long id,
                               @Valid @ModelAttribute ClientDTO clientDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to update client ID: {}",
                currentEmployee.getEmail(), id);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in client update for ID: {}", id);
            return "admin/clients/edit";
        }

        try {
            Client updated = clientService.updateClient(id, clientDTO);
            if (updated != null) {
                logger.info("Client ID: {} updated successfully", id);
                redirectAttributes.addFlashAttribute("success", "Клиент успешно обновлен");
            } else {
                logger.error("Failed to update client ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении клиента");
            }
        } catch (Exception e) {
            logger.error("Error updating client ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении клиента");
        }

        return "redirect:/admin/clients";
    }

    @PostMapping("/clients/delete/{id}")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to delete client ID: {}",
                currentEmployee.getEmail(), id);

        try {
            if (clientService.deleteClient(id)) {
                logger.info("Client ID: {} deleted successfully", id);
                redirectAttributes.addFlashAttribute("success", "Клиент успешно удален");
            } else {
                logger.warn("Failed to delete client ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении клиента");
            }
        } catch (Exception e) {
            logger.error("Error deleting client ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении клиента");
        }

        return "redirect:/admin/clients";
    }

    // ==================== СОТРУДНИКИ ====================

    @GetMapping("/employees")
    public String employeesList(Model model) {
        logger.info("Accessing employees list");

        try {
            List<Employee> employees = employeeService.getAllEmployees();
            model.addAttribute("employees", employees);
            logger.info("Loaded {} employees", employees.size());
            return "admin/employees/list";
        } catch (Exception e) {
            logger.error("Error loading employees list", e);
            model.addAttribute("error", "Ошибка при загрузке списка сотрудников");
            return "admin/employees/list";
        }
    }

    @PostMapping("/employees/create")
    public String createEmployee(@Valid @ModelAttribute EmployeeDTO employeeDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to create employee: {}",
                currentEmployee.getEmail(), employeeDTO.getEmail());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in employee creation for email: {}", employeeDTO.getEmail());
            return "admin/employees/create";
        }

        if (employeeService.existsByEmail(employeeDTO.getEmail())) {
            logger.warn("Employee with email already exists: {}", employeeDTO.getEmail());
            redirectAttributes.addFlashAttribute("error", "Сотрудник с таким email уже существует");
            return "redirect:/admin/employees/create";
        }

        if (employeeService.existsByLogin(employeeDTO.getLogin())) {
            logger.warn("Employee with login already exists: {}", employeeDTO.getLogin());
            redirectAttributes.addFlashAttribute("error", "Сотрудник с таким логином уже существует");
            return "redirect:/admin/employees/create";
        }

        try {
            employeeService.createEmployee(employeeDTO);
            logger.info("Employee created successfully: {}", employeeDTO.getEmail());
            redirectAttributes.addFlashAttribute("success", "Сотрудник успешно создан");
        } catch (Exception e) {
            logger.error("Error creating employee: {}", employeeDTO.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании сотрудника");
        }

        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/edit/{id}")
    public String updateEmployee(@PathVariable Long id,
                                 @Valid @ModelAttribute EmployeeDTO employeeDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to update employee ID: {}",
                currentEmployee.getEmail(), id);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in employee update for ID: {}", id);
            return "admin/employees/edit";
        }

        try {
            Employee updated = employeeService.updateEmployee(id, employeeDTO);
            if (updated != null) {
                logger.info("Employee ID: {} updated successfully", id);
                redirectAttributes.addFlashAttribute("success", "Сотрудник успешно обновлен");
            } else {
                logger.error("Failed to update employee ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении сотрудника");
            }
        } catch (Exception e) {
            logger.error("Error updating employee ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении сотрудника");
        }

        return "redirect:/admin/employees";
    }

    @PostMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to delete employee ID: {}",
                currentEmployee.getEmail(), id);

        try {
            if (employeeService.deleteEmployee(id)) {
                logger.info("Employee ID: {} deleted successfully", id);
                redirectAttributes.addFlashAttribute("success", "Сотрудник успешно удален");
            } else {
                logger.warn("Failed to delete employee ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении сотрудника");
            }
        } catch (Exception e) {
            logger.error("Error deleting employee ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении сотрудника");
        }

        return "redirect:/admin/employees";
    }

    // ==================== ДОГОВОРЫ ====================

    @GetMapping("/contracts")
    public String contractsList(Model model) {
        logger.info("Accessing contracts list");

        try {
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("contracts", contracts);
            logger.info("Loaded {} contracts", contracts.size());
            return "admin/contracts/list";
        } catch (Exception e) {
            logger.error("Error loading contracts list", e);
            model.addAttribute("error", "Ошибка при загрузке списка договоров");
            return "admin/contracts/list";
        }
    }

    @PostMapping("/contracts/create")
    public String createContract(@Valid @ModelAttribute ContractDTO contractDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to create contract for client ID: {}",
                currentEmployee.getEmail(), contractDTO.getClientId());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in contract creation");
            List<Client> clients = clientService.getAllClients();
            List<ServiceEntity> services = serviceService.getAllServices();
            model.addAttribute("clients", clients);
            model.addAttribute("services", services);
            return "admin/contracts/create";
        }

        try {
            Contract created = contractService.createContract(contractDTO);
            if (created != null) {
                logger.info("Contract created successfully with ID: {}", created.getId());
                redirectAttributes.addFlashAttribute("success", "Договор успешно создан");
            } else {
                logger.error("Failed to create contract");
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании договора");
            }
        } catch (Exception e) {
            logger.error("Error creating contract", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании договора");
        }

        return "redirect:/admin/contracts";
    }

    @PostMapping("/contracts/edit/{id}")
    public String updateContract(@PathVariable Long id,
                                 @Valid @ModelAttribute ContractDTO contractDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to update contract ID: {}",
                currentEmployee.getEmail(), id);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in contract update for ID: {}", id);
            List<Client> clients = clientService.getAllClients();
            List<ServiceEntity> services = serviceService.getAllServices();
            model.addAttribute("clients", clients);
            model.addAttribute("services", services);
            return "admin/contracts/edit";
        }

        try {
            Contract updated = contractService.updateContract(id, contractDTO);
            if (updated != null) {
                logger.info("Contract ID: {} updated successfully", id);
                redirectAttributes.addFlashAttribute("success", "Договор успешно обновлен");
            } else {
                logger.error("Failed to update contract ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении договора");
            }
        } catch (Exception e) {
            logger.error("Error updating contract ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении договора");
        }

        return "redirect:/admin/contracts";
    }

    @PostMapping("/contracts/delete/{id}")
    public String deleteContract(@PathVariable Long id,
                                 @RequestParam(defaultValue = "false") boolean confirm,
                                 RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to delete contract ID: {}",
                currentEmployee.getEmail(), id);

        if (!confirm) {
            logger.warn("Delete confirmation not provided for contract ID: {}", id);
            redirectAttributes.addFlashAttribute("error", "Удаление отменено. Подтвердите удаление.");
            return "redirect:/admin/contracts/delete/" + id;
        }

        try {
            if (contractService.deleteContract(id)) {
                logger.info("Contract ID: {} deleted successfully", id);
                redirectAttributes.addFlashAttribute("success", "Контракт и все связанные данные успешно удалены");
            } else {
                logger.warn("Contract not found for deletion: {}", id);
                redirectAttributes.addFlashAttribute("error", "Контракт не найден");
            }
        } catch (Exception e) {
            logger.error("Error deleting contract ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении контракта: " + e.getMessage());
        }

        return "redirect:/admin/contracts";
    }

    // ==================== ОХРАНЯЕМЫЕ ОБЪЕКТЫ ====================

    @GetMapping("/objects")
    public String objectsList(Model model) {
        logger.info("Accessing guard objects list");

        try {
            List<GuardObject> objects = guardObjectService.getAllGuardObjects();
            model.addAttribute("objects", objects);

            Map<Long, Boolean> canDelete = new HashMap<>();
            for (GuardObject object : objects) {
                canDelete.put(object.getId(), guardObjectService.canDeleteGuardObject(object.getId()));
            }
            model.addAttribute("canDelete", canDelete);

            logger.info("Loaded {} guard objects", objects.size());
            return "admin/objects/list";
        } catch (Exception e) {
            logger.error("Error loading guard objects list", e);
            model.addAttribute("error", "Ошибка при загрузке списка объектов");
            return "admin/objects/list";
        }
    }

    @PostMapping("/objects/create")
    public String createObject(@Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to create guard object: {}",
                currentEmployee.getEmail(), guardObjectDTO.getName());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in guard object creation");
            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/create";
        }

        try {
            GuardObject created = guardObjectService.createGuardObject(guardObjectDTO);
            if (created != null) {
                logger.info("Guard object created successfully with ID: {}", created.getId());
                redirectAttributes.addFlashAttribute("success", "Объект успешно создан");
            } else {
                logger.error("Failed to create guard object");
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании объекта");
            }
        } catch (RuntimeException e) {
            logger.error("Error creating guard object", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/create";
        }

        return "redirect:/admin/objects";
    }

    @PostMapping("/objects/edit/{id}")
    public String updateObject(@PathVariable Long id,
                               @Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to update guard object ID: {}",
                currentEmployee.getEmail(), id);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in guard object update for ID: {}", id);
            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);
            return "admin/objects/edit";
        }

        try {
            GuardObject updated = guardObjectService.updateGuardObject(id, guardObjectDTO);
            if (updated != null) {
                logger.info("Guard object ID: {} updated successfully", id);
                redirectAttributes.addFlashAttribute("success", "Объект успешно обновлен");
            } else {
                logger.warn("Guard object not found for update: {}", id);
                redirectAttributes.addFlashAttribute("error", "Объект не найден");
            }
        } catch (RuntimeException e) {
            logger.error("Error updating guard object ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());

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

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to delete guard object ID: {}",
                currentEmployee.getEmail(), id);

        boolean canDelete = guardObjectService.canDeleteGuardObject(id);

        if (!canDelete) {
            logger.warn("Cannot delete guard object ID: {} - related data exists", id);
            redirectAttributes.addFlashAttribute("error",
                    "Невозможно удалить объект: существуют связанные контракты или расписания");
            return "redirect:/admin/objects";
        }

        try {
            if (guardObjectService.deleteGuardObject(id)) {
                logger.info("Guard object ID: {} deleted successfully", id);
                redirectAttributes.addFlashAttribute("success", "Объект успешно удален");
            } else {
                logger.error("Failed to delete guard object ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении объекта");
            }
        } catch (Exception e) {
            logger.error("Error deleting guard object ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении объекта");
        }

        return "redirect:/admin/objects";
    }

    // ==================== УСЛУГИ ====================

    @GetMapping("/services")
    public String servicesList(Model model) {
        logger.info("Accessing services list");

        try {
            List<ServiceEntity> services = serviceService.getAllServices();
            model.addAttribute("services", services);
            logger.info("Loaded {} services", services.size());
            return "admin/services/list";
        } catch (Exception e) {
            logger.error("Error loading services list", e);
            model.addAttribute("error", "Ошибка при загрузке списка услуг");
            return "admin/services/list";
        }
    }

    @PostMapping("/services/create")
    public String createService(@Valid @ModelAttribute ServiceDTO serviceDTO,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to create service: {}",
                currentEmployee.getEmail(), serviceDTO.getName());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in service creation");
            return "admin/services/create";
        }

        try {
            if (serviceService.existsByName(serviceDTO.getName())) {
                logger.warn("Service with name already exists: {}", serviceDTO.getName());
                redirectAttributes.addFlashAttribute("error", "Услуга с таким названием уже существует");
                return "redirect:/admin/services/create";
            }

            serviceService.createService(serviceDTO);
            logger.info("Service created successfully: {}", serviceDTO.getName());
            redirectAttributes.addFlashAttribute("success", "Услуга успешно создана");
        } catch (RuntimeException e) {
            logger.error("Error creating service: {}", serviceDTO.getName(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/services/create";
        }

        return "redirect:/admin/services";
    }

    @PostMapping("/services/edit/{id}")
    public String updateService(@PathVariable Long id,
                                @Valid @ModelAttribute ServiceDTO serviceDTO,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to update service ID: {}",
                currentEmployee.getEmail(), id);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in service update for ID: {}", id);
            return "admin/services/edit";
        }

        try {
            Optional<ServiceEntity> existingService = serviceService.getServiceById(id);
            if (existingService.isPresent()) {
                ServiceEntity currentService = existingService.get();
                if (!currentService.getName().equals(serviceDTO.getName()) &&
                        serviceService.existsByName(serviceDTO.getName())) {
                    logger.warn("Service with name already exists: {}", serviceDTO.getName());
                    redirectAttributes.addFlashAttribute("error", "Услуга с таким названием уже существует");
                    return "redirect:/admin/services/edit/" + id;
                }
            }

            ServiceEntity updated = serviceService.updateService(id, serviceDTO);
            if (updated != null) {
                logger.info("Service ID: {} updated successfully", id);
                redirectAttributes.addFlashAttribute("success", "Услуга успешно обновлена");
            } else {
                logger.warn("Service not found for update: {}", id);
                redirectAttributes.addFlashAttribute("error", "Услуга не найдена");
            }
        } catch (RuntimeException e) {
            logger.error("Error updating service ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/services/edit/" + id;
        }

        return "redirect:/admin/services";
    }

    @PostMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to delete service ID: {}",
                currentEmployee.getEmail(), id);

        try {
            if (serviceService.deleteService(id)) {
                logger.info("Service ID: {} deleted successfully", id);
                redirectAttributes.addFlashAttribute("success", "Услуга успешно удалена");
            } else {
                logger.warn("Service not found for deletion: {}", id);
                redirectAttributes.addFlashAttribute("error", "Услуга не найдена");
            }
        } catch (Exception e) {
            logger.error("Error deleting service ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении услуги: " + e.getMessage());
        }

        return "redirect:/admin/services";
    }

    // ==================== ГРАФИКИ РАБОТЫ ====================

    @GetMapping("/schedules")
    public String schedulesList(Model model) {
        logger.info("Accessing schedules list");

        try {
            List<Schedule> schedules = scheduleService.getAllSchedules();
            model.addAttribute("schedules", schedules);
            logger.info("Loaded {} schedules", schedules.size());
            return "admin/schedules/list";
        } catch (Exception e) {
            logger.error("Error loading schedules list", e);
            model.addAttribute("error", "Ошибка при загрузке списка графиков работы");
            return "admin/schedules/list";
        }
    }

    @PostMapping("/schedules/create")
    public String createSchedule(@Valid @ModelAttribute ScheduleDTO scheduleDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to create schedule for employee ID: {} and object ID: {}",
                currentEmployee.getEmail(), scheduleDTO.getEmployeeId(), scheduleDTO.getGuardObjectId());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in schedule creation");
            List<Employee> employees = employeeService.getAllEmployees();
            List<GuardObject> guardObjects = guardObjectService.getAllGuardObjects();
            model.addAttribute("employees", employees);
            model.addAttribute("guardObjects", guardObjects);
            return "admin/schedules/create";
        }

        try {
            Schedule created = scheduleService.createSchedule(scheduleDTO);
            if (created != null) {
                logger.info("Schedule created successfully with ID: {}", created.getId());
                redirectAttributes.addFlashAttribute("success", "График работы успешно создан");
            } else {
                logger.error("Failed to create schedule");
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании графика работы");
            }
        } catch (RuntimeException e) {
            logger.error("Error creating schedule", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/schedules/create";
        }

        return "redirect:/admin/schedules";
    }

    @PostMapping("/schedules/edit/{id}")
    public String updateSchedule(@PathVariable Long id,
                                 @Valid @ModelAttribute ScheduleDTO scheduleDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to update schedule ID: {}",
                currentEmployee.getEmail(), id);

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in schedule update for ID: {}", id);
            List<Employee> employees = employeeService.getAllEmployees();
            List<GuardObject> guardObjects = guardObjectService.getAllGuardObjects();
            model.addAttribute("employees", employees);
            model.addAttribute("guardObjects", guardObjects);
            return "admin/schedules/edit";
        }

        try {
            Schedule updated = scheduleService.updateSchedule(id, scheduleDTO);
            if (updated != null) {
                logger.info("Schedule ID: {} updated successfully", id);
                redirectAttributes.addFlashAttribute("success", "График работы успешно обновлен");
            } else {
                logger.warn("Schedule not found for update: {}", id);
                redirectAttributes.addFlashAttribute("error", "График работы не найден");
            }
        } catch (RuntimeException e) {
            logger.error("Error updating schedule ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/schedules/edit/" + id;
        }

        return "redirect:/admin/schedules";
    }

    @PostMapping("/schedules/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} attempting to delete schedule ID: {}",
                currentEmployee.getEmail(), id);

        try {
            if (scheduleService.deleteSchedule(id)) {
                logger.info("Schedule ID: {} deleted successfully", id);
                redirectAttributes.addFlashAttribute("success", "График работы успешно удален");
            } else {
                logger.error("Failed to delete schedule ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении графика работы");
            }
        } catch (Exception e) {
            logger.error("Error deleting schedule ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении графика работы");
        }

        return "redirect:/admin/schedules";
    }

    // ==================== ОТЧЕТЫ ====================

    @PostMapping("/reports/revenue")
    public ResponseEntity<byte[]> generateRevenueReport(@RequestParam LocalDate startDate,
                                                        @RequestParam LocalDate endDate) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} generating revenue report from {} to {}",
                currentEmployee.getEmail(), startDate, endDate);

        try {
            byte[] excelBytes = reportService.generateRevenueReport(startDate, endDate);

            String filename = String.format("revenue_report_%s_%s.xlsx",
                    startDate, endDate);

            logger.info("Revenue report generated successfully");
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);

        } catch (IOException e) {
            logger.error("Error generating revenue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error generating revenue report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/reports/contracts")
    public ResponseEntity<byte[]> generateContractsReport(@RequestParam LocalDate startDate,
                                                          @RequestParam LocalDate endDate) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} generating contracts report from {} to {}",
                currentEmployee.getEmail(), startDate, endDate);

        try {
            byte[] excelBytes = reportService.generateContractsReport(startDate, endDate);

            String filename = String.format("contracts_report_%s_%s.xlsx",
                    startDate, endDate);

            logger.info("Contracts report generated successfully");
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);

        } catch (IOException e) {
            logger.error("Error generating contracts report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error generating contracts report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/reports/clients")
    public ResponseEntity<byte[]> generateClientsReport(@RequestParam LocalDate startDate,
                                                        @RequestParam LocalDate endDate) {

        Employee currentEmployee = getCurrentEmployee();
        logger.info("Admin {} generating clients report from {} to {}",
                currentEmployee.getEmail(), startDate, endDate);

        try {
            byte[] excelBytes = reportService.generateClientsReport(startDate, endDate);

            String filename = String.format("clients_report_%s_%s.xlsx",
                    startDate, endDate);

            logger.info("Clients report generated successfully");
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);

        } catch (IOException e) {
            logger.error("Error generating clients report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error generating clients report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        logger.info("Accessing reports page");

        Employee employee = getCurrentEmployee();
        if (employee == null) {
            logger.warn("Attempt to access reports page without authentication");
            return "redirect:/login";
        }

        model.addAttribute("employee", employee);
        return "admin/reports";
    }

    // ==================== ОХРАНЯЕМЫЕ ОБЪЕКТЫ ====================

    @GetMapping("/objects/create")
    public String showCreateObjectForm(Model model) {
        logger.info("Showing create guard object form");

        try {
            List<Client> clients = clientService.getAllClients();
            List<Contract> contracts = contractService.getAllContracts();

            model.addAttribute("guardObjectDTO", new GuardObjectDTO());
            model.addAttribute("clients", clients);
            model.addAttribute("contracts", contracts);

            logger.info("Create guard object form loaded with {} clients and {} contracts",
                    clients.size(), contracts.size());
            return "admin/objects/create";
        } catch (Exception e) {
            logger.error("Error loading create guard object form", e);
            model.addAttribute("error", "Ошибка при загрузке формы создания объекта");
            return "admin/objects/create";
        }
    }

    @GetMapping("/objects/edit/{id}")
    public String showEditObjectForm(@PathVariable Long id, Model model) {
        logger.info("Showing edit guard object form for ID: {}", id);

        try {
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

                logger.info("Edit guard object form loaded for ID: {}", id);
                return "admin/objects/edit";
            } else {
                logger.warn("Guard object not found for editing: {}", id);
                return "redirect:/admin/objects";
            }
        } catch (Exception e) {
            logger.error("Error loading edit guard object form for ID: {}", id, e);
            model.addAttribute("error", "Ошибка при загрузке формы редактирования объекта");
            return "redirect:/admin/objects";
        }
    }

// ==================== ДОГОВОРЫ ====================

    @GetMapping("/contracts/create")
    public String showCreateContractForm(Model model) {
        logger.info("Showing create contract form");

        try {
            List<Client> clients = clientService.getAllClients();
            List<ServiceEntity> services = serviceService.getAllServices();

            model.addAttribute("contractDTO", new ContractDTO());
            model.addAttribute("clients", clients);
            model.addAttribute("services", services);

            logger.info("Create contract form loaded with {} clients and {} services",
                    clients.size(), services.size());
            return "admin/contracts/create";
        } catch (Exception e) {
            logger.error("Error loading create contract form", e);
            model.addAttribute("error", "Ошибка при загрузке формы создания договора");
            return "admin/contracts/create";
        }
    }

    @GetMapping("/contracts/edit/{id}")
    public String showEditContractForm(@PathVariable Long id, Model model) {
        logger.info("Showing edit contract form for ID: {}", id);

        try {
            Optional<Contract> contract = contractService.getContractById(id);
            if (contract.isPresent()) {
                Contract c = contract.get();
                ContractDTO contractDTO = new ContractDTO();
                contractDTO.setId(c.getId());
                contractDTO.setClientId(c.getClient().getId());
                contractDTO.setServiceId(c.getService().getId());
                contractDTO.setStartDate(c.getStartDate());
                contractDTO.setEndDate(c.getEndDate());
                contractDTO.setStatus(c.getStatus());

                List<Client> clients = clientService.getAllClients();
                List<ServiceEntity> services = serviceService.getAllServices();

                model.addAttribute("contractDTO", contractDTO);
                model.addAttribute("clients", clients);
                model.addAttribute("services", services);

                logger.info("Edit contract form loaded for ID: {}", id);
                return "admin/contracts/edit";
            } else {
                logger.warn("Contract not found for editing: {}", id);
                return "redirect:/admin/contracts";
            }
        } catch (Exception e) {
            logger.error("Error loading edit contract form for ID: {}", id, e);
            model.addAttribute("error", "Ошибка при загрузке формы редактирования договора");
            return "redirect:/admin/contracts";
        }
    }

    @GetMapping("/contracts/delete/{id}")
    public String showDeleteContractForm(@PathVariable Long id, Model model) {
        logger.info("Showing delete contract form for ID: {}", id);

        try {
            Optional<Contract> contract = contractService.getContractById(id);
            if (contract.isPresent()) {
                Contract c = contract.get();

                // Создаем объект с информацией об удалении
                Map<String, Object> deletionInfo = new HashMap<>();
                deletionInfo.put("contract", c);

                // Получаем связанные объекты охраны
                List<GuardObject> guardObjects = guardObjectService.getGuardObjectsByContractId(id);
                deletionInfo.put("guardObjects", guardObjects);
                deletionInfo.put("guardObjectsCount", guardObjects.size());

                // Получаем связанные расписания
                List<Schedule> schedules = scheduleService.getSchedulesByContractId(id);
                deletionInfo.put("schedules", schedules);
                deletionInfo.put("schedulesCount", schedules.size());

                model.addAttribute("deletionInfo", deletionInfo);

                logger.info("Delete contract form loaded for ID: {}", id);
                return "admin/contracts/delete";
            } else {
                logger.warn("Contract not found for deletion: {}", id);
                return "redirect:/admin/contracts";
            }
        } catch (Exception e) {
            logger.error("Error loading delete contract form for ID: {}", id, e);
            model.addAttribute("error", "Ошибка при загрузке формы удаления договора");
            return "redirect:/admin/contracts";
        }
    }

// ==================== СОТРУДНИКИ ====================

    @GetMapping("/employees/create")
    public String showCreateEmployeeForm(Model model) {
        logger.info("Showing create employee form");

        try {
            model.addAttribute("employeeDTO", new EmployeeDTO());
            logger.info("Create employee form loaded");
            return "admin/employees/create";
        } catch (Exception e) {
            logger.error("Error loading create employee form", e);
            model.addAttribute("error", "Ошибка при загрузке формы создания сотрудника");
            return "admin/employees/create";
        }
    }

    @GetMapping("/employees/edit/{id}")
    public String showEditEmployeeForm(@PathVariable Long id, Model model) {
        logger.info("Showing edit employee form for ID: {}", id);

        try {
            Optional<Employee> employee = employeeService.getEmployeeById(id);
            if (employee.isPresent()) {
                Employee emp = employee.get();
                EmployeeDTO employeeDTO = new EmployeeDTO();
                employeeDTO.setId(emp.getId());
                employeeDTO.setLastName(emp.getLastName());
                employeeDTO.setFirstName(emp.getFirstName());
                employeeDTO.setPatronymic(emp.getPatronymic());
                employeeDTO.setPhone(emp.getPhone());
                employeeDTO.setEmail(emp.getEmail());
                employeeDTO.setLogin(emp.getLogin());
                employeeDTO.setPosition(emp.getPosition());

                model.addAttribute("employeeDTO", employeeDTO);
                logger.info("Edit employee form loaded for ID: {}", id);
                return "admin/employees/edit";
            } else {
                logger.warn("Employee not found for editing: {}", id);
                return "redirect:/admin/employees";
            }
        } catch (Exception e) {
            logger.error("Error loading edit employee form for ID: {}", id, e);
            model.addAttribute("error", "Ошибка при загрузке формы редактирования сотрудника");
            return "redirect:/admin/employees";
        }
    }

// ==================== УСЛУГИ ====================

    @GetMapping("/services/create")
    public String showCreateServiceForm(Model model) {
        logger.info("Showing create service form");

        try {
            model.addAttribute("serviceDTO", new ServiceDTO());
            logger.info("Create service form loaded");
            return "admin/services/create";
        } catch (Exception e) {
            logger.error("Error loading create service form", e);
            model.addAttribute("error", "Ошибка при загрузке формы создания услуги");
            return "admin/services/create";
        }
    }

    @GetMapping("/services/edit/{id}")
    public String showEditServiceForm(@PathVariable Long id, Model model) {
        logger.info("Showing edit service form for ID: {}", id);

        try {
            Optional<ServiceEntity> service = serviceService.getServiceById(id);
            if (service.isPresent()) {
                ServiceEntity s = service.get();
                ServiceDTO serviceDTO = new ServiceDTO();
                serviceDTO.setId(s.getId());
                serviceDTO.setName(s.getName());
                serviceDTO.setDescription(s.getDescription());
                serviceDTO.setPrice(s.getPrice());

                model.addAttribute("serviceDTO", serviceDTO);
                logger.info("Edit service form loaded for ID: {}", id);
                return "admin/services/edit";
            } else {
                logger.warn("Service not found for editing: {}", id);
                return "redirect:/admin/services";
            }
        } catch (Exception e) {
            logger.error("Error loading edit service form for ID: {}", id, e);
            model.addAttribute("error", "Ошибка при загрузке формы редактирования услуги");
            return "redirect:/admin/services";
        }
    }


}