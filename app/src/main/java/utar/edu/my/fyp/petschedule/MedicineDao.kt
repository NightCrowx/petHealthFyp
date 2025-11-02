package utar.edu.my.fyp.petschedule

import androidx.room.*
import androidx.lifecycle.LiveData
import java.util.Date

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE petId = :petId AND isActive = 1 ORDER BY startDate ASC")
    fun getMedicinesByPet(petId: Long): LiveData<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :medicineId")
    fun getMedicineById(medicineId: Long): LiveData<Medicine?>

    @Query("SELECT * FROM medicines WHERE isActive = 1 AND startDate <= :currentDate AND (endDate IS NULL OR endDate >= :currentDate)")
    suspend fun getActiveMedicines(currentDate: Date): List<Medicine>

    @Query("SELECT * FROM medicines WHERE medicineName = :medicineName AND petId = :petId LIMIT 1")
    suspend fun getMedicineByNameAndPetId(medicineName: String, petId: Long): Medicine?

    @Insert
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("SELECT * FROM medicines")
    fun getAllMedicines(): LiveData<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :medicineId")
    suspend fun getMedicineByIdSync(medicineId: Long): Medicine?
}