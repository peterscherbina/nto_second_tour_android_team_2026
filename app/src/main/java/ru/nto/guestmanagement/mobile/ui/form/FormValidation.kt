package ru.nto.guestmanagement.mobile.ui.form

import ru.nto.guestmanagement.mobile.data.CreateRequestBody
import ru.nto.guestmanagement.mobile.data.toApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

internal data class FormValues(
    val fullName: String = "",
    val phone: String = "",
    val organization: String = "",
    val purpose: String = "",
    val date: String = "",
    val timeFrom: String = "",
    val timeTo: String = "",
)

internal data class FormValidationResult(val errors: Map<String, String>, val body: CreateRequestBody?)

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)

internal fun validateForm(values: FormValues, now: LocalDateTime): FormValidationResult {
    val errors = linkedMapOf<String, String>()
    val name = values.fullName.trim()
    val phone = values.phone.trim()
    val organization = values.organization.trim().takeIf { it.isNotEmpty() }
    val purpose = values.purpose.trim()
    if (name.length !in 5..150) errors["fullName"] = "Введите ФИО длиной от 5 до 150 символов"
    if (!phone.matches(Regex("\\+7[0-9]{10}"))) errors["phone"] = "Введите +7 и ровно 10 цифр без пробелов"
    if (organization != null && organization.length > 150) errors["organization"] = "Не более 150 символов"
    if (purpose.length !in 5..200) errors["purpose"] = "Введите цель длиной от 5 до 200 символов"
    val date = parseDate(values.date.trim())
    val start = parseTime(values.timeFrom.trim())
    val end = parseTime(values.timeTo.trim())
    if (date == null) errors["date"] = "Введите существующую дату в формате ДД.ММ.ГГГГ"
    if (start == null) errors["validFrom"] = "Введите время начала в формате ЧЧ:ММ"
    if (end == null) errors["validTo"] = "Введите время окончания в формате ЧЧ:ММ"
    val from = if (date != null && start != null) LocalDateTime.of(date, start) else null
    val to = if (date != null && end != null) LocalDateTime.of(date, end) else null
    if (from != null && from.isBefore(now)) errors["validFrom"] = "Начало не может быть раньше текущего времени"
    if (from != null && to != null && !to.isAfter(from)) errors["validTo"] = "Окончание должно быть позже начала"
    val body = if (errors.isEmpty()) CreateRequestBody(name, phone, organization, purpose, from!!.toApi(), to!!.toApi()) else null
    return FormValidationResult(errors, body)
}

private fun parseDate(value: String): LocalDate? {
    if (!value.matches(Regex("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}"))) return null
    return try {
        LocalDate.parse(value, dateFormat).takeIf { it.year in 1..9999 }
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun parseTime(value: String): LocalTime? {
    if (!value.matches(Regex("[0-9]{2}:[0-9]{2}"))) return null
    return try {
        LocalTime.parse(value, timeFormat)
    } catch (_: DateTimeParseException) {
        null
    }
}
