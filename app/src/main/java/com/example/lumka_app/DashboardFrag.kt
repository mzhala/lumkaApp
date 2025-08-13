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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DashboardFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashboardFrag : Fragment() {
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
        val view = inflater.inflate(R.layout.frag_dashboard, container, false)
        displayName = view.findViewById(R.id.displayName)
        displayEmail = view.findViewById(R.id.displayEmail)
        initials = view.findViewById(R.id.initial)
        recyclerView = view.findViewById(R.id.transactions_list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TransactionAdapter(transactions)
        recyclerView.adapter = adapter

        incomeTextView = view.findViewById(R.id.totalIncomeTextView)
        expenseTextView = view.findViewById(R.id.totalExpenseTextView)
        avgExpenseTextView = view.findViewById(R.id.averageExpenseTextView)
        budgetTextView = view.findViewById(R.id.budgetTextView)

        val addIncomeBtn: Button = view.findViewById(R.id.incomeBtn)
        val addExpenseBtn: Button = view.findViewById(R.id.expenseBtn)

        addIncomeBtn.setOnClickListener {
            showAddTransactionDialog(isIncome = true)
        }

        addExpenseBtn.setOnClickListener {
            showAddTransactionDialog(isIncome = false)
        }

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        fetchMonthlyBudget(userId) { monthlyBudget ->
            if (monthlyBudget != null) {
                budgetTextView.text = "R%.2f".format(monthlyBudget.amount)
            } else {
                budgetTextView.text = "R0.00"
            }
        }

        calculateAndDisplayTotals()

        loadTransactions()

        calculateAndDisplayAverageExpense()

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

    fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    fun updateMonthlyBudgetOnTransaction(
        uid: String,
        amount: Double,
        isIncome: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val currentMonth = getCurrentMonthYear()
        val refMonthlyBudget = FirebaseDatabase.getInstance().getReference("monthly_budgets")

        refMonthlyBudget.orderByChild("userId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var currentBudget: MonthlyBudget? = null
                    var budgetKey: String? = null

                    // Find existing budget for the current month
                    for (child in snapshot.children) {
                        val budget = child.getValue(MonthlyBudget::class.java)
                        if (budget != null ) {
                            currentBudget = budget
                            budgetKey = child.key
                            break
                        }
                    }

                    if (currentBudget != null && budgetKey != null) {
                        // Adjust the budget based on transaction type
                        val adjustment = if (isIncome) amount else -amount
                        val newAmount = currentBudget.amount + adjustment
                        val updates = mapOf("amount" to newAmount)
                        refMonthlyBudget.child(budgetKey).updateChildren(updates)
                            .addOnSuccessListener { onComplete(true) }
                            .addOnFailureListener { onComplete(false) }
                    } else {
                        // No existing budget, create one if income
                        if (isIncome) {
                            val newBudget = MonthlyBudget(
                                userId = uid,
                                monthYear = currentMonth,
                                amount = amount
                            )
                            refMonthlyBudget.push().setValue(newBudget)
                                .addOnSuccessListener { onComplete(true) }
                                .addOnFailureListener { onComplete(false) }
                        } else {
                            // For expense, if no budget exists, just do nothing or create with negative value
                            // Here, assuming we don't create negative budgets
                            onComplete(false)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(false)
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

    private fun calculateAndDisplayAverageExpense() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("transactions")
        ref.orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalExpense = 0.0
                    var expenseCount = 0

                    for (child in snapshot.children) {
                        val transaction = child.getValue(Transaction::class.java)
                        if (transaction != null && transaction.type == "expense") {
                            totalExpense += transaction.amount
                            expenseCount++
                        }
                    }

                    val averageExpense = if (expenseCount > 0) totalExpense / expenseCount else 0.0
                    avgExpenseTextView.text = "Average Expense: R%.2f".format(averageExpense)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun calculateAndDisplayTotals() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("transactions")
        ref.orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalIncome = 0.0
                    var totalExpense = 0.0

                    for (child in snapshot.children) {
                        val transaction = child.getValue(Transaction::class.java)
                        if (transaction != null) {
                            if (transaction.type == "income") {
                                totalIncome += transaction.amount
                            } else if (transaction.type == "expense") {
                                totalExpense += transaction.amount
                            }
                        }
                    }

                    incomeTextView.text = "R%.2f".format(totalIncome)
                    expenseTextView.text = "R%.2f".format(totalExpense)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun showAddTransactionDialog(isIncome: Boolean) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null)
        val amountInput = dialogView.findViewById<EditText>(R.id.input_amount)
        val dateInput = dialogView.findViewById<EditText>(R.id.input_date)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinner_type)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("income", "expense"))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter

        val categoryList = mutableListOf<Category>()
        val categoryNames = mutableListOf<String>()

        fetchCategories(userId) { categories ->
            categoryList.clear()
            categoryList.addAll(categories)
            categoryNames.clear()
            categoryNames.addAll(categories.map { it.name })

            // Create an ArrayAdapter to set to the spinner
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        }

        // Set default type
        spinnerType.setSelection(if (isIncome) 0 else 1)

        AlertDialog.Builder(requireContext())
            .setTitle(if (isIncome) "Add Income" else "Add Expense")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val amountText = amountInput.text.toString()
                val date = dateInput.text.toString()

                if (amountText.isEmpty() || date.isEmpty()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull()
                if (amount == null) {
                    Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val typeSelected = spinnerType.selectedItem.toString()
                val selectedPosition = spinnerCategory.selectedItemPosition
                val category = categoryList[selectedPosition]

                saveTransaction(amount, category.name, typeSelected, date, category.iconUrl.toString())
                updateMonthlyBudgetOnTransaction(userId, amount, typeSelected == "income") { success ->
                    if (success) {
                        fetchMonthlyBudget(userId) { monthlyBudget ->
                            if (monthlyBudget != null) {
                                budgetTextView.text = "R%.2f".format(monthlyBudget.amount)
                            } else {
                                budgetTextView.text = "R0.00"
                            }
                            calculateAndDisplayTotals()

                            calculateAndDisplayAverageExpense()
                        }
                    } else {
                    }
                }

            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    private fun saveTransaction(amount: Double, category: String, type: String, date: String, url: String) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("transactions").push()
        val transaction = Transaction(
            id = ref.key ?: "",
            amount = amount,
            category = category,
            type = type,
            date = date,
            uid = userId,
            url = url
        )

        ref.setValue(transaction).addOnCompleteListener {
            Toast.makeText(context, "Transaction added!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTransactions() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("transactions")
        val query = ref.orderByChild("uid").equalTo(userId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactions.clear()
                for (child in snapshot.children) {
                    val transaction = child.getValue(Transaction::class.java)
                    if (transaction != null) {
                        transaction.id = child.key ?: ""
                        transactions.add(transaction)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                // handle error
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