package com.example.shifthero.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.shifthero.core.network.ApiConfig

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE settings (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE user_session (
                id INTEGER PRIMARY KEY NOT NULL,
                user_id TEXT NOT NULL,
                public_id TEXT NOT NULL,
                name TEXT NOT NULL,
                display_name TEXT NOT NULL,
                email TEXT NOT NULL,
                access_token TEXT NOT NULL,
                refresh_token TEXT NOT NULL,
                csrf_token TEXT NOT NULL,
                access_token_expires_at_epoch_ms INTEGER NOT NULL,
                refresh_token_expires_at_epoch_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE company_join_requests (
                id TEXT PRIMARY KEY NOT NULL,
                company_id TEXT NOT NULL,
                company_name TEXT NOT NULL,
                requester_user_id TEXT NOT NULL,
                requester_name TEXT NOT NULL,
                requester_email TEXT NOT NULL,
                requested_role TEXT NOT NULL,
                note TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL,
                reviewed_by_user_id TEXT
            )
            """.trimIndent()
        )
        insertDefaultSettings(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS company_join_requests")
        db.execSQL("DROP TABLE IF EXISTS user_session")
        db.execSQL("DROP TABLE IF EXISTS settings")
        onCreate(db)
    }

    private fun insertDefaultSettings(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO settings(key, value) VALUES('store_name', 'ShiftHero')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('selected_company_id', '')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('api_base_url', '${ApiConfig.DEFAULT_API_BASE_URL}')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('base_hourly_rate', '190')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('holiday_multiplier', '1.34')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('night_multiplier', '1.20')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('overtime_multiplier', '1.34')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('monthly_target_income', '22000')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('schedule_published', 'false')")
        db.execSQL("INSERT INTO settings(key, value) VALUES('theme_mode', 'System')")
    }

    private companion object {
        const val DATABASE_NAME = "shifthero.db"
        const val DATABASE_VERSION = 5
    }
}
