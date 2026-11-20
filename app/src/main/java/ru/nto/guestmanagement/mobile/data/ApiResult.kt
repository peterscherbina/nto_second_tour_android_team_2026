package ru.nto.guestmanagement.mobile.data

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    /** `401`: код неизвестен. На экране входа — обычная ошибка, после входа — конец сессии. */
    data object Unauthorized : ApiResult<Nothing>

    /** `404`: заявка не найдена или чужая. */
    data object NotFound : ApiResult<Nothing>

    /** `409`: [message] — `Duplicate request` или `Invalid status transition`. */
    data class Conflict(val message: String?) : ApiResult<Nothing>

    /** `400 {"errors": {...}}`: ошибки полей. */
    data class FieldErrors(val errors: Map<String, String>) : ApiResult<Nothing>

    /** `400 {"error": "..."}` и прочие неожиданные ответы сервера (5xx и т. п.). */
    data class Failure(val message: String?) : ApiResult<Nothing>

    /** Нет связи: исключение ввода-вывода или сбой клиента. */
    data object Network : ApiResult<Nothing>
}
