package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.int
import com.example.shifthero.core.database.nullableString
import com.example.shifthero.core.database.string
import com.example.shifthero.core.model.SwapRequest
import com.example.shifthero.core.model.SwapStatus

interface SwapRequestRepository {
    fun getSwapRequests(): List<SwapRequest>
    fun addSwapRequest(request: SwapRequest)
    fun updateSwapRequest(request: SwapRequest)
}

class SqliteSwapRequestRepository(
    private val database: SQLiteDatabase,
) : SwapRequestRepository {
    override fun getSwapRequests(): List<SwapRequest> {
        return database.query("swaps", null, null, null, null, null, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SwapRequest(
                            id = cursor.string("id"),
                            shiftId = cursor.string("shift_id"),
                            requesterEmployeeId = cursor.string("requester_employee_id"),
                            accepterEmployeeId = cursor.nullableString("accepter_employee_id"),
                            subsidy = cursor.int("subsidy"),
                            reason = cursor.string("reason"),
                            status = SwapStatus.valueOf(cursor.string("status")),
                        )
                    )
                }
            }
        }
    }

    override fun addSwapRequest(request: SwapRequest) {
        database.insert("swaps", null, request.toContentValues())
    }

    override fun updateSwapRequest(request: SwapRequest) {
        database.update("swaps", request.toContentValues(), "id = ?", arrayOf(request.id))
    }

    private fun SwapRequest.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("shift_id", shiftId)
            put("requester_employee_id", requesterEmployeeId)
            put("accepter_employee_id", accepterEmployeeId)
            put("subsidy", subsidy)
            put("reason", reason)
            put("status", status.name)
        }
    }
}
