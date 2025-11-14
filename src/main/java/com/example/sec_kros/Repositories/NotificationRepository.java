package com.example.sec_kros.Repositories;

import com.example.sec_kros.Entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByClientId(Long clientId);
    List<Notification> findByEmployeeId(Long employeeId);
    List<Notification> findByIsReadFalse();
    List<Notification> findByClientIdOrderBySentAtDesc(Long clientId);

    List<Notification> findByEmployeeIdOrderBySentAtDesc(Long employeeId);

    List<Notification> findByClientIdAndIsReadFalseOrderBySentAtDesc(Long clientId);

    List<Notification> findByEmployeeIdAndIsReadFalseOrderBySentAtDesc(Long employeeId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.client.id = :clientId AND n.isRead = false")
    int countByClientIdAndIsReadFalse(@Param("clientId") Long clientId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.employee.id = :employeeId AND n.isRead = false")
    int countByEmployeeIdAndIsReadFalse(@Param("employeeId") Long employeeId);
}