package com.mobdeve.s17.group39.itismob_mco.features.viewbook.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel

class ReviewAdapter(private val data: ArrayList<ReviewModel>) : RecyclerView.Adapter<ReviewViewHolder>() {

    // You'll need to pass current user ID to handle like states
    var currentUserId: String = ""

    // Like click listener
    var onLikeClickListener: ((ReviewModel, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val itemViewBinding: ReviewItemLayoutBinding = ReviewItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val viewHolder = ReviewViewHolder(itemViewBinding)

        // Set up like button click listener
        viewHolder.binding.isLikedIv.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onLikeClickListener?.invoke(data[position], position)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bindData(data[position], currentUserId)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    // Helper method to update a specific review (e.g., after liking)
    fun updateReview(position: Int, updatedReview: ReviewModel) {
        if (position in 0 until data.size) {
            data[position] = updatedReview
            notifyItemChanged(position)
        }
    }

    // Refresh all data
    fun refreshData(newData: List<ReviewModel>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }
}