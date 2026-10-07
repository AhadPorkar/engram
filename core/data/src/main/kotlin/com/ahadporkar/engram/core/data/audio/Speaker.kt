package com.ahadporkar.engram.core.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Pronunciation via the device's text-to-speech engine (dual coding: see it *and* hear it). */
interface Speaker {
    val isReady: StateFlow<Boolean>

    fun canSpeak(languageTag: String): Boolean

    fun speak(text: String, languageTag: String, slow: Boolean = false)

    fun stop()
}

@Singleton
class TextToSpeechSpeaker @Inject constructor(
    @ApplicationContext context: Context,
) : Speaker {

    private val ready = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = ready.asStateFlow()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready.value = status == TextToSpeech.SUCCESS
    }

    override fun canSpeak(languageTag: String): Boolean {
        if (!ready.value) return false
        val result = runCatching { tts.isLanguageAvailable(Locale.forLanguageTag(languageTag)) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        return result >= TextToSpeech.LANG_AVAILABLE
    }

    override fun speak(text: String, languageTag: String, slow: Boolean) {
        if (!ready.value || text.isBlank()) return
        val result = tts.setLanguage(Locale.forLanguageTag(languageTag))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
        tts.setSpeechRate(if (slow) SLOW_RATE else NORMAL_RATE)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        if (ready.value) tts.stop()
    }

    private companion object {
        const val SLOW_RATE = 0.6f
        const val NORMAL_RATE = 1.0f
        const val UTTERANCE_ID = "engram-utterance"
    }
}
