package com.mobdeve.s17.group39.itismob_mco.ui.viewbook

import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewItemLayoutBinding

class ReviewViewHolder (private val viewBinding: ReviewItemLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: ReviewModel) {
        this.viewBinding.userPfpIv.setImageResource(data.userPfpResId)
        this.viewBinding.usernameTv.text = data.username
        this.viewBinding.userRatingRb.rating = data.userRating
        this.viewBinding.reviewBodyTv.text = data.reviewBody
        this.viewBinding.likesTv.text = data.likesCount.toString()
    }
}