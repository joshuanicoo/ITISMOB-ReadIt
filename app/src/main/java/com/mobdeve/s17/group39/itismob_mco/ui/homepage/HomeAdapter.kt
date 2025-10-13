package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding


class HomeAdapter(private val data: GoogleBooksResponse): Adapter<HomeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val itemViewBinding: BooksCardLayoutBinding = BooksCardLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)


        val toViewHolder = HomeViewHolder(itemViewBinding)

        return toViewHolder
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bindData(data.items[position])
    }

    override fun getItemCount(): Int {
        return data.items.size
    }

}