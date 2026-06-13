package com.example.shifthero.core.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.shifthero.core.database.long
import com.example.shifthero.core.database.nullableString
import com.example.shifthero.core.database.string

data class UserSession(
    val id: Long = 1L,
    val userId: String,
    val publicId: String,
    val name: String,
    val displayName: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val csrfToken: String,
    val accessTokenExpiresAtEpochMs: Long,
    val refreshTokenExpiresAtEpochMs: Long,
)

interface UserSessionRepository {
    fun getSession(): UserSession?
    fun upsertSession(session: UserSession)
    fun clearSession()
}

class SqliteUserSessionRepository(
    private val database: SQLiteDatabase,
) : UserSessionRepository {
    override fun getSession(): UserSession? {
        return database.query("user_session", null, "id = ?", arrayOf("1"), null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            UserSession(
                id = cursor.long("id"),
                userId = cursor.string("user_id"),
                publicId = cursor.string("public_id"),
                name = cursor.string("name"),
                displayName = cursor.string("display_name"),
                email = cursor.string("email"),
                accessToken = cursor.string("access_token"),
                refreshToken = cursor.string("refresh_token"),
                csrfToken = cursor.nullableString("csrf_token") ?: "",
                accessTokenExpiresAtEpochMs = cursor.long("access_token_expires_at_epoch_ms"),
                refreshTokenExpiresAtEpochMs = cursor.long("refresh_token_expires_at_epoch_ms"),
            )
        }
    }

    override fun upsertSession(session: UserSession) {
        database.insertWithOnConflict(
            "user_session",
            null,
            ContentValues().apply {
                put("id", 1L)
                put("user_id", session.userId)
                put("public_id", session.publicId)
                put("name", session.name)
                put("display_name", session.displayName)
                put("email", session.email)
                put("access_token", session.accessToken)
                put("refresh_token", session.refreshToken)
                put("csrf_token", session.csrfToken)
                put("access_token_expires_at_epoch_ms", session.accessTokenExpiresAtEpochMs)
                put("refresh_token_expires_at_epoch_ms", session.refreshTokenExpiresAtEpochMs)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun clearSession() {
        database.delete("user_session", null, null)
    }
}
