package com.coderj45.cbsua_cit_attendanceapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClassAdapter(
    private var classList: List<ClassInfo>,
    private val onItemClick: (ClassInfo) -> Unit,
    private val onDeleteClick: (ClassInfo) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubjectName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvSectionName: TextView = view.findViewById(R.id.tvSectionName)
        val tvCreatedOn: TextView = view.findViewById(R.id.tvCreatedOn)
        val btnDeleteFolder: ImageButton = view.findViewById(R.id.btnDeleteFolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val item = classList[position]
        holder.tvSubjectName.text = item.subject
        holder.tvSectionName.text = "Section: ${item.section}"
        holder.tvCreatedOn.text = "First Scan: ${item.firstScan ?: "N/A"}"
        
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnDeleteFolder.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = classList.size

    fun updateList(newList: List<ClassInfo>) {
        classList = newList
        notifyDataSetChanged()
    }
}
