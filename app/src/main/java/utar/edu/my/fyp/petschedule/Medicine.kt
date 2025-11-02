package utar.edu.my.fyp.petschedule

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "medicines",
    foreignKeys = [ForeignKey(
        entity = Pet::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("petId"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val petId: Long,
    val medicineName: String,
    val dosage: String,
    val frequency: String, // "daily", "twice_daily", "weekly"
    val startDate: Date,
    val endDate: Date?,
    val time: String, // "09:00,21:00" for multiple times
    val isActive: Boolean = true,
    val notes: String? = null
)