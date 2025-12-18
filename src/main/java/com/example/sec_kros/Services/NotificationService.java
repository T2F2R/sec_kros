package com.example.sec_kros.Services;

import com.example.sec_kros.Entities.Notification;
import com.example.sec_kros.Repositories.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public List<Notification> getNotificationsByClientId(Long clientId) {
        return notificationRepository.findByClientIdOrderBySentAtDesc(clientId);
    }

    public List<Notification> getUnreadNotificationsByClientId(Long clientId) {
        return notificationRepository.findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId);
    }

    public void markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    public void markAllAsReadByClientId(Long clientId) {
        List<Notification> unreadNotifications = getUnreadNotificationsByClientId(clientId);
        if (!unreadNotifications.isEmpty()) { // Добавляем проверку на пустой список
            for (Notification notification : unreadNotifications) {
                notification.setIsRead(true);
            }
            notificationRepository.saveAll(unreadNotifications);
        }
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    public int getUnreadCountByClientId(Long clientId) {
        return notificationRepository.countByClientIdAndIsReadFalse(clientId);
    }
}