package com.example.lumka_app


import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.lumka_app.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private val FakeCallFragment = FakeCallFragment()
    private val DangerAlertsFragment = DangerAlertsFragment()
    private val SOSFragment = SOSFragment()
    private val LocationStatusFragment = LocationStatusFragment()
    private val NortificationsFragment = NortificationsFragment()
    private val walletFragment = WalletFrag()
    private val insightsFragment = InsightsFrag()
    private val profileFragment = ProfileFragment()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set default fragment
        replaceFragment(DangerAlertsFragment)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_danger_alert -> replaceFragment(DangerAlertsFragment)
                R.id.nav_fake_call -> replaceFragment(FakeCallFragment)
                R.id.nav_sos -> replaceFragment(SOSFragment)
                R.id.nav_location_status -> replaceFragment(LocationStatusFragment)
                R.id.nav_nortification -> replaceFragment(NortificationsFragment)
                else -> false
            }
            true
        }

    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }
}