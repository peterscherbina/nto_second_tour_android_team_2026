package ru.nto.guestmanagement.mobile.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/** Свойство семантики, через которое метка статуса сообщает тестам свой цвет. */
val StatusColorKey = SemanticsPropertyKey<Color>("StatusColor")
private var SemanticsPropertyReceiver.statusColorValue by StatusColorKey

/** Только публикует цвет для тестов; сама окраска элемента остаётся на совести вызывающего кода. */
fun Modifier.statusColor(color: Color): Modifier = semantics { statusColorValue = color }
