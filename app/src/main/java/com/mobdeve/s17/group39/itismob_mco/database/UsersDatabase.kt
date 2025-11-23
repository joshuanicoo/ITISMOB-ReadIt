package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.mobdeve.s17.group39.itismob_mco.models.UserModel

object UsersDatabase : DatabaseHandler<UserModel>(FirestoreDatabase.usersCollection) {

    // Add book to favorites
    fun addToFavorites(userId: String, bookId: String): Task<Void> {
        return collectionRef
            .document(userId)
            .update("favorites", FieldValue.arrayUnion(bookId))
    }

    // Remove book from favorites
    fun removeFromFavorites(userId: String, bookId: String): Task<Void> {
        return collectionRef
            .document(userId)
            .update("favorites", FieldValue.arrayRemove(bookId))
    }

    // Return if book is liked by user - FIXED VERSION
    fun isLiked(userId: String, bookId: String): Task<Boolean> {
        return collectionRef
            .document(userId)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }

                val document = task.result
                if (document != null && document.exists()) {
                    val favorites = document.get("favorites") as? List<String> ?: emptyList()

                    // Check if the array contains the String bookId
                    favorites.contains(bookId)
                } else {
                    // If the document doesn't exist, the book can't be liked
                    false
                }
            }
    }

    // Get user's favorite books
    fun getFavorites(userId: String): Task<List<String>> {
        return collectionRef
            .document(userId)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }

                val document = task.result
                if (document != null && document.exists()) {
                    val favorites = document.get("favorites") as? List<String> ?: emptyList()
                    favorites
                } else {
                    emptyList()
                }
            }
    }
}