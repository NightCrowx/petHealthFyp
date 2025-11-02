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

class PetProfileAdapter(private val onItemClick: (Pet) -> Unit) :
    ListAdapter<Pet, PetProfileAdapter.PetProfileViewHolder>(PetProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetProfileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_profile, parent, false)
        return PetProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetProfileViewHolder, position: Int) {
        val pet = getItem(position)
        holder.bind(pet)
        holder.itemView.setOnClickListener { onItemClick(pet) }
    }

    class PetProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val petImageView: ImageView = itemView.findViewById(R.id.petProfileImage)
        private val nameTextView: TextView = itemView.findViewById(R.id.petInitial)
        fun bind(pet: Pet) {
            nameTextView.text = pet.name
            loadPetImage(pet.imagePath)
        }

        private fun loadPetImage(imagePath: String?) {
            if (imagePath != null && imagePath.isNotEmpty()) {
                val file = File(imagePath)
                if (file.exists()) {
                    try {
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
                    petImageView.setImageResource(R.drawable.ic_pet_default)
                }
            } else {
                petImageView.setImageResource(R.drawable.ic_pet_default)
            }
        }
    }

    class PetProfileDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem == newItem
        }
    }
}