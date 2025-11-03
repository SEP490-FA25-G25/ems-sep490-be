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
        assertThat(student1.getStudentCode()).isEqualTo("ST001");
        assertThat(student1.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(student1.getEmail()).isEqualTo("nguyenvana@email.com");
        assertThat(student1.getPhone()).isEqualTo("0901234567");
        assertThat(student1.getGender()).isEqualTo(Gender.MALE);
        assertThat(student1.getDob()).isEqualTo(LocalDate.of(1995, 1, 15));
        // Note: Skill assessments (general, reading, writing, speaking, listening) can be validated if needed
    }

    @Test
    @DisplayName("Should handle empty student code gracefully")
    void shouldHandleEmptyStudentCode() throws IOException {
        // Arrange
        MultipartFile file = createExcelWithEmptyStudentCode();

        // Act
        List<StudentEnrollmentData> result = excelParserService.parseStudentEnrollment(file);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentCode()).isNull();
        assertThat(result.get(0).getEmail()).isNotNull();
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
        createStudentRow(sheet, 1, "ST001", "Nguyen Van A", "nguyenvana@email.com", "0901234567", "male", "1995-01-15", "A1");
        createStudentRow(sheet, 2, "ST002", "Tran Thi B", "tranthib@email.com", "0902345678", "female", "1996-03-20", "A1");
        createStudentRow(sheet, 3, "ST003", "Le Van C", "levanc@email.com", "0903456789", "male", "1997-05-10", "A1");

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createExcelWithEmptyStudentCode() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Data row with empty student_code
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(""); // Empty student_code
        row.createCell(1).setCellValue("Tran Thi B");
        row.createCell(2).setCellValue("tranthib@email.com");
        row.createCell(3).setCellValue("0902345678");
        row.createCell(4).setCellValue(""); // facebook_url
        row.createCell(5).setCellValue(""); // address
        row.createCell(6).setCellValue("female");
        row.createCell(7).setCellValue("1996-03-20");
        row.createCell(8).setCellValue("A1-70"); // general
        row.createCell(9).setCellValue("A1-68"); // reading
        row.createCell(10).setCellValue("A1-72"); // writing
        row.createCell(11).setCellValue("A1-75"); // speaking
        row.createCell(12).setCellValue("A1-73"); // listening

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
        row.createCell(0).setCellValue("ST001");
        row.createCell(1).setCellValue("Nguyen Van A");
        row.createCell(2).setCellValue("nguyenvana@email.com");
        row.createCell(3).setCellValue("0901234567");
        row.createCell(4).setCellValue(""); // facebook_url
        row.createCell(5).setCellValue(""); // address
        row.createCell(6).setCellValue("invalid_gender");
        row.createCell(7).setCellValue("1995-01-15");
        row.createCell(8).setCellValue("A1-70");
        row.createCell(9).setCellValue("A1-68");
        row.createCell(10).setCellValue("A1-72");
        row.createCell(11).setCellValue("A1-75");
        row.createCell(12).setCellValue("A1-73");

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
        createStudentRow(sheet, 1, "ST001", "Student A", "studenta@email.com", "0901111111", "male", "1995-01-15", "A1"); // yyyy-MM-dd
        createStudentRow(sheet, 2, "ST002", "Student B", "studentb@email.com", "0902222222", "female", "20/03/1996", "A1"); // dd/MM/yyyy
        createStudentRow(sheet, 3, "ST003", "Student C", "studentc@email.com", "0903333333", "male", "10/05/1997", "A1"); // MM/dd/yyyy (May 10)

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private MultipartFile createExcelWithGenderVariations() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Students");

        // Header
        Row header = sheet.createRow(0);
        createHeaderRow(header);

        // Gender variations
        createStudentRow(sheet, 1, "ST001", "Student A", "studenta@email.com", "0901111111", "m", "1995-01-15", "A1");
        createStudentRow(sheet, 2, "ST002", "Student B", "studentb@email.com", "0902222222", "F", "1996-03-20", "A1");
        createStudentRow(sheet, 3, "ST003", "Student C", "studentc@email.com", "0903333333", "Other", "1997-05-10", "A1");

        return convertWorkbookToMultipartFile(workbook, "students.xlsx");
    }

    private void createHeaderRow(Row header) {
        header.createCell(0).setCellValue("student_code");
        header.createCell(1).setCellValue("full_name");
        header.createCell(2).setCellValue("email");
        header.createCell(3).setCellValue("phone");
        header.createCell(4).setCellValue("facebook_url");
        header.createCell(5).setCellValue("address");
        header.createCell(6).setCellValue("gender");
        header.createCell(7).setCellValue("dob");
        header.createCell(8).setCellValue("general");
        header.createCell(9).setCellValue("reading");
        header.createCell(10).setCellValue("writing");
        header.createCell(11).setCellValue("speaking");
        header.createCell(12).setCellValue("listening");
    }

    private void createStudentRow(Sheet sheet, int rowNum, String code, String name, String email,
                                   String phone, String gender, String dob, String level) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(code);
        row.createCell(1).setCellValue(name);
        row.createCell(2).setCellValue(email);
        row.createCell(3).setCellValue(phone);
        row.createCell(4).setCellValue(""); // facebook_url
        row.createCell(5).setCellValue(""); // address
        row.createCell(6).setCellValue(gender);
        row.createCell(7).setCellValue(dob);
        // Skill assessments - use level parameter for all skills
        row.createCell(8).setCellValue(level + "-70"); // general
        row.createCell(9).setCellValue(level + "-68"); // reading
        row.createCell(10).setCellValue(level + "-72"); // writing
        row.createCell(11).setCellValue(level + "-75"); // speaking
        row.createCell(12).setCellValue(level + "-73"); // listening
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
