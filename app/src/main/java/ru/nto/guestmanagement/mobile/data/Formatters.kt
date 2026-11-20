package ru.nto.guestmanagement.mobile.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val apiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT)
private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
private val dateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT)

/** Формат контракта: секунды выводятся всегда (ISO_LOCAL_DATE_TIME опустил бы нулевые). */
fun LocalDateTime.toApi(): String = format(apiFormat)

/** Терпимый разбор ответа сервера: ISO принимает и `10:00`, и `10:00:00`, и доли секунды. */
fun parseApi(value: String): LocalDateTime = LocalDateTime.parse(value)

fun formatDate(apiValue: String): String = parseApi(apiValue).format(dateFormat)

/** Интервал с коротким тире U+2013 без пробелов: `10:00–16:00`. */
fun formatInterval(fromApi: String, toApi: String): String =
    parseApi(fromApi).format(timeFormat) + "–" + parseApi(toApi).format(timeFormat)

fun formatDateTime(apiValue: String): String = parseApi(apiValue).format(dateTimeFormat)
