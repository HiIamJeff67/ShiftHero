package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.long
import com.example.shifthero.core.database.nullableString
import com.example.shifthero.core.database.string
import com.example.shifthero.core.model.CompanyJoinRequest
import com.example.shifthero.core.model.CompanyJoinRequestStatus
import com.example.shifthero.core.model.EmployeeRole
import java.util.UUID

interface CompanyJoinRequestRepository {
    fun getRequestsByCompany(companyId: String): List<CompanyJoinRequest>
    fun getRequestsByRequester(requesterUserId: String): List<CompanyJoinRequest>
    fun getRequestById(requestId: String): CompanyJoinRequest?
    fun hasPendingRequest(companyId: String, requesterUserId: String): Boolean
    fun createRequest(
        companyId: String,
        companyName: String,
        requesterUserId: String,
        requesterName: String,
        requesterEmail: String,
        requestedRole: EmployeeRole,
        note: String,
    ): CompanyJoinRequest
    fun updateStatus(
        requestId: String,
        status: CompanyJoinRequestStatus,
        reviewedByUserId: String?,
    )
}

class SqliteCompanyJoinRequestRepository(
    private val database: SQLiteDatabase,
) : CompanyJoinRequestRepository {
    override fun getRequestsByCompany(companyId: String): List<CompanyJoinRequest> {
        return database.query(
            "company_join_requests",
            null,
            "company_id = ?",
            arrayOf(companyId),
            null,
            null,
            "created_at_epoch_ms DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toCompanyJoinRequest())
            }
        }
    }

    override fun getRequestsByRequester(requesterUserId: String): List<CompanyJoinRequest> {
        return database.query(
            "company_join_requests",
            null,
            "requester_user_id = ?",
            arrayOf(requesterUserId),
            null,
            null,
            "created_at_epoch_ms DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toCompanyJoinRequest())
            }
        }
    }

    override fun getRequestById(requestId: String): CompanyJoinRequest? {
        return database.query(
            "company_join_requests",
            null,
            "id = ?",
            arrayOf(requestId),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.toCompanyJoinRequest()
        }
    }

    override fun hasPendingRequest(companyId: String, requesterUserId: String): Boolean {
        return database.query(
            "company_join_requests",
            arrayOf("id"),
            "company_id = ? AND requester_user_id = ? AND status = ?",
            arrayOf(companyId, requesterUserId, CompanyJoinRequestStatus.Pending.name),
            null,
            null,
            null,
            "1",
        ).use { cursor -> cursor.moveToFirst() }
    }

    override fun createRequest(
        companyId: String,
        companyName: String,
        requesterUserId: String,
        requesterName: String,
        requesterEmail: String,
        requestedRole: EmployeeRole,
        note: String,
    ): CompanyJoinRequest {
        val now = System.currentTimeMillis()
        val request = CompanyJoinRequest(
            id = UUID.randomUUID().toString(),
            companyId = companyId.trim(),
            companyName = companyName.trim(),
            requesterUserId = requesterUserId.trim(),
            requesterName = requesterName.trim(),
            requesterEmail = requesterEmail.trim(),
            requestedRole = requestedRole,
            note = note.trim(),
            status = CompanyJoinRequestStatus.Pending,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            reviewedByUserId = null,
        )

        database.insertWithOnConflict(
            "company_join_requests",
            null,
            request.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        return request
    }

    override fun updateStatus(
        requestId: String,
        status: CompanyJoinRequestStatus,
        reviewedByUserId: String?,
    ) {
        database.update(
            "company_join_requests",
            ContentValues().apply {
                put("status", status.name)
                put("updated_at_epoch_ms", System.currentTimeMillis())
                put("reviewed_by_user_id", reviewedByUserId)
            },
            "id = ?",
            arrayOf(requestId),
        )
    }

    private fun android.database.Cursor.toCompanyJoinRequest(): CompanyJoinRequest {
        return CompanyJoinRequest(
            id = string("id"),
            companyId = string("company_id"),
            companyName = string("company_name"),
            requesterUserId = string("requester_user_id"),
            requesterName = string("requester_name"),
            requesterEmail = string("requester_email"),
            requestedRole = runCatching { EmployeeRole.valueOf(string("requested_role")) }.getOrDefault(EmployeeRole.Staff),
            note = string("note"),
            status = runCatching { CompanyJoinRequestStatus.valueOf(string("status")) }.getOrDefault(CompanyJoinRequestStatus.Pending),
            createdAtEpochMs = long("created_at_epoch_ms"),
            updatedAtEpochMs = long("updated_at_epoch_ms"),
            reviewedByUserId = nullableString("reviewed_by_user_id"),
        )
    }

    private fun CompanyJoinRequest.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("company_id", companyId)
            put("company_name", companyName)
            put("requester_user_id", requesterUserId)
            put("requester_name", requesterName)
            put("requester_email", requesterEmail)
            put("requested_role", requestedRole.name)
            put("note", note)
            put("status", status.name)
            put("created_at_epoch_ms", createdAtEpochMs)
            put("updated_at_epoch_ms", updatedAtEpochMs)
            put("reviewed_by_user_id", reviewedByUserId)
        }
    }
}
