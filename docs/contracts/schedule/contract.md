# Schedule API Contract

## Purpose

ShiftHero scheduling must be remote-first. Any data that affects Manager and Staff interaction must be owned by the backend, not by Android SQLite.

This schedule feature includes:

```text
shift requirements: Manager creates minute-based staffing requirements.
availability slots: Staff submits their own available time windows.
assignments: Backend generates or Manager replaces assigned shifts.
swap requests: Staff requests shift swaps; coworkers claim; Manager approves.
schedule publications: Manager publishes or returns a company week to Draft.
schedule settings: Company-level scheduling rules such as max weekly hours and rest hours.
AI schedule insights: Manager-only deterministic workforce metrics plus an LLM-generated briefing.
```

Android local SQLite should only handle application-local settings such as API URL, theme, and other UI preferences. It must not store employees, shift requirements, availability, assignments, swap requests, company join requests, or schedule publication state as source of truth.

All paths below are relative to the existing API base, for example:

```text
/api/development/v1
```

This feature folder owns its API contracts:

```text
openapi.v1.yaml
postman/ShiftHero-Schedule-v1.postman_collection.json
examples/
```

## Data Models

The backend should own these scheduling models or equivalent tables:

```text
ShiftRequirement
AvailabilitySlot
ShiftAssignment
SwapRequest
SchedulePublication
ScheduleSettings
```

Key fields:

```text
ShiftRequirement:
id uuid primary key
company_id uuid not null
employee_role enum(Manager, Staff) not null
start_at timestamptz not null
end_at timestamptz not null
required_count integer not null
note text nullable
created_at timestamptz not null
updated_at timestamptz not null

AvailabilitySlot:
id uuid primary key
company_id uuid not null
user_id uuid not null
start_at timestamptz not null
end_at timestamptz not null
is_available boolean not null
created_at timestamptz not null
updated_at timestamptz not null

ShiftAssignment:
id uuid primary key
company_id uuid not null
shift_requirement_id uuid not null
user_id uuid not null
start_at timestamptz not null
end_at timestamptz not null
created_at timestamptz not null
updated_at timestamptz not null

SwapRequest:
id uuid primary key
company_id uuid not null
shift_assignment_id uuid not null
requester_user_id uuid not null
claimed_by_user_id uuid nullable
status enum(Open, Claimed, Approved, Cancelled) not null
reason text nullable
created_at timestamptz not null
updated_at timestamptz not null

SchedulePublication:
id uuid primary key
company_id uuid not null
week_start date not null
timezone string not null
status enum(Draft, Published) not null
published_by_user_id uuid nullable
published_at timestamptz nullable
created_at timestamptz not null
updated_at timestamptz not null

ScheduleSettings:
company_id uuid primary key
auto_approve_swaps boolean not null
max_weekly_hours integer nullable
min_rest_hours integer nullable
timezone string not null
created_at timestamptz not null
updated_at timestamptz not null
```

Recommended constraints:

```text
Only Managers can create, update, delete shift requirements.
Availability upsert is scoped to the authenticated caller, not an arbitrary body user id.
Only Managers can generate or replace assignments.
Only the assigned user can create a swap request for their own assignment.
Only company members can claim open swap requests.
Only Managers can approve claimed swap requests.
Only one SchedulePublication row per company_id + week_start.
Missing SchedulePublication should resolve to default Draft instead of 404.
```

## Response Shape

Use the existing backend response envelope:

```json
{
  "success": true,
  "data": {},
  "exception": null
}
```

Error envelope:

```json
{
  "success": false,
  "data": null,
  "exception": {
    "code": 403,
    "reason": "Forbidden",
    "message": "Caller is not allowed to perform this operation.",
    "status": 403
  }
}
```

Concrete response examples are stored in:

```text
examples/
```

## Required Endpoints

### Shift Requirements

```http
POST /companies/shiftRequirements
GET /companies/{companyId}/shiftRequirements
PATCH /companies/shiftRequirements
DELETE /companies/shiftRequirements
```

Purpose:

```text
Manager creates and edits minute-based staffing requirements. Android should not use fixed early/noon/night/closing blocks as the data model.
```

### Availability Slots

```http
PUT /companies/availabilitySlots
GET /companies/{companyId}/availabilitySlots
DELETE /companies/availabilitySlots
```

Purpose:

```text
Staff submits their own available time windows. Manager can read company availability for scheduling.
```

### Assignments

```http
POST /companies/assignments/generate
PUT /companies/assignments
GET /companies/{companyId}/assignments
```

Purpose:

```text
Backend creates or stores the actual assigned shifts. These assignments are the remote source for schedule viewing and swap eligibility.
```

### Swap Requests

```http
POST /companies/swapRequests
GET /companies/{companyId}/swapRequests
POST /companies/swapRequests/claim
POST /companies/swapRequests/approve
POST /companies/swapRequests/cancel
```

Purpose:

```text
Staff can request a swap for their own assignment, other members can claim open requests, and Managers can approve claimed requests.
```

### Schedule Publications

```http
GET /companies/{companyId}/schedulePublications?weekStart=YYYY-MM-DD&timezone=Asia/Taipei
PUT /companies/schedulePublications
```

Purpose:

```text
Manager and Staff devices must agree on whether a company week is Draft or Published.
```

Publication body:

```json
{
  "companyId": "uuid",
  "weekStart": "2026-06-01",
  "timezone": "Asia/Taipei",
  "status": "Published"
}
```

Publication rules:

```text
weekStart is required and formatted as YYYY-MM-DD.
timezone defaults to Asia/Taipei if omitted.
Published sets published_by_user_id and published_at.
Draft clears published_by_user_id and published_at.
Only Managers can update publication state.
Company members can read publication state.
```

### Schedule Settings

```http
GET /companies/{companyId}/scheduleSettings
PATCH /companies/scheduleSettings
```

Purpose:

```text
Company-level schedule rules are remote business settings. They should not be stored as Android SQLite source of truth.
```

### AI Schedule Insights

```http
POST /companies/{companyId}/ai/scheduleInsights
POST /companies/{companyId}/ai/scheduleInsights/stream
```

Purpose:

```text
Managers receive deterministic coverage, workload, fatigue, availability, and swap metrics plus an AI-written briefing. The stream endpoint emits only the final narrative tokens. A successful generation consumes one plan-based monthly AI usage unit and returns `aiUsage`; exhausted quotas return `AIUsageLimitExceeded`.
```

## Minimum Required For Android Frontend

The Android app needs this complete backend-owned scheduling surface:

```text
POST /companies/shiftRequirements
GET /companies/{companyId}/shiftRequirements
PATCH /companies/shiftRequirements
DELETE /companies/shiftRequirements

PUT /companies/availabilitySlots
GET /companies/{companyId}/availabilitySlots
DELETE /companies/availabilitySlots

POST /companies/assignments/generate
PUT /companies/assignments
GET /companies/{companyId}/assignments

POST /companies/swapRequests
GET /companies/{companyId}/swapRequests
POST /companies/swapRequests/claim
POST /companies/swapRequests/approve
POST /companies/swapRequests/cancel

GET /companies/{companyId}/schedulePublications?weekStart=YYYY-MM-DD
PUT /companies/schedulePublications

GET /companies/{companyId}/scheduleSettings
PATCH /companies/scheduleSettings

POST /companies/{companyId}/ai/scheduleInsights
POST /companies/{companyId}/ai/scheduleInsights/stream
```

## Android Integration Plan

```text
Manager creates a minute-based shift requirement:
Android calls POST /companies/shiftRequirements, then refreshes shift requirements.

Manager edits a timeline event:
Android calls PATCH /companies/shiftRequirements, then refreshes shift requirements and assignments.

Staff submits availability:
Android calls PUT /companies/availabilitySlots using the authenticated user context.

Manager generates assignments:
Android calls POST /companies/assignments/generate, then refreshes assignments.

Manager manually adjusts assignments:
Android calls PUT /companies/assignments, then refreshes assignments.

Staff opens schedule:
Android calls GET shift requirements, GET assignments, and GET schedule publication state for the visible week.

Staff creates a swap request:
Android calls POST /companies/swapRequests for one of the authenticated user's assignments.

Coworker claims a swap:
Android calls POST /companies/swapRequests/claim.

Manager approves a swap:
Android calls POST /companies/swapRequests/approve, then refreshes assignments and swap requests.

Manager publishes a schedule:
Android calls PUT /companies/schedulePublications with status Published.
```

Until these endpoints are available, Android must not emulate cross-device scheduling state in SQLite.

## Notes For Backend Agent

```text
Use authenticated context for user identity. Do not trust body user ids for caller-scoped actions.
Use server time for created_at, updated_at, published_at.
Use transactions for assignment replacement and swap approval.
Use company membership checks consistently for all reads.
Use Manager role checks for schedule mutations.
Return default Draft publication state for missing company_id + week_start.
```
