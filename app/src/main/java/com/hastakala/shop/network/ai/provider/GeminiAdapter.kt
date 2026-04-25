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
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Singleton
class GeminiAdapter @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) : AiProviderAdapter {
    override val provider: AiProvider = AiProvider.GEMINI

    override suspend fun listModels(settings: AiSettings): List<String> {
        val service = settings.createService<GeminiService>()
        return service.models(settings.apiKey).models.map { it.name.substringAfter("models/") }
    }

    override suspend fun sendPrompt(settings: AiSettings, prompt: String): String {
        val service = settings.createService<GeminiService>()
        val response = service.generate(
            model = settings.model,
            apiKey = settings.apiKey,
            request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                )
            )
        )
        return response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.joinToString(separator = "\n") { it.text.orEmpty() }
            .orEmpty()
    }

    private inline fun <reified T> AiSettings.createService(): T =
        Retrofit.Builder()
            .baseUrl(provider.defaultBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()
            .create(T::class.java)
}

private interface GeminiService {
    @GET("models")
    suspend fun models(
        @Query("key") apiKey: String
    ): GeminiModelsResponse

    @POST("models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

private data class GeminiModelsResponse(
    val models: List<GeminiModel>
)

private data class GeminiModel(
    val name: String
)

private data class GeminiRequest(
    val contents: List<GeminiContent>
)

private data class GeminiContent(
    val parts: List<GeminiPart>
)

private data class GeminiPart(
    val text: String? = null
)

private data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

private data class GeminiCandidate(
    val content: GeminiContent
)
