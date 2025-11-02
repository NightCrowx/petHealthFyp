package utar.edu.my.fyp.petschedule.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Pet
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PetAdapter(private val onItemClick: (Pet) -> Unit) : ListAdapter<Pet, PetAdapter.PetViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = getItem(position)
        holder.bind(pet)
        holder.itemView.setOnClickListener { onItemClick(pet) }
    }

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val petImageView: ImageView = itemView.findViewById(R.id.imageViewPet)
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewPetName)
        private val breedTextView: TextView = itemView.findViewById(R.id.textViewPetBreed)
        private val ageTextView: TextView = itemView.findViewById(R.id.textViewPetAge)
        private val petTypeTextView: TextView = itemView.findViewById(R.id.textViewPetType)

        fun bind(pet: Pet) {
            nameTextView.text = pet.name
            breedTextView.text = pet.breed

            // Calculate and display age in year month format
            val ageString = calculateAge(pet.dateOfBirth)
            ageTextView.text = ageString

            // Load pet image
            loadPetImage(pet.imagePath)

            //Load pet type
            petTypeTextView.text = pet.petType
        }

        private fun calculateAge(dateOfBirth: String): String {
            return try {
                val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                val birthDate = dateFormat.parse(dateOfBirth)

                if (birthDate != null) {
                    val birthCalendar = Calendar.getInstance().apply { time = birthDate }
                    val currentCalendar = Calendar.getInstance()

                    var years = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
                    var months = currentCalendar.get(Calendar.MONTH) - birthCalendar.get(Calendar.MONTH)

                    // If current month is before birth month, subtract a year and add 12 months
                    if (months < 0) {
                        years--
                        months += 12
                    }

                    // If current day is before birth day, subtract a month
                    if (currentCalendar.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH)) {
                        months--
                        if (months < 0) {
                            years--
                            months += 12
                        }
                    }

                    when {
                        years == 0 && months == 0 -> "0 month old"
                        years == 0 -> if (months == 1) "1 month old" else "$months months old"
                        months == 0 -> if (years == 1) "1 year old" else "$years years old"
                        else -> {
                            val yearText = if (years == 1) "1 year" else "$years years"
                            val monthText = if (months == 1) "1 month" else "$months months"
                            "$yearText $monthText old"
                        }
                    }
                } else {
                    "Unknown age"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "Unknown age"
            }
        }

        private fun loadPetImage(imagePath: String?) {
            if (imagePath != null && imagePath.isNotEmpty()) {
                val file = File(imagePath)
                if (file.exists()) {
                    try {
                        // Decode image with options to prevent memory issues
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 2 // Reduce image size by half
                            inJustDecodeBounds = false
                        }
                        val bitmap = BitmapFactory.decodeFile(imagePath, options)
                        if (bitmap != null) {
                            petImageView.setImageBitmap(bitmap)
                        } else {
                            petImageView.setImageResource(R.drawable.ic_pet_default)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        petImageView.setImageResource(R.drawable.ic_pet_default)
                    }
                } else {
                    // File doesn't exist
                    petImageView.setImageResource(R.drawable.ic_pet_default)
                }
            } else {
                // No image path provided
                petImageView.setImageResource(R.drawable.ic_pet_default)
            }
        }
    }

    class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem == newItem
        }
    }
}