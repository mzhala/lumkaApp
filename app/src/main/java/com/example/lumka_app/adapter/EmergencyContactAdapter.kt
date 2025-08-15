package com.example.lumka_app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lumka_app.R

class EmergencyContactAdapter(
    private val context: Context,
    private var contacts: List<EmergencyContact>,
    private val onCallClick: (EmergencyContact) -> Unit,
    private val onSetMainClick: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvContactName)
        val btnCall: ImageButton = view.findViewById(R.id.btnCallContact)
        val btnSetMain: ImageButton = view.findViewById(R.id.btnSetMain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.tvName.text = contact.name
        holder.btnCall.setOnClickListener { onCallClick(contact) }


        if (contact.isMain) {
            holder.tvName.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.btn_star_big_on, 0, 0, 0
            )
        } else {
            holder.tvName.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.btn_star_big_off, 0, 0, 0
            )
        }
        holder.btnSetMain.setOnClickListener { onSetMainClick(contact) }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateList(newContacts: List<EmergencyContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
