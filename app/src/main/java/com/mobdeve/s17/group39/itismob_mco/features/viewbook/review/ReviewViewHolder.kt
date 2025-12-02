package com.mobdeve.s17.group39.itismob_mco.features.viewbook.review

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel
import com.mobdeve.s17.group39.itismob_mco.R

class ReviewViewHolder(val binding: ReviewItemLayoutBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bindData(review: ReviewModel, currentUserId: String = "") {
        // Set username (now part of ReviewModel)
        binding.usernameTv.text = review.username

        // Set rating
        binding.userRatingRb.rating = review.rating

        // Set review comment
        binding.reviewBodyTv.text = review.comment

        // Set likes count
        binding.likesTv.text = review.likes.toString()

        // Show if the author of the review liked the book
        updateAuthorLikeUI(review.authorLikedBook)

        // Show if the current user liked this review
        val isLikedByCurrentUser = review.likedBy.contains(currentUserId)
        updateReviewLikeUI(isLikedByCurrentUser)

        // Load user profile picture (now part of ReviewModel)
        if (!review.userProfilePicture.isNullOrEmpty()) {
            Glide.with(binding.root.context)
                .load(review.userProfilePicture)
                .placeholder(R.drawable.user_pfp_placeholder)
                .error(R.drawable.user_pfp_placeholder)
                .circleCrop()
                .into(binding.userPfpIv)
        } else {
            binding.userPfpIv.setImageResource(R.drawable.user_pfp_placeholder)
        }
    }

    private fun updateAuthorLikeUI(authorLikedBook: Boolean) {
        // This shows if the review author liked the book
        if (authorLikedBook) {
            binding.isLikedIv.setImageResource(R.drawable.ic_heart_on)
            binding.isLikedIv.visibility = android.view.View.VISIBLE
        } else {
            binding.isLikedIv.setImageResource(R.drawable.ic_heart_off)
            binding.isLikedIv.visibility = android.view.View.GONE
        }
    }

    private fun updateReviewLikeUI(isLikedByCurrentUser: Boolean) {
        // This shows if the current user liked this review
        if (isLikedByCurrentUser) {
            binding.likeReviewBtn.setImageResource(R.drawable.ic_heart_on)
        } else {
            binding.likeReviewBtn.setImageResource(R.drawable.ic_heart_off)
        }
    }
}