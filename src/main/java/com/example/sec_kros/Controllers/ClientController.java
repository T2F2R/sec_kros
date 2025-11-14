package com.example.sec_kros.Controllers;

import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Contract;
import com.example.sec_kros.Entities.GuardObject;
import com.example.sec_kros.Entities.Notification;
import com.example.sec_kros.Services.ClientService;
import com.example.sec_kros.Services.ContractService;
import com.example.sec_kros.Services.GuardObjectService;
import com.example.sec_kros.Services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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

    // Вспомогательный метод для получения текущего клиента
    private Client getCurrentClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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

    @GetMapping("/objects")
    public String clientObjects(Model model) {
        Client client = getCurrentClient();
        if (client == null) {
            return "redirect:/login";
        }

        List<GuardObject> objects = guardObjectService.getGuardObjectsByClientId(client.getId());
        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByClientId(client.getId());

        model.addAttribute("client", client);
        model.addAttribute("objects", objects);
        model.addAttribute("unreadCount", unreadNotifications.size());

        return "client/objects";
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
}