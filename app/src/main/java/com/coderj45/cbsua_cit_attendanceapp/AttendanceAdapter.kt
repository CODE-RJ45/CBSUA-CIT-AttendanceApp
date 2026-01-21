package com.coderj45.cbsua_cit_attendanceapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(
    private var attendanceList: List<AttendanceEntity>,
    private val onDeleteClick: ((AttendanceEntity) -> Unit)? = null,
    private val onEditNameClick: ((AttendanceEntity) -> Unit)? = null
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentId: TextView = view.findViewById(R.id.tvStudentId)
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val item = attendanceList[position]
        holder.tvStudentId.text = item.studentId
        holder.tvDate.text = item.date
        holder.tvTime.text = item.scanTime
        
        if (!item.studentName.isNullOrEmpty()) {
            holder.tvStudentName.text = item.studentName
            holder.tvStudentName.visibility = View.VISIBLE
        } else {
            holder.tvStudentName.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onEditNameClick?.invoke(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(item)
        }
    }

    override fun getItemCount() = attendanceList.size

    fun updateList(newList: List<AttendanceEntity>) {
        attendanceList = newList
        notifyDataSetChanged()
    }
}
