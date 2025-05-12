package edu.msoe.gilla.finalapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import edu.msoe.gilla.finalapp.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationHelper: LocationHelper
    private lateinit var femaApiService: FemaApiService
    private var map: GoogleMap? = null

    private var alerts = mutableListOf<Alert>()
    private var selectedState = "OR"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(
                this,
                "Location permission is required for disaster alerts",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    data class Alert(
        val title: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val radiusKm: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationHelper = LocationHelper(this)
        femaApiService = FemaApiService()

        setupMap()
        setupSpinner()
//        setupButton()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            checkLocationPermission()
        }
    }

    private fun setupSpinner() {
        val spinner = binding.stateSpinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.us_states,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val defaultIndex = adapter.getPosition("OR")
        spinner.setSelection(defaultIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val newState = parent.getItemAtPosition(position).toString()
                if (selectedState != newState) {
                    selectedState = newState
                    binding.alertText.text = "Loading..."
                    fetchFemaAlerts()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

//    private fun setupButton() {
//        binding.testNotificationButton.setOnClickListener {
//            binding.alertText.text = "Loading..."
//            fetchFemaAlerts()
//        }
//    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(
                    this,
                    "Location permission is required for disaster alerts",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocation() {
        if (locationHelper.hasLocationPermission()) {
            locationHelper.getLastLocation()
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        updateUIWithLocation(it)
                        fetchFemaAlerts()
                    } ?: run {
                        binding.alertText.text = "No active alerts (location unknown)"
                    }
                }
                .addOnFailureListener { e ->
                    binding.alertText.text = "No active alerts (location error)"
                    Log.e("LOCATION_ERROR", "Error getting location: ${e.message}")
                }
        }
    }

    private fun updateUIWithLocation(location: Location) {
        val lat = "%.4f".format(location.latitude)
        val lon = "%.4f".format(location.longitude)
        binding.locationText.text = "My Location > Lat: $lat, Long: $lon"
    }

    private fun fetchFemaAlerts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fetchedAlerts = femaApiService.fetchAlerts(selectedState)
                withContext(Dispatchers.Main) {
                    alerts.clear()
                    alerts.addAll(fetchedAlerts)
                    updateUIWithAlerts()
                }
            } catch (e: Exception) {
                Log.e("FEMA_API", "Error fetching FEMA alerts: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.alertText.text = "Failed to fetch alerts."
                }
            }
        }
    }

    private fun updateUIWithAlerts() {
        if (alerts.isNotEmpty()) {
            val alert = alerts.random()
            binding.alertText.text = "${alert.title}\n\n${alert.description}\nLat: ${alert.latitude}, Lon: ${alert.longitude}"
            showAlertOnMap(alert)
        } else {
            binding.alertText.text = "No active alerts."
            map?.clear()
        }
    }

    private fun showAlertOnMap(alert: Alert) {
        map?.let { googleMap ->
            googleMap.clear()
            val alertLocation = LatLng(alert.latitude, alert.longitude)

            googleMap.addMarker(
                MarkerOptions()
                    .position(alertLocation)
                    .title(alert.title)
            )

            googleMap.addCircle(
                CircleOptions()
                    .center(alertLocation)
                    .radius(alert.radiusKm * 1000)
                    .strokeColor(ContextCompat.getColor(this, R.color.purple_500))
                    .fillColor(ContextCompat.getColor(this, R.color.purple_200))
                    .strokeWidth(4f)
            )

            val zoomLevel = when {
                alert.radiusKm > 100 -> 5f
                alert.radiusKm > 50 -> 7f
                alert.radiusKm > 20 -> 8f
                alert.radiusKm > 10 -> 9f
                else -> 10f
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(alertLocation, zoomLevel))
        }
    }
}
