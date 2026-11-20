package ru.nto.guestmanagement.mobile.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.*
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.StatusLabel
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel
import ru.nto.guestmanagement.mobile.ui.refresh.RefreshGate
import ru.nto.guestmanagement.mobile.ui.refresh.ResumeEffect

private data class DetailsState(
    val data: RequestDetails? = null,
    val loading: Boolean = false,
    val cancelling: Boolean = false,
    val error: String? = null,
    val offline: Boolean = false,
    val cancelError: String? = null,
    val sessionExpired: Boolean = false,
)

private class RequestDetailsViewModel(private val container: AppContainer, private val requestId: Long) : ViewModel() {
    var state by mutableStateOf(DetailsState())
        private set
    private val api = RequestsApi(container.httpClient)
    private val gate = RefreshGate()

    fun refresh() {
        if (state.cancelling || state.sessionExpired) return
        gate.launch(viewModelScope) {
            state = state.copy(loading = true, error = null)
            val code = container.session.current()
            if (code == null) {
                expire()
                return@launch
            }
            when (val result = api.get(code, requestId)) {
                is ApiResult.Success -> state = state.copy(data = result.value, loading = false, error = null, offline = false)
                ApiResult.Unauthorized -> expire()
                else -> state = if (state.data == null) {
                    state.copy(loading = false, error = if (result == ApiResult.NotFound) "Заявка не найдена" else "Не удалось загрузить заявку", offline = false)
                } else {
                    state.copy(loading = false, error = null, offline = true)
                }
            }
        }
    }

    fun cancel() {
        if (state.cancelling || state.sessionExpired || state.data?.status != RequestStatus.PENDING) return
        gate.cancel()
        state = state.copy(cancelling = true, loading = false, cancelError = null)
        viewModelScope.launch {
            val code = container.session.current()
            if (code == null) {
                expire()
                return@launch
            }
            when (val result = api.cancel(code, requestId)) {
                is ApiResult.Success -> state = state.copy(data = result.value, cancelling = false, cancelError = null, offline = false)
                ApiResult.Unauthorized -> expire()
                is ApiResult.Conflict -> {
                    state = state.copy(cancelling = false, cancelError = "Заявка уже рассмотрена")
                    refresh()
                }
                else -> state = state.copy(cancelling = false, cancelError = "Не удалось отменить заявку. Проверьте соединение и повторите попытку")
            }
        }
    }

    private suspend fun expire() {
        container.session.clear()
        state = state.copy(loading = false, cancelling = false, sessionExpired = true)
    }
}

@Composable
fun RequestDetailsScreen(
    container: AppContainer,
    requestId: Long,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val model = containerViewModel { RequestDetailsViewModel(container, requestId) }
    val state = model.state
    var confirm by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    ResumeEffect { model.refresh() }
    LaunchedEffect(state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.testTag(TestIds.DETAILS_BACK)) { Text("Назад") }
                Text("Заявка", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = model::refresh, enabled = !state.loading && !state.cancelling, modifier = Modifier.testTag(TestIds.DETAILS_REFRESH)) {
                    Text("Обновить")
                }
            }
            if (state.loading || state.cancelling) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(TestIds.DETAILS_ERROR))
                Button(onClick = model::refresh, modifier = Modifier.testTag(TestIds.DETAILS_RETRY)) { Text("Повторить") }
            }
            if (state.offline) Text("Нет связи", modifier = Modifier.testTag(TestIds.DETAILS_OFFLINE))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.data?.let { data ->
                    DetailField("Номер заявки", data.id.toString(), TestIds.DETAILS_ID)
                    DetailField("ФИО гостя", data.fullName, TestIds.DETAILS_NAME)
                    DetailField("Телефон", data.phone, TestIds.DETAILS_PHONE)
                    data.organization?.takeIf { it.isNotBlank() }?.let { DetailField("Организация", it, TestIds.DETAILS_ORGANIZATION) }
                    DetailField("Цель посещения", data.purpose, TestIds.DETAILS_PURPOSE)
                    DetailField("Начало", formatDateTime(data.validFrom), TestIds.DETAILS_VALID_FROM)
                    DetailField("Окончание", formatDateTime(data.validTo), TestIds.DETAILS_VALID_TO)
                    StatusLabel(data.status, TestIds.DETAILS_STATUS)
                    DetailField("Создана", formatDateTime(data.createdAt), TestIds.DETAILS_CREATED_AT)
                    DetailField("Изменена", formatDateTime(data.updatedAt), TestIds.DETAILS_UPDATED_AT)
                    if (data.status == RequestStatus.REJECTED) DetailField("Комментарий охраны", data.comment.orEmpty(), TestIds.DETAILS_COMMENT)
                }
            }
            state.cancelError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_ERROR))
            }
            if (state.data?.status == RequestStatus.PENDING) {
                Button(
                    onClick = { confirm = true },
                    enabled = !state.cancelling,
                    modifier = Modifier.fillMaxWidth().testTag(TestIds.DETAILS_CANCEL),
                ) { Text("Отменить заявку") }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Отменить заявку?") },
            text = { Text("Заявка перестанет ожидать рассмотрения охраной.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; model.cancel() }, modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_CONFIRM)) {
                    Text("Отменить заявку")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }, modifier = Modifier.testTag(TestIds.DETAILS_CANCEL_DISMISS)) { Text("Назад") }
            },
        )
    }
}

@Composable
private fun DetailField(label: String, value: String, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(tag))
    }
}
