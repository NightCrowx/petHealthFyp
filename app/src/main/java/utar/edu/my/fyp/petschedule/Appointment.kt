package utar.edu.my.fyp.petschedule

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "appointments",
    foreignKeys = [ForeignKey(
        entity = Pet::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("petId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val petId: Long,
    val title: String,
    val description: String? = null,
    val appointmentDate: Date,
    val time: String,
    val location: String? = null,
    val isCompleted: Boolean = false,
    val reminderTime: Int = 60
)


