package utar.edu.my.fyp.petschedule.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Vaccination
import java.text.SimpleDateFormat
import java.util.*

class VaccinationAdapter(
    private val onVaccinationClick: (Vaccination) -> Unit = {}
) : ListAdapter<Vaccination, VaccinationAdapter.VaccinationViewHolder>(VaccinationDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaccinationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vaccination, parent, false)
        return VaccinationViewHolder(view, onVaccinationClick)
    }

    override fun onBindViewHolder(holder: VaccinationViewHolder, position: Int) {
        holder.bind(getItem(position), dateFormat)
    }

    class VaccinationViewHolder(
        itemView: View,
        private val onVaccinationClick: (Vaccination) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val vaccineNameTextView: TextView = itemView.findViewById(R.id.textViewVaccineName)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.textViewDueDate)
        private val statusTextView: TextView = itemView.findViewById(R.id.textViewStatus)

        // Add the missing calendar TextViews
        private val textViewDay: TextView = itemView.findViewById(R.id.textViewDay)
        private val textViewMonth: TextView = itemView.findViewById(R.id.textViewMonth)
        private val textViewYear: TextView = itemView.findViewById(R.id.textViewYear)

        fun bind(vaccination: Vaccination, dateFormat: SimpleDateFormat) {
            vaccineNameTextView.text = vaccination.vaccineName
            dueDateTextView.text = "Due: ${dateFormat.format(vaccination.dueDate)}"
            statusTextView.text = if (vaccination.isCompleted) "Completed" else "Pending"

            // Bind the calendar date display
            val calendar = Calendar.getInstance().apply { time = vaccination.dueDate }
            textViewDay.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
            textViewMonth.text = SimpleDateFormat("MMM", Locale.getDefault())
                .format(vaccination.dueDate).uppercase()
            textViewYear.text = calendar.get(Calendar.YEAR).toString()

            itemView.setOnClickListener {
                onVaccinationClick(vaccination)
            }
        }
    }

    class VaccinationDiffCallback : DiffUtil.ItemCallback<Vaccination>() {
        override fun areItemsTheSame(oldItem: Vaccination, newItem: Vaccination): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Vaccination, newItem: Vaccination): Boolean {
            return oldItem == newItem
        }
    }
}