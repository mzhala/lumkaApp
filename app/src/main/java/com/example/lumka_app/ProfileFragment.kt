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
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var displayName: TextView
    private lateinit var displayEmail: TextView
    private lateinit var initials: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()
    private lateinit var incomeTextView: TextView
    private lateinit var expenseTextView: TextView
    private lateinit var avgExpenseTextView: TextView
    private lateinit var budgetTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchUserDetails()
    }

    @SuppressLint("MissingInflatedId", "CutPasteId", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_fake_call, container, false)

        val viewPager: ViewPager2 = view.findViewById(R.id.viewPager)
        val tabLayout: TabLayout = view.findViewById(R.id.tabLayout)

        val images = listOf(R.drawable.fake_call_d1, R.drawable.fake_call_d2, R.drawable.fake_call_d3, R.drawable.fake_call_d4)
        val adapter = ImageAdapter(images)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // No setup needed here for dots
        }.attach()

        displayName = view.findViewById(R.id.displayName)
        displayEmail = view.findViewById(R.id.displayEmail)
        initials = view.findViewById(R.id.initial)

        // Fetch and display user details (this will update these views)
        fetchUserDetails()
        return view
    }

    fun fetchCategories(userId: String, onResult: (List<Category>) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("categories")
        ref.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categories = mutableListOf<Category>()
                    for (child in snapshot.children) {
                        val category = child.getValue(Category::class.java)
                        category?.let { categories.add(it) }
                    }
                    onResult(categories)
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
    }

    fun getFirstCharacter(username: String?): String {
        return username?.firstOrNull()?.toString() ?: ""
    }

    private fun fetchUserDetails() {
        auth = FirebaseAuth.getInstance();
        val userId = auth.currentUser!!.uid
        val database = FirebaseDatabase.getInstance()
        val userRef: DatabaseReference = database.getReference("users").child(userId)

        // Show a loading indicator if needed
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)

                // Update UI with user data
                displayName.text = username ?: "No username"
                displayEmail.text = email ?: "No email"
                initials.text = getFirstCharacter(username) ?: "No email"
            }

            override fun onCancelled(error: DatabaseError) {
                displayName.text = "Username"
                displayEmail.text = ""
            }
        })
    }
}