package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.double
import com.example.shifthero.core.database.int
import com.example.shifthero.core.database.string
import com.example.shifthero.core.model.Employee
import com.example.shifthero.core.model.EmployeeRole

interface EmployeeRepository {
    fun getEmployees(): List<Employee>
    fun addEmployee(name: String, hourlyRate: Int, targetHours: Double): Employee
}

class SqliteEmployeeRepository(
    private val database: SQLiteDatabase,
) : EmployeeRepository {
    override fun getEmployees(): List<Employee> {
        return database.query("employees", null, null, null, null, null, "name ASC").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Employee(
                            id = cursor.string("id"),
                            name = cursor.string("name"),
                            role = EmployeeRole.valueOf(cursor.string("role")),
                            hourlyRate = cursor.int("hourly_rate"),
                            targetHours = cursor.double("target_hours"),
                            reliability = cursor.double("reliability"),
                            skillTags = cursor.string("skill_tags").split(",").filter { it.isNotBlank() },
                        )
                    )
                }
            }
        }
    }

    override fun addEmployee(name: String, hourlyRate: Int, targetHours: Double): Employee {
        val employee = Employee(
            id = "emp_${System.currentTimeMillis()}",
            name = name.trim(),
            role = EmployeeRole.Staff,
            hourlyRate = hourlyRate,
            targetHours = targetHours,
            reliability = 0.9,
            skillTags = listOf("外場", "收銀"),
        )
        database.insert(
            "employees",
            null,
            ContentValues().apply {
                put("id", employee.id)
                put("name", employee.name)
                put("role", employee.role.name)
                put("hourly_rate", employee.hourlyRate)
                put("target_hours", employee.targetHours)
                put("reliability", employee.reliability)
                put("skill_tags", employee.skillTags.joinToString(","))
            },
        )
        return employee
    }
}
