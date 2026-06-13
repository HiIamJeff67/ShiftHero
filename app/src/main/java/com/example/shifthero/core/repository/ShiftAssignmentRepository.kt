package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.string
import com.example.shifthero.core.model.ShiftAssignment

interface ShiftAssignmentRepository {
    fun getAssignments(): List<ShiftAssignment>
    fun replaceAssignments(assignments: List<ShiftAssignment>)
    fun replaceShiftOwner(shiftId: String, fromEmployeeId: String, toEmployeeId: String, reason: String)
    fun upsertAssignment(assignment: ShiftAssignment)
    fun removeAssignment(shiftId: String, employeeId: String)
}

class SqliteShiftAssignmentRepository(
    private val database: SQLiteDatabase,
) : ShiftAssignmentRepository {
    override fun getAssignments(): List<ShiftAssignment> {
        return database.query("assignments", null, null, null, null, null, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(ShiftAssignment(cursor.string("shift_id"), cursor.string("employee_id"), cursor.string("reason")))
                }
            }
        }
    }

    override fun replaceAssignments(assignments: List<ShiftAssignment>) {
        database.beginTransaction()
        try {
            database.delete("assignments", null, null)
            assignments.forEach(::insertAssignment)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    override fun replaceShiftOwner(shiftId: String, fromEmployeeId: String, toEmployeeId: String, reason: String) {
        database.beginTransaction()
        try {
            database.delete("assignments", "shift_id = ? AND employee_id = ?", arrayOf(shiftId, fromEmployeeId))
            insertAssignment(ShiftAssignment(shiftId, toEmployeeId, reason))
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    override fun upsertAssignment(assignment: ShiftAssignment) {
        insertAssignment(assignment)
    }

    override fun removeAssignment(shiftId: String, employeeId: String) {
        database.delete("assignments", "shift_id = ? AND employee_id = ?", arrayOf(shiftId, employeeId))
    }

    private fun insertAssignment(assignment: ShiftAssignment) {
        database.insertWithOnConflict(
            "assignments",
            null,
            ContentValues().apply {
                put("shift_id", assignment.shiftId)
                put("employee_id", assignment.employeeId)
                put("reason", assignment.reason)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }
}
