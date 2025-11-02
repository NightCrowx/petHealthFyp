package utar.edu.my.fyp.petschedule.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Medicine
import java.text.SimpleDateFormat
import java.util.*

class EditMedicineActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private lateinit var medicineNameEditText: TextInputEditText
    private lateinit var dosageEditText: TextInputEditText
    private lateinit var notesEditText: TextInputEditText
    private lateinit var startDateEditText: TextInputEditText
    private lateinit var endDateEditText: TextInputEditText
    private lateinit var radioGroupFrequency: RadioGroup
    private lateinit var timeChipGroup: ChipGroup
    private lateinit var saveButton: MaterialButton
    private lateinit var deleteButton: MaterialButton

    private var medicineId: Long = 0
    private var petId: Long = 0
    private var currentMedicine: Medicine? = null
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val selectedTimes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_medicine)

        medicineId = intent.getLongExtra("medicine_id", 0)
        petId = intent.getLongExtra("pet_id", 0)
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        initializeViews()
        setupToolbar()
        setupDatePickers()
        setupTimeSelector()
        setupButtons()
        loadMedicineData()
    }

    private fun initializeViews() {
        medicineNameEditText = findViewById(R.id.editTextMedicineName)
        dosageEditText = findViewById(R.id.editTextDosage)
        notesEditText = findViewById(R.id.editTextNotes)
        startDateEditText = findViewById(R.id.editTextStartDate)
        endDateEditText = findViewById(R.id.editTextEndDate)
        radioGroupFrequency = findViewById(R.id.radioGroupFrequency)
        timeChipGroup = findViewById(R.id.timeChipGroup)
        saveButton = findViewById(R.id.buttonSaveMedicine)
        deleteButton = findViewById(R.id.buttonDeleteMedicine)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Edit Medicine"
        }
    }

    private fun setupDatePickers() {
        startDateEditText.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                startDateEditText.setText(dateFormat.format(date))
            }
        }

        endDateEditText.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                endDateEditText.setText(dateFormat.format(date))
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

    private fun setupTimeSelector() {
        // Add time button
        findViewById<MaterialButton>(R.id.buttonAddTime).setOnClickListener {
            showTimePicker()
        }

        // Setup frequency change listener to auto-update time slots
        radioGroupFrequency.setOnCheckedChangeListener { _, checkedId ->
            updateTimeChipsBasedOnFrequency(checkedId)
        }
    }


    private fun showTimePicker() {
        // Check current frequency selection and get max allowed times
        val maxTimes = when (radioGroupFrequency.checkedRadioButtonId) {
            R.id.radioOnceDaily -> 1
            R.id.radioTwiceDaily -> 2
            R.id.radioThreeTimesDaily -> 3
            R.id.radioWeekly -> 1
            else -> 1 // default to 1
        }

        // Check if user has already reached the maximum number of times
        if (selectedTimes.size >= maxTimes) {
            val frequencyName = when (radioGroupFrequency.checkedRadioButtonId) {
                R.id.radioOnceDaily -> "Once Daily"
                R.id.radioTwiceDaily -> "Twice Daily"
                R.id.radioThreeTimesDaily -> "Three Times Daily"
                R.id.radioWeekly -> "Weekly"
                else -> "this frequency"
            }
            Toast.makeText(this, "You can only add $maxTimes time(s) for $frequencyName", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val timeString = String.format("%02d:%02d", hourOfDay, minute)
                if (!selectedTimes.contains(timeString)) {
                    selectedTimes.add(timeString)
                    selectedTimes.sort()
                    updateTimeChips()
                } else {
                    Toast.makeText(this, "This time is already selected", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateTimeChipsBasedOnFrequency(checkedId: Int) {
        // Clear existing times when frequency changes
        selectedTimes.clear()

        when (checkedId) {
            R.id.radioOnceDaily -> {
                // Suggest morning time
                if (selectedTimes.isEmpty()) {
                    selectedTimes.add("09:00")
                }
            }
            R.id.radioTwiceDaily -> {
                // Suggest morning and evening
                selectedTimes.clear()
                selectedTimes.addAll(listOf("09:00", "21:00"))
            }
            R.id.radioThreeTimesDaily -> {
                // Suggest morning, afternoon, evening
                selectedTimes.clear()
                selectedTimes.addAll(listOf("09:00", "15:00", "21:00"))
            }
            R.id.radioWeekly -> {
                // Suggest morning time
                if (selectedTimes.isEmpty()) {
                    selectedTimes.add("09:00")
                }
            }
        }
        updateTimeChips()
    }

    private fun updateTimeChips() {
        timeChipGroup.removeAllViews()

        selectedTimes.forEach { time ->
            val chip = Chip(this).apply {
                text = time
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedTimes.remove(time)
                    updateTimeChips()
                }
            }
            timeChipGroup.addView(chip)
        }
    }

    private fun setupButtons() {
        saveButton.text = "Update Medicine"
        saveButton.setOnClickListener {
            if (validateInputs()) {
                updateMedicine()
            }
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun loadMedicineData() {
        petViewModel.getMedicineById(medicineId).observe(this) { medicine ->
            medicine?.let {
                currentMedicine = it
                populateFields(it)
            }
        }
    }

    private fun populateFields(medicine: Medicine) {
        medicineNameEditText.setText(medicine.medicineName)
        dosageEditText.setText(medicine.dosage)
        notesEditText.setText(medicine.notes ?: "")

        // Set dates
        startDate = medicine.startDate
        startDateEditText.setText(dateFormat.format(medicine.startDate))

        medicine.endDate?.let {
            endDate = it
            endDateEditText.setText(dateFormat.format(it))
        }

        // Set frequency radio button
        when (medicine.frequency) {
            "daily" -> radioGroupFrequency.check(R.id.radioOnceDaily)
            "twice_daily" -> radioGroupFrequency.check(R.id.radioTwiceDaily)
            "three_times_daily" -> radioGroupFrequency.check(R.id.radioThreeTimesDaily)
            "weekly" -> radioGroupFrequency.check(R.id.radioWeekly)
        }

        // Set times
        selectedTimes.clear()
        medicine.time.split(",").forEach { time ->
            selectedTimes.add(time.trim())
        }
        updateTimeChips()
    }

    private fun validateInputs(): Boolean {
        val medicineName = medicineNameEditText.text.toString().trim()
        val dosage = dosageEditText.text.toString().trim()

        return when {
            medicineName.isEmpty() -> {
                showError("Please enter medicine name")
                false
            }
            dosage.isEmpty() -> {
                showError("Please enter dosage")
                false
            }
            startDate == null -> {
                showError("Please select start date")
                false
            }
            selectedTimes.isEmpty() -> {
                showError("Please add at least one time")
                false
            }
            radioGroupFrequency.checkedRadioButtonId == -1 -> {
                showError("Please select frequency")
                false
            }
            !validateTimeCount() -> {
                // This will show appropriate error message
                false
            }
            else -> true
        }
    }

    private fun validateTimeCount(): Boolean {
        val requiredTimes = when (radioGroupFrequency.checkedRadioButtonId) {
            R.id.radioOnceDaily -> 1
            R.id.radioTwiceDaily -> 2
            R.id.radioThreeTimesDaily -> 3
            R.id.radioWeekly -> 1
            else -> 1
        }

        val frequencyName = when (radioGroupFrequency.checkedRadioButtonId) {
            R.id.radioOnceDaily -> "Once Daily"
            R.id.radioTwiceDaily -> "Twice Daily"
            R.id.radioThreeTimesDaily -> "Three Times Daily"
            R.id.radioWeekly -> "Weekly"
            else -> "this frequency"
        }

        return when {
            selectedTimes.size < requiredTimes -> {
                showError("$frequencyName requires exactly $requiredTimes time(s). You have selected ${selectedTimes.size}.")
                false
            }
            selectedTimes.size > requiredTimes -> {
                showError("$frequencyName allows only $requiredTimes time(s). You have selected ${selectedTimes.size}.")
                false
            }
            else -> true
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateMedicine() {
        try {
            val medicineName = medicineNameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()
            val notes = notesEditText.text.toString().trim()

            val frequency = when (radioGroupFrequency.checkedRadioButtonId) {
                R.id.radioOnceDaily -> "daily"
                R.id.radioTwiceDaily -> "twice_daily"
                R.id.radioThreeTimesDaily -> "three_times_daily"
                R.id.radioWeekly -> "weekly"
                else -> "daily"
            }

            val timeString = selectedTimes.joinToString(",")

            val updatedMedicine = currentMedicine?.copy(
                medicineName = medicineName,
                dosage = dosage,
                frequency = frequency,
                startDate = startDate!!,
                endDate = endDate,
                time = timeString,
                notes = notes.ifEmpty { null }
            )

            updatedMedicine?.let {
                petViewModel.updateMedicine(it)
                //<---------------------------------------------------------------------------->
                // ADD: cancel old alarms and schedule new exact ones
                try {
                    val helper = utar.edu.my.fyp.petschedule.NotificationHelper(this)

                    // 1) Cancel old alarms (by old time labels)
                    currentMedicine?.let { prev ->
                        val oldTimes = prev.time.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                        helper.cancelMedicineAlarms(petId, prev.medicineName, oldTimes)
                    }

                    // 2) Schedule next occurrence for each selected time (and +5 min reminders)
                    selectedTimes.forEach { t ->
                        val parts = t.split(":")
                        val cal = java.util.Calendar.getInstance().apply {
                            time = startDate!!                 // use edited startDate as base
                            set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                            set(java.util.Calendar.MINUTE, parts[1].toInt())
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                            if (before(java.util.Calendar.getInstance())) add(java.util.Calendar.DAY_OF_MONTH, 1)
                        }
                        // main
                        if (cal.timeInMillis > System.currentTimeMillis() + 1000L) {
                            helper.scheduleMedicineAlarm(
                                petId = petId,
                                medicineName = medicineName,
                                dosage = dosage,
                                timeLabel = t,
                                triggerAtMillis = cal.timeInMillis,
                                isReminder = false
                            )
                        }
                        // +5 minutes (only if still future)
                        val rem = cal.timeInMillis + 5 * 60_000L
                        if (rem > System.currentTimeMillis() + 1000L) {
                            helper.scheduleMedicineAlarm(
                                petId = petId,
                                medicineName = medicineName,
                                dosage = dosage,
                                timeLabel = t,
                                triggerAtMillis = rem,
                                isReminder = true
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Toast.makeText(this, "Medicine updated successfully!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error updating medicine: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Medicine")
            .setMessage("Are you sure you want to delete ${currentMedicine?.medicineName}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteMedicine()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMedicine() {
        currentMedicine?.let { medicine ->
            petViewModel.deleteMedicine(medicine)
            Toast.makeText(this, "Medicine deleted successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}