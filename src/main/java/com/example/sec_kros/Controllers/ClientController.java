package com.example.sec_kros.Controllers;

import com.example.sec_kros.DTO.ContractCreateDTO;
import com.example.sec_kros.DTO.ContractDTO;
import com.example.sec_kros.DTO.GuardObjectDTO;
import com.example.sec_kros.Entities.*;
import com.example.sec_kros.Services.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/client")
public class ClientController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private GuardObjectService guardObjectService;

    @Autowired
    private ServiceService serviceService;

    // Вспомогательный метод для получения текущего клиента
    private Client getCurrentClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Attempt to get current client without authentication");
            return null;
        }
        String username = authentication.getName();
        Client client = clientService.findByEmail(username);

        if (client == null) {
            logger.warn("Client not found for email: {}", username);
        } else {
            logger.debug("Current client retrieved: {} (ID: {})", client.getEmail(), client.getId());
        }

        return client;
    }

    @GetMapping("/dashboard")
    public String clientDashboard(Model model) {
        logger.info("Client dashboard accessed");

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to client dashboard");
            return "redirect:/login";
        }

        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());
            List<Contract> contracts = contractService.getContractsByClientId(client.getId());
            List<GuardObject> objects = guardObjectService.getGuardObjectsByClientId(client.getId());

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("contractsCount", contracts.size());
            model.addAttribute("objectsCount", objects.size());

            logger.info("Dashboard loaded for client: {} ({} unread notifications, {} contracts, {} objects)",
                    client.getEmail(), unreadNotifications.size(), contracts.size(), objects.size());
            return "client/dashboard";

        } catch (Exception e) {
            logger.error("Error loading dashboard for client: {}", client.getEmail(), e);
            model.addAttribute("error", "Произошла ошибка при загрузке данных");
            return "client/dashboard";
        }
    }

    @GetMapping("/notifications")
    public String clientNotifications(Model model) {
        logger.info("Client notifications page accessed");

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to notifications");
            return "redirect:/login";
        }

        try {
            List<Notification> notifications = notificationService.getNotificationsByClientId(client.getId());
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

            model.addAttribute("client", client);
            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadNotifications.size());

            logger.info("Notifications loaded for client: {} (total: {}, unread: {})",
                    client.getEmail(), notifications.size(), unreadNotifications.size());
            return "client/notifications";

        } catch (Exception e) {
            logger.error("Error loading notifications for client: {}", client.getEmail(), e);
            model.addAttribute("error", "Ошибка при загрузке уведомлений");
            return "client/notifications";
        }
    }

    @GetMapping("/contracts")
    public String clientContracts(Model model) {
        logger.info("Client contracts page accessed");

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to contracts");
            return "redirect:/login";
        }

        try {
            List<Contract> contracts = contractService.getContractsByClientId(client.getId());
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

            model.addAttribute("client", client);
            model.addAttribute("contracts", contracts);
            model.addAttribute("unreadCount", unreadNotifications.size());

            logger.info("Contracts loaded for client: {} (total: {}, unread notifications: {})",
                    client.getEmail(), contracts.size(), unreadNotifications.size());
            return "client/contracts";

        } catch (Exception e) {
            logger.error("Error loading contracts for client: {}", client.getEmail(), e);
            model.addAttribute("error", "Ошибка при загрузке договоров");
            return "client/contracts";
        }
    }

    @GetMapping("/profile")
    public String clientProfile(Model model) {
        logger.info("Client profile page accessed");

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to profile");
            return "redirect:/login";
        }

        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());

            logger.info("Profile loaded for client: {} (unread notifications: {})",
                    client.getEmail(), unreadNotifications.size());
            return "client/profile";

        } catch (Exception e) {
            logger.error("Error loading profile for client: {}", client.getEmail(), e);
            model.addAttribute("error", "Ошибка при загрузке профиля");
            return "client/profile";
        }
    }

    @PostMapping("/notifications/mark-as-read/{id}")
    public String markNotificationAsRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized attempt to mark notification as read");
            return "redirect:/login";
        }

        logger.info("Client {} marking notification {} as read", client.getEmail(), id);

        try {
            notificationService.markAsRead(id);
            logger.info("Notification {} marked as read by client {}", id, client.getEmail());
            redirectAttributes.addFlashAttribute("success", "Уведомление отмечено как прочитанное");
        } catch (Exception e) {
            logger.error("Error marking notification {} as read by client {}", id, client.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении уведомления");
        }
        return "redirect:/client/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsAsRead(RedirectAttributes redirectAttributes) {
        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized attempt to mark all notifications as read");
            return "redirect:/login";
        }

        logger.info("Client {} marking all notifications as read", client.getEmail());

        try {
            notificationService.markAllAsReadByClientId(client.getId());
            logger.info("All notifications marked as read for client {}", client.getEmail());
            redirectAttributes.addFlashAttribute("success", "Все уведомления отмечены как прочитанные");
        } catch (Exception e) {
            logger.error("Error marking all notifications as read for client {}", client.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении уведомлений");
        }
        return "redirect:/client/notifications";
    }

    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized attempt to delete notification");
            return "redirect:/login";
        }

        logger.info("Client {} deleting notification {}", client.getEmail(), id);

        try {
            notificationService.deleteNotification(id);
            logger.info("Notification {} deleted by client {}", id, client.getEmail());
            redirectAttributes.addFlashAttribute("success", "Уведомление удалено");
        } catch (Exception e) {
            logger.error("Error deleting notification {} by client {}", id, client.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении уведомления");
        }
        return "redirect:/client/notifications";
    }

    @GetMapping("/objects/create")
    public String showCreateObjectForm(Model model) {
        logger.info("Client create object form accessed");

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to create object form");
            return "redirect:/login";
        }

        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

            // Получаем НЕактивные контракты
            List<Contract> inactiveContracts = contractService.getContractsByClientId(client.getId())
                    .stream()
                    .filter(c -> "inactive".equals(c.getStatus()))
                    .collect(Collectors.toList());

            // Создаем DTO с предустановленным clientId
            GuardObjectDTO guardObjectDTO = new GuardObjectDTO();
            guardObjectDTO.setClientId(client.getId()); // Устанавливаем clientId автоматически

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("inactiveContracts", inactiveContracts);
            model.addAttribute("guardObjectDTO", guardObjectDTO);

            logger.info("Create object form loaded for client: {} ({} inactive contracts available)",
                    client.getEmail(), inactiveContracts.size());
            return "client/objects-create";

        } catch (Exception e) {
            logger.error("Error loading create object form for client: {}", client.getEmail(), e);
            model.addAttribute("error", "Ошибка при загрузке формы создания объекта");
            return "client/objects-create";
        }
    }

    @PostMapping("/objects/create")
    public String createObject(@Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized attempt to create object");
            return "redirect:/login";
        }

        logger.info("Client {} attempting to create guard object: {}",
                client.getEmail(), guardObjectDTO.getName());

        // ВАЖНО: Устанавливаем clientId из текущего клиента, а не из DTO
        guardObjectDTO.setClientId(client.getId());

        // Проверяем, что клиент имеет доступ к выбранному договору
        Optional<Contract> contractOptional = contractService.getContractById(guardObjectDTO.getContractId());
        if (contractOptional.isEmpty()) {
            logger.warn("Contract not found for ID: {} while creating object for client {}",
                    guardObjectDTO.getContractId(), client.getEmail());
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не найден");
            return "redirect:/client/objects/create";
        }

        Contract contract = contractOptional.get();
        if (!contract.getClient().getId().equals(client.getId())) {
            logger.warn("Client {} attempted to use contract {} not belonging to them",
                    client.getEmail(), contract.getId());
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не принадлежит вам");
            return "redirect:/client/objects/create";
        }

        // Проверяем, что договор имеет статус "inactive"
        if (!"inactive".equals(contract.getStatus())) {
            logger.warn("Client {} attempted to attach object to non-inactive contract {} (status: {})",
                    client.getEmail(), contract.getId(), contract.getStatus());
            redirectAttributes.addFlashAttribute("error", "Объект можно привязать только к договору на рассмотрении (статус 'Неактивен')");
            return "redirect:/client/objects/create";
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in guard object creation for client {}: {} errors",
                    client.getEmail(), bindingResult.getErrorCount());

            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());
            List<Contract> inactiveContracts = contractService.getContractsByClientId(client.getId())
                    .stream()
                    .filter(c -> "inactive".equals(c.getStatus()))
                    .collect(Collectors.toList());

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("inactiveContracts", inactiveContracts);
            return "client/objects-create";
        }

        try {
            GuardObject created = guardObjectService.createGuardObject(guardObjectDTO);
            if (created != null) {
                logger.info("Guard object created successfully for client {} with ID: {}",
                        client.getEmail(), created.getId());
                redirectAttributes.addFlashAttribute("success", "Объект успешно создан");
            } else {
                logger.error("Failed to create guard object for client {}", client.getEmail());
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании объекта");
            }
        } catch (RuntimeException e) {
            logger.error("Error creating guard object for client {}", client.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/objects";
    }

    @GetMapping("/objects/edit/{id}")
    public String showEditObjectForm(@PathVariable Long id, Model model) {
        logger.info("Client edit object form accessed for object ID: {}", id);

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to edit object form");
            return "redirect:/login";
        }

        Optional<GuardObject> guardObject = guardObjectService.getGuardObjectById(id);
        if (guardObject.isEmpty()) {
            logger.warn("Guard object not found for editing: {}", id);
            return "redirect:/client/objects";
        }

        // Проверяем, что объект принадлежит клиенту
        if (!guardObject.get().getClient().getId().equals(client.getId())) {
            logger.warn("Client {} attempted to edit object {} not belonging to them",
                    client.getEmail(), id);
            return "redirect:/client/objects";
        }

        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

            // Получаем НЕактивные контракты
            List<Contract> inactiveContracts = contractService.getContractsByClientId(client.getId())
                    .stream()
                    .filter(c -> "inactive".equals(c.getStatus()))
                    .collect(Collectors.toList());

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

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("inactiveContracts", inactiveContracts);
            model.addAttribute("guardObjectDTO", guardObjectDTO);

            logger.info("Edit object form loaded for client {} object {}", client.getEmail(), id);
            return "client/objects-edit";

        } catch (Exception e) {
            logger.error("Error loading edit object form for client {} object {}", client.getEmail(), id, e);
            model.addAttribute("error", "Ошибка при загрузке формы редактирования");
            return "client/objects-edit";
        }
    }

    @PostMapping("/objects/edit/{id}")
    public String updateObject(@PathVariable Long id,
                               @Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized attempt to update object");
            return "redirect:/login";
        }

        logger.info("Client {} attempting to update guard object ID: {}",
                client.getEmail(), id);

        // Проверяем, что объект принадлежит клиенту
        Optional<GuardObject> existingObject = guardObjectService.getGuardObjectById(id);
        if (existingObject.isEmpty()) {
            logger.warn("Guard object not found for update: {}", id);
            redirectAttributes.addFlashAttribute("error", "Объект не найден");
            return "redirect:/client/objects";
        }

        if (!existingObject.get().getClient().getId().equals(client.getId())) {
            logger.warn("Client {} attempted to update object {} not belonging to them",
                    client.getEmail(), id);
            redirectAttributes.addFlashAttribute("error", "Недостаточно прав для редактирования объекта");
            return "redirect:/client/objects";
        }

        // Устанавливаем clientId из текущего клиента
        guardObjectDTO.setClientId(client.getId());
        guardObjectDTO.setId(id); // Устанавливаем ID для обновления

        // Проверяем доступ к договору
        Optional<Contract> contractOptional = contractService.getContractById(guardObjectDTO.getContractId());
        if (contractOptional.isEmpty()) {
            logger.warn("Contract not found for ID: {} while updating object for client {}",
                    guardObjectDTO.getContractId(), client.getEmail());
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не найден");
            return "redirect:/client/objects/edit/" + id;
        }

        Contract contract = contractOptional.get();
        if (!contract.getClient().getId().equals(client.getId())) {
            logger.warn("Client {} attempted to use contract {} not belonging to them for object update",
                    client.getEmail(), contract.getId());
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не принадлежит вам");
            return "redirect:/client/objects/edit/" + id;
        }

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in guard object update for client {} object {}: {} errors",
                    client.getEmail(), id, bindingResult.getErrorCount());

            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());
            List<Contract> inactiveContracts = contractService.getContractsByClientId(client.getId())
                    .stream()
                    .filter(c -> "inactive".equals(c.getStatus()))
                    .collect(Collectors.toList());

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("inactiveContracts", inactiveContracts);
            return "client/objects-edit";
        }

        try {
            GuardObject updated = guardObjectService.updateGuardObject(id, guardObjectDTO);
            if (updated != null) {
                logger.info("Guard object ID: {} updated successfully by client {}",
                        id, client.getEmail());
                redirectAttributes.addFlashAttribute("success", "Объект успешно обновлен");
            } else {
                logger.warn("Failed to update guard object ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Объект не найден");
            }
        } catch (RuntimeException e) {
            logger.error("Error updating guard object ID: {} for client {}", id, client.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/objects";
    }

    @PostMapping("/objects/delete/{id}")
    public String deleteObject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized attempt to delete object");
            return "redirect:/login";
        }

        logger.info("Client {} attempting to delete guard object ID: {}",
                client.getEmail(), id);

        // Проверяем, что объект принадлежит клиенту
        Optional<GuardObject> guardObject = guardObjectService.getGuardObjectById(id);
        if (guardObject.isEmpty()) {
            logger.warn("Guard object not found for deletion: {}", id);
            redirectAttributes.addFlashAttribute("error", "Объект не найден");
            return "redirect:/client/objects";
        }

        if (!guardObject.get().getClient().getId().equals(client.getId())) {
            logger.warn("Client {} attempted to delete object {} not belonging to them",
                    client.getEmail(), id);
            redirectAttributes.addFlashAttribute("error", "Недостаточно прав для удаления объекта");
            return "redirect:/client/objects";
        }

        // Проверяем возможность удаления
        boolean canDelete = guardObjectService.canDeleteGuardObject(id);

        if (!canDelete) {
            logger.warn("Cannot delete guard object ID: {} - related schedules exist", id);
            redirectAttributes.addFlashAttribute("error",
                    "Невозможно удалить объект: существуют связанные расписания");
            return "redirect:/client/objects";
        }

        try {
            if (guardObjectService.deleteGuardObject(id)) {
                logger.info("Guard object ID: {} deleted successfully by client {}",
                        id, client.getEmail());
                redirectAttributes.addFlashAttribute("success", "Объект успешно удален");
            } else {
                logger.error("Failed to delete guard object ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении объекта");
            }
        } catch (Exception e) {
            logger.error("Error deleting guard object ID: {} for client {}", id, client.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении объекта");
        }

        return "redirect:/client/objects";
    }

    @GetMapping("/objects")
    public String clientObjects(Model model) {
        logger.info("Client objects page accessed");

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to objects");
            return "redirect:/login";
        }

        try {
            List<GuardObject> objects = guardObjectService.getGuardObjectsByClientId(client.getId());
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

            // Получаем активные и неактивные контракты для информации
            List<Contract> activeContracts = contractService.getContractsByClientId(client.getId())
                    .stream()
                    .filter(c -> "active".equals(c.getStatus()))
                    .collect(Collectors.toList());

            List<Contract> inactiveContracts = contractService.getContractsByClientId(client.getId())
                    .stream()
                    .filter(c -> "inactive".equals(c.getStatus()))
                    .collect(Collectors.toList());

            // Создаем Map с информацией о возможности удаления для каждого объекта
            Map<Long, Boolean> canDeleteMap = new HashMap<>();
            for (GuardObject object : objects) {
                canDeleteMap.put(object.getId(), guardObjectService.canDeleteGuardObject(object.getId()));
            }

            model.addAttribute("client", client);
            model.addAttribute("objects", objects);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("activeContracts", activeContracts);
            model.addAttribute("inactiveContracts", inactiveContracts);
            model.addAttribute("canDeleteMap", canDeleteMap);

            logger.info("Objects loaded for client: {} (total: {}, unread notifications: {})",
                    client.getEmail(), objects.size(), unreadNotifications.size());
            return "client/objects";

        } catch (Exception e) {
            logger.error("Error loading objects for client: {}", client.getEmail(), e);
            model.addAttribute("error", "Ошибка при загрузке объектов");
            return "client/objects";
        }
    }

    @GetMapping("/contracts/create")
    public String showCreateContractForm(Model model) {
        logger.info("Client create contract form accessed");

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized access to create contract form");
            return "redirect:/login";
        }

        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());
            List<ServiceEntity> services = serviceService.getAllServices();

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("services", services);
            model.addAttribute("contractCreateDTO", new ContractCreateDTO());

            logger.info("Create contract form loaded for client: {} ({} services available)",
                    client.getEmail(), services.size());
            return "client/contracts-create";

        } catch (Exception e) {
            logger.error("Error loading create contract form for client: {}", client.getEmail(), e);
            model.addAttribute("error", "Ошибка при загрузке формы создания договора");
            return "client/contracts-create";
        }
    }

    @PostMapping("/contracts/create")
    public String createContract(@Valid @ModelAttribute ContractCreateDTO contractCreateDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        Client client = getCurrentClient();
        if (client == null) {
            logger.warn("Unauthorized attempt to create contract");
            return "redirect:/login";
        }

        logger.info("Client {} attempting to create contract with service ID: {}",
                client.getEmail(), contractCreateDTO.getServiceId());

        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors in contract creation for client {}: {} errors",
                    client.getEmail(), bindingResult.getErrorCount());

            bindingResult.getAllErrors().forEach(error -> {
                logger.debug("Validation error: {}", error.getDefaultMessage());
            });

            List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());
            List<ServiceEntity> services = serviceService.getAllServices();

            model.addAttribute("client", client);
            model.addAttribute("unreadCount", unreadNotifications.size());
            model.addAttribute("services", services);
            return "client/contracts-create";
        }

        try {
            // Получаем информацию об услуге для расчета стоимости
            Optional<ServiceEntity> serviceOptional = serviceService.getServiceById(contractCreateDTO.getServiceId());
            if (serviceOptional.isEmpty()) {
                logger.warn("Service not found for ID: {} while creating contract for client {}",
                        contractCreateDTO.getServiceId(), client.getEmail());
                redirectAttributes.addFlashAttribute("error", "Выбранная услуга не найдена");
                return "redirect:/client/contracts/create";
            }

            ServiceEntity service = serviceOptional.get();

            // Автоматически рассчитываем стоимость
            Double calculatedAmount = calculateTotalAmount(service.getPrice());
            contractCreateDTO.setTotalAmount(calculatedAmount);

            logger.debug("Calculating contract cost for client {}: service price={}, calculated amount={}",
                    client.getEmail(), service.getPrice(), calculatedAmount);

            // Преобразуем ContractCreateDTO в ContractDTO
            ContractDTO contractDTO = new ContractDTO();
            contractDTO.setClientId(client.getId());
            contractDTO.setServiceId(contractCreateDTO.getServiceId());
            contractDTO.setStartDate(contractCreateDTO.getStartDate());
            contractDTO.setEndDate(contractCreateDTO.getEndDate());
            contractDTO.setTotalAmount(calculatedAmount);
            contractDTO.setStatus("inactive");

            logger.info("Creating contract for client {}: service={}, dates={} to {}, amount={}",
                    client.getEmail(), service.getName(),
                    contractCreateDTO.getStartDate(), contractCreateDTO.getEndDate(), calculatedAmount);

            Contract created = contractService.createContract(contractDTO);
            if (created != null) {
                logger.info("Contract created successfully for client {} with ID: {}",
                        client.getEmail(), created.getId());
                redirectAttributes.addFlashAttribute("success",
                        "Договор успешно создан и отправлен на рассмотрение. " +
                                "Стоимость: " + String.format("%.2f", calculatedAmount) + " ₽");
            } else {
                logger.error("Failed to create contract for client {}: createContract returned null",
                        client.getEmail());
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании договора");
            }
        } catch (RuntimeException e) {
            logger.error("Error creating contract for client {}", client.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/contracts";
    }

    // Метод для расчета общей стоимости
    private Double calculateTotalAmount(BigDecimal servicePrice) {
        if (servicePrice == null) {
            logger.warn("Service price is null, using default price 5000");
            servicePrice = new BigDecimal("5000.00");
        }

        try {
            BigDecimal setupCost = new BigDecimal("10000.00");
            BigDecimal total = servicePrice.add(setupCost);
            Double result = total.doubleValue();

            logger.debug("Calculating total amount: servicePrice={}, setupCost={}, total={}",
                    servicePrice, setupCost, total);

            return result;
        } catch (Exception e) {
            logger.error("Error in calculateTotalAmount", e);
            return 15000.0;
        }
    }
}