# ENROLLMENT IMPLEMENTATION GUIDE

## Tài Liệu Kỹ Thuật Triển Khai Luồng Ghi Danh Học Viên

**Version:** 2.0
**Last Updated:** 2025-11-02
**Author:** Technical Team
**Focus:** Class-Specific Student Enrollment with Excel Import

---

## MỤC LỤC

1. [Tổng Quan Luồng Enrollment](#1-tổng-quan-luồng-enrollment)
2. [Business Flow Thực Tế](#2-business-flow-thực-tế)
3. [Quy Trình Chi Tiết](#3-quy-trình-chi-tiết)
4. [Excel Import - Class Enrollment](#4-excel-import---class-enrollment)
5. [Capacity Management](#5-capacity-management)
6. [Các Entities Liên Quan](#6-các-entities-liên-quan)
7. [Auto-Generation Logic](#7-auto-generation-logic)
8. [Database Schema Details](#8-database-schema-details)
9. [Business Rules](#9-business-rules)
10. [Edge Cases và Xử Lý Lỗi](#10-edge-cases-và-xử-lý-lỗi)

---

## 1. TỔNG QUAN LUỒNG ENROLLMENT

### 1.1 Các Actors/Roles Liên Quan

| Actor | Vai trò | Trách nhiệm trong Enrollment |
|-------|---------|------------------------------|
| **SALE** (Ngoài hệ thống) | Nhân viên kinh doanh | - Thu thập thông tin students đăng ký<br>- Tạo Excel file cho từng class<br>- Gửi file Excel cho Academic Affair |
| **ACADEMIC AFFAIR** | Giáo vụ | - Nhận Excel file từ Sale<br>- Import students vào class cụ thể<br>- Validate capacity và conflicts<br>- Override capacity nếu cần thiết<br>- Track enrolled_by để audit |
| **STUDENT** | Học viên | - Được ghi danh vào lớp (qua Excel hoặc manual)<br>- Nhận welcome email<br>- Xem lịch học cá nhân sau khi enrolled |
| **SYSTEM** | Hệ thống | - Parse Excel và resolve students<br>- Auto-generate student_session records<br>- Validate business rules<br>- Send notifications<br>- Execute enrollment transaction |

### 1.2 Điều Kiện Tiên Quyết (Pre-conditions)

**Class phải đáp ứng:**
- `class.approval_status = 'approved'` (Class đã được Center Head/Manager duyệt)
- `class.status = 'scheduled'` (Class đã sẵn sàng để ghi danh)
- Tất cả sessions đã được tạo (auto-generated từ course template)
- Có time slots, resources, teachers đã được assign

**Student (nếu đã tồn tại trong DB):**
- Đã có user_account và student record trong hệ thống
- Thuộc branch (hoặc có access cross-branch)
- Chưa được enrolled vào class này (không duplicate enrollment)
- user_account.status = 'active'

### 1.3 Kết Quả Mong Đợi (Post-conditions)

**Enrollment records được tạo:**
- `enrollment` record với status = 'enrolled'
- `enrolled_by` được ghi lại (user_id của Academic Affair)
- `enrolled_at` = CURRENT_TIMESTAMP

**Student_session records được auto-generate:**
- Tạo `student_session` cho **tất cả future sessions** của class
- Mỗi record có:
  - `attendance_status = 'planned'`
  - `is_makeup = false`
  - Link đến student và session tương ứng

**Notifications:**
- Student nhận welcome email với thông tin class và lịch học
- Academic Affair nhận confirmation về số lượng students enrolled

---

## 2. BUSINESS FLOW THỰC TẾ

### 2.1 Luồng Từ Sale → Academic Affair

```
┌─────────────────────────────────────────────────────────────────┐
│ SALE (Ngoài Hệ Thống)                                          │
├─────────────────────────────────────────────────────────────────┤
│ 1. Thu thập thông tin students đăng ký học                     │
│ 2. Group students theo class_code (đã được lên lịch sẵn)       │
│ 3. Tạo Excel file cho từng class:                              │
│    - File name: class_ENG-A1-001_students.xlsx                 │
│    - Columns: student_code, full_name, email, phone,          │
│                gender, dob, level                              │
│ 4. Gửi file Excel cho Academic Affair (email/shared drive)     │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ ACADEMIC AFFAIR (Trong Hệ Thống)                               │
├─────────────────────────────────────────────────────────────────┤
│ 1. Nhận file Excel từ Sale                                     │
│ 2. Login vào hệ thống → Navigate to Class Detail               │
│ 3. Click "Enroll Students" → Tab "Import Excel"                │
│ 4. Upload file Excel → System parse & preview                  │
│ 5. Review preview:                                              │
│    - Students found (đã có trong DB)                           │
│    - Students to create (chưa có trong DB)                     │
│    - Capacity warnings                                          │
│    - Schedule conflicts                                         │
│ 6. Confirm enrollment:                                          │
│    - Option A: Enroll all (if capacity OK)                     │
│    - Option B: Override capacity (with reason)                 │
│    - Option C: Partial enrollment (select students)            │
│    - Option D: Cancel                                           │
│ 7. System executes enrollment transaction                       │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ SYSTEM AUTO-PROCESSING                                         │
├─────────────────────────────────────────────────────────────────┤
│ 1. Create new students (if needed)                             │
│ 2. Create enrollment records                                    │
│ 3. Auto-generate student_session records                       │
│ 4. Send welcome emails to students (async)                     │
│ 5. Send confirmation to Academic Affair                         │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Key Differences từ Version 1.0

| Aspect | Version 1.0 (Old) | Version 2.0 (Current) |
|--------|-------------------|------------------------|
| **Student Source** | Manual select từ available students | Excel file từ Sale (class-specific) |
| **Import Scope** | Global import students (không gắn class) | Import + Enroll vào class cụ thể |
| **Workflow Steps** | 2 bước: Import → Enroll | 1 bước: Import & Enroll |
| **Business Alignment** | Không match với Sale workflow | Match 100% với Sale workflow |
| **Error Prone** | Cao (dễ enroll nhầm class) | Thấp (students đã được group sẵn) |

---

## 3. QUY TRÌNH CHI TIẾT

### Bước 1: Academic Affair Truy Cập Class Detail

**Action:** Academic Affair mở class detail page

**System Logic:**
```sql
SELECT c.*,
       COUNT(e.id) as enrolled_count,
       (c.max_capacity - COUNT(e.id)) as available_slots
FROM class c
LEFT JOIN enrollment e ON c.id = e.class_id AND e.status = 'enrolled'
WHERE c.id = :classId
  AND c.approval_status = 'approved'
  AND c.status = 'scheduled'
GROUP BY c.id
```

**Display:**
- Class info: code, name, course_name, branch, modality, start_date
- Current enrollments: X/max_capacity (ví dụ: 15/20)
- Button: "Enroll Students" (enabled if class is 'scheduled')

---

### Bước 2: Click "Enroll Students" - Choose Import Method

**Action:** Academic Affair clicks "Enroll Students"

**UI Display:** Modal/Page với 3 tabs:

```
┌────────────────────────────────────────────────────────────────┐
│ Enroll Students - Class: ENG-A1-001 (15/20 enrolled)          │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│ [Tab 1: Select Existing] [Tab 2: Add Single] [Tab 3: Import Excel] │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────┐  │
│ │                                                           │  │
│ │   (Tab content area - depends on selected tab)           │  │
│ │                                                           │  │
│ └─────────────────────────────────────────────────────────┘  │
│                                                                 │
│                              [Cancel]  [Enroll Selected]        │
└────────────────────────────────────────────────────────────────┘
```

**Tab 1: Select Existing Students**
- Load available students từ branch (students chưa enrolled vào class này)
- Multi-select checkboxes
- Display: student_code, full_name, email, level
- Use case: Ad-hoc enrollment (không phổ biến)

**Tab 2: Add Single Student**
- Form nhập thông tin 1 student mới
- Fields: full_name, email, phone, gender, dob, level
- Use case: Emergency add (rất hiếm khi dùng)

**Tab 3: Import Excel** ⭐ **PRIMARY METHOD**
- Upload Excel file
- Parse và preview students
- Handle capacity warnings
- **Use case: MAIN enrollment method từ Sale**

---

### Bước 3: Upload Excel và Preview (TAB 3 - PRIMARY FLOW)

**Action:** Academic Affair chọn Tab 3 "Import Excel" và upload file

**Excel File Format:**
```csv
student_code,full_name,email,phone,gender,dob,level
ST001,Nguyen Van A,nguyenvana@email.com,0901234567,male,1995-01-15,A1
,Tran Thi B,tranthib@email.com,0902345678,female,1996-03-20,A1
ST003,Le Van C,levanc@email.com,0903456789,male,1997-05-10,A1
```

**Note:**
- `student_code` có thể empty (student mới chưa có trong DB)
- Nếu có `student_code` → system tìm theo code
- Nếu không có `student_code` → system tìm theo email
- Nếu không tìm thấy → tạo mới

---

## 4. EXCEL IMPORT - CLASS ENROLLMENT

### 4.1 DTOs

```java
/**
 * Request để preview import Excel
 */
@Data
public class ClassEnrollmentImportPreviewRequest {
    @NotNull
    private Long classId;

    @NotNull
    private MultipartFile file;  // Excel file
}

/**
 * Data của mỗi student trong Excel (sau khi parse)
 */
@Data
@Builder
public class StudentEnrollmentData {
    // From Excel
    private String studentCode;  // Nullable
    private String fullName;
    private String email;
    private String phone;
    private Gender gender;
    private LocalDate dob;
    private String level;  // A1, A2, B1...

    // Resolution result (sau khi system xử lý)
    private StudentResolutionStatus status;  // FOUND/CREATE/DUPLICATE/ERROR
    private Long resolvedStudentId;  // Nếu FOUND
    private String errorMessage;  // Nếu ERROR
}

/**
 * Status của mỗi student sau khi resolve
 */
public enum StudentResolutionStatus {
    FOUND,       // Student đã tồn tại trong DB → sẽ enroll
    CREATE,      // Student mới → sẽ tạo mới rồi enroll
    DUPLICATE,   // Trùng trong file Excel (error)
    ERROR        // Validation lỗi (email invalid, missing fields...)
}

/**
 * Preview result trả về cho frontend
 */
@Data
@Builder
public class ClassEnrollmentImportPreview {
    // Class info
    private Long classId;
    private String classCode;
    private String className;

    // Students data
    private List<StudentEnrollmentData> students;
    private int foundCount;      // Số students đã có trong DB
    private int createCount;     // Số students sẽ tạo mới
    private int errorCount;      // Số students có lỗi
    private int totalValid;      // found + create

    // Capacity info
    private int currentEnrolled;
    private int maxCapacity;
    private int availableSlots;
    private boolean exceedsCapacity;
    private int exceededBy;  // Số lượng vượt quá (0 nếu không vượt)

    // Warnings
    private List<String> warnings;
    private List<String> errors;

    // Recommendation
    private EnrollmentRecommendation recommendation;
}

/**
 * Recommendation cho Academic Affair
 */
@Data
@Builder
public class EnrollmentRecommendation {
    private RecommendationType type;
    private String message;
    private Integer suggestedEnrollCount;  // Nếu type = PARTIAL_SUGGESTED
}

public enum RecommendationType {
    OK,                   // Capacity đủ, enroll hết
    PARTIAL_SUGGESTED,    // Vượt capacity, suggest enroll một phần
    OVERRIDE_AVAILABLE,   // Vượt capacity nhưng <= 20%, có thể override
    BLOCKED               // Vượt quá nhiều, không nên enroll
}

/**
 * Request để execute enrollment sau khi preview
 */
@Data
public class ClassEnrollmentImportExecuteRequest {
    @NotNull
    private Long classId;

    @NotNull
    private EnrollmentStrategy strategy;

    // Nếu strategy = PARTIAL → phải có
    private List<Long> selectedStudentIds;

    // Nếu strategy = OVERRIDE → phải có
    @Size(min = 20, message = "Override reason must be at least 20 characters")
    private String overrideReason;

    // Students từ preview
    @NotEmpty
    private List<StudentEnrollmentData> students;
}

public enum EnrollmentStrategy {
    ALL,      // Enroll tất cả (nếu capacity đủ)
    PARTIAL,  // Enroll một phần (selectedStudentIds)
    OVERRIDE  // Override capacity và enroll tất cả
}

/**
 * Enrollment result
 */
@Data
@Builder
public class EnrollmentResult {
    private int enrolledCount;
    private int studentsCreated;
    private int sessionsGeneratedPerStudent;
    private int totalStudentSessionsCreated;
    private List<String> warnings;
}
```

### 4.2 Service Logic - Preview Import

```java
@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExcelParserService excelParserService;

    @Override
    public ClassEnrollmentImportPreview previewClassEnrollmentImport(
        Long classId,
        MultipartFile file,
        Long enrolledBy
    ) {
        // 1. Validate class exists và đủ điều kiện enroll
        ClassEntity classEntity = validateClassForEnrollment(classId);

        // 2. Parse Excel file
        List<StudentEnrollmentData> parsedData = excelParserService.parseStudentEnrollment(file);

        if (parsedData.isEmpty()) {
            throw new BusinessException("Excel file is empty or invalid format");
        }

        // 3. Resolve từng student (FOUND/CREATE/ERROR)
        resolveStudents(parsedData);

        // 4. Calculate capacity
        int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
            classId, EnrollmentStatus.ENROLLED
        );
        int maxCapacity = classEntity.getMaxCapacity();
        int availableSlots = maxCapacity - currentEnrolled;

        int validStudentsCount = (int) parsedData.stream()
            .filter(d -> d.getStatus() == StudentResolutionStatus.FOUND
                      || d.getStatus() == StudentResolutionStatus.CREATE)
            .count();

        int errorCount = (int) parsedData.stream()
            .filter(d -> d.getStatus() == StudentResolutionStatus.ERROR)
            .count();

        boolean exceedsCapacity = validStudentsCount > availableSlots;
        int exceededBy = exceedsCapacity ? (validStudentsCount - availableSlots) : 0;

        // 5. Determine recommendation
        EnrollmentRecommendation recommendation = determineRecommendation(
            validStudentsCount,
            availableSlots,
            maxCapacity,
            currentEnrolled
        );

        // 6. Build warnings
        List<String> warnings = new ArrayList<>();
        if (exceedsCapacity) {
            warnings.add(String.format(
                "Import will exceed capacity by %d students (%d enrolled + %d new = %d/%d)",
                exceededBy, currentEnrolled, validStudentsCount,
                currentEnrolled + validStudentsCount, maxCapacity
            ));
        }
        if (errorCount > 0) {
            warnings.add(String.format("%d students have validation errors", errorCount));
        }

        // 7. Return preview
        return ClassEnrollmentImportPreview.builder()
            .classId(classId)
            .classCode(classEntity.getCode())
            .className(classEntity.getName())
            .students(parsedData)
            .foundCount((int) parsedData.stream().filter(d -> d.getStatus() == StudentResolutionStatus.FOUND).count())
            .createCount((int) parsedData.stream().filter(d -> d.getStatus() == StudentResolutionStatus.CREATE).count())
            .errorCount(errorCount)
            .totalValid(validStudentsCount)
            .currentEnrolled(currentEnrolled)
            .maxCapacity(maxCapacity)
            .availableSlots(availableSlots)
            .exceedsCapacity(exceedsCapacity)
            .exceededBy(exceededBy)
            .warnings(warnings)
            .recommendation(recommendation)
            .build();
    }

    /**
     * Resolve từng student: tìm trong DB hoặc mark as CREATE
     */
    private void resolveStudents(List<StudentEnrollmentData> parsedData) {
        Set<String> seenEmails = new HashSet<>();

        for (StudentEnrollmentData data : parsedData) {
            // Validate required fields
            if (data.getEmail() == null || data.getFullName() == null) {
                data.setStatus(StudentResolutionStatus.ERROR);
                data.setErrorMessage("Missing required fields (email or full_name)");
                continue;
            }

            // Check duplicate trong file Excel
            if (seenEmails.contains(data.getEmail())) {
                data.setStatus(StudentResolutionStatus.DUPLICATE);
                data.setErrorMessage("Duplicate email in Excel file");
                continue;
            }
            seenEmails.add(data.getEmail());

            // Try to find by student_code
            if (data.getStudentCode() != null && !data.getStudentCode().isBlank()) {
                Optional<Student> existing = studentRepository.findByStudentCode(data.getStudentCode());
                if (existing.isPresent()) {
                    data.setStatus(StudentResolutionStatus.FOUND);
                    data.setResolvedStudentId(existing.get().getId());
                    continue;
                }
            }

            // Try to find by email
            Optional<UserAccount> userByEmail = userAccountRepository.findByEmail(data.getEmail());
            if (userByEmail.isPresent()) {
                Optional<Student> student = studentRepository.findByUserId(userByEmail.get().getId());
                if (student.isPresent()) {
                    data.setStatus(StudentResolutionStatus.FOUND);
                    data.setResolvedStudentId(student.get().getId());
                    continue;
                }
            }

            // Mark as CREATE (student mới)
            data.setStatus(StudentResolutionStatus.CREATE);
        }
    }

    /**
     * Determine recommendation based on capacity
     */
    private EnrollmentRecommendation determineRecommendation(
        int toEnroll,
        int available,
        int maxCapacity,
        int currentEnrolled
    ) {
        if (toEnroll <= available) {
            // Case 1: Capacity đủ
            return EnrollmentRecommendation.builder()
                .type(RecommendationType.OK)
                .message("Sufficient capacity. All students can be enrolled.")
                .suggestedEnrollCount(toEnroll)
                .build();
        }

        int exceededBy = toEnroll - available;
        double exceededPercentage = (double) exceededBy / maxCapacity * 100;

        if (exceededPercentage <= 20) {
            // Case 2: Vượt <= 20% → suggest override
            return EnrollmentRecommendation.builder()
                .type(RecommendationType.OVERRIDE_AVAILABLE)
                .message(String.format(
                    "Exceeds capacity by %d students (%.1f%%). You can override with approval reason.",
                    exceededBy, exceededPercentage
                ))
                .suggestedEnrollCount(null)
                .build();
        }

        if (available > 0) {
            // Case 3: Vượt > 20% nhưng vẫn còn slots → suggest partial
            return EnrollmentRecommendation.builder()
                .type(RecommendationType.PARTIAL_SUGGESTED)
                .message(String.format(
                    "Exceeds capacity significantly (%.1f%%). Recommend enrolling only %d students (available slots).",
                    exceededPercentage, available
                ))
                .suggestedEnrollCount(available)
                .build();
        }

        // Case 4: Class đã full
        return EnrollmentRecommendation.builder()
            .type(RecommendationType.BLOCKED)
            .message("Class is full. Cannot enroll any students without capacity override.")
            .suggestedEnrollCount(0)
            .build();
    }

    /**
     * Validate class có đủ điều kiện để enroll không
     */
    private ClassEntity validateClassForEnrollment(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new EntityNotFoundException("Class not found: " + classId));

        if (!classEntity.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new BusinessException("Class must be approved before enrollment");
        }

        if (!classEntity.getStatus().equals(ClassStatus.SCHEDULED)) {
            throw new BusinessException("Class must be in 'scheduled' status for enrollment");
        }

        return classEntity;
    }
}
```

### 4.3 Service Logic - Execute Import

```java
@Override
@Transactional
public EnrollmentResult executeClassEnrollmentImport(
    ClassEnrollmentImportExecuteRequest request,
    Long enrolledBy
) {
    // 1. Lock class để đảm bảo consistency (tránh race condition)
    ClassEntity classEntity = classRepository.findByIdWithLock(request.getClassId())
        .orElseThrow(() -> new EntityNotFoundException("Class not found"));

    // 2. Re-validate capacity (double-check for race condition)
    int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
        request.getClassId(), EnrollmentStatus.ENROLLED
    );

    // 3. Filter students theo strategy
    List<StudentEnrollmentData> studentsToEnroll;

    switch (request.getStrategy()) {
        case ALL:
            // Enroll tất cả valid students
            studentsToEnroll = request.getStudents().stream()
                .filter(s -> s.getStatus() == StudentResolutionStatus.FOUND
                          || s.getStatus() == StudentResolutionStatus.CREATE)
                .collect(Collectors.toList());

            // Validate capacity
            if (currentEnrolled + studentsToEnroll.size() > classEntity.getMaxCapacity()) {
                throw new CapacityExceededException(
                    String.format("Capacity exceeded. Current: %d, Adding: %d, Max: %d",
                        currentEnrolled, studentsToEnroll.size(), classEntity.getMaxCapacity())
                );
            }
            break;

        case PARTIAL:
            // Enroll chỉ selected students
            if (request.getSelectedStudentIds() == null || request.getSelectedStudentIds().isEmpty()) {
                throw new BusinessException("Selected student IDs required for PARTIAL strategy");
            }

            studentsToEnroll = request.getStudents().stream()
                .filter(s -> s.getStatus() == StudentResolutionStatus.FOUND
                          && request.getSelectedStudentIds().contains(s.getResolvedStudentId()))
                .collect(Collectors.toList());

            // Validate capacity
            if (currentEnrolled + studentsToEnroll.size() > classEntity.getMaxCapacity()) {
                throw new CapacityExceededException("Selected students still exceed capacity");
            }
            break;

        case OVERRIDE:
            // Override capacity và enroll tất cả
            if (request.getOverrideReason() == null || request.getOverrideReason().length() < 20) {
                throw new BusinessException("Override reason required (min 20 characters)");
            }

            studentsToEnroll = request.getStudents().stream()
                .filter(s -> s.getStatus() == StudentResolutionStatus.FOUND
                          || s.getStatus() == StudentResolutionStatus.CREATE)
                .collect(Collectors.toList());

            // Log capacity override
            logCapacityOverride(
                request.getClassId(),
                classEntity.getMaxCapacity(),
                studentsToEnroll.size(),
                request.getOverrideReason(),
                enrolledBy
            );
            break;

        default:
            throw new BusinessException("Invalid enrollment strategy");
    }

    // 4. Create new students nếu cần
    List<Long> allStudentIds = new ArrayList<>();
    int studentsCreated = 0;

    for (StudentEnrollmentData data : studentsToEnroll) {
        if (data.getStatus() == StudentResolutionStatus.CREATE) {
            Student newStudent = createStudentQuick(data, classEntity.getBranchId());
            allStudentIds.add(newStudent.getId());
            studentsCreated++;
        } else if (data.getStatus() == StudentResolutionStatus.FOUND) {
            allStudentIds.add(data.getResolvedStudentId());
        }
    }

    // 5. Batch enroll all students
    EnrollmentResult result = enrollStudents(request.getClassId(), allStudentIds, enrolledBy);
    result.setStudentsCreated(studentsCreated);

    return result;
}

/**
 * Core enrollment logic - batch enroll students vào class
 */
@Transactional
public EnrollmentResult enrollStudents(Long classId, List<Long> studentIds, Long enrolledBy) {
    // 1. Validate class (đã lock ở execute method)
    ClassEntity classEntity = classRepository.findById(classId)
        .orElseThrow(() -> new EntityNotFoundException("Class not found"));

    // 2. Get all future sessions của class
    List<Session> futureSessions = sessionRepository.findByClassIdAndDateGreaterThanEqualAndStatus(
        classId,
        LocalDate.now(),
        SessionStatus.PLANNED
    );

    if (futureSessions.isEmpty()) {
        throw new BusinessException("No future sessions available for enrollment");
    }

    // 3. Batch insert enrollments
    List<Enrollment> enrollments = new ArrayList<>();
    for (Long studentId : studentIds) {
        // Check duplicate enrollment
        boolean alreadyEnrolled = enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
            classId, studentId, EnrollmentStatus.ENROLLED
        );
        if (alreadyEnrolled) {
            throw new BusinessException("Student " + studentId + " is already enrolled in this class");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setClassId(classId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setEnrolledBy(enrolledBy);

        // Mid-course enrollment: track join_session_id
        if (LocalDate.now().isAfter(classEntity.getStartDate())) {
            Session firstFutureSession = futureSessions.get(0);
            enrollment.setJoinSessionId(firstFutureSession.getId());
        }

        enrollments.add(enrollment);
    }
    enrollmentRepository.saveAll(enrollments);

    // 4. Auto-generate student_session records
    List<StudentSession> studentSessions = new ArrayList<>();
    for (Enrollment enrollment : enrollments) {
        for (Session session : futureSessions) {
            StudentSession ss = new StudentSession();
            ss.setStudentId(enrollment.getStudentId());
            ss.setSessionId(session.getId());
            ss.setAttendanceStatus(AttendanceStatus.PLANNED);
            ss.setIsMakeup(false);
            studentSessions.add(ss);
        }
    }
    studentSessionRepository.saveAll(studentSessions);

    // 5. Send welcome emails (async)
    for (Long studentId : studentIds) {
        emailService.sendEnrollmentConfirmation(studentId, classId);
    }

    // 6. Return result
    return EnrollmentResult.builder()
        .enrolledCount(enrollments.size())
        .sessionsGeneratedPerStudent(futureSessions.size())
        .totalStudentSessionsCreated(studentSessions.size())
        .build();
}

/**
 * Create student nhanh từ Excel data
 */
private Student createStudentQuick(StudentEnrollmentData data, Long branchId) {
    // 1. Create user_account
    UserAccount user = new UserAccount();
    user.setEmail(data.getEmail());
    user.setFullName(data.getFullName());
    user.setPhone(data.getPhone());
    user.setGender(data.getGender());
    user.setDob(data.getDob());
    user.setStatus(UserStatus.ACTIVE);
    user.setPassword(passwordEncoder.encode(generateTemporaryPassword()));
    UserAccount savedUser = userAccountRepository.save(user);

    // 2. Create student
    Student student = new Student();
    student.setUserId(savedUser.getId());
    student.setStudentCode(generateStudentCode(branchId));
    student.setLevel(data.getLevel());
    Student savedStudent = studentRepository.save(student);

    // 3. Assign STUDENT role
    Role studentRole = roleRepository.findByCode("STUDENT")
        .orElseThrow(() -> new EntityNotFoundException("STUDENT role not found"));
    UserRole userRole = new UserRole();
    userRole.setUserId(savedUser.getId());
    userRole.setRoleId(studentRole.getId());
    userRoleRepository.save(userRole);

    // 4. Assign to branch
    UserBranches userBranch = new UserBranches();
    userBranch.setUserId(savedUser.getId());
    userBranch.setBranchId(branchId);
    userBranch.setAssignedBy(getCurrentUserId());
    userBranchesRepository.save(userBranch);

    return savedStudent;
}

/**
 * Log capacity override để audit
 */
private void logCapacityOverride(
    Long classId,
    int maxCapacity,
    int overrideCount,
    String reason,
    Long approvedBy
) {
    CapacityOverrideLog log = new CapacityOverrideLog();
    log.setClassId(classId);
    log.setOriginalCapacity(maxCapacity);
    log.setOverrideCount(overrideCount);
    log.setReason(reason);
    log.setApprovedBy(approvedBy);
    log.setApprovedAt(LocalDateTime.now());
    capacityOverrideLogRepository.save(log);
}
```

### 4.4 Controller Endpoints

```java
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Preview import Excel cho class enrollment
     * POST /api/v1/enrollments/classes/{classId}/import/preview
     */
    @PostMapping("/classes/{classId}/import/preview")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject> previewImport(
        @PathVariable Long classId,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        // Validate file type
        if (!file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new BusinessException("Only Excel files (.xlsx) are supported");
        }

        ClassEnrollmentImportPreview preview = enrollmentService.previewClassEnrollmentImport(
            classId, file, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.builder()
            .success(true)
            .message("Import preview ready")
            .data(preview)
            .build());
    }

    /**
     * Execute import sau khi preview và confirm
     * POST /api/v1/enrollments/classes/{classId}/import/execute
     */
    @PostMapping("/classes/{classId}/import/execute")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject> executeImport(
        @PathVariable Long classId,
        @RequestBody @Valid ClassEnrollmentImportExecuteRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        // Validate classId match
        if (!classId.equals(request.getClassId())) {
            throw new BusinessException("Class ID mismatch");
        }

        EnrollmentResult result = enrollmentService.executeClassEnrollmentImport(
            request,
            currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.builder()
            .success(true)
            .message(String.format("Successfully enrolled %d students", result.getEnrolledCount()))
            .data(result)
            .build());
    }
}
```

### 4.5 Excel Parser Service

```java
@Service
public class ExcelParserService {

    /**
     * Parse Excel file thành list StudentEnrollmentData
     */
    public List<StudentEnrollmentData> parseStudentEnrollment(MultipartFile file) {
        List<StudentEnrollmentData> students = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    StudentEnrollmentData data = StudentEnrollmentData.builder()
                        .studentCode(getCellValueAsString(row.getCell(0)))
                        .fullName(getCellValueAsString(row.getCell(1)))
                        .email(getCellValueAsString(row.getCell(2)))
                        .phone(getCellValueAsString(row.getCell(3)))
                        .gender(parseGender(getCellValueAsString(row.getCell(4))))
                        .dob(parseDob(getCellValueAsString(row.getCell(5))))
                        .level(getCellValueAsString(row.getCell(6)))
                        .build();

                    students.add(data);
                } catch (Exception e) {
                    // Mark row có lỗi
                    StudentEnrollmentData errorData = new StudentEnrollmentData();
                    errorData.setStatus(StudentResolutionStatus.ERROR);
                    errorData.setErrorMessage("Row " + (i + 1) + ": " + e.getMessage());
                    students.add(errorData);
                }
            }

        } catch (IOException e) {
            throw new BusinessException("Failed to parse Excel file: " + e.getMessage());
        }

        return students;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    private Gender parseGender(String value) {
        if (value == null) return null;
        return Gender.valueOf(value.toUpperCase());
    }

    private LocalDate parseDob(String value) {
        if (value == null) return null;
        return LocalDate.parse(value);  // Expect format: YYYY-MM-DD
    }
}
```

---

## 5. CAPACITY MANAGEMENT

### 5.1 Scenario: 15/20 + 10 Students (Vượt 5 Slots)

**Tình huống:**
- Class hiện tại: 15/20 enrolled
- Excel file có 10 students
- Result: 25/20 (vượt 5 students)

**Preview Response:**
```json
{
  "classId": 1,
  "classCode": "ENG-A1-001",
  "className": "Basic English A1",
  "students": [...10 students...],
  "foundCount": 5,
  "createCount": 5,
  "errorCount": 0,
  "totalValid": 10,
  "currentEnrolled": 15,
  "maxCapacity": 20,
  "availableSlots": 5,
  "exceedsCapacity": true,
  "exceededBy": 5,
  "warnings": [
    "Import will exceed capacity by 5 students (15 enrolled + 10 new = 25/20)"
  ],
  "recommendation": {
    "type": "OVERRIDE_AVAILABLE",
    "message": "Exceeds capacity by 5 students (25.0%). You can override with approval reason.",
    "suggestedEnrollCount": null
  }
}
```

**Frontend Display:**
```
┌─────────────────────────────────────────────────────────────────┐
│ ⚠️  CAPACITY WARNING                                            │
├─────────────────────────────────────────────────────────────────┤
│ Current enrolled: 15/20                                         │
│ Students to enroll: 10                                          │
│ After enrollment: 25/20 (EXCEEDS by 5 students)                 │
│                                                                 │
│ 📋 Students Parsed:                                             │
│ ┌───────────────────────────────────────────────────────────┐  │
│ │ ☑ Nguyen Van A    | nguyenvana@... | ✅ Found             │  │
│ │ ☑ Tran Thi B      | tranthib@...   | 🆕 Create            │  │
│ │ ☑ Le Van C        | levanc@...     | ✅ Found             │  │
│ │ ☑ Pham Thi D      | phamthid@...   | 🆕 Create            │  │
│ │ ☑ Hoang Van E     | hoangvane@...  | ✅ Found             │  │
│ │ ☑ Nguyen Thi F    | nguyenthif@... | 🆕 Create            │  │
│ │ ☑ Vo Van G        | vovang@...     | ✅ Found             │  │
│ │ ☑ Do Thi H        | dothih@...     | 🆕 Create            │  │
│ │ ☑ Bui Van I       | buivani@...    | ✅ Found             │  │
│ │ ☑ Dang Thi K      | dangthik@...   | 🆕 Create            │  │
│ └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│ Summary: 10 students (5 found, 5 new)                           │
│                                                                 │
│ 🎯 Choose Action:                                               │
│                                                                 │
│ ○ Option 1: Override Capacity (Enroll all 10 students)         │
│   [Reason: _______________________________________________]     │
│            (min 20 characters)                                  │
│                                                                 │
│ ○ Option 2: Partial Enrollment (Select 5 students)             │
│   (Uncheck students above to match available slots)            │
│                                                                 │
│ ○ Option 3: Cancel Import                                      │
│                                                                 │
│ [Cancel]                                    [Confirm Enrollment]│
└─────────────────────────────────────────────────────────────────┘
```

**Execute Request (Option 1 - Override):**
```json
{
  "classId": 1,
  "strategy": "OVERRIDE",
  "overrideReason": "Approved by Center Manager due to high student demand and additional teacher support",
  "students": [...10 students từ preview...]
}
```

**Execute Request (Option 2 - Partial):**
```json
{
  "classId": 1,
  "strategy": "PARTIAL",
  "selectedStudentIds": [101, 103, 105, 107, 109],  // Chỉ chọn 5 students
  "students": [...10 students từ preview...]
}
```

### 5.2 Capacity Override Log

**Entity:**
```java
@Entity
@Table(name = "capacity_override_log")
@Data
public class CapacityOverrideLog extends BaseEntity {
    private Long classId;
    private Integer originalCapacity;
    private Integer overrideCount;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    private Long approvedBy;  // FK to user_account
    private LocalDateTime approvedAt;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private UserAccount approvedByUser;
}
```

**Schema:**
```sql
CREATE TABLE capacity_override_log (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    original_capacity INTEGER NOT NULL,
    override_count INTEGER NOT NULL,
    reason TEXT NOT NULL,
    approved_by BIGINT NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_capacity_log_class FOREIGN KEY(class_id) REFERENCES "class"(id),
    CONSTRAINT fk_capacity_log_approved_by FOREIGN KEY(approved_by) REFERENCES user_account(id)
);

CREATE INDEX idx_capacity_log_class ON capacity_override_log(class_id);
CREATE INDEX idx_capacity_log_approved_by ON capacity_override_log(approved_by);
```

---

## 6. CÁC ENTITIES LIÊN QUAN

### 6.1 Entity: Enrollment

```java
@Entity
@Table(name = "enrollment")
@Data
@EqualsAndHashCode(callSuper = true)
public class Enrollment extends BaseEntity {
    @Column(nullable = false)
    private Long classId;

    @Column(nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;  // enrolled/transferred/dropped/completed

    private LocalDateTime enrolledAt;
    private LocalDateTime leftAt;

    private Long joinSessionId;  // First session student attends (for mid-course)
    private Long leftSessionId;  // Last session before leaving

    private Long enrolledBy;  // FK to user_account (Academic Affair)

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "join_session_id", insertable = false, updatable = false)
    private Session joinSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by", insertable = false, updatable = false)
    private UserAccount enrolledByUser;
}
```

### 6.2 Entity: StudentSession

```java
@Entity
@Table(name = "student_session")
@IdClass(StudentSessionId.class)
@Data
public class StudentSession {
    @Id
    private Long studentId;

    @Id
    private Long sessionId;

    @Column(nullable = false)
    private Boolean isMakeup = false;

    private Long makeupSessionId;
    private Long originalSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus attendanceStatus;  // planned/present/absent

    @Enumerated(EnumType.STRING)
    private HomeworkStatus homeworkStatus;

    private String note;
    private LocalDateTime recordedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    private Session session;
}

/**
 * Composite primary key
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentSessionId implements Serializable {
    private Long studentId;
    private Long sessionId;
}
```

---

## 7. AUTO-GENERATION LOGIC

### 7.1 Student Session Generation (Khi Enroll Students)

**Key Points:**

1. **Full Enrollment (từ đầu khóa):**
   - Tạo student_session cho **tất cả future sessions**
   - `join_session_id = NULL` (bắt đầu từ session 1)

2. **Mid-Course Enrollment (giữa khóa):**
   - Chỉ tạo student_session cho **future sessions** (date >= CURRENT_DATE)
   - `join_session_id = first_future_session.id`
   - Không tạo cho past sessions

**Logic:**
```java
// Get future sessions
List<Session> futureSessions = sessionRepository.findByClassIdAndDateGreaterThanEqualAndStatus(
    classId,
    LocalDate.now(),
    SessionStatus.PLANNED
);

// Cartesian product: students × future_sessions
List<StudentSession> studentSessions = new ArrayList<>();
for (Long studentId : enrolledStudentIds) {
    for (Session futureSession : futureSessions) {
        StudentSession ss = new StudentSession();
        ss.setStudentId(studentId);
        ss.setSessionId(futureSession.getId());
        ss.setAttendanceStatus(AttendanceStatus.PLANNED);
        ss.setIsMakeup(false);
        studentSessions.add(ss);
    }
}

// Batch insert (performance optimization)
studentSessionRepository.saveAll(studentSessions);
```

**Performance Note:**
- Với 25 students × 36 sessions = 900 student_session records
- Dùng `saveAll()` với batch size 100 để optimize
- Transaction isolation level: READ_COMMITTED

---

## 8. DATABASE SCHEMA DETAILS

### 8.1 Enrollment Table

```sql
CREATE TABLE enrollment (
  id BIGSERIAL PRIMARY KEY,
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  status enrollment_status_enum NOT NULL DEFAULT 'enrolled',
  enrolled_at TIMESTAMPTZ,
  left_at TIMESTAMPTZ,
  join_session_id BIGINT,
  left_session_id BIGINT,
  enrolled_by BIGINT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

  CONSTRAINT fk_enrollment_class FOREIGN KEY(class_id) REFERENCES "class"(id) ON DELETE CASCADE,
  CONSTRAINT fk_enrollment_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_enrollment_join_session FOREIGN KEY(join_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_left_session FOREIGN KEY(left_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_enrollment_enrolled_by FOREIGN KEY(enrolled_by) REFERENCES user_account(id) ON DELETE SET NULL,

  -- Unique constraint: student không thể enrolled 2 lần vào cùng 1 class
  CONSTRAINT uk_enrollment_student_class UNIQUE(student_id, class_id, status)
);
```

### 8.2 Student_Session Table

```sql
CREATE TABLE student_session (
  student_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  is_makeup BOOLEAN DEFAULT false,
  makeup_session_id BIGINT,
  original_session_id BIGINT,
  attendance_status attendance_status_enum NOT NULL DEFAULT 'planned',
  homework_status homework_status_enum,
  note TEXT,
  recorded_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

  PRIMARY KEY(student_id, session_id),

  CONSTRAINT fk_student_session_student FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_session_session FOREIGN KEY(session_id) REFERENCES session(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_session_makeup FOREIGN KEY(makeup_session_id) REFERENCES session(id) ON DELETE SET NULL,
  CONSTRAINT fk_student_session_original FOREIGN KEY(original_session_id) REFERENCES session(id) ON DELETE SET NULL
);
```

### 8.3 Indexes

```sql
-- ==================== ENROLLMENT QUERIES ====================

-- Query enrolled students của class
CREATE INDEX idx_enrollment_class_status ON enrollment(class_id, status);

-- Count enrolled students
CREATE INDEX idx_enrollment_student ON enrollment(student_id);

-- Audit trail: who enrolled
CREATE INDEX idx_enrollment_enrolled_by ON enrollment(enrolled_by);

-- Mid-course enrollment tracking
CREATE INDEX idx_enrollment_join_session ON enrollment(join_session_id);

-- ==================== STUDENT SESSION QUERIES ====================

-- Load students cho attendance
CREATE INDEX idx_student_session_session ON student_session(session_id);

-- Student's personal schedule
CREATE INDEX idx_student_session_student_attendance ON student_session(student_id, attendance_status);

-- ==================== SESSION QUERIES ====================

-- Future sessions filter (enrollment generation)
CREATE INDEX idx_session_class_date_status ON session(class_id, date, status);
```

---

## 9. BUSINESS RULES

### 9.1 Enrollment Rules

| Rule ID | Description | Validation Level |
|---------|-------------|------------------|
| **BR-ENR-001** | Class must be 'scheduled' (approval_status = approved, status = scheduled) | Hard block |
| **BR-ENR-002** | enrolled_count ≤ max_capacity | Soft (can override with reason) |
| **BR-ENR-003** | Override reason min 20 characters | Hard block (if override) |
| **BR-ENR-004** | Mỗi enrollment auto-generate student_session cho tất cả future sessions | System rule |
| **BR-ENR-005** | Mid-course enrollment chỉ tạo student_session cho future sessions | System rule |
| **BR-ENR-006** | Track enrolled_by để audit | System rule |
| **BR-ENR-007** | Không duplicate enrollment (same student + class + status = enrolled) | Hard block |
| **BR-ENR-008** | Excel import: resolve by student_code → email → create new | System rule |
| **BR-ENR-009** | Capacity override <= 20% → OVERRIDE_AVAILABLE, > 20% → PARTIAL_SUGGESTED | Recommendation |

### 9.2 Capacity Rules

| Scenario | Current/Max | Adding | Result | Recommendation |
|----------|------------|--------|---------|----------------|
| OK | 15/20 | 5 | 20/20 | OK - Enroll all |
| Exceed 20% | 15/20 | 6 | 21/20 (+1) | OVERRIDE_AVAILABLE |
| Exceed 25% | 15/20 | 10 | 25/20 (+5) | OVERRIDE_AVAILABLE |
| Exceed 40% | 15/20 | 13 | 28/20 (+8) | PARTIAL_SUGGESTED (enroll 5) |
| Full | 20/20 | 5 | 25/20 (+5) | BLOCKED (require override) |

---

## 10. EDGE CASES VÀ XỬ LÝ LỖI

### Edge Case 1: Mid-Course Enrollment

**Scenario:** Academic Affair enroll student vào class đã học được 2 tuần (6 sessions)

**Handling:**
```java
if (LocalDate.now().isAfter(classEntity.getStartDate())) {
    // Mid-course enrollment
    Session firstFutureSession = futureSessions.get(0);
    enrollment.setJoinSessionId(firstFutureSession.getId());
}
```

**Result:**
- Chỉ tạo student_session cho 30 sessions còn lại
- `join_session_id` = Session 7
- Student không mất điểm danh cho 6 sessions đã qua

---

### Edge Case 2: Excel File Có Duplicate Emails

**Scenario:** Excel file có 2 rows với cùng email

**Handling:**
```java
Set<String> seenEmails = new HashSet<>();
for (StudentEnrollmentData data : parsedData) {
    if (seenEmails.contains(data.getEmail())) {
        data.setStatus(StudentResolutionStatus.DUPLICATE);
        data.setErrorMessage("Duplicate email in Excel file");
        continue;
    }
    seenEmails.add(data.getEmail());
}
```

**Result:**
- Row thứ 2 được mark là DUPLICATE
- Preview hiển thị warning
- Academic Affair phải fix Excel và re-upload

---

### Edge Case 3: Race Condition - Concurrent Enrollments

**Scenario:** 2 Academic Affairs cùng lúc enroll students vào class

**Handling:** Pessimistic Locking
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM ClassEntity c WHERE c.id = :classId")
Optional<ClassEntity> findByIdWithLock(@Param("classId") Long classId);
```

**Result:**
- Academic Affair 1 lock class → enroll thành công
- Academic Affair 2 wait → re-validate capacity → fail nếu vượt

---

### Edge Case 4: Email Sending Failure

**Scenario:** Enrollment thành công nhưng email service down

**Handling:** Async email với retry mechanism
```java
@Async
@Retryable(
    value = {EmailSendException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 5000)
)
public void sendEnrollmentConfirmation(Long studentId, Long classId) {
    // Send email...
}
```

**Policy:** Email failure **không block** enrollment transaction

---

## KẾT LUẬN

### Implementation Checklist

- [ ] **DTOs:** Create all request/response DTOs
- [ ] **Service:** EnrollmentService với preview + execute methods
- [ ] **Excel Parser:** ExcelParserService để parse Excel file
- [ ] **Controller:** EnrollmentController với 2 endpoints
- [ ] **Repository:** Add `findByIdWithLock()` method
- [ ] **Entity:** CapacityOverrideLog entity
- [ ] **Schema:** Create capacity_override_log table
- [ ] **Tests:** Unit tests + Integration tests
- [ ] **Email:** Async email service với retry

### Next Steps

1. Implement DTOs và enums
2. Implement ExcelParserService
3. Implement EnrollmentService (preview + execute)
4. Add Repository methods
5. Create Controller endpoints
6. Write comprehensive tests
7. Test với real Excel files

---

**Document Status:** ✅ Ready for Implementation
**Review Status:** Pending Technical Review
**Approval Status:** Pending Product Owner Approval
