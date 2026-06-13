package com.example.shifthero.domain.scheduling

import com.example.shifthero.core.model.AvailabilitySlot
import com.example.shifthero.core.model.Employee
import com.example.shifthero.core.model.HeatCell
import com.example.shifthero.core.model.HeatStatus
import com.example.shifthero.core.model.ScheduleSuggestion
import com.example.shifthero.core.model.ShiftAssignment
import com.example.shifthero.core.model.ShiftBlock
import com.example.shifthero.core.model.ShiftRequirement
import com.example.shifthero.core.model.durationHours
import com.example.shifthero.core.model.timeRangeLabel

object SchedulingEngine {
    fun totalShortage(
        shifts: List<ShiftRequirement>,
        assignments: List<ShiftAssignment>,
    ): Int {
        val assignedCountByShift = assignments.groupingBy { it.shiftId }.eachCount()
        return shifts.sumOf { shift ->
            (shift.requiredCount - assignedCountByShift.getOrDefault(shift.id, 0)).coerceAtLeast(0)
        }
    }

    fun heatmap(
        shifts: List<ShiftRequirement>,
        assignments: List<ShiftAssignment>,
    ): List<HeatCell> {
        return shifts.map { shift ->
            val assignedCount = assignments.count { it.shiftId == shift.id }
            val status = when {
                assignedCount < shift.requiredCount -> HeatStatus.Shortage
                assignedCount == shift.requiredCount -> HeatStatus.Balanced
                else -> HeatStatus.Surplus
            }
            HeatCell(shift = shift, assignedCount = assignedCount, status = status)
        }
    }

    fun generateRecommendation(
        employees: List<Employee>,
        shifts: List<ShiftRequirement>,
        availabilitySlots: List<AvailabilitySlot>,
        currentAssignments: List<ShiftAssignment>,
    ): ScheduleSuggestion {
        val assignments = currentAssignments.toMutableList()
        val messages = mutableListOf<String>()

        shifts.forEach { shift ->
            val currentCount = assignments.count { it.shiftId == shift.id }
            val missingCount = shift.requiredCount - currentCount
            if (missingCount <= 0) return@forEach

            repeat(missingCount) {
                val candidate = employees
                    .filter { employee ->
                        val shiftBlock = ShiftBlock.fromMinuteRange(shift.startMinute, shift.endMinute)
                        employee.id !in assignments.filter { it.shiftId == shift.id }.map { it.employeeId } &&
                            availabilitySlots.any {
                                it.employeeId == employee.id && it.day == shift.day && it.block == shiftBlock
                            }
                    }
                    .maxByOrNull { employee ->
                        val assignedHours = assignments
                            .filter { it.employeeId == employee.id }
                            .mapNotNull { assignment -> shifts.firstOrNull { it.id == assignment.shiftId } }
                            .sumOf { it.durationHours }
                        val underTargetScore = (employee.targetHours - assignedHours).coerceAtLeast(0.0)
                        val skillScore = if (employee.skillTags.any { shift.role.contains(it) || it.contains(shift.role) }) 10.0 else 0.0
                        underTargetScore * 2 + employee.reliability * 15 + skillScore
                    }

                if (candidate == null) {
                    messages += "${shift.day} ${shift.timeRangeLabel()} ${shift.role} 仍缺 1 人"
                } else {
                    val reason = "符合可上班時段，穩定度 ${(candidate.reliability * 100).toInt()}%，接近希望工時"
                    assignments += ShiftAssignment(shift.id, candidate.id, reason)
                    messages += "${shift.day} ${shift.timeRangeLabel()} 推薦 ${candidate.name}: $reason"
                }
            }
        }

        return ScheduleSuggestion(assignments = assignments, messages = messages)
    }
}
