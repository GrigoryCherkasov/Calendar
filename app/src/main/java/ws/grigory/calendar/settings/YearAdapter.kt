package ws.grigory.calendar.settings

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ws.grigory.calendar.R

class YearAdapter(
    private val settingsActivity: SettingsActivity,
    private val years: ArrayList<Int>
) :
    RecyclerView.Adapter<YearViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        holder.yearItemValue.text = years[position].toString()
        holder.yearReload.setOnClickListener {
            val calendarStorage = CalendarStorage(settingsActivity)
            calendarStorage.reload(settingsActivity, years[holder.bindingAdapterPosition])
		}
        holder.yearDelete.setOnClickListener {
            val calendarStorage = CalendarStorage(settingsActivity)
            calendarStorage.delete(holder.bindingAdapterPosition, years, this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        return YearViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.year_item, parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return years.size
    }
}