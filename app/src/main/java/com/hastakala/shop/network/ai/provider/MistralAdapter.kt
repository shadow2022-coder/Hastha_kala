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
class MistralAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) : AiProviderAdapter {
    override val provider: AiProvider = AiProvider.MISTRAL

    override suspend fun listModels(settings: AiSettings): List<String> {
        val service = settings.createService<MistralService>()
        return service.models("Bearer ${settings.apiKey}").data.map { it.id }
    }

    override suspend fun sendPrompt(settings: AiSettings, prompt: String): String {
        val service = settings.createService<MistralService>()
        val response = service.chat(
            authorization = "Bearer ${settings.apiKey}",
            request = MistralChatRequest(
                model = settings.model,
                messages = listOf(MistralMessage(role = "user", content = prompt))
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

private interface MistralService {
    @GET("models")
    suspend fun models(
        @Header("Authorization") authorization: String
    ): MistralModelsResponse

    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: MistralChatRequest
    ): MistralChatResponse
}

private data class MistralModelsResponse(
    val data: List<MistralModelItem>
)

private data class MistralModelItem(
    val id: String
)

private data class MistralChatRequest(
    val model: String,
    val temperature: Double = 0.2,
    val messages: List<MistralMessage>
)

private data class MistralMessage(
    val role: String,
    val content: String
)

private data class MistralChatResponse(
    val choices: List<MistralChoice>
)

private data class MistralChoice(
    val message: MistralMessage
)
