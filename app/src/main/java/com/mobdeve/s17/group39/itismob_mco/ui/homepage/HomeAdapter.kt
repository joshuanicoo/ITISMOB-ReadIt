package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import android.view.animation.AnimationUtils
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.ViewBookActivity
import com.mobdeve.s17.group39.itismob_mco.R

class HomeAdapter(private var data: List<Volume>): Adapter<HomeViewHolder>() {

    private var onItemClickListener: ((Volume, Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Volume, Int) -> Unit) {
        onItemClickListener = listener
    }

    fun updateData(newData : List<Volume>) {
        this.data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val itemViewBinding: BooksCardLayoutBinding = BooksCardLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        val viewHolder = HomeViewHolder(itemViewBinding)

        return viewHolder
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bindData(data[position])
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.anim_one))

        holder.itemView.setOnClickListener {
            // custom listener if set, default if not
            if (onItemClickListener != null) {
                onItemClickListener?.invoke(data[position], position)
            } else {
                openBookDetails(holder, data[position], position)
            }
        }

    }

    private fun openBookDetails(holder: HomeViewHolder, volume: Volume, position: Int) {
        val nextIntent = Intent(holder.itemView.context, ViewBookActivity::class.java)
        val authorsString = volume.volumeInfo.authors?.joinToString(", ")
        val genreString = volume.volumeInfo.categories?.joinToString(", ")
        val imageUrl = holder.getEnhancedImageUrl(volume)

        nextIntent.putExtra(ViewBookActivity.TITLE_KEY, volume.volumeInfo.title)
        nextIntent.putExtra(ViewBookActivity.AUTHOR_KEY, authorsString)
        nextIntent.putExtra(ViewBookActivity.DESCRIPTION_KEY, volume.volumeInfo.description)
        nextIntent.putExtra(ViewBookActivity.AVG_RATING_KEY, volume.volumeInfo.averageRating)
        nextIntent.putExtra(ViewBookActivity.RATING_COUNT_KEY, volume.volumeInfo.ratingsCount)
        nextIntent.putExtra(ViewBookActivity.GENRE_KEY, genreString)
        nextIntent.putExtra(ViewBookActivity.POSITION_KEY, position)
        nextIntent.putExtra(ViewBookActivity.IMAGE_URL, imageUrl)

        holder.itemView.context.startActivity(nextIntent)
    }

    override fun getItemCount(): Int {
        return data.size
    }
}