package com.mobdeve.s17.group39.itismob_mco.features.homepage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.HomeGenreItemLayoutBinding

class HomeGenreAdapter(
    private val data: ArrayList<String>,
    private val onGenreClickListener: ((String, Int) -> Unit)? = null
) : RecyclerView.Adapter<HomeGenreViewHolder>() {

    private var selectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeGenreViewHolder {
        val itemViewBinding: HomeGenreItemLayoutBinding = HomeGenreItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val viewHolder = HomeGenreViewHolder(itemViewBinding)

        // Set click listener on the entire genre item
        itemViewBinding.root.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onGenreClickListener?.invoke(data[position], position)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: HomeGenreViewHolder, position: Int) {
        holder.bindData(data[position])

        // Highlight selected genre - style the MaterialCardView instead
        if (position == selectedPosition) {
            holder.viewBinding.genreCardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.viewBinding.root.context, R.color.main_green)
            )
            holder.viewBinding.genreTv.setTextColor(
                ContextCompat.getColor(holder.viewBinding.root.context, R.color.white)
            )
            holder.viewBinding.genreCardView.strokeWidth = 0 // Remove border when selected
        } else {
            holder.viewBinding.genreCardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.viewBinding.root.context, R.color.white)
            )
            holder.viewBinding.genreTv.setTextColor(
                ContextCompat.getColor(holder.viewBinding.root.context, R.color.black)
            )
            holder.viewBinding.genreCardView.strokeWidth = 1 // Add border when not selected
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateGenres(newGenres: List<String>) {
        data.clear()
        data.addAll(newGenres)
        selectedPosition = 0 // Reset selection to "All"
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        val previousSelected = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelected)
        notifyItemChanged(selectedPosition)
    }
}