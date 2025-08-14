package com.example.lumka_app

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class DangerAlertsFragment : Fragment() {

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvLocation: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var btnSelectLocation: Button
    private lateinit var btnPickDateTime: Button
    private lateinit var btnSubmit: Button
    private lateinit var rvIncidents: RecyclerView

    private lateinit var adapter: IncidentAdapter
    private val incidents = mutableListOf<Incident>()

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedTimestamp: Long = System.currentTimeMillis()

    private val MAP_PICKER_REQUEST = 101
    private val LOCATION_PERMISSION_REQUEST = 1001

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("incidents") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            // Replace with your API key
            Places.initialize(requireContext(), "YOUR_API_KEY_HERE")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_danger_alerts, container, false)

        etTitle = v.findViewById(R.id.etTitle)
        etDescription = v.findViewById(R.id.etDescription)
        tvLocation = v.findViewById(R.id.tvLocation)
        tvTimestamp = v.findViewById(R.id.tvTimestamp)
        btnSelectLocation = v.findViewById(R.id.btnSelectLocation)
        btnPickDateTime = v.findViewById(R.id.btnPickDateTime)
        btnSubmit = v.findViewById(R.id.btnSubmitIncident)
        rvIncidents = v.findViewById(R.id.rvIncidents)

        updateTimestampLabel()
        setupAutocomplete()
        checkLocationPermissionAndSetDefault()

        rvIncidents.layoutManager = LinearLayoutManager(requireContext())
        adapter = IncidentAdapter(incidents) { thumbsUpIncident(it) }
        rvIncidents.adapter = adapter

        btnPickDateTime.setOnClickListener { openDateTimePickers() }
        btnSubmit.setOnClickListener { submitIncident() }

        btnSelectLocation.setOnClickListener {
            val intent = Intent(requireContext(), MapPickerActivity::class.java)
            startActivityForResult(intent, MAP_PICKER_REQUEST)
        }

        observeIncidents()

        return v
    }

    /*** AUTOCOMPLETE ***/
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

    /*** CURRENT LOCATION ***/
    private fun checkLocationPermissionAndSetDefault() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setCurrentLocation()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun setCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                selectedLat = location.latitude
                selectedLng = location.longitude
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(selectedLat!!, selectedLng!!, 1)
                val address = if ((addresses?.isNotEmpty() == true)!!) addresses?.get(0)?.getAddressLine(0) else "Unknown location"
                tvLocation.text = "Location: $address"
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied, Please enable on settings", Toast.LENGTH_SHORT).show()
        }
    }

    /*** MAP PICKER RESULT ***/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAP_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedLat = data?.getDoubleExtra("lat", 0.0)
            selectedLng = data?.getDoubleExtra("lng", 0.0)

            // Convert to address
            if (selectedLat != null && selectedLng != null) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(selectedLat!!, selectedLng!!, 1)
                val address = if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Unknown location"
                tvLocation.text = "Location: $address"
            }
        }
    }

    /*** DATE/TIME PICKER ***/
    private fun openDateTimePickers() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        DatePickerDialog(requireContext(),
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(requireContext(),
                    { _, hr, min ->
                        cal.set(Calendar.HOUR_OF_DAY, hr)
                        cal.set(Calendar.MINUTE, min)
                        selectedTimestamp = cal.timeInMillis
                        updateTimestampLabel()
                    },
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                ).show()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateTimestampLabel() {
        val fmt = DateFormat.format("dd MMM yyyy, HH:mm", Date(selectedTimestamp))
        tvTimestamp.text = "Timestamp: $fmt"
    }

    /*** SUBMIT INCIDENT ***/
    private fun submitIncident() {
        val uid = auth.currentUser?.uid ?: return
        val title = etTitle.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), "Description is required", Toast.LENGTH_SHORT).show()
            return
        }

        val key = dbRef.push().key ?: return
        val incident = Incident(
            id = key,
            userId = uid,
            timestamp = selectedTimestamp,
            title = title,
            description = desc,
            latitude = selectedLat,
            longitude = selectedLng,
            likes = 0
        )

        dbRef.child(key).setValue(incident)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Incident reported", Toast.LENGTH_SHORT).show()
                etTitle.setText("")
                etDescription.setText("")
            }
    }

    /*** OBSERVE INCIDENTS ***/
    private fun observeIncidents() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Incident>()
                for (child in snapshot.children) {
                    val model = child.getValue(Incident::class.java)
                    model?.let { list.add(it.copy(id = it.id ?: child.key)) }
                }
                list.sortByDescending { it.timestamp ?: 0L }
                adapter.submitList(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun thumbsUpIncident(incident: Incident) {
        val id = incident.id ?: return
        dbRef.child(id).child("likes").runTransaction(object : Transaction.Handler {
            override fun doTransaction(curr: MutableData): Transaction.Result {
                val current = curr.getValue(Int::class.java) ?: 0
                curr.value = current + 1
                return Transaction.success(curr)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
    }
}
