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
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lumka_app.model.Category
import com.example.lumka_app.model.MonthlyBudget
import com.example.lumka_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [InsightsFrag.newInstance] factory method to
 * create an instance of this fragment.
 */

class InsightsFrag : Fragment() {

    private lateinit var tvTotalBudget: TextView
    private lateinit var tvBudgetLeft: TextView
    private lateinit var btnCreateBudget: Button
    private lateinit var rvCategoryBudgets: RecyclerView
    private lateinit var tvInsight: TextView
    private lateinit var displayName: TextView
    private lateinit var displayEmail: TextView
    private lateinit var initials: TextView

    private val categoryBudgets = mutableListOf<CategoryBudget>()
    private lateinit var adapter: CategoryBudgetAdapter
    private lateinit var userId: String

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_insights, container, false)
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget)
        tvBudgetLeft = view.findViewById(R.id.tvBudgetLeft)
        btnCreateBudget = view.findViewById(R.id.btnCreateBudget)
        rvCategoryBudgets = view.findViewById(R.id.rvCategoryBudgets)
        tvInsight = view.findViewById(R.id.tvInsight)
        displayName = view.findViewById(R.id.displayEmail)
        displayEmail = view.findViewById(R.id.displayEmail)
        initials = view.findViewById(R.id.initial)

        rvCategoryBudgets.layoutManager = LinearLayoutManager(context)
        adapter = CategoryBudgetAdapter()
        rvCategoryBudgets.adapter = adapter

        // Get current user id
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Load budgets and expenses
        updateSummary()

        btnCreateBudget.setOnClickListener {
            showCreateBudgetDialog()
        }

        fetchUserDetails()

        return view
    }

    private fun fetchBudgets() {
        val ref = FirebaseDatabase.getInstance().getReference("category_budgets")
        ref.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryBudgets.clear()
                    for (child in snapshot.children) {
                        val budget = child.getValue(CategoryBudget::class.java)
                        budget?.let { categoryBudgets.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load budgets", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun getTotalExpenses(transactions: List<Transaction>): Double {
        return transactions.filter { it.type == "expense" }
            .sumOf { it.amount }
    }

    fun fetchAllTransactions(userId: String, onComplete: (List<Transaction>) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("transactions")
        ref.orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactions = mutableListOf<Transaction>()
                    for (child in snapshot.children) {
                        val transaction = child.getValue(Transaction::class.java)
                        transaction?.let { transactions.add(it) }
                    }
                    onComplete(transactions)
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete(emptyList())
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

    private fun updateSummary() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        fetchMonthlyBudget(userId) { monthlyBudget ->
            if (monthlyBudget != null) {
                fetchAllTransactions(userId) { transactions ->
                    // Now you have all transactions; you can filter expenses, sum amounts, etc.
                    val totalBudget = getTotalExpenses(transactions) + monthlyBudget.amount
                    val budgetLeft = monthlyBudget.amount
                    tvTotalBudget.text = "Out of "+String.format("R%.2f", totalBudget)+" budgeted"
                    tvBudgetLeft.text = String.format("R%.2f", budgetLeft)+" left"

                    val allOnTrack = categoryBudgets.all { it.spent <= it.amount }
                    tvInsight.text = if (allOnTrack) {
                        "🔥 Your limit for all categories is on track"
                    } else {
                        "⚠️ Some categories have exceeded their limits"
                    }

                    fetchBudgets()
                }
            } else {
                tvTotalBudget.text = "R0.00"
                tvBudgetLeft.text = "R0.00"

            }
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showCreateBudgetDialog() {
        // Implement dialog to create a new CategoryBudget
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_budget, null)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val etAmount = dialogView.findViewById<EditText>(R.id.input_amount)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

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

        AlertDialog.Builder(requireContext())
            .setTitle("Create New Budget Category")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ ->
                val selectedPosition = spinnerCategory.selectedItemPosition
                val categoryName = categoryList[selectedPosition]
                val amountText = etAmount.text.toString().trim()

                if (amountText.isEmpty()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newBudgetRef = FirebaseDatabase.getInstance().getReference("category_budgets").push()
                val newBudget = CategoryBudget(
                    id = newBudgetRef.key ?: "",
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    monthYear = getCurrentMonthYear(), // helper function
                    category = categoryName.name,
                    amount = amount
                )

                newBudgetRef.setValue(newBudget)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Budget created!", Toast.LENGTH_SHORT).show()
                        fetchBudgets() // refresh list
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to create budget", Toast.LENGTH_SHORT).show()
                    }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Helper function to get current month-year string in "YYYY-MM" format
    private fun getCurrentMonthYear(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }

    // Data class with additional 'spent' for display
    data class CategoryBudget(
        val id: String = "",
        val userId: String = "",
        val monthYear: String = "",
        val category: String = "",
        val amount: Double = 0.0,
        var spent: Double = 0.0 // for display purposes
    )

    // Data class for expenses/transactions
    data class Transaction(
        var id: String = "",
        var amount: Double = 0.0,
        var category: String = "",
        var type: String = "", // "income" or "expense"
        var date: String = "",
        var uid: String = "",
        var url: String = ""
    )

    // Adapter class
    inner class CategoryBudgetAdapter :
        RecyclerView.Adapter<CategoryBudgetAdapter.BudgetViewHolder>() {

        inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
            val tvSpent: TextView = itemView.findViewById(R.id.tvSpent)
            val tvLeft: TextView = itemView.findViewById(R.id.tvLeft)
            val tvLimit: TextView = itemView.findViewById(R.id.tvLimit)
            val seekBar: SeekBar = itemView.findViewById(R.id.seekBar)
            val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
            val tvInsights: TextView = itemView.findViewById(R.id.tvInsights)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_budget, parent, false)
            return BudgetViewHolder(view)
        }

        override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
            val budget = categoryBudgets[position]
            holder.tvCategoryName.text = budget.category

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            fetchExpensesByCategory(userId, budget.category) { expensesList ->

                // Calculate total spent per category
                val totalSpentPerCategory = getTotalSpentPerCategory(expensesList)

                // Access total spent for a specific category:
                val spenT = totalSpentPerCategory[budget.category] ?: 0.0

                holder.tvSpent.text = String.format("R%.2f", spenT)
                val left = budget.amount - spenT
                holder.tvLeft.text = String.format("R%.2f", left)
                holder.tvLimit.text = String.format("R%.2f", budget.amount)

                if (spenT <= budget.amount){
                    holder.tvInsights.text = "🔥 Your limit for " +budget.category+" is on track"
                }else{
                    holder.tvInsights.text = "⚠️ You have exceeded your limits"
                }


                // Setup SeekBar
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                fetchMonthlyBudget(userId) { monthlyBudget ->
                    if (monthlyBudget != null) {
                        fetchAllTransactions(userId) { transactions ->
                            // Now you have all transactions; you can filter expenses, sum amounts, etc.
                            val totalBudget = getTotalExpenses(transactions) + monthlyBudget.amount
                            // Setup SeekBar
                            fetchMonthlyBudget(userId) { monthlyBudget ->
                                if (monthlyBudget != null) {
                                    holder.seekBar.max = (totalBudget * 100).toInt()
                                } else {
                                    holder.seekBar.max = 0
                                }
                            }
                            holder.seekBar.progress = (budget.amount * 100).toInt()
                            holder.seekBar.secondaryProgress = (spenT * 100).toInt()
                        }
                        holder.seekBar.max = (monthlyBudget.amount * 100).toInt()
                    } else {
                        holder.seekBar.max = 0
                    }
                }
                holder.seekBar.progress = (2000 * 100).toInt()
                holder.seekBar.secondaryProgress = (900 * 100).toInt()

            }
            fetchCategories(userId) { categoriesList ->
                // Now you have the list of categories, you can call your function
                val iconUrl = getCategoryIconUrl(categoriesList, budget.category)
                // Use iconUrl as needed
                Glide.with(holder.itemView.context)
                    .load(iconUrl)
                    .placeholder(R.drawable.ic_money)
                    .into(holder.categoryIcon)
            }

        }

        override fun getItemCount() = categoryBudgets.size
    }

    fun fetchExpensesByCategory(userId: String, categoryName: String, onComplete: (List<Transaction>) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("transactions")
        ref.orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expenses = mutableListOf<Transaction>()
                    for (child in snapshot.children) {
                        val transaction = child.getValue(Transaction::class.java)
                        if (transaction != null && transaction.type == "expense" && transaction.category == categoryName) {
                            expenses.add(transaction)
                        }
                    }
                    onComplete(expenses)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                    onComplete(emptyList())
                }
            })
    }

    fun getCategoryIconUrl(categories: List<Category>, categoryName: String): String? {
        // Search for a category with a matching name (case-insensitive)
        val category = categories.find { it.name.equals(categoryName, ignoreCase = true) }
        // Return the iconUrl if found, else null
        return category?.iconUrl
    }

    fun getTotalSpentPerCategory(transactions: List<Transaction>): Map<String, Double> {
        val spentMap = mutableMapOf<String, Double>()
        for (transaction in transactions) {
            if (transaction.type == "expense") {
                val currentTotal = spentMap[transaction.category] ?: 0.0
                spentMap[transaction.category] = currentTotal + transaction.amount
            }
        }
        return spentMap
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

    fun fetchCategories(userId: String, onComplete: (List<Category>) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("categories")
        ref.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categories = mutableListOf<Category>()
                    for (child in snapshot.children) {
                        val category = child.getValue(Category::class.java)
                        category?.let { categories.add(it) }
                    }
                    onComplete(categories)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                    onComplete(emptyList())
                }
            })
    }


}