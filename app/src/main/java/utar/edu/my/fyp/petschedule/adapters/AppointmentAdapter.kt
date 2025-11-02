package utar.edu.my.fyp.petschedule.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Appointment
import java.text.SimpleDateFormat
import java.util.*

class AppointmentAdapter(
    private val onAppointmentClick: (Appointment) -> Unit
) : ListAdapter<Appointment, AppointmentAdapter.AppointmentViewHolder>(AppointmentDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view, onAppointmentClick)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat)
    }

    class AppointmentViewHolder(
        itemView: View,
        private val onAppointmentClick: (Appointment) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewAppointmentTitle)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewAppointmentDate)
        private val timeTextView: TextView = itemView.findViewById(R.id.textViewAppointmentTime)
        private val locationTextView: TextView = itemView.findViewById(R.id.textViewLocation)

        // New date box views similar to vaccination layout
        private val dayTextView: TextView = itemView.findViewById(R.id.textViewDay)
        private val monthTextView: TextView = itemView.findViewById(R.id.textViewMonth)
        private val yearTextView: TextView = itemView.findViewById(R.id.textViewYear)

        fun bind(appointment: Appointment, dateFormat: SimpleDateFormat) {
            titleTextView.text = appointment.title
            dateTextView.text = dateFormat.format(appointment.appointmentDate)
            timeTextView.text = appointment.time
            locationTextView.text = appointment.location ?: "No location specified"

            // Format and set the date box components similar to vaccination
            val calendar = Calendar.getInstance()
            calendar.time = appointment.appointmentDate

            // Set day
            dayTextView.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            // Set month (abbreviated)
            val monthNames = arrayOf(
                "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
            )
            monthTextView.text = monthNames[calendar.get(Calendar.MONTH)]

            // Set year
            yearTextView.text = calendar.get(Calendar.YEAR).toString()

            itemView.setOnClickListener {
                onAppointmentClick(appointment)
            }
        }
    }

    class AppointmentDiffCallback : DiffUtil.ItemCallback<Appointment>() {
        override fun areItemsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Appointment, newItem: Appointment): Boolean {
            return oldItem == newItem
        }
    }
}