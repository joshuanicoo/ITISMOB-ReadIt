package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.SavedListItemLayoutBinding

class SavedListsViewHolder(private val viewBinding: SavedListItemLayoutBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: SavedList, isDeleteMode: Boolean = false, onDeleteClick: (() -> Unit)? = null) {
        viewBinding.listNameTv.text = data.name
        viewBinding.bookCountTv.text = "${data.bookCount} ${if (data.bookCount == 1) "book" else "books"}"

        // Handle delete mode
        if (isDeleteMode) {
            // Show delete button
            viewBinding.deleteListBtn.visibility = View.VISIBLE

            // Set delete button click listener
            viewBinding.deleteListBtn.setOnClickListener {
                onDeleteClick?.invoke()
            }

            // Disable normal item click in delete mode
            itemView.isClickable = false
            itemView.isFocusable = false
        } else {
            // Hide delete button
            viewBinding.deleteListBtn.visibility = View.GONE

            // Remove delete button click listener
            viewBinding.deleteListBtn.setOnClickListener(null)

            // Enable normal item click
            itemView.isClickable = true
            itemView.isFocusable = true
        }
    }
}