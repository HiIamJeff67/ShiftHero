package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.int
import com.example.shifthero.core.database.string
import com.example.shifthero.core.model.ShiftBlock
import com.example.shifthero.core.model.ShiftRequirement

interface ShiftRequirementRepository {
    fun getShiftRequirements(): List<ShiftRequirement>
    fun addShiftRequirement(day: String, block: ShiftBlock, role: String, requiredCount: Int, isHoliday: Boolean)
    fun addTimelineShift(day: String, startMinute: Int, endMinute: Int, role: String, requiredCount: Int, isHoliday: Boolean): ShiftRequirement
    fun updateShiftTime(shiftId: String, startMinute: Int, endMinute: Int)
}

class SqliteShiftRequirementRepository(
    private val database: SQLiteDatabase,
) : ShiftRequirementRepository {
    override fun getShiftRequirements(): List<ShiftRequirement> {
        return database.query("shifts", null, null, null, null, null, "day ASC").use { cursor ->
            val startMinuteIndex = cursor.getColumnIndex("start_minute")
            val endMinuteIndex = cursor.getColumnIndex("end_minute")
            buildList {
                while (cursor.moveToNext()) {
                    val block = ShiftBlock.valueOf(cursor.string("block"))
                    val startMinute = if (startMinuteIndex >= 0) cursor.getInt(startMinuteIndex) else block.defaultStartMinute
                    val endMinute = if (endMinuteIndex >= 0) cursor.getInt(endMinuteIndex) else block.defaultEndMinute
                    add(
                        ShiftRequirement(
                            id = cursor.string("id"),
                            day = cursor.string("day"),
                            block = ShiftBlock.fromMinuteRange(startMinute, endMinute),
                            role = cursor.string("role"),
                            requiredCount = cursor.int("required_count"),
                            isHoliday = cursor.int("is_holiday") == 1,
                            startMinute = startMinute,
                            endMinute = endMinute,
                        )
                    )
                }
            }
        }
    }

    override fun addShiftRequirement(
        day: String,
        block: ShiftBlock,
        role: String,
        requiredCount: Int,
        isHoliday: Boolean,
    ) {
        addTimelineShift(
            day = day,
            startMinute = block.defaultStartMinute,
            endMinute = block.defaultEndMinute,
            role = role,
            requiredCount = requiredCount,
            isHoliday = isHoliday,
        )
    }

    override fun addTimelineShift(
        day: String,
        startMinute: Int,
        endMinute: Int,
        role: String,
        requiredCount: Int,
        isHoliday: Boolean,
    ): ShiftRequirement {
        val normalizedStart = startMinute.coerceIn(0, 24 * 60 - 1)
        val normalizedEnd = endMinute.coerceIn(normalizedStart + 1, 24 * 60)
        val block = ShiftBlock.fromMinuteRange(normalizedStart, normalizedEnd)
        val id = "shift_${System.currentTimeMillis()}_${normalizedStart}_$normalizedEnd"
        database.insert(
            "shifts",
            null,
            ContentValues().apply {
                put("id", id)
                put("day", day)
                put("block", block.name)
                put("role", role.trim().ifBlank { "外場" })
                put("required_count", requiredCount.coerceAtLeast(1))
                put("is_holiday", if (isHoliday) 1 else 0)
                put("start_minute", normalizedStart)
                put("end_minute", normalizedEnd)
            },
        )
        return ShiftRequirement(
            id = id,
            day = day,
            block = block,
            role = role.trim().ifBlank { "外場" },
            requiredCount = requiredCount.coerceAtLeast(1),
            isHoliday = isHoliday,
            startMinute = normalizedStart,
            endMinute = normalizedEnd,
        )
    }

    override fun updateShiftTime(shiftId: String, startMinute: Int, endMinute: Int) {
        val normalizedStart = startMinute.coerceIn(0, 24 * 60 - 1)
        val normalizedEnd = endMinute.coerceIn(normalizedStart + 1, 24 * 60)
        val block = ShiftBlock.fromMinuteRange(normalizedStart, normalizedEnd)
        database.update(
            "shifts",
            ContentValues().apply {
                put("block", block.name)
                put("start_minute", normalizedStart)
                put("end_minute", normalizedEnd)
            },
            "id = ?",
            arrayOf(shiftId),
        )
    }
}
