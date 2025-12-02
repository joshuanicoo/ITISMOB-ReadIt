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

    // Expose current data for filtering
    val currentData: List<Volume>
        get() = data

    fun setOnItemClickListener(listener: (Volume, Int) -> Unit) {
        onItemClickListener = listener
    }

    fun updateData(newData: List<Volume>) {
        // Handle null case by converting to empty list
        this.data = newData ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val itemViewBinding: BooksCardLayoutBinding = BooksCardLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        return HomeViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        // Check if position is valid
        if (position < data.size) {
            val volume = data[position]
            holder.bindData(volume)
            holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.context, R.anim.anim_one))

            holder.itemView.setOnClickListener {
                if (onItemClickListener != null) {
                    onItemClickListener?.invoke(volume, position)
                } else {
                    openBookDetails(holder, volume, position)
                }
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

                        val intent = Intent(holder.itemView.context, ViewBookActivity::class.java)
                        intent.putExtra(ViewBookActivity.ID_KEY, volume.id)
                        intent.putExtra(ViewBookActivity.TITLE_KEY, volume.volumeInfo.title ?: "")
                        intent.putExtra(ViewBookActivity.AUTHOR_KEY, volume.volumeInfo.authors?.joinToString(", ") ?: "")
                        intent.putExtra(ViewBookActivity.DESCRIPTION_KEY, volume.volumeInfo.description ?: "")
                        intent.putExtra(ViewBookActivity.AVG_RATING_KEY, volume.volumeInfo.averageRating ?: 0.0)
                        intent.putExtra(ViewBookActivity.RATING_COUNT_KEY, volume.volumeInfo.ratingsCount ?: 0)
                        intent.putExtra(ViewBookActivity.GENRE_KEY, genreString)
                        intent.putExtra(ViewBookActivity.POSITION_KEY, position)
                        intent.putExtra(ViewBookActivity.IMAGE_URL, holder.getEnhancedImageUrl(volume))

                        holder.itemView.context.startActivity(intent)
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                // Silent fail - user can try again
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