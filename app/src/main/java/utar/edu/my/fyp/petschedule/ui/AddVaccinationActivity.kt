package utar.edu.my.fyp.petschedule.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Vaccination
import java.text.SimpleDateFormat
import java.util.*

class AddVaccinationActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private var petId: Long = 0
    private var dueDate: Date? = null
    private var nextDueDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_vaccination)

        petId = intent.getLongExtra("pet_id", 0)
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        setupToolbar()
        setupDatePickers()
        setupSaveButton()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupDatePickers() {
        val dueDateEditText = findViewById<TextInputEditText>(R.id.editTextDueDate)
        val nextDueDateEditText = findViewById<TextInputEditText>(R.id.editTextNextDueDate)

        dueDateEditText.setOnClickListener {
            showDatePicker { date ->
                dueDate = date
                dueDateEditText.setText(dateFormat.format(date))
            }
        }

        nextDueDateEditText.setOnClickListener {
            showDatePicker { date ->
                nextDueDate = date
                nextDueDateEditText.setText(dateFormat.format(date))
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

    private fun setupSaveButton() {
        findViewById<MaterialButton>(R.id.buttonSaveVaccination).setOnClickListener {
            saveVaccination()
        }
    }

    private fun saveVaccination() {
        val vaccineName = findViewById<TextInputEditText>(R.id.editTextVaccineName).text.toString().trim()
        val notes = findViewById<TextInputEditText>(R.id.editTextVaccinationNotes).text.toString().trim()

        if (vaccineName.isEmpty() || dueDate == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val vaccination = Vaccination(
            petId = petId,
            vaccineName = vaccineName,
            dueDate = dueDate!!,
            nextDueDate = nextDueDate,
            notes = notes.ifEmpty { null }
        )

        petViewModel.insertVaccination(vaccination)
        //<------------------------------------------------------------------->
        val helper = utar.edu.my.fyp.petschedule.NotificationHelper(this)

        fun scheduleAtNine(date: java.util.Date, isReminder: Boolean = false) {
            val cal = java.util.Calendar.getInstance().apply {
                time = date
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (before(java.util.Calendar.getInstance())) add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            helper.scheduleVaccinationAlarm(
                petId = petId,
                vaccineName = vaccineName,
                triggerAtMillis = cal.timeInMillis,
                isReminder = isReminder
            )
            val rem = cal.timeInMillis + 5 * 60_000L
            if (rem > System.currentTimeMillis()) {
                helper.scheduleVaccinationAlarm(
                    petId = petId,
                    vaccineName = vaccineName,
                    triggerAtMillis = rem,
                    isReminder = true
                )
            }
        }

// due date at 09:00 (+5)
        scheduleAtNine(dueDate!!)

// 7 days before due at 09:00
        val pre = java.util.Calendar.getInstance().apply { time = dueDate!!; add(java.util.Calendar.DAY_OF_MONTH, -7) }.time
        scheduleAtNine(pre)

// optional next due at 09:00
        nextDueDate?.let { scheduleAtNine(it) }

        Toast.makeText(this, "Vaccination added successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}