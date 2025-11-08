# Generate Frontend Handoff Documentation

---

## Task: Generate Frontend Handoff Documentation

Create a comprehensive handoff document for the frontend team in `docs/frontend-handoff/[feature-name].md` following this structure:

### 1. Feature Overview (2-3 sentences)
- What this feature does from user perspective
- Main business flow/purpose
- Who can use it (roles/permissions)

### 2. API Endpoints Table
| Endpoint | Method | Description | Required Role | Success Status |
|----------|--------|-------------|---------------|----------------|
| `/api/v1/example` | GET | Get all examples | ADMIN/MANAGER | 200 |

### 3. Request/Response DTOs
For each endpoint, provide:
```json
// Request (if applicable)
{
  "fieldName": "type",
  "description": "what this field does"
}

// Response
{
  "success": boolean,
  "message": "string",
  "data": {
    // Main response structure
  }
}
```

### 4. Validation Rules & Error Handling
- Required fields with constraints
- Field formats (email, phone, etc.)
- Length limits, allowed values
- Common error scenarios and response format
- Error message format: `{ "success": false, "message": "error description" }`

### 5. Business Rules & Logic
- Important business constraints
- State transitions (if any)
- Calculations or processing rules
- Permission/role restrictions

### 6. Flow Sequence (if applicable)
For multi-step processes:
1. Step 1: Call endpoint X with Y data
2. Step 2: Use result to call endpoint Z
3. Step 3: Final confirmation with endpoint W

### 7. Implementation Notes for Frontend
- Required headers (Authorization Bearer token)
- Pagination format (if applicable)
- File upload considerations (if any)
- Special handling requirements
- Loading states or long-running operations

### 8. Testing Examples
Provide curl examples for key endpoints:
```bash
curl -X POST http://localhost:8080/api/v1/example \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{ "field": "value" }'
```

## File Location Guidelines
- Create in: `docs/frontend-handoff/[feature-name].md`
- Use kebab-case for filename (e.g., `student-management.md`)
- Update index.md if exists to link new document

## Requirements
- Keep documentation under 2 pages
- Use clear, concise language
- Focus on what frontend needs to implement
- Include actual code examples from your implementation
- Reference any enum values or constants that frontend needs

## Example Structure
See existing examples in `docs/frontend-handoff/` for format reference.

---

**Note:** This document should enable frontend developers to integrate with the API without needing additional clarification from the backend team.