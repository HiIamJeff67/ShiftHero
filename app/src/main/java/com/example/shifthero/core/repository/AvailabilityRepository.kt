package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.string
import com.example.shifthero.core.model.AvailabilitySlot
import com.example.shifthero.core.model.ShiftBlock

interface AvailabilityRepository {
    fun getAvailabilitySlots(): List<AvailabilitySlot>
    fun toggleAvailability(employeeId: String, day: String, block: ShiftBlock)
}

class SqliteAvailabilityRepository(
    private val database: SQLiteDatabase,
) : AvailabilityRepository {
    override fun getAvailabilitySlots(): List<AvailabilitySlot> {
        return database.query("availability", null, null, null, null, null, "day ASC").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        AvailabilitySlot(
                            employeeId = cursor.string("employee_id"),
                            day = cursor.string("day"),
                            block = ShiftBlock.valueOf(cursor.string("block")),
                            note = cursor.string("note"),
                        )
                    )
                }
            }
        }
    }

    override fun toggleAvailability(employeeId: String, day: String, block: ShiftBlock) {
        val exists = database.query(
            "availability",
            arrayOf("employee_id"),
            "employee_id = ? AND day = ? AND block = ?",
            arrayOf(employeeId, day, block.name),
            null,
            null,
            null,
        ).use { it.moveToFirst() }
        if (exists) {
            database.delete("availability", "employee_id = ? AND day = ? AND block = ?", arrayOf(employeeId, day, block.name))
        } else {
            database.insert(
                "availability",
                null,
                ContentValues().apply {
                    put("employee_id", employeeId)
                    put("day", day)
                    put("block", block.name)
                    put("note", "")
                },
            )
        }
    }
}
