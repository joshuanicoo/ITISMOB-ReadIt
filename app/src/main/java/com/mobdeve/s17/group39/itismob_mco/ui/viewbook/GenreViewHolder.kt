package com.mobdeve.s17.group39.itismob_mco.ui.viewbook

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.GenreItemLayoutBinding

class GenreViewHolder (private val viewBinding: GenreItemLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: String) {
        this.viewBinding.genreTv.text = data
    }
}