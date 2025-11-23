package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel

object ReviewsDatabase : DatabaseHandler<ReviewModel>(FirestoreDatabase.reviewsCollection) {

    // READ - Get all reviews for a specific book
    fun getReviewsByBookId(bookId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereEqualTo("bookId", bookId) // Try bookId first
            .get()
    }

    // READ - Get all reviews by a specific user
    fun getReviewsByUserId(userId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereEqualTo("userId", userId) // Changed from "user_id" to "userId"
            .get()
    }

    // Like a review
    fun likeReview(reviewId: String, userId: String): Task<Void> {

        val updates = hashMapOf<String, Any>(
            "likes" to FieldValue.increment(1),
            "likedBy" to FieldValue.arrayUnion(userId)
        )

        return collectionRef
            .document(reviewId)
            .update(updates)
            .addOnSuccessListener {
            }
    }

    // Unlike a review
    fun unlikeReview(reviewId: String, userId: String): Task<Void> {

        val updates = hashMapOf<String, Any>(
            "likes" to FieldValue.increment(-1),
            "likedBy" to FieldValue.arrayRemove(userId)
        )

        return collectionRef
            .document(reviewId)
            .update(updates)
            .addOnSuccessListener {
            }
    }
}