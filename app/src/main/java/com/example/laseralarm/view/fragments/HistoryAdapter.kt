package com.example.laseralarm.view.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.laseralarm.R
import com.example.laseralarm.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private var items: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    fun update(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val valueText: TextView = itemView.findViewById(R.id.tvHistoryValue)
        private val timeText: TextView = itemView.findViewById(R.id.tvHistoryTime)
        private val sourceText: TextView = itemView.findViewById(R.id.tvHistorySource)
        private val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())

        fun bind(item: HistoryItem) {
            valueText.text = "Value: ${item.value}"
            timeText.text = "Time: ${dateFormat.format(Date(item.timestamp))}"
            sourceText.text = "Source: ${item.source ?: "Unknown"}"
        }
    }
}
