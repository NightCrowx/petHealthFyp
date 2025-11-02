//package utar.edu.my.fyp.petschedule
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "pets")
//data class Pet(
//    @PrimaryKey(autoGenerate = true)
//    val id: Long = 0,
//    val name: String,
//    val breed: String,
//    val age: Int,
//    val gender: String,
//    val dateOfBirth: String,
//    val petType: String,
//    val imagePath: String? = null,
//    val userId: String
//)


package utar.edu.my.fyp.petschedule

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val breed: String,
    val gender: String,
    val dateOfBirth: String,
    val petType: String,
    val imagePath: String? = null,
    val userId: String
) {
    // Computed property - not stored in database
    @get:Ignore
    val age: Int
        get() = calculateCurrentAge()

    // Helper function to calculate current age dynamically
    @Ignore
    private fun calculateCurrentAge(): Int {
        return try {
            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            val birthDate = dateFormat.parse(dateOfBirth)

            if (birthDate != null) {
                val birthCalendar = Calendar.getInstance().apply { time = birthDate }
                val currentCalendar = Calendar.getInstance()

                var age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

                // Adjust if birthday hasn't occurred this year
                if (currentCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }

                return maxOf(0, age) // Ensure age is never negative
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}