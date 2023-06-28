package ws.grigory.calendar.settings

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ws.grigory.calendar.R

class YearViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val yearItemValue: TextView = itemView.findViewById(R.id.yearItemValue)
    val yearReload: ImageView = itemView.findViewById(R.id.yearReload)
    val yearDelete: ImageView = itemView.findViewById(R.id.yearDelete)
}