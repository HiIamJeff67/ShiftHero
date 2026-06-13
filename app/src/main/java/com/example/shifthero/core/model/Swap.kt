package com.example.shifthero.core.model

enum class SwapStatus(val label: String) {
    Open("徵 Shift"),
    Claimed("待核准"),
    Approved("已核准"),
    Cancelled("已取消"),
}

data class SwapRequest(
    val id: String,
    val shiftId: String,
    val requesterEmployeeId: String,
    val accepterEmployeeId: String?,
    val subsidy: Int,
    val reason: String,
    val status: SwapStatus,
)
