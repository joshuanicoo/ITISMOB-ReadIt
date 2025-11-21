package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel

object ReviewsDatabase : DatabaseHandler<ReviewModel>(FirestoreDatabase.reviewsCollection) {

    // READ - Get all reviews by a specific user
    fun getReviewsByUserId(userId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereEqualTo("user_id", userId)
            .get()
    }

    // Like a review (increment operation)
    fun likeReview(reviewId: String, userId: String): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "likes" to FieldValue.increment(1),
            "liked_by" to FieldValue.arrayUnion(userId)
        )

        return collectionRef
            .document(reviewId)
            .update(updates)
    }

    // Unlike a review
    fun unlikeReview(reviewId: String, userId: String): Task<Void> {
        val updates = hashMapOf<String, Any>(
            "likes" to FieldValue.increment(-1),
            "liked_by" to FieldValue.arrayRemove(userId)
        )

        return collectionRef
            .document(reviewId)
            .update(updates)
    }

    // Get top rated reviews (with ordering)
    fun getTopRatedReviews(limit: Int = 10): Task<QuerySnapshot> {
        return collectionRef
            .orderBy("rating", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
    }
}
