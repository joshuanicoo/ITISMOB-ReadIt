package com.mobdeve.s17.group39.itismob_mco.features.viewbook.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.ListsItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ListModel

class AddToListAdapter(
    private var lists: List<ListModel>,
    private val bookId: String,
    private val onListCheckedChange: (String, String, Boolean) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<AddToListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddToListViewHolder {
        val itemViewBinding: ListsItemLayoutBinding = ListsItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddToListViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: AddToListViewHolder, position: Int) {
        val list = lists[position]
        holder.bindData(list, bookId, onListCheckedChange)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    fun updateData(newLists: List<ListModel>) {
        lists = newLists
        notifyDataSetChanged()
    }
}