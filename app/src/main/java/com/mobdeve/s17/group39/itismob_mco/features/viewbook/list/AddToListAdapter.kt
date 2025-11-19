package com.mobdeve.s17.group39.itismob_mco.features.viewbook.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.ListsItemLayoutBinding

class AddToListAdapter (private val data: ArrayList<String>) : RecyclerView.Adapter<AddToListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddToListViewHolder {
        val itemViewBinding: ListsItemLayoutBinding = ListsItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val viewHolder = AddToListViewHolder(itemViewBinding)

        return viewHolder
    }

    override fun onBindViewHolder(holder: AddToListViewHolder, position: Int) {
        holder.bindData(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}