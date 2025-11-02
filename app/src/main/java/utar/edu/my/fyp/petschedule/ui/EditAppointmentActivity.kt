package utar.edu.my.fyp.petschedule.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Appointment
import java.text.SimpleDateFormat
import java.util.*

class EditAppointmentActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private var appointmentId: Long = 0
    private var petId: Long = 0
    private var currentAppointment: Appointment? = null
    private var appointmentDate: Date? = null
    private var appointmentTime: String = ""
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // UI Elements
    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var timeEditText: TextInputEditText
    private lateinit var locationEditText: TextInputEditText
    private lateinit var reminderRadioGroup: RadioGroup
    private lateinit var saveButton: MaterialButton
    private lateinit var deleteButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_appointment)

        appointmentId = intent.getLongExtra("appointment_id", 0)
        petId = intent.getLongExtra("pet_id", 0)

        if (appointmentId == 0L || petId == 0L) {
            Toast.makeText(this, "Invalid appointment data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        setupToolbar()
        setupViews()
        setupDateTimePickers()
        setupButtons()
        loadAppointmentData()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Appointment"
    }

    private fun setupViews() {
        titleEditText = findViewById(R.id.editTextAppointmentTitle)
        descriptionEditText = findViewById(R.id.editTextAppointmentDescription)
        dateEditText = findViewById(R.id.editTextAppointmentDate)
        timeEditText = findViewById(R.id.editTextAppointmentTime)
        locationEditText = findViewById(R.id.editTextLocation)
        reminderRadioGroup = findViewById(R.id.radioGroupReminderTime)
        saveButton = findViewById(R.id.buttonSaveAppointment)
        deleteButton = findViewById(R.id.buttonDeleteAppointment)
    }

    private fun setupDateTimePickers() {
        dateEditText.setOnClickListener {
            showDatePicker { date ->
                appointmentDate = date
                dateEditText.setText(dateFormat.format(date))
            }
        }

        timeEditText.setOnClickListener {
            showTimePicker { time ->
                appointmentTime = time
                timeEditText.setText(time)
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today (restrict past dates)
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

        datePickerDialog.show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        // If we have an existing time, parse it
        if (appointmentTime.isNotEmpty()) {
            try {
                val timeParts = appointmentTime.split(":")
                calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            } catch (e: Exception) {
                // Use current time if parsing fails
            }
        }

        TimePickerDialog(
            this,
            { _, hour, minute ->
                val time = String.format("%02d:%02d", hour, minute)
                onTimeSelected(time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun setupButtons() {
        saveButton.setOnClickListener {
            updateAppointment()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun loadAppointmentData() {
        petViewModel.getAppointmentById(appointmentId).observe(this) { appointment ->
            appointment?.let {
                currentAppointment = it
                populateFields(it)
            } ?: run {
                Toast.makeText(this, "Appointment not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateFields(appointment: Appointment) {
        titleEditText.setText(appointment.title)
        descriptionEditText.setText(appointment.description ?: "")
        locationEditText.setText(appointment.location ?: "")

        appointmentDate = appointment.appointmentDate
        dateEditText.setText(dateFormat.format(appointment.appointmentDate))

        appointmentTime = appointment.time
        timeEditText.setText(appointment.time)

        // Set reminder time radio button
        when (appointment.reminderTime) {
            15 -> reminderRadioGroup.check(R.id.radio15Minutes)
            30 -> reminderRadioGroup.check(R.id.radio30Minutes)
            60 -> reminderRadioGroup.check(R.id.radio1Hour)
            1440 -> reminderRadioGroup.check(R.id.radio1Day)
            else -> reminderRadioGroup.check(R.id.radio1Hour)
        }
    }

    private fun updateAppointment() {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()

        if (title.isEmpty() || appointmentDate == null || appointmentTime.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val reminderMinutes = when (reminderRadioGroup.checkedRadioButtonId) {
            R.id.radio15Minutes -> 15
            R.id.radio30Minutes -> 30
            R.id.radio1Hour -> 60
            R.id.radio1Day -> 1440
            else -> 60
        }

        currentAppointment?.let { current ->
            val updatedAppointment = current.copy(
                title = title,
                description = description.ifEmpty { null },
                appointmentDate = appointmentDate!!,
                time = appointmentTime,
                location = location.ifEmpty { null },
                reminderTime = reminderMinutes
            )

            petViewModel.updateAppointment(updatedAppointment)
            //<----------------------------------------------------------------------->
            // ADD: re-schedule exact alarms on edit
            try {
                val helper = utar.edu.my.fyp.petschedule.NotificationHelper(this)

                // 1) Cancel old (by previous title)
                currentAppointment?.let { prev ->
                    helper.cancelAppointmentAlarms(petId, prev.title)
                }

                // 2) Compute new triggers from edited fields
                val parts = appointmentTime.split(":")
                val base = java.util.Calendar.getInstance().apply {
                    time = appointmentDate!!
                    set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(java.util.Calendar.MINUTE, parts[1].toInt())
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }

                val trigger1 = base.timeInMillis - (reminderMinutes * 60_000L)
                if (trigger1 > System.currentTimeMillis() + 1000L) {
                    helper.scheduleAppointmentAlarm(
                        petId = petId,
                        appointmentTitle = title,
                        timeLabel = appointmentTime,
                        triggerAtMillis = trigger1,
                        isReminder = false
                    )
                    val trigger2 = trigger1 + 5 * 60_000L
                    if (trigger2 > System.currentTimeMillis() + 1000L) {
                        helper.scheduleAppointmentAlarm(
                            petId = petId,
                            appointmentTitle = title,
                            timeLabel = appointmentTime,
                            triggerAtMillis = trigger2,
                            isReminder = true
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Toast.makeText(this, "Appointment updated successfully", Toast.LENGTH_SHORT).show()

            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Appointment")
            .setMessage("Are you sure you want to delete this appointment? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAppointment()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAppointment() {
        currentAppointment?.let { appointment ->
            petViewModel.deleteAppointment(appointment)
            Toast.makeText(this, "Appointment deleted successfully", Toast.LENGTH_SHORT).show()

            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

