# Transfer Request Implementation Guide

**Version:** 1.0  
**Date:** 2025-11-07  
**Request Type:** TRANSFER  

---

## Overview

**Purpose:** Student chuyển lớp (schedule, branch, or modality change)  
**Complexity:** High  
**Flow Support:** Dual (Self-Service + On-Behalf with Consultation)  
**Business Impact:** Retention, student satisfaction  
**Key Policy:** **ONE transfer per student per course** (hard limit)

---

## 📱 Student UX Flow

### UX Principle
> **Critical Decision Support:** Transfer is a one-time opportunity. UI must clearly show consequences (content gap, tier requirements) and guide student to best decision.

### Flow Diagram - Tier 1 (Self-Service)
```
My Requests Page → [+ New Request] 
  → Modal: Choose Type (Transfer)
  → Step 1: Check Eligibility (show quota status)
  → Step 2: Choose Transfer Type (Tier 1 / Tier 2)
  → Step 3: Select Target Class (with content gap warnings)
  → Step 4: Set Effective Date & Fill Form
  → Submit → Success Message
```

### Flow Diagram - Tier 2 (Consultation)
```
Step 1-2: Same as Tier 1
  → Step 3: Consultation Form (preferences + reason)
  → Submit Consultation Request
  → Wait for Counselor Contact (24h)
  → [Counselor creates request on behalf]
  → Student receives confirmation request
  → Student confirms → Status: PENDING
  → AA reviews → APPROVED/REJECTED
```

---

## TIER 1: SELF-SERVICE FLOW

### 🖥️ Screen 1: Check Transfer Eligibility

**Purpose:** Show which classes are eligible for transfer and remaining quota

**UI Components:** `Card`, `Badge`, `Button`, `Alert`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Transfer Request                      [✕]     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Your Transfer Eligibility                               │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ✅ CHN-A1-01 • Chinese A1 - Morning Class       │   │
│ │    Central Branch • Offline                     │   │
│ │    Transfer Available: 1/1 remaining            │   │
│ │                                  [Start Transfer]│   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ❌ ENG-B2-03 • English B2 - Evening Class       │   │
│ │    Central Branch • Online                      │   │
│ │    Transfer Used: 1/1 (No transfers remaining)  │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ⚠️ IMPORTANT: You can only transfer ONCE per course   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. See all enrolled classes with transfer quota status
2. Classes with quota used (1/1) are disabled
3. Click [Start Transfer] on eligible class → Go to Step 2

**API Call Trigger:** When entering this screen (page load)

---

### 🖥️ Screen 2: Choose Transfer Type

**Purpose:** Determine if student wants Tier 1 (simple) or Tier 2 (complex) transfer

**UI Components:** `RadioGroup`, `Card`, `Alert`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Transfer Request                      [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 1 of 4                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ What would you like to change?                          │
│ Current: CHN-A1-01 (Central Branch • Offline)           │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ Schedule Only (Time/Days)                     │   │
│ │   ➔ Fast approval (4-8 hours)                   │   │
│ │   Keep same branch and learning mode            │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ Branch or Learning Mode                       │   │
│ │   ➔ Consultation required (2-3 days)            │   │
│ │   Change location or online/offline mode        │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ⚠️ Remember: Only ONE transfer per course allowed!    │
│                                                         │
│                                        [Cancel] [Next]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Choose between Tier 1 (schedule change) or Tier 2 (branch/modality change)
2. See approval time expectations
3. Click [Next] → If Tier 1: Go to Step 3A | If Tier 2: Go to Consultation Flow

**No API Call** - Just UI state selection

---

### 🖥️ Screen 3A: Select Target Class (Tier 1)

**Purpose:** Show available classes with same branch/modality, display content gap warnings

**UI Components:** `RadioGroup`, `Card`, `Badge`, `Alert`, `Collapsible`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Transfer Request (Schedule Change)   [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 2 of 4                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Available Classes (Same Branch & Mode)                  │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ CHN-A1-02 • Chinese A1 - Afternoon Class      │   │
│ │   Tue, Thu, Sat • 14:00-16:00                   │   │
│ │   Session 14/30 • 4 slots available             │   │
│ │   ⚠️ Content Gap: 2 sessions (minor)           │   │
│ │   [View Gap Details ▼]                          │   │
│ │                                                 │   │
│ │   ┌─────────────────────────────────────────┐  │   │
│ │   │ You will miss:                          │  │   │
│ │   │ • Session 13: Listening Practice        │  │   │
│ │   │ • Session 14: Speaking Practice         │  │   │
│ │   │                                         │  │   │
│ │   │ ⓘ Recommendation: Review materials or   │  │   │
│ │   │   request makeup sessions after transfer│  │   │
│ │   └─────────────────────────────────────────┘  │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ CHN-A1-03 • Chinese A1 - Evening Class        │   │
│ │   Mon, Wed, Fri • 18:00-20:00                   │   │
│ │   Session 10/30 • 6 slots available             │   │
│ │   ✅ No Content Gap                             │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ No suitable class? [Contact Academic Affairs]          │
│                                        [Cancel] [Next]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. See all eligible target classes (same branch + same modality)
2. Expand [View Gap Details] to see missed sessions
3. Content gap severity: 
   - ✅ None (0 sessions)
   - ⚠️ Minor (1-2 sessions)
   - ⚠️ Moderate (3-5 sessions)  
   - 🛑 Major (>5 sessions)
4. Select target class
5. Click [Next] → Go to Step 4

**API Call Trigger:** When entering this screen (after choosing Tier 1)

---

### 🖥️ Screen 4: Set Effective Date & Submit

**Purpose:** Confirm transfer details, set when transfer takes effect, and submit

**UI Components:** `DatePicker`, `Card`, `Textarea`, `Alert`, `Button`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Transfer Request                      [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 4 of 4                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Transfer Summary                                        │
│                                                         │
│ From:                                                   │
│ ┌─────────────────────────────────────────────────┐   │
│ │ CHN-A1-01 • Morning Class                       │   │
│ │ Mon, Wed, Fri • 08:00-10:00                     │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ To:                                                     │
│ ┌─────────────────────────────────────────────────┐   │
│ │ CHN-A1-03 • Evening Class                       │   │
│ │ Mon, Wed, Fri • 18:00-20:00                     │   │
│ │ 6 slots available                               │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ Effective Date *                                        │
│ [2025-11-15 ▼]  (Must be a class day: Mon/Wed/Fri)    │
│                                                         │
│ Reason for Transfer * (min 20 characters)               │
│ ┌─────────────────────────────────────────────────┐   │
│ │ I need to change to evening schedule due to     │   │
│ │ new work commitments starting next week.        │   │
│ └─────────────────────────────────────────────────┘   │
│ 62/20 characters                                        │
│                                                         │
│ ⚠️ This is your ONLY transfer for this course!        │
│                                                         │
│                                    [Cancel] [Submit]    │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Review FROM and TO class details
2. Select effective date (must be a valid class session date)
3. Enter reason (minimum 20 characters)
4. See final warning about one-time transfer
5. Click [Submit] → API call to create transfer request

**Client-Side Validation:**
- Effective date must be:
  - Future date (>= today)
  - A valid session date in target class (check schedule days)
- Reason must be ≥ 20 characters
- Show character counter

**API Call Trigger:** When student clicks [Submit]

---

### 🖥️ Screen 5: Success State (Tier 1)

**Purpose:** Confirm submission with next steps

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│                         ✓                               │
│                                                         │
│          Transfer Request Submitted Successfully        │
│                                                         │
│     Your transfer request has been sent to              │
│     Academic Affairs for review.                        │
│                                                         │
│     Request ID: #044                                    │
│     Status: Pending                                     │
│                                                         │
│     Transfer Details:                                   │
│     • From: CHN-A1-01 (Morning Class)                   │
│     • To: CHN-A1-03 (Evening Class)                     │
│     • Effective: Nov 15, 2025                           │
│                                                         │
│     Expected approval time: 4-8 hours                   │
│                                                         │
│     ⓘ You'll receive email notification once approved   │
│                                                         │
│                            [View My Requests]           │
└─────────────────────────────────────────────────────────┘
```

---

## TIER 2: CONSULTATION FLOW

### 🖥️ Screen 3B: Consultation Request Form

**Purpose:** Collect student preferences for branch/modality change

**UI Components:** `Checkbox`, `RadioGroup`, `Textarea`, `Button`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Transfer Request - Consultation      [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 2 of 2                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Tell us about your transfer needs                       │
│                                                         │
│ You want to change:                                     │
│ ☑ Branch/Location                                       │
│ ☐ Learning Mode (Online ↔ Offline)                     │
│                                                         │
│ Preferred new branch (if changing location):            │
│ ( ) North Branch                                        │
│ ( ) South Branch                                        │
│ ( ) Any branch is fine                                  │
│                                                         │
│ Preferred modality (if changing mode):                  │
│ ( ) Online                                              │
│ ( ) Offline                                             │
│ ( ) Either is fine                                      │
│                                                         │
│ Why do you need this change? * (min 20 characters)      │
│ ┌─────────────────────────────────────────────────┐   │
│ │ I am relocating to the North area next month    │   │
│ │ and would like to continue my studies at the    │   │
│ │ North Branch.                                   │   │
│ └─────────────────────────────────────────────────┘   │
│ 87/20 characters                                        │
│                                                         │
│ ⚠️ A counselor will contact you within 24 hours to    │
│    discuss your options.                                │
│                                                         │
│                      [Cancel] [Submit Consultation]     │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Check what they want to change (branch and/or modality)
2. Select preferences
3. Enter detailed reason (min 20 chars)
4. Click [Submit Consultation] → API call

**API Call Trigger:** When student clicks [Submit Consultation]

---

### 🖥️ Screen 4: Consultation Submitted

**Purpose:** Set expectations for counselor contact

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│                         ✓                               │
│                                                         │
│         Consultation Request Submitted                  │
│                                                         │
│     A counselor will contact you within 24 hours        │
│     to discuss your transfer options.                   │
│                                                         │
│     Consultation ID: #501                               │
│     Status: Waiting for Counselor                       │
│                                                         │
│     What to expect:                                     │
│     1. Counselor will call/email you                    │
│     2. Discuss available classes and options            │
│     3. Counselor will create transfer request           │
│     4. You'll need to confirm the request               │
│     5. Academic Affairs will review and approve         │
│                                                         │
│     Please keep your phone and email accessible.        │
│                                                         │
│                            [View My Requests]           │
└─────────────────────────────────────────────────────────┘
```

---

### 🖥️ Screen 5: Confirm Counselor-Created Request

**Purpose:** Student reviews and confirms request created by counselor

**UI Components:** `Card`, `Alert`, `Button`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ Transfer Request - Awaiting Your Confirmation   [✕]    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Your counselor has prepared a transfer request          │
│                                                         │
│ Created by: Counselor Tran                              │
│ Date: Nov 8, 2025 10:30                                 │
│                                                         │
│ From:                                                   │
│ ┌─────────────────────────────────────────────────┐   │
│ │ CHN-A1-01 • Chinese A1 - Morning Class          │   │
│ │ Central Branch • Offline                        │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ To:                                                     │
│ ┌─────────────────────────────────────────────────┐   │
│ │ CHN-A1-NORTH-01 • Chinese A1 - North Branch     │   │
│ │ North Branch • Offline                          │   │
│ │ Mon, Wed, Fri • 08:00-10:00                     │   │
│ │ 5 slots available                               │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ Effective Date: Nov 20, 2025                            │
│                                                         │
│ Counselor Notes:                                        │
│ "Discussed with student via phone on Nov 8.             │
│  Student confirmed preference for offline class at      │
│  North Branch. No content gap issues."                  │
│                                                         │
│ ⏰ Confirmation Deadline: Nov 10, 2025 10:30           │
│                                                         │
│ ⚠️ This is your ONLY transfer for this course!        │
│                                                         │
│                    [Reject] [Confirm Transfer Request]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Review counselor-created transfer request details
2. See counselor notes explaining the recommendation
3. Note confirmation deadline (48 hours)
4. Click [Confirm] → API call to confirm → Status changes to PENDING
5. OR click [Reject] → Consultation reopened

**API Call Trigger:** When student clicks [Confirm Transfer Request]

---

## Transfer Tiers

### Tier 1: Simple Transfer (Self-Service)
- **Change:** Schedule only (time/days)
- **Keep:** Same branch + same modality
- **Approval Time:** 4-8 hours
- **Process:** Direct student submission → AA review → Auto-execute

### Tier 2: Complex Transfer (Consultation Required)
- **Change:** Branch OR modality (Online ↔ Offline)
- **Approval Time:** 2-3 days
- **Process:** Consultation request → Counselor discusses → Creates request → Student confirms → AA review → Auto-execute

---

## Business Rules

### Tier 1: Simple Transfer (Self-Service)
- **Change:** Schedule only (time/days)
- **Keep:** Same branch + same modality
- **Approval Time:** 4-8 hours
- **Process:** Direct student submission → AA review → Auto-execute

### Tier 2: Complex Transfer (Consultation Required)
- **Change:** Branch OR modality (Online ↔ Offline)
- **Approval Time:** 2-3 days
- **Process:** Consultation request → Counselor discusses → Creates request → Student confirms → AA review → Auto-execute

---

## Business Rules

| Rule ID | Description | Enforcement |
|---------|-------------|-------------|
| BR-TRF-001 | **ONE transfer per student per course** | Blocking (Database) |
| BR-TRF-002 | Both classes must have same `course_id` | Blocking |
| BR-TRF-003 | Target class must have capacity | Blocking |
| BR-TRF-004 | Target class `status IN ('ONGOING', 'PLANNED')` | Blocking |
| BR-TRF-005 | Effective date must be >= CURRENT_DATE | Blocking |
| BR-TRF-006 | Effective date must be a class session date | Blocking |
| BR-TRF-007 | No concurrent transfer requests | Blocking |
| BR-TRF-008 | Content gap detection and warning | Warning only |
| BR-TRF-009 | Tier 2: Student confirmation within 48h | Blocking |
| BR-TRF-010 | Transfer reason min 20 characters | Blocking |
| BR-TRF-011 | Preserve audit trail (status updates only) | Data Integrity |

**Configuration:**
```yaml
transfer_request:
  transfers_per_course: 1
  tier2_confirmation_hours: 48
  tier2_counselor_response_hours: 24
  max_content_gap_sessions: 3
  min_notice_days: 3
  reason_min_length: 20
  allow_cross_branch: true
  allow_cross_modality: true
```

---

## API Endpoints

### 1. Check Transfer Eligibility

**When to Call:** When entering Screen 1 (Check Eligibility screen)

**Purpose:** Show which classes student can transfer from and remaining quota

**Request:**
```http
GET /api/v1/students/me/transfer-eligibility
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "currentEnrollments": [
      {
        "enrollmentId": 1001,
        "classId": 101,
        "classCode": "CHN-A1-01",
        "className": "Chinese A1 - Morning Class",
        "courseId": 10,
        "courseName": "Chinese A1",
        "branchId": 1,
        "branchName": "Central Branch",
        "modality": "OFFLINE",
        "enrollmentStatus": "ENROLLED",
        "transferQuota": {
          "used": 0,
          "limit": 1,
          "remaining": 1
        },
        "hasPendingTransfer": false,
        "canTransfer": true
      }
    ]
  }
}
```

**Frontend Usage:**
```typescript
// Screen 1: Check eligibility on mount
useEffect(() => {
  const checkEligibility = async () => {
    const response = await fetch(
      '/api/v1/students/me/transfer-eligibility',
      { headers: { Authorization: `Bearer ${token}` } }
    );
    const data = await response.json();
    
    // Separate eligible and ineligible classes
    const eligible = data.data.currentEnrollments.filter(e => e.canTransfer);
    const ineligible = data.data.currentEnrollments.filter(e => !e.canTransfer);
    
    setEligibleClasses(eligible);
    setIneligibleClasses(ineligible);
  };
  
  checkEligibility();
}, []);
```

**Important Notes:**
- `canTransfer = false` when `transferQuota.remaining = 0`
- Show clear message: "Transfer Used: 1/1 (No transfers remaining)"
- Disable [Start Transfer] button for ineligible classes

---

### 2. Get Tier 1 Transfer Options

**When to Call:** When entering Screen 3A (after student chooses Tier 1)

**Purpose:** Find available target classes with same branch/modality, calculate content gap

**Request:**
```http
GET /api/v1/student-requests/transfer-options/tier1?currentClassId=101
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01",
      "name": "Chinese A1 - Morning Class",
      "courseId": 10,
      "branchId": 1,
      "branchName": "Central Branch",
      "modality": "OFFLINE",
      "scheduleDays": [1, 3, 5],
      "currentSessionProgress": {
        "completedSessions": 12,
        "totalSessions": 30
      }
    },
    "transferCriteria": {
      "sameBranch": true,
      "sameModality": true,
      "sameCourse": true,
      "differentSchedule": true
    },
    "availableClasses": [
      {
        "classId": 102,
        "classCode": "CHN-A1-02",
        "className": "Chinese A1 - Afternoon Class",
        "branchId": 1,
        "branchName": "Central Branch",
        "modality": "OFFLINE",
        "scheduleDays": [2, 4, 6],
        "startDate": "2025-10-01",
        "plannedEndDate": "2025-12-20",
        "currentSession": 14,
        "maxCapacity": 20,
        "enrolledCount": 16,
        "availableSlots": 4,
        "classStatus": "ONGOING",
        "contentGap": {
          "missedSessions": 2,
          "gapSessions": [
            {
              "courseSessionNumber": 13,
              "courseSessionTitle": "Listening Practice"
            },
            {
              "courseSessionNumber": 14,
              "courseSessionTitle": "Speaking Practice"
            }
          ],
          "severity": "MINOR"
        }
      }
    ]
  }
}
```

**Frontend Usage:**
```typescript
// Screen 3A: Load transfer options when entering screen
const handleChooseTier1 = async () => {
  const response = await fetch(
    `/api/v1/student-requests/transfer-options/tier1?currentClassId=${selectedClass.id}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const data = await response.json();
  
  // Sort by content gap severity (None → Minor → Moderate → Major)
  const sorted = data.data.availableClasses.sort((a, b) => {
    const severityOrder = { 'NONE': 0, 'MINOR': 1, 'MODERATE': 2, 'MAJOR': 3 };
    return severityOrder[a.contentGap.severity] - severityOrder[b.contentGap.severity];
  });
  
  setAvailableClasses(sorted);
  goToStep(3);
};
```

**Content Gap Severity Mapping:**
```typescript
const getSeverityBadge = (severity: string) => {
  switch (severity) {
    case 'NONE': return <Badge variant="success">✅ No Content Gap</Badge>;
    case 'MINOR': return <Badge variant="warning">⚠️ Content Gap: {count} sessions (minor)</Badge>;
    case 'MODERATE': return <Badge variant="warning">⚠️ Content Gap: {count} sessions (moderate)</Badge>;
    case 'MAJOR': return <Badge variant="destructive">🛑 Content Gap: {count} sessions (major)</Badge>;
  }
};
```

---

### 3. Submit Transfer Request (Tier 1)

**When to Call:** When student clicks [Submit] in Screen 4

**Purpose:** Create transfer request with effective date and reason

**Request:**
```http
POST /api/v1/student-requests
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "requestType": "TRANSFER",
  "currentClassId": 101,
  "targetClassId": 103,
  "effectiveDate": "2025-11-15",
  "requestReason": "I need to change to evening schedule due to new work commitments.",
  "note": "",
  "transferTier": "TIER1"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transfer request submitted successfully",
  "data": {
    "id": 44,
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe"
    },
    "requestType": "TRANSFER",
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01",
      "name": "Chinese A1 - Morning Class"
    },
    "targetClass": {
      "id": 103,
      "code": "CHN-A1-03",
      "name": "Chinese A1 - Evening Class"
    },
    "effectiveDate": "2025-11-15",
    "effectiveSession": {
      "sessionId": 3010,
      "courseSessionNumber": 13,
      "courseSessionTitle": "Writing Practice"
    },
    "requestReason": "I need to change to evening schedule due to new work commitments.",
    "status": "PENDING",
    "transferTier": "TIER1",
    "submittedAt": "2025-11-07T18:30:00+07:00"
  }
}
```

**Frontend Usage:**
```typescript
// Screen 4: When student clicks Submit
const handleSubmit = async (formData: TransferFormData) => {
  try {
    const response = await fetch('/api/v1/student-requests', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        requestType: 'TRANSFER',
        currentClassId: currentClass.id,
        targetClassId: selectedTargetClass.id,
        effectiveDate: formData.effectiveDate,
        requestReason: formData.reason,
        note: formData.note,
        transferTier: 'TIER1'
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      // Show success screen
      showSuccessDialog({
        requestId: result.data.id,
        effectiveSession: result.data.effectiveSession,
        expectedApprovalTime: '4-8 hours'
      });
    }
  } catch (error) {
    // Handle specific errors
    if (error.message.includes('quota exceeded')) {
      showError('You have already used your one transfer for this course.');
    } else if (error.message.includes('full')) {
      showError('Target class is now full. Please select another class.');
    } else {
      showError(error.message);
    }
  }
};
```

**Effective Date Validation:**
```typescript
// Client-side: Check if selected date is a valid session date
const validateEffectiveDate = (date: Date, targetClass: Class) => {
  const dayOfWeek = date.getDay(); // 0=Sunday, 1=Monday, ...
  
  // Check if date matches target class schedule days
  // e.g., targetClass.scheduleDays = [1, 3, 5] (Mon, Wed, Fri)
  if (!targetClass.scheduleDays.includes(dayOfWeek)) {
    return {
      valid: false,
      message: `Selected date is not a class day. Class meets on ${getScheduleDayNames(targetClass.scheduleDays)}`
    };
  }
  
  if (date < new Date()) {
    return { valid: false, message: 'Effective date must be in the future' };
  }
  
  return { valid: true };
};
```

---

### 4. Submit Consultation Request (Tier 2)

**When to Call:** When student clicks [Submit Consultation] in Screen 3B

**Purpose:** Create consultation request for counselor to handle

**Request:**
```http
POST /api/v1/student-requests/consultation
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "currentClassId": 101,
  "transferType": "TIER2",
  "changeRequirements": {
    "changeBranch": true,
    "changeModality": false,
    "preferredBranchId": 2,
    "preferredModality": null
  },
  "reason": "I am relocating to the North area next month."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Consultation request submitted. A counselor will contact you within 24 hours.",
  "data": {
    "consultationRequestId": 501,
    "studentId": 123,
    "currentClassId": 101,
    "status": "CONSULTATION_PENDING",
    "submittedAt": "2025-11-07T18:45:00+07:00",
    "expectedContactBy": "2025-11-08T18:45:00+07:00"
  }
}
```

**Frontend Usage:**
```typescript
// Screen 3B: When student submits consultation
const handleSubmitConsultation = async (formData: ConsultationFormData) => {
  try {
    const response = await fetch('/api/v1/student-requests/consultation', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        currentClassId: currentClass.id,
        transferType: 'TIER2',
        changeRequirements: {
          changeBranch: formData.changeBranch,
          changeModality: formData.changeModality,
          preferredBranchId: formData.preferredBranchId,
          preferredModality: formData.preferredModality
        },
        reason: formData.reason
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      // Show consultation submitted screen
      showConsultationSuccess({
        consultationId: result.data.consultationRequestId,
        expectedContactBy: result.data.expectedContactBy
      });
    }
  } catch (error) {
    showError(error.message);
  }
};
```

---

### 5. Get Pending Confirmation Requests

**When to Call:** Periodically (or via notification link) to check if counselor created a request

**Purpose:** Show requests created by counselor that need student confirmation

**Request:**
```http
GET /api/v1/students/me/requests/pending-confirmation
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "requests": [
      {
        "id": 45,
        "requestType": "TRANSFER",
        "status": "WAITING_CONFIRM",
        "currentClass": {
          "id": 101,
          "code": "CHN-A1-01",
          "name": "Chinese A1 - Morning Class"
        },
        "targetClass": {
          "id": 301,
          "code": "CHN-A1-NORTH-01",
          "name": "Chinese A1 - North Branch Morning",
          "branchName": "North Branch",
          "modality": "OFFLINE",
          "availableSlots": 5
        },
        "effectiveDate": "2025-11-20",
        "consultationNotes": "Discussed with student via phone...",
        "submittedBy": {
          "id": 890,
          "fullName": "Counselor Tran"
        },
        "submittedAt": "2025-11-08T10:30:00+07:00",
        "confirmationDeadline": "2025-11-10T10:30:00+07:00"
      }
    ]
  }
}
```

**Frontend Usage:**
```typescript
// Check for pending confirmations (e.g., on My Requests page load)
useEffect(() => {
  const checkPendingConfirmations = async () => {
    const response = await fetch(
      '/api/v1/students/me/requests/pending-confirmation',
      { headers: { Authorization: `Bearer ${token}` } }
    );
    const data = await response.json();
    
    if (data.data.requests.length > 0) {
      // Show notification badge or banner
      showConfirmationAlert(data.data.requests[0]);
    }
  };
  
  checkPendingConfirmations();
}, []);
```

---

### 6. Confirm Transfer Request (Tier 2)

**When to Call:** When student clicks [Confirm Transfer Request] in Screen 5

**Purpose:** Student confirms counselor-created request, changing status from WAITING_CONFIRM → PENDING
```http
POST /api/v1/student-requests/on-behalf
Authorization: Bearer {counselor_access_token}
Content-Type: application/json

{
  "studentId": 123,
  "requestType": "TRANSFER",
  "currentClassId": 101,
  "targetClassId": 301,
  "effectiveDate": "2025-11-20",
  "requestReason": "Student relocating to North area. CHN-A1-NORTH-01 matches schedule.",
  "transferTier": "TIER2",
  "consultationNotes": "Discussed via phone. Student confirmed preference for offline at North Branch."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transfer request created. Awaiting student confirmation.",
  "data": {
    "id": 45,
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe"
    },
    "requestType": "TRANSFER",
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01"
    },
    "targetClass": {
      "id": 301,
      "code": "CHN-A1-NORTH-01",
      "branchName": "North Branch",
      "modality": "OFFLINE"
    },
    "effectiveDate": "2025-11-20",
    "status": "WAITING_CONFIRM",
    "transferTier": "TIER2",
    "submittedAt": "2025-11-08T10:30:00+07:00",
    "submittedBy": {
      "id": 890,
      "fullName": "Counselor Tran"
    },
    "confirmationDeadline": "2025-11-10T10:30:00+07:00"
  }
}
```

**Frontend Usage:**
```typescript
// Screen 5: When student confirms
const handleConfirmRequest = async (requestId: number) => {
  try {
    const response = await fetch(
      `/api/v1/student-requests/${requestId}/confirm`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ confirmed: true })
      }
    );
    
    const result = await response.json();
    
    if (result.success) {
      showToast('Transfer request confirmed! Academic Affairs will review shortly.');
      navigateToMyRequests();
    }
  } catch (error) {
    showError(error.message);
  }
};

// When student rejects
const handleRejectRequest = async (requestId: number) => {
  const confirmed = await showConfirmDialog(
    'Are you sure you want to reject this transfer request? You can discuss alternatives with your counselor.'
  );
  
  if (confirmed) {
    await fetch(`/api/v1/student-requests/${requestId}/confirm`, {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ confirmed: false })
    });
    
    showToast('Request rejected. Your counselor will contact you again.');
    navigateToMyRequests();
  }
};
```

---

### 7. Approve Transfer Request (Academic Affairs Only)

**Request:**
```http
PUT /api/v1/student-requests/{id}/confirm
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "confirmed": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transfer confirmed. Forwarded to Academic Affairs for final review.",
  "data": {
    "id": 45,
    "status": "PENDING",
    "confirmedAt": "2025-11-08T14:00:00+07:00"
  }
}
```

### 7. Approve Transfer Request

**Request:**
```http
PUT /api/v1/student-requests/{id}/approve
Authorization: Bearer {aa_access_token}
Content-Type: application/json

{
  "note": "Approved. Valid reason and no content gap issues."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transfer approved and executed successfully",
  "data": {
    "request": {
      "id": 44,
      "status": "APPROVED",
      "decidedAt": "2025-11-07T19:00:00+07:00",
      "decidedBy": {
        "id": 789,
        "fullName": "AA Staff Nguyen"
      }
    },
    "enrollmentChanges": {
      "oldEnrollment": {
        "id": 1001,
        "classId": 101,
        "status": "TRANSFERRED",
        "leftAt": "2025-11-07T19:00:00+07:00",
        "leftSessionId": 1012
      },
      "newEnrollment": {
        "id": 1050,
        "classId": 103,
        "status": "ENROLLED",
        "enrolledAt": "2025-11-07T19:00:00+07:00",
        "joinSessionId": 3010
      }
    },
    "sessionTransfers": {
      "transferredCount": 18,
      "futureSessionsInNewClass": 18
    }
  }
}
```

---

## Database Schema

### Table: `student_request`

```sql
CREATE TABLE student_request (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    request_type request_type_enum NOT NULL, -- 'TRANSFER'
    current_class_id BIGINT NOT NULL REFERENCES class(id),
    target_class_id BIGINT REFERENCES class(id),
    effective_date DATE,
    effective_session_id BIGINT REFERENCES session(id),
    transfer_tier transfer_tier_enum, -- 'TIER1', 'TIER2'
    request_reason TEXT NOT NULL,
    note TEXT,
    consultation_notes TEXT,
    status request_status_enum NOT NULL DEFAULT 'pending',
    confirmation_deadline TIMESTAMP,
    confirmed_at TIMESTAMP,
    submitted_by BIGINT NOT NULL REFERENCES user(id),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    decided_by BIGINT REFERENCES user(id),
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_transfer_valid CHECK (
        request_type != 'TRANSFER' OR (
            target_class_id IS NOT NULL AND
            effective_date IS NOT NULL AND
            target_session_id IS NULL AND
            makeup_session_id IS NULL
        )
    )
);

CREATE INDEX idx_student_request_transfer ON student_request(student_id, request_type, status);
CREATE INDEX idx_student_request_confirmation ON student_request(status, confirmation_deadline);
```

### Table: `enrollment` (Enhanced)

```sql
CREATE TABLE enrollment (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    class_id BIGINT NOT NULL REFERENCES class(id),
    course_id BIGINT NOT NULL REFERENCES course(id),
    status enrollment_status_enum NOT NULL DEFAULT 'enrolled',
    enrolled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    left_at TIMESTAMP,
    left_session_id BIGINT REFERENCES session(id),
    join_session_id BIGINT REFERENCES session(id),
    transfer_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_transfer_limit CHECK (transfer_count <= 1)
);

CREATE INDEX idx_enrollment_transfer ON enrollment(student_id, course_id, transfer_count);
```

### Table: `consultation_request` (Tier 2)

```sql
CREATE TABLE consultation_request (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    current_class_id BIGINT NOT NULL REFERENCES class(id),
    change_branch BOOLEAN NOT NULL DEFAULT FALSE,
    change_modality BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_branch_id BIGINT REFERENCES branch(id),
    preferred_modality modality_enum,
    reason TEXT NOT NULL,
    status consultation_status_enum NOT NULL DEFAULT 'pending',
    assigned_counselor_id BIGINT REFERENCES user(id),
    contacted_at TIMESTAMP,
    resolved_at TIMESTAMP,
    student_request_id BIGINT REFERENCES student_request(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_status ON consultation_request(status, created_at);
```

---

## Content Gap Analysis Algorithm

```java
public ContentGapDTO analyzeContentGap(Long currentClassId, Long targetClassId) {
    Class currentClass = classRepository.findById(currentClassId)
        .orElseThrow(() -> new ResourceNotFoundException("Current class not found"));
    
    Class targetClass = classRepository.findById(targetClassId)
        .orElseThrow(() -> new ResourceNotFoundException("Target class not found"));
    
    // Get completed sessions in current class
    List<Session> completedSessions = sessionRepository
        .findByClassIdAndStatusIn(currentClassId, 
            List.of(SessionStatus.COMPLETED, SessionStatus.CANCELLED));
    
    Set<Long> completedCourseSessionIds = completedSessions.stream()
        .map(s -> s.getCourseSession().getId())
        .collect(Collectors.toSet());
    
    // Get target class's past sessions
    List<Session> targetPastSessions = sessionRepository
        .findByClassIdAndDateBefore(targetClassId, LocalDate.now());
    
    // Find gap: sessions target class covered but current class hasn't
    List<Session> gapSessions = targetPastSessions.stream()
        .filter(s -> !completedCourseSessionIds.contains(s.getCourseSession().getId()))
        .collect(Collectors.toList());
    
    // Calculate severity
    String severity = "NONE";
    if (gapSessions.size() > 0 && gapSessions.size() <= 2) {
        severity = "MINOR";
    } else if (gapSessions.size() >= 3 && gapSessions.size() <= 5) {
        severity = "MODERATE";
    } else if (gapSessions.size() > 5) {
        severity = "MAJOR";
    }
    
    return ContentGapDTO.builder()
        .missedSessions(gapSessions.size())
        .gapSessions(gapSessions.stream()
            .map(s -> new GapSessionDTO(
                s.getCourseSession().getCourseSessionNumber(),
                s.getCourseSession().getCourseSessionTitle()))
            .collect(Collectors.toList()))
        .severity(severity)
        .recommendation(generateRecommendation(severity, gapSessions))
        .build();
}

private String generateRecommendation(String severity, List<Session> gapSessions) {
    switch (severity) {
        case "NONE":
            return "No content gap. You can transfer seamlessly.";
        case "MINOR":
            return String.format("You will miss %d session(s). Review materials or request makeup.", 
                gapSessions.size());
        case "MODERATE":
            return String.format("You will miss %d sessions. Makeup sessions recommended.", 
                gapSessions.size());
        case "MAJOR":
            return String.format("Large content gap (%d sessions). Consider retaking course.", 
                gapSessions.size());
        default:
            return "";
    }
}
```

---

## Backend Transaction Logic

### Validation (Submit Tier 1)

```java
public StudentRequestResponseDTO submitTransferRequest(TransferRequestDTO dto) {
    // 1. Check transfer quota
    Enrollment currentEnrollment = enrollmentRepository
        .findByStudentIdAndClassIdAndStatus(
            getCurrentUserId(), dto.getCurrentClassId(), EnrollmentStatus.ENROLLED)
        .orElseThrow(() -> new BusinessRuleException("Not enrolled in current class"));
    
    if (currentEnrollment.getTransferCount() >= 1) {
        throw new BusinessRuleException("Transfer quota exceeded. Maximum 1 transfer per course.");
    }
    
    // 2. Check concurrent transfer requests
    boolean hasPendingTransfer = studentRequestRepository.existsByStudentIdAndRequestTypeAndStatusIn(
        getCurrentUserId(), RequestType.TRANSFER, 
        List.of(RequestStatus.PENDING, RequestStatus.WAITING_CONFIRM));
    
    if (hasPendingTransfer) {
        throw new BusinessRuleException("You already have a pending transfer request");
    }
    
    // 3. Validate target class
    Class targetClass = classRepository.findById(dto.getTargetClassId())
        .orElseThrow(() -> new ResourceNotFoundException("Target class not found"));
    
    if (!targetClass.getCourse().getId().equals(currentEnrollment.getCourse().getId())) {
        throw new BusinessRuleException("Target class must be for the same course");
    }
    
    if (!List.of(ClassStatus.ONGOING, ClassStatus.PLANNED).contains(targetClass.getStatus())) {
        throw new BusinessRuleException("Target class must be ONGOING or PLANNED");
    }
    
    // 4. Check capacity
    int enrolledCount = enrollmentRepository.countByClassIdAndStatus(
        dto.getTargetClassId(), EnrollmentStatus.ENROLLED);
    
    if (enrolledCount >= targetClass.getMaxCapacity()) {
        throw new BusinessRuleException("Target class is full");
    }
    
    // 5. Validate effective date
    if (dto.getEffectiveDate().isBefore(LocalDate.now())) {
        throw new BusinessRuleException("Effective date must be in the future");
    }
    
    Session effectiveSession = sessionRepository
        .findByClassIdAndDate(dto.getTargetClassId(), dto.getEffectiveDate())
        .orElseThrow(() -> new BusinessRuleException("No session on effective date"));
    
    // 6. Tier validation
    TransferTier tier = determineTransferTier(currentEnrollment.getClassEntity(), targetClass);
    if (tier == TransferTier.TIER2 && dto.getTransferTier() == TransferTier.TIER1) {
        throw new BusinessRuleException("This transfer requires Tier 2 (consultation)");
    }
    
    // 7. Content gap analysis
    ContentGapDTO contentGap = analyzeContentGap(dto.getCurrentClassId(), dto.getTargetClassId());
    if (contentGap.getSeverity().equals("MAJOR")) {
        // Log warning but don't block
        log.warn("Major content gap detected for transfer request: {} sessions", 
            contentGap.getMissedSessions());
    }
    
    // 8. Create request
    StudentRequest request = StudentRequest.builder()
        .student(studentRepository.getReferenceById(getCurrentUserId()))
        .requestType(RequestType.TRANSFER)
        .currentClass(classRepository.getReferenceById(dto.getCurrentClassId()))
        .targetClass(targetClass)
        .effectiveDate(dto.getEffectiveDate())
        .effectiveSession(effectiveSession)
        .transferTier(tier)
        .requestReason(dto.getRequestReason())
        .note(dto.getNote())
        .status(RequestStatus.PENDING)
        .submittedBy(userRepository.getReferenceById(getCurrentUserId()))
        .submittedAt(LocalDateTime.now())
        .build();
    
    request = studentRequestRepository.save(request);
    
    // 9. Send notification
    notificationService.notifyAcademicAffair(request);
    
    return mapper.toResponseDTO(request);
}

private TransferTier determineTransferTier(Class currentClass, Class targetClass) {
    boolean sameBranch = currentClass.getBranch().getId().equals(targetClass.getBranch().getId());
    boolean sameModality = currentClass.getModality().equals(targetClass.getModality());
    
    if (sameBranch && sameModality) {
        return TransferTier.TIER1;
    } else {
        return TransferTier.TIER2;
    }
}
```

### Approval Transaction (Auto-Execute)

```java
@Transactional
public StudentRequestResponseDTO approveTransferRequest(Long requestId, ApprovalDTO dto) {
    // 1. Load request
    StudentRequest request = studentRequestRepository.findById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    
    if (!request.getRequestType().equals(RequestType.TRANSFER)) {
        throw new BusinessRuleException("Not a transfer request");
    }
    
    if (!request.getStatus().equals(RequestStatus.PENDING)) {
        throw new BusinessRuleException("Request not in PENDING status");
    }
    
    // 2. Re-validate capacity
    int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
        request.getTargetClass().getId(), EnrollmentStatus.ENROLLED);
    
    if (currentEnrolled >= request.getTargetClass().getMaxCapacity()) {
        throw new BusinessRuleException("Target class became full");
    }
    
    // 3. Update request status
    request.setStatus(RequestStatus.APPROVED);
    request.setDecidedBy(userRepository.getReferenceById(getCurrentUserId()));
    request.setDecidedAt(LocalDateTime.now());
    request.setNote(dto.getNote());
    request = studentRequestRepository.save(request);
    
    // 4. Execute Transfer
    executeTransfer(request);
    
    return mapper.toResponseDTO(request);
}

@Transactional
private void executeTransfer(StudentRequest request) {
    Long studentId = request.getStudent().getId();
    Long currentClassId = request.getCurrentClass().getId();
    Long targetClassId = request.getTargetClass().getId();
    LocalDate effectiveDate = request.getEffectiveDate();
    
    // 1. Update old enrollment
    Enrollment oldEnrollment = enrollmentRepository
        .findByStudentIdAndClassIdAndStatus(studentId, currentClassId, EnrollmentStatus.ENROLLED)
        .orElseThrow(() -> new ResourceNotFoundException("Old enrollment not found"));
    
    Session lastSession = sessionRepository
        .findByClassIdAndDateBefore(currentClassId, effectiveDate)
        .stream()
        .max(Comparator.comparing(Session::getDate))
        .orElse(null);
    
    oldEnrollment.setStatus(EnrollmentStatus.TRANSFERRED);
    oldEnrollment.setLeftAt(LocalDateTime.now());
    oldEnrollment.setLeftSessionId(lastSession != null ? lastSession.getId() : null);
    enrollmentRepository.save(oldEnrollment);
    
    // 2. Create new enrollment
    Enrollment newEnrollment = Enrollment.builder()
        .student(request.getStudent())
        .classEntity(request.getTargetClass())
        .course(request.getTargetClass().getCourse())
        .status(EnrollmentStatus.ENROLLED)
        .enrolledAt(LocalDateTime.now())
        .joinSessionId(request.getEffectiveSession().getId())
        .transferCount(oldEnrollment.getTransferCount() + 1)
        .build();
    
    newEnrollment = enrollmentRepository.save(newEnrollment);
    
    // 3. Update old future student_sessions
    List<StudentSession> oldFutureSessions = studentSessionRepository
        .findByStudentIdAndClassIdAndDateAfter(studentId, currentClassId, effectiveDate);
    
    for (StudentSession oldSession : oldFutureSessions) {
        oldSession.setAttendanceStatus(AttendanceStatus.ABSENT);
        oldSession.setNote("Transferred to " + request.getTargetClass().getCode() + 
            " on " + effectiveDate);
    }
    studentSessionRepository.saveAll(oldFutureSessions);
    
    // 4. Create new student_sessions for target class
    List<Session> newFutureSessions = sessionRepository
        .findByClassIdAndDateAfter(targetClassId, effectiveDate.minusDays(1));
    
    List<StudentSession> newStudentSessions = newFutureSessions.stream()
        .map(session -> StudentSession.builder()
            .student(request.getStudent())
            .session(session)
            .attendanceStatus(AttendanceStatus.PLANNED)
            .isMakeup(false)
            .note("Joined via transfer from " + request.getCurrentClass().getCode())
            .build())
        .collect(Collectors.toList());
    
    studentSessionRepository.saveAll(newStudentSessions);
    
    // 5. Send notifications
    notificationService.notifyStudent(request, "approved");
    notificationService.notifyTeacher(request.getCurrentClass(), "student_left", request.getStudent());
    notificationService.notifyTeacher(request.getTargetClass(), "student_joined", request.getStudent());
}
```

---

## Status State Machine

### Tier 1 Flow
```
[Student submits] → PENDING → [AA approves] → APPROVED → [Auto-execute transfer]
                                             → REJECTED
```

### Tier 2 Flow
```
[Student requests consultation] → CONSULTATION_PENDING → [Counselor creates request] 
→ WAITING_CONFIRM → [Student confirms] → PENDING → [AA approves] → APPROVED 
→ [Auto-execute transfer]
```

**Key States:**
- `CONSULTATION_PENDING`: Waiting for counselor to create transfer request
- `WAITING_CONFIRM`: Student must confirm counselor-created request (48h deadline)
- `PENDING`: Waiting for AA final approval
- `APPROVED`: Auto-execution triggered

---

## Notifications

### Email to Student (Approved)

```
Subject: Your Transfer Request has been Approved

Dear {student_name},

Your transfer request has been approved!

Old Class: {current_class_code}
New Class: {target_class_code}
Effective Date: {effective_date}

New Class Details:
- Branch: {branch_name}
- Schedule: {schedule_days}
- Teacher: {teacher_name}
- Location: {modality} {location}

Important:
- Attend new class starting {effective_date}
- Your schedule has been updated automatically
- Contact Academic Affairs if you have questions

Welcome to your new class!

Best regards,
Academic Affairs Team
```

### Email to Old Teacher

```
Subject: Student Transfer Notice - {student_name}

Dear {teacher_name},

A student will be leaving your class:

Student: {student_name} ({student_code})
Last Class Date: {last_session_date}
Reason: Transfer to {target_class_code}

Please:
- Update your records
- No further attendance marking required

Thank you!
Academic Affairs Team
```

### Email to New Teacher

```
Subject: New Student Joining Your Class - {student_name}

Dear {teacher_name},

A new student will join your class via transfer:

Student: {student_name} ({student_code})
First Class Date: {effective_date}
Previous Class: {current_class_code}

Please:
- Welcome the student
- Include in attendance starting {effective_date}
- Provide any catch-up materials if needed

Thank you!
Academic Affairs Team
```

---

## Key Points for Implementation

1. **ONE Transfer Limit:** Enforce at database level with CHECK constraint
2. **Tier Detection:** Automatic based on branch/modality change
3. **Tier 2 Confirmation:** 48-hour deadline, auto-reject if timeout
4. **Content Gap Analysis:** Calculate and display to student and AA
5. **Auto-Execution:** Transfer enrollments and sessions in single transaction
6. **Audit Trail:** Track old enrollment (transferred), new enrollment (enrolled)
7. **Bidirectional Tracking:** `left_session_id` and `join_session_id`
8. **No Deletion:** Status updates only, preserve full history
9. **Race Condition:** Re-check capacity in approval transaction
10. **Notifications:** Alert student, old teacher, new teacher

---

## Performance Considerations

**Indexes:**
```sql
CREATE INDEX idx_enrollment_student_course ON enrollment(student_id, course_id, status);
CREATE INDEX idx_enrollment_transfer_count ON enrollment(student_id, course_id, transfer_count);
CREATE INDEX idx_session_class_date ON session(class_id, date);
```

**Query Optimization:**
```sql
-- Find transfer-eligible classes
SELECT c.* 
FROM class c
WHERE c.course_id = ?
  AND c.id != ?  -- Exclude current class
  AND c.status IN ('ONGOING', 'PLANNED')
  AND c.branch_id = ?  -- Tier 1 filter
  AND c.modality = ?   -- Tier 1 filter
  AND (SELECT COUNT(*) FROM enrollment e WHERE e.class_id = c.id AND e.status = 'ENROLLED') 
      < c.max_capacity;
```

---

**End of Transfer Request Guide**
