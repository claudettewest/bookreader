package com.claudettewest.offlinebookshelf.tts

import java.util.Locale

object SpeechLanguageDetector {
    private val hindiIndia = Locale("hi", "IN")

    fun localeFor(text: String, fallback: Locale = Locale.getDefault()): Locale {
        val letters = text.count(Char::isLetter)
        if (letters == 0) return fallback

        val devanagariLetters = text.count { it in '\u0900'..'\u097F' && it.isLetter() }
        return if (devanagariLetters >= 8 && devanagariLetters.toDouble() / letters >= 0.15) {
            hindiIndia
        } else {
            fallback
        }
    }
}
