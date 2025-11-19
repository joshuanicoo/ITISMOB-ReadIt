package com.mobdeve.s17.group39.itismob_mco.features.homepage

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.utils.Volume


class HomeViewHolder(private val viewBinding: BooksCardLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: Volume) {
        val imageUrl = getEnhancedImageUrl(data)

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.content)
                .error(R.drawable.content)
                .centerCrop()
                .into(this.viewBinding.bookImageIv)
        } else {
            this.viewBinding.bookImageIv.setImageResource(R.drawable.content)
        }

        this.viewBinding.bookTitleTv.text = data.volumeInfo.title
    }

    public fun getEnhancedImageUrl(data: Volume): String? {
        val baseUrl = data.volumeInfo?.imageLinks?.thumbnail
            ?: data.volumeInfo?.imageLinks?.smallThumbnail
            ?: ""

        if (baseUrl.isEmpty()) return null

        var url = baseUrl.replace("http://", "https://")

        url = url.replace("&edge=curl", "")
        url = url.replace("zoom=1", "zoom=2")
        url = url.replace("imgmax=128", "imgmax=512")

        if (url.contains("googlebooks")) {
            url = url.replace("&printsec=frontcover", "&printsec=frontcover&img=1&zoom=2")
        }

        return url
    }
}