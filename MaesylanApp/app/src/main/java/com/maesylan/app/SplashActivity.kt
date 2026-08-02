package com.maesylan.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * A lightweight splash screen shown while the app initialises.
 *
 * On Android 12+ the system splash screen (declared in the theme) handles the
 * cold-start experience. On older devices we fall back to a branded layout
 * displayed briefly before MainActivity starts.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the AndroidX splash screen — on Android 12+ this uses the
        // system-level splash API with our branded icon/background.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        // Delay so users see the branding on older devices where the system
        // splash is very brief.
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DELAY_MS)
    }

    companion object {
        /** How long the splash is shown (milliseconds). */
        private const val SPLASH_DELAY_MS = 1400L
    }
}
