package ru.nto.guestmanagement.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.nto.guestmanagement.mobile.data.RequestStatus
import ru.nto.guestmanagement.mobile.ui.statusColor

/** Номер записи в стеке навигации: ViewModel принадлежит конкретному экрану, а не классу (выставляет `AppNavigation`). */
val LocalScreenKey = compositionLocalOf { "" }

@Composable
inline fun <reified VM : ViewModel> containerViewModel(crossinline create: () -> VM): VM =
    viewModel(key = LocalScreenKey.current, factory = viewModelFactory { initializer { create() } })

/** Метка статуса: всегда текст + цвет; цвет сообщается тестам через `Modifier.statusColor`. */
@Composable
fun StatusLabel(status: RequestStatus, tag: String, modifier: Modifier = Modifier) {
    val color = colorResource(status.color)
    Text(
        text = stringResource(status.label),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .testTag(tag)
            .statusColor(color)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
