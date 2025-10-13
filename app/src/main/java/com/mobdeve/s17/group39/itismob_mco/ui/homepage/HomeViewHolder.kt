package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding

class HomeViewHolder(private val viewBinding: BooksCardLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: Volume) {
        this.viewBinding.bookTitleTv.text = data.volumeInfo.title
    }

}