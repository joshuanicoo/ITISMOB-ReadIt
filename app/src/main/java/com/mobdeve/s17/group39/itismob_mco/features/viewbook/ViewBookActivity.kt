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
import com.google.firebase.auth.FirebaseAuth
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.BooksDatabase
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.database.ReviewsDatabase
import com.mobdeve.s17.group39.itismob_mco.database.UsersDatabase
import com.mobdeve.s17.group39.itismob_mco.services.BookUserService
import com.mobdeve.s17.group39.itismob_mco.databinding.NewListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.ViewBookActivityBinding
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.review.ReviewDialog
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.genre.GenreAdapter
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.list.AddToListDialog
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.review.ReviewAdapter
import com.mobdeve.s17.group39.itismob_mco.models.BookModel
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel
import com.mobdeve.s17.group39.itismob_mco.models.UserModel
import com.mobdeve.s17.group39.itismob_mco.utils.LoadingUtils
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

    private lateinit var binding: ViewBookActivityBinding
    private lateinit var auth: FirebaseAuth
    private var userId: String = ""
    private var bookDocId: String = ""
    private var googleBooksId: String = ""
    private var isLiked = false
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviews = ArrayList<ReviewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ViewBookActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLoading()

        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""

        // Get book details from the previous screen
        googleBooksId = intent.getStringExtra(ID_KEY) ?: ""
        bookDocId = "book_${googleBooksId}" // Our custom ID format

        val title = intent.getStringExtra(TITLE_KEY).toString()
        val author = intent.getStringExtra(AUTHOR_KEY).toString()
        val description = intent.getStringExtra(DESCRIPTION_KEY).toString()
        val avgRating = intent.getDoubleExtra(AVG_RATING_KEY, 0.0)
        val ratingCount = intent.getIntExtra(RATING_COUNT_KEY, 0)
        val genre = intent.getStringExtra(GENRE_KEY)
        val imageUrl = intent.getStringExtra(IMAGE_URL)

        // Set up the basic book info
        binding.titleTv.text = title
        binding.authorTv.text = author
        binding.descriptionTv.text = description
        binding.avgRatingRb.rating = avgRating.toFloat()
        binding.numberOfRatingsTv.text = ratingCount.toString()

        // Load book cover and banner images
        loadImages(imageUrl)

        // Set up genre tags
        setupGenres(genre)

        // Set up the reviews list
        setupReviewsList()

        // Load reviews and check if user already liked this book
        loadReviews()
        checkIfLiked()

        // Set up clickables
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        // Refresh reviews when coming back to this screen
        refreshReviews()
    }

    private fun showLoading() {
        LoadingUtils.showLoading(
            binding.loadingContainer,
            binding.root.findViewById(R.id.mainContentContainer),
            binding.quoteText,
            binding.quoteAuthor
        )
    }

    private fun hideLoading() {
        LoadingUtils.hideLoading(
            binding.loadingContainer,
            binding.root.findViewById(R.id.mainContentContainer)
        )
    }

    private fun loadImages(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            // Load the book cover
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.book_placeholder)
                .error(R.drawable.book_placeholder)
                .centerCrop()
                .into(binding.coverIv)

            // Load the blurred banner background
            Glide.with(this)
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .transform(
                            CenterCrop(),
                            BlurTransformation(25, 3),
                            ColorFilterTransformation(Color.parseColor("#66000000"))
                        )
                        .placeholder(android.R.color.transparent)
                        .error(R.drawable.book_placeholder)
                )
                .into(binding.bannerIv)
        }
    }

    private fun setupGenres(genreString: String?) {
        val genres = ArrayList<String>()

        if (!genreString.isNullOrEmpty()) {
            // Split categories by comma (e.g., "Fiction, Science, Adventure")
            if (genreString.contains(",")) {
                val genreList = genreString.split(",").map { it.trim() }
                genres.addAll(genreList)
            } else {
                // Single category
                genres.add(genreString.trim())
            }
        }

        binding.genreRv.adapter = GenreAdapter(genres)
        binding.genreRv.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    private fun setupReviewsList() {
        reviewAdapter = ReviewAdapter(reviews)
        reviewAdapter.currentUserId = userId
        binding.reviewRv.adapter = reviewAdapter
        binding.reviewRv.layoutManager = LinearLayoutManager(this)

        // Handle when someone likes a review
        reviewAdapter.onReviewLikeClickListener = { review, position ->
            handleReviewLike(review, position)
        }
    }

    private fun checkIfLiked() {
        // Check if the current user has already liked this book
        binding.root.postDelayed({
            if (userId.isNotEmpty() && bookDocId.isNotEmpty()) {
                BookUserService.isBookLikedByUser(userId, bookDocId)
                    .addOnSuccessListener { liked ->
                        isLiked = liked
                        updateLikeButton()
                    }
                    .addOnFailureListener {
                        isLiked = false
                        updateLikeButton()
                    }
            } else {
                binding.likeBtn.isEnabled = userId.isNotEmpty()
                updateLikeButton()
            }
        }, 500)
    }

    private fun setupClicks() {
        // Like button - add/remove from favorites
        binding.likeBtn.setOnClickListener {
            handleLike()
        }

        // Review button - open review dialog
        binding.reviewBtn.setOnClickListener {
            showReviewDialog()
        }

        // Add to list button - save to custom reading lists
        binding.addToListBtn.setOnClickListener {
            showAddToListDialog()
        }

        // Star rating - quick rating without writing review
        binding.rateRb.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            if (fromUser && userId.isNotEmpty()) {
                handleRating(rating.toFloat())
            }
        }
    }

    private fun handleLike() {

        if (isLiked) {
            // Remove from favorites
            BookUserService.removeFromIsLiked(userId, bookDocId)
                .addOnSuccessListener {
                    isLiked = false
                    updateLikeButton()
                    updateReviewLikeStatus()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Add to favorites
            BookUserService.addToIsLiked(userId, bookDocId, googleBooksId)
                .addOnSuccessListener {
                    isLiked = true
                    updateLikeButton()
                    updateReviewLikeStatus() // Also update any existing review
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateLikeButton() {
        if (isLiked) {
            binding.likeBtn.setIconResource(R.drawable.ic_heart_on)
        } else {
            binding.likeBtn.setIconResource(R.drawable.ic_heart_off)
        }
        binding.likeBtn.isEnabled = userId.isNotEmpty()
    }

    private fun showReviewDialog() {
        val userRating = findUserRating()

        ReviewDialog(
            context = this,
            bookTitle = intent.getStringExtra(TITLE_KEY).toString(),
            bookCoverUrl = intent.getStringExtra(IMAGE_URL),
            bookDocumentId = bookDocId,
            googleBooksId = googleBooksId,
            currentUserDocumentId = userId,
            existingUserRating = userRating,
            isBookLiked = isLiked,
            onReviewSubmitted = {
                loadReviews() // Refresh after submitting
            },
            onLikeToggled = { newLike ->
                handleLikeToggle(newLike)
            }
        ).show()
    }

    private fun handleLikeToggle(newLike: Boolean) {
        if (newLike != isLiked) {
            isLiked = newLike
            updateLikeButton()
            updateReviewLikeStatus()

            if (newLike) {
                BookUserService.addToIsLiked(userId, bookDocId, googleBooksId)
                    .addOnFailureListener { e ->
                        isLiked = !newLike
                        updateLikeButton()
                        Toast.makeText(this, "Failed to like", Toast.LENGTH_SHORT).show()
                    }
            } else {
                BookUserService.removeFromIsLiked(userId, bookDocId)
                    .addOnFailureListener { e ->
                        isLiked = !newLike
                        updateLikeButton()
                        Toast.makeText(this, "Failed to unlike", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun loadReviews() {
        if (bookDocId.isEmpty()) {
            showNoReviews()
            return
        }

        ReviewsDatabase.getReviewsByBookId(bookDocId)
            .addOnSuccessListener { snapshot ->
                val loadedReviews = mutableListOf<ReviewModel>()
                val userIds = mutableSetOf<String>()

                // Get all reviews for this book
                for (doc in snapshot.documents) {
                    try {
                        val review = ReviewModel.fromMap(doc.id, doc.data!!)
                        loadedReviews.add(review)
                        userIds.add(review.userId) // Collect user IDs for fetching profile data
                    } catch (e: Exception) {
                        // Skip bad data
                    }
                }

                // Update user's rating display
                val userRating = loadedReviews.firstOrNull { it.userId == userId }?.rating ?: 0f
                binding.rateRb.rating = userRating

                // Fetch user profile pictures and names
                fetchUserData(loadedReviews, userIds)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
                showNoReviews()
            }
    }

    private fun fetchUserData(reviews: List<ReviewModel>, userIds: Set<String>) {
        val userData = mutableMapOf<String, Pair<String, String?>>()
        var fetchedCount = 0

        if (userIds.isEmpty()) {
            updateReviewsUI(reviews, userData)
            return
        }

        // Fetch each user's profile data
        userIds.forEach { userId ->
            UsersDatabase.getById(userId)
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val user = UserModel.fromMap(doc.id, doc.data ?: emptyMap())
                        userData[userId] = Pair(
                            user.username ?: "Anonymous",
                            user.profilePicture
                        )
                    } else {
                        userData[userId] = Pair("Anonymous", null)
                    }

                    fetchedCount++
                    if (fetchedCount == userIds.size) {
                        updateReviewsUI(reviews, userData)
                    }
                }
                .addOnFailureListener {
                    userData[userId] = Pair("Anonymous", null)
                    fetchedCount++
                    if (fetchedCount == userIds.size) {
                        updateReviewsUI(reviews, userData)
                    }
                }
        }
    }

    private fun updateReviewsUI(reviews: List<ReviewModel>, userData: Map<String, Pair<String, String?>>) {
        // Add user profile data to reviews
        val reviewsWithUserData = reviews.map { review ->
            val (username, profilePic) = userData[review.userId] ?: Pair("Anonymous", null)
            review.copy(
                username = username,
                userProfilePicture = profilePic
            )
        }

        // Update the list
        this.reviews.clear()
        this.reviews.addAll(reviewsWithUserData)
        reviewAdapter.notifyDataSetChanged()

        // Update average rating display
        calculateAvgRating()

        // Show/hide empty state
        if (reviewAdapter.hasReviewsWithComments()) {
            binding.reviewRv.visibility = android.view.View.VISIBLE
            binding.noReviewsTv.visibility = android.view.View.GONE
        } else {
            showNoReviews()
        }

        hideLoading()
    }

    private fun showNoReviews() {
        binding.reviewRv.visibility = android.view.View.GONE
        binding.noReviewsTv.visibility = android.view.View.VISIBLE
        hideLoading()
    }

    private fun handleRating(newRating: Float) {
        val existing = reviews.firstOrNull { it.userId == userId }

        if (existing != null) {
            updateRating(existing, newRating)
        } else {
            createRating(newRating)
        }
    }

    private fun createRating(rating: Float) {
        val review = ReviewModel(
            bookId = bookDocId,
            userId = userId,
            rating = rating,
            comment = "", // Empty comment = rating only
            likes = 0,
            likedBy = emptyList(),
            createdAt = com.google.firebase.Timestamp.now(),
            authorLikedBook = isLiked
        )

        // Make sure book exists in database
        ensureBookExists { created ->
            if (created) {
                ReviewsDatabase.create(review)
                    .addOnSuccessListener { docRef ->
                        val reviewId = docRef.id
                        addReviewToBook(reviewId)
                        loadReviews()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                        binding.rateRb.rating = 0f
                    }
            } else {
                Toast.makeText(this, "Error creating book entry", Toast.LENGTH_SHORT).show()
                binding.rateRb.rating = 0f
            }
        }
    }

    private fun updateRating(existing: ReviewModel, newRating: Float) {
        ReviewsDatabase.update(existing.id, mapOf("rating" to newRating))
            .addOnSuccessListener {
                val pos = reviews.indexOfFirst { it.id == existing.id }
                if (pos != -1) {
                    reviews[pos] = reviews[pos].copy(rating = newRating)
                    reviewAdapter.notifyItemChanged(pos)
                    calculateAvgRating()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateReviewLikeStatus() {
        // If user has a review, update its "author liked book" status
        val existing = reviews.firstOrNull { it.userId == userId }
        if (existing != null) {
            ReviewsDatabase.update(existing.id, mapOf("authorLikedBook" to isLiked))
                .addOnSuccessListener {
                    val pos = reviews.indexOfFirst { it.id == existing.id }
                    if (pos != -1) {
                        reviews[pos] = reviews[pos].copy(authorLikedBook = isLiked)
                        reviewAdapter.notifyItemChanged(pos)
                    }
                }
        }
    }

    private fun handleReviewLike(review: ReviewModel, position: Int) {
        val alreadyLiked = review.likedBy.contains(userId)

        if (alreadyLiked) {
            // Unlike this review
            ReviewsDatabase.unlikeReview(review.id, userId)
                .addOnSuccessListener {
                    refreshReview(review.id, position)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to unlike", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Like this review
            ReviewsDatabase.likeReview(review.id, userId)
                .addOnSuccessListener {
                    refreshReview(review.id, position)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to like", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun refreshReview(reviewId: String, position: Int) {
        // Get fresh data from server after like/unlike
        ReviewsDatabase.getById(reviewId)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val updated = ReviewModel.fromMap(doc.id, doc.data!!)
                    fetchUserForReview(updated, position)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to refresh", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserForReview(review: ReviewModel, position: Int) {
        UsersDatabase.getById(review.userId)
            .addOnSuccessListener { doc ->
                val updatedReview = if (doc.exists()) {
                    val user = UserModel.fromMap(doc.id, doc.data ?: emptyMap())
                    review.copy(
                        username = user.username ?: "Anonymous",
                        userProfilePicture = user.profilePicture
                    )
                } else {
                    review.copy(username = "Anonymous")
                }

                if (position in 0 until reviews.size) {
                    reviews[position] = updatedReview
                    reviewAdapter.notifyItemChanged(position)
                }
            }
            .addOnFailureListener {
                val updatedReview = review.copy(username = "Anonymous")
                if (position in 0 until reviews.size) {
                    reviews[position] = updatedReview
                    reviewAdapter.notifyItemChanged(position)
                }
            }
    }

    private fun ensureBookExists(callback: (Boolean) -> Unit) {
        BooksDatabase.getById(bookDocId)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    callback(true)
                } else {
                    createBook(callback)
                }
            }
            .addOnFailureListener {
                createBook(callback)
            }
    }

    private fun createBook(callback: (Boolean) -> Unit) {
        val book = BookModel(
            documentId = bookDocId,
            bookId = googleBooksId,
            likedBy = emptyList(),
            reviews = emptyList()
        )

        BooksDatabase.createWithId(bookDocId, book)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { e ->
                callback(false)
            }
    }

    private fun addReviewToBook(reviewId: String) {
        BooksDatabase.update(bookDocId, mapOf(
            "reviews" to com.google.firebase.firestore.FieldValue.arrayUnion(reviewId)
        ))
    }

    private fun findUserRating(): Float {
        return reviews.firstOrNull { it.userId == userId }?.rating ?: 0f
    }

    private fun calculateAvgRating() {
        if (reviews.isEmpty()) {
            binding.avgRatingRb.rating = 0f
            binding.numberOfRatingsTv.text = "0 ratings"
            return
        }

        // Only count reviews with actual ratings (not 0)
        val rated = reviews.filter { it.rating > 0 }

        if (rated.isEmpty()) {
            binding.avgRatingRb.rating = 0f
            binding.numberOfRatingsTv.text = "0 ratings"
            return
        }

        val total = rated.sumOf { it.rating.toDouble() }
        val average = total / rated.size

        binding.avgRatingRb.rating = average.toFloat()
        binding.numberOfRatingsTv.text = rated.size.toString()
    }

    private fun showAddToListDialog() {
        AddToListDialog(
            context = this,
            bookId = bookDocId,
            onNewListRequested = {
                showCreateListDialog()
            }
        ).show()
    }

    private fun showCreateListDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        val binding = NewListLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (200 * resources.displayMetrics.density).toInt()
        )

        binding.saveNewListBtn.setOnClickListener {
            val name = binding.newListNameEt.text.toString().trim()
            if (name.isNotEmpty()) {
                createList(name, dialog)
            } else {
                Toast.makeText(this, "Enter a list name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelNewListBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createList(name: String, dialog: Dialog) {
        val currentId = auth.currentUser?.uid ?: ""

        ListsDatabase.createList(name, currentId)
            .addOnSuccessListener {
                dialog.dismiss()
                showAddToListDialog() // Go back to add to list dialog
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshReviews() {
        if (bookDocId.isNotEmpty()) {
            ReviewsDatabase.getReviewsByBookId(bookDocId)
                .addOnSuccessListener { snapshot ->
                    val loadedReviews = mutableListOf<ReviewModel>()
                    val userIds = mutableSetOf<String>()

                    for (doc in snapshot.documents) {
                        try {
                            val review = ReviewModel.fromMap(doc.id, doc.data!!)
                            loadedReviews.add(review)
                            userIds.add(review.userId)
                        } catch (e: Exception) {
                            // Skip bad data
                        }
                    }

                    fetchUserData(loadedReviews, userIds)
                }
        }
    }
}