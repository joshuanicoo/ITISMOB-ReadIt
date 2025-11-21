package com.mobdeve.s17.group39.itismob_mco.features.viewbook.review

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel
import com.mobdeve.s17.group39.itismob_mco.R

class ReviewViewHolder(val binding: ReviewItemLayoutBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindData(review: ReviewModel, currentUserId: String = "") {
        // Set username
        binding.usernameTv.text = review.username

        // Set rating (convert Int to Float for RatingBar)
        binding.userRatingRb.rating = review.rating.toFloat()

        // Set review comment
        binding.reviewBodyTv.text = review.comment

        // Load user profile picture
        if (!review.userProfilePicture.isNullOrEmpty()) {
            Glide.with(binding.root.context)
                .load(review.userProfilePicture)
                .placeholder(R.drawable.user_pfp_placeholder)
                .error(R.drawable.user_pfp_placeholder)
                .circleCrop()
                .into(binding.userPfpIv)
        } else {
            // Use default placeholder if no profile picture
            binding.userPfpIv.setImageResource(R.drawable.user_pfp_placeholder)
        }

        // Update like button state based on whether current user liked this review
        val isLikedByCurrentUser = review.likedBy.contains(currentUserId)
        updateLikeButtonUI(isLikedByCurrentUser)
    }

    private fun updateLikeButtonUI(isLiked: Boolean) {
        if (isLiked) {
            binding.isLikedIv.setImageResource(R.drawable.ic_heart_on)
        } else {
            binding.isLikedIv.setImageResource(R.drawable.ic_heart_off)
        }
    }
}