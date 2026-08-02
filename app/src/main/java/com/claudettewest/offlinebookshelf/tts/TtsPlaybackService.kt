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
    private var engine: TextToSpeech? = null; private var pendingText = ""
    override fun onCreate() { super.onCreate(); engine = TextToSpeech(this, this); createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pendingText = intent?.getStringExtra(EXTRA_TEXT).orEmpty()
        startForeground(NOTIFICATION_ID, notification("Reading aloud")); speak()
        return START_NOT_STICKY
    }
    override fun onInit(status: Int) { if(status == TextToSpeech.SUCCESS) { engine?.language = Locale.getDefault(); speak() } }
    private fun speak() { if(pendingText.isNotBlank()) engine?.speak(pendingText, TextToSpeech.QUEUE_FLUSH, null, "book-playback") }
    private fun notification(text:String): Notification = Notification.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Offline Bookshelf").setContentText(text).setOngoing(true).build()
    private fun createChannel() { getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID,"Read aloud",NotificationManager.IMPORTANCE_LOW)) }
    override fun onDestroy() { engine?.stop(); engine?.shutdown(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val EXTRA_TEXT="text"; const val EXTRA_BOOK_ID="bookId"; private const val CHANNEL_ID="tts_playback"; private const val NOTIFICATION_ID=41 }
}
