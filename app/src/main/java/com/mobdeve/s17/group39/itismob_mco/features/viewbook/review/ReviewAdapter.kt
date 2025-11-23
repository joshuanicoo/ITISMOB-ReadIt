package com.mobdeve.s17.group39.itismob_mco.features.viewbook.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel

class ReviewAdapter(private val data: ArrayList<ReviewModel>) : RecyclerView.Adapter<ReviewViewHolder>() {

    var currentUserId: String = ""
    var onReviewLikeClickListener: ((ReviewModel, Int) -> Unit)? = null
    private var isProcessingLike = false // Add this flag

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val itemViewBinding: ReviewItemLayoutBinding = ReviewItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val viewHolder = ReviewViewHolder(itemViewBinding)

        viewHolder.binding.likeReviewBtn.setOnClickListener {
            if (!isProcessingLike) {
                isProcessingLike = true
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onReviewLikeClickListener?.invoke(data[position], position)
                }
                // Re-enable after a short delay to prevent rapid clicking
                viewHolder.binding.likeReviewBtn.postDelayed({
                    isProcessingLike = false
                }, 1000)
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

    // Helper method to update a specific review
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