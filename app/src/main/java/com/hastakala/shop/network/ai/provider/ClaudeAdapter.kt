package com.hastakala.shop.network.ai.provider

import com.hastakala.shop.network.ai.AiProviderAdapter
import com.hastakala.shop.network.ai.model.AiProvider
import com.hastakala.shop.network.ai.model.AiSettings
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Singleton
class ClaudeAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) : AiProviderAdapter {
    override val provider: AiProvider = AiProvider.CLAUDE

    override suspend fun listModels(settings: AiSettings): List<String> {
        val service = settings.createService<ClaudeService>()
        return service.models(
            apiKey = settings.apiKey,
            anthropicVersion = CLAUDE_VERSION
        ).data.map { it.id }
    }

    override suspend fun sendPrompt(settings: AiSettings, prompt: String): String {
        val service = settings.createService<ClaudeService>()
        val response = service.message(
            apiKey = settings.apiKey,
            anthropicVersion = CLAUDE_VERSION,
            request = ClaudeRequest(
                model = settings.model,
                maxTokens = 500,
                messages = listOf(ClaudeMessage(role = "user", content = prompt))
            )
        )
        return response.content.firstOrNull()?.text.orEmpty()
    }

    private inline fun <reified T> AiSettings.createService(): T =
        Retrofit.Builder()
            .baseUrl(provider.defaultBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(T::class.java)

    private companion object {
        const val CLAUDE_VERSION = "2023-06-01"
    }
}

private interface ClaudeService {
    @GET("models")
    suspend fun models(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String
    ): ClaudeModelsResponse

    @POST("messages")
    suspend fun message(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String,
        @Body request: ClaudeRequest
    ): ClaudeResponse
}

private data class ClaudeModelsResponse(
    val data: List<ClaudeModel>
)

private data class ClaudeModel(
    val id: String
)

private data class ClaudeRequest(
    val model: String,
    @Json(name = "max_tokens") val maxTokens: Int,
    val messages: List<ClaudeMessage>
)

private data class ClaudeMessage(
    val role: String,
    val content: String
)

private data class ClaudeResponse(
    val content: List<ClaudeContent>
)

private data class ClaudeContent(
    val text: String
)
