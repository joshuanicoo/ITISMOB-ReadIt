package com.mobdeve.s17.group39.itismob_mco.features.viewbook.list

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.ListsItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.models.ListModel

class AddToListViewHolder(private val itemViewBinding: ListsItemLayoutBinding) :
    RecyclerView.ViewHolder(itemViewBinding.root) {

    fun bindData(list: ListModel, bookId: String, onListCheckedChange: (String, String, Boolean) -> Unit) {
        itemViewBinding.listNameTv.text = list.listName

        ListsDatabase.isBookInList(list.documentId, bookId)
            .addOnSuccessListener { isInList ->
                itemViewBinding.listCheckbox.isChecked = isInList
            }
            .addOnFailureListener {
                itemViewBinding.listCheckbox.isChecked = false
            }

        itemViewBinding.listCheckbox.setOnCheckedChangeListener { _, isChecked ->
            onListCheckedChange(list.documentId, list.listName, isChecked)
        }

        itemViewBinding.root.setOnClickListener {
            itemViewBinding.listCheckbox.isChecked = !itemViewBinding.listCheckbox.isChecked
        }
    }
}