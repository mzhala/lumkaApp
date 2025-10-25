package com.example.lumka_app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.loginEmail)
        passwordEditText = findViewById(R.id.passwordLogin)
    }

    fun signIn(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    fun login(view: View) {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            loginUser(email, password)
        } else {
            showPopupMessage("Missing Fields", "Please fill all fields before continuing.")
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showPopupMessage("Welcome Back", "Login successful!")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showPopupMessage(
                        "Login Failed",
                        task.exception?.message ?: "Please check your credentials and try again."
                    )
                }
            }
    }

    private fun showPopupMessage(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        // Create rounded white background with 5dp corners
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f  // about 5dp corners
            setColor(resources.getColor(android.R.color.white, theme))
        }

        dialog.window?.setBackgroundDrawable(background)

        // Adjust dialog margins (≈20dp on each side)
        val window = dialog.window
        val params = window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        window?.attributes = params

        // Change text and button colors to black
        dialog.findViewById<android.widget.TextView>(android.R.id.message)
            ?.setTextColor(resources.getColor(android.R.color.black, theme))
        dialog.findViewById<android.widget.Button>(android.R.id.button1)
            ?.setTextColor(resources.getColor(android.R.color.black, theme))
    }
}
