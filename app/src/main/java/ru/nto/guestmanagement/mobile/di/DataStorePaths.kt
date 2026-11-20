package ru.nto.guestmanagement.mobile.di

import android.content.Context
import java.io.File

/**
 * Не менять. Каталог файлов DataStore. Боевая сборка: `filesDir/datastore`. Проверка входа после перезапуска
 * подставляет в [dir] свой каталог (между двумя JVM переносятся только файлы `*.preferences_pb`).
 */
object DataStorePaths {
    @Volatile var dir: File? = null
    const val SESSION_FILE = "session.preferences_pb"

    fun file(context: Context, name: String): File =
        File(dir ?: File(context.applicationContext.filesDir, "datastore"), name).also { it.parentFile?.mkdirs() }
}
