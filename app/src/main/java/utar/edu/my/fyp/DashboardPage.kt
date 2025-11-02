package utar.edu.my.fyp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import com.google.android.material.navigation.NavigationView
import android.view.MenuItem
import android.widget.ImageButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bumptech.glide.Glide
import utar.edu.my.fyp.petschedule.ui.MainActivity
import utar.edu.my.fyp.petschedule.ui.PetViewModel
import utar.edu.my.fyp.petschedule.ui.AddPetActivity
import utar.edu.my.fyp.petschedule.ui.PetDetailActivity
import utar.edu.my.fyp.petschedule.adapters.PetEventAdapter
import utar.edu.my.fyp.petschedule.adapters.PetProfileAdapter
import utar.edu.my.fyp.petschedule.PetEvent
import utar.edu.my.fyp.petschedule.EventType
import utar.edu.my.fyp.petschedule.Pet
import utar.edu.my.fyp.petschedule.adapters.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class DashboardPage : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var petViewModel: PetViewModel
    private lateinit var petEventAdapter: PetEventAdapter
    private lateinit var petProfileAdapter: PetProfileAdapter
    private lateinit var recyclerViewPetEvents: RecyclerView
    private lateinit var recyclerViewPetProfiles: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var auth: FirebaseAuth
    private lateinit var scrollIndicator: LinearLayout
    private lateinit var scrollIndicatorStart: View
    private lateinit var scrollIndicatorEnd: View

    companion object {
        private const val TAG = "DashboardPage"
        private const val ADD_PET_REQUEST_CODE = 1001
        private const val EDIT_PROFILE_REQUEST_CODE = 1002
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize ViewModel
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        // ENSURE USER IS SET
        val userId = UserSessionManager.getCurrentUserId(this)
        if (userId != null) {
            petViewModel.setCurrentUser(userId)
        } else {
            // No user logged in, redirect to login
            startActivity(Intent(this, MainPage::class.java))
            finish()
            return
        }

        // Initialize drawer components
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        // Setup toolbar
        setSupportActionBar(toolbar)

        // Hide the action bar title since we have custom title
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Set navigation item selected listener
        navigationView.setNavigationItemSelectedListener(this)

        // Initialize views
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val fabChatbot = findViewById<FloatingActionButton>(R.id.fabChatbot)
        val fabAddPet = findViewById<FloatingActionButton>(R.id.fabAddPet)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        recyclerViewPetEvents = findViewById(R.id.recyclerViewPetEvents)
        recyclerViewPetProfiles = findViewById(R.id.recyclerViewPetProfiles)

        // Setup RecyclerViews
        setupRecyclerViews()

        // Observe data
        observeUpcomingEvents()
        observePetProfiles()

        // Load user data into navigation header
        loadUserDataIntoNavHeader()

        // Set home as selected
        bottomNav.selectedItemId = R.id.nav_home

        // Add hamburger menu click listener
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_schedule -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_symptom -> {
                    startActivity(Intent(this, SymptomCheckerPage::class.java))
                    true
                }
                R.id.nav_navigation -> {
                    startActivity(Intent(this, MapsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Floating chatbot button listener
        fabChatbot.setOnClickListener {
            startActivity(Intent(this, AiChatbotPage::class.java))
        }

        // Add Pet button listener
        fabAddPet.setOnClickListener {
            val intent = Intent(this, AddPetActivity::class.java)
            startActivityForResult(intent, ADD_PET_REQUEST_CODE)
        }

    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_edit_profile -> {
                // Navigate to Edit Profile
                val intent = Intent(this, EditUserProfileActivity::class.java)
                startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE)
            }
            R.id.nav_logout -> {
                // Show logout confirmation dialog
                showLogoutConfirmationDialog()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadUserDataIntoNavHeader() {
        val headerView = navigationView.getHeaderView(0)
        val navUserName = headerView.findViewById<TextView>(R.id.nav_user_name)
        val navUserEmail = headerView.findViewById<TextView>(R.id.nav_user_email)
        val navUserImage = headerView.findViewById<ImageView>(R.id.nav_user_image)

        val userId = UserSessionManager.getCurrentUserId(this)
        if (userId != null) {
            // First, get data from Firebase Auth (for Google Sign-In users)
            val currentUser = auth.currentUser
            val googlePhotoUrl = currentUser?.photoUrl?.toString()
            val googleDisplayName = currentUser?.displayName
            val googleEmail = currentUser?.email

            val dbRef = FirebaseDatabase.getInstance().getReference("user").child(userId)
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                        val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: googleEmail ?: ""
                        val avatarUrl = snapshot.child("avatarUrl").getValue(String::class.java)

                        // Determine display name - prefer database values, fallback to Google
                        val displayName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                            "$firstName $lastName"
                        } else {
                            googleDisplayName ?: "User"
                        }

                        // Update navigation header
                        navUserName.text = displayName
                        navUserEmail.text = email

                        // Determine which photo to use - prefer database, fallback to Google
                        val photoUrl = if (!avatarUrl.isNullOrEmpty()) {
                            avatarUrl
                        } else {
                            googlePhotoUrl
                        }

                        // Load avatar
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this@DashboardPage)
                                .load(photoUrl)
                                .placeholder(R.drawable.icuser)
                                .error(R.drawable.icuser)
                                .into(navUserImage)
                        } else {
                            // Set default avatar if no photo available
                            navUserImage.setImageResource(R.drawable.icuser)
                        }
                    } else {
                        // If no database record exists, use Google Sign-In data
                        navUserName.text = googleDisplayName ?: "User"
                        navUserEmail.text = googleEmail ?: ""

                        if (!googlePhotoUrl.isNullOrEmpty()) {
                            Glide.with(this@DashboardPage)
                                .load(googlePhotoUrl)
                                .placeholder(R.drawable.icuser)
                                .error(R.drawable.icuser)
                                .into(navUserImage)
                        } else {
                            navUserImage.setImageResource(R.drawable.icuser)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load user data: ${error.message}")

                    // Fallback to Google Sign-In data on database error
                    if (currentUser != null) {
                        navUserName.text = googleDisplayName ?: "User"
                        navUserEmail.text = googleEmail ?: ""

                        if (!googlePhotoUrl.isNullOrEmpty()) {
                            Glide.with(this@DashboardPage)
                                .load(googlePhotoUrl)
                                .placeholder(R.drawable.icuser)
                                .error(R.drawable.icuser)
                                .into(navUserImage)
                        }
                    }
                }
            })
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            logout()
            dialog.dismiss()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun logout() {
        try {
            // Sign out from Firebase Auth
            auth.signOut()

            // Sign out from Google Sign-In if user used Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInClient.signOut().addOnCompleteListener(this) {
                // Google Sign-In signout completed
                Log.d(TAG, "Google Sign-In signout completed")
            }

            // Clear user session
            UserSessionManager.clearCurrentUser(this)
            UserSessionManager.clearRememberedCredentials(this)

            // Navigate to login page
            val intent = Intent(this, MainPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Toast.makeText(this, "Error during logout: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh events when returning to dashboard
        Log.d(TAG, "onResume: Refreshing upcoming events")
        observeUpcomingEvents()
        // Reload user data in navigation header
        loadUserDataIntoNavHeader()
    }


    private fun setupRecyclerViews() {
        // Setup Pet Events RecyclerView (existing code)
        petEventAdapter = PetEventAdapter { petEvent ->
            handleEventClick(petEvent)
        }

        recyclerViewPetEvents.apply {
            layoutManager = LinearLayoutManager(this@DashboardPage)
            adapter = petEventAdapter
        }

        // Enhanced Pet Profiles RecyclerView setup
        petProfileAdapter = PetProfileAdapter { pet ->
            handlePetProfileClick(pet)
        }

        recyclerViewPetProfiles.apply {
            layoutManager = LinearLayoutManager(this@DashboardPage, LinearLayoutManager.HORIZONTAL, false)
            adapter = petProfileAdapter

            // Add smooth scrolling behavior
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)

            // Enable smooth scrolling
            isNestedScrollingEnabled = false

            // Add scroll listener for indicators (optional)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateScrollIndicators()
                }
            })
        }

        // Initialize scroll indicators (if you want to show them)
        initializeScrollIndicators()

        Log.d(TAG, "RecyclerViews setup completed")
    }

    private fun initializeScrollIndicators() {
        // Find the scroll indicator views
        scrollIndicator = findViewById(R.id.scrollIndicator)
        scrollIndicatorStart = findViewById(R.id.scrollIndicatorStart)
        scrollIndicatorEnd = findViewById(R.id.scrollIndicatorEnd)
    }

    private fun updateScrollIndicators() {
        val layoutManager = recyclerViewPetProfiles.layoutManager as? LinearLayoutManager
        layoutManager?.let { lm ->
            val itemCount = petProfileAdapter.itemCount
            if (itemCount <= 1) {
                scrollIndicator.visibility = View.GONE
                return
            }

            val firstVisiblePosition = lm.findFirstVisibleItemPosition()
            val lastVisiblePosition = lm.findLastVisibleItemPosition()

            // Show indicator only if there are items to scroll
            val canScrollStart = firstVisiblePosition > 0
            val canScrollEnd = lastVisiblePosition < itemCount - 1

            if (canScrollStart || canScrollEnd) {
                scrollIndicator.visibility = View.VISIBLE

                // Update indicator colors based on scroll position
                scrollIndicatorStart.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        if (canScrollStart) R.color.black else android.R.color.transparent
                    )
                )
                scrollIndicatorEnd.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        if (canScrollEnd) R.color.black else android.R.color.transparent
                    )
                )
            } else {
                scrollIndicator.visibility = View.GONE
            }
        }
    }

    private fun observeUpcomingEvents() {
        Log.d(TAG, "Starting to observe upcoming events")

        // Add this debug call
        val userId = UserSessionManager.getCurrentUserId(this)
        if (userId != null) {
            lifecycleScope.launch {
            }
        }

        // Increase the days to 14 to catch more events for debugging
        petViewModel.getUpcomingEvents(14).observe(this) { events ->
            Log.d(TAG, "Received ${events?.size ?: 0} upcoming events")

            events?.forEach { event ->
                Log.d(TAG, "Event: ${event.eventType} - ${event.title} for ${event.petName} on ${event.date}")
            }

            if (events.isNullOrEmpty()) {
                Log.w(TAG, "No upcoming events found!")
            } else {
                Log.i(TAG, "Displaying ${events.size} events in RecyclerView")
            }

            petEventAdapter.submitList(events)
        }
    }

    private fun observePetProfiles() {
        Log.d(TAG, "Starting to observe pet profiles")

        petViewModel.allPets.observe(this) { pets ->
            Log.d(TAG, "Received ${pets?.size ?: 0} pets")

            pets?.forEach { pet ->
                Log.d(TAG, "Pet: ${pet.name} (ID: ${pet.id})")
            }

            petProfileAdapter.submitList(pets)

            // Update scroll indicators after data is loaded
            recyclerViewPetProfiles.post {
                updateScrollIndicators()
            }

            if (pets.isNullOrEmpty()) {
                Log.w(TAG, "No pets found for current user")
            } else {
                Log.i(TAG, "Displaying ${pets.size} pet profiles")
            }
        }
    }

    // Optional: Add smooth scroll to specific pet position
    private fun scrollToPetPosition(position: Int) {
        val layoutManager = recyclerViewPetProfiles.layoutManager as? LinearLayoutManager
        layoutManager?.let { lm ->
            val smoothScroller = object : LinearSmoothScroller(this) {
                override fun getHorizontalSnapPreference(): Int = SNAP_TO_START
            }
            smoothScroller.targetPosition = position
            lm.startSmoothScroll(smoothScroller)
        }
    }

    private fun handlePetProfileClick(pet: Pet) {
        Log.d(TAG, "Pet profile clicked: ${pet.name} (ID: ${pet.id})")

        // Navigate to PetDetailActivity
        val intent = Intent(this, PetDetailActivity::class.java)
        intent.putExtra("pet_id", pet.id)
        startActivity(intent)
    }

    private fun handleEventClick(petEvent: PetEvent) {
        Log.d(TAG, "Event clicked: ${petEvent.eventType} - ${petEvent.title}")

        try {
            when (petEvent.eventType) {
                EventType.APPOINTMENT -> {
                    // Navigate to appointment detail
                    val intent = Intent(this, AppointmentDetailActivity::class.java)
                    intent.putExtra("appointmentId", petEvent.id)
                    intent.putExtra("petId", petEvent.petId)
                    startActivity(intent)
                }
                EventType.MEDICINE -> {
                    // For medicine events, we need to handle the modified ID
                    val originalMedicineId = petEvent.id / 1000 // Extract original medicine ID
                    val intent = Intent(this, MedicineDetailActivity::class.java)
                    intent.putExtra("medicineId", originalMedicineId)
                    intent.putExtra("petId", petEvent.petId)
                    startActivity(intent)
                }
                EventType.VACCINATION -> {
                    // Navigate to vaccination detail
                    val intent = Intent(this, VaccinationDetailActivity::class.java)
                    intent.putExtra("vaccinationId", petEvent.id)
                    intent.putExtra("petId", petEvent.petId)
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling event click", e)
            Toast.makeText(this, "Error opening event details", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ADD_PET_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Pet added successfully, refreshing pet profiles")
                    showPostAddPetDialog()
                }
            }
            EDIT_PROFILE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Profile updated successfully")
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    // Reload user data in navigation header
                    loadUserDataIntoNavHeader()
                }
            }
        }
    }

    private fun showPostAddPetDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Pet Added Successfully!")
        builder.setMessage("What would you like to do next?")

        builder.setPositiveButton("View Pet Details") { dialog, _ ->
            // Get the most recently added pet and navigate to its details
            petViewModel.allPets.value?.let { pets ->
                if (pets.isNotEmpty()) {
                    val mostRecentPet = pets.maxByOrNull { it.id }
                    mostRecentPet?.let { pet ->
                        val intent = Intent(this, PetDetailActivity::class.java)
                        intent.putExtra("pet_id", pet.id)
                        startActivity(intent)
                    }
                }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Stay on Dashboard") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}

