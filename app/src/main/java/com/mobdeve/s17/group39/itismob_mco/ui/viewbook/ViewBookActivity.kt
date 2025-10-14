package com.mobdeve.s17.group39.itismob_mco.ui.viewbook

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.databinding.ViewBookActivityBinding

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

        Glide.with(this.applicationContext)
            .load(imageUrl)
            .placeholder(R.drawable.content)
            .error(R.drawable.content)
            .centerCrop()
            .into(this.viewBookVB.coverIv)

        val dataGenre = ArrayList<String>()
        if (!genreString.isNullOrEmpty()) {
            val genres = genreString.split(",").map { it.trim() }
            dataGenre.addAll(genres)
        }

        this.viewBookVB.genreRv.adapter = GenreAdapter(dataGenre)
        this.viewBookVB.genreRv.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

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

        var isAdded = false
        viewBookVB.addToListBtn.setOnClickListener {
            isAdded = !isAdded
            if (isAdded) {
                viewBookVB.addToListBtn.setIconResource(R.drawable.ic_add_on)
            } else {
                viewBookVB.addToListBtn.setIconResource(R.drawable.ic_add_off)
            }
        }



    }

    private fun generateReviews(): ArrayList<ReviewModel> {
        val tempData = ArrayList<ReviewModel>()

        tempData.add(ReviewModel(
            userPfpResId = R.drawable.user_pfp_1,
            username = "Kasane Teto",
            userRating = 4.5f,
            reviewBody = "Absolutely loved this book! Couldn't put it down.",
            isLikedByCurrentUser = true,
            likesCount = 67
        ))

        tempData.add(ReviewModel(
            userPfpResId = R.drawable.user_pfp_2,
            username = "BookWorm42",
            userRating = 3.0f,
            reviewBody = "Good premise but slow pacing in the middle chapters.",
            isLikedByCurrentUser = false,
            likesCount = 23
        ))

        tempData.add(ReviewModel(
            userPfpResId = R.drawable.user_pfp_3,
            username = "LiteraryExplorer",
            userRating = 5.0f,
            reviewBody = "Masterpiece! The character development was incredible.",
            isLikedByCurrentUser = true,
            likesCount = 89
        ))

        tempData.add(ReviewModel(
            userPfpResId = R.drawable.user_pfp_4,
            username = "CriticalReader",
            userRating = 2.5f,
            reviewBody = "Interesting concept but poor execution.",
            isLikedByCurrentUser = false,
            likesCount = 12
        ))

        tempData.add(ReviewModel(
            userPfpResId = R.drawable.user_pfp_5,
            username = "PageTurner",
            userRating = 4.0f,
            reviewBody = "Great weekend read, highly recommend!",
            isLikedByCurrentUser = true,
            likesCount = 45
        ))

        return tempData
    }

}