package ru.nto.guestmanagement.mobile.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.ktor.client.HttpClient
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import ru.nto.guestmanagement.mobile.Constants
import ru.nto.guestmanagement.mobile.data.SessionStorage
import ru.nto.guestmanagement.mobile.data.SystemTimeProvider
import ru.nto.guestmanagement.mobile.data.TimeProvider

/**
 * Единственная точка подмены зависимостей. Не менять.
 *
 * Боевая сборка — [create]; тесты собирают контейнер сами: `HttpClientEngine` (Ktor CIO, адрес — `Constants.HOST` эталонного сервера),
 * `DataStore<Preferences>` во временном файле и управляемый [TimeProvider].
 *
 * Ktor-клиент уже настроен: базовый адрес — [Constants.HOST], JSON разбирается вручную через `org.json` в `RequestsApi`,
 * `expectSuccess = false` (коды 4xx/5xx приходят обычным ответом, а не исключением).
 */
class AppContainer(
    engine: HttpClientEngine,
    dataStore: DataStore<Preferences>,
    val timeProvider: TimeProvider,
) {
    val httpClient: HttpClient = HttpClient(engine) {
        expectSuccess = false
        defaultRequest {
            url(Constants.HOST)
        }
    }

    val session: SessionStorage = SessionStorage(dataStore)

    companion object {
        // Один DataStore на файл в JVM: два экземпляра на один файл запрещены (при пересоздании Application в тестах).
        private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

        fun create(context: Context): AppContainer {
            val file = DataStorePaths.file(context, DataStorePaths.SESSION_FILE)
            val dataStore = stores.getOrPut(file.absolutePath) {
                PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) { file }
            }
            return AppContainer(CIO.create(), dataStore, SystemTimeProvider)
        }
    }
}

/** Реализуется `Application`; тесты подставляют свою реализацию через `@Config(application = …)`. */
interface ContainerProvider {
    val container: AppContainer
}
