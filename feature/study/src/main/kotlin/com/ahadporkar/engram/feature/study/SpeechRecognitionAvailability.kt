package com.ahadporkar.engram.feature.study

import android.content.Context
import android.speech.SpeechRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/** Whether the device has a speech recogniser for speaking exercises. */
fun interface SpeechRecognitionAvailability {
    fun isAvailable(): Boolean
}

class AndroidSpeechRecognitionAvailability @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechRecognitionAvailability {
    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StudyModule {
    @Binds
    abstract fun bindSpeechRecognitionAvailability(
        impl: AndroidSpeechRecognitionAvailability,
    ): SpeechRecognitionAvailability
}
