package com.mobdeve.s17.group39.itismob_mco.features.homepage

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksCardLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.ViewBookActivity
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.utils.GoogleBooksApiInterface
import com.mobdeve.s17.group39.itismob_mco.utils.RetrofitInstance
import com.mobdeve.s17.group39.itismob_mco.utils.Volume
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeAdapter(private var data: List<Volume>): androidx.recyclerview.widget.RecyclerView.Adapter<HomeViewHolder>() {

    private var onItemClickListener: ((Volume, Int) -> Unit)? = null
    private val googleBooksApi: GoogleBooksApiInterface = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)

    // Expose current data for filtering - ADD THIS PROPERTY
    val currentData: List<Volume>
        get() = data

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
            if (onItemClickListener != null) {
                onItemClickListener?.invoke(data[position], position)
            } else {
                openBookDetails(holder, data[position], position)
            }
        }
    }

    private fun openBookDetails(holder: HomeViewHolder, volume: Volume, position: Int) {
        // When searching by book name, genre, etc., it does not return full details
        // So we do double API call to get full details of each book
        googleBooksApi.getBookByVolumeId(volume.id).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    response.body()?.let { fullBookData ->
                        val volumeInfo = fullBookData["volumeInfo"] as? Map<String, Any>
                        val categories = processCategories(volumeInfo?.get("categories"))
                        val genreString = categories.joinToString(", ")

                        holder.itemView.context.startActivity(
                            Intent(holder.itemView.context, ViewBookActivity::class.java).apply {
                                putExtra(ViewBookActivity.ID_KEY, volume.id)
                                putExtra(ViewBookActivity.TITLE_KEY, volume.volumeInfo.title)
                                putExtra(ViewBookActivity.AUTHOR_KEY, volume.volumeInfo.authors?.joinToString(", "))
                                putExtra(ViewBookActivity.DESCRIPTION_KEY, volume.volumeInfo.description)
                                putExtra(ViewBookActivity.AVG_RATING_KEY, volume.volumeInfo.averageRating)
                                putExtra(ViewBookActivity.RATING_COUNT_KEY, volume.volumeInfo.ratingsCount)
                                putExtra(ViewBookActivity.GENRE_KEY, genreString)
                                putExtra(ViewBookActivity.POSITION_KEY, position)
                                putExtra(ViewBookActivity.IMAGE_URL, holder.getEnhancedImageUrl(volume))
                            }
                        )
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                // Silent fail
            }
        })
    }

    private fun processCategories(categoriesData: Any?): List<String> {
        return when (categoriesData) {
            is List<*> -> {
                categoriesData.filterIsInstance<String>()
            }
            is String -> {
                if (categoriesData.contains(",")) {
                    categoriesData.split(",").map { it.trim() }
                } else {
                    listOf(categoriesData.trim())
                }
            }
            else -> emptyList()
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}