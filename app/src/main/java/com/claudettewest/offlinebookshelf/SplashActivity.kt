package com.claudettewest.offlinebookshelf

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import kotlinx.coroutines.runBlocking

class SplashActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var minimumTimeElapsed = false
    private var libraryLoaded = false
    private var transitionStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FrameLayout(this)
        container.addView(ImageView(this).apply {
            setImageResource(R.drawable.splash_screen)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Read Aloud Library"
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        container.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.rgb(189, 169, 164))
            contentDescription = "Loading your library"
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5)).apply {
            gravity = Gravity.BOTTOM
            marginStart = dp(48)
            marginEnd = dp(48)
            bottomMargin = dp(42)
        })
        setContentView(container)

        handler.postDelayed({ minimumTimeElapsed = true; finishWhenReady() }, MINIMUM_SPLASH_MS)
        Thread {
            runCatching { runBlocking { (application as BookshelfApplication).database.libraryDao().bookCount() } }
                .onSuccess { handler.post { libraryLoaded = true; finishWhenReady() } }
                .onFailure { handler.post { libraryLoaded = true; finishWhenReady() } }
        }.start()
    }

    private fun finishWhenReady() {
        if (!minimumTimeElapsed || !libraryLoaded || transitionStarted || isFinishing) return
        transitionStarted = true
        handler.post {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object { private const val MINIMUM_SPLASH_MS = 5_000L }
}
