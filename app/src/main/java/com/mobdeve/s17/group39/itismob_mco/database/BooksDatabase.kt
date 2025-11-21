package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.mobdeve.s17.group39.itismob_mco.models.BookModel

object BooksDatabase : DatabaseHandler<BookModel>(FirestoreDatabase.booksCollection) {

    // Add user to book's likedBy
    fun addUserToLikedBy(bookId: String, userDocumentId: String): Task<Void> {
        return collectionRef
            .document(bookId)
            .update("likedBy", FieldValue.arrayUnion(userDocumentId))
    }

    // Remove user from book's likedBy
    fun removeUserFromLikedBy(bookId: String, userDocumentId: String): Task<Void> {
        return collectionRef
            .document(bookId)
            .update("likedBy", FieldValue.arrayRemove(userDocumentId))
    }

    // Get all books liked by a specific user
    fun getBooksLikedByUser(userDocumentId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereArrayContains("likedBy", userDocumentId)
            .get()
    }
}