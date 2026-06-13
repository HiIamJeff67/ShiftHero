package com.example.shifthero.core.model

data class CompanySummary(
    val id: String,
    val name: String,
)

data class ScheduleSettings(
    val autoApproveSwaps: Boolean = false,
    val maxWeeklyHours: Int = 40,
    val minRestHours: Int = 8,
    val timezone: String = "Asia/Taipei",
)

data class AppState(
    val storeName: String,
    val currentCompanyId: String,
    val currentCompanyEmail: String,
    val currentCompanyDescription: String,
    val availableCompanies: List<CompanySummary>,
    val days: List<String>,
    val employees: List<Employee>,
    val viewerUserId: String,
    val viewerDisplayName: String,
    val viewerEmail: String,
    val viewerRoleInCompany: EmployeeRole?,
    val wageRule: WageRule,
    val shiftRequirements: List<ShiftRequirement>,
    val availabilitySlots: List<AvailabilitySlot>,
    val assignments: List<ShiftAssignment>,
    val swapRequests: List<SwapRequest>,
    val companyJoinRequests: List<CompanyJoinRequest>,
    val myJoinRequests: List<CompanyJoinRequest>,
    val scheduleSettings: ScheduleSettings = ScheduleSettings(),
)
