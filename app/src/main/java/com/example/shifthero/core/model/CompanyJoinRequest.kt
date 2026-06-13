package com.example.shifthero.core.model

enum class CompanyJoinRequestStatus(val label: String) {
    Pending("待審核"),
    Approved("已核准"),
    Rejected("已婉拒"),
    Cancelled("已取消"),
}

data class CompanyJoinRequest(
    val id: String,
    val companyId: String,
    val companyName: String,
    val requesterUserId: String,
    val requesterName: String,
    val requesterEmail: String,
    val requestedRole: EmployeeRole,
    val note: String,
    val status: CompanyJoinRequestStatus,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val reviewedByUserId: String?,
)
