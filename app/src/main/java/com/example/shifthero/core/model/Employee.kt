package com.example.shifthero.core.model

enum class EmployeeRole(val label: String) {
    Manager("店長"),
    Staff("店員"),
}

data class Employee(
    val id: String,
    val name: String,
    val role: EmployeeRole,
    val hourlyRate: Int,
    val targetHours: Double,
    val reliability: Double,
    val skillTags: List<String>,
)
