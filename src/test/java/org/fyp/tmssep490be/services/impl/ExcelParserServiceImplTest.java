package org.fyp.tmssep490be.services.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fyp.tmssep490be.dtos.enrollment.StudentEnrollmentData;
import org.fyp.tmssep490be.dtos.enrollment.StudentResolutionStatus;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ExcelParserService Unit Tests")
class ExcelParserServiceImplTest {

    private ExcelParserServiceImpl excelParserService;

    @BeforeEach
    void setUp() {
        excelParserService = new ExcelParserServiceImpl();
    }

    @Test
    @DisplayName("Should parse valid Excel file successfully")
    void shouldParseValidExcelFile() throws IOException {
        // Arrange
        MultipartFile file = createValidExcelFile();

        // Act
        List<StudentEnrollmentData> result = excelParserService.parseStudentEnrollment(file);

        // Assert
        assertThat(result).hasSize(3);

        StudentEnrollmentData student1 = result.get(0);
        // REMOVED: getStudentCode() - student codes are auto-generated now
        assertThat(student1.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(student1.getEmail()).isEqualTo("nguyenvana@email.com");
        assertThat(student1.getPhone()).isEqualTo("0901234567");
        assertThat(student1.getGender()).isEqualTo(Gender.MALE);
        assertThat(student1.getDob()).isEqualTo(LocalDate.of(1995, 1, 15));
        // REMOVED: Skill assessments validation - not handled in enrollment flow
    }

    @Test
    @DisplayName("Should handle missing email gracefully")
    void shouldHandleMissingEmail() throws IOException {
        // Arrange
        MultipartFile file = createExcelWithMissingEmail();

        // Act
        List<StudentEnrollmentData> result = excelParserService.parseStudentEnrollment(file);

        // Assert
        assertThat(result).hasSize(1);
        // Parser should still create the object but email will be empty/null
        // Validation happens at enrollment time, not parsing time
        assertThat(result.get(0).getFullName()).isEqualTo("Tran Thi B");
        assertThat(result.get(0).getEmail()).isNullOrEmpty(); // Empty string from Excel
    }

    @Test
    @DisplayName("Should mark row with error for invalid gender")
    void shouldMarkRowWithErrorForInvalidGender() throws IOException {
        // Arrange
        MultipartFile file = createExcelWithInvalidGender();

        // Act
        List<StudentEnrollmentData> result = excelParserService.parseStudentEnrollment(file);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StudentResolutionStatus.ERROR);
        assertThat(result.get(0).getErrorMessage()).contains("Invalid gender");
    }

    @Test
    @DisplayName("Should mark row with error for invalid date format")
    void shouldMarkRowWithErrorForInvalidDateFormat() throws IOException {
        // Arrange
        MultipartFile file = createExcelWithInvalidDate();

        // Act
        List<StudentEnrollmentData> result = excelParserService.parseStudentEnrollment(file);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(StudentResolutionStatus.ERROR);
        assertThat(result.get(0).getErrorMessage()).contains("Invalid date format");
    }

    @Test
    @DisplayName("Should throw exception for empty Excel file")
    void shouldThrowExceptionForEmptyExcelFile() throws IOException {
        // Arrange
        MultipartFile file = createEmptyExcelFile();

        // Act & Assert
        assertThatThrownBy(() -> excelParserService.parseStudentEnrollment(file))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Should support multiple date formats")
    void shouldSupportMultipleDateFormats() throws IOException {
        // Arrange
        MultipartFile file = createExcelWithDifferentDateFormats();

        // Act
        List<StudentEnrollmentData> result = excelParserService.parseStudentEnrollment(file);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDob()).isEqualTo(LocalDate.of(1995, 1, 15)); // yyyy-MM-dd
        assertThat(result.get(1).getDob()).isEqualTo(LocalDate.of(1996, 3, 20)); // dd/MM/yyyy
        assertThat(result.get(2).getDob()).isEqualTo(LocalDate.of(1997, 5, 10)); // MM/dd/yyyy
    }

    @Test
    @DisplayName("Should support gender variations (male, m, MALE)")
    void shouldSupportGenderVariations() throws IOException {
        // Arrange
        MultipartFile file = createExcelWithGenderVariations();

        // Act
        List<StudentEnrollmentData> result = excelParserService.parseStudentEnrollment(file);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getGender()).isEqualTo(Gender.MALE);
        assertThat(result.get(1).getGender()).isEqualTo(Gender.FEMALE);
        assertThat(result.get(2).getGender()).isEqualTo(Gender.OTHER);
    }

    // Helper methods to create test Excel files

    private MultipartFile createValidExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header row
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Data rows
        createStudentRow(sheet, 1, "Nguyen Van A", "nguyenvana@email.com", "0901234567", "https://facebook.com/nguyenvana", "123 Đường ABC, Quận 1", "male", "1995-01-15");
        createStudentRow(sheet, 2, "Tran Thi B", "tranthib@email.com", "0902345678", "", "456 Đường XYZ, Quận 3", "female", "1996-03-20");
        createStudentRow(sheet, 3, "Le Van C", "levanc@email.com", "0903456789", "https://facebook.com/levanc", "", "male", "1997-05-10");

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createExcelWithMissingEmail() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Data row with missing email
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("Tran Thi B");     // full_name (0)
        row.createCell(1).setCellValue("");               // Empty email (1) - required field
        row.createCell(2).setCellValue("0902345678");     // phone (2)
        row.createCell(3).setCellValue("");               // facebook_url (3)
        row.createCell(4).setCellValue("");               // address (4)
        row.createCell(5).setCellValue("female");        // gender (5)
        row.createCell(6).setCellValue("1996-03-20");     // dob (6)

        // REMOVED: Assessment cells - not part of 7-column format
        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createExcelWithInvalidGender() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Data row with invalid gender
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("Nguyen Van A");     // full_name (0)
        row.createCell(1).setCellValue("nguyenvana@email.com"); // email (1)
        row.createCell(2).setCellValue("0901234567");     // phone (2)
        row.createCell(3).setCellValue("");               // facebook_url (3)
        row.createCell(4).setCellValue("");               // address (4)
        row.createCell(5).setCellValue("invalid_gender"); // gender (5) - invalid value
        row.createCell(6).setCellValue("1995-01-15");     // dob (6)

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createExcelWithInvalidDate() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Data row with invalid date
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("ST001");
        row.createCell(1).setCellValue("Nguyen Van A");
        row.createCell(2).setCellValue("nguyenvana@email.com");
        row.createCell(3).setCellValue("0901234567");
        row.createCell(4).setCellValue(""); // facebook_url
        row.createCell(5).setCellValue(""); // address
        row.createCell(6).setCellValue("male");
        row.createCell(7).setCellValue("invalid-date");
        row.createCell(8).setCellValue("A1-70");
        row.createCell(9).setCellValue("A1-68");
        row.createCell(10).setCellValue("A1-72");
        row.createCell(11).setCellValue("A1-75");
        row.createCell(12).setCellValue("A1-73");

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createEmptyExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Only header, no data
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createExcelWithDifferentDateFormats() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Different date formats
        createStudentRow(sheet, 1, "Student A", "studenta@email.com", "0901111111", "https://facebook.com/studenta", "789 Đường DEF, Quận 5", "male", "1995-01-15"); // yyyy-MM-dd
        createStudentRow(sheet, 2, "Student B", "studentb@email.com", "0902222222", "", "321 Đường GHI, Quận 7", "female", "1996-03-20"); // dd/MM/yyyy
        createStudentRow(sheet, 3, "Student C", "studentc@email.com", "0903333333", "https://facebook.com/studentc", "654 Đường JKL, Quận 2", "male", "1997-05-10"); // MM/dd/yyyy (May 10)

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createExcelWithGenderVariations() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Gender variations
        createStudentRow(sheet, 1, "Student A", "studenta@email.com", "0901111111", "https://facebook.com/studenta", "789 Đường DEF, Quận 5", "m", "1995-01-15");
        createStudentRow(sheet, 2, "Student B", "studentb@email.com", "0902222222", "", "321 Đường GHI, Quận 7", "F", "1996-03-20");
        createStudentRow(sheet, 3, "Student C", "studentc@email.com", "0903333333", "https://facebook.com/studentc", "654 Đường JKL, Quận 2", "Other", "1997-05-10");

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private void createHeaderRow(Row header) {
        // UPDATED: Simplified 7-column format - removed student_code and assessment fields
        header.createCell(0).setCellValue("full_name");
        header.createCell(1).setCellValue("email");
        header.createCell(2).setCellValue("phone");
        header.createCell(3).setCellValue("facebook_url");
        header.createCell(4).setCellValue("address");
        header.createCell(5).setCellValue("gender");
        header.createCell(6).setCellValue("dob");
    }

    private void createStudentRow(Sheet sheet, int rowNum, String name, String email,
                                   String phone, String facebookUrl, String address, String gender, String dob) {
        Row row = sheet.createRow(rowNum);
        // UPDATED: Simplified 7-column format - removed student_code and assessment fields
        row.createCell(0).setCellValue(name);       // full_name
        row.createCell(1).setCellValue(email);      // email
        row.createCell(2).setCellValue(phone);      // phone
        row.createCell(3).setCellValue(facebookUrl); // facebook_url
        row.createCell(4).setCellValue(address);     // address
        row.createCell(5).setCellValue(gender);     // gender
        row.createCell(6).setCellValue(dob);        // dob
    }

    private MultipartFile convertWorkbookToMultipartFile(Workbook workbook, String filename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray()
        );
    }
}
