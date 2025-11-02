package utar.edu.my.fyp.petschedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Date
import utar.edu.my.fyp.petschedule.*
import utar.edu.my.fyp.petschedule.adapters.UserSessionManager
import utar.edu.my.fyp.petschedule.database.PetDatabase

class PetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PetRepository

    // Current user ID
    private val _currentUserId = MutableLiveData<String?>()

    init {
        val database = PetDatabase.getDatabase(application)
        repository = PetRepository(
            database.petDao(),
            database.medicineDao(),
            database.vaccinationDao(),
            database.appointmentDao()
        )

        // Initialize current user ID
        val userId = UserSessionManager.getCurrentUserId(application)
        _currentUserId.value = userId
    }

    // USER-SPECIFIC pets - only show pets for current user
    val allPets: LiveData<List<Pet>> = _currentUserId.switchMap { userId ->
        if (userId != null) {
            repository.getPetsByUser(userId)
        } else {
            liveData { emit(emptyList()) }
        }
    }

    fun setCurrentUser(userId: String) {
        UserSessionManager.setCurrentUserId(getApplication(), userId)
        _currentUserId.value = userId
    }

    fun getCurrentUserId(): String? {
        return _currentUserId.value
    }

    // Pet operations - now include userId
    fun insertPet(pet: Pet) = viewModelScope.launch {
        val userId = getCurrentUserId()
        if (userId != null) {
            val petWithUser = pet.copy(userId = userId)
            repository.insertPet(petWithUser)
        }
    }

    fun updatePet(pet: Pet) = viewModelScope.launch {
        repository.updatePet(pet)
    }

    fun deletePet(pet: Pet) = viewModelScope.launch {
        repository.deletePet(pet)
    }

    fun getPetById(petId: Long): LiveData<Pet?> {
        val userId = getCurrentUserId()
        return if (userId != null) {
            repository.getPetByIdAndUser(petId, userId)
        } else {
            liveData { emit(null) }
        }
    }

    // Medicine operations (unchanged - filtered by petId)
    fun getMedicinesByPet(petId: Long): LiveData<List<Medicine>> = repository.getMedicinesByPet(petId)
    fun getMedicineById(medicineId: Long): LiveData<Medicine?> = repository.getMedicineById(medicineId)
    fun insertMedicine(medicine: Medicine) = viewModelScope.launch { repository.insertMedicine(medicine) }
    fun updateMedicine(medicine: Medicine) = viewModelScope.launch { repository.updateMedicine(medicine) }
    fun deleteMedicine(medicine: Medicine) = viewModelScope.launch { repository.deleteMedicine(medicine) }

    // Vaccination operations (unchanged - filtered by petId)
    fun getVaccinationsByPet(petId: Long): LiveData<List<Vaccination>> = repository.getVaccinationsByPet(petId)
    fun getVaccinationById(vaccinationId: Long): LiveData<Vaccination?> = repository.getVaccinationById(vaccinationId)
//    fun insertVaccination(vaccination: Vaccination) = viewModelScope.launch { repository.insertVaccination(vaccination) }

    fun insertVaccination(vaccination: Vaccination) = viewModelScope.launch {
        repository.insertVaccination(vaccination)
    }
    fun updateVaccination(vaccination: Vaccination) = viewModelScope.launch { repository.updateVaccination(vaccination) }
    fun deleteVaccination(vaccination: Vaccination) = viewModelScope.launch { repository.deleteVaccination(vaccination) }

    fun getUpcomingVaccinations(currentDate: Date, futureDate: Date): LiveData<List<Vaccination>> = liveData {
        emit(repository.getUpcomingVaccinations(currentDate, futureDate))
    }

    // Appointment operations (unchanged - filtered by petId)
    fun getAppointmentsByPet(petId: Long): LiveData<List<Appointment>> = repository.getAppointmentsByPet(petId)
    fun getAppointmentById(appointmentId: Long): LiveData<Appointment?> = repository.getAppointmentById(appointmentId)
    fun insertAppointment(appointment: Appointment) = viewModelScope.launch { repository.insertAppointment(appointment) }
    fun updateAppointment(appointment: Appointment) = viewModelScope.launch { repository.updateAppointment(appointment) }
    fun deleteAppointment(appointment: Appointment) = viewModelScope.launch { repository.deleteAppointment(appointment) }

    // USER-SPECIFIC upcoming events
    fun getUpcomingEvents(limitDays: Int = 30): LiveData<List<PetEvent>> {
        return _currentUserId.switchMap { userId ->
            if (userId != null) {
                repository.getUpcomingEventsForUserLiveData(userId, limitDays)
            } else {
                liveData { emit(emptyList()) }
            }
        }
    }



}