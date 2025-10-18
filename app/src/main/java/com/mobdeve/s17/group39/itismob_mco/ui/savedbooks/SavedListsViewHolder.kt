package com.mobdeve.s17.group39.itismob_mco.ui.savedbooks

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.SavedListItemLayoutBinding

class SavedListsViewHolder(private val viewBinding: SavedListItemLayoutBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: SavedList) {
        viewBinding.listNameTv.text = data.name
        viewBinding.bookCountTv.text = "${data.bookCount} ${if (data.bookCount == 1) "book" else "books"}"
    }
}