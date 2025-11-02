package utar.edu.my.fyp.petschedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import utar.edu.my.fyp.petschedule.database.PetDatabase

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val helper = NotificationHelper(context)
        val petId = intent.getLongExtra("petId", 0L)

        // Run on a background thread; finish() when done
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = utar.edu.my.fyp.petschedule.database.PetDatabase.getDatabase(context)
                val repo = PetRepository(
                    db.petDao(),
                    db.medicineDao(),
                    db.vaccinationDao(),
                    db.appointmentDao()
                )
                val pet = repo.getPetByIdSync(petId)
                val petName = pet?.name ?: "Your pet"

                when (type) {
                    "medicine" -> {
                        val medicineName = intent.getStringExtra("medicineName") ?: return@launch
                        val dosage = intent.getStringExtra("dosage") ?: ""
                        val time = intent.getStringExtra("time") ?: ""
                        val isReminder = intent.getBooleanExtra("isReminder", false)
                        val title = if (isReminder) "Medication Reminder" else "Medication Time"

                        val medicine = repo.getMedicineByNameAndPetId(medicineName, petId)
                        val medicineId = medicine?.id ?: 0L
                        helper.showMedicineNotification(
                            petName,
                            medicineName,
                            dosage,
                            time,
                            title,
                            petId,
                            medicineId
                        )
                    }

                    "vaccination" -> {
                        val vaccineName = intent.getStringExtra("vaccineName") ?: return@launch
                        val isReminder = intent.getBooleanExtra("isReminder", false)
                        val title =
                            if (isReminder) "Vaccination Reminder" else "Vaccination Due Notice"

                        val vaccination = repo.getVaccinationByNameAndPetId(vaccineName, petId)
                        val vaccinationId = vaccination?.id ?: 0L
                        helper.showVaccinationNotification(
                            petName,
                            vaccineName,
                            title,
                            petId,
                            vaccinationId
                        )
                    }

                    "appointment" -> {
                        val appointmentTitle =
                            intent.getStringExtra("appointmentTitle") ?: return@launch
                        val appointmentTime = intent.getStringExtra("appointmentTime") ?: ""
                        val isReminder = intent.getBooleanExtra("isReminder", false)
                        val title = if (isReminder) "Appointment Reminder" else "Appointment Alert"

                        val appointment =
                            repo.getAppointmentByTitleAndPetId(appointmentTitle, petId)
                        val appointmentId = appointment?.id ?: 0L
                        helper.showAppointmentNotification(
                            petName,
                            appointmentTitle,
                            appointmentTime,
                            title,
                            petId,
                            appointmentId
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }
}