package com.mobdeve.s17.group39.itismob_mco.features.homepage

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.HomeGenreItemLayoutBinding

class HomeGenreViewHolder(val viewBinding: HomeGenreItemLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: String) {
        this.viewBinding.genreTv.text = data
    }
}