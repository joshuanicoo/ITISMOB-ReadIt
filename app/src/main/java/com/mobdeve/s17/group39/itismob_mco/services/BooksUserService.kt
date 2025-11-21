package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Tasks
import com.mobdeve.s17.group39.itismob_mco.models.BookModel

object BookUserService {

    // Add like relationship both ways
    // Handle book creating in collection if needed
    fun addToIsLiked(userDocumentId: String, bookDocumentId: String, googleBooksId: Int): com.google.android.gms.tasks.Task<Void> {
        // First check if book exists
        return BooksDatabase.getById(bookDocumentId)
            .continueWithTask { bookTask ->
                if (bookTask.isSuccessful && bookTask.result.exists()) {
                    // Book exists, just update likedBy
                    BooksDatabase.addUserToLikedBy(bookDocumentId, userDocumentId)
                } else {
                    // Book doesn't exist, create it first with the user already in likedBy
                    val bookModel = BookModel(
                        documentId = bookDocumentId,
                        bookId = googleBooksId,
                        likedBy = listOf(userDocumentId),
                        reviews = emptyList()
                    )
                    BooksDatabase.createWithId(bookDocumentId, bookModel)
                }
            }
            .continueWithTask {
                // Then add to user's favorites
                UsersDatabase.addToFavorites(userDocumentId, bookDocumentId)
            }
    }

    // Remove like relationship both ways
    fun removeFromIsLiked(userDocumentId: String, bookDocumentId: String): com.google.android.gms.tasks.Task<Void> {
        // Execute both operations atomically
        val removeFromFavoritesTask = UsersDatabase.removeFromFavorites(userDocumentId, bookDocumentId)
        val removeFromLikedByTask = BooksDatabase.removeUserFromLikedBy(bookDocumentId, userDocumentId)

        // Wait for both tasks to complete
        return Tasks.whenAll(removeFromFavoritesTask, removeFromLikedByTask)
    }

    // Check if user has liked a book
    fun isBookLikedByUser(userDocumentId: String, bookDocumentId: String): com.google.android.gms.tasks.Task<Boolean> {
        return UsersDatabase.getById(userDocumentId)
            .continueWith { task ->
                if (task.isSuccessful) {
                    val userDocument = task.result
                    val favorites = userDocument?.get("favorites") as? List<String> ?: emptyList()
                    favorites.contains(bookDocumentId)
                } else {
                    false
                }
            }
    }
}