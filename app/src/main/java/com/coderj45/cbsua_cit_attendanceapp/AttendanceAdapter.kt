package com.coderj45.cbsua_cit_attendanceapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AttendanceAdapter(
    private var attendanceList: List<AttendanceEntity>,
    private val onDeleteClick: ((AttendanceEntity) -> Unit)? = null,
    private val onEditNameClick: ((AttendanceEntity) -> Unit)? = null,
    private val onStatusClick: ((AttendanceEntity) -> Unit)? = null
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStudentId: TextView = view.findViewById(R.id.tvStudentId)
        val tvStudentName: TextView = view.findViewById(R.id.tvStudentName)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
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
        
        // Handle scan time display
        if (item.scanTime == "ABSENT") {
            holder.tvTime.visibility = View.GONE
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = "A"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_circle)
            holder.tvStatus.background.setTint(Color.parseColor("#FF5252")) // Red for Absent
        } else if (item.scanTime == "EXCUSED") {
            holder.tvTime.visibility = View.GONE
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = "E"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_circle)
            holder.tvStatus.background.setTint(Color.parseColor("#FFB300")) // Amber for Excused
        } else {
            holder.tvTime.text = item.scanTime
            holder.tvTime.visibility = View.VISIBLE
            holder.tvStatus.visibility = View.GONE
        }
        
        if (!item.studentName.isNullOrEmpty()) {
            holder.tvStudentName.text = item.studentName
            holder.tvStudentName.visibility = View.VISIBLE
        } else {
            holder.tvStudentName.text = "Unknown"
            holder.tvStudentName.visibility = View.VISIBLE
        }
        
        holder.itemView.setOnClickListener {
            if (item.scanTime == "ABSENT" || item.scanTime == "EXCUSED") {
                onStatusClick?.invoke(item)
            } else {
                onEditNameClick?.invoke(item)
            }
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