package utar.edu.my.fyp.petschedule.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import utar.edu.my.fyp.MainPage
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Pet
import utar.edu.my.fyp.petschedule.adapters.UserSessionManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddPetActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private lateinit var petImageView: ImageView
    private lateinit var genderAutoComplete: MaterialAutoCompleteTextView
    private lateinit var petTypeAutoComplete: MaterialAutoCompleteTextView
    private lateinit var dateOfBirthEditText: TextInputEditText
    private lateinit var petNameEditText: TextInputEditText
    private lateinit var breedEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton

    private var selectedImageUri: Uri? = null
    private var imagePath: String? = null
    private val calendar = Calendar.getInstance()

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
        setContentView(R.layout.activity_add_pet)

        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        initializeViews()
        setupToolbar()
        setupDropdowns()
        setupDatePicker()
        setupImagePicker()
        setupSaveButton()
    }

    private fun initializeViews() {
        petImageView = findViewById(R.id.imageViewPet)
        genderAutoComplete = findViewById(R.id.autoCompleteGender)
        petTypeAutoComplete = findViewById(R.id.autoCompletePetType)
        dateOfBirthEditText = findViewById(R.id.editTextDateOfBirth)
        petNameEditText = findViewById(R.id.editTextPetName)
        breedEditText = findViewById(R.id.editTextBreed)
        saveButton = findViewById(R.id.buttonSavePet)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add Pet"
        }
    }

    private fun setupDropdowns() {
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

                // Remove the calculateAndSetAge() call
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set max date to today (can't select future dates)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }


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
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun requestImagePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE
            )
        }
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

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (validateInputs()) {
                savePet()
            }
        }
    }



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



    private fun savePet() {
        try {
            val name = petNameEditText.text.toString().trim()
            val breed = breedEditText.text.toString().trim()
            val gender = genderAutoComplete.text.toString().trim()
            val petType = petTypeAutoComplete.text.toString().trim()
            val dateOfBirth = dateOfBirthEditText.text.toString().trim()

            // GET CURRENT USER ID
            val userId = UserSessionManager.getCurrentUserId(this)
            if (userId == null) {
                Toast.makeText(this, "User not logged in. Please log in again.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainPage::class.java))
                finish()
                return
            }

            // Create pet without age field - it will be calculated dynamically
            val pet = Pet(
                name = name,
                breed = breed,
                gender = gender,
                dateOfBirth = dateOfBirth,
                petType = petType,
                imagePath = imagePath,
                userId = userId
            )

            // Save pet to database
            petViewModel.insertPet(pet)

            Toast.makeText(this, "Pet added successfully!", Toast.LENGTH_SHORT).show()

            setResult(RESULT_OK)
            finish()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error saving pet: ${e.message}", Toast.LENGTH_LONG).show()
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

    override fun onBackPressed() {
        super.onBackPressed()
        // Optional: Show confirmation dialog if user has entered data
        finish()
    }

    companion object {
        private const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1001
    }
}
