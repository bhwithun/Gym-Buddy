package com.gymbuddy

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface GrokApiService {
    @POST("chat/completions")
    fun chatCompletion(
        @Header("Authorization") auth: String,
        @Body request: GrokChatRequest
    ): Call<GrokChatResponse>

    @GET("models")
    fun getModels(
        @Header("Authorization") auth: String
    ): Call<GrokModelsResponse>
}
