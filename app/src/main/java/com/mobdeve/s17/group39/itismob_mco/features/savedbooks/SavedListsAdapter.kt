package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.SavedListItemLayoutBinding

class SavedListsAdapter(
    private var data: List<SavedList>,
    private val onItemClick: (SavedList) -> Unit
) : RecyclerView.Adapter<SavedListsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedListsViewHolder {
        val itemViewBinding: SavedListItemLayoutBinding = SavedListItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedListsViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: SavedListsViewHolder, position: Int) {
        holder.bindData(data[position])
        holder.itemView.setOnClickListener {
            onItemClick(data[position])
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateData(newData: List<SavedList>) {
        data = newData
        notifyDataSetChanged()
    }
}