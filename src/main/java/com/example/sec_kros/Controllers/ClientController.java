package com.example.sec_kros.Controllers;

import com.example.sec_kros.DTO.ContractCreateDTO;
import com.example.sec_kros.DTO.ContractDTO;
import com.example.sec_kros.DTO.GuardObjectDTO;
import com.example.sec_kros.Entities.*;
import com.example.sec_kros.Services.*;
import jakarta.validation.Valid;
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
            return null;
        }
        String username = authentication.getName();
        return clientService.findByEmail(username);
    }

    @GetMapping("/dashboard")
    public String clientDashboard(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());
        List<Contract> contracts = contractService.getContractsByClientId(client.getId());
        List<GuardObject> objects = guardObjectService.getGuardObjectsByClientId(client.getId());

        model.addAttribute("client", client);
        model.addAttribute("unreadCount", unreadNotifications.size());
        model.addAttribute("contractsCount", contracts.size());
        model.addAttribute("objectsCount", objects.size());

        return "client/dashboard";
    }

    @GetMapping("/notifications")
    public String clientNotifications(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        List<Notification> notifications = notificationService.getNotificationsByClientId(client.getId());
        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

        model.addAttribute("client", client);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadNotifications.size());

        return "client/notifications";
    }

    @GetMapping("/contracts")
    public String clientContracts(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        List<Contract> contracts = contractService.getContractsByClientId(client.getId());
        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

        model.addAttribute("client", client);
        model.addAttribute("contracts", contracts);
        model.addAttribute("unreadCount", unreadNotifications.size());

        return "client/contracts";
    }

    @GetMapping("/profile")
    public String clientProfile(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

        model.addAttribute("client", client);
        model.addAttribute("unreadCount", unreadNotifications.size());

        return "client/profile";
    }

    @PostMapping("/notifications/mark-as-read/{id}")
    public String markNotificationAsRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(id);
            redirectAttributes.addFlashAttribute("success", "Уведомление отмечено как прочитанное");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении уведомления");
        }
        return "redirect:/client/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsAsRead(RedirectAttributes redirectAttributes) {
        Client client = getCurrentClient();
        if (client != null) {
            notificationService.markAllAsReadByClientId(client.getId());
            redirectAttributes.addFlashAttribute("success", "Все уведомления отмечены как прочитанные");
        }
        return "redirect:/client/notifications";
    }

    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.deleteNotification(id);
            redirectAttributes.addFlashAttribute("success", "Уведомление удалено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении уведомления");
        }
        return "redirect:/client/notifications";
    }

    @GetMapping("/objects/create")
    public String showCreateObjectForm(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

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

        return "client/objects-create";
    }

    @PostMapping("/objects/create")
    public String createObject(@Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        // ВАЖНО: Устанавливаем clientId из текущего клиента, а не из DTO
        guardObjectDTO.setClientId(client.getId());

        // Проверяем, что клиент имеет доступ к выбранному договору
        Optional<Contract> contractOptional = contractService.getContractById(guardObjectDTO.getContractId());
        if (contractOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не найден");
            return "redirect:/client/objects/create";
        }

        Contract contract = contractOptional.get();
        if (!contract.getClient().getId().equals(client.getId())) {
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не принадлежит вам");
            return "redirect:/client/objects/create";
        }

        // Проверяем, что договор имеет статус "inactive"
        if (!"inactive".equals(contract.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Объект можно привязать только к договору на рассмотрении (статус 'Неактивен')");
            return "redirect:/client/objects/create";
        }

        if (bindingResult.hasErrors()) {
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
                redirectAttributes.addFlashAttribute("success", "Объект успешно создан");
            } else {
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании объекта");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/objects";
    }

    @GetMapping("/objects/edit/{id}")
    public String showEditObjectForm(@PathVariable Long id, Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        Optional<GuardObject> guardObject = guardObjectService.getGuardObjectById(id);
        if (guardObject.isEmpty()) {
            return "redirect:/client/objects";
        }

        // Проверяем, что объект принадлежит клиенту
        if (!guardObject.get().getClient().getId().equals(client.getId())) {
            return "redirect:/client/objects";
        }

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

        return "client/objects-edit";
    }

    @PostMapping("/objects/edit/{id}")
    public String updateObject(@PathVariable Long id,
                               @Valid @ModelAttribute GuardObjectDTO guardObjectDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        // Проверяем, что объект принадлежит клиенту
        Optional<GuardObject> existingObject = guardObjectService.getGuardObjectById(id);
        if (existingObject.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Объект не найден");
            return "redirect:/client/objects";
        }

        if (!existingObject.get().getClient().getId().equals(client.getId())) {
            redirectAttributes.addFlashAttribute("error", "Недостаточно прав для редактирования объекта");
            return "redirect:/client/objects";
        }

        // Устанавливаем clientId из текущего клиента
        guardObjectDTO.setClientId(client.getId());
        guardObjectDTO.setId(id); // Устанавливаем ID для обновления

        // Проверяем доступ к договору
        Optional<Contract> contractOptional = contractService.getContractById(guardObjectDTO.getContractId());
        if (contractOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не найден");
            return "redirect:/client/objects/edit/" + id;
        }

        Contract contract = contractOptional.get();
        if (!contract.getClient().getId().equals(client.getId())) {
            redirectAttributes.addFlashAttribute("error", "Выбранный договор не принадлежит вам");
            return "redirect:/client/objects/edit/" + id;
        }

        if (bindingResult.hasErrors()) {
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
                redirectAttributes.addFlashAttribute("success", "Объект успешно обновлен");
            } else {
                redirectAttributes.addFlashAttribute("error", "Объект не найден");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/objects";
    }

    @PostMapping("/objects/delete/{id}")
    public String deleteObject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        // Проверяем, что объект принадлежит клиенту
        Optional<GuardObject> guardObject = guardObjectService.getGuardObjectById(id);
        if (guardObject.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Объект не найден");
            return "redirect:/client/objects";
        }

        if (!guardObject.get().getClient().getId().equals(client.getId())) {
            redirectAttributes.addFlashAttribute("error", "Недостаточно прав для удаления объекта");
            return "redirect:/client/objects";
        }

        // Проверяем возможность удаления
        boolean canDelete = guardObjectService.canDeleteGuardObject(id);

        if (!canDelete) {
            redirectAttributes.addFlashAttribute("error",
                    "Невозможно удалить объект: существуют связанные расписания");
            return "redirect:/client/objects";
        }

        if (guardObjectService.deleteGuardObject(id)) {
            redirectAttributes.addFlashAttribute("success", "Объект успешно удален");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении объекта");
        }
        return "redirect:/client/objects";
    }

    @GetMapping("/objects")
    public String clientObjects(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

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

        return "client/objects";
    }

    @GetMapping("/contracts/create")
    public String showCreateContractForm(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());
        List<ServiceEntity> services = serviceService.getAllServices();

        model.addAttribute("client", client);
        model.addAttribute("unreadCount", unreadNotifications.size());
        model.addAttribute("services", services);
        model.addAttribute("contractCreateDTO", new ContractCreateDTO());

        return "client/contracts-create";
    }

    @PostMapping("/contracts/create")
    public String createContract(@Valid @ModelAttribute ContractCreateDTO contractCreateDTO,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            System.out.println("=== ОШИБКИ ВАЛИДАЦИИ ===");
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println("Error: " + error.getDefaultMessage());
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
                redirectAttributes.addFlashAttribute("error", "Выбранная услуга не найдена");
                return "redirect:/client/contracts/create";
            }

            ServiceEntity service = serviceOptional.get();

            // Автоматически рассчитываем стоимость
            Double calculatedAmount = calculateTotalAmount(service.getPrice());
            contractCreateDTO.setTotalAmount(calculatedAmount);

            System.out.println("=== РАСЧЕТ СТОИМОСТИ ===");
            System.out.println("Стоимость услуги: " + service.getPrice());
            System.out.println("Расчетная стоимость договора: " + calculatedAmount);

            // Преобразуем ContractCreateDTO в ContractDTO
            ContractDTO contractDTO = new ContractDTO();
            contractDTO.setClientId(client.getId());
            contractDTO.setServiceId(contractCreateDTO.getServiceId());
            contractDTO.setStartDate(contractCreateDTO.getStartDate());
            contractDTO.setEndDate(contractCreateDTO.getEndDate());
            contractDTO.setTotalAmount(calculatedAmount);
            contractDTO.setStatus("inactive");

            System.out.println("=== СОЗДАНИЕ ДОГОВОРА ===");
            Contract created = contractService.createContract(contractDTO);
            if (created != null) {
                System.out.println("Договор успешно создан с ID: " + created.getId());
                redirectAttributes.addFlashAttribute("success",
                        "Договор успешно создан и отправлен на рассмотрение. " +
                                "Стоимость: " + String.format("%.2f", calculatedAmount) + " ₽");
            } else {
                System.out.println("Ошибка: createContract вернул null");
                redirectAttributes.addFlashAttribute("error", "Ошибка при создании договора");
            }
        } catch (RuntimeException e) {
            System.out.println("=== ИСКЛЮЧЕНИЕ ===");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/client/contracts";
    }

    // Метод для расчета общей стоимости
    private Double calculateTotalAmount(BigDecimal servicePrice) {
        if (servicePrice == null) {
            System.out.println("Service price is null! Using default price 5000");
            servicePrice = new BigDecimal("5000.00");
        }

        try {
            BigDecimal setupCost = new BigDecimal("10000.00");
            BigDecimal total = servicePrice.add(setupCost);
            Double result = total.doubleValue();

            System.out.println("=== РАСЧЕТ СТОИМОСТИ ===");
            System.out.println("Стоимость услуги: " + servicePrice);
            System.out.println("Стоимость оформления: " + setupCost);
            System.out.println("Общая стоимость: " + total);
            System.out.println("Результат (Double): " + result);

            return result;
        } catch (Exception e) {
            System.out.println("Error in calculateTotalAmount: " + e.getMessage());
            return 15000.0;
        }
    }
}