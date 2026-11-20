package ru.nto.guestmanagement.mobile

import android.app.Application
import ru.nto.guestmanagement.mobile.di.AppContainer
import ru.nto.guestmanagement.mobile.di.ContainerProvider

class GuestApplication : Application(), ContainerProvider {
    override val container: AppContainer by lazy { AppContainer.create(this) }
}
