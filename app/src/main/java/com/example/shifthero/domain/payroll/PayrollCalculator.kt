package com.example.shifthero.domain.payroll

import com.example.shifthero.core.model.Employee
import com.example.shifthero.core.model.PayrollSummary
import com.example.shifthero.core.model.ShiftAssignment
import com.example.shifthero.core.model.ShiftRequirement
import com.example.shifthero.core.model.SwapRequest
import com.example.shifthero.core.model.SwapStatus
import com.example.shifthero.core.model.WageRule
import com.example.shifthero.core.model.durationHours
import com.example.shifthero.core.model.isNightShift
import kotlin.math.roundToInt

object PayrollCalculator {
    fun calculate(
        employee: Employee,
        shifts: List<ShiftRequirement>,
        assignments: List<ShiftAssignment>,
        swapRequests: List<SwapRequest>,
        wageRule: WageRule,
    ): PayrollSummary {
        val assignedShiftIds = assignments
            .filter { it.employeeId == employee.id }
            .map { it.shiftId }
            .toSet()
        val approvedSwapShiftIds = swapRequests
            .filter { it.accepterEmployeeId == employee.id && it.status == SwapStatus.Approved }
            .map { it.shiftId }
            .toSet()
        val coveredShiftIds = assignedShiftIds + approvedSwapShiftIds
        val employeeShifts = shifts.filter { it.id in coveredShiftIds }
        val totalHours = employeeShifts.sumOf { it.durationHours }
        val basePay = employeeShifts.sumOf { shift ->
            val multiplier = when {
                shift.isNightShift() -> wageRule.nightMultiplier
                shift.isHoliday -> wageRule.holidayMultiplier
                else -> 1.0
            }
            (employee.hourlyRate * shift.durationHours * multiplier).roundToInt()
        }
        val bonusPay = swapRequests
            .filter { it.accepterEmployeeId == employee.id && it.status == SwapStatus.Approved }
            .sumOf { it.subsidy }

        return PayrollSummary(
            totalHours = totalHours,
            basePay = basePay,
            bonusPay = bonusPay,
            estimatedPay = basePay + bonusPay,
            targetIncome = wageRule.monthlyTargetIncome,
        )
    }
}
