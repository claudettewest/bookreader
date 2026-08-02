package com.claudettewest.offlinebookshelf.tts

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsPlaybackService : Service(), TextToSpeech.OnInitListener {
    private var engine: TextToSpeech? = null; private var pendingText = ""; private var engineReady = false
    override fun onCreate() { super.onCreate(); engine = TextToSpeech(this, this); createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            engine?.stop(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            return START_NOT_STICKY
        }
        pendingText = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        startForeground(NOTIFICATION_ID, notification("Reading aloud")); speak()
        return START_NOT_STICKY
    }
    override fun onInit(status: Int) {
        engineReady = status == TextToSpeech.SUCCESS
        if (engineReady) speak()
    }
    private fun speak() {
        if (!engineReady || pendingText.isBlank()) return
        val requestedLocale = SpeechLanguageDetector.localeFor(pendingText)
        val availability = engine?.isLanguageAvailable(requestedLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        val selectedLocale = if (availability >= TextToSpeech.LANG_AVAILABLE) requestedLocale else Locale.getDefault()
        engine?.language = selectedLocale
        val preferredVoice = TtsVoicePreferences.selectedVoice(this)
            ?.let { name -> engine?.voices?.firstOrNull { it.name == name } }
        if (preferredVoice != null) engine?.voice = preferredVoice
        engine?.setSpeechRate(TtsVoicePreferences.speechRate(this))
        val voiceLabel = preferredVoice?.locale?.displayLanguage ?: selectedLocale.displayLanguage
        startForeground(NOTIFICATION_ID, notification("Reading aloud in $voiceLabel"))
        val chunks = pendingText.replace('\u0000', ' ').chunked(MAX_UTTERANCE_LENGTH)
        chunks.forEachIndexed { index, text ->
            engine?.speak(text, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "book-playback-$index")
        }
    }
    private fun notification(text:String): Notification = Notification.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Read Aloud Library").setContentText(text).setOngoing(true).build()
    private fun createChannel() { getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,"Read aloud",NotificationManager.IMPORTANCE_LOW)) }
    override fun onDestroy() { engine?.stop(); engine?.shutdown(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val EXTRA_TEXT="text"; const val EXTRA_BOOK_ID="bookId"; const val ACTION_STOP="com.claudettewest.offlinebookshelf.STOP_TTS"; private const val CHANNEL_ID="tts_playback"; private const val NOTIFICATION_ID=41; private const val MAX_UTTERANCE_LENGTH=3_500 }
}
