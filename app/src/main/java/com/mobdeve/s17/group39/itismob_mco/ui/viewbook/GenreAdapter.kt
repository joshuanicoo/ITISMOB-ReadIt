package com.mobdeve.s17.group39.itismob_mco.ui.viewbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.mobdeve.s17.group39.itismob_mco.databinding.GenreItemLayoutBinding

class GenreAdapter (private val data: ArrayList<String>) : Adapter<GenreViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
            val itemViewBinding: GenreItemLayoutBinding = GenreItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            val viewHolder = GenreViewHolder(itemViewBinding)

            return viewHolder
        }

        override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
            holder.bindData(data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }
}