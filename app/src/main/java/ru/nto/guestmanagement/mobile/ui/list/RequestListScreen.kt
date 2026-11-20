package ru.nto.guestmanagement.mobile.ui.list

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
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

private data class ListState(
    val filter: RequestStatus? = null,
    val items: List<RequestSummary>? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val offline: Boolean = false,
    val loggedOut: Boolean = false,
    val sessionExpired: Boolean = false,
)

private class RequestListViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf(ListState())
        private set
    private val api = RequestsApi(container.httpClient)
    private val gate = RefreshGate()
    private var leaving = false

    fun select(filter: RequestStatus?) {
        if (leaving || state.filter == filter) return
        gate.cancel()
        state = ListState(filter = filter)
        refresh()
    }

    fun refresh() {
        if (leaving) return
        gate.launch(viewModelScope) {
            state = state.copy(loading = true, error = null)
            val code = container.session.current()
            if (code == null) {
                expire()
                return@launch
            }
            when (val result = api.list(code, state.filter)) {
                is ApiResult.Success -> state = state.copy(items = result.value, loading = false, error = null, offline = false)
                ApiResult.Unauthorized -> expire()
                else -> state = if (state.items == null) {
                    state.copy(loading = false, error = "Не удалось загрузить заявки", offline = false)
                } else {
                    state.copy(loading = false, error = null, offline = true)
                }
            }
        }
    }

    fun logout() {
        if (leaving) return
        leaving = true
        gate.cancel()
        viewModelScope.launch {
            container.session.clear()
            state = state.copy(loggedOut = true)
        }
    }

    private suspend fun expire() {
        if (leaving) return
        leaving = true
        container.session.clear()
        state = state.copy(sessionExpired = true)
    }
}

@Composable
fun RequestListScreen(
    container: AppContainer,
    onOpenDetails: (Long) -> Unit,
    onAddRequest: () -> Unit,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val model = containerViewModel { RequestListViewModel(container) }
    val state = model.state
    ResumeEffect { model.refresh() }
    LaunchedEffect(state.loggedOut, state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired() else if (state.loggedOut) onLogout()
    }
    val filters = listOf(
        Triple(null, "Все", TestIds.REQUESTS_FILTER_ALL),
        Triple(RequestStatus.PENDING, "На рассмотрении", TestIds.REQUESTS_FILTER_PENDING),
        Triple(RequestStatus.APPROVED, "Одобрены", TestIds.REQUESTS_FILTER_APPROVED),
        Triple(RequestStatus.REJECTED, "Отклонены", TestIds.REQUESTS_FILTER_REJECTED),
        Triple(RequestStatus.CANCELLED, "Отменены", TestIds.REQUESTS_FILTER_CANCELLED),
    )
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Мои заявки", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = model::logout, modifier = Modifier.testTag(TestIds.REQUESTS_LOGOUT)) {
                    Text("Выйти")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.forEach { (filter, label, tag) ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { model.select(filter) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag(tag),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = model::refresh,
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f).testTag(TestIds.REQUESTS_REFRESH),
                ) { Text("Обновить") }
                Button(onClick = onAddRequest, modifier = Modifier.weight(1f).testTag(TestIds.REQUESTS_ADD)) {
                    Text("Новая заявка")
                }
            }
            if (state.loading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(TestIds.REQUESTS_ERROR))
            }
            if (state.offline) Text("Нет связи", modifier = Modifier.testTag(TestIds.REQUESTS_OFFLINE))
            if (state.items?.isEmpty() == true) Text("Заявок нет", modifier = Modifier.testTag(TestIds.REQUESTS_EMPTY))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.items.orEmpty(), key = { _, item -> item.id }) { index, request ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag(TestIds.requestsPos(index))
                            .pointerInput(request.id) { detectTapGestures { onOpenDetails(request.id) } }
                            .semantics { onClick { onOpenDetails(request.id); true } },
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(request.fullName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag(TestIds.REQUEST_NAME))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatDate(request.validFrom), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag(TestIds.REQUEST_DATE))
                                Text(formatInterval(request.validFrom, request.validTo), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag(TestIds.REQUEST_TIME))
                            }
                            StatusLabel(request.status, TestIds.REQUEST_STATUS)
                        }
                    }
                }
            }
        }
    }
}
