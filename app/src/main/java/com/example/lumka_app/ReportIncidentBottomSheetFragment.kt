package com.example.lumka_app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class ReportIncidentBottomSheetFragment(
    private val onIncidentSubmitted: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvLocation: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var btnPickDateTime: ImageButton  // Updated type
    private lateinit var btnSubmit: Button

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedTimestamp: Long = 0L

    private lateinit var auth: FirebaseAuth
    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("incidents") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyBhITPUEQqXfIXsM5EOTZPh3bHH_SclJZ8")
        }
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.bottomsheet_report_incident, container, false)

        etTitle = v.findViewById(R.id.etTitle)
        etDescription = v.findViewById(R.id.etDescription)
        tvLocation = v.findViewById(R.id.tvLocation)
        tvTimestamp = v.findViewById(R.id.tvTimestamp)
        btnPickDateTime = v.findViewById(R.id.btnPickDateTime) // Button now
        btnSubmit = v.findViewById(R.id.btnSubmitIncident)

        setupAutocomplete()

        // Set click listener for Date/Time button
        //btnPickDateTime.setOnClickListener {
        //    Toast.makeText(requireContext(), "DateTime button clicked", Toast.LENGTH_SHORT).show()
        //    openDateTimePickers()
        //}

        // Set click listener for Submit button
        btnSubmit.setOnClickListener { submitIncident() }

        checkLocationPermissionAndSetDefault()

        return v
    }

    private fun setupAutocomplete() {
        val autocompleteFragment = childFragmentManager
            .findFragmentById(R.id.autocompleteFragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        )
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedLat = place.latLng?.latitude
                selectedLng = place.latLng?.longitude
                tvLocation.text = "Location: ${place.address}"
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "Autocomplete error: $status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openDateTimePickers() {
        val cal = Calendar.getInstance()

        val datePicker = DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, d)

            val timePicker = TimePickerDialog(requireContext(), { _, hr, min ->
                cal.set(Calendar.HOUR_OF_DAY, hr)
                cal.set(Calendar.MINUTE, min)
                selectedTimestamp = cal.timeInMillis
                tvTimestamp.text = DateFormat.format("dd MMM yyyy, HH:mm", Date(selectedTimestamp))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)

            timePicker.show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

        datePicker.show()
    }

    private fun checkLocationPermissionAndSetDefault() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setCurrentLocation()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                selectedLat = location.latitude
                selectedLng = location.longitude
                updateLocationText()
            }
        }
    }

    private fun updateLocationText() {
        if (selectedLat != null && selectedLng != null) {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(selectedLat!!, selectedLng!!, 1)
            val address = if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Unknown"
            tvLocation.text = "Location: $address"
        }
    }

    private fun submitIncident() {
        val uid = auth.currentUser?.uid ?: return
        val title = etTitle.text.toString().trim()
        val desc = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show()
            return
        }
        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), "Description required", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedLat == null || selectedLng == null) {
            Toast.makeText(requireContext(), "Location required", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTimestamp == 0L) {
            Toast.makeText(requireContext(), "Time required", Toast.LENGTH_SHORT).show()
            return
        }

        val key = dbRef.push().key ?: return
        var address = "Unknown"
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(selectedLat!!, selectedLng!!, 1)
            if (!addresses.isNullOrEmpty()) {
                address = addresses[0].getAddressLine(0)
            }
        } catch (_: Exception) {}

        val incident = Incident(
            id = key,
            userId = uid,
            timestamp = selectedTimestamp,
            title = title,
            description = desc,
            latitude = selectedLat,
            longitude = selectedLng,
            address = address,
            likes = 0
        )

        dbRef.child(key).setValue(incident).addOnSuccessListener {
            Toast.makeText(requireContext(), "Incident reported", Toast.LENGTH_SHORT).show()
            onIncidentSubmitted()
            dismiss()
        }
    }
}
