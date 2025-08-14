package com.example.lumka_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var selectedLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnConfirm: FloatingActionButton = findViewById(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            selectedLatLng?.let {
                val result = Intent().apply {
                    putExtra("lat", it.latitude)
                    putExtra("lng", it.longitude)
                }
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Optional: set default location
        val defaultLoc = LatLng(-26.2041, 28.0473) // Johannesburg CBD
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 12f))

        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            selectedLatLng = latLng
        }
    }
}
