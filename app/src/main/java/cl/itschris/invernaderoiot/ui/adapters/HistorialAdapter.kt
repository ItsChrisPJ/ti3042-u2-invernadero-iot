package cl.itschris.invernaderoiot.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import cl.itschris.invernaderoiot.R
import cl.itschris.invernaderoiot.data.models.SensorReading
import cl.itschris.invernaderoiot.databinding.ItemSensorReadingBinding

/**
 * Adaptador de RecyclerView para listar las lecturas de telemetría almacenadas en SQLite.
 */
class HistorialAdapter(
    private var readings: List<SensorReading>
) : RecyclerView.Adapter<HistorialAdapter.ReadingViewHolder>() {

    inner class ReadingViewHolder(val binding: ItemSensorReadingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReadingViewHolder {
        val binding = ItemSensorReadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReadingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReadingViewHolder, position: Int) {
        val item = readings[position]
        val context = holder.itemView.context

        holder.binding.tvItemTimestamp.text = item.timestamp
        holder.binding.tvItemMode.text = item.mode.uppercase()
        holder.binding.tvItemTemp.text = "🌡️ ${String.format("%.1f", item.temperature)} °C"
        holder.binding.tvItemHumidity.text = "💧 ${String.format("%.1f", item.humidity)} %"

        // Color dinámico según la temperatura
        val tempColor = when {
            item.temperature >= 28.0 -> ContextCompat.getColor(context, R.color.temp_hot)
            item.temperature <= 18.0 -> ContextCompat.getColor(context, R.color.temp_cold)
            else -> ContextCompat.getColor(context, R.color.temp_optimal)
        }
        holder.binding.tvItemTemp.setTextColor(tempColor)

        // Estado del ventilador
        if (item.fanStatus) {
            holder.binding.tvItemFanStatus.text = "VENTILADOR ON"
            holder.binding.tvItemFanStatus.setBackgroundColor(
                ContextCompat.getColor(context, R.color.fan_active)
            )
        } else {
            holder.binding.tvItemFanStatus.text = "VENTILADOR OFF"
            holder.binding.tvItemFanStatus.setBackgroundColor(
                ContextCompat.getColor(context, R.color.fan_inactive)
            )
        }
    }

    override fun getItemCount(): Int = readings.size

    fun updateList(newList: List<SensorReading>) {
        readings = newList
        notifyDataSetChanged()
    }
}
