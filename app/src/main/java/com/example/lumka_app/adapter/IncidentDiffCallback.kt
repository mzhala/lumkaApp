package com.example.lumka_app

import androidx.recyclerview.widget.DiffUtil

class IncidentDiffCallback : DiffUtil.ItemCallback<Incident>() {

    // This checks if two items represent the same object.
    // It's usually based on a unique ID.
    override fun areItemsTheSame(oldItem: Incident, newItem: Incident): Boolean {
        return oldItem.id == newItem.id
    }

    // This checks if the content of the items is the same.
    // It's called only if areItemsTheSame() returns true.
    // The default data class 'equals' implementation works perfectly here.
    override fun areContentsTheSame(oldItem: Incident, newItem: Incident): Boolean {
        return oldItem == newItem
    }
}
