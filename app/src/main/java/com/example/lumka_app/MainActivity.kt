package com.example.lumka_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.lumka_app.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private val fakeCallFragment = FakeCallFragment()
    private val dangerAlertsFragment = DangerAlertsFragment()
    private val sosFragment = SOSFragment()
    private val locationStatusFragment = LocationStatusFragment()
    private val nortificationsFragment = NortificationsFragment()

    private lateinit var auth: FirebaseAuth

    // Header views
    private lateinit var tvInitial: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvDisplayEmail: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Initialize header views
        tvInitial = findViewById(R.id.initial)
        tvDisplayName = findViewById(R.id.displayName)
        tvDisplayEmail = findViewById(R.id.displayEmail)

        fetchUserDetails()  // fetch once on launch

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set default fragment
        replaceFragment(dangerAlertsFragment)
        bottomNavigation.menu.findItem(R.id.nav_danger_alert).isChecked = true

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_danger_alert -> replaceFragment(dangerAlertsFragment)
                R.id.nav_fake_call -> replaceFragment(fakeCallFragment)
                R.id.nav_sos -> replaceFragment(sosFragment)
                R.id.nav_location_status -> replaceFragment(locationStatusFragment)
                R.id.nav_nortification -> replaceFragment(nortificationsFragment)
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Fetch user info from Firebase and populate header
    private fun fetchUserDetails() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)

                tvDisplayName.text = username ?: "No username"
                tvDisplayEmail.text = email ?: "No email"
                tvInitial.text = username?.firstOrNull()?.toString() ?: ""
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Optional: Helper function for fragments to update header dynamically
    fun updateHeader(username: String, email: String) {
        tvDisplayName.text = username
        tvDisplayEmail.text = email
        tvInitial.text = username.firstOrNull()?.toString() ?: ""
    }
}
