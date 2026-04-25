package com.hastakala.shop.network.ai.provider

import com.hastakala.shop.network.ai.AiProviderAdapter
import com.hastakala.shop.network.ai.model.AiProvider
import com.hastakala.shop.network.ai.model.AiSettings
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
class OpenAiAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) : AiProviderAdapter {
    override val provider: AiProvider = AiProvider.OPENAI

    override suspend fun listModels(settings: AiSettings): List<String> {
        val service = settings.createService<OpenAiService>()
        return service.models("Bearer ${settings.apiKey}").data.map { it.id }
    }

    override suspend fun sendPrompt(settings: AiSettings, prompt: String): String {
        val service = settings.createService<OpenAiService>()
        val response = service.chat(
            authorization = "Bearer ${settings.apiKey}",
            request = OpenAiChatRequest(
                model = settings.model,
                messages = listOf(OpenAiMessage(role = "user", content = prompt))
            )
        )
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    private inline fun <reified T> AiSettings.createService(): T =
        Retrofit.Builder()
            .baseUrl(provider.defaultBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(T::class.java)
}

private interface OpenAiService {
    @GET("models")
    suspend fun models(
        @Header("Authorization") authorization: String
    ): OpenAiModelsResponse

    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

private data class OpenAiModelsResponse(
    val data: List<OpenAiModelItem>
)

private data class OpenAiModelItem(
    val id: String
)

private data class OpenAiChatRequest(
    val model: String,
    val temperature: Double = 0.2,
    val messages: List<OpenAiMessage>
)

private data class OpenAiMessage(
    val role: String,
    val content: String
)

private data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>
)

private data class OpenAiChoice(
    val message: OpenAiMessage
)
