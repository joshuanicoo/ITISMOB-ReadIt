package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.mobdeve.s17.group39.itismob_mco.models.UserModel

object UsersDatabase : DatabaseHandler<UserModel>(FirestoreDatabase.usersCollection) {

    // Add book to favorites
    fun addToFavorites(userId: String, bookId: Int): Task<Void> {
        return collectionRef
            .document(userId)
            .update("favorites", FieldValue.arrayUnion(bookId))
    }

    // Remove book from favorites
    fun removeFromFavorites(userId: String, bookId: Int): Task<Void> {
        return collectionRef
            .document(userId)
            .update("favorites", FieldValue.arrayRemove(bookId))
    }

    // Return if book is liked by user
    fun isLiked(userId: String, bookId: Int): Task<Boolean> {
        return collectionRef
            .document(userId)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    // If the task fails, let the caller handle it by propagating the exception
                    throw task.exception!!
                }

                val document = task.result
                if (document != null && document.exists()) {
                    val favorites = document.get("favorites") as? List<Number>

                    // Check if the array is not null and contains the Int bookId
                    favorites?.map { it.toInt() }?.contains(bookId) == true
                } else {
                    // If the document doesn't exist, the book can't be liked
                    false
                }
            }
    }


}
