package ru.nto.guestmanagement.mobile.ui.refresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun ResumeEffect(onResume: () -> Unit) {
    val callback by rememberUpdatedState(onResume)
    LifecycleResumeEffect(Unit) {
        callback()
        onPauseOrDispose { }
    }
}
