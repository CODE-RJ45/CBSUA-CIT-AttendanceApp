package com.coderj45.cbsua_cit_attendanceapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DateAdapter(
    private var dateList: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAttendanceDate: TextView = view.findViewById(R.id.tvAttendanceDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_folder, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dateList[position]
        holder.tvAttendanceDate.text = date
        holder.itemView.setOnClickListener { onItemClick(date) }
    }

    override fun getItemCount() = dateList.size

    fun updateList(newList: List<String>) {
        dateList = newList
        notifyDataSetChanged()
    }
}
