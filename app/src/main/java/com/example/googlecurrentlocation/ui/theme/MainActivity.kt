package com.example.googlecurrentlocation.ui.theme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.googlecurrentlocation.R
import com.example.googlecurrentlocation.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    private val permissionCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkLocationPermissions()) {
            return
        }

        initializeApp()
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionExplanationDialog()
            } else {
                requestLocationPermissions()
            }
            return false
        }
        return true
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permission to show your current location on the map. Please grant the permission to continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestLocationPermissions()
            }
            .setNegativeButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            permissionCode
        )
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
        finish()
    }

    private fun initializeApp() {

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        getCurrentLocationUser()


        val btnMapType: FloatingActionButton = findViewById(R.id.btnMapType)
        btnMapType.setOnClickListener { showMapTypeMenu(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionCode) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                initializeApp()
            } else {
                showPermissionExplanationDialog()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationUser() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                showLocationOnMap()
            } else {
                requestNewLocationData()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Error fetching location", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2000
            numUpdates = 1
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (locationResult.locations.isNotEmpty()) {
                    currentLocation = locationResult.locations[0]
                    showLocationOnMap()
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun showLocationOnMap() {
        currentLocation?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            val markerOptions = MarkerOptions().position(latLng).title("Current Location")

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            mMap.addMarker(markerOptions)

            Toast.makeText(this, "Location: ${it.latitude}, ${it.longitude}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMapTypeMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.map_type_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.normal -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                R.id.satellite -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                R.id.terrain -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                R.id.hybrid -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
            true
        }

        popupMenu.show()
    }
}