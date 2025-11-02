package utar.edu.my.fyp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.*
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.*
import okhttp3.OkHttpClient
import org.json.JSONObject
import okhttp3.Request
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.os.Handler
import androidx.appcompat.app.AlertDialog
data class OpeningHours(
    val isOpen: Boolean?,
    val periods: List<Period>?,
    val weekdayText: List<String>?
)

data class Period(
    val open: TimeInfo,
    val close: TimeInfo?
)

data class TimeInfo(
    val day: Int,
    val time: String
)

data class ClinicInfo(
    val name: String,
    val location: LatLng,
    val distanceInKm: Float,
    val address: String?,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val isOpenNow: Boolean?,
    val placeId: String? = null,
    val photoReferences: List<String> = emptyList(),
    val openingHours: OpeningHours? = null
)



class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var placesClient: PlacesClient

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var openDrawerBtn: ImageButton
    private var currentLatLng: LatLng? = null
    private lateinit var closeMapBtn: ImageButton


    private var nearbyClinics = mutableListOf<ClinicInfo>()
    private var otherClinics = mutableListOf<ClinicInfo>()

    private lateinit var clinicListView: ListView
    private var allClinics = mutableListOf<ClinicInfo>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "MAP API")
        }
        placesClient = Places.createClient(this)

        // Setup drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        openDrawerBtn = findViewById(R.id.openDrawerBtn)
        clinicListView = findViewById(R.id.clinicListView)
        val searchClinicView = findViewById<SearchView>(R.id.searchClinicView)
        val spinnerDistance = findViewById<Spinner>(R.id.spinnerDistance)
        val spinnerStatus = findViewById<Spinner>(R.id.spinnerStatus)
        val spinnerRating = findViewById<Spinner>(R.id.spinnerRating)
        closeMapBtn = findViewById(R.id.closeMapBtn)

        ArrayAdapter.createFromResource(
            this,
            R.array.filter_distance,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDistance.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.filter_status,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.filter_rating,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRating.adapter = adapter
        }

        val filterChangedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyClinicFilters(searchClinicView.query?.toString() ?: "")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerDistance.onItemSelectedListener = filterChangedListener
        spinnerStatus.onItemSelectedListener = filterChangedListener
        spinnerRating.onItemSelectedListener = filterChangedListener


        openDrawerBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        //Diagnose navigation
        checkForDiagnosisNavigation()

        searchClinicView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filtered = allClinics.filter {
                    it.name.contains(newText ?: "", ignoreCase = true)
                }

                clinicListView.adapter = ArrayAdapter(
                    this@MapsActivity,
                    R.layout.clinic_list_item,
                    R.id.clinicNameText,
                    filtered.map { "${it.name} (${String.format("%.1f", it.distanceInKm)} km)" }
                )

                clinicListView.setOnItemClickListener { _, _, position, _ ->
                    handleClinicClick(filtered[position])
                }

                return true
            }
        })

        closeMapBtn.setOnClickListener {
            navigateBackToDashboard()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            moveToCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        mMap.setOnMarkerClickListener { marker ->
            val clinic = marker.tag as? ClinicInfo
            if (clinic != null) {
                showClinicBottomSheet(clinic)
            }
            true
        }
    }

    private fun moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 15f))
                val marker = mMap.addMarker(MarkerOptions().position(currentLatLng!!).title("You are here"))
                marker?.showInfoWindow()


                loadNearbyClinics(currentLatLng!!)
                loadAllClinics()
            } else {
                requestLocationUpdate()
                loadAllClinics()
            }
        }
    }

    private fun requestLocationUpdate() {
        locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = 1000
            fastestInterval = 500
            numUpdates = 1
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 15f))
                    mMap.addMarker(MarkerOptions().position(currentLatLng!!).title("You are here"))
                    loadNearbyClinics(currentLatLng!!)
                    loadAllClinics()
                } else {
                    Toast.makeText(this@MapsActivity, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    private fun loadNearbyClinics(location: LatLng) {
        val apiKey = "MAP API"
        val radius = 10000

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${location.latitude},${location.longitude}" +
                "&radius=$radius" +
                "&type=veterinary_care" +
                "&key=$apiKey"

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val jsonData = response.body?.string()

                if (jsonData != null) {
                    val jsonObject = JSONObject(jsonData)
                    val results = jsonObject.getJSONArray("results")

                    nearbyClinics.clear()
                    otherClinics.clear()

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val name = place.getString("name")
                        val geometry = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = geometry.getDouble("lat")
                        val lng = geometry.getDouble("lng")
                        val clinicLatLng = LatLng(lat, lng)

                        val address = place.optString("vicinity", null)
                        val rating = if (place.has("rating")) place.getDouble("rating") else null
                        val userRatings = if (place.has("user_ratings_total")) place.getInt("user_ratings_total") else null

                        // Get basic open now status
                        val basicOpenNow = place.optJSONObject("opening_hours")?.optBoolean("open_now")

                        val photoRefs = mutableListOf<String>()
                        val photosArray = place.optJSONArray("photos")
                        if (photosArray != null) {
                            for (j in 0 until photosArray.length()) {
                                val ref = photosArray.getJSONObject(j).optString("photo_reference")
                                if (!ref.isNullOrEmpty()) {
                                    photoRefs.add(ref)
                                }
                            }
                        }

                        val distance = FloatArray(1)
                        Location.distanceBetween(location.latitude, location.longitude, lat, lng, distance)
                        val distanceInKm = distance[0] / 1000f

                        val placeId = place.optString("place_id")
                        val clinic = ClinicInfo(
                            name, clinicLatLng, distanceInKm, address,
                            rating, userRatings, basicOpenNow, placeId, photoRefs
                        )

                        if (distanceInKm <= 10) {
                            nearbyClinics.add(clinic)
                        } else {
                            otherClinics.add(clinic)
                        }
                    }

                    // Fetch detailed opening hours for all clinics
                    val allClinicsTemp = nearbyClinics + otherClinics
                    fetchDetailedOpeningHours(allClinicsTemp) { clinicsWithHours ->
                        runOnUiThread {
                            // Update the lists with detailed hours
                            nearbyClinics.clear()
                            otherClinics.clear()

                            for (clinic in clinicsWithHours) {
                                if (clinic.distanceInKm <= 10) {
                                    nearbyClinics.add(clinic)
                                } else {
                                    otherClinics.add(clinic)
                                }
                            }

                            // Clear existing map markers
                            mMap.clear()
                            val marker = mMap.addMarker(MarkerOptions().position(location).title("You are here"))
                            marker?.showInfoWindow()

                            // Combine both lists
                            val combinedClinics = (nearbyClinics + otherClinics).sortedBy { it.distanceInKm }
                            allClinics.clear()
                            allClinics.addAll(combinedClinics)

                            // Show all clinics on the map
                            for (clinic in combinedClinics) {
                                val marker = mMap.addMarker(
                                    MarkerOptions()
                                        .position(clinic.location)
                                        .title("${clinic.name} (${String.format("%.1f", clinic.distanceInKm)} km)")
                                )
                                marker?.tag = clinic
                            }

                            // Update the single combined ListView
                            clinicListView.adapter = ArrayAdapter(
                                this@MapsActivity,
                                R.layout.clinic_list_item,
                                R.id.clinicNameText,
                                combinedClinics.map { "${it.name} (${String.format("%.1f", it.distanceInKm)} km)" }
                            )

                            clinicListView.setOnItemClickListener { _, _, position, _ ->
                                val clinic = combinedClinics[position]
                                handleClinicClick(clinic)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to fetch nearby clinics.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun handleClinicClick(clinic: ClinicInfo) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clinic.location, 16f))

        val existingMarker = mMap.addMarker(
            MarkerOptions()
                .position(clinic.location)
                .title(clinic.name)
        )
        existingMarker?.tag = clinic
        existingMarker?.showInfoWindow()
        drawerLayout.closeDrawer(GravityCompat.START)

        showClinicBottomSheet(clinic)
    }

    private fun checkForDiagnosisNavigation() {
        val fromDiagnosis = intent.getBooleanExtra("from_diagnosis", false)
        if (fromDiagnosis) {
            Handler(Looper.getMainLooper()).postDelayed({
                showNearestClinicAlert()
            }, 1000) // Delay to allow map to load first
        }
    }

    private fun showNearestClinicAlert() {
        if (allClinics.isEmpty() || currentLatLng == null) {
            // If no clinics loaded yet, try again after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                showNearestClinicAlert()
            }, 2000)
            return
        }

        val nearestClinic = allClinics.minByOrNull { it.distanceInKm }

        if (nearestClinic != null) {
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Navigate to Nearest Clinic?")
                .setMessage("Based on your pet's diagnosis, would you like to navigate directly to the nearest veterinary clinic?\n\n${nearestClinic.name}\n${String.format("%.1f", nearestClinic.distanceInKm)} km away")
                .setPositiveButton("Navigate Now") { _, _ ->
                    // Navigate to the nearest clinic
                    val uri = "google.navigation:q=${nearestClinic.location.latitude},${nearestClinic.location.longitude}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    intent.setPackage("com.google.android.apps.maps")
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Show on Map") { _, _ ->
                    // Just focus on the nearest clinic on the map
                    handleClinicClick(nearestClinic)
                }
                .setCancelable(true)
                .create()

            alertDialog.show()

            // Style the buttons after showing the dialog
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setBackgroundColor(ContextCompat.getColor(this@MapsActivity, android.R.color.holo_green_dark))
                setTextColor(ContextCompat.getColor(this@MapsActivity, android.R.color.white))
                setPadding(24, 16, 24, 16)
            }

            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setBackgroundColor(ContextCompat.getColor(this@MapsActivity, android.R.color.holo_blue_dark))
                setTextColor(ContextCompat.getColor(this@MapsActivity, android.R.color.white))
                setPadding(24, 16, 24, 16)
            }
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
                moveToCurrentLocation()
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadAllClinics() {
        val apiKey = "MAP API"
        val radius = 50000

        val current = currentLatLng
        if (current == null) {
            runOnUiThread {
                Toast.makeText(this, "User location unavailable", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${current.latitude},${current.longitude}" +
                "&radius=$radius" +
                "&type=veterinary_care" +
                "&key=$apiKey"

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val jsonData = response.body?.string()

                if (jsonData != null) {
                    val jsonObject = JSONObject(jsonData)
                    val results = jsonObject.getJSONArray("results")

                    val tempClinics = mutableListOf<ClinicInfo>()

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val name = place.getString("name")
                        val geometry = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = geometry.getDouble("lat")
                        val lng = geometry.getDouble("lng")
                        val clinicLatLng = LatLng(lat, lng)

                        val address = place.optString("vicinity", null)
                        val rating = if (place.has("rating")) place.getDouble("rating") else null
                        val userRatings = if (place.has("user_ratings_total")) place.getInt("user_ratings_total") else null
                        val basicOpenNow = place.optJSONObject("opening_hours")?.optBoolean("open_now")

                        val photoRefs = mutableListOf<String>()
                        val photosArray = place.optJSONArray("photos")
                        if (photosArray != null) {
                            for (j in 0 until photosArray.length()) {
                                val ref = photosArray.getJSONObject(j).optString("photo_reference")
                                if (!ref.isNullOrEmpty()) {
                                    photoRefs.add(ref)
                                }
                            }
                        }

                        val distance = FloatArray(1)
                        Location.distanceBetween(
                            current.latitude, current.longitude,
                            lat, lng, distance
                        )
                        val distanceInKm = distance[0] / 1000f

                        if (distanceInKm <= 50) {
                            val placeId = place.optString("place_id")
                            val clinic = ClinicInfo(
                                name, clinicLatLng, distanceInKm, address,
                                rating, userRatings, basicOpenNow, placeId, photoRefs
                            )
                            tempClinics.add(clinic)
                        }
                    }

                    // Fetch detailed opening hours
                    fetchDetailedOpeningHours(tempClinics) { clinicsWithHours ->
                        runOnUiThread {
                            allClinics.clear()
                            allClinics.addAll(clinicsWithHours)
                            allClinics.sortBy { it.distanceInKm }

                            // Show all clinics on map
                            for (clinic in allClinics) {
                                val marker = mMap.addMarker(
                                    MarkerOptions()
                                        .position(clinic.location)
                                        .title("${clinic.name} (${String.format("%.1f", clinic.distanceInKm)} km)")
                                )
                                marker?.tag = clinic
                            }

                            // Display all clinics in the ListView
                            clinicListView.adapter = ArrayAdapter(
                                this@MapsActivity,
                                R.layout.clinic_list_item,
                                R.id.clinicNameText,
                                allClinics.map { "${it.name} (${String.format("%.1f", it.distanceInKm)} km)" }
                            )

                            clinicListView.setOnItemClickListener { _, _, position, _ ->
                                val clinic = allClinics[position]
                                handleClinicClick(clinic)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to load clinics in Malaysia", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }




    private fun getPhotoUrl(photoRef: String): String {
            val apiKey = "MAP API"
            return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=$photoRef&key=$apiKey"
        }



    private fun showClinicBottomSheet(clinic: ClinicInfo) {
        val view = layoutInflater.inflate(R.layout.custom_clinic_bottom_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        val nameText = view.findViewById<TextView>(R.id.clinicName)
        val addressText = view.findViewById<TextView>(R.id.clinicAddress)
        val distanceText = view.findViewById<TextView>(R.id.clinicDistance)
        val ratingText = view.findViewById<TextView>(R.id.clinicRating)
        val statusText = view.findViewById<TextView>(R.id.clinicStatus)
        val navigateBtn = view.findViewById<Button>(R.id.navigateButton)

        nameText.text = clinic.name
        addressText.text = clinic.address ?: "Address not available"
        distanceText.text = "${String.format("%.1f", clinic.distanceInKm)} km away"

        ratingText.text = if (clinic.rating != null && clinic.userRatingsTotal != null)
            "⭐ ${clinic.rating} (${clinic.userRatingsTotal} reviews)"
        else
            "Rating not available"

        // Set real-time status
        when (clinic.isOpenNow) {
            true -> {
                statusText.text = "Open now"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
            false -> {
                statusText.text = "Closed now"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            null -> {
                statusText.text = "Hours unknown"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }

        val imageSlider = view.findViewById<ViewPager2>(R.id.imageSlider)

        fetchPlacePhotos(clinic.placeId ?: "") { imageUrls ->
            val safeImageUrls = if (imageUrls.isEmpty()) {
                mutableListOf("https://via.placeholder.com/600x300?text=No+Image")
            } else {
                imageUrls.toMutableList()
            }
            imageSlider.adapter = ClinicImageAdapter(safeImageUrls)
        }

        navigateBtn.setOnClickListener {
            val uri = "google.navigation:q=${clinic.location.latitude},${clinic.location.longitude}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun fetchPlacePhotos(placeId: String, callback: (List<String>) -> Unit) {
        val apiKey = "MAP API"
        val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=$placeId&fields=photo&key=$apiKey"

        Thread {
            val photoUrls = mutableListOf<String>()
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (body != null) {
                    val result = JSONObject(body).optJSONObject("result")
                    val photos = result?.optJSONArray("photos")
                    if (photos != null) {
                        for (i in 0 until photos.length()) {
                            val ref = photos.getJSONObject(i).optString("photo_reference")
                            if (!ref.isNullOrEmpty()) {
                                photoUrls.add(getPhotoUrl(ref))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                callback(photoUrls)
            }
        }.start()
    }

    private fun applyClinicFilters(searchQuery: String) {
        val spinnerDistance = findViewById<Spinner>(R.id.spinnerDistance)
        val spinnerStatus = findViewById<Spinner>(R.id.spinnerStatus)
        val spinnerRating = findViewById<Spinner>(R.id.spinnerRating)

        val selectedDistance = spinnerDistance.selectedItem.toString()
        val selectedStatus = spinnerStatus.selectedItem.toString()
        val selectedRating = spinnerRating.selectedItem.toString()

        val filtered = allClinics.filter { clinic ->
            val matchesName = clinic.name.contains(searchQuery, ignoreCase = true)

            val passDistance = when (selectedDistance) {
                "≤ 5 km" -> clinic.distanceInKm <= 5
                "≤ 10 km" -> clinic.distanceInKm <= 10
                "≤ 20 km" -> clinic.distanceInKm <= 20
                "> 20 km" -> clinic.distanceInKm > 20
                else -> true
            }

            val passStatus = when (selectedStatus) {
                "Open Now" -> clinic.isOpenNow == true
                else -> true
            }

            val passRating = when (selectedRating) {
                "≥ 3.5" -> clinic.rating != null && clinic.rating >= 3.5
                "≥ 4.0" -> clinic.rating != null && clinic.rating >= 4.0
                "≥ 4.5" -> clinic.rating != null && clinic.rating >= 4.5
                else -> true
            }

            matchesName && passDistance && passStatus && passRating
        }

        clinicListView.adapter = ArrayAdapter(
            this,
            R.layout.clinic_list_item,
            R.id.clinicNameText,
            filtered.map { "${it.name} (${String.format("%.1f", it.distanceInKm)} km)" }
        )

        clinicListView.setOnItemClickListener { _, _, position, _ ->
            handleClinicClick(filtered[position])
        }
    }

    private fun fetchDetailedOpeningHours(clinics: List<ClinicInfo>, callback: (List<ClinicInfo>) -> Unit) {
        val apiKey = "MAP API"
        val updatedClinics = mutableListOf<ClinicInfo>()
        var processedCount = 0

        if (clinics.isEmpty()) {
            callback(emptyList())
            return
        }

        for (clinic in clinics) {
            if (clinic.placeId.isNullOrEmpty()) {
                updatedClinics.add(clinic)
                processedCount++
                if (processedCount == clinics.size) {
                    callback(updatedClinics)
                }
                continue
            }

            val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=${clinic.placeId}&fields=opening_hours&key=$apiKey"

            Thread {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    var updatedClinic = clinic
                    if (body != null) {
                        val result = JSONObject(body).optJSONObject("result")
                        val openingHours = result?.optJSONObject("opening_hours")

                        if (openingHours != null) {
                            val isOpenNow = openingHours.optBoolean("open_now")

                            // Create updated clinic with real-time open status
                            updatedClinic = clinic.copy(isOpenNow = isOpenNow)
                        }
                    }

                    synchronized(updatedClinics) {
                        updatedClinics.add(updatedClinic)
                        processedCount++
                        if (processedCount == clinics.size) {
                            callback(updatedClinics.sortedBy { it.distanceInKm })
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    synchronized(updatedClinics) {
                        updatedClinics.add(clinic)
                        processedCount++
                        if (processedCount == clinics.size) {
                            callback(updatedClinics.sortedBy { it.distanceInKm })
                        }
                    }
                }
            }.start()
        }
    }


    private fun navigateBackToDashboard() {
        val intent = Intent(this, DashboardPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

}
