package utar.edu.my.fyp.petschedule

import androidx.room.*
import androidx.lifecycle.LiveData
import java.util.Date

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE petId = :petId ORDER BY appointmentDate ASC")
    fun getAppointmentsByPet(petId: Long): LiveData<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE id = :appointmentId")
    fun getAppointmentById(appointmentId: Long): LiveData<Appointment?>

    @Query("SELECT * FROM appointments WHERE isCompleted = 0 AND DATE(appointmentDate/1000, 'unixepoch') >= DATE(:currentDate/1000, 'unixepoch') AND appointmentDate <= :futureDate ORDER BY appointmentDate ASC")
    suspend fun getUpcomingAppointments(currentDate: Date, futureDate: Date): List<Appointment>

    @Query("SELECT * FROM appointments WHERE title = :appointmentTitle AND petId = :petId LIMIT 1")
    suspend fun getAppointmentByTitleAndPetId(appointmentTitle: String, petId: Long): Appointment?

    @Insert
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    @Query("SELECT * FROM appointments")
    fun getAllAppointments(): LiveData<List<Appointment>>

    // ADD THIS METHOD:
    @Query("SELECT * FROM appointments ORDER BY appointmentDate ASC")
    suspend fun getAllAppointmentsSync(): List<Appointment>
}