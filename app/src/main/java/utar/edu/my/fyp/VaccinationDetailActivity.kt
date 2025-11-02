package utar.edu.my.fyp

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import utar.edu.my.fyp.petschedule.ui.PetViewModel
import utar.edu.my.fyp.petschedule.Vaccination
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VaccinationDetailActivity : AppCompatActivity() {

    private lateinit var petViewModel: PetViewModel
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var currentVaccination: Vaccination? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vaccination_detail)

        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        val vaccinationId = intent.getLongExtra("vaccinationId", -1)
        val petId = intent.getLongExtra("petId", -1)

        if (vaccinationId != -1L && petId != -1L) {
            loadVaccinationDetails(vaccinationId, petId)
            setupMarkAsCompletedButton(vaccinationId)
            setupCloseButton()
        }
    }

    private fun loadVaccinationDetails(vaccinationId: Long, petId: Long) {
        // Get pet details
        petViewModel.getPetById(petId).observe(this) { pet ->
            pet?.let {
                findViewById<TextView>(R.id.textViewPetName).text = pet.name
                // Load pet image
                loadPetImage(pet.imagePath)
            }
        }

        // Get vaccination details
        petViewModel.getVaccinationById(vaccinationId).observe(this) { vaccination ->
            vaccination?.let {
                currentVaccination = it
                findViewById<TextView>(R.id.textViewVaccineName).text = vaccination.vaccineName
                findViewById<TextView>(R.id.textViewDueDate).text =
                    dateFormat.format(vaccination.dueDate)
                findViewById<TextView>(R.id.textViewStatus).text =
                    if (vaccination.isCompleted) "Completed" else "Pending"
                findViewById<TextView>(R.id.textViewCompletedDate).text =
                    vaccination.completedDate?.let { dateFormat.format(it) } ?: "Not completed yet"
                findViewById<TextView>(R.id.textViewNextDueDate).text =
                    vaccination.nextDueDate?.let { dateFormat.format(it) } ?: "Not scheduled"
                findViewById<TextView>(R.id.textViewNotes).text =
                    vaccination.notes ?: "No additional notes"

                // Update button visibility based on completion status
                updateMarkAsCompletedButton(vaccination.isCompleted)
            }
        }
    }

    private fun setupMarkAsCompletedButton(vaccinationId: Long) {
        val btnMarkAsCompleted = findViewById<MaterialButton>(R.id.btnMarkAsCompleted)

        btnMarkAsCompleted.setOnClickListener {
            currentVaccination?.let { vaccination ->
                // Show confirmation dialog
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Mark as Completed")
                    .setMessage("Are you sure you want to mark this vaccination as completed? This will remove it from your upcoming events.")
                    .setPositiveButton("Yes") { _, _ ->
                        markVaccinationAsCompleted(vaccination)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
    }

    private fun setupCloseButton() {
        val btnClose = findViewById<MaterialButton>(R.id.btnClose)

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun markVaccinationAsCompleted(vaccination: Vaccination) {
        lifecycleScope.launch {
            try {
                // Update vaccination as completed with current date
                val today = Date()
                val updatedVaccination = vaccination.copy(
                    isCompleted = true,
                    completedDate = today
                )
                petViewModel.updateVaccination(updatedVaccination)

                // Show success message
                Toast.makeText(this@VaccinationDetailActivity,
                    "Vaccination marked as completed!", Toast.LENGTH_SHORT).show()

                // Set result to notify calling activity
                setResult(RESULT_OK)

                // Finish activity and return to previous screen
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@VaccinationDetailActivity,
                    "Error updating vaccination: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateMarkAsCompletedButton(isCompleted: Boolean) {
        val btnMarkAsCompleted = findViewById<MaterialButton>(R.id.btnMarkAsCompleted)

        if (isCompleted) {
            btnMarkAsCompleted.text = "Completed"
            btnMarkAsCompleted.isEnabled = false
            btnMarkAsCompleted.alpha = 0.6f
        } else {
            btnMarkAsCompleted.text = "Mark as Completed"
            btnMarkAsCompleted.isEnabled = true
            btnMarkAsCompleted.alpha = 1.0f
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
