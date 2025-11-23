package com.mobdeve.s17.group39.itismob_mco.features.homepage

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.utils.Volume


class HomeViewHolder(private val viewBinding: BooksCardLayoutBinding) : RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: Volume) {
        val imageUrl = getEnhancedImageUrl(data)

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(android.R.color.transparent)
                .error(R.drawable.book_placeholder)
                .centerCrop()
                .into(this.viewBinding.bookImageIv)
        } else {
            this.viewBinding.bookImageIv.setImageResource(R.drawable.book_placeholder)
        }

        this.viewBinding.bookTitleTv.text = data.volumeInfo.title ?: "Unknown Title"
    }

    public fun getEnhancedImageUrl(data: Volume): String? {
        val imageLinks = data.volumeInfo?.imageLinks ?: return null

        // Try different image sizes in order of preference
        val baseUrl = imageLinks.thumbnail?.takeIf { it.isNotEmpty() }
            ?: imageLinks.smallThumbnail?.takeIf { it.isNotEmpty() }
            ?: imageLinks.medium?.takeIf { it.isNotEmpty() }
            ?: imageLinks.large?.takeIf { it.isNotEmpty() }
            ?: imageLinks.extraLarge?.takeIf { it.isNotEmpty() }
            ?: imageLinks.small?.takeIf { it.isNotEmpty() }
            ?: return null

        return enhanceGoogleBooksUrl(baseUrl)
    }

    private fun enhanceGoogleBooksUrl(url: String): String {
        var enhancedUrl = url.replace("http://", "https://")

        // Remove common unwanted parameters
        enhancedUrl = enhancedUrl.replace("&edge=curl", "")

        // Handle Google Books specific URLs
        when {
            enhancedUrl.contains("googleapis.com") -> {
                // For Google Books API images
                enhancedUrl = enhancedUrl.replace("zoom=1", "zoom=2")
                enhancedUrl = enhancedUrl.replace("imgmax=128", "imgmax=512")

                // Ensure we have proper parameters
                if (!enhancedUrl.contains("imgmax=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&imgmax=512" else "?imgmax=512"
                }
                if (!enhancedUrl.contains("zoom=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&zoom=2" else "?zoom=2"
                }
            }

            enhancedUrl.contains("books.google.com") -> {
                // For direct Google Books links
                enhancedUrl = enhancedUrl.replace("&printsec=frontcover", "&printsec=frontcover&img=1&zoom=2")

                // Add missing parameters for better quality
                if (!enhancedUrl.contains("img=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&img=1" else "?img=1"
                }
                if (!enhancedUrl.contains("zoom=")) {
                    enhancedUrl += "&zoom=2"
                }
            }

            enhancedUrl.contains("gstatic.com") -> {
            }
        }

        return enhancedUrl
    }
}