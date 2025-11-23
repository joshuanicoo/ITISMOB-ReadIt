package com.mobdeve.s17.group39.itismob_mco.features.viewbook.review

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.BooksDatabase
import com.mobdeve.s17.group39.itismob_mco.database.ReviewsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewBookLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.BookModel
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel

class ReviewDialog(
    private val context: Context,
    private val bookTitle: String,
    private val bookCoverUrl: String?,
    private val bookDocumentId: String,
    private val currentUserDocumentId: String,
    private val currentUsername: String?,
    private val googleBooksId: String,
    private val isBookLiked: Boolean,
    private val onReviewSubmitted: () -> Unit,
    private val onLikeToggled: (Boolean) -> Unit,
    private val existingUserRating: Float
) {

    private lateinit var dialog: Dialog
    private lateinit var binding: ReviewBookLayoutBinding
    private var isLiked = isBookLiked
    private val auth = FirebaseAuth.getInstance()

    // Displays the Dialog
    fun show() {
        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        binding = ReviewBookLayoutBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(binding.root)
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)

        setupViews()
        setupClickListeners()

        dialog.show()
    }

    private fun setupViews() {
        binding.bookTitleDialogTv.text = bookTitle

        // Load book cover
        Glide.with(context)
            .load(bookCoverUrl)
            .placeholder(R.drawable.book_placeholder)
            .error(R.drawable.book_placeholder)
            .centerCrop()
            .into(binding.bookCoverDialogIv)

        // Pre-fill with existing review data if user has already reviewed
        checkExistingReview { existingReview ->
            if (existingReview != null) {
                // User already has a review, pre-fill the dialog
                binding.rateDialogRb.rating = existingReview.rating
                binding.reviewBodyEt.setText(existingReview.comment)
                binding.reviewBodyEt.setSelection(existingReview.comment.length) // Move cursor to end
            } else {
                // No existing review, use the passed rating
                binding.rateDialogRb.rating = existingUserRating
            }
        }

        // Remove like button functionality from review dialog
        binding.likeDialogBtn.visibility = android.view.View.GONE
        binding.likeDialogHeaderTv.visibility = android.view.View.GONE
    }

    private fun setupClickListeners() {
        binding.likeDialogBtn.setOnClickListener {
            toggleLike()
        }

        binding.saveReviewBtn.setOnClickListener {
            submitReview()
        }

        binding.cancelReviewBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun toggleLike() {
        isLiked = !isLiked
        updateLikeButtonUI()

        // Notify the activity about the like state change
        onLikeToggled(isLiked)
    }

    private fun updateLikeButtonUI() {
        if (isLiked) {
            binding.likeDialogBtn.setImageResource(R.drawable.ic_heart_on)
        } else {
            binding.likeDialogBtn.setImageResource(R.drawable.ic_heart_off)
        }
    }

    private fun submitReview() {
        val rating = binding.rateDialogRb.rating
        val comment = binding.reviewBodyEt.text.toString().trim()

        if (rating == 0f) {
            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        if (comment.isEmpty()) {
            Toast.makeText(context, "Please write a review comment", Toast.LENGTH_SHORT).show()
            return
        }

        // Always update or create the user's single review
        createOrUpdateReview(rating, comment)
    }

    private fun createOrUpdateReview(rating: Float, comment: String) {
        // Check if user already has a review for this book
        checkExistingReview { existingReview ->
            if (existingReview != null) {
                // Update existing review with new rating and comment
                updateExistingReview(existingReview, rating, comment)
            } else {
                // Create new review (user's first review for this book)
                createNewReview(rating, comment)
            }
        }
    }

    private fun updateExistingReview(existingReview: ReviewModel, newRating: Float, newComment: String) {
        val updates = mapOf(
            "rating" to newRating,
            "comment" to newComment,
            "authorLikedBook" to isLiked
        )

        ReviewsDatabase.update(existingReview.id, updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Review updated successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onReviewSubmitted()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update review: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewReview(rating: Float, comment: String) {
        // Get current user from Firebase Auth
        val currentUser = auth.currentUser
        val userProfilePicture = currentUser?.photoUrl?.toString()

        val review = ReviewModel(
            bookId = bookDocumentId,
            userId = currentUserDocumentId,
            username = currentUsername ?: "Anonymous",
            userProfilePicture = userProfilePicture,
            rating = rating,
            comment = comment,
            likes = 0,
            likedBy = emptyList(),
            createdAt = Timestamp.now(),
            authorLikedBook = isLiked
        )

        // Ensure the book exists in the books collection
        ensureBookExists { bookCreated ->
            if (bookCreated) {
                ReviewsDatabase.create(review)
                    .addOnSuccessListener { documentReference ->
                        val reviewId = documentReference.id
                        updateBookReviews(reviewId)
                        Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        onReviewSubmitted()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to submit review: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "Failed to create book entry", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkExistingReview(callback: (ReviewModel?) -> Unit) {
        ReviewsDatabase.getReviewsByBookId(bookDocumentId)
            .addOnSuccessListener { querySnapshot ->
                val existingReview = querySnapshot.documents
                    .map { document ->
                        try {
                            ReviewModel.fromMap(document.id, document.data!!)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .filterNotNull()
                    .firstOrNull { it.userId == currentUserDocumentId }
                callback(existingReview)
            }
            .addOnFailureListener { e ->
                callback(null)
            }
    }

    private fun ensureBookExists(callback: (Boolean) -> Unit) {
        // Check if book already exists
        BooksDatabase.getById(bookDocumentId)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Book already exists
                    callback(true)
                } else {
                    // Book doesn't exist, create it
                    createBookInCollection(callback)
                }
            }
    }

    private fun createBookInCollection(callback: (Boolean) -> Unit) {
        val bookModel = BookModel(
            documentId = bookDocumentId,
            bookId = googleBooksId,
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
}