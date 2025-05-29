package utar.edu.my.fyp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.*
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

data class ClinicInfo(
    val name: String,
    val location: LatLng,
    val distanceInKm: Float,
    val address: String?,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val isOpenNow: Boolean?,
    val placeId: String? = null,
    val photoReferences: List<String> = emptyList()
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

    private lateinit var nearbyClinicListView: ListView
    private lateinit var otherClinicListView: ListView

    private var nearbyClinics = mutableListOf<ClinicInfo>()
    private var otherClinics = mutableListOf<ClinicInfo>()

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
            Places.initialize(applicationContext, "AIzaSyCinxe3do2C5_GgfSyzGnKcRk4UatbZQSo")
        }
        placesClient = Places.createClient(this)

        // Setup drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        openDrawerBtn = findViewById(R.id.openDrawerBtn)
        nearbyClinicListView = findViewById(R.id.nearbyClinicListView)
        otherClinicListView = findViewById(R.id.otherClinicListView)

        openDrawerBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
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
        val apiKey = "AIzaSyAgwdQLKc5HLn6x2jxd3NeGxNoQMILBQF4"
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
                        val isOpen = place.optJSONObject("opening_hours")?.optBoolean("open_now")

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
                            rating, userRatings, isOpen, placeId, photoRefs
                        )

                        if (distanceInKm <= 10) {
                            nearbyClinics.add(clinic)
                        } else {
                            otherClinics.add(clinic)
                        }
                    }

                    runOnUiThread {

                        mMap.clear()
                        val marker = mMap.addMarker(MarkerOptions().position(location).title("You are here"))
                        marker?.showInfoWindow()


                        for (clinic in nearbyClinics) {
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(clinic.location)
                                    .title("${clinic.name} (${String.format("%.1f", clinic.distanceInKm)} km)")
                            )
                            marker?.tag = clinic
                        }

                        // Add markers for other clinics (those between 10 km and 50 km)
                        for (clinic in otherClinics) {
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(clinic.location)
                                    .title("${clinic.name} (${String.format("%.1f", clinic.distanceInKm)} km)")
                            )
                        }

                        // Populate the list view for nearby clinics
                        nearbyClinicListView.adapter = ArrayAdapter(
                            this@MapsActivity,
                            R.layout.clinic_list_item,
                            R.id.clinicNameText,
                            nearbyClinics.map { "${it.name} (${String.format("%.1f", it.distanceInKm)} km)" }
                        )

                        // Populate the list view for other clinics
                        otherClinicListView.adapter = ArrayAdapter(
                            this@MapsActivity,
                            R.layout.clinic_list_item,
                            R.id.clinicNameText,
                            otherClinics.map { "${it.name} (${String.format("%.1f", it.distanceInKm)} km)" }
                        )

                        nearbyClinicListView.setOnItemClickListener { _, _, position, _ ->
                            val clinic = nearbyClinics[position]
                            val clinicLocation = clinic.location
                            val clinicName = clinic.name
                            handleClinicClick(clinic)
                        }

                        otherClinicListView.setOnItemClickListener { _, _, position, _ ->
                            val clinic = otherClinics[position]
                            val clinicLocation = clinic.location
                            val clinicName = clinic.name
                            handleClinicClick(clinic)
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
        val apiKey = "AIzaSyCinxe3do2C5_GgfSyzGnKcRk4UatbZQSo"
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
                        val isOpen = place.optJSONObject("opening_hours")?.optBoolean("open_now")
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


                        if (distanceInKm > 10 && distanceInKm <= 50) {
                            val placeId = place.optString("place_id")
                            val clinic = ClinicInfo(
                                name, clinicLatLng, distanceInKm, address,
                                rating, userRatings, isOpen, placeId, photoRefs
                            )
                            otherClinics.add(clinic)
                        }
                    }

                    runOnUiThread {
                        otherClinicListView.adapter = ArrayAdapter(
                            this@MapsActivity,
                            R.layout.clinic_list_item,
                            R.id.clinicNameText,
                            otherClinics.map { "${it.name} (${String.format("%.1f", it.distanceInKm)} km)" }
                        )

                        otherClinicListView.setOnItemClickListener { _, _, position, _ ->
                            val clinic = otherClinics[position]
                            handleClinicClick(clinic)
                        }

                        for (clinic in otherClinics) {
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(clinic.location)
                                    .title("${clinic.name} (${String.format("%.1f", clinic.distanceInKm)} km)")
                            )
                            marker?.tag = clinic
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
            val apiKey = "AIzaSyCinxe3do2C5_GgfSyzGnKcRk4UatbZQSo"
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
        val navigateBtn = view.findViewById<Button>(R.id.navigateButton)


        nameText.text = clinic.name
        addressText.text = clinic.address ?: "Address not available"
        distanceText.text = "${String.format("%.1f", clinic.distanceInKm)} km away"

        ratingText.text = if (clinic.rating != null && clinic.userRatingsTotal != null)
            "⭐ ${clinic.rating} (${clinic.userRatingsTotal} reviews)"
        else
            "Rating not available"

        val imageSlider = view.findViewById<ViewPager2>(R.id.imageSlider)


        fetchPlacePhotos(clinic.placeId ?: "") { imageUrls ->
            val safeImageUrls = if (imageUrls.isEmpty()) {
                mutableListOf("https://via.placeholder.com/600x300?text=No+Image")
            } else {
                imageUrls.toMutableList()
            }
            imageSlider.adapter = ClinicImageAdapter(safeImageUrls)
        }


        // Navigate when button clicked
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
        val apiKey = "AIzaSyCinxe3do2C5_GgfSyzGnKcRk4UatbZQSo"
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


}
