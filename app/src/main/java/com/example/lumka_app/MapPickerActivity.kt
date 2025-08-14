package com.example.lumka_app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.location.Geocoder
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var selectedLatLng: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST = 1001
    private lateinit var etSearchLocation: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        etSearchLocation = findViewById(R.id.etSearchLocation)

        etSearchLocation.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchLocation(query)
                }
                true
            } else false
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnConfirm: FloatingActionButton = findViewById(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            selectedLatLng?.let {
                val result = intent.apply {
                    putExtra("lat", it.latitude)
                    putExtra("lng", it.longitude)
                }
                setResult(RESULT_OK, result)
                finish()
            } ?: Toast.makeText(this, "Select a location first", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }

        // Add marker on map tap
        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            selectedLatLng = latLng
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun enableUserLocation() {
        map.isMyLocationEnabled = true
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val userLatLng = LatLng(loc.latitude, loc.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            } else {
                // fallback if location is null
                val defaultLoc = LatLng(-26.2041, 28.0473)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f))
            }
        }.addOnFailureListener {
            val defaultLoc = LatLng(-26.2041, 28.0473)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f))
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied, using default location. Please enable location permission on setting", Toast.LENGTH_SHORT).show()
                val defaultLoc = LatLng(-26.2041, 28.0473)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f))
            }
        }
    }

    private fun searchLocation(query: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val loc = addresses[0]
                val latLng = LatLng(loc.latitude, loc.longitude)
                map.clear()
                map.addMarker(MarkerOptions().position(latLng).title(loc.featureName ?: query))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                selectedLatLng = latLng
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}