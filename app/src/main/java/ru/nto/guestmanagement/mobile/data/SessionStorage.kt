package ru.nto.guestmanagement.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Хранилище кода сотрудника поверх DataStore. Код переживает перезапуск приложения. */
class SessionStorage(private val dataStore: DataStore<Preferences>) {
    val code: Flow<String?> = dataStore.data.map { it[KEY] }

    suspend fun current(): String? = code.first()

    suspend fun save(code: String) {
        dataStore.edit { it[KEY] = code }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("employee_code")
    }
}
