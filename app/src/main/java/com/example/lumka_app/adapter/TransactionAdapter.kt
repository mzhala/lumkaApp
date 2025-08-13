package com.example.budgettracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.lumka_app.model.Category
import com.example.lumka_app.model.Transaction
import com.example.lumka_app.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val category: TextView = itemView.findViewById(R.id.text_category)
        val amount: TextView = itemView.findViewById(R.id.text_amount)
        val date: TextView = itemView.findViewById(R.id.text_date)
        val type: TextView = itemView.findViewById(R.id.text_types)
        val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.category.text = transaction.category
        holder.date.text = transaction.date
        holder.type.text = transaction.type.capitalize()
        // Optional: change background color based on type
        if (transaction.type == "income") {
            holder.amount.setTextColor(0xFF00FF00.toInt()) // green
            holder.amount.text = "+ R%.2f".format(transaction.amount)
        } else {
            holder.amount.setTextColor(0xFFFF0000.toInt()) // red
            holder.amount.text = "- R%.2f".format(transaction.amount)
        }

        Glide.with(holder.itemView.context)
            .load(transaction?.url)
            .placeholder(R.drawable.ic_money)
            .into(holder.categoryIcon)
    }



    override fun getItemCount(): Int = transactions.size
}