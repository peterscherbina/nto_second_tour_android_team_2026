package ru.nto.guestmanagement.mobile.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel

private data class LoginState(
    val code: String = "",
    val checking: Boolean = true,
    val loading: Boolean = false,
    val authenticated: Boolean = false,
    val error: String? = null,
)

private class LoginViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf(LoginState())
        private set
    private val api = RequestsApi(container.httpClient)

    init {
        viewModelScope.launch {
            try {
                val saved = container.session.current()
                state = state.copy(checking = false, authenticated = !saved.isNullOrEmpty())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = state.copy(checking = false, error = "Не удалось прочитать сохранённый вход")
            }
        }
    }

    fun edit(value: String) {
        if (!state.loading) state = state.copy(code = value, error = null)
    }

    fun submit() {
        if (state.checking || state.loading || !state.code.matches(Regex("[A-Za-z0-9]{4}"))) return
        val code = state.code
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            when (api.auth(code)) {
                is ApiResult.Success -> {
                    try {
                        container.session.save(code)
                        state = state.copy(loading = false, authenticated = true)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        state = state.copy(loading = false, error = "Не удалось сохранить вход. Повторите попытку")
                    }
                }
                ApiResult.Unauthorized -> state = state.copy(loading = false, error = "Неверный код сотрудника")
                else -> state = state.copy(loading = false, error = "Не удалось войти. Проверьте соединение и повторите попытку")
            }
        }
    }
}

@Composable
fun LoginScreen(container: AppContainer, sessionExpired: Boolean, onLoggedIn: () -> Unit) {
    val model = containerViewModel { LoginViewModel(container) }
    val state = model.state
    LaunchedEffect(state.authenticated) {
        if (state.authenticated) onLoggedIn()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Заявки на пропуск", style = MaterialTheme.typography.headlineSmall)
            Text("Введите код сотрудника", style = MaterialTheme.typography.bodyLarge)
            if (sessionExpired) Text("Сессия завершена", modifier = Modifier.testTag(TestIds.AUTH_SESSION_EXPIRED))
            OutlinedTextField(
                value = state.code,
                onValueChange = model::edit,
                label = { Text("Код сотрудника") },
                singleLine = true,
                enabled = !state.loading && !state.checking,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth().testTag(TestIds.AUTH_CODE),
            )
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(TestIds.AUTH_ERROR))
            }
            if (state.loading || state.checking) CircularProgressIndicator(modifier = Modifier.testTag(TestIds.AUTH_LOADING))
            Button(
                onClick = model::submit,
                enabled = !state.checking && !state.loading && state.code.matches(Regex("[A-Za-z0-9]{4}")),
                modifier = Modifier.fillMaxWidth().testTag(TestIds.AUTH_SUBMIT),
            ) { Text("Войти") }
        }
    }
}
