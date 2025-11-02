package utar.edu.my.fyp.petschedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import utar.edu.my.fyp.petschedule.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "pet_schedule_channel"
        const val MEDICINE_CHANNEL_ID = "medicine_channel"
        const val VACCINATION_CHANNEL_ID = "vaccination_channel"
        const val APPOINTMENT_CHANNEL_ID = "appointment_channel"
        const val MEDICINE_NOTIFICATION_ID = 1001
        const val VACCINATION_NOTIFICATION_ID = 1002
        const val APPOINTMENT_NOTIFICATION_ID = 1003

        // SharedPreferences keys for tracking notification counts
        private const val PREF_NAME = "notification_counts"
        private const val MAX_NOTIFICATIONS = 2
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        createNotificationChannels()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mainChannel = NotificationChannel(
                CHANNEL_ID,
                "Pet Schedule",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General pet schedule notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val medicineChannel = NotificationChannel(
                MEDICINE_CHANNEL_ID,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medicine dosage and timing alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val vaccinationChannel = NotificationChannel(
                VACCINATION_CHANNEL_ID,
                "Vaccination Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Vaccination due date reminders"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val appointmentChannel = NotificationChannel(
                APPOINTMENT_CHANNEL_ID,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Upcoming appointment alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(mainChannel, medicineChannel, vaccinationChannel, appointmentChannel)
            )
        }
    }

    private fun getNotificationKey(type: String, petId: Long, itemName: String, time: String = ""): String {
        val today = dateFormat.format(Date())
        return "${type}_${petId}_${itemName}_${time}_$today"
    }

    private fun canShowNotification(key: String): Boolean {
        val count = sharedPreferences.getInt(key, 0)
        return count < MAX_NOTIFICATIONS
    }

    private fun incrementNotificationCount(key: String) {
        val currentCount = sharedPreferences.getInt(key, 0)
        sharedPreferences.edit().putInt(key, currentCount + 1).apply()
    }


    fun showMedicineNotification(
        petName: String,
        medicineName: String,
        dosage: String,
        time: String = "",
        customTitle: String = "Medication Reminder",
        petId: Long = 0,
        medicineId: Long = 0
    ) {

        if (!hasNotificationPermission()) return

        val notificationKey = getNotificationKey("medicine", petId, medicineName, time)

        if (!canShowNotification(notificationKey)) {
            return // Don't show more than 2 notifications per medicine per day
        }

        val intent = Intent(context, utar.edu.my.fyp.MedicineDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("medicineId", medicineId)
            putExtra("petId", petId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            MEDICINE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Formal notification text with custom title
        val title = customTitle
        val contentText = "Please administer $medicineName ($dosage) to $petName as prescribed."
        val bigText = if (customTitle.contains("Reminder")) {
            "Dear Pet Owner,\n\nThis is a follow-up reminder to administer $medicineName with a dosage of $dosage to your pet $petName. Please ensure proper administration according to veterinary instructions if not already done."
        } else {
            "Dear Pet Owner,\n\nThis is a gentle reminder to administer $medicineName with a dosage of $dosage to your pet $petName. Please ensure proper administration according to veterinary instructions."
        }

        val notification = NotificationCompat.Builder(context, MEDICINE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                val uniqueId = MEDICINE_NOTIFICATION_ID + petName.hashCode() + medicineName.hashCode() + time.hashCode()
                notify(uniqueId, notification)
                incrementNotificationCount(notificationKey)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showVaccinationNotification(
        petName: String,
        vaccineName: String,
        customTitle: String = "Vaccination Due Notice",
        petId: Long = 0,
        vaccinationId: Long = 0
    ) {
        if (!hasNotificationPermission()) return

        val notificationKey = getNotificationKey("vaccination", petId, vaccineName)

        if (!canShowNotification(notificationKey)) {
            return // Don't show more than 2 notifications per vaccination per day
        }

        val intent = Intent(context, utar.edu.my.fyp.VaccinationDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("vaccinationId", vaccinationId)
            putExtra("petId", petId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            VACCINATION_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Formal notification text with custom title
        val title = customTitle
        val contentText = "$petName requires $vaccineName vaccination. Please schedule an appointment with your veterinarian."
        val bigText = if (customTitle.contains("Reminder")) {
            "Dear Pet Owner,\n\nThis is a follow-up reminder that $petName is due for $vaccineName vaccination. If you haven't already done so, please contact your veterinary clinic to schedule an appointment at your earliest convenience to ensure your pet's continued health and protection."
        } else {
            "Dear Pet Owner,\n\nThis is an important reminder that $petName is due for $vaccineName vaccination. Please contact your veterinary clinic to schedule an appointment at your earliest convenience to ensure your pet's continued health and protection."
        }

        val notification = NotificationCompat.Builder(context, VACCINATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                val uniqueId = VACCINATION_NOTIFICATION_ID + petName.hashCode() + vaccineName.hashCode()
                notify(uniqueId, notification)
                incrementNotificationCount(notificationKey)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showAppointmentNotification(
        petName: String,
        appointmentTitle: String,
        appointmentTime: String = "",
        customTitle: String = "Veterinary Appointment Reminder",
        petId: Long = 0,
        appointmentId: Long = 0
    ) {
        if (!hasNotificationPermission()) return

        val notificationKey = getNotificationKey("appointment", petId, appointmentTitle)

        if (!canShowNotification(notificationKey)) {
            return // Don't show more than 2 notifications per appointment per day
        }

        // Modified: Create intent for AppointmentDetailActivity instead of MainActivity
        val intent = Intent(context, utar.edu.my.fyp.AppointmentDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("appointmentId", appointmentId)
            putExtra("petId", petId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            APPOINTMENT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Formal notification text with custom title
        val title = customTitle
        val timeText = if (appointmentTime.isNotEmpty()) " scheduled for $appointmentTime" else ""
        val contentText = "Reminder: $appointmentTitle for $petName$timeText. Please ensure timely attendance."
        val bigText = if (customTitle.contains("Alert")) {
            "Dear Pet Owner,\n\nThis is an urgent reminder about the upcoming appointment: $appointmentTitle for your pet $petName$timeText. Please prepare for departure."
        } else {
            "Dear Pet Owner,\n\nThis is a follow-up reminder about the upcoming  appointment: $appointmentTitle for your pet $petName$timeText. Please arrive promptly."
        }

        val notification = NotificationCompat.Builder(context, APPOINTMENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                val uniqueId = APPOINTMENT_NOTIFICATION_ID + petName.hashCode() + appointmentTitle.hashCode()
                notify(uniqueId, notification)
                incrementNotificationCount(notificationKey)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Clear notification counts for a new day (call this daily)
     */
    fun clearDailyNotificationCounts() {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val yesterdayString = dateFormat.format(yesterday.time)

        // Remove old notification counts
        val editor = sharedPreferences.edit()
        val allEntries = sharedPreferences.all
        for ((key, _) in allEntries) {
            if (key.endsWith(yesterdayString)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    /**
     * Get remaining notification count for debugging
     */
    fun getRemainingNotificationCount(type: String, petId: Long, itemName: String, time: String = ""): Int {
        val key = getNotificationKey(type, petId, itemName, time)
        val currentCount = sharedPreferences.getInt(key, 0)
        return MAX_NOTIFICATIONS - currentCount
    }

    //<--------------------------------------------------------------------->//

    // === Add below your existing functions in NotificationHelper ===
    private fun alarmManager(): android.app.AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

    private fun buildBroadcastPendingIntent(
        type: String,
        requestCode: Int,
        extras: android.os.Bundle
    ): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "utar.edu.my.fyp.petschedule.ACTION_ALARM"
            putExtra("type", type)
            putExtras(extras)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarmWithBestMethod(triggerAtMillis: Long, pi: PendingIntent) {
        val am = alarmManager()
        val safe = kotlin.math.max(triggerAtMillis, System.currentTimeMillis() + 1500L)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, safe, pi)
            } else {
                val show = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, utar.edu.my.fyp.petschedule.ui.MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val info = android.app.AlarmManager.AlarmClockInfo(safe, show)
                am.setAlarmClock(info, pi)
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, safe, pi)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            am.setExact(android.app.AlarmManager.RTC_WAKEUP, safe, pi)
        } else {
            am.set(android.app.AlarmManager.RTC_WAKEUP, safe, pi)
        }
    }

    // ----- Appointment -----
    fun scheduleAppointmentAlarm(
        petId: Long,
        appointmentTitle: String,
        timeLabel: String,
        triggerAtMillis: Long,
        isReminder: Boolean = false
    ) {
        val b = android.os.Bundle().apply {
            putString("appointmentTitle", appointmentTitle)
            putString("appointmentTime", timeLabel)
            putLong("petId", petId)
            putBoolean("isReminder", isReminder)
        }
        val req = (APPOINTMENT_NOTIFICATION_ID + petId.toInt() + appointmentTitle.hashCode() + if (isReminder) 5 else 0)
        scheduleAlarmWithBestMethod(triggerAtMillis, buildBroadcastPendingIntent("appointment", req, b))
    }

    fun cancelAppointmentAlarms(petId: Long, appointmentTitle: String) {
        val am = alarmManager()
        val base = APPOINTMENT_NOTIFICATION_ID + petId.toInt() + appointmentTitle.hashCode()
        am.cancel(buildBroadcastPendingIntent("appointment", base, android.os.Bundle()))
        am.cancel(buildBroadcastPendingIntent("appointment", base + 5, android.os.Bundle()))
    }

    // ----- Medicine -----
    fun scheduleMedicineAlarm(
        petId: Long,
        medicineName: String,
        dosage: String,
        timeLabel: String,
        triggerAtMillis: Long,
        isReminder: Boolean = false
    ) {
        val b = android.os.Bundle().apply {
            putString("medicineName", medicineName)
            putString("dosage", dosage)
            putString("time", timeLabel)
            putLong("petId", petId)
            putBoolean("isReminder", isReminder)
        }
        val req = (MEDICINE_NOTIFICATION_ID + petId.toInt() + medicineName.hashCode() + timeLabel.hashCode() + if (isReminder) 5 else 0)
        scheduleAlarmWithBestMethod(triggerAtMillis, buildBroadcastPendingIntent("medicine", req, b))
    }

    fun cancelMedicineAlarms(petId: Long, medicineName: String, timeLabels: List<String>) {
        val am = alarmManager()
        timeLabels.forEach { t ->
            val base = MEDICINE_NOTIFICATION_ID + petId.toInt() + medicineName.hashCode() + t.hashCode()
            am.cancel(buildBroadcastPendingIntent("medicine", base, android.os.Bundle()))
            am.cancel(buildBroadcastPendingIntent("medicine", base + 5, android.os.Bundle()))
        }
    }

    // ----- Vaccination -----
    fun scheduleVaccinationAlarm(
        petId: Long,
        vaccineName: String,
        triggerAtMillis: Long,
        isReminder: Boolean = false
    ) {
        val b = android.os.Bundle().apply {
            putString("vaccineName", vaccineName)
            putLong("petId", petId)
            putBoolean("isReminder", isReminder)
        }
        // Make request code unique per fire-time so multiple events (7-days-before, due, next-due) coexist
        val unique = (triggerAtMillis xor (triggerAtMillis ushr 32)).toInt()
        val req = VACCINATION_NOTIFICATION_ID + petId.toInt() + vaccineName.hashCode() + unique + if (isReminder) 5 else 0
        scheduleAlarmWithBestMethod(triggerAtMillis, buildBroadcastPendingIntent("vaccination", req, b))
    }

    fun cancelVaccinationAlarmsForTimes(petId: Long, vaccineName: String, triggerMillisList: List<Long>) {
        val am = alarmManager()
        triggerMillisList.forEach { tm ->
            val unique = (tm xor (tm ushr 32)).toInt()
            val base = VACCINATION_NOTIFICATION_ID + petId.toInt() + vaccineName.hashCode() + unique
            am.cancel(buildBroadcastPendingIntent("vaccination", base, android.os.Bundle()))
            am.cancel(buildBroadcastPendingIntent("vaccination", base + 5, android.os.Bundle()))
        }
    }



}