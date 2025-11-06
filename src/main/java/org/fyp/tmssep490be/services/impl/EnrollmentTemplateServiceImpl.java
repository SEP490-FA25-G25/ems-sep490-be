package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.services.EnrollmentTemplateService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Service for generating Excel templates for student enrollment
 * Simplified 7-column format: full_name, email, phone, facebook_url, address, gender, dob
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentTemplateServiceImpl implements EnrollmentTemplateService {

    private final ClassRepository classRepository;

    @Override
    public byte[] generateExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Student Enrollment Template");

            // Create header row with 7 columns
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "full_name *",        // Required
                "email *",           // Required
                "phone *",           // Required
                "facebook_url",      // Optional
                "address",           // Optional
                "gender *",          // Required (male/female/other)
                "dob *"              // Required (yyyy-MM-dd format)
            };

            // Style headers
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add sample data rows
            addSampleData(sheet);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate Excel template", e);
            throw new RuntimeException("Failed to generate Excel template", e);
        }
    }

    @Override
    public byte[] generateExcelTemplateWithClassInfo(Long classId) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with ID: " + classId));

            String className = classEntity.getName();
            String courseName = classEntity.getCourse() != null ? classEntity.getCourse().getName() : "Unknown Course";

            Sheet sheet = workbook.createSheet(className + " - Enrollment");

            // Add class information row (row 0)
            Row infoRow = sheet.createRow(0);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue("Class: " + className + " | Course: " + courseName);
            CellStyle infoStyle = createInfoStyle(workbook);
            infoCell.setCellStyle(infoStyle);

            // Merge cells for class info
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 6)); // 7 columns (0-6)

            // Create header row (row 1)
            Row headerRow = sheet.createRow(1);
            String[] headers = {
                "full_name *",        // Required
                "email *",           // Required
                "phone *",           // Required
                "facebook_url",      // Optional
                "address",           // Optional
                "gender *",          // Required (male/female/other)
                "dob *"              // Required (yyyy-MM-dd format)
            };

            // Style headers
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add sample data starting from row 2 (parser will skip row 0 & 1 for class-specific templates)
            addSampleData(sheet, 2);

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate class-specific Excel template for classId: {}", classId, e);
            throw new RuntimeException("Failed to generate class-specific Excel template", e);
        }
    }

    /**
     * Add sample data to the sheet starting from row 2
     */
    private void addSampleData(Sheet sheet) {
        addSampleData(sheet, 2);
    }

    /**
     * Add sample data to the sheet starting from specified row
     */
    private void addSampleData(Sheet sheet, int startRow) {
        String[][] sampleData = {
            {
                "Nguyễn Văn An",           // full_name
                "an.nguyen@email.com",    // email
                "0912345678",             // phone
                "https://facebook.com/an.nguyen", // facebook_url
                "123 Đường ABC, Quận 1, TP.HCM", // address
                "male",                   // gender
                "2000-05-15"              // dob
            },
            {
                "Trần Thị Bình",           // full_name
                "binh.tran@email.com",    // email
                "0987654321",             // phone
                "",                       // facebook_url (optional)
                "456 Đường XYZ, Quận 3, TP.HCM", // address
                "female",                 // gender
                "2001-08-22"              // dob
            },
            {
                "Lê Văn Cường",           // full_name
                "cuong.le@email.com",     // email
                "0901234567",             // phone
                "https://facebook.com/cuong.le", // facebook_url
                "",                       // address (optional)
                "male",                   // gender
                "1999-12-10"              // dob
            }
        };

        // Add sample data rows
        for (int i = 0; i < sampleData.length; i++) {
            Row row = sheet.createRow(startRow + i);
            for (int j = 0; j < sampleData[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(sampleData[i][j]);
            }
        }
    }

    /**
     * Create header cell style with bold and background color
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * Create info cell style for class information
     */
    private CellStyle createInfoStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }
}