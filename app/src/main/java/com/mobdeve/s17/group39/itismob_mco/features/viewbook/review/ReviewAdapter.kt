package com.mobdeve.s17.group39.itismob_mco.features.viewbook.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel

class ReviewAdapter(private val data: ArrayList<ReviewModel>) : RecyclerView.Adapter<ReviewViewHolder>() {

    var currentUserId: String = ""
    var onReviewLikeClickListener: ((ReviewModel, Int) -> Unit)? = null
    private var isProcessingLike = false

    // Filtered list that only contains reviews with comments
    private val filteredData: List<ReviewModel>
        get() = data.filter { it.comment.isNotEmpty() }

    // Map to convert filtered position to original position
    private fun getOriginalPosition(filteredPosition: Int): Int {
        var count = 0
        for (i in data.indices) {
            if (data[i].comment.isNotEmpty()) {
                if (count == filteredPosition) {
                    return i
                }
                count++
            }
        }
        return -1
    }

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
                    val originalPosition = getOriginalPosition(position)
                    if (originalPosition != -1) {
                        onReviewLikeClickListener?.invoke(data[originalPosition], originalPosition)
                    }
                }
                viewHolder.binding.likeReviewBtn.postDelayed({
                    isProcessingLike = false
                }, 1000)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = filteredData[position]
        holder.bindData(review, currentUserId)
    }

    override fun getItemCount(): Int {
        return filteredData.size
    }

    // Helper method to update a specific review
    fun updateReview(position: Int, updatedReview: ReviewModel) {
        if (position in 0 until data.size) {
            data[position] = updatedReview
            // Notify based on whether this item should be visible or not
            if (updatedReview.comment.isNotEmpty()) {
                // Find the filtered position
                val filteredPosition = filteredData.indexOfFirst { it.id == updatedReview.id }
                if (filteredPosition != -1) {
                    notifyItemChanged(filteredPosition)
                } else {
                    // If it was empty before but now has a comment, insert it
                    notifyDataSetChanged()
                }
            } else {
                // If it now has empty comment, remove it from display
                notifyDataSetChanged()
            }
        }
    }

    // Refresh all data
    fun refreshData(newData: List<ReviewModel>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    // Helper to check if there are any reviews with comments
    fun hasReviewsWithComments(): Boolean {
        return filteredData.isNotEmpty()
    }
}