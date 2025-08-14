package com.example.lumka_app

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.cardview.widget.CardView
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

    private lateinit var displayName: TextView
    private lateinit var displayEmail: TextView
    private lateinit var initials: TextView

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvLocation: TextView
    private lateinit var tvLocationFilter: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var btnSelectLocation: ImageButton
    private lateinit var btnSelectLocationFilter: ImageButton
    private lateinit var btnPickDateTime: ImageButton
    private lateinit var btnSubmit: Button
    private lateinit var rvIncidents: RecyclerView

    private lateinit var adapter: IncidentAdapter
    private val incidents = mutableListOf<Incident>()

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var selectedTimestamp: Long = System.currentTimeMillis()

    private var filterLat: Double? = null
    private var filterLng: Double? = null

    private val MAP_PICKER_REQUEST = 101
    private val MAP_PICKER_FILTER_REQUEST = 102
    private val LOCATION_PERMISSION_REQUEST = 1001

    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("incidents") }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyBhITPUEQqXfIXsM5EOTZPh3bHH_SclJZ8")
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_danger_alerts, container, false)

        val btnShowForm = v.findViewById<Button>(R.id.btnShowForm)
        val formContainer = v.findViewById<CardView>(R.id.formContainer)

        auth = FirebaseAuth.getInstance()
        btnShowForm.setOnClickListener {
            formContainer.visibility = View.VISIBLE
            btnShowForm.visibility = View.GONE
        }

        // --- UI ---
        displayName = v.findViewById(R.id.displayName)
        displayEmail = v.findViewById(R.id.displayEmail)
        initials = v.findViewById(R.id.initial)

        etTitle = v.findViewById(R.id.etTitle)
        etDescription = v.findViewById(R.id.etDescription)
        tvLocation = v.findViewById(R.id.tvLocation)
        tvLocationFilter = v.findViewById(R.id.tvLocationFilter)
        tvTimestamp = v.findViewById(R.id.tvTimestamp)
        btnSelectLocation = v.findViewById(R.id.btnSelectLocation)
        btnSelectLocationFilter = v.findViewById(R.id.btnSelectLocationFilter)
        btnPickDateTime = v.findViewById(R.id.btnPickDateTime)
        btnSubmit = v.findViewById(R.id.btnSubmitIncident)
        rvIncidents = v.findViewById(R.id.rvIncidents)

        fetchUserDetails()
        observeIncidents()

        rvIncidents.layoutManager = LinearLayoutManager(requireContext())
        adapter = IncidentAdapter(incidents) { thumbsUpIncident(it) }
        rvIncidents.adapter = adapter

        updateTimestampLabel()
        setupAutocomplete()
        setupFilterAutocomplete()
        checkLocationPermissionAndSetDefault()

        btnPickDateTime.setOnClickListener { openDateTimePickers() }
        btnSubmit.setOnClickListener { submitIncident() }

        btnSelectLocation.setOnClickListener {
            val intent = Intent(requireContext(), MapPickerActivity::class.java)
            startActivityForResult(intent, MAP_PICKER_REQUEST)
        }

        btnSelectLocationFilter.setOnClickListener {
            val intent = Intent(requireContext(), MapPickerActivity::class.java)
            startActivityForResult(intent, MAP_PICKER_FILTER_REQUEST)
        }


        return v
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

    private fun setupAutocomplete() {
        val autocompleteFragment = childFragmentManager
            .findFragmentById(R.id.autocompleteFragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                selectedLat = place.latLng?.latitude
                selectedLng = place.latLng?.longitude
                tvLocation.text = "Location: ${place.address}"
                tvLocationFilter.text = "Filter Location: ${place.address}"

            }
            override fun onError(status: com.google.android.gms.common.api.Status) {
                //Toast.makeText(requireContext(), "Autocomplete error: $status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupFilterAutocomplete() {
        val autocompleteFilter = childFragmentManager
            .findFragmentById(R.id.autocompleteFragmentFilter) as AutocompleteSupportFragment
        autocompleteFilter.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFilter.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                filterLat = place.latLng?.latitude
                filterLng = place.latLng?.longitude
                //tvLocation.text = "Filter Location: ${place.address}"
                tvLocationFilter.text = "Filter Location: ${place.address}"

                applyDistanceFilter()
            }
            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(requireContext(), "Autocomplete filter error: $status", Toast.LENGTH_SHORT).show()
            }
        })
    }


    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun checkLocationPermissionAndSetDefault() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

                // Set reporting location
                updateLocationText(tvLocation, selectedLat, selectedLng)

                // Set filter location at startup
                filterLat = selectedLat
                filterLng = selectedLng
                updateLocationText(tvLocationFilter, filterLat, filterLng, prefix = "Filter Location")

                // Apply filter with this location
                applyDistanceFilter()
            }
        }
    }


    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return

        when(requestCode) {
            MAP_PICKER_REQUEST -> {
                selectedLat = data.getDoubleExtra("lat", 0.0)
                selectedLng = data.getDoubleExtra("lng", 0.0)
                updateLocationText(tvLocation, selectedLat, selectedLng)
            }
            MAP_PICKER_FILTER_REQUEST -> {
                filterLat = data.getDoubleExtra("lat", 0.0)
                filterLng = data.getDoubleExtra("lng", 0.0)
                //(tvLocation, filterLat, filterLng, prefix = "Filter Location")
                updateLocationText(tvLocationFilter, filterLat, filterLng, prefix = "Filter Location")
                applyDistanceFilter()
            }
        }
    }

    private fun updateLocationText(tv: TextView, lat: Double?, lng: Double?, prefix: String = "Location") {
        if (lat != null && lng != null) {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val address = if (!addresses.isNullOrEmpty()) addresses[0].getAddressLine(0) else "Unknown"
            tv.text = "$prefix: $address"
        }
    }

    private fun openDateTimePickers() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(requireContext(), { _, hr, min ->
                cal.set(Calendar.HOUR_OF_DAY, hr)
                cal.set(Calendar.MINUTE, min)
                selectedTimestamp = cal.timeInMillis
                updateTimestampLabel()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateTimestampLabel() {
        tvTimestamp.text = "${DateFormat.format("dd MMM yyyy, HH:mm", Date(selectedTimestamp))}"
    }

    private fun submitIncident() {
        val uid = auth.currentUser?.uid ?: return
        val title = etTitle.text.toString().trim()
        val desc = etDescription.text.toString().trim()

        // Check title
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show()
            return
        }

        // Check description
        if (desc.isEmpty()) {
            Toast.makeText(requireContext(), "Description required", Toast.LENGTH_SHORT).show()
            return
        }

        // Check location
        if (selectedLat == null || selectedLng == null) {
            Toast.makeText(requireContext(), "Location required", Toast.LENGTH_SHORT).show()
            return
        }

        // Check timestamp
        if (selectedTimestamp == 0L) {
            Toast.makeText(requireContext(), "Time required", Toast.LENGTH_SHORT).show()
            return
        }

        val key = dbRef.push().key ?: return

        // Get address from lat/lng
        var address = "Unknown location"
        if (selectedLat != null && selectedLng != null) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(selectedLat!!, selectedLng!!, 1)
                if (!addresses.isNullOrEmpty()) {
                    address = addresses[0].getAddressLine(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val incident = Incident(
            id = key,
            userId = uid,
            timestamp = selectedTimestamp,
            title = title,
            description = desc,
            latitude = selectedLat,
            longitude = selectedLng,
            address = address,  // <--- store it
            likes = 0
        )

        dbRef.child(key).setValue(incident).addOnSuccessListener {
            Toast.makeText(requireContext(), "Incident reported", Toast.LENGTH_SHORT).show()
            etTitle.setText("")
            etDescription.setText("")

            // Hide form and show the button again
            val btnShowForm = view?.findViewById<Button>(R.id.btnShowForm)
            val formContainer = view?.findViewById<CardView>(R.id.formContainer)

            if (formContainer != null) {
                formContainer.visibility = View.GONE
            }
            if (btnShowForm != null) {
                btnShowForm.visibility = View.VISIBLE
            }

        }
    }


    private fun observeIncidents() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Incident>()
                for (child in snapshot.children) {
                    val incident = child.getValue(Incident::class.java)
                    incident?.let { inc ->
                        inc.id = inc.id ?: child.key

                        // Fetch user initial
                        inc.userId?.let { uid ->
                            val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val username = userSnapshot.child("username").getValue(String::class.java)
                                    inc.userInitial = username?.firstOrNull()?.toString() ?: "?"
                                    adapter.notifyItemChanged(list.indexOf(inc))
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                        }

                        list.add(inc)
                    }
                }

                // --- SORTING ---
                if (selectedLat != null && selectedLng != null) {
                    list.forEach { incident ->
                        val lat = incident.latitude
                        val lng = incident.longitude
                        if (lat != null && lng != null) {
                            incident.distanceKm = distanceInMeters(
                                selectedLat!!, selectedLng!!, lat, lng
                            ) / 1000f
                        }
                    }
                    list.sortBy { it.distanceKm } // nearest first
                } else {
                    list.sortByDescending { it.timestamp ?: 0L } // newest first
                }

                // Update main incidents list and apply filter
                incidents.clear()
                incidents.addAll(list)
                applyDistanceFilter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load incidents", Toast.LENGTH_SHORT).show()
            }
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

    private fun distanceInMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val loc1 = Location("").apply { latitude = lat1; longitude = lng1 }
        val loc2 = Location("").apply { latitude = lat2; longitude = lng2 }
        return loc1.distanceTo(loc2)
    }

    private fun applyDistanceFilter(maxDistanceKm: Float = 50f) {
        val filteredList = incidents.map { incident ->
            val lat = incident.latitude
            val lng = incident.longitude
            if (lat != null && lng != null && filterLat != null && filterLng != null) {
                incident.distanceKm = distanceInMeters(filterLat!!, filterLng!!, lat, lng) / 1000f
            }
            incident
        }.filter { it.distanceKm == null || it.distanceKm!! <= maxDistanceKm }

        // Sort by distance before submitting
        val sortedFilteredList = filteredList.sortedBy { it.distanceKm ?: Float.MAX_VALUE }

        adapter.submitList(sortedFilteredList)

        // Expand RecyclerView height to show all items
        rvIncidents.post {
            val adapter = rvIncidents.adapter ?: return@post
            var totalHeight = 0
            for (i in 0 until adapter.itemCount) {
                val holder = adapter.createViewHolder(rvIncidents, adapter.getItemViewType(i))
                adapter.onBindViewHolder(holder, i)
                holder.itemView.measure(
                    View.MeasureSpec.makeMeasureSpec(rvIncidents.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.UNSPECIFIED
                )
                val lp = holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams
                val itemHeight = holder.itemView.measuredHeight + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)
                totalHeight += itemHeight
            }
            val params = rvIncidents.layoutParams
            params.height = totalHeight + rvIncidents.paddingTop + rvIncidents.paddingBottom
            rvIncidents.layoutParams = params
        }
    }

    fun getFirstCharacter(username: String?): String {
        return username?.firstOrNull()?.toString() ?: ""
    }


}
