package com.example.lumka_app


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.lumka_app.model.MonthlyBudget
import com.example.lumka_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WalletFrag.newInstance] factory method to
 * create an instance of this fragment.
 */

class WalletFrag : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var btnAddWallet: LinearLayout
    private lateinit var budgetTextView: TextView
    private lateinit var displayName: TextView
    private lateinit var displayEmail: TextView
    private lateinit var initials: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_wallet, container, false)
        btnAddWallet = view.findViewById(R.id.addNew)
        budgetTextView = view.findViewById(R.id.budgetTextView)
        displayName = view.findViewById(R.id.displayEmail)
        displayEmail = view.findViewById(R.id.displayEmail)
        initials = view.findViewById(R.id.initial)
        btnAddWallet.setOnClickListener {
            showCreateMonthlyBudgetDialog()
        }

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        fetchMonthlyBudget(userId) { monthlyBudget ->
            if (monthlyBudget != null) {
                budgetTextView.text = "R%.2f".format(monthlyBudget.amount)
            } else {
                budgetTextView.text = "R0.00"
            }
        }

        fetchUserDetails()

        return view
    }
    private fun showCreateMonthlyBudgetDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_monthly_budget, null)
        val etMonthYear = dialogView.findViewById<EditText>(R.id.etMonthYear)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)

        // Optional: Show a DatePickerDialog when clicking the month/year EditText
        etMonthYear.setOnClickListener {
            showMonthYearPicker { selectedMonthYear ->
                etMonthYear.setText(selectedMonthYear)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Create/Update Monthly Budget")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val monthYearStr = etMonthYear.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()

                if (monthYearStr.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!isValidMonthYearFormat(monthYearStr)) {
                    Toast.makeText(requireContext(), "Enter month in YYYY-MM format", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Save or update the budget
                setMonthlyMainBudget(monthYearStr, amount)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Helper to show a month-year picker dialog
    private fun showMonthYearPicker(onMonthYearSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        val datePicker = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, _ ->
            val monthStr = String.format("%02d", selectedMonth + 1)
            val selectedMonthYear = "$selectedYear-$monthStr"
            onMonthYearSelected(selectedMonthYear)
        }, year, month, 1)

        // Hide the day picker
        datePicker.datePicker.findViewById<View>(
            resources.getIdentifier("day", "id", "android")
        )?.visibility = View.GONE

        datePicker.show()
    }
    fun getFirstCharacter(username: String?): String {
        return username?.firstOrNull()?.toString() ?: ""
    }

    private fun fetchUserDetails() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
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

    // Simple validation for YYYY-MM format
    private fun isValidMonthYearFormat(input: String): Boolean {
        return input.matches(Regex("\\d{4}-\\d{2}"))
    }

    private fun setMonthlyMainBudget(monthYear: String, amount: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("monthly_budgets")

        // Query to find existing budget for user and month
        val query = ref.orderByChild("userId").equalTo(userId)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var budgetExists = false
                for (child in snapshot.children) {
                    val existingBudget = child.getValue(MonthlyBudget::class.java)
                    if (existingBudget != null && existingBudget.monthYear == monthYear) {
                        // Update existing budget
                        child.ref.child("amount").setValue(amount)
                        budgetExists = true
                        break
                    }
                }
                if (!budgetExists) {
                    // Create new budget entry
                    val newRef = ref.push()
                    val newMonthlyBudget = MonthlyBudget(
                        id = newRef.key ?: "",
                        userId = userId,
                        monthYear = monthYear,
                        amount = amount
                    )
                    newRef.setValue(newMonthlyBudget)
                }
                Toast.makeText(context, "Monthly budget for $monthYear set to \$${amount}", Toast.LENGTH_SHORT).show()
                // Optionally refresh your data/display here
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to set monthly budget: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun fetchMonthlyBudget(uid: String, onResult: (MonthlyBudget?) -> Unit) {
        //val currentMonth = getCurrentMonthYear()
        val ref = FirebaseDatabase.getInstance().getReference("monthly_budgets")
        ref.orderByChild("userId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var result: MonthlyBudget? = null
                    for (child in snapshot.children) {
                        val budget = child.getValue(MonthlyBudget::class.java)
                        if (budget != null) {
                            result = budget
                            break // Assuming one per user per month
                        }
                    }
                    onResult(result)
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

}