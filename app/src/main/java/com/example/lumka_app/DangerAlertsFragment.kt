package com.example.lumka_app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.database.FirebaseDatabase

class DangerAlertsFragment : Fragment() {

    private lateinit var rvIncidents: RecyclerView
    private lateinit var adapter: IncidentAdapter
    private val incidents = mutableListOf<Incident>()

    private var filterLat: Double? = null
    private var filterLng: Double? = null

    private val LOCATION_PERMISSION_REQUEST = 1001
    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("incidents") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Places SDK once
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyBhITPUEQqXfIXsM5EOTZPh3bHH_SclJZ8")
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_danger_alerts, container, false)

        rvIncidents = v.findViewById(R.id.rvIncidents)
        rvIncidents.layoutManager = LinearLayoutManager(requireContext())
        rvIncidents = v.findViewById(R.id.rvIncidents)
        rvIncidents.layoutManager = LinearLayoutManager(requireContext())
        adapter = IncidentAdapter(
            incidents,
            onThumbsUp = { incident -> thumbsUpIncident(incident) },
            filterLat = filterLat,
            filterLng = filterLng
        )
        rvIncidents.adapter = adapter
        rvIncidents.adapter = adapter

        val btnShowForm = v.findViewById<Button>(R.id.btnShowForm)
        btnShowForm.setOnClickListener {
            val dialog = ReportIncidentDialogFragment {
                // Refresh RecyclerView when new incident is submitted
                observeIncidents()
            }
            dialog.show(parentFragmentManager, "ReportIncidentDialog")
        }

        // Observe Firebase data
        observeIncidents()

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dynamically add the autocomplete fragment into FrameLayout
        val autocompleteFragment = AutocompleteSupportFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.autocompleteFragmentFilter, autocompleteFragment)
            .commitNow() // commitNow ensures it's ready immediately

        // Configure place fields
        autocompleteFragment.setPlaceFields(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        )

        autocompleteFragment.setHint("Search location")

        // Handle place selection
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i("PlaceSelected", "Selected: ${place.name} (${place.id})")
                filterLat = place.latLng?.latitude
                filterLng = place.latLng?.longitude
                applyDistanceFilter()
            }

            override fun onError(status: Status) {
                Log.e("PlaceError", "Error: $status")
            }
        })
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun checkLocationPermissionAndSetDefault() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setCurrentLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun setCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                filterLat = location.latitude
                filterLng = location.longitude
                applyDistanceFilter()
            }
        }
    }

    private fun applyDistanceFilter(maxDistanceKm: Float = 50f) {
        val filteredList = incidents.filter { incident ->
            val lat = incident.latitude
            val lng = incident.longitude
            if (lat != null && lng != null && filterLat != null && filterLng != null) {
                val distanceKm = distanceInMeters(filterLat!!, filterLng!!, lat, lng) / 1000f
                distanceKm <= maxDistanceKm
            } else true
        }
        adapter.submitList(filteredList)
    }

    private fun distanceInMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val loc1 = Location("").apply { latitude = lat1; longitude = lng1 }
        val loc2 = Location("").apply { latitude = lat2; longitude = lng2 }
        return loc1.distanceTo(loc2)
    }

    private fun thumbsUpIncident(incident: Incident) {
        val id = incident.id ?: return
        dbRef.child(id).child("likes").runTransaction(object :
            com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(curr: com.google.firebase.database.MutableData):
                    com.google.firebase.database.Transaction.Result {
                val current = curr.getValue(Int::class.java) ?: 0
                curr.value = current + 1
                return com.google.firebase.database.Transaction.success(curr)
            }

            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                snapshot: com.google.firebase.database.DataSnapshot?
            ) {}
        })
    }

    private fun observeIncidents() {
        dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<Incident>()
                for (child in snapshot.children) {
                    val incident = child.getValue(Incident::class.java)
                    if (incident != null
                        && !incident.userId.isNullOrEmpty()
                        && !incident.title.isNullOrEmpty()
                        && !incident.description.isNullOrEmpty()
                        && incident.latitude != null
                        && incident.longitude != null
                    ) {
                        list.add(incident)
                    }
                }
                incidents.clear()
                incidents.addAll(list)
                applyDistanceFilter()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

}
