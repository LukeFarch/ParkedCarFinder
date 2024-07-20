package com.example.parkedcarfinder

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.example.parkedcarfinder.databinding.ActivityMapsBinding

class MapsFragment : Fragment(), OnMapReadyCallback {

    private var parkLocation: LatLng? = null

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private var marker: Marker? = null

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ActivityMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getLocation()
            } else {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
        }

        // New Code
        binding.parkedButton.setOnClickListener {
            moveCarIconToCurrentLocation()
        }

        Log.d("MapsFragment", "onViewCreated() called. parkLocation: ${parkLocation?.toString() ?: "not set"}")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            setOnMapClickListener { latLng ->
                addOrMoveSelectedPositionMarker(latLng)
            }
        }
        when {
            hasLocationPermission() -> getLocation()
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
            else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        Log.d("MapsFragment", "getLocation() called.")
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val userLocation = LatLng(it.latitude, it.longitude)
                parkLocation = userLocation
                updateMapLocation(userLocation)
                addMarkerAtLocation(userLocation, "Your Location")
                Log.d("MapsFragment", "User location obtained: $userLocation")
            }
        }
    }

    private fun moveCarIconToCurrentLocation() {
        if (hasLocationPermission()) {
            // Check if the user has granted location permissions
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request missing permissions if not granted
                requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                return
            }

            // Get the user's current location
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLocation = LatLng(it.latitude, it.longitude)

                    // Check if a marker already exists
                    if (marker == null) {
                        // Create a new marker at the user's location if no marker exists
                        marker = addMarkerAtLocation(userLocation, "Park Here", getBitmapDescriptorFromVector(R.drawable.car_parking_48))
                        Log.d("MapsFragment", "Marker created at: $userLocation")
                    } else {
                        // Move the existing marker to the user's location
                        marker?.position = userLocation
                        updateMapLocation(userLocation)
                        Log.d("MapsFragment", "Marker moved to: $userLocation")
                    }

                    // Update parkLocation with the new marker location
                    parkLocation = userLocation
                    Log.d("MapsFragment", "parkLocation updated to: $parkLocation")
                }
            }
        } else {
            // Request location permission if not already granted
            requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    private fun addMarkerAtLocation(location: LatLng, title: String, markerIcon: BitmapDescriptor? = null) =
        mMap.addMarker(
            MarkerOptions().title(title).position(location).apply {
                markerIcon?.let { icon(markerIcon) }
            }
        )

    private fun getBitmapDescriptorFromVector(@DrawableRes vectorDrawableResourceId: Int): BitmapDescriptor? {
        val bitmap = ContextCompat.getDrawable(requireContext(), vectorDrawableResourceId)?.let { vectorDrawable ->
            vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
            val drawableWithTint = DrawableCompat.wrap(vectorDrawable)
            DrawableCompat.setTint(drawableWithTint, Color.RED)
            val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawableWithTint.draw(canvas)
            bitmap
        } ?: return null
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun addOrMoveSelectedPositionMarker(latLng: LatLng) {
        if (marker == null) {
            marker = addMarkerAtLocation(latLng, "Park Here", getBitmapDescriptorFromVector(R.drawable.car_parking_48))
        } else {
            marker?.position = latLng
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Location permission")
            .setMessage("We need your permission to find your current position")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }
}
 5 changes: 5 additions & 0 deletions5  
app/src/main/res/drawable/car_parking_40.xml
Original file line number	Original file line	Diff line number	Diff line change
@@ -0,0 +1,5 @@
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="40dp" android:tint="#0A0190" android:viewportHeight="24" android:viewportWidth="24" android:width="40dp">

    <path android:fillColor="@android:color/white" android:pathData="M16.22,12c0.68,0 1.22,-0.54 1.22,-1.22c0,-0.67 -0.54,-1.22 -1.22,-1.22S15,10.11 15,10.78C15,11.46 15.55,12 16.22,12zM6.56,10.78c0,0.67 0.54,1.22 1.22,1.22S9,11.46 9,10.78c0,-0.67 -0.54,-1.22 -1.22,-1.22S6.56,10.11 6.56,10.78zM7.61,4L6.28,8h11.43l-1.33,-4H7.61zM16.28,3c0,0 0.54,0.01 0.92,0.54c0.02,0.02 0.03,0.04 0.05,0.07c0.07,0.11 0.14,0.24 0.19,0.4C17.66,4.66 19,8.69 19,8.69v6.5c0,0.45 -0.35,0.81 -0.78,0.81h-0.44C17.35,16 17,15.64 17,15.19V14H7v1.19C7,15.64 6.65,16 6.22,16H5.78C5.35,16 5,15.64 5,15.19v-6.5c0,0 1.34,-4.02 1.55,-4.69c0.05,-0.16 0.12,-0.28 0.19,-0.4C6.77,3.58 6.78,3.56 6.8,3.54C7.18,3.01 7.72,3 7.72,3H16.28zM4,17.01h16V19h-7v3h-2v-3H4V17.01z"/>

</vector>