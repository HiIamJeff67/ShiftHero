package com.example.shifthero.core.model

data class WageRule(
    val baseHourlyRate: Int,
    val holidayMultiplier: Double,
    val nightMultiplier: Double,
    val overtimeMultiplier: Double,
    val monthlyTargetIncome: Int,
)

data class PayrollSummary(
    val totalHours: Double,
    val basePay: Int,
    val bonusPay: Int,
    val estimatedPay: Int,
    val targetIncome: Int,
)
