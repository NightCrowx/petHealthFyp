package utar.edu.my.fyp.petschedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import java.util.*
import utar.edu.my.fyp.petschedule.database.PetDao

class PetRepository(
    private val petDao: PetDao,
    private val medicineDao: MedicineDao,
    private val vaccinationDao: VaccinationDao,
    private val appointmentDao: AppointmentDao
) {
    // USER-SPECIFIC Pet operations
    fun getPetsByUser(userId: String): LiveData<List<Pet>> = petDao.getAllPetsByUser(userId)

    fun getPetByIdAndUser(petId: Long, userId: String): LiveData<Pet?> = petDao.getPetByIdAndUser(petId, userId)

    suspend fun getPetByIdAndUserSync(petId: Long, userId: String): Pet? = petDao.getPetByIdAndUserSync(petId, userId)

    suspend fun insertPet(pet: Pet): Long = petDao.insertPet(pet)

    suspend fun updatePet(pet: Pet) = petDao.updatePet(pet)

    suspend fun deletePet(pet: Pet) = petDao.deletePet(pet)

    suspend fun getPetsByUserSync(userId: String): List<Pet> = petDao.getAllPetsByUserSync(userId)

    // Keep original methods for backward compatibility (but they will return all pets)
    fun getAllPets(): LiveData<List<Pet>> = petDao.getAllPets()
    fun getPetById(petId: Long): LiveData<Pet?> = petDao.getPetById(petId)
    suspend fun getPetByIdSync(petId: Long): Pet? = petDao.getPetByIdSync(petId)
    private suspend fun getAllPetsSync(): List<Pet> = petDao.getAllPetsSync()

    // Medicine operations (already filtered by petId, so no direct user filtering needed)
    fun getMedicinesByPet(petId: Long): LiveData<List<Medicine>> = medicineDao.getMedicinesByPet(petId)
    fun getMedicineById(medicineId: Long): LiveData<Medicine?> = medicineDao.getMedicineById(medicineId)
    suspend fun insertMedicine(medicine: Medicine): Long = medicineDao.insertMedicine(medicine)
    suspend fun updateMedicine(medicine: Medicine) = medicineDao.updateMedicine(medicine)
    suspend fun deleteMedicine(medicine: Medicine) = medicineDao.deleteMedicine(medicine)
    suspend fun getActiveMedicines(currentDate: Date): List<Medicine> = medicineDao.getActiveMedicines(currentDate)

    // Add the missing method for getting medicine by name and petId
    suspend fun getMedicineByNameAndPetId(medicineName: String, petId: Long): Medicine? =
        medicineDao.getMedicineByNameAndPetId(medicineName, petId)

    // Vaccination operations (already filtered by petId)
    fun getVaccinationsByPet(petId: Long): LiveData<List<Vaccination>> = vaccinationDao.getVaccinationsByPet(petId)
    fun getVaccinationById(vaccinationId: Long): LiveData<Vaccination?> = vaccinationDao.getVaccinationById(vaccinationId)

    // FIXED: Return the generated ID from insertion
    suspend fun insertVaccination(vaccination: Vaccination): Long = vaccinationDao.insertVaccination(vaccination)

    suspend fun updateVaccination(vaccination: Vaccination) = vaccinationDao.updateVaccination(vaccination)
    suspend fun deleteVaccination(vaccination: Vaccination) = vaccinationDao.deleteVaccination(vaccination)
    suspend fun getUpcomingVaccinations(futureDate: Date): List<Vaccination> {
        val currentDate = Date()
        return vaccinationDao.getUpcomingVaccinations(currentDate, futureDate)
    }
    suspend fun getUpcomingVaccinations(currentDate: Date, futureDate: Date): List<Vaccination> =
        vaccinationDao.getUpcomingVaccinations(currentDate, futureDate)

    // Add method to get vaccination by name and petId
    suspend fun getVaccinationByNameAndPetId(vaccineName: String, petId: Long): Vaccination? {
        return vaccinationDao.getVaccinationByNameAndPetId(vaccineName, petId)
    }

    // Appointment operations (already filtered by petId)
    fun getAppointmentsByPet(petId: Long): LiveData<List<Appointment>> = appointmentDao.getAppointmentsByPet(petId)
    fun getAppointmentById(appointmentId: Long): LiveData<Appointment?> = appointmentDao.getAppointmentById(appointmentId)
    suspend fun insertAppointment(appointment: Appointment): Long = appointmentDao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: Appointment) = appointmentDao.updateAppointment(appointment)
    suspend fun deleteAppointment(appointment: Appointment) = appointmentDao.deleteAppointment(appointment)
    suspend fun getUpcomingAppointments(currentDate: Date): List<Appointment> {
        val futureDate = Calendar.getInstance().apply {
            time = currentDate
            add(Calendar.DAY_OF_MONTH, 30) // Default 30 days ahead
        }.time
        return appointmentDao.getUpcomingAppointments(currentDate, futureDate)
    }
    suspend fun getUpcomingAppointments(currentDate: Date, futureDate: Date): List<Appointment> =
        appointmentDao.getUpcomingAppointments(currentDate, futureDate)

    // Add method to get appointment by title and petId
    suspend fun getAppointmentByTitleAndPetId(appointmentTitle: String, petId: Long): Appointment? {
        return appointmentDao.getAppointmentByTitleAndPetId(appointmentTitle, petId)
    }

    // USER-SPECIFIC upcoming events with filtering for completed items
    suspend fun getUpcomingEventsForUser(userId: String, limitDays: Int = 30): List<PetEvent> {
        // Use start of today instead of current time
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val currentDate = Date()

        val futureDate = Calendar.getInstance().apply {
            time = startOfToday
            add(Calendar.DAY_OF_MONTH, limitDays)
        }.time

        val allEvents = mutableListOf<PetEvent>()

        try {
            println("DEBUG: Using date range: $startOfToday to $futureDate")

            // Get USER'S pets only
            val userPets = getPetsByUserSync(userId)
            val petsMap = userPets.associateBy { it.id }

            if (userPets.isEmpty()) {
                return emptyList()
            }

            val userPetIds = userPets.map { it.id }

            // Get appointments for user's pets only - FILTER OUT COMPLETED APPOINTMENTS
            try {
                val allAppointments = appointmentDao.getUpcomingAppointments(startOfToday, futureDate)
                println("DEBUG: Total appointments from DAO with new date range: ${allAppointments.size}")

                // Filter for user's pets AND exclude completed appointments
                val appointments = allAppointments.filter {
                    it.petId in userPetIds && !it.isCompleted
                }
                println("DEBUG: Filtered active appointments for user's pets: ${appointments.size}")

                appointments.forEach { appointment ->
                    println("DEBUG: Processing appointment - ID: ${appointment.id}, Date: ${appointment.appointmentDate}")
                    val pet = petsMap[appointment.petId]
                    if (pet != null) {
                        allEvents.add(
                            PetEvent(
                                id = appointment.id,
                                petId = pet.id,
                                petName = pet.name,
                                petImagePath = pet.imagePath,
                                eventType = EventType.APPOINTMENT,
                                title = appointment.title,
                                description = appointment.description,
                                date = appointment.appointmentDate,
                                time = appointment.time,
                                location = appointment.location,
                                isCompleted = appointment.isCompleted
                            )
                        )
                        println("DEBUG: Added appointment event: ${appointment.title}")
                    }
                }
            } catch (e: Exception) {
                println("ERROR: Failed to get appointments: ${e.message}")
                e.printStackTrace()
            }

            // Get vaccinations for user's pets only - FILTER OUT COMPLETED VACCINATIONS
            try {
                val allVaccinations = vaccinationDao.getUpcomingVaccinations(startOfToday, futureDate)
                println("DEBUG: Total vaccinations from DAO with new date range: ${allVaccinations.size}")

                // Filter for user's pets AND exclude completed vaccinations
                val vaccinations = allVaccinations.filter {
                    it.petId in userPetIds && !it.isCompleted
                }
                println("DEBUG: Filtered active vaccinations for user's pets: ${vaccinations.size}")

                vaccinations.forEach { vaccination ->
                    println("DEBUG: Processing vaccination - ID: ${vaccination.id}, Date: ${vaccination.dueDate}")
                    val pet = petsMap[vaccination.petId]
                    if (pet != null) {
                        allEvents.add(
                            PetEvent(
                                id = vaccination.id,
                                petId = pet.id,
                                petName = pet.name,
                                petImagePath = pet.imagePath,
                                eventType = EventType.VACCINATION,
                                title = vaccination.vaccineName,
                                description = vaccination.notes,
                                date = vaccination.dueDate,
                                time = null,
                                location = null,
                                isCompleted = vaccination.isCompleted
                            )
                        )
                        println("DEBUG: Added vaccination event: ${vaccination.vaccineName}")
                    }
                }
            } catch (e: Exception) {
                println("ERROR: Failed to get vaccinations: ${e.message}")
                e.printStackTrace()
            }

            // Get medicines for user's pets only - FILTER OUT INACTIVE MEDICINES
            val medicines = medicineDao.getActiveMedicines(currentDate)
                .filter { it.petId in userPetIds && it.isActive }

            println("DEBUG: Found ${medicines.size} active medicines for user")

            medicines.forEach { medicine ->
                val pet = petsMap[medicine.petId]
                if (pet != null) {
                    val times = medicine.time.split(",").map { it.trim() }
                    times.forEachIndexed { index, time ->
                        if (time.isNotBlank()) {
                            allEvents.add(
                                PetEvent(
                                    id = medicine.id * 1000 + index,
                                    petId = pet.id,
                                    petName = pet.name,
                                    petImagePath = pet.imagePath,
                                    eventType = EventType.MEDICINE,
                                    title = "Medicine: ${medicine.medicineName}",
                                    description = "${medicine.dosage}${if (medicine.notes != null) " - ${medicine.notes}" else ""}",
                                    date = currentDate,
                                    time = time,
                                    location = null,
                                    isCompleted = false
                                )
                            )
                        }
                    }
                }
            }

            println("DEBUG: Total events created for user: ${allEvents.size}")

        } catch (e: Exception) {
            println("ERROR in getUpcomingEventsForUser: ${e.message}")
            e.printStackTrace()
        }

        return allEvents.sortedWith(compareBy<PetEvent> { it.date }.thenBy { it.time ?: "" })
    }

    fun getUpcomingEventsForUserLiveData(userId: String, limitDays: Int = 30): LiveData<List<PetEvent>> {
        return MediatorLiveData<List<PetEvent>>().apply {
            val userPetsLiveData = getPetsByUser(userId)

            addSource(userPetsLiveData) { pets ->
                value = try {
                    kotlinx.coroutines.runBlocking {
                        getUpcomingEventsForUser(userId, limitDays)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }
    }

    // Keep the original methods for backward compatibility
    suspend fun getUpcomingEvents(limitDays: Int = 30): List<PetEvent> {
        return getUpcomingEventsForUser("", limitDays) // This will return empty since no user specified
    }

    fun getUpcomingEventsLiveData(limitDays: Int = 30): LiveData<List<PetEvent>> {
        return MediatorLiveData<List<PetEvent>>().apply {
            value = emptyList() // Return empty for non-user-specific calls
        }
    }
}