package com.xiashuidaolaoshuren.allergyguard.logic

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

data class TranslationLanguageOption(
    val displayName: String,
    val languageTag: String
)

object TranslationManager {
    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()

    fun supportedLanguages(): List<TranslationLanguageOption> {
        return listOf(
            TranslationLanguageOption("Chinese", TranslateLanguage.CHINESE),
            TranslationLanguageOption("Japanese", TranslateLanguage.JAPANESE),
            TranslationLanguageOption("Korean", TranslateLanguage.KOREAN)
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
