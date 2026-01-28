package com.coderj45.cbsua_cit_attendanceapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class DateAdapter(
    private var dateList: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM. dd, yyyy EEEE", Locale.getDefault())

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAttendanceDate: TextView = view.findViewById(R.id.tvAttendanceDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_folder, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val rawDate = dateList[position]
        
        // Format the date for display
        val formattedDate = try {
            val dateObj = inputFormat.parse(rawDate)
            if (dateObj != null) displayFormat.format(dateObj) else rawDate
        } catch (e: Exception) {
            rawDate
        }

        holder.tvAttendanceDate.text = formattedDate
        holder.itemView.setOnClickListener { onItemClick(rawDate) }
    }

    override fun getItemCount() = dateList.size

    fun updateList(newList: List<String>) {
        dateList = newList
        notifyDataSetChanged()
    }
}
