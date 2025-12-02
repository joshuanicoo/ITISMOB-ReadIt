package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.SavedListItemLayoutBinding

class SavedListsAdapter(
    private var data: List<SavedList>,
    private val onItemClick: (SavedList) -> Unit,
    private val onDeleteClick: ((String) -> Unit)? = null // Add delete callback
) : RecyclerView.Adapter<SavedListsViewHolder>() {

    private var isDeleteMode: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedListsViewHolder {
        val itemViewBinding: SavedListItemLayoutBinding = SavedListItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedListsViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: SavedListsViewHolder, position: Int) {
        val savedList = data[position]

        holder.bindData(
            data = savedList,
            isDeleteMode = isDeleteMode,
            onDeleteClick = if (isDeleteMode) {
                { onDeleteClick?.invoke(savedList.id) }
            } else {
                null
            }
        )

        // Set item click listener only if not in delete mode
        if (!isDeleteMode) {
            holder.itemView.setOnClickListener {
                onItemClick(savedList)
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateData(newData: List<SavedList>) {
        data = newData
        notifyDataSetChanged()
    }

    // Add method to toggle delete mode
    fun setDeleteMode(deleteMode: Boolean) {
        isDeleteMode = deleteMode
        notifyDataSetChanged()
    }

    fun getDeleteMode(): Boolean = isDeleteMode
}