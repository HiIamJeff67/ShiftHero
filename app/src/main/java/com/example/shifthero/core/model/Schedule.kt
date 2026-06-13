package com.example.shifthero.core.model

enum class ShiftBlock(
    val label: String,
    val timeRange: String,
    val hours: Double,
    val isNight: Boolean = false,
) {
    Morning("早班", "10:00-14:00", 4.0),
    Afternoon("午班", "14:00-18:00", 4.0),
    Evening("晚班", "18:00-22:00", 4.0),
    Closing("閉店", "22:00-24:00", 2.0, isNight = true),
    Custom("自訂", "Custom", 0.0),
    ;

    val defaultStartMinute: Int
        get() = when (this) {
            Morning -> 10 * 60
            Afternoon -> 14 * 60
            Evening -> 18 * 60
            Closing -> 22 * 60
            Custom -> 9 * 60
        }

    val defaultEndMinute: Int
        get() = when (this) {
            Morning -> 14 * 60
            Afternoon -> 18 * 60
            Evening -> 22 * 60
            Closing -> 24 * 60
            Custom -> 13 * 60
        }

    companion object {
        fun fromMinuteRange(startMinute: Int, endMinute: Int): ShiftBlock {
            return when {
                startMinute < 14 * 60 && endMinute <= 14 * 60 -> Morning
                startMinute >= 14 * 60 && endMinute <= 18 * 60 -> Afternoon
                startMinute >= 18 * 60 && endMinute <= 22 * 60 -> Evening
                startMinute >= 22 * 60 -> Closing
                else -> Custom
            }
        }
    }
}

enum class HeatStatus(val label: String) {
    Shortage("缺人"),
    Balanced("剛好"),
    Surplus("充足"),
}

data class AvailabilitySlot(
    val id: String = "",
    val employeeId: String,
    val day: String,
    val block: ShiftBlock,
    val startMinute: Int = block.defaultStartMinute,
    val endMinute: Int = block.defaultEndMinute,
    val note: String = "",
)

data class ShiftRequirement(
    val id: String,
    val day: String,
    val block: ShiftBlock,
    val role: String,
    val requiredCount: Int,
    val isHoliday: Boolean = false,
    val startMinute: Int = block.defaultStartMinute,
    val endMinute: Int = block.defaultEndMinute,
)

val ShiftRequirement.durationHours: Double
    get() = ((endMinute - startMinute).coerceAtLeast(0) / 60.0)

fun ShiftRequirement.isNightShift(): Boolean {
    return endMinute > 22 * 60 || startMinute >= 22 * 60
}

fun ShiftRequirement.timeRangeLabel(): String {
    fun toLabel(totalMinutes: Int): String {
        val normalized = totalMinutes.coerceIn(0, 24 * 60)
        val hour = normalized / 60
        val minute = normalized % 60
        return "%02d:%02d".format(hour, minute)
    }
    return "${toLabel(startMinute)}-${toLabel(endMinute)}"
}

fun ShiftRequirement.withResolvedBlock(): ShiftRequirement {
    return copy(block = ShiftBlock.fromMinuteRange(startMinute, endMinute))
}

data class ShiftAssignment(
    val shiftId: String,
    val employeeId: String,
    val reason: String,
    val id: String = "",
)

data class HeatCell(
    val shift: ShiftRequirement,
    val assignedCount: Int,
    val status: HeatStatus,
)

data class ScheduleSuggestion(
    val assignments: List<ShiftAssignment>,
    val messages: List<String>,
)
