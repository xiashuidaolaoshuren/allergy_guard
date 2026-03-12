package com.xiashuidaolaoshuren.allergyguard.logic

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

data class TranslationLanguageOption(
    val displayName: String,
    val languageTag: String,
    val flagEmoji: String
)

object TranslationManager {
    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()
    private val languageIdentifier: LanguageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    suspend fun identifyLanguage(text: String): String {
        return try {
            val result = languageIdentifier.identifyLanguage(text).await()
            result
        } catch (e: Exception) {
            TranslateLanguage.ENGLISH // Fallback to safe assumption
        }
    }

    suspend fun translateText(text: String, sourceLang: String): String? {
        if (sourceLang == TranslateLanguage.ENGLISH || sourceLang == "und") {
            return text
        }

        val modelDownloaded = isModelDownloaded(sourceLang).await()
        if (!modelDownloaded) {
            return null // Signal that model is missing
        }

        val translator = createTranslator(sourceLang)
        return try {
            val result = translator.translate(text).await()
            result
        } catch (e: Exception) {
            null
        } finally {
            translator.close()
        }
    }

    fun supportedLanguages(): List<TranslationLanguageOption> {
        return listOf(
            TranslationLanguageOption("English", TranslateLanguage.ENGLISH, "🇬🇧"),
            TranslationLanguageOption("Chinese", TranslateLanguage.CHINESE, "🇨🇳"),
            TranslationLanguageOption("Spanish", TranslateLanguage.SPANISH, "🇪🇸"),
            TranslationLanguageOption("Hindi", TranslateLanguage.HINDI, "🇮🇳"),
            TranslationLanguageOption("Arabic", TranslateLanguage.ARABIC, "🇸🇦"),
            TranslationLanguageOption("Portuguese", TranslateLanguage.PORTUGUESE, "🇵🇹"),
            TranslationLanguageOption("Russian", TranslateLanguage.RUSSIAN, "🇷🇺"),
            TranslationLanguageOption("Japanese", TranslateLanguage.JAPANESE, "🇯🇵"),
            TranslationLanguageOption("French", TranslateLanguage.FRENCH, "🇫🇷"),
            TranslationLanguageOption("German", TranslateLanguage.GERMAN, "🇩🇪"),
            TranslationLanguageOption("Korean", TranslateLanguage.KOREAN, "🇰🇷")
        )
    }

    fun createTranslator(sourceLanguageTag: String, targetLanguageTag: String = TranslateLanguage.ENGLISH): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguageTag)
            .setTargetLanguage(targetLanguageTag)
            .build()
        return Translation.getClient(options)
    }

    fun isModelDownloaded(languageTag: String): Task<Boolean> {
        val model = TranslateRemoteModel.Builder(languageTag).build()
        return remoteModelManager.isModelDownloaded(model)
    }

    fun downloadModel(languageTag: String): Task<Void> {
        val model = TranslateRemoteModel.Builder(languageTag).build()
        val downloadConditions = DownloadConditions.Builder().build()
        return remoteModelManager.download(model, downloadConditions)
    }

    fun deleteModel(languageTag: String): Task<Void> {
        val model = TranslateRemoteModel.Builder(languageTag).build()
        return remoteModelManager.deleteDownloadedModel(model)
    }
}
