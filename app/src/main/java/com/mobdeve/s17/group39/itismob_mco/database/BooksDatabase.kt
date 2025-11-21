package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import com.mobdeve.s17.group39.itismob_mco.models.BookModel

object BooksDatabase : DatabaseHandler<BookModel>(FirestoreDatabase.reviewsCollection) {

    // READ - Get all reviews for a specific book
    fun getReviewsByBookId(bookId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereEqualTo("book_id", bookId)
            .get()
    }
}