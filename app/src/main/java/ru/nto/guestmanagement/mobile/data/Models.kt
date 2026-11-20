package ru.nto.guestmanagement.mobile.data

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import org.json.JSONObject
import ru.nto.guestmanagement.mobile.R

enum class RequestStatus(@StringRes val label: Int, @ColorRes val color: Int) {
    PENDING(R.string.status_pending, R.color.request_status_pending),
    APPROVED(R.string.status_approved, R.color.request_status_approved),
    REJECTED(R.string.status_rejected, R.color.request_status_rejected),
    CANCELLED(R.string.status_cancelled, R.color.request_status_cancelled),
}

data class Profile(val name: String) {
    companion object {
        fun parse(json: JSONObject) = Profile(json.getString("name"))
    }
}

data class RequestSummary(
    val id: Long,
    val fullName: String,
    val validFrom: String,
    val validTo: String,
    val status: RequestStatus,
) {
    companion object {
        fun parse(json: JSONObject) = RequestSummary(
            id = json.getLong("id"),
            fullName = json.getString("fullName"),
            validFrom = json.getString("validFrom"),
            validTo = json.getString("validTo"),
            status = RequestStatus.valueOf(json.getString("status")),
        )
    }
}

data class RequestDetails(
    val id: Long,
    val fullName: String,
    val phone: String,
    val organization: String? = null,
    val purpose: String,
    val validFrom: String,
    val validTo: String,
    val status: RequestStatus,
    val comment: String? = null,
    val createdAt: String,
    val updatedAt: String,
) {
    companion object {
        fun parse(json: JSONObject) = RequestDetails(
            id = json.getLong("id"),
            fullName = json.getString("fullName"),
            phone = json.getString("phone"),
            organization = json.optStringOrNull("organization"),
            purpose = json.getString("purpose"),
            validFrom = json.getString("validFrom"),
            validTo = json.getString("validTo"),
            status = RequestStatus.valueOf(json.getString("status")),
            comment = json.optStringOrNull("comment"),
            createdAt = json.getString("createdAt"),
            updatedAt = json.getString("updatedAt"),
        )
    }
}

/** Все поля — строки или `null`, как в контракте: ошибки полей отдаёт сервер. */
data class CreateRequestBody(
    val fullName: String?,
    val phone: String?,
    val organization: String?,
    val purpose: String?,
    val validFrom: String?,
    val validTo: String?,
) {
    /** Все ключи присутствуют, `null` уходит как JSON `null` (`JSONObject.put(k, null)` ключ бы удалил). */
    fun toJson(): String = JSONObject().apply {
        put("fullName", fullName ?: JSONObject.NULL)
        put("phone", phone ?: JSONObject.NULL)
        put("organization", organization ?: JSONObject.NULL)
        put("purpose", purpose ?: JSONObject.NULL)
        put("validFrom", validFrom ?: JSONObject.NULL)
        put("validTo", validTo ?: JSONObject.NULL)
    }.toString()
}

internal data class ErrorBody(val error: String? = null, val errors: Map<String, String>? = null) {
    companion object {
        fun parse(json: JSONObject) = ErrorBody(
            error = json.optStringOrNull("error"),
            errors = json.optJSONObject("errors")?.let { o -> o.keys().asSequence().associateWith { o.getString(it) } },
        )
    }
}

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
