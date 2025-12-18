package com.example.sec_kros.services;

import com.example.sec_kros.Services.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendContractApprovalEmailToClient_ShouldSendEmailWithCorrectContent() {
        // Arrange
        String clientEmail = "client@example.com";
        String clientName = "Иван Иванов";
        String contractNumber = "12345";
        String startDate = "01.01.2024";
        String endDate = "31.01.2024";
        String guardObjectName = "Офисный комплекс";
        String address = "ул. Ленина, 1";
        String employeeName = "Петр Петров";
        String shiftTime = "08:00 - 20:00";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendContractApprovalEmailToClient(
                clientEmail, clientName, contractNumber, startDate, endDate,
                guardObjectName, address, employeeName, shiftTime
        );

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isNotNull();
        assertThat(sentMessage.getTo()).containsExactly(clientEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("SEC - Ваш договор охраны одобрен");
    }

    @Test
    void sendContractApprovalEmailToEmployee_ShouldSendEmailWithCorrectContent() {
        // Arrange
        String employeeEmail = "employee@example.com";
        String employeeName = "Петр Петров";
        String contractNumber = "12345";
        String clientName = "Иван Иванов";
        String guardObjectName = "Офисный комплекс";
        String address = "ул. Ленина, 1";
        String shiftTime = "08:00 - 20:00";
        String startDate = "01.01.2024";
        String endDate = "31.01.2024";
        String additionalNotes = "Особые указания: проверять все помещения";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendContractApprovalEmailToEmployee(
                employeeEmail, employeeName, contractNumber, clientName,
                guardObjectName, address, shiftTime, startDate, endDate, additionalNotes
        );

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isNotNull();
        assertThat(sentMessage.getTo()).containsExactly(employeeEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("SEC - Новое задание по охране объекта");
    }

    @Test
    void sendContractApprovalEmailToClient_ShouldNotThrowException_WhenMailSenderFails() {
        // Arrange
        doThrow(new MailSendException("SMTP connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert - метод не должен бросать исключение
        assertThatCode(() ->
                emailService.sendContractApprovalEmailToClient(
                        "test@example.com", "Name", "123", "01.01.2024", "31.01.2024",
                        "Object", "Address", "Employee", "08:00-20:00"
                )
        ).doesNotThrowAnyException();

        // Verify that send was called despite the exception
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendContractApprovalEmailToEmployee_ShouldNotThrowException_WhenMailSenderFails() {
        // Arrange
        doThrow(new MailSendException("SMTP connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert - метод не должен бросать исключение
        assertThatCode(() ->
                emailService.sendContractApprovalEmailToEmployee(
                        "test@example.com", "Name", "123", "Client", "Object",
                        "Address", "08:00-20:00", "01.01.2024", "31.01.2024", "Notes"
                )
        ).doesNotThrowAnyException();

        // Verify that send was called despite the exception
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendContractApprovalEmailToClient_ShouldHandleMailSendException() {
        // Arrange
        MailSendException mailException = new MailSendException("Failed to send email");
        doThrow(mailException).when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendContractApprovalEmailToClient(
                "test@example.com", "Name", "123", "01.01.2024", "31.01.2024",
                "Object", "Address", "Employee", "08:00-20:00"
        );

        // Assert - не должно быть исключения
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendContractApprovalEmailToEmployee_ShouldHandleMailSendException() {
        // Arrange
        MailSendException mailException = new MailSendException("Failed to send email");
        doThrow(mailException).when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendContractApprovalEmailToEmployee(
                "test@example.com", "Name", "123", "Client", "Object",
                "Address", "08:00-20:00", "01.01.2024", "31.01.2024", "Notes"
        );

        // Assert - не должно быть исключения
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendContractApprovalEmailToClient_ShouldLogError_WhenMailFails() {
        // Arrange
        String errorMessage = "SMTP connection failed";
        MailSendException exception = new MailSendException(errorMessage);
        doThrow(exception).when(mailSender).send(any(SimpleMailMessage.class));

        // Для тестирования логирования можно использовать System.err
        // или добавить логгер в сервис. Здесь просто проверяем что не бросается исключение

        // Act & Assert
        assertThatCode(() ->
                emailService.sendContractApprovalEmailToClient(
                        "test@example.com", "Name", "123", "01.01.2024", "31.01.2024",
                        "Object", "Address", "Employee", "08:00-20:00"
                )
        ).doesNotThrowAnyException();
    }

    // Другие тесты остаются без изменений...

    @Test
    void sendContractApprovalEmailToClient_ShouldUseCorrectEmailAddress() {
        // Arrange
        String expectedEmail = "test@example.com";
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(captor.capture());

        // Act
        emailService.sendContractApprovalEmailToClient(
                expectedEmail, "Name", "123", "01.01.2024", "31.01.2024",
                "Object", "Address", "Employee", "08:00-20:00"
        );

        // Assert
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly(expectedEmail);
    }

    @Test
    void sendContractApprovalEmailToEmployee_ShouldUseCorrectEmailAddress() {
        // Arrange
        String expectedEmail = "test@example.com";
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(captor.capture());

        // Act
        emailService.sendContractApprovalEmailToEmployee(
                expectedEmail, "Name", "123", "Client", "Object",
                "Address", "08:00-20:00", "01.01.2024", "31.01.2024", "Notes"
        );

        // Assert
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly(expectedEmail);
    }
}