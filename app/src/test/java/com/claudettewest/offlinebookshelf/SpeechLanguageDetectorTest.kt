package com.claudettewest.offlinebookshelf

import com.claudettewest.offlinebookshelf.tts.SpeechLanguageDetector
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SpeechLanguageDetectorTest {
    @Test fun `selects Hindi for Devanagari book text`() {
        val text = "रश्मिरथी रामधारी सिंह दिनकर की प्रसिद्ध हिंदी काव्य रचना है।"
        assertEquals("hi-IN", SpeechLanguageDetector.localeFor(text, Locale.US).toLanguageTag())
    }

    @Test fun `keeps fallback language for English text`() {
        assertEquals(Locale.US, SpeechLanguageDetector.localeFor("This is an English language book.", Locale.US))
    }
}
