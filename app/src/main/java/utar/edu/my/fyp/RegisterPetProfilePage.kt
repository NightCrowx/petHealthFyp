package utar.edu.my.fyp

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class RegisterPetProfilePage : AppCompatActivity() {

    private lateinit var etPetName: TextInputEditText
    private lateinit var etPetBirthdate: TextInputEditText
    private lateinit var etPetBreed: TextInputEditText
    private lateinit var etPetType: AutoCompleteTextView
    private lateinit var etPetGender: AutoCompleteTextView
    private lateinit var etPetWeight: TextInputEditText
    private lateinit var btnSavePet: Button
    private lateinit var btnUploadImage: Button
    private lateinit var imgPet: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef: DatabaseReference
    private lateinit var storageRef: FirebaseStorage
    private var selectedImageUri: Uri? = null

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_pet_profile_page)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().getReference("user")
        storageRef = FirebaseStorage.getInstance()

        etPetName = findViewById(R.id.pet_name)
        etPetBirthdate = findViewById(R.id.pet_birthdate)
        etPetBreed = findViewById(R.id.pet_breed)
        etPetType = findViewById(R.id.pet_type)
        etPetGender = findViewById(R.id.pet_gender)
        etPetWeight = findViewById(R.id.pet_weight)
        btnSavePet = findViewById(R.id.save_pet)
        btnUploadImage = findViewById(R.id.btn_upload_pet_image)
        imgPet = findViewById(R.id.img_pet)

        val petTypes = listOf("Dog", "Cat")
        etPetType.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, petTypes))

        val petGenders = listOf("Male", "Female")
        etPetGender.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, petGenders))

        btnUploadImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        etPetBirthdate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    etPetBirthdate.setText(format.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSavePet.setOnClickListener {
            savePetProfile()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imgPet.setImageURI(it)
        }
    }

    private fun savePetProfile() {
        val name = etPetName.text.toString().trim()
        val birthdate = etPetBirthdate.text.toString().trim()
        val breed = etPetBreed.text.toString().trim()
        val type = etPetType.text.toString().trim()
        val gender = etPetGender.text.toString().trim()
        val weight = etPetWeight.text.toString().trim()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (name.isEmpty() || birthdate.isEmpty() || breed.isEmpty() || type.isEmpty() || gender.isEmpty() || weight.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val petId = dbRef.child(userId).child("pets").push().key ?: return

        if (selectedImageUri != null) {
            val storagePath = storageRef.reference.child("pet_images/$userId/$petId.jpg")
            storagePath.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storagePath.downloadUrl.addOnSuccessListener { uri ->
                        val pet = hashMapOf(
                            "petId" to petId,
                            "petName" to name,
                            "petBirthdate" to birthdate,
                            "petBreed" to breed,
                            "petType" to type,
                            "petGender" to gender,
                            "petWeight" to weight,
                            "petImage" to uri.toString()
                        )
                        dbRef.child(userId).child("pets").child(petId).setValue(pet)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Pet profile saved!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, DashboardPage::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to save pet data", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Please upload a pet image", Toast.LENGTH_SHORT).show()
        }
    }
}
