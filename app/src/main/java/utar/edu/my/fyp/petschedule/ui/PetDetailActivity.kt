//package utar.edu.my.fyp.petschedule.ui
//
//import android.animation.AnimatorSet
//import android.animation.ObjectAnimator
//import android.animation.ValueAnimator
//import android.content.Intent
//import android.graphics.BitmapFactory
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.view.View
//import android.view.animation.AccelerateDecelerateInterpolator
//import android.view.animation.DecelerateInterpolator
//import android.view.animation.OvershootInterpolator
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.button.MaterialButton
//import com.google.android.material.floatingactionbutton.FloatingActionButton
//import utar.edu.my.fyp.R
//import utar.edu.my.fyp.petschedule.Pet
//import utar.edu.my.fyp.petschedule.adapters.AppointmentAdapter
//import utar.edu.my.fyp.petschedule.adapters.MedicineAdapter
//import utar.edu.my.fyp.petschedule.adapters.VaccinationAdapter
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.*
//
//class PetDetailActivity : AppCompatActivity() {
//    private lateinit var petViewModel: PetViewModel
//    private lateinit var medicineAdapter: MedicineAdapter
//    private lateinit var vaccinationAdapter: VaccinationAdapter
//    private lateinit var appointmentAdapter: AppointmentAdapter
//    private var petId: Long = 0
//    private var currentPet: Pet? = null
//
//    // UI Elements
//    private lateinit var petImageView: ImageView
//    private lateinit var petNameTextView: TextView
//    private lateinit var breedTextView: TextView
//    private lateinit var genderTextView: TextView
//    private lateinit var dateOfBirthTextView: TextView
//    private lateinit var quickBreed: TextView
//    private lateinit var quickAge: TextView
//
//    // Animation views
//    private lateinit var heroSection: View
//    private lateinit var quickInfoLayout: View
//    private lateinit var mainContent: View
//    private lateinit var medicineCard: View
//    private lateinit var vaccinationCard: View
//    private lateinit var appointmentCard: View
//
//    // Auto-delete functionality
//    private lateinit var autoDeleteHandler: Handler
//    private lateinit var autoDeleteRunnable: Runnable
//    private val autoDeleteInterval = 60000L // Check every minute
//
//    // FIXED: Consistent date format across the app
//    private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
//    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_pet_detail)
//
//        petId = intent.getLongExtra("pet_id", 0)
//        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]
//
//        setupToolbar()
//        setupViews()
//        setupRecyclerViews()
//        setupButtons()
//        setupAnimations()
//        setupAutoDelete()
//        observeData()
//        startEntryAnimation()
//    }
//
//    private fun setupToolbar() {
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//    }
//
//    private fun setupViews() {
//        petImageView = findViewById(R.id.imageViewPetDetail)
//        petNameTextView = findViewById(R.id.textViewPetName)
//        breedTextView = findViewById(R.id.textViewPetBreed)
//        genderTextView = findViewById(R.id.textViewGender)
//        dateOfBirthTextView = findViewById(R.id.textViewDateOfBirth)
//        quickBreed = findViewById(R.id.quickBreed)
//        quickAge = findViewById(R.id.quickAge)
//
//        // Animation views
//        heroSection = findViewById(R.id.heroSection)
//        quickInfoLayout = findViewById(R.id.quickInfoLayout)
//        mainContent = findViewById(R.id.mainContent)
//        medicineCard = findViewById(R.id.medicineCard)
//        vaccinationCard = findViewById(R.id.vaccinationCard)
//        appointmentCard = findViewById(R.id.appointmentCard)
//    }
//
//    private fun setupRecyclerViews() {
//        // Medicine RecyclerView with click handler and animation
//        medicineAdapter = MedicineAdapter { medicine ->
//            animateCardClick(medicineCard) {
//                val intent = Intent(this, EditMedicineActivity::class.java)
//                intent.putExtra("medicine_id", medicine.id)
//                intent.putExtra("pet_id", petId)
//                startActivityForResult(intent, EDIT_MEDICINE_REQUEST_CODE)
//            }
//        }
//        findViewById<RecyclerView>(R.id.recyclerViewMedicines).apply {
//            adapter = medicineAdapter
//            layoutManager = LinearLayoutManager(this@PetDetailActivity)
//        }
//
//        // Vaccination RecyclerView with click handler and animation
//        vaccinationAdapter = VaccinationAdapter { vaccination ->
//            animateCardClick(vaccinationCard) {
//                val intent = Intent(this, EditVaccinationActivity::class.java)
//                intent.putExtra("vaccination_id", vaccination.id)
//                intent.putExtra("pet_id", petId)
//                startActivityForResult(intent, EDIT_VACCINATION_REQUEST_CODE)
//            }
//        }
//        findViewById<RecyclerView>(R.id.recyclerViewVaccinations).apply {
//            adapter = vaccinationAdapter
//            layoutManager = LinearLayoutManager(this@PetDetailActivity)
//        }
//
//        // Appointment RecyclerView with click handler and animation
//        appointmentAdapter = AppointmentAdapter { appointment ->
//            animateCardClick(appointmentCard) {
//                val intent = Intent(this, EditAppointmentActivity::class.java)
//                intent.putExtra("appointment_id", appointment.id)
//                intent.putExtra("pet_id", petId)
//                startActivityForResult(intent, EDIT_APPOINTMENT_REQUEST_CODE)
//            }
//        }
//        findViewById<RecyclerView>(R.id.recyclerViewAppointments).apply {
//            adapter = appointmentAdapter
//            layoutManager = LinearLayoutManager(this@PetDetailActivity)
//        }
//    }
//
//    private fun setupButtons() {
//        findViewById<MaterialButton>(R.id.buttonAddMedicine).setOnClickListener {
//            animateButtonClick(it) {
//                val intent = Intent(this, AddMedicineActivity::class.java)
//                intent.putExtra("pet_id", petId)
//                startActivityForResult(intent, ADD_MEDICINE_REQUEST_CODE)
//            }
//        }
//
//        findViewById<MaterialButton>(R.id.buttonAddVaccination).setOnClickListener {
//            animateButtonClick(it) {
//                val intent = Intent(this, AddVaccinationActivity::class.java)
//                intent.putExtra("pet_id", petId)
//                startActivityForResult(intent, ADD_VACCINATION_REQUEST_CODE)
//            }
//        }
//
//        findViewById<MaterialButton>(R.id.buttonAddAppointment).setOnClickListener {
//            animateButtonClick(it) {
//                val intent = Intent(this, AddAppointmentActivity::class.java)
//                intent.putExtra("pet_id", petId)
//                startActivityForResult(intent, ADD_APPOINTMENT_REQUEST_CODE)
//            }
//        }
//
//        findViewById<MaterialButton>(R.id.buttonEdit).setOnClickListener {
//            animateButtonClick(it) {
//                val intent = Intent(this, EditPetActivity::class.java)
//                intent.putExtra("pet_id", petId)
//                startActivityForResult(intent, EDIT_PET_REQUEST_CODE)
//            }
//        }
//    }
//
//    private fun setupAnimations() {
//        // Set up interactive animations for detail rows
//        setupDetailRowAnimations()
//
//        // Set up card hover effects
//        setupCardHoverEffects()
//    }
//
//    // Setup auto-delete functionality
//    private fun setupAutoDelete() {
//        autoDeleteHandler = Handler(Looper.getMainLooper())
//        autoDeleteRunnable = object : Runnable {
//            override fun run() {
//                try {
//                    checkAndDeleteExpiredItems()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                autoDeleteHandler.postDelayed(this, autoDeleteInterval)
//            }
//        }
//    }
//
//    // FIXED: Improved auto-delete functionality with better error handling
//    private fun checkAndDeleteExpiredItems() {
//        try {
//            val currentDate = Date()
//
//            // Check medicines - using dueDate as string, convert to Date for comparison
//            petViewModel.getMedicinesByPet(petId).value?.forEach { medicine ->
//                try {
//                    // Delete expired medicines (where endDate has passed)
//                    medicine.endDate?.let { endDate ->
//                        if (endDate.before(currentDate)) {
//                            petViewModel.deleteMedicine(medicine)
//                        }
//                    }
//                } catch (e: Exception) {
//                    // Skip this medicine if date comparison fails
//                    e.printStackTrace()
//                }
//            }
//
//            // Check vaccinations - dueDate is a Date object
//            petViewModel.getVaccinationsByPet(petId).value?.forEach { vaccination ->
//                try {
//                    if (vaccination.dueDate.before(currentDate) && !vaccination.isCompleted) {
//                        petViewModel.deleteVaccination(vaccination)
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//
//            // FIXED: Check appointments - handle both Date and String formats
//            petViewModel.getAppointmentsByPet(petId).value?.forEach { appointment ->
//                try {
//                    // Check if appointmentDate exists and is before current date
//                    if (appointment.appointmentDate.before(currentDate)) {
//                        petViewModel.deleteAppointment(appointment)
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//
//    // Start auto-delete monitoring
//    private fun startAutoDeleteMonitoring() {
//        autoDeleteHandler.post(autoDeleteRunnable)
//    }
//
//    // Stop auto-delete monitoring
//    private fun stopAutoDeleteMonitoring() {
//        autoDeleteHandler.removeCallbacks(autoDeleteRunnable)
//    }
//
//    private fun setupDetailRowAnimations() {
//        val breedRow = findViewById<LinearLayout>(R.id.breedRow)
//        val genderRow = findViewById<LinearLayout>(R.id.genderRow)
//        val dobRow = findViewById<LinearLayout>(R.id.dobRow)
//
//        val detailRows = listOf(breedRow, genderRow, dobRow)
//
//        detailRows.forEach { row ->
//            row.setOnClickListener { view ->
//                val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.95f, 1.0f)
//                val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.95f, 1.0f)
//
//                AnimatorSet().apply {
//                    playTogether(scaleX, scaleY)
//                    duration = 150
//                    interpolator = AccelerateDecelerateInterpolator()
//                    start()
//                }
//            }
//        }
//    }
//
//    private fun setupCardHoverEffects() {
//        // Add subtle elevation animation on touch
//        val cards = listOf(medicineCard, vaccinationCard, appointmentCard)
//
//        cards.forEach { card ->
//            card.setOnTouchListener { view, motionEvent ->
//                when (motionEvent.action) {
//                    android.view.MotionEvent.ACTION_DOWN -> {
//                        animateCardElevation(view, 12f)
//                    }
//                    android.view.MotionEvent.ACTION_UP,
//                    android.view.MotionEvent.ACTION_CANCEL -> {
//                        animateCardElevation(view, 8f)
//                    }
//                }
//                false
//            }
//        }
//    }
//
//    private fun startEntryAnimation() {
//        // Animate pet name
//        ObjectAnimator.ofFloat(petNameTextView, "alpha", 0f, 1f).apply {
//            duration = 600
//            startDelay = 300
//            interpolator = DecelerateInterpolator()
//            start()
//        }
//
//        ObjectAnimator.ofFloat(petNameTextView, "translationY", 20f, 0f).apply {
//            duration = 600
//            startDelay = 300
//            interpolator = DecelerateInterpolator()
//            start()
//        }
//
//        // Animate quick info layout
//        ObjectAnimator.ofFloat(quickInfoLayout, "alpha", 0f, 1f).apply {
//            duration = 600
//            startDelay = 450
//            interpolator = DecelerateInterpolator()
//            start()
//        }
//
//        ObjectAnimator.ofFloat(quickInfoLayout, "translationY", 20f, 0f).apply {
//            duration = 600
//            startDelay = 450
//            interpolator = DecelerateInterpolator()
//            start()
//        }
//
//        // Animate main content
//        ObjectAnimator.ofFloat(mainContent, "alpha", 0f, 1f).apply {
//            duration = 800
//            startDelay = 600
//            interpolator = DecelerateInterpolator()
//            start()
//        }
//
//        ObjectAnimator.ofFloat(mainContent, "translationY", 30f, 0f).apply {
//            duration = 800
//            startDelay = 600
//            interpolator = DecelerateInterpolator()
//            start()
//        }
//
//        // Stagger card animations
//        animateCardsEntry()
//    }
//
//    private fun animateCardsEntry() {
//        val cards = listOf(medicineCard, vaccinationCard, appointmentCard)
//
//        cards.forEachIndexed { index, card ->
//            ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
//                duration = 600
//                startDelay = 800 + (index * 150L)
//                interpolator = DecelerateInterpolator()
//                start()
//            }
//
//            ObjectAnimator.ofFloat(card, "translationY", 20f, 0f).apply {
//                duration = 600
//                startDelay = 800 + (index * 150L)
//                interpolator = OvershootInterpolator(1.1f)
//                start()
//            }
//        }
//    }
//
//    private fun animateButtonClick(view: View, action: () -> Unit) {
//        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.9f, 1.0f)
//        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.9f, 1.0f)
//
//        AnimatorSet().apply {
//            playTogether(scaleX, scaleY)
//            duration = 200
//            interpolator = AccelerateDecelerateInterpolator()
//            start()
//        }
//
//        Handler(Looper.getMainLooper()).postDelayed({
//            action()
//        }, 100)
//    }
//
//    private fun animateFabClick(fab: FloatingActionButton, action: () -> Unit) {
//        val rotation = ObjectAnimator.ofFloat(fab, "rotation", 0f, 360f)
//        val scaleX = ObjectAnimator.ofFloat(fab, "scaleX", 1.0f, 0.8f, 1.0f)
//        val scaleY = ObjectAnimator.ofFloat(fab, "scaleY", 1.0f, 0.8f, 1.0f)
//
//        AnimatorSet().apply {
//            playTogether(rotation, scaleX, scaleY)
//            duration = 300
//            interpolator = AccelerateDecelerateInterpolator()
//            start()
//        }
//
//        Handler(Looper.getMainLooper()).postDelayed({
//            action()
//        }, 150)
//    }
//
//    private fun animateCardClick(card: View, action: () -> Unit) {
//        val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1.0f, 0.98f, 1.0f)
//        val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1.0f, 0.98f, 1.0f)
//
//        AnimatorSet().apply {
//            playTogether(scaleX, scaleY)
//            duration = 200
//            interpolator = AccelerateDecelerateInterpolator()
//            start()
//        }
//
//        Handler(Looper.getMainLooper()).postDelayed({
//            action()
//        }, 100)
//    }
//
//    private fun animateCardElevation(view: View, targetElevation: Float) {
//        ValueAnimator.ofFloat(view.elevation, targetElevation).apply {
//            duration = 200
//            interpolator = AccelerateDecelerateInterpolator()
//            addUpdateListener { animation ->
//                view.elevation = animation.animatedValue as Float
//            }
//            start()
//        }
//    }
//
//
//    private fun observeData() {
//        // Observe pet details
//        petViewModel.getPetById(petId).observe(this) { pet ->
//            pet?.let {
//                currentPet = it
//                displayPetInfo(it)
//                supportActionBar?.title = it.name
//            }
//        }
//
//        // Filter out inactive medicines
//        petViewModel.getMedicinesByPet(petId).observe(this) { medicines ->
//            val activeMedicines = medicines?.filter { it.isActive } ?: emptyList()
//            medicineAdapter.submitList(activeMedicines)
//            animateListChange(findViewById(R.id.recyclerViewMedicines))
//        }
//
//        // Filter out completed vaccinations
//        petViewModel.getVaccinationsByPet(petId).observe(this) { vaccinations ->
//            val pendingVaccinations = vaccinations?.filter { !it.isCompleted } ?: emptyList()
//            vaccinationAdapter.submitList(pendingVaccinations)
//            animateListChange(findViewById(R.id.recyclerViewVaccinations))
//        }
//
//        // Filter out completed appointments
//        petViewModel.getAppointmentsByPet(petId).observe(this) { appointments ->
//            val pendingAppointments = appointments?.filter { !it.isCompleted } ?: emptyList()
//            appointmentAdapter.submitList(pendingAppointments)
//            animateListChange(findViewById(R.id.recyclerViewAppointments))
//        }
//    }
//
//    private fun animateListChange(recyclerView: RecyclerView) {
//        recyclerView.scheduleLayoutAnimation()
//    }
//
//    private fun displayPetInfo(pet: Pet) {
//        petNameTextView.text = pet.name
//        breedTextView.text = pet.breed
//        genderTextView.text = pet.gender
//
//        // Update quick info pills
//        quickBreed.text = pet.breed.split(" ").first() // First word of breed
//
//        // Calculate and display age in year month format
//        val ageString = calculateAge(pet.dateOfBirth)
//        quickAge.text = ageString
//        dateOfBirthTextView.text = "${pet.dateOfBirth} ($ageString)"
//
//        // Load pet image with animation
//        pet.imagePath?.let { imagePath ->
//            val file = File(imagePath)
//            if (file.exists()) {
//                val bitmap = BitmapFactory.decodeFile(imagePath)
//                // Animate image change
//                ObjectAnimator.ofFloat(petImageView, "alpha", 1f, 0f).apply {
//                    duration = 200
//                    addListener(object : android.animation.AnimatorListenerAdapter() {
//                        override fun onAnimationEnd(animation: android.animation.Animator) {
//                            petImageView.setImageBitmap(bitmap)
//                            ObjectAnimator.ofFloat(petImageView, "alpha", 0f, 1f).apply {
//                                duration = 200
//                                start()
//                            }
//                        }
//                    })
//                    start()
//                }
//            } else {
//                petImageView.setImageResource(R.drawable.ic_pet_default)
//            }
//        } ?: run {
//            petImageView.setImageResource(R.drawable.ic_pet_default)
//        }
//    }
//
//    // FIXED: Improved calculateAge function with better error handling
//    private fun calculateAge(dateOfBirth: String): String {
//        return try {
//            val birthDate = dateFormat.parse(dateOfBirth)
//
//            if (birthDate != null) {
//                val birthCalendar = Calendar.getInstance().apply { time = birthDate }
//                val currentCalendar = Calendar.getInstance()
//
//                var years = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
//                var months = currentCalendar.get(Calendar.MONTH) - birthCalendar.get(Calendar.MONTH)
//
//                // If current month is before birth month, subtract a year and add 12 months
//                if (months < 0) {
//                    years--
//                    months += 12
//                }
//
//                // If current day is before birth day, subtract a month
//                if (currentCalendar.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH)) {
//                    months--
//                    if (months < 0) {
//                        years--
//                        months += 12
//                    }
//                }
//
//                when {
//                    years == 0 && months == 0 -> "0 month"
//                    years == 0 -> if (months == 1) "1 month" else "$months months"
//                    months == 0 -> if (years == 1) "1 year" else "$years years"
//                    else -> {
//                        val yearText = if (years == 1) "1 year" else "$years years"
//                        val monthText = if (months == 1) "1 month" else "$months months"
//                        "$yearText $monthText"
//                    }
//                }
//            } else {
//                "Unknown age"
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            "Unknown age"
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (requestCode) {
//            EDIT_PET_REQUEST_CODE -> {
//                if (resultCode == RESULT_OK) {
//                    // Pet was updated or deleted, check if it still exists
//                    petViewModel.getPetById(petId).observe(this) { pet ->
//                        if (pet == null) {
//                            // Pet was deleted, finish this activity with exit animation
//                            finishWithAnimation()
//                        }
//                        // If pet still exists, the observer will automatically update the UI
//                    }
//                }
//            }
//            ADD_MEDICINE_REQUEST_CODE, EDIT_MEDICINE_REQUEST_CODE -> {
//                if (resultCode == RESULT_OK) {
//                    // Medicine was added, updated, or deleted
//                    // The LiveData observer will automatically refresh the list
//                    animateContentUpdate()
//                }
//            }
//            ADD_VACCINATION_REQUEST_CODE, EDIT_VACCINATION_REQUEST_CODE -> {
//                if (resultCode == RESULT_OK) {
//                    // Vaccination was added, updated, or deleted
//                    // The LiveData observer will automatically refresh the list
//                    animateContentUpdate()
//                }
//            }
//            ADD_APPOINTMENT_REQUEST_CODE, EDIT_APPOINTMENT_REQUEST_CODE -> {
//                if (resultCode == RESULT_OK) {
//                    // Appointment was added, updated, or deleted
//                    // The LiveData observer will automatically refresh the list
//                    animateContentUpdate()
//                }
//            }
//        }
//    }
//
//    private fun animateContentUpdate() {
//        // Subtle pulse animation to indicate content update
//        val pulse = ObjectAnimator.ofFloat(mainContent, "alpha", 1f, 0.8f, 1f)
//        pulse.duration = 300
//        pulse.interpolator = AccelerateDecelerateInterpolator()
//        pulse.start()
//    }
//
//    private fun finishWithAnimation() {
//        // Exit animation
//        ObjectAnimator.ofFloat(findViewById(R.id.heroSection), "alpha", 1f, 0f).apply {
//            duration = 300
//            start()
//        }
//
//        ObjectAnimator.ofFloat(mainContent, "translationY", 0f, 100f).apply {
//            duration = 300
//            addListener(object : android.animation.AnimatorListenerAdapter() {
//                override fun onAnimationEnd(animation: android.animation.Animator) {
//                    finish()
//                }
//            })
//            start()
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Start auto-delete monitoring when activity becomes visible
//        startAutoDeleteMonitoring()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        // Stop auto-delete monitoring when activity is not visible
//        stopAutoDeleteMonitoring()
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        finishWithAnimation()
//        return true
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        finishWithAnimation()
//    }
//
//    companion object {
//        private const val EDIT_PET_REQUEST_CODE = 1002
//        private const val ADD_MEDICINE_REQUEST_CODE = 1003
//        private const val EDIT_MEDICINE_REQUEST_CODE = 1004
//        private const val ADD_VACCINATION_REQUEST_CODE = 1005
//        private const val EDIT_VACCINATION_REQUEST_CODE = 1006
//        private const val ADD_APPOINTMENT_REQUEST_CODE = 1007
//        private const val EDIT_APPOINTMENT_REQUEST_CODE = 1008
//    }
//}



package utar.edu.my.fyp.petschedule.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Pet
import utar.edu.my.fyp.petschedule.adapters.AppointmentAdapter
import utar.edu.my.fyp.petschedule.adapters.MedicineAdapter
import utar.edu.my.fyp.petschedule.adapters.VaccinationAdapter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PetDetailActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private lateinit var medicineAdapter: MedicineAdapter
    private lateinit var vaccinationAdapter: VaccinationAdapter
    private lateinit var appointmentAdapter: AppointmentAdapter
    private var petId: Long = 0
    private var currentPet: Pet? = null

    // UI Elements
    private lateinit var petImageView: ImageView
    private lateinit var petNameTextView: TextView
    private lateinit var breedTextView: TextView
    private lateinit var genderTextView: TextView
    private lateinit var dateOfBirthTextView: TextView
    private lateinit var quickBreed: TextView
    private lateinit var quickAge: TextView

    // Animation views
    private lateinit var heroSection: View
    private lateinit var quickInfoLayout: View
    private lateinit var mainContent: View
    private lateinit var medicineCard: View
    private lateinit var vaccinationCard: View
    private lateinit var appointmentCard: View

    // Auto-delete functionality
    private lateinit var autoDeleteHandler: Handler
    private lateinit var autoDeleteRunnable: Runnable
    private val autoDeleteInterval = 60000L // Check every minute

    // FIXED: Consistent date format across the app
    private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_detail)

        petId = intent.getLongExtra("pet_id", 0)
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        setupToolbar()
        setupViews()
        setupRecyclerViews()
        setupButtons()
        setupAnimations()
        setupAutoDelete()
        observeData()
        startEntryAnimation()
    }

    private fun setupToolbar() {
        // Set up the toolbar properly
        setSupportActionBar(findViewById(R.id.toolbar))

        // Enable the back button
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Pet Details" // Default title, will be updated when pet data loads
        }
    }

    private fun setupViews() {
        petImageView = findViewById(R.id.imageViewPetDetail)
        petNameTextView = findViewById(R.id.textViewPetName)
        breedTextView = findViewById(R.id.textViewPetBreed)
        genderTextView = findViewById(R.id.textViewGender)
        dateOfBirthTextView = findViewById(R.id.textViewDateOfBirth)
        quickBreed = findViewById(R.id.quickBreed)
        quickAge = findViewById(R.id.quickAge)

        // Animation views
        heroSection = findViewById(R.id.heroSection)
        quickInfoLayout = findViewById(R.id.quickInfoLayout)
        mainContent = findViewById(R.id.mainContent)
        medicineCard = findViewById(R.id.medicineCard)
        vaccinationCard = findViewById(R.id.vaccinationCard)
        appointmentCard = findViewById(R.id.appointmentCard)
    }

    private fun setupRecyclerViews() {
        // Medicine RecyclerView with click handler and animation
        medicineAdapter = MedicineAdapter { medicine ->
            animateCardClick(medicineCard) {
                val intent = Intent(this, EditMedicineActivity::class.java)
                intent.putExtra("medicine_id", medicine.id)
                intent.putExtra("pet_id", petId)
                startActivityForResult(intent, EDIT_MEDICINE_REQUEST_CODE)
            }
        }
        findViewById<RecyclerView>(R.id.recyclerViewMedicines).apply {
            adapter = medicineAdapter
            layoutManager = LinearLayoutManager(this@PetDetailActivity)
        }

        // Vaccination RecyclerView with click handler and animation
        vaccinationAdapter = VaccinationAdapter { vaccination ->
            animateCardClick(vaccinationCard) {
                val intent = Intent(this, EditVaccinationActivity::class.java)
                intent.putExtra("vaccination_id", vaccination.id)
                intent.putExtra("pet_id", petId)
                startActivityForResult(intent, EDIT_VACCINATION_REQUEST_CODE)
            }
        }
        findViewById<RecyclerView>(R.id.recyclerViewVaccinations).apply {
            adapter = vaccinationAdapter
            layoutManager = LinearLayoutManager(this@PetDetailActivity)
        }

        // Appointment RecyclerView with click handler and animation
        appointmentAdapter = AppointmentAdapter { appointment ->
            animateCardClick(appointmentCard) {
                val intent = Intent(this, EditAppointmentActivity::class.java)
                intent.putExtra("appointment_id", appointment.id)
                intent.putExtra("pet_id", petId)
                startActivityForResult(intent, EDIT_APPOINTMENT_REQUEST_CODE)
            }
        }
        findViewById<RecyclerView>(R.id.recyclerViewAppointments).apply {
            adapter = appointmentAdapter
            layoutManager = LinearLayoutManager(this@PetDetailActivity)
        }
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.buttonAddMedicine).setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, AddMedicineActivity::class.java)
                intent.putExtra("pet_id", petId)
                startActivityForResult(intent, ADD_MEDICINE_REQUEST_CODE)
            }
        }

        findViewById<MaterialButton>(R.id.buttonAddVaccination).setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, AddVaccinationActivity::class.java)
                intent.putExtra("pet_id", petId)
                startActivityForResult(intent, ADD_VACCINATION_REQUEST_CODE)
            }
        }

        findViewById<MaterialButton>(R.id.buttonAddAppointment).setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, AddAppointmentActivity::class.java)
                intent.putExtra("pet_id", petId)
                startActivityForResult(intent, ADD_APPOINTMENT_REQUEST_CODE)
            }
        }

        findViewById<MaterialButton>(R.id.buttonEdit).setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, EditPetActivity::class.java)
                intent.putExtra("pet_id", petId)
                startActivityForResult(intent, EDIT_PET_REQUEST_CODE)
            }
        }
    }

    private fun setupAnimations() {
        // Set up interactive animations for detail rows
        setupDetailRowAnimations()

        // Set up card hover effects
        setupCardHoverEffects()
    }

    // Setup auto-delete functionality
    private fun setupAutoDelete() {
        autoDeleteHandler = Handler(Looper.getMainLooper())
        autoDeleteRunnable = object : Runnable {
            override fun run() {
                try {
                    checkAndDeleteExpiredItems()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                autoDeleteHandler.postDelayed(this, autoDeleteInterval)
            }
        }
    }

    // FIXED: Improved auto-delete functionality with better error handling
    private fun checkAndDeleteExpiredItems() {
        try {
            val currentDate = Date()

            // Check medicines - using dueDate as string, convert to Date for comparison
            petViewModel.getMedicinesByPet(petId).value?.forEach { medicine ->
                try {
                    // Delete expired medicines (where endDate has passed)
                    medicine.endDate?.let { endDate ->
                        if (endDate.before(currentDate)) {
                            petViewModel.deleteMedicine(medicine)
                        }
                    }
                } catch (e: Exception) {
                    // Skip this medicine if date comparison fails
                    e.printStackTrace()
                }
            }

            // Check vaccinations - dueDate is a Date object
            petViewModel.getVaccinationsByPet(petId).value?.forEach { vaccination ->
                try {
                    if (vaccination.dueDate.before(currentDate) && !vaccination.isCompleted) {
                        petViewModel.deleteVaccination(vaccination)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // FIXED: Check appointments - handle both Date and String formats
            petViewModel.getAppointmentsByPet(petId).value?.forEach { appointment ->
                try {
                    // Check if appointmentDate exists and is before current date
                    if (appointment.appointmentDate.before(currentDate)) {
                        petViewModel.deleteAppointment(appointment)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Start auto-delete monitoring
    private fun startAutoDeleteMonitoring() {
        autoDeleteHandler.post(autoDeleteRunnable)
    }

    // Stop auto-delete monitoring
    private fun stopAutoDeleteMonitoring() {
        autoDeleteHandler.removeCallbacks(autoDeleteRunnable)
    }

    private fun setupDetailRowAnimations() {
        val breedRow = findViewById<LinearLayout>(R.id.breedRow)
        val genderRow = findViewById<LinearLayout>(R.id.genderRow)
        val dobRow = findViewById<LinearLayout>(R.id.dobRow)

        val detailRows = listOf(breedRow, genderRow, dobRow)

        detailRows.forEach { row ->
            row.setOnClickListener { view ->
                val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.95f, 1.0f)
                val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.95f, 1.0f)

                AnimatorSet().apply {
                    playTogether(scaleX, scaleY)
                    duration = 150
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }
    }

    private fun setupCardHoverEffects() {
        // Add subtle elevation animation on touch
        val cards = listOf(medicineCard, vaccinationCard, appointmentCard)

        cards.forEach { card ->
            card.setOnTouchListener { view, motionEvent ->
                when (motionEvent.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        animateCardElevation(view, 12f)
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        animateCardElevation(view, 8f)
                    }
                }
                false
            }
        }
    }

    private fun startEntryAnimation() {
        // Animate pet name
        ObjectAnimator.ofFloat(petNameTextView, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 300
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(petNameTextView, "translationY", 20f, 0f).apply {
            duration = 600
            startDelay = 300
            interpolator = DecelerateInterpolator()
            start()
        }

        // Animate quick info layout
        ObjectAnimator.ofFloat(quickInfoLayout, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 450
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(quickInfoLayout, "translationY", 20f, 0f).apply {
            duration = 600
            startDelay = 450
            interpolator = DecelerateInterpolator()
            start()
        }

        // Animate main content
        ObjectAnimator.ofFloat(mainContent, "alpha", 0f, 1f).apply {
            duration = 800
            startDelay = 600
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofFloat(mainContent, "translationY", 30f, 0f).apply {
            duration = 800
            startDelay = 600
            interpolator = DecelerateInterpolator()
            start()
        }

        // Stagger card animations
        animateCardsEntry()
    }

    private fun animateCardsEntry() {
        val cards = listOf(medicineCard, vaccinationCard, appointmentCard)

        cards.forEachIndexed { index, card ->
            ObjectAnimator.ofFloat(card, "alpha", 0f, 1f).apply {
                duration = 600
                startDelay = 800 + (index * 150L)
                interpolator = DecelerateInterpolator()
                start()
            }

            ObjectAnimator.ofFloat(card, "translationY", 20f, 0f).apply {
                duration = 600
                startDelay = 800 + (index * 150L)
                interpolator = OvershootInterpolator(1.1f)
                start()
            }
        }
    }

    private fun animateButtonClick(view: View, action: () -> Unit) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.9f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.9f, 1.0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            action()
        }, 100)
    }

    private fun animateFabClick(fab: FloatingActionButton, action: () -> Unit) {
        val rotation = ObjectAnimator.ofFloat(fab, "rotation", 0f, 360f)
        val scaleX = ObjectAnimator.ofFloat(fab, "scaleX", 1.0f, 0.8f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(fab, "scaleY", 1.0f, 0.8f, 1.0f)

        AnimatorSet().apply {
            playTogether(rotation, scaleX, scaleY)
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            action()
        }, 150)
    }

    private fun animateCardClick(card: View, action: () -> Unit) {
        val scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1.0f, 0.98f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1.0f, 0.98f, 1.0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            action()
        }, 100)
    }

    private fun animateCardElevation(view: View, targetElevation: Float) {
        ValueAnimator.ofFloat(view.elevation, targetElevation).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                view.elevation = animation.animatedValue as Float
            }
            start()
        }
    }

    private fun observeData() {
        // Observe pet details
        petViewModel.getPetById(petId).observe(this) { pet ->
            pet?.let {
                currentPet = it
                displayPetInfo(it)
                supportActionBar?.title = it.name
            }
        }

        // Filter out inactive medicines
        petViewModel.getMedicinesByPet(petId).observe(this) { medicines ->
            val activeMedicines = medicines?.filter { it.isActive } ?: emptyList()
            medicineAdapter.submitList(activeMedicines)
            animateListChange(findViewById(R.id.recyclerViewMedicines))
        }

        // Filter out completed vaccinations
        petViewModel.getVaccinationsByPet(petId).observe(this) { vaccinations ->
            val pendingVaccinations = vaccinations?.filter { !it.isCompleted } ?: emptyList()
            vaccinationAdapter.submitList(pendingVaccinations)
            animateListChange(findViewById(R.id.recyclerViewVaccinations))
        }

        // Filter out completed appointments
        petViewModel.getAppointmentsByPet(petId).observe(this) { appointments ->
            val pendingAppointments = appointments?.filter { !it.isCompleted } ?: emptyList()
            appointmentAdapter.submitList(pendingAppointments)
            animateListChange(findViewById(R.id.recyclerViewAppointments))
        }
    }

    private fun animateListChange(recyclerView: RecyclerView) {
        recyclerView.scheduleLayoutAnimation()
    }

    private fun displayPetInfo(pet: Pet) {
        petNameTextView.text = pet.name
        breedTextView.text = pet.breed
        genderTextView.text = pet.gender

        // Update quick info pills
        quickBreed.text = pet.breed.split(" ").first() // First word of breed

        // Calculate and display age in year month format
        val ageString = calculateAge(pet.dateOfBirth)
        quickAge.text = ageString
        dateOfBirthTextView.text = "${pet.dateOfBirth} ($ageString)"

        // Load pet image with animation
        pet.imagePath?.let { imagePath ->
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                // Animate image change
                ObjectAnimator.ofFloat(petImageView, "alpha", 1f, 0f).apply {
                    duration = 200
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            petImageView.setImageBitmap(bitmap)
                            ObjectAnimator.ofFloat(petImageView, "alpha", 0f, 1f).apply {
                                duration = 200
                                start()
                            }
                        }
                    })
                    start()
                }
            } else {
                petImageView.setImageResource(R.drawable.ic_pet_default)
            }
        } ?: run {
            petImageView.setImageResource(R.drawable.ic_pet_default)
        }
    }

    // FIXED: Improved calculateAge function with better error handling
    private fun calculateAge(dateOfBirth: String): String {
        return try {
            val birthDate = dateFormat.parse(dateOfBirth)

            if (birthDate != null) {
                val birthCalendar = Calendar.getInstance().apply { time = birthDate }
                val currentCalendar = Calendar.getInstance()

                var years = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
                var months = currentCalendar.get(Calendar.MONTH) - birthCalendar.get(Calendar.MONTH)

                // If current month is before birth month, subtract a year and add 12 months
                if (months < 0) {
                    years--
                    months += 12
                }

                // If current day is before birth day, subtract a month
                if (currentCalendar.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH)) {
                    months--
                    if (months < 0) {
                        years--
                        months += 12
                    }
                }

                when {
                    years == 0 && months == 0 -> "0 month"
                    years == 0 -> if (months == 1) "1 month" else "$months months"
                    months == 0 -> if (years == 1) "1 year" else "$years years"
                    else -> {
                        val yearText = if (years == 1) "1 year" else "$years years"
                        val monthText = if (months == 1) "1 month" else "$months months"
                        "$yearText $monthText"
                    }
                }
            } else {
                "Unknown age"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown age"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            EDIT_PET_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    // Pet was updated or deleted, check if it still exists
                    petViewModel.getPetById(petId).observe(this) { pet ->
                        if (pet == null) {
                            // Pet was deleted, finish this activity with exit animation
                            finishWithAnimation()
                        }
                        // If pet still exists, the observer will automatically update the UI
                    }
                }
            }
            ADD_MEDICINE_REQUEST_CODE, EDIT_MEDICINE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    // Medicine was added, updated, or deleted
                    // The LiveData observer will automatically refresh the list
                    animateContentUpdate()
                }
            }
            ADD_VACCINATION_REQUEST_CODE, EDIT_VACCINATION_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    // Vaccination was added, updated, or deleted
                    // The LiveData observer will automatically refresh the list
                    animateContentUpdate()
                }
            }
            ADD_APPOINTMENT_REQUEST_CODE, EDIT_APPOINTMENT_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    // Appointment was added, updated, or deleted
                    // The LiveData observer will automatically refresh the list
                    animateContentUpdate()
                }
            }
        }
    }

    private fun animateContentUpdate() {
        // Subtle pulse animation to indicate content update
        val pulse = ObjectAnimator.ofFloat(mainContent, "alpha", 1f, 0.8f, 1f)
        pulse.duration = 300
        pulse.interpolator = AccelerateDecelerateInterpolator()
        pulse.start()
    }

    private fun finishWithAnimation() {
        // Exit animation
        ObjectAnimator.ofFloat(findViewById(R.id.heroSection), "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        ObjectAnimator.ofFloat(mainContent, "translationY", 0f, 100f).apply {
            duration = 300
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    finish()
                }
            })
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        // Start auto-delete monitoring when activity becomes visible
        startAutoDeleteMonitoring()
    }

    override fun onPause() {
        super.onPause()
        // Stop auto-delete monitoring when activity is not visible
        stopAutoDeleteMonitoring()
    }

    // FIXED: Handle options menu item selection (back arrow)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the back arrow button press
                finishWithAnimation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // FIXED: Updated onSupportNavigateUp to properly handle back navigation
    override fun onSupportNavigateUp(): Boolean {
        finishWithAnimation()
        return true
    }

    // FIXED: Updated onBackPressed for newer Android versions
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }

    companion object {
        private const val EDIT_PET_REQUEST_CODE = 1002
        private const val ADD_MEDICINE_REQUEST_CODE = 1003
        private const val EDIT_MEDICINE_REQUEST_CODE = 1004
        private const val ADD_VACCINATION_REQUEST_CODE = 1005
        private const val EDIT_VACCINATION_REQUEST_CODE = 1006
        private const val ADD_APPOINTMENT_REQUEST_CODE = 1007
        private const val EDIT_APPOINTMENT_REQUEST_CODE = 1008
    }
}