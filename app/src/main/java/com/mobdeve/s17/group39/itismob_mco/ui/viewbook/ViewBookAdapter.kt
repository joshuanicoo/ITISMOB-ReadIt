package com.mobdeve.s17.group39.itismob_mco.ui.viewbook

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.activity.result.ActivityResultLauncher
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.ui.homepage.HomeViewHolder
import com.mobdeve.s17.group39.itismob_mco.ui.homepage.Volume

class ViewBookAdapter (private val data: ArrayList<Volume>, private val viewBookLauncher: ActivityResultLauncher<Intent>) : Adapter<HomeViewHolder>() {
        companion object {
            const val TITLE_KEY = "TITLE_KEY"
            const val AUTHOR_KEY = "AUTHOR_KEY"
            const val DESCRIPTION_KEY = "DESCRIPTION_KEY"
            const val AVG_RATING_COUNT_KEY = "AVG_RATING_COUNT_KEY"
            const val RATING_COUNT_KEY = "RATING_COUNT_KEY"
            const val POSITION_KEY = "POSITION_KEY"
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
            val itemViewBinding: BooksCardLayoutBinding = BooksCardLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            val myViewHolder = HomeViewHolder(itemViewBinding)

            return myViewHolder
        }

        override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
            holder.bindData(data[position])
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }