package com.example.shifthero.domain.scheduling

import com.example.shifthero.core.model.ShiftAssignment
import com.example.shifthero.core.model.ShiftBlock
import com.example.shifthero.core.model.ShiftRequirement
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulingEngineTest {
    @Test
    fun totalShortageSumsMissingHeadcountInsteadOfShiftCount() {
        val shifts = listOf(
            shift(id = "shift-1", requiredCount = 10),
            shift(id = "shift-2", requiredCount = 3),
        )
        val assignments = listOf(
            assignment(shiftId = "shift-1", employeeId = "employee-1"),
            assignment(shiftId = "shift-2", employeeId = "employee-2"),
            assignment(shiftId = "shift-2", employeeId = "employee-3"),
        )

        assertEquals(10, SchedulingEngine.totalShortage(shifts, assignments))
    }

    private fun shift(id: String, requiredCount: Int) = ShiftRequirement(
        id = id,
        day = "週一 6/8",
        block = ShiftBlock.Custom,
        role = "Staff",
        requiredCount = requiredCount,
        startMinute = 9 * 60,
        endMinute = 17 * 60,
    )

    private fun assignment(shiftId: String, employeeId: String) = ShiftAssignment(
        shiftId = shiftId,
        employeeId = employeeId,
        reason = "test",
    )
}
