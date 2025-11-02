package utar.edu.my.fyp.petschedule.database

import androidx.lifecycle.LiveData
import androidx.room.*
import utar.edu.my.fyp.petschedule.Pet

@Dao
interface PetDao {

    @Query("SELECT * FROM pets WHERE userId = :userId ORDER BY name ASC")
    fun getAllPetsByUser(userId: String): LiveData<List<Pet>>

    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId")
    fun getPetByIdAndUser(petId: Long, userId: String): LiveData<Pet?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: Pet): Long

    @Update
    suspend fun updatePet(pet: Pet)

    @Delete
    suspend fun deletePet(pet: Pet)

    @Query("DELETE FROM pets WHERE id = :petId AND userId = :userId")
    suspend fun deletePetByIdAndUser(petId: Long, userId: String)

    @Query("SELECT * FROM pets WHERE id = :petId AND userId = :userId")
    suspend fun getPetByIdAndUserSync(petId: Long, userId: String): Pet?

    @Query("SELECT * FROM pets WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllPetsByUserSync(userId: String): List<Pet>

    // Keep the old methods for backward compatibility if needed
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPets(): LiveData<List<Pet>>

    @Query("SELECT * FROM pets WHERE id = :petId")
    fun getPetById(petId: Long): LiveData<Pet?>

    @Query("SELECT * FROM pets WHERE id = :petId")
    suspend fun getPetByIdSync(petId: Long): Pet?

    @Query("SELECT * FROM pets ORDER BY name ASC")
    suspend fun getAllPetsSync(): List<Pet>



}