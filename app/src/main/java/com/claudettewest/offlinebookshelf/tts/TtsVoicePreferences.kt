package com.claudettewest.offlinebookshelf.tts

import android.content.Context

object TtsVoicePreferences {
    private const val FILE = "tts_preferences"
    private const val VOICE = "selected_voice"
    private const val RATE = "speech_rate"

    fun selectedVoice(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(VOICE, null)

    fun selectVoice(context: Context, voiceName: String?) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().apply {
            if (voiceName == null) remove(VOICE) else putString(VOICE, voiceName)
        }.apply()
    }

    fun speechRate(context: Context): Float =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat(RATE, 1f)

    fun setSpeechRate(context: Context, rate: Float) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putFloat(RATE, rate.coerceIn(0.5f, 2f)).apply()
    }
}
