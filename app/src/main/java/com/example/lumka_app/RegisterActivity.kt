package com.example.lumka_app


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.example.lumka_app.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var database: FirebaseDatabase
    private lateinit var progressBar: ProgressBar

    private lateinit var databaseReference: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        progressBar = findViewById(R.id.progressBar)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        emailEditText = findViewById(R.id.registerEmail)
        passwordEditText = findViewById(R.id.registerPassword)
        nameEditText = findViewById(R.id.registerName)

        databaseReference = database.getReference("users")

    }

    private fun AddUser(password: String, username: String, email: String, url: String) {
        progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val user =  User(auth.currentUser!!.uid, username, email, url)


                    databaseReference.child(auth.currentUser!!.uid).setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to the login activity or another desired activity
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            progressBar.visibility = View.GONE
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add the user account: ${e.message}", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                        }

                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
    }

    fun singUp(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    fun registerUser(view: View) {

        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        val name = nameEditText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
            AddUser(password, name, email ,"") //R.string.user_pic.toString()
        } else {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }
}