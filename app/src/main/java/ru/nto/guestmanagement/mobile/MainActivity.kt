package ru.nto.guestmanagement.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import ru.nto.guestmanagement.mobile.di.ContainerProvider
import ru.nto.guestmanagement.mobile.ui.navigation.AppNavigation
import ru.nto.guestmanagement.mobile.ui.theme.GuestManagementTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge: окно не уменьшается под клавиатуру, IME-insets доставляются в Compose (imePadding).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val container = (application as ContainerProvider).container
        setContent {
            GuestManagementTheme {
                AppNavigation(container)
            }
        }
    }
}
