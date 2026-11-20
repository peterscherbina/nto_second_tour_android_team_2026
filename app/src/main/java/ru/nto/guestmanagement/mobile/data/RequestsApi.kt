package ru.nto.guestmanagement.mobile.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException

/** Вызовы пользовательских методов контракта (`team-contract/openapi.yaml`). Служебный метод охраны клиенту не нужен. */
class RequestsApi(private val client: HttpClient) {

    suspend fun auth(code: String): ApiResult<Profile> = call(::parseProfile) { client.get("api/$code/auth") }

    /** [status] = `null` — все статусы, параметр в запрос не добавляется. */
    suspend fun list(code: String, status: RequestStatus?): ApiResult<List<RequestSummary>> = call(::parseList) {
        client.get("api/$code/requests") {
            if (status != null) parameter("status", status.name)
        }
    }

    suspend fun get(code: String, id: Long): ApiResult<RequestDetails> =
        call(::parseDetails) { client.get("api/$code/requests/$id") }

    suspend fun create(code: String, body: CreateRequestBody): ApiResult<RequestDetails> = call(::parseDetails) {
        client.post("api/$code/requests") {
            contentType(ContentType.Application.Json)
            setBody(body.toJson())
        }
    }

    suspend fun cancel(code: String, id: Long): ApiResult<RequestDetails> =
        call(::parseDetails) { client.post("api/$code/requests/$id/cancel") }

    private fun parseProfile(text: String) = Profile.parse(JSONObject(text))
    private fun parseDetails(text: String) = RequestDetails.parse(JSONObject(text))
    private fun parseList(text: String): List<RequestSummary> =
        JSONArray(text).let { a -> List(a.length()) { RequestSummary.parse(a.getJSONObject(it)) } }

    private suspend fun <T> call(parse: (String) -> T, block: suspend () -> HttpResponse): ApiResult<T> = try {
        val response = block()
        when (response.status.value) {
            200, 201 -> ApiResult.Success(parse(response.bodyAsText()))
            401 -> ApiResult.Unauthorized
            404 -> ApiResult.NotFound
            409 -> ApiResult.Conflict(errorBody(response)?.error)
            400 -> {
                val body = errorBody(response)
                if (body?.errors != null) ApiResult.FieldErrors(body.errors) else ApiResult.Failure(body?.error)
            }
            else -> ApiResult.Failure("HTTP ${response.status.value}")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.Network
    }

    private suspend fun errorBody(response: HttpResponse): ErrorBody? =
        try {
            ErrorBody.parse(JSONObject(response.bodyAsText()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

}
