package org.fyp.tmssep490be.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fyp.tmssep490be.dtos.enrollment.StudentEnrollmentData;
import org.fyp.tmssep490be.dtos.enrollment.StudentResolutionStatus;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.services.ExcelParserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation của ExcelParserService
 * Parse Excel file với format:
 * student_code, full_name, email, phone, facebook_url, address, gender, dob, general, reading, writing, speaking, listening
 * Skill format: "Level-Score" (e.g., "B1-75")
 */
@Service
@Slf4j
public class ExcelParserServiceImpl implements ExcelParserService {

    private static final int COLUMN_STUDENT_CODE = 0;
    private static final int COLUMN_FULL_NAME = 1;
    private static final int COLUMN_EMAIL = 2;
    private static final int COLUMN_PHONE = 3;
    private static final int COLUMN_FACEBOOK_URL = 4;
    private static final int COLUMN_ADDRESS = 5;
    private static final int COLUMN_GENDER = 6;
    private static final int COLUMN_DOB = 7;
    private static final int COLUMN_GENERAL = 8;
    private static final int COLUMN_READING = 9;
    private static final int COLUMN_WRITING = 10;
    private static final int COLUMN_SPEAKING = 11;
    private static final int COLUMN_LISTENING = 12;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    @Override
    public List<StudentEnrollmentData> parseStudentEnrollment(MultipartFile file) {
        List<StudentEnrollmentData> students = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    StudentEnrollmentData data = parseRow(row, i);
                    students.add(data);
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", i + 1, e.getMessage());
                    // Mark row có lỗi
                    StudentEnrollmentData errorData = StudentEnrollmentData.builder()
                            .status(StudentResolutionStatus.ERROR)
                            .errorMessage("Row " + (i + 1) + ": " + e.getMessage())
                            .build();
                    students.add(errorData);
                }
            }

            if (students.isEmpty()) {
                throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
            }

        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new CustomException(ErrorCode.EXCEL_PARSE_FAILED);
        }

        return students;
    }

    /**
     * Parse một row thành StudentEnrollmentData
     */
    private StudentEnrollmentData parseRow(Row row, int rowIndex) {
        return StudentEnrollmentData.builder()
                .studentCode(getCellValueAsString(row.getCell(COLUMN_STUDENT_CODE)))
                .fullName(getCellValueAsString(row.getCell(COLUMN_FULL_NAME)))
                .email(getCellValueAsString(row.getCell(COLUMN_EMAIL)))
                .phone(getCellValueAsString(row.getCell(COLUMN_PHONE)))
                .facebookUrl(getCellValueAsString(row.getCell(COLUMN_FACEBOOK_URL)))
                .address(getCellValueAsString(row.getCell(COLUMN_ADDRESS)))
                .gender(parseGender(getCellValueAsString(row.getCell(COLUMN_GENDER))))
                .dob(parseDob(getCellValueAsString(row.getCell(COLUMN_DOB))))
                .general(getCellValueAsString(row.getCell(COLUMN_GENERAL)))
                .reading(getCellValueAsString(row.getCell(COLUMN_READING)))
                .writing(getCellValueAsString(row.getCell(COLUMN_WRITING)))
                .speaking(getCellValueAsString(row.getCell(COLUMN_SPEAKING)))
                .listening(getCellValueAsString(row.getCell(COLUMN_LISTENING)))
                .build();
    }

    /**
     * Get cell value as String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> {
                String value = cell.getStringCellValue().trim();
                yield value.isEmpty() ? null : value;
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Date cell
                    LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield date.toString();
                } else {
                    // Numeric cell - convert to string without decimal
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> null;
            default -> cell.toString().trim();
        };
    }

    /**
     * Parse gender string to Gender enum
     */
    private Gender parseGender(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // Support: "male", "MALE", "Male", "m", "M"
            String normalized = value.toLowerCase().trim();
            return switch (normalized) {
                case "male", "m" -> Gender.MALE;
                case "female", "f" -> Gender.FEMALE;
                case "other", "o" -> Gender.OTHER;
                default -> throw new IllegalArgumentException("Invalid gender value: " + value);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid gender: " + value);
        }
    }

    /**
     * Parse date string to LocalDate
     * Support multiple formats: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy
     */
    private LocalDate parseDob(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        throw new IllegalArgumentException("Invalid date format: " + value + ". Expected: yyyy-MM-dd, dd/MM/yyyy, or MM/dd/yyyy");
    }

    /**
     * Check if row is empty
     */
    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
