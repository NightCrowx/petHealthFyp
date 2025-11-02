package utar.edu.my.fyp.petschedule.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Appointment
import java.text.SimpleDateFormat
import java.util.*

class AddAppointmentActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private var petId: Long = 0
    private var appointmentDate: Date? = null
    private var appointmentTime: String = ""
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_appointment)

        petId = intent.getLongExtra("pet_id", 0)
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        setupToolbar()
        setupDateTimePickers()
        setupSaveButton()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupDateTimePickers() {
        val appointmentDateEditText = findViewById<TextInputEditText>(R.id.editTextAppointmentDate)
        val appointmentTimeEditText = findViewById<TextInputEditText>(R.id.editTextAppointmentTime)

        appointmentDateEditText.setOnClickListener {
            showDatePicker { date ->
                appointmentDate = date
                appointmentDateEditText.setText(dateFormat.format(date))
            }
        }

        appointmentTimeEditText.setOnClickListener {
            showTimePicker { time ->
                appointmentTime = time
                appointmentTimeEditText.setText(time)
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

    private fun setupSaveButton() {
        findViewById<MaterialButton>(R.id.buttonSaveAppointment).setOnClickListener {
            saveAppointment()
        }
    }

    private fun saveAppointment() {
        val title = findViewById<TextInputEditText>(R.id.editTextAppointmentTitle).text.toString().trim()
        val description = findViewById<TextInputEditText>(R.id.editTextAppointmentDescription).text.toString().trim()
        val location = findViewById<TextInputEditText>(R.id.editTextLocation).text.toString().trim()
        val reminderRadioGroup = findViewById<RadioGroup>(R.id.radioGroupReminderTime)

        if (title.isEmpty() || appointmentDate == null || appointmentTime.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val reminderMinutes = when (reminderRadioGroup.checkedRadioButtonId) {
            R.id.radio15Minutes -> 15
            R.id.radio30Minutes -> 30
            R.id.radio1Hour -> 60
            R.id.radio1Day -> 1440
            else -> 60 // Default to 1 hour
        }

        val appointment = Appointment(
            petId = petId,
            title = title,
            description = description.ifEmpty { null },
            appointmentDate = appointmentDate!!,
            time = appointmentTime,
            location = location.ifEmpty { null },
            reminderTime = reminderMinutes
        )

        petViewModel.insertAppointment(appointment)
        //<-------------------------------------------------------------------->
        val helper = utar.edu.my.fyp.petschedule.NotificationHelper(this)

// Combine date + time
        val parts = appointmentTime.split(":")
        val base = java.util.Calendar.getInstance().apply {
            time = appointmentDate!!
            set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(java.util.Calendar.MINUTE, parts[1].toInt())
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

// fire at (base - reminderMinutes)
        val trigger1 = base.timeInMillis - (appointment.reminderTime * 60_000L)
        if (trigger1 > System.currentTimeMillis() + 1000L) {
            helper.scheduleAppointmentAlarm(
                petId = petId,
                appointmentTitle = title,
                timeLabel = appointmentTime,
                triggerAtMillis = trigger1,
                isReminder = false
            )
        }

// +5 minutes reminder
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

        Toast.makeText(this, "Appointment added successfully", Toast.LENGTH_SHORT).show()

        setResult(RESULT_OK)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}