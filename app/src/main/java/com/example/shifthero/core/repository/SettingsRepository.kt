package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.string
import com.example.shifthero.core.model.AppThemeMode
import com.example.shifthero.core.model.WageRule
import com.example.shifthero.core.network.ApiConfig

data class StoreSettings(
    val storeName: String,
    val selectedCompanyId: String,
    val apiBaseUrl: String,
    val wageRule: WageRule,
    val themeMode: AppThemeMode,
)

interface SettingsRepository {
    fun getSettings(): StoreSettings
    fun setSelectedCompanyId(companyId: String)
    fun setApiBaseUrl(baseUrl: String)
    fun setThemeMode(themeMode: AppThemeMode)
    fun resetDefaults()
}

class SqliteSettingsRepository(
    private val database: SQLiteDatabase,
) : SettingsRepository {
    override fun getSettings(): StoreSettings {
        val settings = database.query("settings", arrayOf("key", "value"), null, null, null, null, null).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) put(cursor.string("key"), cursor.string("value"))
            }
        }
        return StoreSettings(
            storeName = settings["store_name"] ?: "ShiftHero",
            selectedCompanyId = settings["selected_company_id"].orEmpty(),
            apiBaseUrl = ApiConfig.normalizeBaseUrl(settings["api_base_url"] ?: ApiConfig.DEFAULT_API_BASE_URL),
            wageRule = WageRule(
                baseHourlyRate = settings["base_hourly_rate"]?.toIntOrNull() ?: 190,
                holidayMultiplier = settings["holiday_multiplier"]?.toDoubleOrNull() ?: 1.34,
                nightMultiplier = settings["night_multiplier"]?.toDoubleOrNull() ?: 1.20,
                overtimeMultiplier = settings["overtime_multiplier"]?.toDoubleOrNull() ?: 1.34,
                monthlyTargetIncome = settings["monthly_target_income"]?.toIntOrNull() ?: 22000,
            ),
            themeMode = settings["theme_mode"]
                ?.let { raw ->
                    AppThemeMode.entries.firstOrNull { mode -> mode.name.equals(raw, ignoreCase = true) }
                }
                ?: AppThemeMode.System,
        )
    }

    override fun setSelectedCompanyId(companyId: String) {
        setValue("selected_company_id", companyId)
    }

    override fun setApiBaseUrl(baseUrl: String) {
        setValue("api_base_url", ApiConfig.normalizeBaseUrl(baseUrl))
    }

    override fun setThemeMode(themeMode: AppThemeMode) {
        setValue("theme_mode", themeMode.name)
    }

    override fun resetDefaults() {
        setValue("store_name", "ShiftHero")
        setValue("selected_company_id", "")
        setValue("api_base_url", ApiConfig.DEFAULT_API_BASE_URL)
        setValue("base_hourly_rate", "190")
        setValue("holiday_multiplier", "1.34")
        setValue("night_multiplier", "1.20")
        setValue("overtime_multiplier", "1.34")
        setValue("monthly_target_income", "22000")
        setValue("theme_mode", AppThemeMode.System.name)
    }

    private fun setValue(key: String, value: String) {
        database.insertWithOnConflict(
            "settings",
            null,
            ContentValues().apply {
                put("key", key)
                put("value", value)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

}
