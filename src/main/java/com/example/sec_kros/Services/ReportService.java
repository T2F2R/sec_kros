package com.example.sec_kros.Services;

import com.example.sec_kros.Entities.*;
import com.example.sec_kros.Repositories.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    public byte[] generateRevenueReport(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Contract> contracts = contractRepository.findByCreatedAtBetween(startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Отчет по выручке");

        // Стили
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        // Заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID договора", "Клиент", "Услуга", "Дата начала", "Дата окончания",
                "Сумма договора", "Статус", "Дата создания"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Данные
        int rowNum = 1;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Contract contract : contracts) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(contract.getId());
            row.createCell(1).setCellValue(contract.getClient().getLastName() + " " +
                    contract.getClient().getFirstName());
            row.createCell(2).setCellValue(contract.getService().getName());

            Cell startDateCell = row.createCell(3);
            startDateCell.setCellValue(contract.getStartDate().toString());
            startDateCell.setCellStyle(dateStyle);

            Cell endDateCell = row.createCell(4);
            endDateCell.setCellValue(contract.getEndDate().toString());
            endDateCell.setCellStyle(dateStyle);

            Cell amountCell = row.createCell(5);
            if (contract.getTotalAmount() != null) {
                amountCell.setCellValue(contract.getTotalAmount().doubleValue());
                totalRevenue = totalRevenue.add(contract.getTotalAmount());
            } else {
                amountCell.setCellValue(0);
            }
            amountCell.setCellStyle(currencyStyle);

            row.createCell(6).setCellValue(contract.getStatus());

            Cell createdAtCell = row.createCell(7);
            createdAtCell.setCellValue(contract.getCreatedAt().toString());
            createdAtCell.setCellStyle(dateStyle);
        }

        // Итоговая строка
        Row totalRow = sheet.createRow(rowNum + 1);
        totalRow.createCell(4).setCellValue("ИТОГО:");
        Cell totalCell = totalRow.createCell(5);
        totalCell.setCellValue(totalRevenue.doubleValue());
        totalCell.setCellStyle(currencyStyle);

        // Авто-размер колонок
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbookToBytes(workbook);
    }

    public byte[] generateContractsReport(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Contract> contracts = contractRepository.findByCreatedAtBetween(startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Отчет по договорам");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        // Заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID договора", "Клиент", "Телефон", "Email", "Услуга",
                "Дата начала", "Дата окончания", "Статус", "Дата создания"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Данные
        int rowNum = 1;
        for (Contract contract : contracts) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(contract.getId());
            row.createCell(1).setCellValue(contract.getClient().getLastName() + " " +
                    contract.getClient().getFirstName());
            row.createCell(2).setCellValue(contract.getClient().getPhone());
            row.createCell(3).setCellValue(contract.getClient().getEmail());
            row.createCell(4).setCellValue(contract.getService().getName());

            Cell startDateCell = row.createCell(5);
            startDateCell.setCellValue(contract.getStartDate().toString());
            startDateCell.setCellStyle(dateStyle);

            Cell endDateCell = row.createCell(6);
            endDateCell.setCellValue(contract.getEndDate().toString());
            endDateCell.setCellStyle(dateStyle);

            row.createCell(7).setCellValue(contract.getStatus());

            Cell createdAtCell = row.createCell(8);
            createdAtCell.setCellValue(contract.getCreatedAt().toString());
            createdAtCell.setCellStyle(dateStyle);
        }

        // Авто-размер колонок
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbookToBytes(workbook);
    }

    public byte[] generateClientsReport(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Client> clients = clientRepository.findByCreatedAtBetween(startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Отчет по клиентам");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        // Заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID клиента", "Фамилия", "Имя", "Отчество", "Телефон",
                "Email", "Адрес", "Дата регистрации"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Данные
        int rowNum = 1;
        for (Client client : clients) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(client.getId());
            row.createCell(1).setCellValue(client.getLastName());
            row.createCell(2).setCellValue(client.getFirstName());
            row.createCell(3).setCellValue(client.getPatronymic() != null ? client.getPatronymic() : "");
            row.createCell(4).setCellValue(client.getPhone());
            row.createCell(5).setCellValue(client.getEmail());
            row.createCell(6).setCellValue(client.getAddress() != null ? client.getAddress() : "");

            Cell createdAtCell = row.createCell(7);
            createdAtCell.setCellValue(client.getCreatedAt().toString());
            createdAtCell.setCellStyle(dateStyle);
        }

        // Авто-размер колонок
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        return workbookToBytes(workbook);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("###,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("dd.mm.yyyy"));
        return style;
    }

    private byte[] workbookToBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }
}