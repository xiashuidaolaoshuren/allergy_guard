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
import java.util.Locale

data class TranslationLanguageOption(
    val displayName: String,
    val languageTag: String,
    val flagEmoji: String
)

object TranslationManager {
    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()
    private val supportedTranslateLanguages: Set<String> = TranslateLanguage.getAllLanguages().toSet()
    private val languageIdentifier: LanguageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    suspend fun identifyLanguage(text: String): String {
        return try {
            val result = languageIdentifier.identifyLanguage(text).await()
            normalizeToTranslateLanguage(result) ?: "und"
        } catch (e: Exception) {
            TranslateLanguage.ENGLISH // Fallback to safe assumption
        }
    }

    suspend fun translateText(text: String, sourceLang: String): String? {
        val normalizedSourceLang = normalizeToTranslateLanguage(sourceLang)
        if (normalizedSourceLang == null || normalizedSourceLang == TranslateLanguage.ENGLISH || sourceLang == "und") {
            return text
        }

        val modelDownloaded = isModelDownloaded(normalizedSourceLang).await()
        if (!modelDownloaded) {
            return null // Signal that model is missing
        }

        val translator = createTranslator(normalizedSourceLang)
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
        val normalizedSourceLanguageTag = normalizeToTranslateLanguage(sourceLanguageTag)
            ?: TranslateLanguage.ENGLISH
        val normalizedTargetLanguageTag = normalizeToTranslateLanguage(targetLanguageTag)
            ?: TranslateLanguage.ENGLISH
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(normalizedSourceLanguageTag)
            .setTargetLanguage(normalizedTargetLanguageTag)
            .build()
        return Translation.getClient(options)
    }

    fun isModelDownloaded(languageTag: String): Task<Boolean> {
        val normalizedLanguageTag = normalizeToTranslateLanguage(languageTag)
            ?: return Tasks.forResult(false)
        val model = TranslateRemoteModel.Builder(normalizedLanguageTag).build()
        return remoteModelManager.isModelDownloaded(model)
    }

    fun downloadModel(languageTag: String): Task<Void> {
        val normalizedLanguageTag = normalizeToTranslateLanguage(languageTag)
            ?: return Tasks.forResult(null)
        val model = TranslateRemoteModel.Builder(normalizedLanguageTag).build()
        val downloadConditions = DownloadConditions.Builder().build()
        return remoteModelManager.download(model, downloadConditions)
    }

    fun deleteModel(languageTag: String): Task<Void> {
        val normalizedLanguageTag = normalizeToTranslateLanguage(languageTag)
            ?: return Tasks.forResult(null)
        val model = TranslateRemoteModel.Builder(normalizedLanguageTag).build()
        return remoteModelManager.deleteDownloadedModel(model)
    }

    private fun normalizeToTranslateLanguage(languageTag: String): String? {
        val baseLanguageCode = languageTag
            .trim()
            .substringBefore('-')
            .lowercase(Locale.ROOT)

        return if (supportedTranslateLanguages.contains(baseLanguageCode)) {
            baseLanguageCode
        } else {
            null
        }
    }
}
