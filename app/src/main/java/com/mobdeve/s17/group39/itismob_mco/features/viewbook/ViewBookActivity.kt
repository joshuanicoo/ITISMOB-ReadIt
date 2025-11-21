package com.mobdeve.s17.group39.itismob_mco.features.viewbook

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
import com.mobdeve.s17.group39.itismob_mco.database.BooksDatabase
import com.mobdeve.s17.group39.itismob_mco.services.BookUserService
import com.mobdeve.s17.group39.itismob_mco.database.ReviewsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.AddToListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.NewListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.ViewBookActivityBinding
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.review.ReviewDialog
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.genre.GenreAdapter
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.list.AddToListAdapter
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.review.ReviewAdapter
import com.mobdeve.s17.group39.itismob_mco.models.BookModel
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel
import com.mobdeve.s17.group39.itismob_mco.utils.SharedPrefsManager
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
        const val ID_KEY = "ID_KEY"
    }

    private lateinit var viewBookVB: ViewBookActivityBinding
    private lateinit var sharedPrefs: SharedPrefsManager
    private var currentUserDocumentId: String = ""
    private var bookDocumentId: String = ""
    private var googleBooksId: String = ""
    private var isLiked = false
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviewList = ArrayList<ReviewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.viewBookVB = ViewBookActivityBinding.inflate(layoutInflater)
        setContentView(viewBookVB.root)

        sharedPrefs = SharedPrefsManager(this)
        currentUserDocumentId = sharedPrefs.getCurrentUserId() ?: ""

        googleBooksId = intent.getStringExtra(ID_KEY) ?: ""
        bookDocumentId = generateBookDocumentId()

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

        this.viewBookVB.genreRv.adapter = GenreAdapter(dataGenre)
        this.viewBookVB.genreRv.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        reviewAdapter = ReviewAdapter(reviewList)
        reviewAdapter.currentUserId = currentUserDocumentId
        this.viewBookVB.reviewRv.adapter = reviewAdapter
        this.viewBookVB.reviewRv.layoutManager = LinearLayoutManager(this)
        loadReviews()

        initializeLikeState()

        viewBookVB.likeBtn.setOnClickListener {
            handleLikeClick()
        }

        viewBookVB.reviewBtn.setOnClickListener {
            showReviewDialog()
        }

        viewBookVB.addToListBtn.setOnClickListener {
            showAddToListDialog()
        }

        viewBookVB.rateRb.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            if (fromUser && currentUserDocumentId.isNotEmpty()) {
                handleRatingChange(rating.toFloat())
            } else if (fromUser && currentUserDocumentId.isEmpty()) {
                Toast.makeText(this, "Please log in to rate books", Toast.LENGTH_SHORT).show()
                ratingBar.rating = 0f
            }
        }

        reviewAdapter.onReviewLikeClickListener = { review, position ->
            handleReviewLikeClick(review, position)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh reviews data to ensure like states are current
        refreshReviewsData()
    }

    private fun generateBookDocumentId(): String {
        return "book_${googleBooksId}"
    }

    private fun initializeLikeState() {
        if (currentUserDocumentId.isNotEmpty() && bookDocumentId.isNotEmpty()) {
            BookUserService.isBookLikedByUser(currentUserDocumentId, bookDocumentId)
                .addOnSuccessListener { liked ->
                    isLiked = liked
                    updateLikeButtonUI()
                }
                .addOnFailureListener {
                    isLiked = false
                    updateLikeButtonUI()
                }
        } else {
            viewBookVB.likeBtn.isEnabled = currentUserDocumentId.isNotEmpty()
            updateLikeButtonUI()
        }
    }

    private fun handleLikeClick() {
        if (currentUserDocumentId.isEmpty()) {
            Toast.makeText(this, "Please log in to like books", Toast.LENGTH_SHORT).show()
            return
        }

        val googleBooksIdInt = try {
            googleBooksId.toInt()
        } catch (e: NumberFormatException) {
            googleBooksId.hashCode()
        }

        if (isLiked) {
            BookUserService.removeFromIsLiked(currentUserDocumentId, bookDocumentId)
                .addOnSuccessListener {
                    isLiked = false
                    updateLikeButtonUI()
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to remove from favorites", Toast.LENGTH_SHORT).show()
                }
        } else {
            BookUserService.addToIsLiked(currentUserDocumentId, bookDocumentId, googleBooksIdInt)
                .addOnSuccessListener {
                    isLiked = true
                    updateLikeButtonUI()
                    Toast.makeText(this, "Added to favorites!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add to favorites", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateLikeButtonUI() {
        if (isLiked) {
            viewBookVB.likeBtn.setIconResource(R.drawable.ic_heart_on)
        } else {
            viewBookVB.likeBtn.setIconResource(R.drawable.ic_heart_off)
        }

        viewBookVB.likeBtn.isEnabled = currentUserDocumentId.isNotEmpty()
    }

    private fun showReviewDialog() {
        val currentUsername = sharedPrefs.getCurrentUsername()
        val existingUserRating = findUserRating(reviewList)

        ReviewDialog(
            context = this,
            bookTitle = intent.getStringExtra(TITLE_KEY).toString(),
            bookCoverUrl = intent.getStringExtra(IMAGE_URL),
            bookDocumentId = bookDocumentId,
            googleBooksId = googleBooksId,
            currentUserDocumentId = currentUserDocumentId,
            currentUsername = currentUsername,
            existingUserRating = existingUserRating,
            isBookLiked = isLiked,
            onReviewSubmitted = {
                loadReviews()
            },
            onLikeToggled = { newLikeState ->
                handleLikeStateChange(newLikeState)
            }
        ).show()
    }

    private fun handleLikeStateChange(newLikeState: Boolean) {
        if (newLikeState != isLiked) {
            isLiked = newLikeState
            updateLikeButtonUI()

            val googleBooksIdInt = try {
                googleBooksId.toInt()
            } catch (e: NumberFormatException) {
                googleBooksId.hashCode()
            }

            if (newLikeState) {
                BookUserService.addToIsLiked(currentUserDocumentId, bookDocumentId, googleBooksIdInt)
                    .addOnFailureListener { e ->
                        isLiked = !newLikeState
                        updateLikeButtonUI()
                        Toast.makeText(this, "Failed to like book", Toast.LENGTH_SHORT).show()
                    }
            } else {
                BookUserService.removeFromIsLiked(currentUserDocumentId, bookDocumentId)
                    .addOnFailureListener { e ->
                        isLiked = !newLikeState
                        updateLikeButtonUI()
                        Toast.makeText(this, "Failed to unlike book", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun loadReviews() {

        if (bookDocumentId.isNotEmpty()) {
            ReviewsDatabase.getReviewsByBookId(bookDocumentId)
                .addOnSuccessListener { querySnapshot ->

                    val reviews = mutableListOf<ReviewModel>()

                    for (document in querySnapshot.documents) {
                        try {
                            val review = ReviewModel.fromMap(document.id, document.data!!)
                            reviews.add(review)
                        } catch (e: Exception) {
                        }
                    }

                    // Update the rating bar with user's existing rating
                    val userRating = findUserRating(reviews)
                    viewBookVB.rateRb.rating = userRating

                    // Update the adapter
                    reviewList.clear()
                    reviewList.addAll(reviews)
                    reviewAdapter.notifyDataSetChanged()

                    avgRating()

                    // Show/hide empty state
                    if (reviews.isEmpty()) {
                        viewBookVB.reviewRv.visibility = android.view.View.GONE
                        viewBookVB.noReviewsTv.visibility = android.view.View.VISIBLE
                    } else {
                        viewBookVB.reviewRv.visibility = android.view.View.VISIBLE
                        viewBookVB.noReviewsTv.visibility = android.view.View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
                    viewBookVB.reviewRv.visibility = android.view.View.GONE
                    viewBookVB.noReviewsTv.visibility = android.view.View.VISIBLE
                }
        } else {
            viewBookVB.reviewRv.visibility = android.view.View.GONE
            viewBookVB.noReviewsTv.visibility = android.view.View.VISIBLE
        }
    }

    private fun handleRatingChange(newRating: Float) {
        if (currentUserDocumentId.isEmpty()) {
            Toast.makeText(this, "Please log in to rate books", Toast.LENGTH_SHORT).show()
            viewBookVB.rateRb.rating = 0f
            return
        }

        val existingReview = reviewList.firstOrNull { it.userId == currentUserDocumentId }

        if (existingReview != null) {
            updateExistingReviewRating(existingReview, newRating)
        } else {
            createRatingOnlyReview(newRating)
        }
    }

    private fun createRatingOnlyReview(rating: Float) {
        val currentUsername = sharedPrefs.getCurrentUsername() ?: "Anonymous"
        val userProfilePicture = sharedPrefs.getUserProfilePicture()

        val review = ReviewModel(
            bookId = bookDocumentId,
            userId = currentUserDocumentId,
            username = currentUsername,
            userProfilePicture = userProfilePicture,
            rating = rating,
            comment = "(Rated)",
            likes = 0,
            likedBy = emptyList(),
            createdAt = com.google.firebase.Timestamp.now(),
            authorLikedBook = isLiked
        )

        ensureBookExists { bookCreated ->
            if (bookCreated) {
                ReviewsDatabase.create(review)
                    .addOnSuccessListener { documentReference ->
                        val reviewId = documentReference.id
                        updateBookReviews(reviewId)
                        Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show()
                        loadReviews()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                        viewBookVB.rateRb.rating = 0f
                    }
            } else {
                Toast.makeText(this, "Failed to create book entry", Toast.LENGTH_SHORT).show()
                viewBookVB.rateRb.rating = 0f
            }
        }
    }

    private fun updateExistingReviewRating(existingReview: ReviewModel, newRating: Float) {
        val updatedReview = existingReview.copy(rating = newRating)

        ReviewsDatabase.update(existingReview.id, mapOf("rating" to newRating))
            .addOnSuccessListener {
                Toast.makeText(this, "Rating updated!", Toast.LENGTH_SHORT).show()
                val position = reviewList.indexOfFirst { it.id == existingReview.id }
                if (position != -1) {
                    reviewList[position] = updatedReview
                    reviewAdapter.notifyItemChanged(position)
                    avgRating()
                }
            }
    }

    private fun handleReviewLikeClick(review: ReviewModel, position: Int) {

        ReviewsDatabase.getById(review.id)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val currentReview = ReviewModel.fromMap(documentSnapshot.id, documentSnapshot.data!!)
                    val isCurrentlyLiked = currentReview.likedBy.contains(currentUserDocumentId)

                    if (isCurrentlyLiked) {
                        // Unlike the review
                        ReviewsDatabase.unlikeReview(review.id, currentUserDocumentId)
                            .addOnSuccessListener {
                                // Refresh from Firestore to get the actual updated data
                                refreshSingleReview(review.id, position)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to unlike review", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Like the review
                        ReviewsDatabase.likeReview(review.id, currentUserDocumentId)
                            .addOnSuccessListener {
                                // Refresh from Firestore to get the actual updated data
                                refreshSingleReview(review.id, position)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to like review", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to check review status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshSingleReview(reviewId: String, position: Int) {
        ReviewsDatabase.getById(reviewId)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val updatedReview = ReviewModel.fromMap(documentSnapshot.id, documentSnapshot.data!!)

                    if (position in 0 until reviewList.size) {
                        reviewList[position] = updatedReview
                        reviewAdapter.notifyItemChanged(position)
                    }
                }
            }
    }

    private fun ensureBookExists(callback: (Boolean) -> Unit) {
        BooksDatabase.getById(bookDocumentId)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    callback(true)
                } else {
                    createBookInCollection(callback)
                }
            }
            .addOnFailureListener {
                createBookInCollection(callback)
            }
    }

    private fun createBookInCollection(callback: (Boolean) -> Unit) {
        val googleBooksIdInt = try {
            googleBooksId.toInt()
        } catch (e: NumberFormatException) {
            googleBooksId.hashCode()
        }

        val bookModel = BookModel(
            documentId = bookDocumentId,
            bookId = googleBooksIdInt,
            likedBy = emptyList(),
            reviews = emptyList()
        )

        BooksDatabase.createWithId(bookDocumentId, bookModel)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                callback(false)
            }
    }

    private fun updateBookReviews(reviewId: String) {
        val updates = mapOf(
            "reviews" to com.google.firebase.firestore.FieldValue.arrayUnion(reviewId)
        )
        BooksDatabase.update(bookDocumentId, updates)
    }

    private fun findUserRating(reviews: List<ReviewModel>): Float {
        return reviews
            .firstOrNull { it.userId == currentUserDocumentId }
            ?.rating ?: 0f
    }

    private fun avgRating() {
        if (reviewList.isEmpty()) {
            viewBookVB.avgRatingRb.rating = 0f
            viewBookVB.numberOfRatingsTv.text = "0 ratings"
            return
        }

        // Filter out reviews with 0 rating
        val ratedReviews = reviewList.filter { it.rating > 0 }

        if (ratedReviews.isEmpty()) {
            viewBookVB.avgRatingRb.rating = 0f
            viewBookVB.numberOfRatingsTv.text = "0 ratings"
            return
        }

        val totalRating = ratedReviews.sumOf { it.rating.toDouble() }
        val averageRating = totalRating / ratedReviews.size

        viewBookVB.avgRatingRb.rating = averageRating.toFloat()
        viewBookVB.numberOfRatingsTv.text = ratedReviews.size.toString()
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

    private fun refreshReviewsData() {
        if (bookDocumentId.isNotEmpty()) {
            ReviewsDatabase.getReviewsByBookId(bookDocumentId)
                .addOnSuccessListener { querySnapshot ->
                    val reviews = mutableListOf<ReviewModel>()
                    for (document in querySnapshot.documents) {
                        try {
                            val review = ReviewModel.fromMap(document.id, document.data!!)
                            reviews.add(review)
                        } catch (e: Exception) {
                        }
                    }

                    reviewList.clear()
                    reviewList.addAll(reviews)
                    reviewAdapter.notifyDataSetChanged()
                }
        }
    }
}