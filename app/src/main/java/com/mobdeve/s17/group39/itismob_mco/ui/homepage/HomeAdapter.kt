package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.ViewBookActivity

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

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(data[position], position)

            val nextIntent = Intent(holder.itemView.context, ViewBookActivity::class.java)
            val authorsString = data[position].volumeInfo.authors?.joinToString(", ")
            val genreString = data[position].volumeInfo.categories?.joinToString(", ")
            val imageUrl = holder.getEnhancedImageUrl(data[position])

            nextIntent.putExtra(ViewBookActivity.TITLE_KEY, data[position].volumeInfo.title)
            nextIntent.putExtra(ViewBookActivity.AUTHOR_KEY, authorsString)
            nextIntent.putExtra(ViewBookActivity.DESCRIPTION_KEY, data[position].volumeInfo.description)
            nextIntent.putExtra(ViewBookActivity.AVG_RATING_KEY, data[position].volumeInfo.averageRating)
            nextIntent.putExtra(ViewBookActivity.RATING_COUNT_KEY, data[position].volumeInfo.ratingsCount)
            nextIntent.putExtra(ViewBookActivity.GENRE_KEY, genreString)
            nextIntent.putExtra(ViewBookActivity.POSITION_KEY, position)
            nextIntent.putExtra(ViewBookActivity.IMAGE_URL, imageUrl)

            holder.itemView.context.startActivity(nextIntent)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}