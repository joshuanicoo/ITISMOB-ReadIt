package com.mobdeve.s17.group39.itismob_mco.features.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.R

class ProfileFavoritesAdapter(private var favorites: List<ProfileActivity.BookItem> = emptyList(), private val onBookClick: (ProfileActivity.BookItem) -> Unit) : RecyclerView.Adapter<ProfileFavoritesAdapter.FavoriteBookViewHolder>() {

    inner class FavoriteBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookCoverIv: ImageView = itemView.findViewById(R.id.bookCoverIv)
        private val bookTitleTv: TextView = itemView.findViewById(R.id.bookTitleTv)
        private val bookAuthorTv: TextView = itemView.findViewById(R.id.bookAuthorTv)

        fun bind(book: ProfileActivity.BookItem) {
            if (book.thumbnailUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(book.thumbnailUrl)
                    .placeholder(android.R.color.transparent)
                    .error(R.drawable.book_placeholder)
                    .centerCrop()
                    .into(bookCoverIv)
            } else {
                bookCoverIv.setImageResource(R.drawable.book_placeholder)
            }

            bookTitleTv.text = book.title
            bookAuthorTv.text = book.authors.joinToString(", ")
            itemView.setOnClickListener { onBookClick(book) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_favorite_book, parent, false)
        return FavoriteBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteBookViewHolder, position: Int) {
        holder.bind(favorites[position])
    }

    override fun getItemCount(): Int = favorites.size

    fun updateFavorites(newFavorites: List<ProfileActivity.BookItem>) {
        favorites = newFavorites
        notifyDataSetChanged()
    }
}