package android.content.coroutines

import android.database.Cursor

fun interface CursorMapper<T> {
    fun mapCursor(cursor: Cursor): T
}