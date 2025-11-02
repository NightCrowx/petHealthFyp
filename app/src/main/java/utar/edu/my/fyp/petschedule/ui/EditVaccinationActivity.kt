package utar.edu.my.fyp.petschedule.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Vaccination
import java.text.SimpleDateFormat
import java.util.*

class EditVaccinationActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private var vaccinationId: Long = 0
    private var petId: Long = 0
    private var currentVaccination: Vaccination? = null
    private var dueDate: Date? = null
    private var nextDueDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // UI Elements
    private lateinit var vaccineNameEditText: TextInputEditText
    private lateinit var dueDateEditText: TextInputEditText
    private lateinit var nextDueDateEditText: TextInputEditText
    private lateinit var notesEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var deleteButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_vaccination)

        vaccinationId = intent.getLongExtra("vaccination_id", 0)
        petId = intent.getLongExtra("pet_id", 0)
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        setupToolbar()
        setupViews()
        setupDatePickers()
        setupButtons()
        loadVaccinationData()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Vaccination"
    }

    private fun setupViews() {
        vaccineNameEditText = findViewById(R.id.editTextVaccineName)
        dueDateEditText = findViewById(R.id.editTextDueDate)
        nextDueDateEditText = findViewById(R.id.editTextNextDueDate)
        notesEditText = findViewById(R.id.editTextVaccinationNotes)
        saveButton = findViewById(R.id.buttonSaveVaccination)
        deleteButton = findViewById(R.id.buttonDeleteVaccination)
    }

    private fun setupDatePickers() {
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

    private fun setupButtons() {
        saveButton.setOnClickListener {
            updateVaccination()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun loadVaccinationData() {
        petViewModel.getVaccinationById(vaccinationId).observe(this) { vaccination ->
            vaccination?.let {
                currentVaccination = it
                populateFields(it)
            }
        }
    }

    private fun populateFields(vaccination: Vaccination) {
        vaccineNameEditText.setText(vaccination.vaccineName)

        dueDate = vaccination.dueDate
        dueDateEditText.setText(dateFormat.format(vaccination.dueDate))

        vaccination.nextDueDate?.let {
            nextDueDate = it
            nextDueDateEditText.setText(dateFormat.format(it))
        }

        vaccination.notes?.let {
            notesEditText.setText(it)
        }
    }

    private fun updateVaccination() {
        val vaccineName = vaccineNameEditText.text.toString().trim()
        val notes = notesEditText.text.toString().trim()

        if (vaccineName.isEmpty() || dueDate == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        currentVaccination?.let { vaccination ->
            val updatedVaccination = vaccination.copy(
                vaccineName = vaccineName,
                dueDate = dueDate!!,
                nextDueDate = nextDueDate,
                notes = notes.ifEmpty { null }
            )

            petViewModel.updateVaccination(updatedVaccination)
            //<------------------------------------------------------------------------------>
            // ADD: cancel old vaccination alarms and schedule new exact ones
            try {
                val helper = utar.edu.my.fyp.petschedule.NotificationHelper(this)

                // 1) Cancel old alarms for previously saved dates (due, 7-days-before, next-due)
                currentVaccination?.let { prev ->
                    val oldTriggers = mutableListOf<Long>()
                    fun atNine(d: java.util.Date): Long {
                        val c = java.util.Calendar.getInstance().apply {
                            time = d; set(java.util.Calendar.HOUR_OF_DAY, 9)
                            set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                        }
                        return c.timeInMillis
                    }
                    oldTriggers += atNine(prev.dueDate)
                    oldTriggers += atNine(java.util.Calendar.getInstance().apply { time = prev.dueDate; add(java.util.Calendar.DAY_OF_MONTH, -7) }.time)
                    prev.nextDueDate?.let { nd -> oldTriggers += atNine(nd) }
                    helper.cancelVaccinationAlarmsForTimes(petId, prev.vaccineName, oldTriggers)
                }

                // 2) Schedule new ones for edited dates (due, 7-days-before, next-due), each +5 min reminder
                fun scheduleAtNine(date: java.util.Date) {
                    val cal = java.util.Calendar.getInstance().apply {
                        time = date
                        set(java.util.Calendar.HOUR_OF_DAY, 9)
                        set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                        if (before(java.util.Calendar.getInstance())) add(java.util.Calendar.DAY_OF_MONTH, 1)
                    }
                    val t1 = cal.timeInMillis
                    if (t1 > System.currentTimeMillis() + 1000L) {
                        helper.scheduleVaccinationAlarm(petId, vaccineName, t1, false)
                        val t2 = t1 + 5 * 60_000L
                        if (t2 > System.currentTimeMillis() + 1000L) {
                            helper.scheduleVaccinationAlarm(petId, vaccineName, t2, true)
                        }
                    }
                }

                // due
                scheduleAtNine(dueDate!!)
                // 7-days before
                val pre = java.util.Calendar.getInstance().apply { time = dueDate!!; add(java.util.Calendar.DAY_OF_MONTH, -7) }.time
                scheduleAtNine(pre)
                // next due (optional)
                nextDueDate?.let { scheduleAtNine(it) }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            Toast.makeText(this, "Vaccination updated successfully", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Vaccination")
            .setMessage("Are you sure you want to delete this vaccination? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteVaccination()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteVaccination() {
        currentVaccination?.let { vaccination ->
            petViewModel.deleteVaccination(vaccination)
            Toast.makeText(this, "Vaccination deleted successfully", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
