package utar.edu.my.fyp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import utar.edu.my.fyp.petschedule.ui.PetViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MedicineDetailActivity : AppCompatActivity() {

    private lateinit var petViewModel: PetViewModel
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_detail)

        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        val medicineId = intent.getLongExtra("medicineId", -1)
        val petId = intent.getLongExtra("petId", -1)

        if (medicineId != -1L && petId != -1L) {
            loadMedicineDetails(medicineId, petId)
        }

        setupCloseButton()
    }

    private fun setupCloseButton() {
        val closeButton = findViewById<MaterialButton>(R.id.buttonClose)
        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun loadMedicineDetails(medicineId: Long, petId: Long) {
        // Get pet details
        petViewModel.getPetById(petId).observe(this) { pet ->
            pet?.let {
                findViewById<TextView>(R.id.textViewPetName).text = pet.name

                // Load pet image
                loadPetImage(pet.imagePath)
            }
        }

        // Get medicine details
        petViewModel.getMedicineById(medicineId).observe(this) { medicine ->
            medicine?.let {
                findViewById<TextView>(R.id.textViewMedicineName).text = medicine.medicineName
                findViewById<TextView>(R.id.textViewDosage).text = medicine.dosage
                findViewById<TextView>(R.id.textViewFrequency).text =
                    medicine.frequency.replace("_", " ").replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                findViewById<TextView>(R.id.textViewStartDate).text =
                    dateFormat.format(medicine.startDate)
                findViewById<TextView>(R.id.textViewEndDate).text =
                    medicine.endDate?.let { dateFormat.format(it) } ?: "Ongoing"

                // Format time display
                val times = medicine.time.split(",").map { it.trim() }
                findViewById<TextView>(R.id.textViewTime).text = times.joinToString(", ")

                findViewById<TextView>(R.id.textViewNotes).text =
                    medicine.notes ?: "No additional notes"
                findViewById<TextView>(R.id.textViewStatus).text =
                    if (medicine.isActive) "Active" else "Inactive"
            }
        }
    }

    private fun loadPetImage(imagePath: String?) {
        val imageView = findViewById<ImageView>(R.id.imageViewPet)

        if (!imagePath.isNullOrEmpty()) {
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                // Load image from file path using Glide
                Glide.with(this)
                    .load(imageFile)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.ic_pet_default) // Default placeholder
                        .error(R.drawable.ic_pet_default) // Error fallback
                        .centerCrop())
                    .into(imageView)
            } else {
                // File doesn't exist, use default image
                imageView.setImageResource(R.drawable.ic_pet_default)
            }
        } else {
            // No image path, use default image
            imageView.setImageResource(R.drawable.ic_pet_default)
        }
    }
}


//package utar.edu.my.fyp
//
//import android.os.Bundle
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import com.bumptech.glide.Glide
//import com.bumptech.glide.request.RequestOptions
//import com.google.android.material.button.MaterialButton
//import utar.edu.my.fyp.petschedule.ui.PetViewModel
//import utar.edu.my.fyp.petschedule.Medicine
//import kotlinx.coroutines.launch
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.*
//
//class MedicineDetailActivity : AppCompatActivity() {
//
//    private lateinit var petViewModel: PetViewModel
//    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//    private var currentMedicine: Medicine? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_medicine_detail)
//
//        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]
//
//        val medicineId = intent.getLongExtra("medicineId", -1)
//        val petId = intent.getLongExtra("petId", -1)
//
//    }
//
//    private fun loadMedicineDetails(medicineId: Long, petId: Long) {
//        // Get pet details
//        petViewModel.getPetById(petId).observe(this) { pet ->
//            pet?.let {
//                findViewById<TextView>(R.id.textViewPetName).text = pet.name
//                // Load pet image
//                loadPetImage(pet.imagePath)
//            }
//        }
//
//        // Get medicine details
//        petViewModel.getMedicineById(medicineId).observe(this) { medicine ->
//            medicine?.let {
//                currentMedicine = it
//                findViewById<TextView>(R.id.textViewMedicineName).text = medicine.medicineName
//                findViewById<TextView>(R.id.textViewDosage).text = medicine.dosage
//                findViewById<TextView>(R.id.textViewFrequency).text =
//                    medicine.frequency.replace("_", " ").replaceFirstChar { char ->
//                        if (char.isLowerCase()) char.titlecase() else char.toString()
//                    }
//                findViewById<TextView>(R.id.textViewStartDate).text =
//                    dateFormat.format(medicine.startDate)
//                findViewById<TextView>(R.id.textViewEndDate).text =
//                    medicine.endDate?.let { dateFormat.format(it) } ?: "Ongoing"
//
//                // Format time display
//                val times = medicine.time.split(",").map { it.trim() }
//                findViewById<TextView>(R.id.textViewTime).text = times.joinToString(", ")
//
//                findViewById<TextView>(R.id.textViewNotes).text =
//                    medicine.notes ?: "No additional notes"
//                findViewById<TextView>(R.id.textViewStatus).text =
//                    if (medicine.isActive) "Active" else "Inactive"
//
//            }
//        }
//    }
//
//
//
//
//
//
//
//    private fun loadPetImage(imagePath: String?) {
//        val imageView = findViewById<ImageView>(R.id.imageViewPet)
//
//        if (!imagePath.isNullOrEmpty()) {
//            val imageFile = File(imagePath)
//            if (imageFile.exists()) {
//                // Load image from file path using Glide
//                Glide.with(this)
//                    .load(imageFile)
//                    .apply(RequestOptions()
//                        .placeholder(R.drawable.ic_pet_default) // Default placeholder
//                        .error(R.drawable.ic_pet_default) // Error fallback
//                        .centerCrop())
//                    .into(imageView)
//            } else {
//                // File doesn't exist, use default image
//                imageView.setImageResource(R.drawable.ic_pet_default)
//            }
//        } else {
//            // No image path, use default image
//            imageView.setImageResource(R.drawable.ic_pet_default)
//        }
//    }
//}
