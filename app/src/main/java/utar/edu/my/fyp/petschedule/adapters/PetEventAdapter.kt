package utar.edu.my.fyp.petschedule.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.EventType
import utar.edu.my.fyp.petschedule.PetEvent
import java.text.SimpleDateFormat
import java.util.*

class PetEventAdapter(
    private val onEventClick: (PetEvent) -> Unit
) : ListAdapter<PetEvent, PetEventAdapter.PetEventViewHolder>(PetEventDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetEventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_event, parent, false)
        return PetEventViewHolder(view, onEventClick)
    }

    override fun onBindViewHolder(holder: PetEventViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat)
    }

    class PetEventViewHolder(
        itemView: View,
        private val onEventClick: (PetEvent) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val petImageView: ImageView = itemView.findViewById(R.id.imageViewPet)
        private val petNameTextView: TextView = itemView.findViewById(R.id.textViewPetName)
        private val eventTypeTextView: TextView = itemView.findViewById(R.id.textViewEventType)
        private val eventTitleTextView: TextView = itemView.findViewById(R.id.textViewEventTitle)
        private val eventDateTextView: TextView = itemView.findViewById(R.id.textViewEventDate)
        private val eventTimeTextView: TextView = itemView.findViewById(R.id.textViewEventTime)

        fun bind(petEvent: PetEvent, dateFormat: SimpleDateFormat) {
            petNameTextView.text = petEvent.petName
            eventTitleTextView.text = petEvent.title
            eventDateTextView.text = dateFormat.format(petEvent.date)

            // Set event type with appropriate color
            when (petEvent.eventType) {
                EventType.APPOINTMENT -> {
                    eventTypeTextView.text = "Appointment"
                    eventTypeTextView.setBackgroundResource(R.drawable.bg_appointment_tag)
                }
                EventType.MEDICINE -> {
                    eventTypeTextView.text = "Medicine"
                    eventTypeTextView.setBackgroundResource(R.drawable.bg_medicine_tag)
                }
                EventType.VACCINATION -> {
                    eventTypeTextView.text = "Vaccination"
                    eventTypeTextView.setBackgroundResource(R.drawable.bg_vaccination_tag)
                }
            }

            // Show time if available
            if (!petEvent.time.isNullOrEmpty()) {
                eventTimeTextView.visibility = View.VISIBLE
                eventTimeTextView.text = petEvent.time
            } else {
                eventTimeTextView.visibility = View.GONE
            }

            // Load pet image
            if (!petEvent.petImagePath.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(petEvent.petImagePath)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(petImageView)
            } else {
                petImageView.setImageResource(R.drawable.ic_placeholder)
            }

            itemView.setOnClickListener {
                onEventClick(petEvent)
            }
        }
    }

    class PetEventDiffCallback : DiffUtil.ItemCallback<PetEvent>() {
        override fun areItemsTheSame(oldItem: PetEvent, newItem: PetEvent): Boolean {
            return oldItem.id == newItem.id && oldItem.eventType == newItem.eventType
        }

        override fun areContentsTheSame(oldItem: PetEvent, newItem: PetEvent): Boolean {
            return oldItem == newItem
        }
    }
}