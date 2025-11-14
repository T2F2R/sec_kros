package com.example.sec_kros.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendContractApprovalEmailToClient(String clientEmail, String clientName,
                                                  String contractNumber, String startDate,
                                                  String endDate, String guardObjectName,
                                                  String address, String employeeName,
                                                  String shiftTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(clientEmail);
        message.setSubject("SEC - Ваш договор охраны одобрен");
        message.setText(
                "Уважаемый(ая) " + clientName + "!\n\n" +
                        "Ваш договор №" + contractNumber + " на охрану объекта был одобрен и вступает в силу.\n\n" +
                        "Детали договора:\n" +
                        "• Номер договора: " + contractNumber + "\n" +
                        "• Объект охраны: " + guardObjectName + "\n" +
                        "• Адрес: " + address + "\n" +
                        "• Ответственный охранник: " + employeeName + "\n" +
                        "• Время охраны: " + shiftTime + "\n" +
                        "• Дата начала: " + startDate + "\n" +
                        "• Дата окончания: " + endDate + "\n\n" +
                        "Охрана объекта будет осуществляться согласно утвержденному графику.\n" +
                        "По всем вопросам обращайтесь в нашу службу поддержки.\n\n" +
                        "С уважением,\n" +
                        "Служба безопасности SEC\n" +
                        "Телефон: +7 (XXX) XXX-XX-XX\n" +
                        "Email: info@sec-kros.ru"
        );

        mailSender.send(message);
    }

    public void sendContractApprovalEmailToEmployee(String employeeEmail, String employeeName,
                                                    String contractNumber, String clientName,
                                                    String guardObjectName, String address,
                                                    String shiftTime, String startDate,
                                                    String endDate, String additionalNotes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(employeeEmail);
        message.setSubject("SEC - Новое задание по охране объекта");
        message.setText(
                "Уважаемый(ая) " + employeeName + "!\n\n" +
                        "Вам назначено новое задание по охране объекта.\n\n" +
                        "Детали задания:\n" +
                        "• Договор №: " + contractNumber + "\n" +
                        "• Клиент: " + clientName + "\n" +
                        "• Объект: " + guardObjectName + "\n" +
                        "• Адрес: " + address + "\n" +
                        "• Время смены: " + shiftTime + "\n" +
                        "• Период охраны: с " + startDate + " по " + endDate + "\n" +
                        "• Дополнительные указания: " + additionalNotes + "\n\n" +
                        "Пожалуйста, ознакомьтесь с графиком работы в системе и приступайте к выполнению обязанностей " +
                        "в указанное время. Не забудьте отметить начало и окончание смены в мобильном приложении.\n\n" +
                        "С уважением,\n" +
                        "Администрация SEC\n" +
                        "Телефон координатора: +7 (XXX) XXX-XX-XX"
        );

        mailSender.send(message);
    }
}