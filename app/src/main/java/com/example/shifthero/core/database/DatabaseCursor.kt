package com.example.shifthero.core.database

import android.database.Cursor

internal fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column))

internal fun Cursor.nullableString(column: String): String? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getString(index)
}

internal fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

internal fun Cursor.double(column: String): Double = getDouble(getColumnIndexOrThrow(column))

internal fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
