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
import utar.edu.my.fyp.petschedule.Appointment
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AppointmentDetailActivity : AppCompatActivity() {

    private lateinit var petViewModel: PetViewModel
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var currentAppointment: Appointment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_detail)

        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        val appointmentId = intent.getLongExtra("appointmentId", -1)
        val petId = intent.getLongExtra("petId", -1)

        if (appointmentId != -1L && petId != -1L) {
            loadAppointmentDetails(appointmentId, petId)
            setupMarkAsCompletedButton(appointmentId)
            setupCloseButton()
        }
    }

    private fun loadAppointmentDetails(appointmentId: Long, petId: Long) {
        // Get pet details
        petViewModel.getPetById(petId).observe(this) { pet ->
            pet?.let {
                findViewById<TextView>(R.id.textViewPetName).text = pet.name
                loadPetImage(pet.imagePath)
            }
        }

        // Get appointment details
        petViewModel.getAppointmentById(appointmentId).observe(this) { appointment ->
            appointment?.let {
                currentAppointment = it
                findViewById<TextView>(R.id.textViewAppointmentTitle).text = appointment.title
                findViewById<TextView>(R.id.textViewAppointmentDescription).text =
                    appointment.description ?: "No description provided"
                findViewById<TextView>(R.id.textViewAppointmentDate).text =
                    dateFormat.format(appointment.appointmentDate)
                findViewById<TextView>(R.id.textViewAppointmentTime).text = appointment.time
                findViewById<TextView>(R.id.textViewAppointmentLocation).text =
                    appointment.location ?: "No location specified"
                findViewById<TextView>(R.id.textViewReminderTime).text =
                    "${appointment.reminderTime} minutes before"

                // Update button visibility based on completion status
                updateMarkAsCompletedButton(appointment.isCompleted)
            }
        }
    }

    private fun setupMarkAsCompletedButton(appointmentId: Long) {
        val btnMarkAsCompleted = findViewById<MaterialButton>(R.id.btnMarkAsCompleted)

        btnMarkAsCompleted.setOnClickListener {
            currentAppointment?.let { appointment ->
                // Show confirmation dialog
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Mark as Completed")
                    .setMessage("Are you sure you want to mark this appointment as completed? This will remove it from your upcoming events.")
                    .setPositiveButton("Yes") { _, _ ->
                        markAppointmentAsCompleted(appointment)
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

    private fun markAppointmentAsCompleted(appointment: Appointment) {
        lifecycleScope.launch {
            try {
                // Update appointment as completed
                val updatedAppointment = appointment.copy(isCompleted = true)
                petViewModel.updateAppointment(updatedAppointment)

                // Show success message
                Toast.makeText(this@AppointmentDetailActivity,
                    "Appointment marked as completed!", Toast.LENGTH_SHORT).show()

                // Set result to notify calling activity
                setResult(RESULT_OK)

                // Finish activity and return to previous screen
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@AppointmentDetailActivity,
                    "Error updating appointment: ${e.message}", Toast.LENGTH_LONG).show()
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
                    .apply(
                        RequestOptions()
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