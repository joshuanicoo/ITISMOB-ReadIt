package com.mobdeve.s17.group39.itismob_mco.features.viewbook.list

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.ListsItemLayoutBinding

class AddToListViewHolder (private val viewBinding: ListsItemLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: String) {
        this.viewBinding.listNameTv.text = data
    }
}