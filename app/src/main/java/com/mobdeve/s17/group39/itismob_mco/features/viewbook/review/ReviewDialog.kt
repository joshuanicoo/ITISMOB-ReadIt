package com.mobdeve.s17.group39.itismob_mco.features.viewbook.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Toast
import com.bumptech.glide.Glide
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.BooksDatabase
import com.mobdeve.s17.group39.itismob_mco.database.ReviewsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.ReviewBookLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel
import com.google.firebase.Timestamp

class ReviewDialog(
    private val context: Context,
    private val bookTitle: String,
    private val bookCoverUrl: String?,
    private val bookDocumentId: String,
    private val currentUserDocumentId: String,
    private val currentUsername: String?,
    private val isBookLiked: Boolean,
    private val onReviewSubmitted: () -> Unit,
    private val onLikeToggled: (Boolean) -> Unit
) {

    private lateinit var dialog: Dialog
    private lateinit var binding: ReviewBookLayoutBinding
    private var isLiked = isBookLiked

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
            .placeholder(R.drawable.content)
            .error(R.drawable.content)
            .centerCrop()
            .into(binding.bookCoverDialogIv)

        // Set initial like button state
        updateLikeButtonUI()
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
        if (currentUserDocumentId.isEmpty()) {
            Toast.makeText(context, "Please log in to like books", Toast.LENGTH_SHORT).show()
            return
        }

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
        // Use the correct view IDs from your layout
        val rating = binding.rateDialogRb.rating.toInt()
        val comment = binding.reviewBodyEt.text.toString().trim()

        // Validate input
        if (rating == 0) {
            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        if (comment.isEmpty()) {
            Toast.makeText(context, "Please write a review comment", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserDocumentId.isEmpty()) {
            Toast.makeText(context, "Please log in to submit a review", Toast.LENGTH_SHORT).show()
            return
        }

        createReview(rating, comment)
    }

    private fun createReview(rating: Int, comment: String) {
        val review = ReviewModel(
            bookId = bookDocumentId,
            userId = currentUserDocumentId,
            username = currentUsername ?: "Anonymous",
            userProfilePicture = null,
            rating = rating,
            comment = comment,
            likes = 0,
            likedBy = emptyList(),
            createdAt = Timestamp.now()
        )

        ReviewsDatabase.create(review)
            .addOnSuccessListener { documentReference ->
                val reviewId = documentReference.id
                updateBookReviews(reviewId)

                Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onReviewSubmitted() // Callback to refresh reviews in activity
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to submit review: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBookReviews(reviewId: String) {
        val updates = mapOf(
            "reviews" to com.google.firebase.firestore.FieldValue.arrayUnion(reviewId)
        )

        BooksDatabase.update(bookDocumentId, updates)
            .addOnFailureListener { e ->
                println("Failed to update book reviews: ${e.message}")
            }
    }
}