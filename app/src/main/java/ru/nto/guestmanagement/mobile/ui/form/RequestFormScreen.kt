package ru.nto.guestmanagement.mobile.ui.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.nto.guestmanagement.mobile.data.ApiResult
import ru.nto.guestmanagement.mobile.data.RequestsApi
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.ui.TestIds
import ru.nto.guestmanagement.mobile.ui.common.containerViewModel

private data class FormState(
    val values: FormValues = FormValues(),
    val attempted: Boolean = false,
    val loading: Boolean = false,
    val clientErrors: Map<String, String> = emptyMap(),
    val serverErrors: Map<String, String> = emptyMap(),
    val commonError: String? = null,
    val created: Boolean = false,
    val sessionExpired: Boolean = false,
)

private class RequestFormViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf(FormState())
        private set
    private val api = RequestsApi(container.httpClient)

    fun edit(field: String, value: String) {
        if (state.loading) return
        val values = when (field) {
            "fullName" -> state.values.copy(fullName = value)
            "phone" -> state.values.copy(phone = value)
            "organization" -> state.values.copy(organization = value)
            "purpose" -> state.values.copy(purpose = value)
            "date" -> state.values.copy(date = value)
            "validFrom" -> state.values.copy(timeFrom = value)
            "validTo" -> state.values.copy(timeTo = value)
            else -> return
        }
        state = state.copy(
            values = values,
            clientErrors = if (state.attempted) validateForm(values, container.timeProvider.now()).errors else emptyMap(),
            serverErrors = state.serverErrors - field,
            commonError = null,
        )
    }

    fun submit() {
        if (state.loading || state.created || state.sessionExpired) return
        val validation = validateForm(state.values, container.timeProvider.now())
        state = state.copy(attempted = true, clientErrors = validation.errors, serverErrors = emptyMap(), commonError = null)
        val body = validation.body ?: return
        state = state.copy(loading = true)
        viewModelScope.launch {
            val code = container.session.current()
            if (code == null) {
                expire()
                return@launch
            }
            when (val result = api.create(code, body)) {
                is ApiResult.Success -> state = state.copy(loading = false, created = true)
                ApiResult.Unauthorized -> expire()
                is ApiResult.FieldErrors -> state = state.copy(loading = false, serverErrors = result.errors)
                is ApiResult.Conflict -> state = state.copy(loading = false, commonError = "На это время уже есть заявка для указанного телефона")
                else -> state = state.copy(loading = false, commonError = "Не удалось отправить заявку. Проверьте соединение и повторите попытку")
            }
        }
    }

    private suspend fun expire() {
        container.session.clear()
        state = state.copy(loading = false, sessionExpired = true)
    }
}

@Composable
fun RequestFormScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val model = containerViewModel { RequestFormViewModel(container) }
    val state = model.state
    val values = state.values
    val errors = state.clientErrors + state.serverErrors
    BackHandler(onBack = onBack)
    LaunchedEffect(state.created, state.sessionExpired) {
        if (state.sessionExpired) onSessionExpired() else if (state.created) onCreated()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, modifier = Modifier.testTag(TestIds.FORM_BACK)) { Text("Назад") }
                Text("Новая заявка", style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormField("ФИО гостя", values.fullName, "fullName", TestIds.FORM_FULL_NAME, errors, state.loading, model::edit)
                FormField("Телефон (+7 и 10 цифр)", values.phone, "phone", TestIds.FORM_PHONE, errors, state.loading, model::edit, KeyboardType.Phone)
                FormField("Организация (необязательно)", values.organization, "organization", TestIds.FORM_ORGANIZATION, errors, state.loading, model::edit)
                FormField("Цель посещения", values.purpose, "purpose", TestIds.FORM_PURPOSE, errors, state.loading, model::edit)
                FormField("Дата (ДД.ММ.ГГГГ)", values.date, "date", TestIds.FORM_DATE, errors, state.loading, model::edit)
                FormField("Время начала (ЧЧ:ММ)", values.timeFrom, "validFrom", TestIds.FORM_TIME_FROM, errors, state.loading, model::edit)
                FormField("Время окончания (ЧЧ:ММ)", values.timeTo, "validTo", TestIds.FORM_TIME_TO, errors, state.loading, model::edit)
                state.commonError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(TestIds.FORM_ERROR_COMMON))
                }
                if (state.loading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).testTag(TestIds.FORM_LOADING))
                Button(onClick = model::submit, enabled = !state.loading, modifier = Modifier.fillMaxWidth().testTag(TestIds.FORM_SUBMIT)) {
                    Text("Отправить")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    field: String,
    tag: String,
    errors: Map<String, String>,
    loading: Boolean,
    onEdit: (String, String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onEdit(field, it) },
            label = { Text(label) },
            enabled = !loading,
            isError = errors.containsKey(field),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
        errors[field]?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("form_error_$field"))
        }
    }
}
