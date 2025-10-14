package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.ViewBookActivity


class HomeAdapter(private var data: List<Volume>): Adapter<HomeViewHolder>() {

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

        holder.itemView.setOnClickListener {
            val nextIntent = Intent(holder.itemView.context,
                ViewBookActivity::class.java)
            val position = holder.adapterPosition
            val authorsString = data.get(position).volumeInfo.authors?.joinToString(", ")
            val genreString = data.get(position).volumeInfo.categories?.joinToString(", ")
            val imageUrl = holder.getEnhancedImageUrl(data.get(position))

            nextIntent.putExtra(ViewBookActivity.Companion.TITLE_KEY, data.get(position).volumeInfo.title)
            nextIntent.putExtra(ViewBookActivity.Companion.AUTHOR_KEY, authorsString)
            nextIntent.putExtra(ViewBookActivity.Companion.DESCRIPTION_KEY, data.get(position).volumeInfo.description)
            nextIntent.putExtra(ViewBookActivity.Companion.AVG_RATING_KEY, data.get(position).volumeInfo.averageRating)
            nextIntent.putExtra(ViewBookActivity.Companion.RATING_COUNT_KEY, data.get(position).volumeInfo.ratingsCount)
            nextIntent.putExtra(ViewBookActivity.Companion.GENRE_KEY, genreString)
            nextIntent.putExtra(ViewBookActivity.Companion.POSITION_KEY, position)
            nextIntent.putExtra(ViewBookActivity.Companion.IMAGE_URL, imageUrl)

            holder.itemView.context.startActivity(nextIntent)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

}