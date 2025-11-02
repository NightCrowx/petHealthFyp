package utar.edu.my.fyp.petschedule

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "vaccinations",
    foreignKeys = [ForeignKey(
        entity = Pet::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("petId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Vaccination(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val petId: Long,
    val vaccineName: String,
    val dueDate: Date,
    val isCompleted: Boolean = false,
    val completedDate: Date? = null,
    val nextDueDate: Date? = null,
    val notes: String? = null
)