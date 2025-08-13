package com.example.lumka_app


import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lumka_app.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        navigateWithDelay()
    }
    fun navigateWithDelay() {
        runOnUiThread {
            Handler(Looper.getMainLooper()).postDelayed({
                // Intent to start MainActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // Finish splash activity so user can't go back
            }, 3000) // 3000 milliseconds = 3 seconds
        }
    }
}