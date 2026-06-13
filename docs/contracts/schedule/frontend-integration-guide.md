# Schedule Frontend Integration Guide

## Scope

This guide describes how Android/frontend should integrate with the backend-owned schedule APIs.

Base path follows the existing backend environment prefix:

```text
/api/development/v1
```

All endpoints below require:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

All successful responses use the existing envelope:

```json
{
  "success": true,
  "data": {},
  "exception": null
}
```

Errors use:

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

## Important Rules

Do not store schedule business state in Android SQLite as source of truth. The backend owns:

- shift requirements
- availability slots
- shift assignments
- swap requests
- schedule publication state
- schedule settings

Use ISO-8601 timestamps for `startAt`, `endAt`, `createdAt`, `updatedAt`, and `publishedAt`.

Use `YYYY-MM-DD` for `weekStart`.

Default timezone is:

```text
Asia/Taipei
```

Frontend should call the contract paths:

```text
GET /companies/{companyId}/scheduleSettings
PATCH /companies/scheduleSettings
```

The backend still keeps older `/settings` routes for compatibility, but new frontend code should use `/scheduleSettings`.

## Permissions

Company members can read scheduling data for their company.

Managers can:

- create, update, delete shift requirements
- generate assignments
- replace assignments
- approve swap requests
- update schedule publication state
- update schedule settings

Staff can:

- submit their own availability
- delete their own availability
- create swap requests for their own assignments
- claim another member's open swap request
- cancel their own non-approved swap request

The authenticated token decides caller identity. Do not send arbitrary `userId` for caller-scoped mutations such as availability upsert.

## Schedule Publication

Use this to decide whether a company week is still `Draft` or visible as `Published`.

### Get Publication State

```http
GET /companies/{companyId}/schedulePublications?weekStart=2026-06-01&timezone=Asia/Taipei
```

Rules:

- caller must be a company member
- `weekStart` is required
- `timezone` is optional and defaults to `Asia/Taipei`
- if no row exists, backend returns a default `Draft` object instead of 404

Response data:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "weekStart": "2026-06-01",
  "timezone": "Asia/Taipei",
  "status": "Draft",
  "publishedByUserId": null,
  "publishedAt": null,
  "updatedAt": "2026-06-05T09:00:00Z",
  "createdAt": "2026-06-05T09:00:00Z"
}
```

### Publish Or Return To Draft

```http
PUT /companies/schedulePublications
```

Publish body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "weekStart": "2026-06-01",
  "timezone": "Asia/Taipei",
  "status": "Published"
}
```

Return to draft body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "weekStart": "2026-06-01",
  "timezone": "Asia/Taipei",
  "status": "Draft"
}
```

Rules:

- caller must be a Manager
- backend upserts by `companyId + weekStart`
- `Published` sets `publishedByUserId` to the caller and sets `publishedAt`
- `Draft` clears `publishedByUserId` and `publishedAt`

Status enum:

```text
Draft
Published
```

## Schedule Settings

Use this for company-level schedule behavior. These settings should not be local-only UI settings.

### Get Settings

```http
GET /companies/{companyId}/scheduleSettings
```

Rules:

- caller must be a company member
- if settings do not exist yet, backend creates/returns defaults

Response data:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "autoApproveSwaps": false,
  "maxWeeklyHours": 40,
  "minRestHours": 8,
  "timezone": "Asia/Taipei",
  "updatedAt": "2026-06-05T09:00:00Z",
  "createdAt": "2026-06-05T09:00:00Z"
}
```

### Update Settings

```http
PATCH /companies/scheduleSettings
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "values": {
    "autoApproveSwaps": true,
    "maxWeeklyHours": 40,
    "minRestHours": 8,
    "timezone": "Asia/Taipei"
  }
}
```

Rules:

- caller must be a Manager
- at least one field must be present in `values`
- `timezone` must be a valid IANA timezone, for example `Asia/Taipei`

## Shift Requirements

Managers use shift requirements to define required staffing windows.

### Create Requirement

```http
POST /companies/shiftRequirements
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "employeeRole": "Staff",
  "startAt": "2026-06-01T09:00:00Z",
  "endAt": "2026-06-01T13:00:00Z",
  "requiredCount": 2,
  "note": "Lunch rush"
}
```

Rules:

- caller must be a Manager
- `employeeRole` is `Manager` or `Staff`
- `startAt` must be before `endAt`
- backend truncates times to minute precision

### Get Requirements

```http
GET /companies/{companyId}/shiftRequirements
```

Rules:

- caller must be a company member
- sorted by `startAt ASC`

### Update Requirement

```http
PATCH /companies/shiftRequirements
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "shiftRequirementId": "00000000-0000-0000-0000-000000000101",
  "values": {
    "requiredCount": 3,
    "note": "Updated demand"
  }
}
```

Rules:

- caller must be a Manager
- `values` can include `employeeRole`, `startAt`, `endAt`, `requiredCount`, `note`

### Delete Requirement

```http
DELETE /companies/shiftRequirements
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "shiftRequirementId": "00000000-0000-0000-0000-000000000101"
}
```

## Availability Slots

Staff submit their own availability. The backend uses the authenticated caller as `userId`.

### Upsert Availability

```http
PUT /companies/availabilitySlots
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "slots": [
    {
      "startAt": "2026-06-01T09:00:00Z",
      "endAt": "2026-06-01T17:00:00Z",
      "isAvailable": true
    }
  ]
}
```

Rules:

- caller must be a company member
- no `userId` is accepted in body
- same `companyId + caller userId + startAt + endAt` updates the existing slot

### Get Availability

```http
GET /companies/{companyId}/availabilitySlots?userId={userId}&startAt=2026-06-01T00:00:00Z&endAt=2026-06-08T00:00:00Z
```

Query params are optional:

- `userId`
- `startAt`
- `endAt`

Rules:

- caller must be a company member
- sorted by `startAt ASC`

### Delete Availability

```http
DELETE /companies/availabilitySlots
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "availabilitySlotId": "00000000-0000-0000-0000-000000000201"
}
```

Rules:

- Managers can delete company availability
- Staff can only delete their own availability

## Assignments

Assignments are the actual remote schedule.

### Generate Assignments

```http
POST /companies/assignments/generate
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "startAt": "2026-06-01T00:00:00Z",
  "endAt": "2026-06-08T00:00:00Z"
}
```

Rules:

- caller must be a Manager
- `startAt` and `endAt` are optional filters
- backend matches availability slots covering each requirement window

### Replace Assignments

```http
PUT /companies/assignments
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "assignments": [
    {
      "shiftRequirementId": "00000000-0000-0000-0000-000000000101",
      "userId": "00000000-0000-0000-0000-000000000301",
      "startAt": "2026-06-01T09:00:00Z",
      "endAt": "2026-06-01T13:00:00Z"
    }
  ]
}
```

Rules:

- caller must be a Manager
- replaces all assignments for the company
- frontend should refresh assignments after success

### Get Assignments

```http
GET /companies/{companyId}/assignments?userId={userId}&startAt=2026-06-01T00:00:00Z&endAt=2026-06-08T00:00:00Z
```

Query params are optional:

- `userId`
- `startAt`
- `endAt`

Rules:

- caller must be a company member
- sorted by `startAt ASC`

## Swap Requests

### Create Swap Request

```http
POST /companies/swapRequests
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "shiftAssignmentId": "00000000-0000-0000-0000-000000000401",
  "reason": "Need coverage"
}
```

Rules:

- caller must be assigned to `shiftAssignmentId`
- new request starts with status `Open`

### Get Swap Requests

```http
GET /companies/{companyId}/swapRequests?status=Open
```

Query params are optional:

- `status`: `Open`, `Claimed`, `Approved`, `Cancelled`

Rules:

- caller must be a company member
- sorted by `createdAt DESC`

### Claim Swap Request

```http
POST /companies/swapRequests/claim
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "swapRequestId": "00000000-0000-0000-0000-000000000501"
}
```

Rules:

- caller must be a company member
- only `Open` requests can be claimed
- requester cannot claim their own request
- status becomes `Claimed`

### Approve Swap Request

```http
POST /companies/swapRequests/approve
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "swapRequestId": "00000000-0000-0000-0000-000000000501"
}
```

Rules:

- caller must be a Manager
- only `Claimed` requests can be approved
- assignment `userId` changes to `claimedByUserId`
- status becomes `Approved`
- frontend should refresh assignments and swap requests after success

### Cancel Swap Request

```http
POST /companies/swapRequests/cancel
```

Body:

```json
{
  "companyId": "00000000-0000-0000-0000-000000000001",
  "swapRequestId": "00000000-0000-0000-0000-000000000501"
}
```

Rules:

- Manager can cancel non-approved requests
- requester can cancel their own non-approved request
- `Approved` or `Cancelled` requests cannot be cancelled again

## Recommended Frontend Flows

### Staff Opens Weekly Schedule

1. Compute visible `weekStart` as `YYYY-MM-DD`.
2. `GET /companies/{companyId}/schedulePublications?weekStart={weekStart}&timezone=Asia/Taipei`
3. `GET /companies/{companyId}/shiftRequirements`
4. `GET /companies/{companyId}/assignments?startAt={weekStartStartIso}&endAt={nextWeekStartIso}`
5. If staff view needs swap data, call `GET /companies/{companyId}/swapRequests`.

If publication status is `Draft`, frontend can hide staff-visible schedule details or show a draft indicator depending on product requirements.

### Manager Builds And Publishes Weekly Schedule

1. Create or update shift requirements.
2. Review staff availability.
3. Generate assignments or manually replace assignments.
4. Review assignments.
5. Publish:

```json
{
  "companyId": "{companyId}",
  "weekStart": "2026-06-01",
  "timezone": "Asia/Taipei",
  "status": "Published"
}
```

6. Refresh publication state and assignments.

### Manager Opens AI Briefing

Use `POST /companies/{companyId}/ai/scheduleInsights` for a regular JSON response.

Use `POST /companies/{companyId}/ai/scheduleInsights/stream` for the animated briefing UI. Because this is an authenticated POST request, use a fetch-based SSE parser instead of browser `EventSource`.

Handle these event names:

- `stage`: show the current analysis phase
- `token`: append text to the briefing
- `done`: store the final metrics and metadata
- `error`: stop the loading state and show the safe API error

Read `done.data.aiUsage` to display the monthly usage meter. For the JSON endpoint, handle HTTP `429` with exception reason `AIUsageLimitExceeded`. For streaming, the same exception arrives in the `error` event because the SSE response has already started.

See `docs/AI_SCHEDULE_INSIGHTS.md` for the request body and workflow details.

### Staff Submits Availability

1. `PUT /companies/availabilitySlots`
2. Refresh with `GET /companies/{companyId}/availabilitySlots?userId={currentUserId}`

The body must not include `userId`; backend uses the token user.

### Swap Lifecycle

1. Assigned staff creates swap request.
2. Other member claims it.
3. Manager approves it.
4. Frontend refreshes assignments and swap requests.

## Client Handling Notes

Treat mutation success as authoritative, but refresh list endpoints after mutations.

For date filters, use inclusive overlap behavior:

- availability/assignments with `endAt >= startAtFilter`
- availability/assignments with `startAt <= endAtFilter`

For `PATCH` requests, send only changed fields under `values`.

For `weekStart`, send date only:

```text
2026-06-01
```

Do not send a timestamp for `weekStart`.
