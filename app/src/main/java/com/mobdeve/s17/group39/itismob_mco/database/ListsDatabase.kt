package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.mobdeve.s17.group39.itismob_mco.models.ListModel

object ListsDatabase : DatabaseHandler<ListModel>(FirestoreDatabase.listsCollection) {

    // READ - Get all lists by user
    fun getListsByUserId(userId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereEqualTo("user_id", userId)
            .get()
    }

    // Add book to list (array operation)
    fun addBookToList(listId: String, bookId: String): Task<Void> {
        return collectionRef
            .document(listId)
            .update("books", FieldValue.arrayUnion(bookId))
    }

    // Remove book from list
    fun removeBookFromList(listId: String, bookId: String): Task<Void> {
        return collectionRef
            .document(listId)
            .update("books", FieldValue.arrayRemove(bookId))
    }
}
