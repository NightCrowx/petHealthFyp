package utar.edu.my.fyp.petschedule

import java.util.Date

data class PetEvent(
    val id: Long,
    val petId: Long,
    val petName: String,
    val petImagePath: String?,
    val eventType: EventType,
    val title: String,
    val description: String?,
    val date: Date,
    val time: String?,
    val location: String?,
    val isCompleted: Boolean = false
)

enum class EventType {
    APPOINTMENT,
    MEDICINE,
    VACCINATION
}