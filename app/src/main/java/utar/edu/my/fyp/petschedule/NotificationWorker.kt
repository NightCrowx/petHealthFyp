package utar.edu.my.fyp.petschedule

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import utar.edu.my.fyp.petschedule.database.PetDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = PetDatabase.getDatabase(applicationContext)
        val repository = PetRepository(
            database.petDao(),
            database.medicineDao(),
            database.vaccinationDao(),
            database.appointmentDao()
        )
        val notificationHelper = NotificationHelper(applicationContext)

        try {
            // Clear old notification counts daily
            notificationHelper.clearDailyNotificationCounts()

            checkMedicineReminders(repository, notificationHelper)
            checkVaccinationReminders(repository, notificationHelper)
            checkAppointmentReminders(repository, notificationHelper)
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private suspend fun checkMedicineReminders(repository: PetRepository, notificationHelper: NotificationHelper) {
        val currentDate = Date()
        val activeMedicines = repository.getActiveMedicines(currentDate)

        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentTimeString = String.format("%02d:%02d", currentHour, currentMinute)

        for (medicine in activeMedicines) {
            val times = medicine.time.split(",").map { it.trim() }

            for (time in times) {
                val notificationType = shouldNotifyForTime(time, currentTimeString, medicine.frequency, currentDate, medicine.startDate)

                if (notificationType != NotificationType.NONE) {
                    // Get pet name
                    val pet = repository.getPetByIdSync(medicine.petId)
                    pet?.let {
                        // Check remaining notification count before sending
                        val remainingCount = notificationHelper.getRemainingNotificationCount(
                            "medicine",
                            it.id,
                            medicine.medicineName,
                            time
                        )

                        if (remainingCount > 0) {
                            val notificationTitle = when (notificationType) {
                                NotificationType.IMMEDIATE -> "Medication Time"
                                NotificationType.REMINDER -> "Medication Reminder"
                                else -> "Medication Reminder"
                            }

                            notificationHelper.showMedicineNotification(
                                petName = it.name,
                                medicineName = medicine.medicineName,
                                dosage = medicine.dosage,
                                time = time,
                                customTitle = notificationTitle,
                                petId = it.id,
                                medicineId = medicine.id
                            )
                        }
                    }
                }
            }
        }
    }

    private enum class NotificationType {
        NONE, IMMEDIATE, REMINDER
    }

    private fun shouldNotifyForTime(
        medicineTime: String,
        currentTime: String,
        frequency: String,
        currentDate: Date,
        startDate: Date
    ): NotificationType {
        val timeMatch = getTimeMatchType(medicineTime, currentTime)

        if (timeMatch == NotificationType.NONE) {
            return NotificationType.NONE
        }

        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate

        val shouldNotifyToday = when (frequency) {
            "daily", "twice_daily", "three_times_daily" -> {
                // For daily frequencies, notify every day
                true
            }
            "weekly" -> {
                // For weekly, check if it's the same day of week as start date
                val daysDifference = ((currentDate.time - startDate.time) / (24 * 60 * 60 * 1000)).toInt()
                daysDifference % 7 == 0
            }
            else -> true
        }

        return if (shouldNotifyToday) timeMatch else NotificationType.NONE
    }

    private fun getTimeMatchType(medicineTime: String, currentTime: String): NotificationType {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val medicineDate = timeFormat.parse(medicineTime)
            val currentDate = timeFormat.parse(currentTime)

            if (medicineDate != null && currentDate != null) {
                val differenceInMinutes = (currentDate.time - medicineDate.time) / (60 * 1000)

                return when {
                    // Exact time match (within 1 minute)
                    Math.abs(differenceInMinutes) <= 1 -> NotificationType.IMMEDIATE
                    // 5 minutes after scheduled time
                    differenceInMinutes in 4..6 -> NotificationType.REMINDER
                    else -> NotificationType.NONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return NotificationType.NONE
    }

    private suspend fun checkVaccinationReminders(repository: PetRepository, notificationHelper: NotificationHelper) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 7) // Check for vaccinations due in next 7 days
        val upcomingVaccinations = repository.getUpcomingVaccinations(calendar.time)

        // Get current time for checking if we should send immediate or reminder notification
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        for (vaccination in upcomingVaccinations) {
            val pet = repository.getPetByIdSync(vaccination.petId)
            pet?.let {
                val remainingCount = notificationHelper.getRemainingNotificationCount(
                    "vaccination",
                    it.id,
                    vaccination.vaccineName
                )

                if (remainingCount > 0) {
                    // Check if this is the reminder notification (5 minutes after initial notification time)
                    // Assuming vaccination notifications are sent at 9:00 AM daily for due vaccinations
                    val shouldSendReminder = (currentHour == 9 && currentMinute in 4..6)
                    val customTitle = if (shouldSendReminder) "Vaccination Reminder" else "Vaccination Due Notice"

                    // Send notification if it's 9:00 AM (immediate) or 9:05 AM (reminder)
                    if ((currentHour == 9 && currentMinute <= 1) || shouldSendReminder) {
                        notificationHelper.showVaccinationNotification(
                            petName = it.name,
                            vaccineName = vaccination.vaccineName,
                            customTitle = customTitle,
                            petId = it.id,
                            vaccinationId = vaccination.id
                        )
                    }
                }
            }
        }
    }

    private suspend fun checkAppointmentReminders(repository: PetRepository, notificationHelper: NotificationHelper) {
        val currentDate = Date()
        val upcomingAppointments = repository.getUpcomingAppointments(currentDate)

        for (appointment in upcomingAppointments) {
            val pet = repository.getPetByIdSync(appointment.petId)
            pet?.let {
                // Check if appointment should trigger notification based on reminder time
                val notificationType = shouldNotifyForAppointment(appointment, currentDate)

                if (notificationType != NotificationType.NONE) {
                    val remainingCount = notificationHelper.getRemainingNotificationCount(
                        "appointment",
                        it.id,
                        appointment.title
                    )

                    if (remainingCount > 0) {
                        // Format appointment date and time
                        val appointmentTimeString = formatAppointmentDateTime(appointment)

                        val customTitle = when (notificationType) {
                            NotificationType.IMMEDIATE -> "Appointment Alert"
                            NotificationType.REMINDER -> "Appointment Reminder"
                            else -> "Appointment Reminder"
                        }

                        notificationHelper.showAppointmentNotification(
                            petName = it.name,
                            appointmentTitle = appointment.title,
                            appointmentTime = appointmentTimeString,
                            customTitle = customTitle,
                            petId = it.id,
                            appointmentId = appointment.id
                        )
                    }
                }
            }
        }
    }

    private fun shouldNotifyForAppointment(appointment: Appointment, currentDate: Date): NotificationType {
        try {
            // Combine appointment date and time
            val appointmentDateTime = combineDateTime(appointment.appointmentDate, appointment.time)

            // Calculate time difference in minutes
            val timeDifferenceMinutes = (appointmentDateTime.time - currentDate.time) / (60 * 1000)

            // Check if we should notify based on reminder time
            val reminderMinutes = appointment.reminderTime.toLong()

            return when {
                // Exact reminder time (±1 minute tolerance)
                timeDifferenceMinutes in (reminderMinutes - 1)..(reminderMinutes + 1) -> NotificationType.IMMEDIATE
                // 5 minutes after the initial reminder time
                timeDifferenceMinutes in (reminderMinutes - 6)..(reminderMinutes - 4) -> NotificationType.REMINDER
                else -> NotificationType.NONE
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return NotificationType.NONE
        }
    }

    private fun combineDateTime(date: Date, timeString: String): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date

        try {
            val timeParts = timeString.split(":")
            if (timeParts.size == 2) {
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If time parsing fails, use the original date
        }

        return calendar.time
    }

    private fun formatAppointmentDateTime(appointment: Appointment): String {
        return try {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(appointment.appointmentDate)
            "$formattedDate at ${appointment.time}"
        } catch (e: Exception) {
            e.printStackTrace()
            appointment.time
        }
    }
}