# Student Schedule Feature - Frontend Handoff Documentation

## 1. Feature Overview

The Student Schedule feature allows authenticated students to view their weekly class schedule in a timetable format and access detailed information about individual sessions. Students can navigate between different weeks, view session details including materials and homework, and see their attendance status for each class. This feature provides a comprehensive view of the student's academic schedule with the ability to drill down into specific session details.

**Main Business Flow**: Students log in → View weekly schedule grid → Click on sessions for details → Navigate between weeks

**Target Users**: Students with `ROLE_STUDENT` permission only

## 2. API Endpoints

| Endpoint | Method | Description | Required Role | Success Status |
|----------|--------|-------------|---------------|----------------|
| `/api/v1/students/me/schedule` | GET | Get weekly schedule for authenticated student | STUDENT | 200 |
| `/api/v1/students/me/sessions/{sessionId}` | GET | Get detailed information for specific session | STUDENT | 200 |
| `/api/v1/students/me/current-week` | GET | Get current week Monday date for navigation | STUDENT | 200 |

## 3. Request/Response DTOs

### 3.1 Weekly Schedule Response

**Endpoint**: `GET /api/v1/students/me/schedule?weekStart=2025-11-04`

```json
{
  "success": true,
  "message": "Weekly schedule retrieved successfully",
  "data": {
    "weekStart": "2025-11-04",
    "weekEnd": "2025-11-10",
    "studentId": 123,
    "studentName": "Nguyen Van B",
    "timeSlots": [
      {
        "timeSlotTemplateId": 1,
        "name": "HN Morning 1",
        "startTime": "08:00:00",
        "endTime": "10:00:00"
      },
      {
        "timeSlotTemplateId": 2,
        "name": "HN Morning 2",
        "startTime": "10:15:00",
        "endTime": "12:15:00"
      }
    ],
    "schedule": {
      "MONDAY": [
        {
          "sessionId": 1001,
          "studentSessionId": 5001,
          "date": "2025-11-04",
          "dayOfWeek": "MONDAY",
          "timeSlotTemplateId": 1,
          "startTime": "08:00:00",
          "endTime": "10:00:00",
          "classCode": "HN-FOUND-O1",
          "className": "IELTS Foundation - Oct 2025",
          "courseId": 1,
          "courseName": "IELTS Foundation",
          "topic": "Introduction to IELTS",
          "sessionType": "CLASS",
          "sessionStatus": "PLANNED",
          "modality": "OFFLINE",
          "location": "Hanoi Branch",
          "branchName": "Hanoi Branch",
          "attendanceStatus": "PLANNED",
          "isMakeup": false,
          "makeupInfo": null
        }
      ],
      "TUESDAY": [],
      "WEDNESDAY": [],
      "THURSDAY": [],
      "FRIDAY": [],
      "SATURDAY": [],
      "SUNDAY": []
    }
  }
}
```

### 3.2 Session Detail Response

**Endpoint**: `GET /api/v1/students/me/sessions/1001`

```json
{
  "success": true,
  "message": "Session details retrieved successfully",
  "data": {
    "sessionId": 1001,
    "studentSessionId": 5001,
    "date": "2025-11-04",
    "dayOfWeek": "MONDAY",
    "startTime": "08:00:00",
    "endTime": "10:00:00",
    "timeSlotName": "HN Morning 1",
    "classInfo": {
      "classId": 10,
      "classCode": "HN-FOUND-O1",
      "className": "IELTS Foundation - Oct 2025",
      "courseId": 1,
      "courseName": "IELTS Foundation",
      "teacherId": 50,
      "teacherName": "Mr. Nguyen Van A",
      "branchId": 1,
      "branchName": "Hanoi Branch",
      "modality": "OFFLINE"
    },
    "sessionInfo": {
      "topic": "Introduction to IELTS",
      "description": "Overview of IELTS exam structure and scoring",
      "sessionType": "CLASS",
      "sessionStatus": "PLANNED",
      "location": "Hanoi Branch",
      "onlineLink": null
    },
    "studentStatus": {
      "attendanceStatus": "PLANNED",
      "homeworkStatus": "NOT_SUBMITTED",
      "homeworkDueDate": "2025-11-06",
      "homeworkDescription": "Complete Workbook pages 10-15"
    },
    "materials": [
      {
        "materialId": 201,
        "fileName": "Slide_Intro_IELTS.pdf",
        "fileUrl": "/api/v1/materials/download/201",
        "uploadedAt": "2025-10-30T10:00:00"
      },
      {
        "materialId": 202,
        "fileName": "Vocabulary_List.pdf",
        "fileUrl": "/api/v1/materials/download/202",
        "uploadedAt": "2025-10-30T10:05:00"
      }
    ],
    "makeupInfo": null
  }
}
```

### 3.3 Current Week Response

**Endpoint**: `GET /api/v1/students/me/current-week`

```json
{
  "success": true,
  "message": "Current week start retrieved successfully",
  "data": "2025-11-04"
}
```

## 4. Validation Rules & Error Handling

### 4.1 Input Validation

| Parameter | Validation | Error Response |
|-----------|------------|----------------|
| `weekStart` | Must be Monday (ISO 8601 format: YYYY-MM-DD) | `400 Bad Request` |
| `sessionId` | Must be valid session ID enrolled for student | `404 Not Found` |

### 4.2 Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### 4.3 Common Error Scenarios

**Invalid weekStart (not Monday)**:
```json
{
  "success": false,
  "message": "weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)",
  "data": null
}
```

**Session not found or not enrolled**:
```json
{
  "success": false,
  "message": "Session not found or you are not enrolled in this session",
  "data": null
}
```

**Unauthorized access**:
```json
{
  "success": false,
  "message": "Access denied: Student role required",
  "data": null
}
```

## 5. Business Rules & Logic

### 5.1 Enum Values

**SessionStatus**:
- `PLANNED` - Session scheduled but not yet occurred
- `DONE` - Session completed
- `CANCELLED` - Session was cancelled

**SessionType**:
- `CLASS` - Regular class session
- `TEACHER_RESCHEDULE` - Rescheduled by teacher

**AttendanceStatus**:
- `PLANNED` - Student planned to attend (default)
- `PRESENT` - Student attended
- `ABSENT` - Student was absent

**Modality**:
- `OFFLINE` - In-person at branch
- `ONLINE` - Virtual session
- `HYBRID` - Mixed modality

### 5.2 Important Business Rules

1. **Student Data Isolation**: Students can only view their own schedules, not other students' data
2. **Week Structure**: Week always starts on Monday and ends on Sunday
3. **Authorization**: All endpoints require `ROLE_STUDENT` and valid JWT token
4. **Makeup Sessions**: When `isMakeup: true`, `makeupInfo` contains original session details
5. **Materials**: File URLs are relative and should be prefixed with API base URL

## 6. Flow Sequence

### 6.1 Initial Load Flow
1. Call `/api/v1/students/me/current-week` to get current week Monday
2. Call `/api/v1/students/me/schedule?weekStart={currentWeek}` to load initial schedule
3. Display weekly grid with time slots and sessions

### 6.2 Week Navigation Flow
1. User clicks "Previous Week" or "Next Week"
2. Calculate new Monday date (±7 days)
3. Call `/api/v1/students/me/schedule?weekStart={newWeekDate}`
4. Update schedule grid with new data

### 6.3 Session Detail Flow
1. User clicks on a session cell in the weekly grid
2. Extract `sessionId` from the clicked session
3. Call `/api/v1/students/me/sessions/{sessionId}`
4. Display modal with detailed session information
5. Show materials list with download links
6. Display attendance status and homework information

## 7. Implementation Notes for Frontend

### 7.1 Authentication Headers
All requests must include JWT token:
```
Authorization: Bearer <jwt_token>
```

### 7.2 Date Handling
- All dates are in ISO 8601 format (YYYY-MM-DD)
- Times are in 24-hour format (HH:mm:ss)
- Use `dayOfWeek` values from Java DayOfWeek enum: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY

### 7.3 Schedule Grid Rendering
- Use `timeSlots` array for Y-axis (rows)
- Use `schedule` object keys for X-axis (columns)
- Each day contains an array of sessions sorted by time
- Handle empty cells when no sessions exist for a specific time slot

### 7.4 Material Downloads
- Use `fileUrl` from materials array
- Prefix with API base URL: `${API_BASE_URL}${material.fileUrl}`
- Handle file downloads appropriately for PDF documents

### 7.5 Loading States
- Show loading spinner during API calls
- Handle empty schedule states gracefully
- Display error messages for failed requests

### 7.6 Responsive Considerations
- Schedule grid should be responsive for mobile/tablet
- Session detail modal should be accessible on all screen sizes
- Week navigation should be touch-friendly

## 8. Testing Examples

### 8.1 Get Weekly Schedule

```bash
curl -X GET "http://localhost:8080/api/v1/students/me/schedule?weekStart=2025-11-04" \
  -H "Authorization: Bearer <student_jwt_token>" \
  -H "Content-Type: application/json"
```

### 8.2 Get Session Details

```bash
curl -X GET "http://localhost:8080/api/v1/students/me/sessions/1001" \
  -H "Authorization: Bearer <student_jwt_token>" \
  -H "Content-Type: application/json"
```

### 8.3 Get Current Week

```bash
curl -X GET "http://localhost:8080/api/v1/students/me/current-week" \
  -H "Authorization: Bearer <student_jwt_token>" \
  -H "Content-Type: application/json"
```

### 8.4 Error Example (Invalid Week Start)

```bash
curl -X GET "http://localhost:8080/api/v1/students/me/schedule?weekStart=2025-11-05" \
  -H "Authorization: Bearer <student_jwt_token>" \
  -H "Content-Type: application/json"

# Response: 400 Bad Request
# {
#   "success": false,
#   "message": "weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)",
#   "data": null
# }
```

### 8.5 Error Example (Session Not Found)

```bash
curl -X GET "http://localhost:8080/api/v1/students/me/sessions/99999" \
  -H "Authorization: Bearer <student_jwt_token>" \
  -H "Content-Type: application/json"

# Response: 404 Not Found
# {
#   "success": false,
#   "message": "Session not found or you are not enrolled in this session",
#   "data": null
# }
```

---

## Additional Notes

- **API Base URL**: Replace `http://localhost:8080` with your actual API base URL
- **JWT Token**: Obtain token from authentication endpoint (`/api/v1/auth/login`)
- **Error Handling**: Always check `success` field before accessing `data`
- **CORS**: Ensure frontend domain is configured in backend CORS settings
- **Performance**: Consider caching weekly schedule data to reduce API calls during navigation

This API provides all necessary data for building a complete student schedule viewing experience with weekly grid layout, session details, and proper navigation functionality.