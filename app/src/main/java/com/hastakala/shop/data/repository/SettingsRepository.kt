package com.hastakala.shop.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hastakala.shop.network.ai.model.AiProvider
import com.hastakala.shop.network.ai.model.AiSettings
import com.hastakala.shop.network.ai.model.AppSettings
import com.hastakala.shop.network.ai.model.ReplyLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "hasta_kala_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val providerKey = stringPreferencesKey("ai_provider")
    private val languageKey = stringPreferencesKey("language_tag")
    private val modelKey = stringPreferencesKey("ai_model")

    private val encryptedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "hasta_kala_secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val apiKeyState = MutableStateFlow(readApiKey())

    val settingsFlow: Flow<AppSettings> = combine(
        context.settingsDataStore.data,
        apiKeyState
    ) { preferences, apiKey ->
        val provider = AiProvider.fromName(preferences[providerKey])
        val languageTag = preferences[languageKey] ?: ReplyLanguage.ENGLISH.tag
        AppSettings(
            languageTag = languageTag,
            aiSettings = AiSettings(
                provider = provider,
                baseUrl = provider.defaultBaseUrl,
                model = preferences[modelKey].orEmpty(),
                apiKey = apiKey,
                language = ReplyLanguage.fromTag(languageTag)
            )
        )
    }

    suspend fun saveLanguage(languageTag: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[languageKey] = languageTag
        }
    }

    suspend fun saveAiSettings(
        provider: AiProvider,
        model: String,
        apiKey: String
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[providerKey] = provider.name
            preferences[modelKey] = model.trim()
        }
        encryptedPreferences.edit()
            .putString(KEY_API, apiKey.trim())
            .apply()
        apiKeyState.value = apiKey.trim()
    }

    suspend fun clearAiApiKey() {
        encryptedPreferences.edit()
            .remove(KEY_API)
            .apply()
        apiKeyState.value = ""
    }

    suspend fun currentSettings(): AppSettings = settingsFlow.first()

    private fun readApiKey(): String = encryptedPreferences.getString(KEY_API, "").orEmpty()

    private companion object {
        const val KEY_API = "ai_api_key"
    }
}
