# ✅ IMPLEMENTATION CHECKLIST: Class Creation Workflow

**Project:** Training Management System (TMS)
**Feature:** Complete Class Creation Workflow (7 Steps)
**Version:** 1.1.0
**Date:** January 4, 2025

---

## 📋 TABLE OF CONTENTS

1. [Pre-Implementation Setup](#pre-implementation-setup)
2. [Step 1: Create Class](#step-1-create-class)
3. [Step 2: Generate Sessions (Auto)](#step-2-generate-sessions-auto)
4. [Step 3: Assign Time Slots](#step-3-assign-time-slots)
5. [Step 4: Assign Resources](#step-4-assign-resources)
6. [Step 5: Assign Teachers](#step-5-assign-teachers)
7. [Step 6: Validate](#step-6-validate)
8. [Step 7: Submit & Approve](#step-7-submit--approve)
9. [Testing Tasks](#testing-tasks)
10. [Deployment Tasks](#deployment-tasks)
11. [Documentation Tasks](#documentation-tasks)

---

## 🔧 PRE-IMPLEMENTATION SETUP

### Database Setup
- [ ] Review existing schema for `class` table
- [ ] Review existing schema for `session` table
- [ ] Review existing schema for `session_resource` table
- [ ] Review existing schema for `teaching_slot` table
- [ ] Review existing schema for `time_slot_template` table
- [ ] Verify PostgreSQL enums exist: `modality_enum`, `class_status_enum`, `approval_status_enum`
- [ ] Add missing columns to `class` table if needed:
  - [ ] `submitted_at TIMESTAMP WITH TIME ZONE`
  - [ ] `approved_by BIGINT REFERENCES user_account(id)`
  - [ ] `approved_at TIMESTAMP WITH TIME ZONE`
  - [ ] `rejection_reason TEXT`
- [ ] Add index on `class(branch_id, code)` for uniqueness check
- [ ] Add index on `session(class_id, date)` for performance
- [ ] Add index on `session_resource(resource_id, session_id)` for conflict detection

### Project Structure
- [ ] Create package `org.fyp.tmssep490be.dtos.classmanagement`
- [ ] Verify package `org.fyp.tmssep490be.services.impl` exists
- [ ] Create `ClassManagementController.java` in controllers package
- [ ] Update `application.yml` if needed for new configurations

### Dependencies
- [ ] Verify Spring Boot 3.5.7 and Java 21 are configured
- [ ] Verify PostgreSQL driver is up to date
- [ ] Verify Lombok is working
- [ ] Verify validation dependencies (`spring-boot-starter-validation`)
- [ ] Verify TestContainers for integration tests

---

## 📘 STEP 1: CREATE CLASS

### Backend - Entities
- [ ] Review/update `ClassEntity.java`
  - [ ] Add `scheduleDays` field (List&lt;Integer&gt;)
  - [ ] Add `submittedAt` field
  - [ ] Add `approvedBy` field
  - [ ] Add `approvedAt` field
  - [ ] Add `rejectionReason` field
  - [ ] Add `@UniqueConstraint` on `(branch_id, code)`
  - [ ] Verify `BaseEntity` extends for auditing

### Backend - DTOs
- [ ] Create `CreateClassRequest.java`
  - [ ] Add validation annotations
  - [ ] Add `@Pattern` for class code format
  - [ ] Add `@FutureOrPresent` for start date
  - [ ] Add `scheduleDays` validation (1-7 values)
  - [ ] Add `maxCapacity` validation (1-100)
- [ ] Create `CreateClassResponse.java`
  - [ ] Include `sessionsGenerated` field
  - [ ] Include all class fields

### Backend - Repository
- [ ] Update `ClassRepository.java`
  - [ ] Add `existsByBranchAndCode(Branch, String)` method
  - [ ] Add custom query if needed

### Backend - Service
- [ ] Create/update `ClassService.java` interface
  - [ ] Add `CreateClassResponse createClass(CreateClassRequest, Long userId)` signature
- [ ] Create/update `ClassServiceImpl.java`
  - [ ] Implement `createClass()` method
  - [ ] Validate course exists and is APPROVED
  - [ ] Validate branch exists
  - [ ] Validate start_date is in schedule_days
  - [ ] Check unique (branch_id, code)
  - [ ] Create ClassEntity
  - [ ] Set status = DRAFT, approval_status = PENDING
  - [ ] Call `sessionGenerationService.generateSessions()`
  - [ ] Calculate and set `planned_end_date`
  - [ ] Return response with sessionsGenerated count

### Backend - Controller
- [ ] Create `ClassManagementController.java`
  - [ ] Add `@RestController` and `@RequestMapping("/api/v1/classes")`
  - [ ] Implement `POST /` endpoint for createClass
  - [ ] Add `@PreAuthorize("hasRole('ACADEMIC_STAFF')")`
  - [ ] Add `@Valid` for request validation
  - [ ] Return `ResponseObject&lt;CreateClassResponse&gt;` with 201 status

### Frontend - Screens
- [ ] Create `CreateClassForm.tsx` component
  - [ ] Branch selection dropdown (load from API)
  - [ ] Course selection dropdown (filter by branch, APPROVED only)
  - [ ] Class code input with format validation
  - [ ] Class name input (optional)
  - [ ] Modality radio buttons (OFFLINE/ONLINE/HYBRID)
  - [ ] Start date picker
  - [ ] Schedule days multi-select (Mon-Sun)
  - [ ] Max capacity number input (1-100)
  - [ ] Estimated schedule summary display
  - [ ] Client-side validation
  - [ ] Submit button with loading state
- [ ] Create `CreateClassSuccessModal.tsx`
  - [ ] Show sessions generated count
  - [ ] Show duration and dates
  - [ ] Navigate to next step button

### Frontend - API Integration
- [ ] Create `ClassService.ts`
  - [ ] Implement `createClass(request)` method
  - [ ] Implement `getApprovedCourses(branchId)` method
  - [ ] Add error handling

### Testing - Step 1
- [ ] **Unit Tests** (`ClassServiceImplTest.java`)
  - [ ] Test successful class creation
  - [ ] Test course not approved throws exception
  - [ ] Test start date not in schedule days throws exception
  - [ ] Test duplicate class code throws exception
  - [ ] Test branch not found throws exception
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes` with valid data returns 201
  - [ ] Test POST with invalid data returns 400
  - [ ] Test POST without authentication returns 401
  - [ ] Test POST without ACADEMIC_STAFF role returns 403

---

## 🔄 STEP 2: GENERATE SESSIONS (AUTO)

### Backend - Service
- [ ] Create `SessionGenerationService.java`
  - [ ] Implement `generateSessions(ClassEntity)` method
  - [ ] Load course_sessions from course (ordered by phase, sequence)
  - [ ] Implement date calculation algorithm
  - [ ] Handle schedule_days rotation
  - [ ] Create Session entities with calculated dates
  - [ ] Set type = CLASS, status = PLANNED
  - [ ] Batch save all sessions
  - [ ] Return count of sessions generated
  - [ ] Add logging for debugging

### Backend - Repository
- [ ] Update `SessionRepository.java`
  - [ ] Verify save/saveAll methods exist
  - [ ] Add `findTopByClassEntityOrderByDateDesc()` for planned_end_date calculation
  - [ ] Add `countByClassEntityId()` method

### Frontend - Screens
- [ ] Create `SessionGenerationProgress.tsx` component
  - [ ] Show loading spinner
  - [ ] Show progress bar (if possible)
  - [ ] Show generation status messages
- [ ] Create `SessionGenerationComplete.tsx` modal
  - [ ] Show total sessions generated
  - [ ] Show duration (weeks)
  - [ ] Show first and last session dates
  - [ ] Show schedule pattern breakdown
  - [ ] Continue button

### Testing - Step 2
- [ ] **Unit Tests** (`SessionGenerationServiceTest.java`)
  - [ ] Test correct number of sessions generated
  - [ ] Test sessions on correct dates (verify algorithm)
  - [ ] Test date calculation for Mon/Wed/Fri pattern
  - [ ] Test date calculation for different schedule patterns
  - [ ] Test sessions linked to correct course_sessions
  - [ ] Test exception when course has no sessions
- [ ] **Integration Tests**
  - [ ] Test end-to-end: create class → verify sessions in DB
  - [ ] Test sessions have correct dates matching schedule_days

---

## ⏰ STEP 3: ASSIGN TIME SLOTS

### Backend - DTOs
- [ ] Create `AssignTimeSlotsRequest.java`
  - [ ] Create nested `Assignment` class (dayOfWeek, timeSlotTemplateId)
  - [ ] Add validation annotations
- [ ] Create `AssignTimeSlotsResponse.java`
  - [ ] Include assigned sessions count
  - [ ] Create nested `AssignmentResult` class
  - [ ] Include breakdown by day

### Backend - Repository
- [ ] Update `SessionRepository.java`
  - [ ] Add `updateTimeSlotByClassAndDayOfWeek()` method
  - [ ] Use JPQL with `EXTRACT(ISODOW FROM date)`
  - [ ] Add `@Modifying` annotation
  - [ ] Update `updatedAt` timestamp

### Backend - Service
- [ ] Create `TimeSlotAssignmentService.java`
  - [ ] Implement `assignTimeSlots(classId, request)` method
  - [ ] Validate class exists
  - [ ] Validate all time slot templates exist
  - [ ] Loop through assignments
  - [ ] Call repository method to update sessions per day
  - [ ] Build response with results per day
  - [ ] Add logging

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/time-slots` endpoint
  - [ ] Add `@PreAuthorize("hasRole('ACADEMIC_STAFF')")`
  - [ ] Add `@Valid` for request validation
  - [ ] Return `ResponseObject&lt;AssignTimeSlotsResponse&gt;`

### Frontend - Screens
- [ ] Create `AssignTimeSlotsScreen.tsx`
  - [ ] Load time slot templates for branch
  - [ ] Display one dropdown per schedule day
  - [ ] Show time slot options with time ranges
  - [ ] Implement "Apply to All Days" button
  - [ ] Show summary of assignments
  - [ ] Show count per day
  - [ ] Enable/disable continue based on completion
- [ ] Add validation for all days assigned

### Frontend - API Integration
- [ ] Update `ClassService.ts`
  - [ ] Implement `assignTimeSlots(classId, request)` method
  - [ ] Implement `getTimeSlotTemplates(branchId)` method

### Testing - Step 3
- [ ] **Unit Tests** (`TimeSlotAssignmentServiceTest.java`)
  - [ ] Test successful time slot assignment
  - [ ] Test class not found throws exception
  - [ ] Test time slot template not found throws exception
  - [ ] Test correct number of sessions updated per day
- [ ] **Repository Tests**
  - [ ] Test `updateTimeSlotByClassAndDayOfWeek()` SQL query
  - [ ] Test ISODOW extraction works correctly
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/time-slots` returns 200
  - [ ] Test time slots correctly assigned in database

---

## 🏫 STEP 4: ASSIGN RESOURCES

### Backend - DTOs
- [ ] Create `AssignResourcesRequest.java`
  - [ ] Create nested `PatternItem` class (dayOfWeek, resourceId)
  - [ ] Add validation
- [ ] Create `AssignResourcesResponse.java`
  - [ ] Include successCount, conflictCount
  - [ ] Create nested `ConflictDetail` class
  - [ ] Include session info, conflict reason, conflicting class
- [ ] Create `ResourceDTO.java` for available resources

### Backend - Repository
- [ ] Update `SessionResourceRepository.java`
  - [ ] Add `bulkAssignResource()` native SQL query
  - [ ] Use INSERT with NOT EXISTS for conflict avoidance
  - [ ] Use EXTRACT(ISODOW FROM date) for day filtering
  - [ ] Return count of inserted rows
- [ ] Update `SessionRepository.java`
  - [ ] Add `findUnassignedSessionsByDayOfWeek()` query
  - [ ] Add `findConflictingSession()` query for conflict analysis
  - [ ] Add `countByClassEntityId()` if not exists
- [ ] Update `ResourceRepository.java`
  - [ ] Add `findAvailableResources()` query
  - [ ] Filter by branch, resource type, capacity, date, time slot

### Backend - Service
- [ ] Create `ResourceAssignmentService.java`
  - [ ] Implement `assignResources(classId, request)` method
    - [ ] **PHASE 1: SQL Bulk Assignment**
      - [ ] Loop through pattern items
      - [ ] Call `bulkAssignResource()` for each day
      - [ ] Accumulate success count
    - [ ] **PHASE 2: Conflict Analysis**
      - [ ] Find unassigned sessions per day
      - [ ] Analyze each conflict with `analyzeResourceConflict()`
      - [ ] Build conflict details with reason
  - [ ] Implement `analyzeResourceConflict()` helper
    - [ ] Find conflicting session
    - [ ] Determine reason (booked by class X, maintenance, etc.)
    - [ ] Return ConflictDetail
  - [ ] Implement `getAvailableResources()` method
    - [ ] Determine search criteria (from session or params)
    - [ ] Query available resources from repository
    - [ ] Filter by modality (ROOM for OFFLINE, ONLINE_ACCOUNT for ONLINE)

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/resources` endpoint
  - [ ] Add `GET /{classId}/available-resources` endpoint with query params
  - [ ] Add authorization checks
  - [ ] Handle success/conflict messages dynamically

### Frontend - Screens
- [ ] Create `AssignResourcesScreen.tsx`
  - [ ] **Week 1 Pattern View**
    - [ ] Display representative sessions (first week)
    - [ ] Show session details (date, time, topic, skills)
    - [ ] Resource selection dropdown per session
    - [ ] Load available resources from API
    - [ ] Show availability status (available/booked)
    - [ ] Show same resource message for subsequent days
  - [ ] **Auto-Propagation Settings**
    - [ ] Checkboxes for each day (apply pattern)
    - [ ] Assign button
- [ ] Create `AutoPropagationProgress.tsx` modal
  - [ ] Show progress bar
  - [ ] Show assignment status per day
- [ ] Create `ConflictReportScreen.tsx`
  - [ ] Show success count vs conflict count
  - [ ] List conflict details
    - [ ] Session number, date, time
    - [ ] Reason (booked by class X, maintenance)
    - [ ] Alternative resources
    - [ ] Quick assign buttons
  - [ ] Resolve all conflicts button
  - [ ] Continue with conflicts option

### Frontend - API Integration
- [ ] Update `ClassService.ts`
  - [ ] Implement `assignResources(classId, request)` method
  - [ ] Implement `getAvailableResources(classId, params)` method
  - [ ] Implement `resolveConflict(classId, sessionId, resourceId)` method

### Testing - Step 4
- [ ] **Unit Tests** (`ResourceAssignmentServiceTest.java`)
  - [ ] Test bulk assignment returns correct count
  - [ ] Test conflict detection works
  - [ ] Test conflict analysis provides correct reason
  - [ ] Test available resources query filters correctly
- [ ] **Repository Tests**
  - [ ] Test `bulkAssignResource()` SQL performance
  - [ ] Test conflict detection query
  - [ ] Test available resources query
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/resources` with no conflicts
  - [ ] Test POST with conflicts returns conflict details
  - [ ] Test GET `/api/v1/classes/{id}/available-resources` returns filtered list

---

## 👨‍🏫 STEP 5: ASSIGN TEACHERS

### Backend - DTOs
- [ ] Create `AvailableTeachersResponse.java`
  - [ ] Include classId, totalSessions
  - [ ] Include list of `TeacherAvailabilityDTO`
- [ ] Create `TeacherAvailabilityDTO.java`
  - [ ] Teacher basic info (id, name, employee code, contract type)
  - [ ] Skills array
  - [ ] Availability stats (totalSessions, availableSessions, percentage)
  - [ ] Conflict breakdown (noAvailability, teachingConflict, leaveConflict)
  - [ ] Availability status (fully_available, partially_available, unavailable)
- [ ] Create `AssignTeacherRequest.java`
  - [ ] teacherId, role (primary/substitute)
  - [ ] sessionIds (optional, for substitute)
- [ ] Create `AssignTeacherResponse.java`
  - [ ] Include assignedCount, needsSubstitute
  - [ ] Include remainingSessions list if partial

### Backend - Repository
- [ ] Update `TeacherRepository.java`
  - [ ] Add `findAvailableTeachersWithPreCheck()` native SQL query
  - [ ] **CTE 1: skill_matched_teachers**
    - [ ] Match teachers by skill (or has 'general')
    - [ ] Aggregate skills per teacher
  - [ ] **CTE 2: session_conflicts**
    - [ ] Cross join teachers with sessions
    - [ ] Count conflicts per type (no availability, teaching conflict, leave)
  - [ ] **Main Query**
    - [ ] Calculate availableSessions = total - conflicts
    - [ ] Calculate availability percentage
    - [ ] Determine availability status
    - [ ] Order by contract type, available sessions DESC
- [ ] Update `SessionRepository.java`
  - [ ] Add `findSessionsMatchingTeacherSkills()` query
  - [ ] Add `findUnassignedSessions()` query
  - [ ] Add `countByClassEntityId()` if not exists

### Backend - Service
- [ ] Create `TeacherAssignmentService.java`
  - [ ] Implement `getAvailableTeachersWithPreCheck(classId, skillFilter)` method
    - [ ] Call repository query
    - [ ] Return response with teacher list sorted by availability
  - [ ] Implement `assignTeacher(classId, request)` method
    - [ ] Validate class and teacher exist
    - [ ] Determine sessions to assign (all matching or specific IDs)
    - [ ] Create TeachingSlot entities
    - [ ] Batch save teaching slots
    - [ ] Check if needs substitute (assigned < total)
    - [ ] Return response with assignment details

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `GET /{classId}/available-teachers` endpoint
    - [ ] Optional skillSet query param
    - [ ] Return `AvailableTeachersResponse`
  - [ ] Add `POST /{classId}/teachers` endpoint
    - [ ] Accept `AssignTeacherRequest`
    - [ ] Return `AssignTeacherResponse`
    - [ ] Dynamic success message based on needsSubstitute

### Frontend - Screens
- [ ] Create `TeacherSelectionScreen.tsx`
  - [ ] Load teachers with PRE-CHECK from API
  - [ ] **Filter Section**
    - [ ] Skill filter dropdown
  - [ ] **Fully Available Section**
    - [ ] List teachers with 100% availability
    - [ ] Show teacher info (name, code, contract type)
    - [ ] Show skills with levels
    - [ ] Show availability (36/36 100%)
    - [ ] Show "No conflicts" badge
    - [ ] Quick assign button
    - [ ] Highlight recommended (first in list)
  - [ ] **Partially Available Section**
    - [ ] List teachers with &lt;100% availability
    - [ ] Show availability percentage
    - [ ] Show conflict breakdown
      - [ ] No availability (count)
      - [ ] Teaching conflicts (count)
      - [ ] Leave conflicts (count)
    - [ ] Assign to available sessions button
    - [ ] View conflict details button
  - [ ] Sort by: contract type (full-time first), availability DESC
- [ ] Create `AssignmentSuccessModal.tsx`
  - [ ] Show teacher name and assigned count
  - [ ] Show "No substitute needed" if 100%
  - [ ] Continue button
- [ ] Create `PartialAssignmentModal.tsx`
  - [ ] Show assigned count vs total
  - [ ] List remaining sessions with reasons
  - [ ] Find substitute button
  - [ ] Show available substitutes for remaining sessions
  - [ ] Quick assign substitute option

### Frontend - API Integration
- [ ] Update `ClassService.ts`
  - [ ] Implement `getAvailableTeachers(classId, skillSet?)` method
  - [ ] Implement `assignTeacher(classId, request)` method

### Testing - Step 5
- [ ] **Unit Tests** (`TeacherAssignmentServiceTest.java`)
  - [ ] Test PRE-CHECK returns fully available teachers
  - [ ] Test PRE-CHECK returns partially available teachers
  - [ ] Test PRE-CHECK calculates conflict counts correctly
  - [ ] Test direct assignment without re-checking
  - [ ] Test partial assignment sets needsSubstitute = true
  - [ ] Test 'general' skill matches all sessions
- [ ] **Repository Tests**
  - [ ] Test `findAvailableTeachersWithPreCheck()` SQL query
  - [ ] Test skill matching logic
  - [ ] Test conflict detection (3 types)
  - [ ] Test sorting order
- [ ] **Integration Tests**
  - [ ] Test GET `/api/v1/classes/{id}/available-teachers` returns sorted list
  - [ ] Test POST `/api/v1/classes/{id}/teachers` assigns successfully
  - [ ] Test POST with specific sessionIds (substitute scenario)

---

## ✅ STEP 6: VALIDATE

### Backend - DTOs
- [ ] Create `ValidateClassResponse.java`
  - [ ] isValid, canSubmit flags
  - [ ] totalSessions count
  - [ ] Create nested `Checks` class
    - [ ] timeSlotsAssigned, resourcesAssigned, teachersAssigned flags
  - [ ] errors list (blocking issues)
  - [ ] warnings list (non-blocking issues)

### Backend - Repository
- [ ] Update `SessionRepository.java`
  - [ ] Add `countByClassEntityIdAndTimeSlotTemplateIsNull()` method
  - [ ] Add `countByClassEntityIdAndNotHavingResources()` custom query
  - [ ] Add `countByClassEntityIdAndNotHavingTeachers()` custom query
  - [ ] Add `findSessionsWithoutTeachers()` for detailed error
- [ ] Update `TeachingSlotRepository.java`
  - [ ] Add `countDistinctTeachersByClassId()` method

### Backend - Service
- [ ] Update `ClassServiceImpl.java`
  - [ ] Implement `validateClass(classId)` method
    - [ ] **Check 1: Time slots**
      - [ ] Count sessions without time_slot_template
      - [ ] Add error if count &gt; 0
    - [ ] **Check 2: Resources**
      - [ ] Count sessions without session_resource
      - [ ] Add error if count &gt; 0
    - [ ] **Check 3: Teachers**
      - [ ] Count sessions without teaching_slot
      - [ ] Add error with session IDs if count &gt; 0
    - [ ] **Warning 1: Multiple teachers**
      - [ ] Count distinct teachers
      - [ ] Add warning if &gt; 1
    - [ ] **Warning 2: Start date in past**
      - [ ] Check if start_date &lt; today
      - [ ] Add warning if true
    - [ ] Set isValid = errors.isEmpty()
    - [ ] Return validation response

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/validate` endpoint
  - [ ] Return `ResponseObject&lt;ValidateClassResponse&gt;`
  - [ ] Dynamic message based on isValid

### Frontend - Screens
- [ ] Create `ValidationScreen.tsx`
  - [ ] Call validate API on mount
  - [ ] **Success View**
    - [ ] Show checkmark icon
    - [ ] Show "Validation Complete" title
    - [ ] Display setup progress
      - [ ] Step 1: Class Created ✅
      - [ ] Step 2: Sessions Generated ✅
      - [ ] Step 3: Time Slots Assigned (with details) ✅
      - [ ] Step 4: Resources Assigned (with breakdown) ✅
      - [ ] Step 5: Teachers Assigned (with breakdown) ✅
      - [ ] Step 6: Validation Passed ✅
    - [ ] Show warnings if any (non-blocking)
    - [ ] Show class summary
    - [ ] Submit for Approval button (enabled)
  - [ ] **Failure View**
    - [ ] Show error icon
    - [ ] Show "Validation Failed" title
    - [ ] List errors with links to fix
      - [ ] "X sessions missing teachers" → Go to Step 5
      - [ ] "X sessions missing resources" → Go to Step 4
    - [ ] Show completed steps with percentages
    - [ ] Submit button (disabled)
    - [ ] Fix Issues button
    - [ ] Save as Draft button

### Frontend - API Integration
- [ ] Update `ClassService.ts`
  - [ ] Implement `validateClass(classId)` method

### Testing - Step 6
- [ ] **Unit Tests** (`ClassServiceImplTest.java`)
  - [ ] Test validation passes when all complete
  - [ ] Test validation fails when missing time slots
  - [ ] Test validation fails when missing resources
  - [ ] Test validation fails when missing teachers
  - [ ] Test validation includes warnings
  - [ ] Test canSubmit = false when errors exist
- [ ] **Repository Tests**
  - [ ] Test counting methods for incomplete assignments
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/validate` returns valid response
  - [ ] Test validation with incomplete class returns errors

---

## 📨 STEP 7: SUBMIT & APPROVE

### Backend - DTOs
- [ ] Create `SubmitClassResponse.java`
  - [ ] Include classId, code, status, approvalStatus
  - [ ] Include submittedAt, submittedBy
- [ ] Create `ApproveClassResponse.java`
  - [ ] Include classId, code, status, approvalStatus
  - [ ] Include approvedBy, approvedAt
- [ ] Create `RejectClassRequest.java`
  - [ ] reason field (validation: min 10 chars)
- [ ] Create `RejectClassResponse.java`
  - [ ] Include classId, code, status, approvalStatus
  - [ ] Include rejectionReason, rejectedBy, rejectedAt

### Backend - Service
- [ ] Update `ClassServiceImpl.java`
  - [ ] Implement `submitClass(classId, userId)` method
    - [ ] Validate class exists
    - [ ] Call validateClass() to ensure completeness
    - [ ] Throw exception if !canSubmit
    - [ ] Set submittedAt = now
    - [ ] Save class
    - [ ] Call notificationService to notify Center Head
    - [ ] Return response
  - [ ] Implement `approveClass(classId, userId)` method
    - [ ] Validate class exists
    - [ ] Check submittedAt is not null
    - [ ] Set status = SCHEDULED
    - [ ] Set approvalStatus = APPROVED
    - [ ] Set approvedBy = userId
    - [ ] Set approvedAt = now
    - [ ] Save class
    - [ ] Call notificationService to notify Academic Staff
    - [ ] Return response
  - [ ] Implement `rejectClass(classId, reason, userId)` method
    - [ ] Validate class exists
    - [ ] Set status = DRAFT
    - [ ] Set approvalStatus = REJECTED
    - [ ] Set rejectionReason = reason
    - [ ] Set submittedAt = null (reset)
    - [ ] Save class
    - [ ] Call notificationService to notify Academic Staff
    - [ ] Return response

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/submit` endpoint
    - [ ] `@PreAuthorize("hasRole('ACADEMIC_STAFF')")`
    - [ ] Return `SubmitClassResponse`
  - [ ] Add `POST /{classId}/approve` endpoint
    - [ ] `@PreAuthorize("hasRole('CENTER_HEAD')")`
    - [ ] Return `ApproveClassResponse`
  - [ ] Add `POST /{classId}/reject` endpoint
    - [ ] `@PreAuthorize("hasRole('CENTER_HEAD')")`
    - [ ] Accept `RejectClassRequest` with validation
    - [ ] Return `RejectClassResponse`

### Backend - Notification Service (Optional/Future)
- [ ] Create `NotificationService.java` (if not exists)
  - [ ] Implement `notifyClassSubmission(classEntity)` stub
  - [ ] Implement `notifyClassApproved(classEntity)` stub
  - [ ] Implement `notifyClassRejected(classEntity, reason)` stub
  - [ ] *Note: Full notification implementation can be done later*

### Frontend - Screens (Academic Staff)
- [ ] Create `SubmitConfirmationModal.tsx`
  - [ ] Show class summary
  - [ ] List all requirements met
  - [ ] Explain post-submission process
  - [ ] Cancel and Submit buttons
- [ ] Create `SubmissionSuccessModal.tsx`
  - [ ] Show success message
  - [ ] Show notification sent to Center Head
  - [ ] Show current status (Pending Approval)
  - [ ] View Class / Track Status buttons

### Frontend - Screens (Center Head)
- [ ] Create `ClassApprovalScreen.tsx`
  - [ ] Load class details from API
  - [ ] **Class Information Section**
    - [ ] Show basic info (code, branch, course, etc.)
    - [ ] Show submitted by and submitted at
  - [ ] **Sessions Summary Section**
    - [ ] Total sessions, duration, dates
    - [ ] Time slots breakdown
  - [ ] **Resource Assignment Section**
    - [ ] List resources used with session counts
    - [ ] Show coverage percentage
  - [ ] **Teacher Assignment Section**
    - [ ] List teachers with session counts and skills
    - [ ] Show coverage percentage
  - [ ] **Warnings Section** (if any)
  - [ ] **Decision Section**
    - [ ] Radio buttons: Approve / Reject
    - [ ] Rejection reason textarea (required if Reject selected)
    - [ ] Min 10 characters validation
  - [ ] Submit Decision button
- [ ] Create `ApprovalSuccessModal.tsx`
  - [ ] Show success message
  - [ ] Show updated status (SCHEDULED)
  - [ ] Show approval timestamp
  - [ ] Notification sent message
- [ ] Create `RejectionSuccessModal.tsx`
  - [ ] Show rejection confirmation
  - [ ] Echo rejection reason
  - [ ] Show updated status (DRAFT)
  - [ ] Notification sent message

### Frontend - API Integration
- [ ] Update `ClassService.ts`
  - [ ] Implement `submitClass(classId)` method
  - [ ] Implement `approveClass(classId)` method
  - [ ] Implement `rejectClass(classId, reason)` method

### Testing - Step 7
- [ ] **Unit Tests** (`ClassServiceImplTest.java`)
  - [ ] Test submitClass() succeeds when valid
  - [ ] Test submitClass() throws exception when invalid
  - [ ] Test approveClass() updates status correctly
  - [ ] Test approveClass() throws exception if not submitted
  - [ ] Test rejectClass() resets submission and sets reason
  - [ ] Test notification service called in all methods
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/submit` returns 200
  - [ ] Test POST `/api/v1/classes/{id}/approve` by CENTER_HEAD returns 200
  - [ ] Test POST `/api/v1/classes/{id}/approve` by ACADEMIC_STAFF returns 403
  - [ ] Test POST `/api/v1/classes/{id}/reject` with reason returns 200
  - [ ] Test POST `/api/v1/classes/{id}/reject` without reason returns 400

---

## 🧪 TESTING TASKS

### Unit Testing
- [ ] Write tests for all service methods (target 90%+ coverage)
- [ ] Write tests for custom repository queries
- [ ] Write tests for DTO validations
- [ ] Write tests for exception scenarios
- [ ] Write tests for edge cases (boundary values)
- [ ] Use Mockito/MockitoBean for mocking
- [ ] Use AssertJ for fluent assertions
- [ ] Follow AAA pattern (Arrange-Act-Assert)

### Integration Testing
- [ ] Set up TestContainers with PostgreSQL
- [ ] Write end-to-end workflow tests
  - [ ] Test: Create → Generate → Assign All → Submit → Approve
  - [ ] Test: Create → Partial Assign → Validate Fails
  - [ ] Test: Submit → Reject → Fix → Resubmit → Approve
- [ ] Test API endpoints with Spring MockMvc or REST Assured
- [ ] Test authentication and authorization
- [ ] Test concurrent access scenarios
- [ ] Test database constraints (unique, foreign keys)
- [ ] Test rollback scenarios
- [ ] Achieve 80%+ integration test coverage

### Repository Testing
- [ ] Test custom JPQL queries
- [ ] Test native SQL queries
- [ ] Test EXTRACT(ISODOW) functionality
- [ ] Test bulk operations performance
- [ ] Test transaction boundaries

### API Testing
- [ ] Create Postman collection for all endpoints
- [ ] Test all CRUD operations
- [ ] Test pagination and filtering
- [ ] Test error responses (400, 401, 403, 404, 500)
- [ ] Test request validation
- [ ] Test response format consistency

### Performance Testing
- [ ] Test bulk resource assignment (target: &lt;200ms)
- [ ] Test teacher PRE-CHECK query performance
- [ ] Test session generation for large classes
- [ ] Test with realistic data volumes (100+ classes)
- [ ] Identify and optimize slow queries

### Frontend Testing (Reference)
- [ ] Unit test components
- [ ] Test form validations
- [ ] Test API integration with mocked responses
- [ ] Test navigation flows
- [ ] Test error handling
- [ ] E2E tests with Cypress/Playwright

---

## 🚀 DEPLOYMENT TASKS

### Pre-Deployment
- [ ] Run all tests and ensure passing (90%+ unit, 80%+ integration)
- [ ] Code review completed and approved
- [ ] Update OpenAPI specification (`/docs/create-class/openapi.yaml`)
- [ ] Generate API documentation (Swagger UI)
- [ ] Performance testing completed
- [ ] Security audit completed (input validation, SQL injection prevention)
- [ ] Environment variables documented
  - [ ] `JWT_SECRET`
  - [ ] Database credentials
  - [ ] Any new configs

### Database Migration
- [ ] Write Flyway/Liquibase migration scripts
  - [ ] `V1__create_class_workflow_tables.sql`
  - [ ] Add new columns to existing tables
  - [ ] Create indexes
  - [ ] Create constraints
- [ ] Test migrations on local database
- [ ] Test migrations on staging database
- [ ] Prepare rollback scripts

### Staging Deployment
- [ ] Build backend: `mvn clean package`
- [ ] Deploy JAR to staging server
- [ ] Run database migrations on staging
- [ ] Deploy frontend build to staging CDN
- [ ] Update API endpoints in frontend config
- [ ] Run smoke tests on staging
- [ ] Test complete workflow on staging
- [ ] Load testing on staging

### Production Deployment
- [ ] Backup production database
- [ ] Schedule maintenance window (if needed)
- [ ] Deploy backend to production
- [ ] Run database migrations on production
- [ ] Deploy frontend to production CDN
- [ ] Verify API endpoints are correct
- [ ] Run smoke tests on production
- [ ] Monitor logs for errors
- [ ] Monitor performance metrics
- [ ] Notify stakeholders of completion

### Rollback Plan (if needed)
- [ ] Keep previous Docker image ready
- [ ] Keep database backup ready
- [ ] Revert to previous version command documented
- [ ] Test rollback procedure in staging

### Post-Deployment
- [ ] Monitor application logs for 24 hours
- [ ] Monitor error rates
- [ ] Monitor API response times
- [ ] Check database performance
- [ ] Gather user feedback
- [ ] Create release notes
- [ ] Update project documentation

---

## 📚 DOCUMENTATION TASKS

### Code Documentation
- [ ] Add JavaDoc comments to all public methods
- [ ] Add JavaDoc comments to all service classes
- [ ] Add JavaDoc comments to all DTOs
- [ ] Document complex algorithms (session generation, conflict detection)
- [ ] Document SQL queries with explanations
- [ ] Add inline comments for non-obvious logic

### API Documentation
- [ ] Ensure all endpoints are in OpenAPI spec
- [ ] Add request/response examples for each endpoint
- [ ] Document error codes and messages
- [ ] Document authentication requirements
- [ ] Document rate limits (if applicable)
- [ ] Update Swagger UI annotations in controllers

### Technical Documentation
- [ ] Update README.md with new features
- [ ] Document HYBRID approach rationale
- [ ] Document PRE-CHECK approach rationale
- [ ] Document PostgreSQL ISODOW usage
- [ ] Document 'general' skill behavior
- [ ] Create database schema diagram
- [ ] Create sequence diagrams for complex flows

### User Documentation
- [ ] Create user guide for Academic Staff
  - [ ] How to create a class
  - [ ] How to handle resource conflicts
  - [ ] How to assign substitute teachers
  - [ ] How to submit for approval
- [ ] Create user guide for Center Head
  - [ ] How to review submissions
  - [ ] How to approve/reject classes
  - [ ] What to check before approval

### Architecture Documentation
- [ ] Update architecture overview in CLAUDE.md
- [ ] Document new package structure
- [ ] Document new entities and relationships
- [ ] Document service layer design
- [ ] Document security configuration changes

### Testing Documentation
- [ ] Document test strategy
- [ ] Document how to run tests
- [ ] Document TestContainers setup
- [ ] Document test data creation utilities
- [ ] Create testing best practices guide

---

## 📊 PROGRESS TRACKING

### Overall Progress
- [ ] **Step 1: Create Class** (0/X tasks completed)
- [ ] **Step 2: Generate Sessions** (0/X tasks completed)
- [ ] **Step 3: Assign Time Slots** (0/X tasks completed)
- [ ] **Step 4: Assign Resources** (0/X tasks completed)
- [ ] **Step 5: Assign Teachers** (0/X tasks completed)
- [ ] **Step 6: Validate** (0/X tasks completed)
- [ ] **Step 7: Submit & Approve** (0/X tasks completed)
- [ ] **Testing** (0/X tasks completed)
- [ ] **Deployment** (0/X tasks completed)
- [ ] **Documentation** (0/X tasks completed)

### Milestones
- [ ] **M1:** Steps 1-2 complete with tests (Week 1)
- [ ] **M2:** Steps 3-4 complete with tests (Week 2)
- [ ] **M3:** Steps 5-6 complete with tests (Week 3)
- [ ] **M4:** Step 7 complete, E2E tested (Week 4)
- [ ] **M5:** Production deployment (Week 4)

---

## ✅ DEFINITION OF DONE

A task is considered "done" when:

1. **Code Complete**
   - [ ] Implementation matches specification
   - [ ] Code follows project coding standards
   - [ ] No code smells or obvious bugs
   - [ ] No hardcoded values (use constants/configs)

2. **Tested**
   - [ ] Unit tests written and passing
   - [ ] Integration tests written and passing
   - [ ] Coverage meets targets (90% unit, 80% integration)
   - [ ] Manual testing completed

3. **Reviewed**
   - [ ] Code review completed by peer
   - [ ] All review comments addressed
   - [ ] Approved by tech lead

4. **Documented**
   - [ ] JavaDoc added
   - [ ] API documentation updated
   - [ ] Complex logic explained in comments

5. **Integrated**
   - [ ] Merged to main/develop branch
   - [ ] No merge conflicts
   - [ ] CI/CD pipeline passing

---

## 📝 NOTES

### Important Reminders

1. **PostgreSQL ISODOW**: Day numbering is 1=Monday through 7=Sunday
2. **'general' Skill**: Acts as a universal skill that can teach any session
3. **HYBRID Approach**: Use SQL bulk operations first, then Java for conflict analysis
4. **PRE-CHECK v1.1**: Always check availability BEFORE showing teachers to user
5. **Dual Status**: Classes have both `status` (DRAFT/SCHEDULED) and `approval_status` (PENDING/APPROVED/REJECTED)
6. **Validation Before Submit**: Always call `validateClass()` before allowing submission

### Technical Debt / Future Improvements

- [ ] Implement full notification system (email/in-app)
- [ ] Add caching for frequently accessed data (courses, branches)
- [ ] Add audit log for all state changes
- [ ] Implement soft delete for classes
- [ ] Add bulk operations for multiple classes
- [ ] Add class cloning feature
- [ ] Add undo/redo functionality

### Known Limitations

- Resource assignment assumes no overlapping time slots on same date
- Teacher availability must be pre-configured (no on-the-fly updates)
- Rejection reason is free text (no predefined categories)
- No support for recurring maintenance windows yet

---

**Status:** 🚀 Ready to Start Implementation
**Last Updated:** January 4, 2025
**Version:** 1.1.0
