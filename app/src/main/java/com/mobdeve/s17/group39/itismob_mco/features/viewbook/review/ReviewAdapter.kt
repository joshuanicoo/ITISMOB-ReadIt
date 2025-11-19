package com.mobdeve.s17.group39.itismob_mco.features.viewbook.review

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewItemLayoutBinding

class ReviewAdapter (private val data: ArrayList<ReviewModel>) : RecyclerView.Adapter<ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val itemViewBinding: ReviewItemLayoutBinding = ReviewItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val viewHolder = ReviewViewHolder(itemViewBinding)

        return viewHolder
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bindData(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}