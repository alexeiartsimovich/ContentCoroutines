package android.content.coroutines

import android.database.Cursor

fun interface CursorMapper<T> {
    /**
     * Maps [cursor] to a model of type [T].
     */
    fun mapCursor(cursor: Cursor): T
}