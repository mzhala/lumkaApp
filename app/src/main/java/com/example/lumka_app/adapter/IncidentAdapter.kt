package com.example.lumka_app

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Date
import java.util.Locale

class IncidentAdapter(
    private var items: MutableList<Incident>,
    private val onThumbsUp: (Incident) -> Unit
) : RecyclerView.Adapter<IncidentAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance) // <-- add this
        val imgThumbsUp: ImageView = view.findViewById(R.id.imgThumbsUp)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_incident, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val it = items[position]
        h.tvTitle.text = it.title.orEmpty()
        h.tvDescription.text = it.description.orEmpty()


        val ts = it.timestamp ?: 0L
        val dateStr = DateFormat.format("dd MMM yyyy, HH:mm", Date(ts)).toString()
        val latStr = it.latitude?.let { d -> String.format(Locale.US, "%.5f", d) } ?: "—"
        val lngStr = it.longitude?.let { d -> String.format(Locale.US, "%.5f", d) } ?: "—"
        h.tvMeta.text = "$dateStr • $latStr, $lngStr"

        // Display distance in km
        h.tvDistance.text = it.distanceKm?.let { dist -> "%.1f km away".format(dist) } ?: ""

        h.tvLikes.text = it.likes.toString()
        h.imgThumbsUp.setOnClickListener { _ -> onThumbsUp(it) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Incident>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
