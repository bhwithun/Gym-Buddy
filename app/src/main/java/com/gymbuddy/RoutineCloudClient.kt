package com.gymbuddy

import android.content.Context
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object RoutineCloudClient {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    data class Version(
        val id: String,
        val name: String,
        val updatedAt: String
    )

    data class Record(
        val id: String,
        val name: String,
        val updatedAt: String,
        val days: List<ExportRoutineDay>
    )

    private data class ListResponse(val routines: List<Version>?)
    private data class SaveBody(
        val name: String,
        val days: List<ExportRoutineDay>,
        val overwriteByName: Boolean
    )

    fun list(context: Context): Result<List<Version>> {
        return request(context, "/routines") { body ->
            val parsed = RoutineExport.gson().fromJson(body, ListResponse::class.java)
            parsed.routines.orEmpty()
        }
    }

    fun get(context: Context, id: String): Result<Record> {
        return request(context, "/routines/${id}") { body ->
            parseRecord(body)
        }
    }

    fun save(context: Context, name: String, days: List<ExportRoutineDay>): Result<Record> {
        val payload = RoutineExport.gson().toJson(SaveBody(name, days, overwriteByName = true))
        return request(context, "/routines", "POST", payload) { body ->
            parseRecord(body)
        }
    }

    private fun parseRecord(body: String): Record {
        val gson = RoutineExport.gson()
        val type = object : TypeToken<Record>() {}.type
        return gson.fromJson(body, type)
    }

    private fun <T> request(
        context: Context,
        path: String,
        method: String = "GET",
        jsonBody: String? = null,
        parse: (String) -> T
    ): Result<T> {
        val base = WorkerRemote.getUrl(context)
            ?: return Result.failure(IllegalStateException("No worker configured"))
        val builder = Request.Builder().url("$base$path")
        if (jsonBody != null) {
            builder.method(method, jsonBody.toRequestBody(jsonType))
            builder.header("Content-Type", "application/json")
        } else {
            builder.get()
        }
        val token = WorkerRemote.getToken(context)
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return try {
            http.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = errorMessage(body, response.code)
                    Result.failure(IllegalStateException(message))
                } else {
                    Result.success(parse(body))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun errorMessage(body: String, code: Int): String {
        return try {
            val err = RoutineExport.gson().fromJson(body, ErrorBody::class.java)
            err.error?.takeIf { it.isNotBlank() } ?: "Worker returned $code"
        } catch (_: Exception) {
            "Worker returned $code"
        }
    }

    private data class ErrorBody(val error: String?)
}
