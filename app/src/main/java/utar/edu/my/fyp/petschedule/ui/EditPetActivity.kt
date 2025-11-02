package utar.edu.my.fyp.petschedule.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Pet
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditPetActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private lateinit var petImageView: ImageView
    private lateinit var genderAutoComplete: MaterialAutoCompleteTextView
    private lateinit var petTypeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var dateOfBirthEditText: TextInputEditText
    private lateinit var petNameEditText: TextInputEditText
    private lateinit var breedEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var deleteButton: MaterialButton

    private var selectedImageUri: Uri? = null
    private var imagePath: String? = null
    private val calendar = Calendar.getInstance()
    private var petId: Long = 0
    private var currentPet: Pet? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            petImageView.setImageURI(it)
            saveImageToInternalStorage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet) // Reusing the same layout

        petId = intent.getLongExtra("pet_id", 0)
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        initializeViews()
        setupToolbar()
        setupSpinners()
        setupDatePicker()
        setupImagePicker()
        setupButtons()
        loadPetData()
    }

    private fun initializeViews() {
        petImageView = findViewById(R.id.imageViewPet)
        genderAutoComplete = findViewById(R.id.autoCompleteGender)
        petTypeAutoComplete = findViewById(R.id.autoCompletePetType)
        dateOfBirthEditText = findViewById(R.id.editTextDateOfBirth)
        petNameEditText = findViewById(R.id.editTextPetName)
        breedEditText = findViewById(R.id.editTextBreed)
        saveButton = findViewById(R.id.buttonSavePet)

        // Create delete button - but add it in setupButtons() after layout is ready
        deleteButton = MaterialButton(this).apply {
            text = "Delete Pet"
            setBackgroundColor(ContextCompat.getColor(this@EditPetActivity, android.R.color.holo_red_dark))
            setTextColor(ContextCompat.getColor(this@EditPetActivity, android.R.color.white))
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Edit Pet"
        }
    }

    private fun setupSpinners() {
        // Gender Dropdown
        val genderOptions = arrayOf("Male", "Female")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genderOptions)
        genderAutoComplete.setAdapter(genderAdapter)

        // Pet Type Dropdown
        val petTypeOptions = arrayOf("Dog", "Cat")
        val petTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, petTypeOptions)
        petTypeAutoComplete.setAdapter(petTypeAdapter)
    }

    private fun setupDatePicker() {
        dateOfBirthEditText.apply {
            isFocusable = false
            isClickable = true
            setOnClickListener {
                showDatePickerDialog()
            }
        }
    }

//    private fun showDatePickerDialog() {
//        val datePickerDialog = DatePickerDialog(
//            this,
//            { _, year, month, dayOfMonth ->
//                calendar.set(Calendar.YEAR, year)
//                calendar.set(Calendar.MONTH, month)
//                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
//
//                val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
//                val formattedDate = dateFormat.format(calendar.time)
//                dateOfBirthEditText.setText(formattedDate)
//
//                // Auto-calculate age
//                calculateAndSetAge()
//            },
//            calendar.get(Calendar.YEAR),
//            calendar.get(Calendar.MONTH),
//            calendar.get(Calendar.DAY_OF_MONTH)
//        )
//
//        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
//        datePickerDialog.show()
//    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)
                dateOfBirthEditText.setText(formattedDate)

                // Remove this line: calculateAndSetAge()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

//    private fun calculateAndSetAge() {
//        val currentCalendar = Calendar.getInstance()
//        val birthCalendar = calendar.clone() as Calendar
//
//        var years = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
//        var months = currentCalendar.get(Calendar.MONTH) - birthCalendar.get(Calendar.MONTH)
//
//        // If current month is before birth month, subtract a year and add 12 months
//        if (months < 0) {
//            years--
//            months += 12
//        }
//
//        // If current day is before birth day, subtract a month
//        if (currentCalendar.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH)) {
//            months--
//            if (months < 0) {
//                years--
//                months += 12
//            }
//        }
//
//        val ageString = when {
//            years == 0 && months == 0 -> "0 month"
//            years == 0 -> if (months == 1) "1 month" else "$months months"
//            months == 0 -> if (years == 1) "1 year" else "$years years"
//            else -> {
//                val yearText = if (years == 1) "1 year" else "$years years"
//                val monthText = if (months == 1) "1 month" else "$months months"
//                "$yearText $monthText"
//            }
//        }
//
//        ageEditText.setText(ageString)
//    }

    private fun setupImagePicker() {
        petImageView.setOnClickListener {
            if (checkImagePermission()) {
                openImagePicker()
            } else {
                requestImagePermission()
            }
        }
    }

    private fun checkImagePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestImagePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_PERMISSION_READ_EXTERNAL_STORAGE
        )
    }

    private fun openImagePicker() {
        try {
            imagePickerLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening image picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val filename = "pet_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, filename)
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            imagePath = file.absolutePath
            Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
            imagePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun setupButtons() {
        saveButton.text = "Update Pet"
        saveButton.setOnClickListener {
            if (validateInputs()) {
                updatePet()
            }
        }

        // Add delete button to layout properly
        val parent = saveButton.parent as android.view.ViewGroup
        val layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Add some margin
        if (layoutParams is android.view.ViewGroup.MarginLayoutParams) {
            layoutParams.topMargin = 16
        }

        parent.addView(deleteButton, layoutParams)

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun loadPetData() {
        petViewModel.getPetById(petId).observe(this) { pet ->
            pet?.let {
                currentPet = it
                populateFields(it)
            }
        }
    }

    private fun populateFields(pet: Pet) {
        petNameEditText.setText(pet.name)
        breedEditText.setText(pet.breed)


        dateOfBirthEditText.setText(pet.dateOfBirth)

        // Set dropdowns
        genderAutoComplete.setText(pet.gender, false)
        petTypeAutoComplete.setText(pet.petType, false)

        // Load existing image
        pet.imagePath?.let { path ->
            imagePath = path
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                petImageView.setImageBitmap(bitmap)
            }
        }

        // Parse date for calendar
        try {
            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            val date = dateFormat.parse(pet.dateOfBirth)
            date?.let {
                calendar.time = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//    private fun calculateAndDisplayAge(dateOfBirth: String) {
//        try {
//            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
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
//                val ageString = when {
//                    years == 0 && months == 0 -> "0 month"
//                    years == 0 -> if (months == 1) "1 month" else "$months months"
//                    months == 0 -> if (years == 1) "1 year" else "$years years"
//                    else -> {
//                        val yearText = if (years == 1) "1 year" else "$years years"
//                        val monthText = if (months == 1) "1 month" else "$months months"
//                        "$yearText $monthText"
//                    }
//                }
//
//                ageEditText.setText(ageString)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            ageEditText.setText("Unknown")
//        }
//    }

//    private fun validateInputs(): Boolean {
//        val name = petNameEditText.text.toString().trim()
//        val breed = breedEditText.text.toString().trim()
//        val ageText = ageEditText.text.toString().trim()
//        val dateOfBirth = dateOfBirthEditText.text.toString().trim()
//        val gender = genderAutoComplete.text.toString()
//        val petType = petTypeAutoComplete.text.toString()
//
//        return when {
//            name.isEmpty() -> {
//                showError("Please enter pet name")
//                false
//            }
//            breed.isEmpty() -> {
//                showError("Please enter breed")
//                false
//            }
//            ageText.isEmpty() -> {
//                showError("Please enter age")
//                false
//            }
//            dateOfBirth.isEmpty() -> {
//                showError("Please select date of birth")
//                false
//            }
//            gender == "Select Gender" || gender.isEmpty() -> {
//                showError("Please select gender")
//                false
//            }
//            petType == "Select Pet Type" || petType.isEmpty() -> {
//                showError("Please select pet type")
//                false
//            }
//            else -> true
//        }
//    }

    private fun validateInputs(): Boolean {
        val name = petNameEditText.text.toString().trim()
        val breed = breedEditText.text.toString().trim()
        val dateOfBirth = dateOfBirthEditText.text.toString().trim()
        val gender = genderAutoComplete.text.toString().trim()
        val petType = petTypeAutoComplete.text.toString().trim()

        return when {
            name.isEmpty() -> {
                showError("Please enter pet name")
                false
            }
            breed.isEmpty() -> {
                showError("Please enter breed")
                false
            }
            dateOfBirth.isEmpty() -> {
                showError("Please select date of birth")
                false
            }
            gender.isEmpty() -> {
                showError("Please select gender")
                false
            }
            petType.isEmpty() -> {
                showError("Please select pet type")
                false
            }
            else -> true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updatePet() {
        try {
            val name = petNameEditText.text.toString().trim()
            val breed = breedEditText.text.toString().trim()
            val gender = genderAutoComplete.text.toString().trim()
            val petType = petTypeAutoComplete.text.toString().trim()
            val dateOfBirth = dateOfBirthEditText.text.toString().trim()

            val updatedPet = currentPet?.copy(
                name = name,
                breed = breed,
                gender = gender,
                dateOfBirth = dateOfBirth,
                petType = petType,
                imagePath = imagePath ?: currentPet?.imagePath
            )

            updatedPet?.let {
                petViewModel.updatePet(it)
                Toast.makeText(this, "Pet updated successfully!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error updating pet: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Pet")
            .setMessage("Are you sure you want to delete ${currentPet?.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePet()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePet() {
        currentPet?.let { pet ->
            petViewModel.deletePet(pet)

            // Delete image file if exists
            pet.imagePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Toast.makeText(this, "Pet deleted successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_PERMISSION_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker()
                } else {
                    Toast.makeText(
                        this,
                        "Permission denied. Cannot access images.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1001
    }
}