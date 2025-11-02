import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.*
import utar.edu.my.fyp.petschedule.NotificationWorker
import utar.edu.my.fyp.petschedule.PetRepository
import utar.edu.my.fyp.petschedule.database.PetDatabase
import java.util.concurrent.TimeUnit

class PetScheduleApplication : Application(), Configuration.Provider {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "pet_schedule_channel"
        const val MEDICINE_CHANNEL_ID = "medicine_channel"
        const val VACCINATION_CHANNEL_ID = "vaccination_channel"
        const val APPOINTMENT_CHANNEL_ID = "appointment_channel"
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        WorkManager.initialize(this, workManagerConfiguration)
        schedulePeriodicNotificationWork()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mainChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
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

    private fun schedulePeriodicNotificationWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(false)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("pet_schedule_notifications")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PetScheduleNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun getDatabase(): PetDatabase {
        return PetDatabase.getDatabase(this)
    }

    fun getRepository(): PetRepository {
        val database = getDatabase()
        return PetRepository(
            database.petDao(),
            database.medicineDao(),
            database.vaccinationDao(),
            database.appointmentDao()
        )
    }

    fun triggerManualNotificationCheck() {
        val oneTimeWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .addTag("manual_notification_check")
            .build()

        WorkManager.getInstance(this).enqueue(oneTimeWork)
    }

    fun cancelNotificationWork() {
        WorkManager.getInstance(this).cancelUniqueWork("PetScheduleNotificationWork")
    }

    fun areNotificationsEnabled(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }
}
