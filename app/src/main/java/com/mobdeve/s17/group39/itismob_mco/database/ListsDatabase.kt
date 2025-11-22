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

    // CREATE - Create a new list for user
    fun createList(listName: String, userId: String): Task<Void> {
        val listModel = ListModel(
            listName = listName,
            userId = userId,
            books = emptyList()
        )

        // Generate a unique ID for the list
        val listId = collectionRef.document().id
        return createWithId(listId, listModel)
    }

    // UPDATE - Update list name
    fun updateListName(listId: String, newListName: String): Task<Void> {
        return updateField(listId, "list_name", newListName)
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

    // Check if book exists in list
    fun isBookInList(listId: String, bookId: String): Task<Boolean> {
        return getById(listId)
            .continueWith { task ->
                if (task.isSuccessful) {
                    val list = task.result?.toObject(ListModel::class.java)
                    list?.books?.contains(bookId) ?: false
                } else {
                    false
                }
            }
    }

    // Get lists containing a specific book
    fun getListsContainingBook(userId: String, bookId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereEqualTo("user_id", userId)
            .whereArrayContains("books", bookId)
            .get()
    }
}