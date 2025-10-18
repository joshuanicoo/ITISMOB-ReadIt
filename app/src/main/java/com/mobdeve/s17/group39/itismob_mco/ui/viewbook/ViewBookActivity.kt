package com.mobdeve.s17.group39.itismob_mco.ui.viewbook

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color

import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.AddToListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.NewListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewBookLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.ViewBookActivityBinding
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.genre.GenreAdapter
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.list.AddToListAdapter
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.review.ReviewAdapter
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.review.ReviewModel
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.ColorFilterTransformation

class ViewBookActivity : AppCompatActivity() {
    companion object {
        const val TITLE_KEY = "TITLE_KEY"
        const val AUTHOR_KEY = "AUTHOR_KEY"
        const val DESCRIPTION_KEY = "DESCRIPTION_KEY"
        const val AVG_RATING_KEY = "AVG_RATING_KEY"
        const val RATING_COUNT_KEY = "RATING_COUNT_KEY"
        const val POSITION_KEY = "POSITION_KEY"
        const val GENRE_KEY = "GENRE_KEY"
        const val IMAGE_URL = "IMAGE_URL"
    }

    private lateinit var viewBookVB: ViewBookActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        this.viewBookVB = ViewBookActivityBinding.inflate(layoutInflater)
        setContentView(viewBookVB.root)


        val titleString = intent.getStringExtra(TITLE_KEY).toString()
        val authorString = intent.getStringExtra(AUTHOR_KEY).toString()
        val descriptionString = intent.getStringExtra(DESCRIPTION_KEY).toString()
        val avgRatingDouble = intent.getDoubleExtra(AVG_RATING_KEY, 0.0)
        val ratingCountInt = intent.getIntExtra(RATING_COUNT_KEY, 0)
        val genreString = intent.getStringExtra(GENRE_KEY)
        val position = intent.getIntExtra(POSITION_KEY, -1)
        val imageUrl = intent.getStringExtra(IMAGE_URL)

        viewBookVB.titleTv.text = titleString
        viewBookVB.authorTv.text = authorString
        viewBookVB.descriptionTv.text = descriptionString
        viewBookVB.avgRatingRb.rating = avgRatingDouble.toFloat()
        viewBookVB.numberOfRatingsTv.text = ratingCountInt.toString()

        // For fetching book cover
        Glide.with(this.applicationContext)
            .load(imageUrl)
            .placeholder(R.drawable.content)
            .error(R.drawable.content)
            .centerCrop()
            .into(this.viewBookVB.coverIv)

        // Making a book banner using the cover
        Glide.with(this.applicationContext)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .transform(
                        CenterCrop(),
                        BlurTransformation(25, 3),
                        ColorFilterTransformation(Color.parseColor("#66000000"))
                    )
                    .placeholder(R.drawable.content)
                    .error(R.drawable.content)
            )
            .into(this.viewBookVB.bannerIv)

        val dataGenre = ArrayList<String>()
        if (!genreString.isNullOrEmpty()) {
            val genres = genreString.split(",").map { it.trim() }
            dataGenre.addAll(genres)
        }

        // Recycler view for genres
        this.viewBookVB.genreRv.adapter = GenreAdapter(dataGenre)
        this.viewBookVB.genreRv.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // Recycler view for reviews
        val dataReviews = generateReviews()
        this.viewBookVB.reviewRv.adapter = ReviewAdapter(dataReviews)
        this.viewBookVB.reviewRv.layoutManager = LinearLayoutManager(this)


        var isLiked = false
        viewBookVB.likeBtn.setOnClickListener {
            isLiked = !isLiked
            if (isLiked) {
                viewBookVB.likeBtn.setIconResource(R.drawable.ic_heart_on)
            } else {
                viewBookVB.likeBtn.setIconResource(R.drawable.ic_heart_off)
            }
        }

        viewBookVB.reviewBtn.setOnClickListener {
            showReviewDialog()
        }

        viewBookVB.addToListBtn.setOnClickListener {
            showAddToListDialog()
        }
    }

    fun showReviewDialog() {
        val dialog = Dialog(this@ViewBookActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        val binding = ReviewBookLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val titleString = intent.getStringExtra(TITLE_KEY).toString()
        val imageUrl = intent.getStringExtra(IMAGE_URL)

        binding.bookTitleDialogTv.text = titleString
        Glide.with(this.applicationContext)
            .load(imageUrl)
            .placeholder(R.drawable.content)
            .error(R.drawable.content)
            .centerCrop()
            .into(binding.bookCoverDialogIv)

        var isLiked = false
        binding.likeDialogBtn.setOnClickListener {
            isLiked = !isLiked
            if (isLiked) {
                binding.likeDialogBtn.setImageResource(R.drawable.ic_heart_on)
            } else {
                binding.likeDialogBtn.setImageResource(R.drawable.ic_heart_off)
            }
        }

        binding.saveReviewBtn.setOnClickListener {
            Toast.makeText(this@ViewBookActivity, "Review added to your diary", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        binding.cancelReviewBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showAddToListDialog() {
        val dialog = Dialog(this@ViewBookActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        val binding = AddToListLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val dataLists = arrayListOf(
            "Currently Reading",
            "Want to Read",
            "Read",
            "Favorites",
            "To Buy",
            "Summer Reading",
            "Classics",
            "Non-Fiction"
        )

        binding.addToListRv.adapter = AddToListAdapter(dataLists)
        binding.addToListRv.layoutManager = LinearLayoutManager(this)

        binding.newListBtn.setOnClickListener {
            dialog.dismiss()
            showNewListDialog()
        }

        binding.addToListDialogBtn.setOnClickListener {
            Toast.makeText(this@ViewBookActivity, "Successfully added to list", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        binding.cancelAddToListBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showNewListDialog() {
        val dialog = Dialog(this@ViewBookActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        val binding = NewListLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        val heightInPixels = (200 * resources.displayMetrics.density).toInt()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, heightInPixels)


        binding.saveNewListBtn.setOnClickListener {
            Toast.makeText(this@ViewBookActivity, "Successfully created list", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        binding.cancelNewListBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun generateReviews(): ArrayList<ReviewModel> {
        val tempData = ArrayList<ReviewModel>()

        tempData.add(
            ReviewModel(
                userPfpResId = R.drawable.user_pfp_1,
                username = "Kasane Teto",
                userRating = 4.5f,
                reviewBody = "Absolutely loved this book! Couldn't put it down.",
                isLikedByCurrentUser = true,
                likesCount = 67
            )
        )

        tempData.add(
            ReviewModel(
                userPfpResId = R.drawable.user_pfp_2,
                username = "BookWorm42",
                userRating = 3.0f,
                reviewBody = "Good premise but slow pacing in the middle chapters.",
                isLikedByCurrentUser = false,
                likesCount = 23
            )
        )

        tempData.add(
            ReviewModel(
                userPfpResId = R.drawable.user_pfp_3,
                username = "LiteraryExplorer",
                userRating = 5.0f,
                reviewBody = "Masterpiece! The character development was incredible.",
                isLikedByCurrentUser = true,
                likesCount = 89
            )
        )

        tempData.add(
            ReviewModel(
                userPfpResId = R.drawable.user_pfp_4,
                username = "CriticalReader",
                userRating = 2.5f,
                reviewBody = "Interesting concept but poor execution.",
                isLikedByCurrentUser = false,
                likesCount = 12
            )
        )

        tempData.add(
            ReviewModel(
                userPfpResId = R.drawable.user_pfp_5,
                username = "PageTurner",
                userRating = 4.0f,
                reviewBody = "Great weekend read, highly recommend!",
                isLikedByCurrentUser = true,
                likesCount = 45
            )
        )

        return tempData
    }

}