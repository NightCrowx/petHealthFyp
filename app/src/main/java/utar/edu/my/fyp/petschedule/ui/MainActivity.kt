package utar.edu.my.fyp.petschedule.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utar.edu.my.fyp.MainPage
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.NotificationWorker
import utar.edu.my.fyp.petschedule.adapters.PetAdapter
import utar.edu.my.fyp.petschedule.adapters.UserSessionManager
import utar.edu.my.fyp.petschedule.Pet
import java.util.concurrent.TimeUnit
import java.util.Date
import com.google.android.material.bottomnavigation.BottomNavigationView
import utar.edu.my.fyp.SymptomCheckerPage
import utar.edu.my.fyp.MapsActivity
import utar.edu.my.fyp.AiChatbotPage
import utar.edu.my.fyp.DashboardPage

class MainActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private lateinit var petAdapter: PetAdapter

    // Add these view references
    private lateinit var recyclerViewPets: RecyclerView
    private lateinit var fabAddPet: ExtendedFloatingActionButton
    private lateinit var layoutEmptyState: View
    private lateinit var textViewTotalPets: TextView
    private lateinit var textViewActiveSchedules: TextView
    private lateinit var textViewDueToday: TextView
    private lateinit var iconSearch: ImageView
    private var searchJob: Job? = null
    private lateinit var bottomNav: BottomNavigationView

    // Store original pets list for search functionality
    private var allPets: List<Pet> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main_schedule)

            // Initialize views first
            initializeViews()

            // Initialize ViewModel
            petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

            // ENSURE USER IS SET
            val userId = UserSessionManager.getCurrentUserId(this)
            if (userId != null) {
                petViewModel.setCurrentUser(userId)
            } else {
                // No user logged in, redirect to login
                Log.d("MainActivity", "No user logged in, redirecting to MainPage")
                startActivity(Intent(this, MainPage::class.java))
                finish()
                return
            }

            // Setup RecyclerView
            setupRecyclerView()

            // Observe pets data
            observePetsData()

            // Setup search functionality
            setupSearchFunctionality()

            // Setup statistics
            setupStatistics()

            // Setup FAB for adding new pet
            setupFAB()

            // bottom navigation
            setupBottomNavigation()

            // Schedule periodic notification checks
            scheduleNotificationWork()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            // Handle the error gracefully - maybe show a toast or redirect
        }
    }

    private fun initializeViews() {
        try {
            recyclerViewPets = findViewById(R.id.recyclerViewPets)
            fabAddPet = findViewById(R.id.fabAddPet)
            layoutEmptyState = findViewById(R.id.layoutEmptyState)
            textViewTotalPets = findViewById(R.id.textViewTotalPets)
            textViewActiveSchedules = findViewById(R.id.textViewActiveSchedules)
            textViewDueToday = findViewById(R.id.textViewDueToday)
            iconSearch = findViewById(R.id.iconSearch)
            bottomNav = findViewById(R.id.bottomNav)

            Log.d("MainActivity", "Views initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing views: ${e.message}", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            petAdapter = PetAdapter { pet ->
                // Handle pet item click - navigate to pet details
                val intent = Intent(this, PetDetailActivity::class.java)
                intent.putExtra("pet_id", pet.id)
                startActivity(intent)
            }

            recyclerViewPets.adapter = petAdapter
            recyclerViewPets.layoutManager = LinearLayoutManager(this)

            Log.d("MainActivity", "RecyclerView setup completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up RecyclerView: ${e.message}", e)
        }
    }

    private fun observePetsData() {
        try {
            // Observe pets data
            petViewModel.allPets.observe(this) { pets ->
                Log.d("MainActivity", "Pets data received: ${pets?.size ?: 0} pets")

                if (pets != null) {
                    allPets = pets // Store all pets for search functionality
                    petAdapter.submitList(pets)

                    // Update UI based on pets list
                    if (pets.isEmpty()) {
                        recyclerViewPets.visibility = View.GONE
                        layoutEmptyState.visibility = View.VISIBLE
                    } else {
                        recyclerViewPets.visibility = View.VISIBLE
                        layoutEmptyState.visibility = View.GONE
                    }

                    // Update statistics
                    textViewTotalPets.text = pets.size.toString()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error observing pets data: ${e.message}", e)
        }
    }

    private fun setupSearchFunctionality() {
        try {
            iconSearch.setOnClickListener {
                showSearchDialog()
            }
            Log.d("MainActivity", "Search functionality setup completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up search functionality: ${e.message}", e)
        }
    }

    private fun showSearchDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_search_pets, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.inputLayoutSearch)
        val input = view.findViewById<TextInputEditText>(R.id.editTextSearch)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Search Pets")
            .setView(view)
            .setPositiveButton("Close", null) // realtime search—no extra buttons needed
            .create()

        dialog.show()

        // IME search action
        input.setOnEditorActionListener { _, actionId, _ ->
            val handled = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            if (handled) {
                val q = input.text?.toString()?.trim().orEmpty()
                if (q.isEmpty()) petAdapter.submitList(allPets) else performSearch(q)
            }
            handled
        }

        // Debounced realtime search
        input.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(250) // debounce
                val q = text?.toString()?.trim().orEmpty()
                if (q.isEmpty()) {
                    petAdapter.submitList(allPets)
                    inputLayout.helperText = "Showing all pets"
                } else {
                    performSearch(q)
                    inputLayout.helperText = "Searching: \"$q\""
                }
            }
        }

        // Clear (end icon) resets list
        inputLayout.setEndIconOnClickListener {
            input.text = null
            petAdapter.submitList(allPets)
            inputLayout.helperText = "Showing all pets"
        }
    }

    private fun performSearch(query: String) {
        try {
            val filteredPets = allPets.filter { pet ->
                pet.name.contains(query, ignoreCase = true) ||
                        pet.breed.contains(query, ignoreCase = true) ||
                        pet.petType.contains(query, ignoreCase = true)
            }

            petAdapter.submitList(filteredPets)

            // Update UI based on search results
            if (filteredPets.isEmpty()) {
                recyclerViewPets.visibility = View.GONE
                layoutEmptyState.visibility = View.VISIBLE
            } else {
                recyclerViewPets.visibility = View.VISIBLE
                layoutEmptyState.visibility = View.GONE
            }

            Log.d("MainActivity", "Search performed for query: '$query', found ${filteredPets.size} results")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error performing search: ${e.message}", e)
        }
    }


    private fun setupStatistics() {
        try {
            // Observe upcoming events for "Due Today" count
            petViewModel.getUpcomingEvents(1).observe(this) { events ->
                val dueToday = events?.size ?: 0
                textViewDueToday.text = dueToday.toString()
                Log.d("MainActivity", "Due today count: $dueToday")
            }

            // Use a data class to track counts for each pet
            data class PetScheduleCounts(
                val petId: Long,
                var appointments: Int = 0,
                var medicines: Int = 0,
                var vaccinations: Int = 0,
                var completedObservers: Int = 0
            ) {
                val totalCount: Int get() = appointments + medicines + vaccinations
                val isComplete: Boolean get() = completedObservers >= 3
            }

            // Get total active schedules with proper counting
            petViewModel.allPets.observe(this) { pets ->
                if (pets != null) {
                    Log.d("MainActivity", "Processing ${pets.size} pets for statistics")

                    if (pets.isEmpty()) {
                        textViewActiveSchedules.text = "0"
                        return@observe
                    }

                    val currentDate = Date(System.currentTimeMillis())
                    val petCounts = mutableMapOf<Long, PetScheduleCounts>()
                    val expectedPetCount = pets.size

                    // Initialize counts for each pet
                    pets.forEach { pet ->
                        petCounts[pet.id] = PetScheduleCounts(pet.id)
                    }

                    fun updateTotalCount() {
                        // Calculate total from all pets, whether complete or not
                        val totalActive = petCounts.values.sumOf { it.totalCount }
                        val completePets = petCounts.values.count { it.isComplete }

                        textViewActiveSchedules.text = totalActive.toString()
                        Log.d("MainActivity", "Total active schedules: $totalActive (Complete pets: $completePets/$expectedPetCount)")

                    }

                    pets.forEach { pet ->
                        val petCount = petCounts[pet.id]!!

                        // Count appointments
                        petViewModel.getAppointmentsByPet(pet.id).observe(this) { appointments ->
                            val activeCount = appointments?.count { appointment ->
                                appointment.appointmentDate.after(currentDate) && !appointment.isCompleted
                            } ?: 0

                            petCount.appointments = activeCount
                            if (petCount.completedObservers < 3) petCount.completedObservers++
                            Log.d("MainActivity", "Pet ${pet.name}: $activeCount active appointments (observer ${petCount.completedObservers}/3)")

                            updateTotalCount() // Update every time
                        }

                        // Count medicines - Fixed logic
                        petViewModel.getMedicinesByPet(pet.id).observe(this) { medicines ->
                            val activeCount = medicines?.count { medicine ->
                                medicine.isActive &&
                                        !medicine.startDate.after(currentDate) &&
                                        (medicine.endDate == null || medicine.endDate!!.after(currentDate))
                            } ?: 0

                            petCount.medicines = activeCount
                            if (petCount.completedObservers < 3) petCount.completedObservers++
                            Log.d("MainActivity", "Pet ${pet.name}: $activeCount active medicines (observer ${petCount.completedObservers}/3)")

                            updateTotalCount() // Update every time
                        }

                        // Count vaccinations
                        petViewModel.getVaccinationsByPet(pet.id).observe(this) { vaccinations ->
                            val activeCount = vaccinations?.count { vaccination ->
                                vaccination.dueDate.after(currentDate) && !vaccination.isCompleted
                            } ?: 0

                            petCount.vaccinations = activeCount
                            if (petCount.completedObservers < 3) petCount.completedObservers++
                            Log.d("MainActivity", "Pet ${pet.name}: $activeCount active vaccinations (observer ${petCount.completedObservers}/3)")

                            updateTotalCount() // Update every time
                        }
                    }
                }
            }

            Log.d("MainActivity", "Statistics setup completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up statistics: ${e.message}", e)
        }
    }

    private fun setupFAB() {
        try {
            fabAddPet.setOnClickListener {
                val intent = Intent(this, AddPetActivity::class.java)
                startActivity(intent)
            }
            Log.d("MainActivity", "FAB setup completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up FAB: ${e.message}", e)
        }
    }

    private fun scheduleNotificationWork() {
        try {
            val notificationWork = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(this).enqueue(notificationWork)
            Log.d("MainActivity", "Notification work scheduled")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error scheduling notification work: ${e.message}", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
            // Set the current item as selected (schedule page)
            bottomNav.selectedItemId = R.id.nav_schedule

            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Navigate to home/dashboard
                        startActivity(Intent(this, DashboardPage::class.java))
                        true
                    }
                    R.id.nav_schedule -> {
                        // Already on schedule page, do nothing
                        true
                    }
                    R.id.nav_symptom -> {
                        // Navigate to symptom checker
                        startActivity(Intent(this, SymptomCheckerPage::class.java))
                        true
                    }
                    R.id.nav_navigation -> {
                        // Navigate to maps
                       startActivity(Intent(this, MapsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }

            Log.d("MainActivity", "Bottom navigation setup completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up bottom navigation: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset search when returning to activity
        if (::petAdapter.isInitialized && allPets.isNotEmpty()) {
            petAdapter.submitList(allPets)
            recyclerViewPets.visibility = View.VISIBLE
            layoutEmptyState.visibility = if (allPets.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}

