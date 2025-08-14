package com.example.lumka_app


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettracker.adapter.TransactionAdapter
import com.example.lumka_app.model.Category
import com.example.lumka_app.model.MonthlyBudget
import com.example.lumka_app.model.Transaction
import com.example.lumka_app.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import androidx.viewpager2.widget.ViewPager2
import com.example.lumka_app.adapter.ImageAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DashboardFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class NortificationsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var displayName: TextView
    private lateinit var displayEmail: TextView
    private lateinit var initials: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    @SuppressLint("MissingInflatedId", "CutPasteId", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_nortifications, container, false)
        auth = FirebaseAuth.getInstance()

        // --- UI ---
        displayName = v.findViewById(R.id.displayName)
        displayEmail = v.findViewById(R.id.displayEmail)
        initials = v.findViewById(R.id.initial)

        // Fetch and display user details AFTER views are initialized
        fetchUserDetails()

        return v
    }



    fun getFirstCharacter(username: String?): String {
        return username?.firstOrNull()?.toString() ?: ""
    }

    private fun fetchUserDetails() {
        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val userRef: DatabaseReference = database.getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)

                // Only update UI if the fragment is still added and views are initialized
                if (isAdded && ::displayName.isInitialized && ::displayEmail.isInitialized && ::initials.isInitialized) {
                    displayName.text = username ?: "No username"
                    displayEmail.text = email ?: "No email"
                    initials.text = getFirstCharacter(username)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded && ::displayName.isInitialized && ::displayEmail.isInitialized) {
                    displayName.text = "Username"
                    displayEmail.text = ""
                }
            }
        })
    }


}