package utar.edu.my.fyp.petschedule.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import utar.edu.my.fyp.R
import utar.edu.my.fyp.petschedule.Medicine

class MedicineAdapter(
    private val onItemClick: (Medicine) -> Unit = {}
) : ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MedicineViewHolder(
        itemView: View,
        private val onItemClick: (Medicine) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val medicineNameTextView: TextView = itemView.findViewById(R.id.textViewMedicineName)
        private val dosageTextView: TextView = itemView.findViewById(R.id.textViewDosage)
        private val frequencyTextView: TextView = itemView.findViewById(R.id.textViewFrequency)
        private val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        private val timeBoxesContainer: LinearLayout = itemView.findViewById(R.id.timeBoxesContainer)

        fun bind(medicine: Medicine) {
            medicineNameTextView.text = medicine.medicineName
            dosageTextView.text = "Dosage: ${medicine.dosage}"
            frequencyTextView.text = medicine.frequency.replace("_", " ").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            // Format time display for backward compatibility (keep the original timeTextView updated)
            val times = medicine.time.split(",").map { it.trim() }
            timeTextView.text = times.joinToString(", ")

            // Setup dynamic time boxes
            setupTimeBoxes(times)

            itemView.setOnClickListener {
                onItemClick(medicine)
            }
        }

        private fun setupTimeBoxes(times: List<String>) {
            timeBoxesContainer.removeAllViews()

            times.forEach { timeString ->
                val timeBox = createTimeBox(timeString, itemView.context)
                timeBoxesContainer.addView(timeBox)
            }
        }

        private fun createTimeBox(timeString: String, context: Context): View {
            // Create the time box container
            val timeBoxLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, dpToPx(context, 8), 0) // 8dp right margin
                }
                setPadding(dpToPx(context, 12), dpToPx(context, 12), dpToPx(context, 12), dpToPx(context, 12))
                setBackgroundResource(R.drawable.time_box_background)
                var minWidth = dpToPx(context, 64)
                gravity = android.view.Gravity.CENTER
            }

            // Create time TextView
            val timeTextView = TextView(context).apply {
                text = timeString
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                gravity = android.view.Gravity.CENTER
            }

            // Create AM/PM TextView
            val amPmTextView = TextView(context).apply {
                text = getAmPmText(timeString)
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, R.color.gray_text)) // You may need to define this color
                gravity = android.view.Gravity.CENTER
                setPadding(0, dpToPx(context, 2), 0, 0)
            }

            timeBoxLayout.addView(timeTextView)
            timeBoxLayout.addView(amPmTextView)

            return timeBoxLayout
        }

        private fun getAmPmText(timeString: String): String {
            return try {
                val parts = timeString.split(":")
                val hour = parts[0].toInt()
                if (hour < 12) "AM" else "PM"
            } catch (e: Exception) {
                "AM"
            }
        }

        private fun dpToPx(context: Context, dp: Int): Int {
            val density = context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }

    class MedicineDiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}