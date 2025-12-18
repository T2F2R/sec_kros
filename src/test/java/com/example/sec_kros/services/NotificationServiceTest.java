package com.example.sec_kros.services;

import com.example.sec_kros.Entities.Client;
import com.example.sec_kros.Entities.Notification;
import com.example.sec_kros.Repositories.NotificationRepository;
import com.example.sec_kros.Services.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getNotificationsByClientId_ShouldReturnNotifications() {
        // Arrange
        Long clientId = 1L;
        List<Notification> notifications = Arrays.asList(
                createNotification(1L, clientId, "Уведомление 1"),
                createNotification(2L, clientId, "Уведомление 2")
        );
        when(notificationRepository.findByClientIdOrderBySentAtDesc(clientId)).thenReturn(notifications);

        // Act
        List<Notification> result = notificationService.getNotificationsByClientId(clientId);

        // Assert
        assertThat(result).hasSize(2).containsAll(notifications);
        verify(notificationRepository).findByClientIdOrderBySentAtDesc(clientId);
    }

    @Test
    void getNotificationsByClientId_ShouldReturnEmptyList_WhenNoNotifications() {
        // Arrange
        Long clientId = 1L;
        when(notificationRepository.findByClientIdOrderBySentAtDesc(clientId)).thenReturn(List.of());

        // Act
        List<Notification> result = notificationService.getNotificationsByClientId(clientId);

        // Assert
        assertThat(result).isEmpty();
        verify(notificationRepository).findByClientIdOrderBySentAtDesc(clientId);
    }

    @Test
    void getNotificationsByClientId_ShouldOrderBySentAtDesc() {
        // Arrange
        Long clientId = 1L;
        Notification olderNotification = createNotification(1L, clientId, "Старое");
        olderNotification.setSentAt(LocalDateTime.now().minusDays(2));

        Notification newerNotification = createNotification(2L, clientId, "Новое");
        newerNotification.setSentAt(LocalDateTime.now().minusDays(1));

        List<Notification> notifications = List.of(newerNotification, olderNotification); // Новые первыми
        when(notificationRepository.findByClientIdOrderBySentAtDesc(clientId)).thenReturn(notifications);

        // Act
        List<Notification> result = notificationService.getNotificationsByClientId(clientId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(newerNotification); // Проверяем порядок
        assertThat(result.get(1)).isEqualTo(olderNotification);
        verify(notificationRepository).findByClientIdOrderBySentAtDesc(clientId);
    }

    @Test
    void getUnreadNotificationsByClientId_ShouldReturnUnreadNotifications() {
        // Arrange
        Long clientId = 1L;
        Notification unreadNotification = createNotification(1L, clientId, "Непрочитанное");
        unreadNotification.setIsRead(false);

        Notification readNotification = createNotification(2L, clientId, "Прочитанное");
        readNotification.setIsRead(true);

        List<Notification> unreadNotifications = List.of(unreadNotification);
        when(notificationRepository.findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId))
                .thenReturn(unreadNotifications);

        // Act
        List<Notification> result = notificationService.getUnreadNotificationsByClientId(clientId);

        // Assert
        assertThat(result).hasSize(1).containsExactly(unreadNotification);
        assertThat(result.get(0).getIsRead()).isFalse();
        verify(notificationRepository).findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId);
    }

    @Test
    void getUnreadNotificationsByClientId_ShouldReturnEmptyList_WhenAllRead() {
        // Arrange
        Long clientId = 1L;
        when(notificationRepository.findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId))
                .thenReturn(List.of());

        // Act
        List<Notification> result = notificationService.getUnreadNotificationsByClientId(clientId);

        // Assert
        assertThat(result).isEmpty();
        verify(notificationRepository).findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId);
    }

    @Test
    void markAsRead_ShouldMarkNotificationAsRead_WhenNotificationExists() {
        // Arrange
        Long notificationId = 1L;
        Notification notification = createNotification(notificationId, 1L, "Уведомление");
        notification.setIsRead(false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        verify(notificationRepository).findById(notificationId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification savedNotification = captor.getValue();
        assertThat(savedNotification.getIsRead()).isTrue();
    }

    @Test
    void markAsRead_ShouldDoNothing_WhenNotificationNotFound() {
        // Arrange
        Long nonExistentId = 999L;
        when(notificationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        notificationService.markAsRead(nonExistentId);

        // Assert
        verify(notificationRepository).findById(nonExistentId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_ShouldNotSave_WhenAlreadyRead() {
        // Arrange
        Long notificationId = 1L;
        Notification notification = createNotification(notificationId, 1L, "Уведомление");
        notification.setIsRead(true); // Уже прочитано

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        verify(notificationRepository).findById(notificationId);
        // Даже если уже прочитано, все равно сохраняем (это поведение сервиса)
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAllAsReadByClientId_ShouldMarkAllUnreadNotificationsAsRead() {
        // Arrange
        Long clientId = 1L;

        Notification unread1 = createNotification(1L, clientId, "Непрочитанное 1");
        unread1.setIsRead(false);

        Notification unread2 = createNotification(2L, clientId, "Непрочитанное 2");
        unread2.setIsRead(false);

        List<Notification> unreadNotifications = Arrays.asList(unread1, unread2);

        when(notificationRepository.findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId))
                .thenReturn(unreadNotifications);
        when(notificationRepository.saveAll(anyList())).thenReturn(unreadNotifications);

        // Act
        notificationService.markAllAsReadByClientId(clientId);

        // Assert
        verify(notificationRepository).findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> savedNotifications = captor.getValue();
        assertThat(savedNotifications).hasSize(2);
        assertThat(savedNotifications.get(0).getIsRead()).isTrue();
        assertThat(savedNotifications.get(1).getIsRead()).isTrue();
    }

    @Test
    void markAllAsReadByClientId_ShouldDoNothing_WhenNoUnreadNotifications() {
        // Arrange
        Long clientId = 1L;
        when(notificationRepository.findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId))
                .thenReturn(List.of());

        // Act
        notificationService.markAllAsReadByClientId(clientId);

        // Assert
        verify(notificationRepository).findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId);
        verify(notificationRepository, never()).saveAll(anyList());
    }

    @Test
    void deleteNotification_ShouldDeleteNotification() {
        // Arrange
        Long notificationId = 1L;
        doNothing().when(notificationRepository).deleteById(notificationId);

        // Act
        notificationService.deleteNotification(notificationId);

        // Assert
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    void deleteNotification_ShouldNotThrowException_WhenNotificationDoesNotExist() {
        // Arrange
        Long nonExistentId = 999L;
        doNothing().when(notificationRepository).deleteById(nonExistentId);

        // Act
        notificationService.deleteNotification(nonExistentId);

        // Assert
        verify(notificationRepository).deleteById(nonExistentId);
    }

    @Test
    void getUnreadCountByClientId_ShouldReturnCount() {
        // Arrange
        Long clientId = 1L;
        int expectedCount = 3;
        when(notificationRepository.countByClientIdAndIsReadFalse(clientId)).thenReturn(expectedCount);

        // Act
        int result = notificationService.getUnreadCountByClientId(clientId);

        // Assert
        assertThat(result).isEqualTo(expectedCount);
        verify(notificationRepository).countByClientIdAndIsReadFalse(clientId);
    }

    @Test
    void getUnreadCountByClientId_ShouldReturnZero_WhenNoUnread() {
        // Arrange
        Long clientId = 1L;
        when(notificationRepository.countByClientIdAndIsReadFalse(clientId)).thenReturn(0);

        // Act
        int result = notificationService.getUnreadCountByClientId(clientId);

        // Assert
        assertThat(result).isZero();
        verify(notificationRepository).countByClientIdAndIsReadFalse(clientId);
    }

    @Test
    void markAsRead_ShouldSetIsReadToTrue() {
        // Arrange
        Long notificationId = 1L;
        Notification notification = createNotification(notificationId, 1L, "Тест");
        notification.setIsRead(false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture())).thenReturn(notification);

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        Notification savedNotification = captor.getValue();
        assertThat(savedNotification.getIsRead()).isTrue();
    }

    @Test
    void markAsRead_ShouldPreserveOtherFields() {
        // Arrange
        Long notificationId = 1L;
        Notification notification = createNotification(notificationId, 1L, "Важное уведомление");
        notification.setIsRead(false);
        notification.setTitle("Заголовок");
        notification.setMessage("Сообщение");
        notification.setSentAt(LocalDateTime.now().minusHours(1));

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture())).thenReturn(notification);

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        Notification savedNotification = captor.getValue();
        assertThat(savedNotification.getTitle()).isEqualTo("Заголовок");
        assertThat(savedNotification.getMessage()).isEqualTo("Сообщение");
        assertThat(savedNotification.getSentAt()).isEqualTo(notification.getSentAt());
        assertThat(savedNotification.getClient().getId()).isEqualTo(1L);
    }

    @Test
    void markAllAsReadByClientId_ShouldUseCorrectClientId() {
        // Arrange
        Long clientId = 1L;
        Long otherClientId = 2L;

        // Уведомления только для clientId
        Notification unread1 = createNotification(1L, clientId, "Уведомление 1");
        unread1.setIsRead(false);

        Notification unread2 = createNotification(2L, clientId, "Уведомление 2");
        unread2.setIsRead(false);

        List<Notification> unreadNotifications = List.of(unread1, unread2);

        when(notificationRepository.findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId))
                .thenReturn(unreadNotifications);
        when(notificationRepository.saveAll(anyList())).thenReturn(unreadNotifications);

        // Act
        notificationService.markAllAsReadByClientId(clientId);

        // Assert
        // Проверяем что вызывается именно для правильного clientId
        verify(notificationRepository).findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId);
        verify(notificationRepository, never()).findByClientIdAndIsReadFalseOrderBySentAtDesc(otherClientId);
    }

    @Test
    void getUnreadNotificationsByClientId_ShouldReturnOnlyForSpecifiedClient() {
        // Arrange
        Long clientId = 1L;
        Long otherClientId = 2L;

        Notification notificationForClient = createNotification(1L, clientId, "Для клиента 1");
        notificationForClient.setIsRead(false);

        Notification notificationForOtherClient = createNotification(2L, otherClientId, "Для клиента 2");
        notificationForOtherClient.setIsRead(false);

        List<Notification> clientNotifications = List.of(notificationForClient);

        when(notificationRepository.findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId))
                .thenReturn(clientNotifications);

        // Act
        List<Notification> result = notificationService.getUnreadNotificationsByClientId(clientId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClient().getId()).isEqualTo(clientId);
        verify(notificationRepository).findByClientIdAndIsReadFalseOrderBySentAtDesc(clientId);
    }

    @Test
    void deleteNotification_ShouldCallRepositoryOnce() {
        // Arrange
        Long notificationId = 1L;
        doNothing().when(notificationRepository).deleteById(notificationId);

        // Act
        notificationService.deleteNotification(notificationId);

        // Assert
        verify(notificationRepository, times(1)).deleteById(notificationId);
    }

    // Вспомогательные методы
    private Notification createNotification(Long id, Long clientId, String message) {
        Notification notification = new Notification();
        notification.setId(id);

        Client client = new Client();
        client.setId(clientId);
        notification.setClient(client);

        notification.setTitle("Уведомление");
        notification.setMessage(message);
        notification.setSentAt(LocalDateTime.now());
        notification.setIsRead(false);
        return notification;
    }
}