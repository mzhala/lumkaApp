package com.example.lumka_app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.location.Geocoder
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class ReportIncidentDialogFragment(
    private val onSubmit: () -> Unit
) : DialogFragment() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvLocation: TextView
    private lateinit var btnSelectLocation: ImageButton
    private lateinit var btnPickDateTime: ImageButton  // <-- Changed to Button
    private lateinit var tvTimestamp: TextView
    private lateinit var btnSubmit: Button

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("incidents") }
    private lateinit var auth: FirebaseAuth
    private var selectedTimestamp: Long = 0L  // store the selected date/time

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.bottomsheet_report_incident, container, false)

        auth = FirebaseAuth.getInstance()
        etTitle = v.findViewById(R.id.etTitle)
        etDescription = v.findViewById(R.id.etDescription)
        tvLocation = v.findViewById(R.id.tvLocation)
        btnSelectLocation = v.findViewById(R.id.btnSelectLocation)
        btnPickDateTime = v.findViewById(R.id.btnPickDateTime) // Button now
        btnSubmit = v.findViewById(R.id.btnSubmitIncident)
        tvTimestamp = v.findViewById(R.id.tvTimestamp)

        setupLocationPicker()


        btnPickDateTime.setOnClickListener {
            val cal = Calendar.getInstance()

            // Open DatePicker
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // After date is picked, open TimePicker
                val timePicker = TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    cal.set(Calendar.MINUTE, minute)

                    // Save selected timestamp
                    selectedTimestamp = cal.timeInMillis

                    // Update the TextView
                    tvTimestamp.text = DateFormat.format(
                        "dd MMM yyyy, HH:mm",
                        Date(selectedTimestamp)
                    )

                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)

                timePicker.show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

            datePicker.show()
        }


        btnSubmit.setOnClickListener {
            submitIncident()
        }

        return v
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun setupLocationPicker() {
        val autocompleteFragment = childFragmentManager
            .findFragmentById(R.id.autocompleteFragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        ))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedLat = place.latLng?.latitude
                selectedLng = place.latLng?.longitude
                tvLocation.text = place.address ?: "Unknown Location"
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                tvLocation.text = "Error picking location"
            }
        })
    }

    private fun submitIncident() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etTitle.text.toString().trim()
        val desc = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLat == null || selectedLng == null) {
            Toast.makeText(requireContext(), "Please pick a location", Toast.LENGTH_SHORT).show()
            return
        }

        val key = dbRef.push().key
        if (key == null) {
            Toast.makeText(requireContext(), "Failed to generate key", Toast.LENGTH_SHORT).show()
            return
        }

        val address = tvLocation.text.toString()

        val incident = Incident(
            id = key,
            userId = uid,
            title = title,
            description = desc,
            latitude = selectedLat,
            longitude = selectedLng,
            timestamp = System.currentTimeMillis(),
            address = address,
            likes = 0
        )

        dbRef.child(key).setValue(incident)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Incident reported successfully", Toast.LENGTH_SHORT).show()
                onSubmit()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to report incident: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
