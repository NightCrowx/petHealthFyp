package utar.edu.my.fyp.petschedule.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Medicine
import java.text.SimpleDateFormat
import java.util.*

class AddMedicineActivity : AppCompatActivity() {
    private lateinit var petViewModel: PetViewModel
    private lateinit var timeChipGroup: ChipGroup
    private var petId: Long = 0
    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val selectedTimes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine) // Update this layout

        petId = intent.getLongExtra("pet_id", 0)
        petViewModel = ViewModelProvider(this)[PetViewModel::class.java]

        setupToolbar()
        setupDatePickers()
        setupTimeSelector()
        setupSaveButton()
    }


    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupDatePickers() {
        val startDateEditText = findViewById<TextInputEditText>(R.id.editTextStartDate)
        val endDateEditText = findViewById<TextInputEditText>(R.id.editTextEndDate)

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

    private fun setupTimeSelector() {
        timeChipGroup = findViewById(R.id.timeChipGroup)

        // Add time button
        findViewById<MaterialButton>(R.id.buttonAddTime).setOnClickListener {
            showTimePicker()
        }

        // Setup frequency change listener to auto-update time slots
        val radioGroupFrequency = findViewById<RadioGroup>(R.id.radioGroupFrequency)
        radioGroupFrequency.setOnCheckedChangeListener { _, checkedId ->
            updateTimeChipsBasedOnFrequency(checkedId)
        }

        // Set default time for once daily
        selectedTimes.add("09:00")
        updateTimeChips()
    }

    private fun showTimePicker() {
        // Check current frequency selection and get max allowed times
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupFrequency)
        val maxTimes = when (radioGroup.checkedRadioButtonId) {
            R.id.radioOnceDaily -> 1
            R.id.radioTwiceDaily -> 2
            R.id.radioThreeTimesDaily -> 3
            R.id.radioWeekly -> 1
            else -> 1 // default to 1
        }

        // Check if user has already reached the maximum number of times
        if (selectedTimes.size >= maxTimes) {
            val frequencyName = when (radioGroup.checkedRadioButtonId) {
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
                selectedTimes.add("09:00")
            }
            R.id.radioTwiceDaily -> {
                selectedTimes.addAll(listOf("09:00", "21:00"))
            }
            R.id.radioThreeTimesDaily -> {
                selectedTimes.addAll(listOf("09:00", "15:00", "21:00"))
            }
            R.id.radioWeekly -> {
                selectedTimes.add("09:00")
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
        findViewById<MaterialButton>(R.id.buttonSaveMedicine).setOnClickListener {
            saveMedicine()
        }
    }


    private fun saveMedicine() {
        val medicineName = findViewById<TextInputEditText>(R.id.editTextMedicineName).text.toString().trim()
        val dosage = findViewById<TextInputEditText>(R.id.editTextDosage).text.toString().trim()
        val notes = findViewById<TextInputEditText>(R.id.editTextNotes).text.toString().trim()

        // Basic field validation
        if (medicineName.isEmpty() || dosage.isEmpty() || selectedTimes.isEmpty() || startDate == null) {
            Toast.makeText(this, "Please fill all required fields and add at least one time", Toast.LENGTH_SHORT).show()
            return
        }

        // Frequency-specific validation
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupFrequency)
        val requiredTimeSlots = when (radioGroup.checkedRadioButtonId) {
            R.id.radioOnceDaily -> 1
            R.id.radioTwiceDaily -> 2
            R.id.radioThreeTimesDaily -> 3
            R.id.radioWeekly -> 1
            else -> 1
        }

        val frequencyName = when (radioGroup.checkedRadioButtonId) {
            R.id.radioOnceDaily -> "Once Daily"
            R.id.radioTwiceDaily -> "Twice Daily"
            R.id.radioThreeTimesDaily -> "Three Times Daily"
            R.id.radioWeekly -> "Weekly"
            else -> "Once Daily"
        }

        // Validate time slots count matches frequency
        when {
            selectedTimes.size < requiredTimeSlots -> {
                Toast.makeText(this,
                    "Please add ${requiredTimeSlots - selectedTimes.size} more time slot(s) for $frequencyName",
                    Toast.LENGTH_LONG).show()
                return
            }
            selectedTimes.size > requiredTimeSlots -> {
                Toast.makeText(this,
                    "$frequencyName requires only $requiredTimeSlots time slot(s). Please remove the extra ones.",
                    Toast.LENGTH_LONG).show()
                return
            }
        }

        // If validation passes, proceed with saving
        val frequency = when (radioGroup.checkedRadioButtonId) {
            R.id.radioOnceDaily -> "daily"
            R.id.radioTwiceDaily -> "twice_daily"
            R.id.radioThreeTimesDaily -> "three_times_daily"
            R.id.radioWeekly -> "weekly"
            else -> "daily"
        }

        val timeString = selectedTimes.joinToString(",")

        val medicine = Medicine(
            petId = petId,
            medicineName = medicineName,
            dosage = dosage,
            frequency = frequency,
            startDate = startDate!!,
            endDate = endDate,
            time = timeString,
            notes = notes.ifEmpty { null }
        )

        petViewModel.insertMedicine(medicine)

        // Schedule notifications
        val helper = utar.edu.my.fyp.petschedule.NotificationHelper(this)
        selectedTimes.forEach { t ->
            val parts = t.split(":")
            val cal = java.util.Calendar.getInstance().apply {
                time = startDate!!
                set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(java.util.Calendar.MINUTE, parts[1].toInt())
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (before(java.util.Calendar.getInstance())) add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            helper.scheduleMedicineAlarm(
                petId = petId,
                medicineName = medicineName,
                dosage = dosage,
                timeLabel = t,
                triggerAtMillis = cal.timeInMillis,
                isReminder = false
            )
            // +5 minutes reminder
            val rem = (cal.timeInMillis + 5 * 60 * 1000L)
            if (rem > System.currentTimeMillis()) {
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

        Toast.makeText(this, "Medicine added successfully", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}