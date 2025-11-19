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
}
