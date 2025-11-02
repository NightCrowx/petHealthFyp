package utar.edu.my.fyp.petschedule

import androidx.room.*
import androidx.lifecycle.LiveData
import java.util.Date

@Dao
interface VaccinationDao {
    @Query("SELECT * FROM vaccinations WHERE petId = :petId ORDER BY dueDate ASC")
    fun getVaccinationsByPet(petId: Long): LiveData<List<Vaccination>>

    @Query("SELECT * FROM vaccinations WHERE id = :vaccinationId")
    fun getVaccinationById(vaccinationId: Long): LiveData<Vaccination?>

    @Query("SELECT * FROM vaccinations WHERE isCompleted = 0 AND DATE(dueDate/1000, 'unixepoch') >= DATE(:currentDate/1000, 'unixepoch') AND dueDate <= :futureDate ORDER BY dueDate ASC")
    suspend fun getUpcomingVaccinations(currentDate: Date, futureDate: Date): List<Vaccination>

    @Query("SELECT * FROM vaccinations WHERE vaccineName = :vaccineName AND petId = :petId LIMIT 1")
    suspend fun getVaccinationByNameAndPetId(vaccineName: String, petId: Long): Vaccination?

    @Insert
    suspend fun insertVaccination(vaccination: Vaccination): Long

    @Update
    suspend fun updateVaccination(vaccination: Vaccination)

    @Delete
    suspend fun deleteVaccination(vaccination: Vaccination)

    @Query("SELECT * FROM vaccinations")
    fun getAllVaccinations(): LiveData<List<Vaccination>>

    @Query("SELECT * FROM vaccinations ORDER BY dueDate ASC")
    suspend fun getAllVaccinationsSync(): List<Vaccination>
}