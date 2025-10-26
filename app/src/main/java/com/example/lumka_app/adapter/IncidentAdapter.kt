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
    private val onThumbsUp: (Incident) -> Unit,
    private var filterLat: Double? = null,    // <-- current location
    private var filterLng: Double? = null
) : RecyclerView.Adapter<IncidentAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.initial)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val imgThumbsUp: ImageView = view.findViewById(R.id.imgThumbsUp)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_incident, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val it = items[position]

        // Show first letter of username
        h.tvInitial.text = it.userId?.firstOrNull()?.uppercaseChar()?.toString() ?: "A"

        h.tvTitle.text = it.title.orEmpty()
        h.tvDescription.text = it.description.orEmpty()

        val ts = it.timestamp ?: 0L
        val dateStr = DateFormat.format("dd MMM yyyy, HH:mm", Date(ts)).toString()
        val latStr = it.latitude?.let { d -> String.format(Locale.US, "%.5f", d) } ?: "—"
        val lngStr = it.longitude?.let { d -> String.format(Locale.US, "%.5f", d) } ?: "—"
        h.tvMeta.text = "$dateStr • $latStr, $lngStr"

        // Compute distance dynamically
        if (filterLat != null && filterLng != null && it.latitude != null && it.longitude != null) {
            val loc1 = android.location.Location("").apply { latitude = filterLat!!; longitude = filterLng!! }
            val loc2 = android.location.Location("").apply { latitude = it.latitude!!; longitude = it.longitude!! }
            val distKm = loc1.distanceTo(loc2) / 1000f
            h.tvDistance.text = "%.1f km away".format(distKm)
        } else {
            h.tvDistance.text = ""
        }

        h.tvLikes.text = it.likes.toString()
        h.imgThumbsUp.setOnClickListener { _ -> onThumbsUp(it) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Incident>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Helper to update current location when fragment changes it
    fun setCurrentLocation(lat: Double?, lng: Double?) {
        filterLat = lat
        filterLng = lng
        notifyDataSetChanged()
    }
}

