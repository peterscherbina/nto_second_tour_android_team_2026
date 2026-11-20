package ru.nto.guestmanagement.mobile.data

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

interface TimeProvider {
    /** Текущее время в зоне `Europe/Moscow` с точностью до секунды. Используется для клиентской валидации. */
    fun now(): LocalDateTime
}

object SystemTimeProvider : TimeProvider {
    private val zone = ZoneId.of("Europe/Moscow")

    override fun now(): LocalDateTime = LocalDateTime.now(zone).truncatedTo(ChronoUnit.SECONDS)
}
