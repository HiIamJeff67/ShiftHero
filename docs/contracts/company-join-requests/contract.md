# Company Join Requests API Contract

## Purpose

ShiftHero needs a backend-backed company join request workflow.

Currently, the Android client can create join requests locally in SQLite, but that does not work across devices. If a Staff user submits a request on their phone, the Manager cannot see it on another device. The backend must own this data and expose APIs for creating, listing, approving, and rejecting company join requests.

All paths below are relative to the existing API base, for example:

```text
/api/development/v1
```

## Data Model

Create a backend model/table similar to:

```text
CompanyJoinRequest
```

Fields:

```text
id uuid primary key
company_id uuid not null
requester_user_id uuid not null
requested_role string/enum not null
note text nullable
status string/enum not null
reviewed_by_user_id uuid nullable
reviewed_at timestamptz nullable
created_at timestamptz not null
updated_at timestamptz not null
```

Recommended enum values:

```text
requested_role: Staff, Manager
status: Pending, Approved, Rejected, Cancelled
```

Recommended constraints:

```text
Only one Pending request per company_id + requester_user_id.
requester_user_id cannot create a Pending request if already a member of company_id.
Approved or Rejected requests cannot be processed again.
Approve and reject require the caller to be a Manager of that company.
```

## Response Shape

Use the existing backend response envelope:

```json
{
  "success": true,
  "data": {}
}
```

The frontend expects a join request object in this shape:

```json
{
  "id": "uuid",
  "companyId": "uuid",
  "companyName": "Shift Hero Inc",
  "requesterUserId": "uuid",
  "requesterName": "Alice Chen",
  "requesterEmail": "alice@example.com",
  "requestedRole": "Staff",
  "note": "I want to join this company.",
  "status": "Pending",
  "reviewedByUserId": null,
  "reviewedAt": null,
  "createdAt": "2026-06-05T10:00:00Z",
  "updatedAt": "2026-06-05T10:00:00Z"
}
```

## Endpoints

### Create Join Request

Staff creates a request to join a company.

```http
POST /companies/joinRequests
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request body:

```json
{
  "companyId": "uuid",
  "requestedRole": "Staff",
  "note": "optional message"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "companyId": "uuid",
    "companyName": "Shift Hero Inc",
    "requesterUserId": "uuid",
    "requesterName": "Alice Chen",
    "requesterEmail": "alice@example.com",
    "requestedRole": "Staff",
    "note": "optional message",
    "status": "Pending",
    "reviewedByUserId": null,
    "reviewedAt": null,
    "createdAt": "2026-06-05T10:00:00Z",
    "updatedAt": "2026-06-05T10:00:00Z"
  }
}
```

Rules:

```text
Caller must be authenticated.
Caller must not already be a member of the company.
Caller must not already have a Pending request for the same company.
companyId must point to an existing company.
requestedRole should default to Staff if omitted.
```

Recommended errors:

```text
400 invalid companyId/requestedRole
401 unauthenticated
404 company not found
409 already member or pending request already exists
```

### List Join Requests For Company

Manager lists requests submitted by other users for the selected company.

```http
GET /companies/{companyId}/joinRequests
Authorization: Bearer <accessToken>
```

Optional query:

```text
status=Pending
```

Response:

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "companyId": "uuid",
      "companyName": "Shift Hero Inc",
      "requesterUserId": "uuid",
      "requesterName": "Alice Chen",
      "requesterEmail": "alice@example.com",
      "requestedRole": "Staff",
      "note": "I want to join this company.",
      "status": "Pending",
      "reviewedByUserId": null,
      "reviewedAt": null,
      "createdAt": "2026-06-05T10:00:00Z",
      "updatedAt": "2026-06-05T10:00:00Z"
    }
  ]
}
```

Rules:

```text
Caller must be authenticated.
Caller must be a Manager of companyId.
Return requests for that company only.
Do not include requests for unrelated companies.
Default sorting should be newest first.
```

Recommended errors:

```text
401 unauthenticated
403 caller is not a Manager of companyId
404 company not found
```

### Approve Join Request

Manager approves a request and adds the requester as a company member.

```http
POST /companies/joinRequests/approve
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request body:

```json
{
  "companyId": "uuid",
  "joinRequestId": "uuid"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "companyId": "uuid",
    "companyName": "Shift Hero Inc",
    "requesterUserId": "uuid",
    "requesterName": "Alice Chen",
    "requesterEmail": "alice@example.com",
    "requestedRole": "Staff",
    "note": "I want to join this company.",
    "status": "Approved",
    "reviewedByUserId": "uuid",
    "reviewedAt": "2026-06-05T10:05:00Z",
    "createdAt": "2026-06-05T10:00:00Z",
    "updatedAt": "2026-06-05T10:05:00Z"
  }
}
```

Backend transaction:

```text
1. Verify caller is a Manager of companyId.
2. Verify joinRequestId belongs to companyId.
3. Verify request status is Pending.
4. Verify requester is not already a member.
5. Insert membership into UsersToCompaniesTable using requestedRole.
6. Update request status to Approved.
7. Set reviewed_by_user_id and reviewed_at.
8. Commit atomically.
```

Recommended errors:

```text
401 unauthenticated
403 caller is not a Manager of companyId
404 request not found
409 request already processed or requester is already a member
```

### Reject Join Request

Manager rejects a request.

```http
POST /companies/joinRequests/reject
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Request body:

```json
{
  "companyId": "uuid",
  "joinRequestId": "uuid"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "companyId": "uuid",
    "companyName": "Shift Hero Inc",
    "requesterUserId": "uuid",
    "requesterName": "Alice Chen",
    "requesterEmail": "alice@example.com",
    "requestedRole": "Staff",
    "note": "I want to join this company.",
    "status": "Rejected",
    "reviewedByUserId": "uuid",
    "reviewedAt": "2026-06-05T10:05:00Z",
    "createdAt": "2026-06-05T10:00:00Z",
    "updatedAt": "2026-06-05T10:05:00Z"
  }
}
```

Rules:

```text
Caller must be a Manager of companyId.
Request must belong to companyId.
Request must be Pending.
No membership should be created.
Set reviewed_by_user_id and reviewed_at.
```

Recommended errors:

```text
401 unauthenticated
403 caller is not a Manager of companyId
404 request not found
409 request already processed
```

### List My Join Requests

This endpoint is optional, but recommended for frontend UX.

```http
GET /companies/joinRequests/me
Authorization: Bearer <accessToken>
```

Response:

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "companyId": "uuid",
      "companyName": "Shift Hero Inc",
      "requesterUserId": "uuid",
      "requesterName": "Alice Chen",
      "requesterEmail": "alice@example.com",
      "requestedRole": "Staff",
      "note": "I want to join this company.",
      "status": "Pending",
      "reviewedByUserId": null,
      "reviewedAt": null,
      "createdAt": "2026-06-05T10:00:00Z",
      "updatedAt": "2026-06-05T10:00:00Z"
    }
  ]
}
```

Rules:

```text
Caller must be authenticated.
Return only requests where requester_user_id equals caller user id.
Default sorting should be newest first.
```

## Minimum Required For Android Frontend

The Android app needs these endpoints to replace local SQLite join requests:

```text
POST /companies/joinRequests
GET /companies/{companyId}/joinRequests
POST /companies/joinRequests/approve
POST /companies/joinRequests/reject
```

`GET /companies/joinRequests/me` is optional but recommended.

## Android Integration Plan

After backend is ready:

```text
Staff submit request:
Android calls POST /companies/joinRequests.

Manager opens Management -> Requests:
Android calls GET /companies/{companyId}/joinRequests.

Manager approves:
Android calls POST /companies/joinRequests/approve, then refreshes company members and join requests.

Manager rejects:
Android calls POST /companies/joinRequests/reject, then refreshes join requests.
```

The existing Android `company_join_requests` SQLite table must be removed as a source of truth. Join request state is cross-device business data and must be read from the backend.


## Current Implementation Note

This workflow has been implemented by the backend and should now be treated as backend-owned data.

Android must not use a local SQLite `company_join_requests` table as the source of truth. The app should create, list, approve, reject, and cancel join requests through backend APIs so Staff and Manager devices always observe the same request state.

This feature folder also owns its API contracts:

```text
openapi.v1.yaml
postman/ShiftHero-Company-Join-Requests-v1.postman_collection.json
examples/
```
