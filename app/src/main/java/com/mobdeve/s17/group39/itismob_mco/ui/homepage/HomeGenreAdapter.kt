package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.GenreItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.HomeGenreItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.genre.GenreViewHolder

class HomeGenreAdapter (private val data: ArrayList<String>) : RecyclerView.Adapter<HomeGenreViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeGenreViewHolder {
            val itemViewBinding: HomeGenreItemLayoutBinding = HomeGenreItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            val viewHolder = HomeGenreViewHolder(itemViewBinding)

            return viewHolder
        }

        override fun onBindViewHolder(holder: HomeGenreViewHolder, position: Int) {
            holder.bindData(data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }
}