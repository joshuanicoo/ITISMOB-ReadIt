package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.mobdeve.s17.group39.itismob_mco.models.ListModel

object ListsDatabase : DatabaseHandler<ListModel>(FirestoreDatabase.listsCollection) {

    // CREATE - Create a new list for user
    fun createList(listName: String, userId: String): Task<Void> {
        val listModel = ListModel(
            listName = listName,
            userId = userId,
            books = emptyList()
        )

        // Generate a unique ID for the list
        val documentId = collectionRef.document().id
        return createWithId(documentId, listModel)
    }

    // UPDATE - Update list name
    fun updateListName(documentId: String, newListName: String): Task<Void> {
        val updates = mapOf(
            "listName" to newListName
        )
        return update(documentId, updates)
    }

    // Add book to list (array operation)
    fun addBookToList(documentId: String, bookId: String): Task<Void> {
        return collectionRef
            .document(documentId)
            .update("books", FieldValue.arrayUnion(bookId))
    }

    // Remove book from list
    fun removeBookFromList(documentId: String, bookId: String): Task<Void> {
        return collectionRef
            .document(documentId)
            .update("books", FieldValue.arrayRemove(bookId))
    }

    // Check if book exists in list
    fun isBookInList(documentId: String, bookId: String): Task<Boolean> {
        return getById(documentId)
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
            .whereEqualTo("userId", userId)
            .whereArrayContains("books", bookId)
            .get()
    }

    // Get all lists for a specific user
    fun getUserLists(userId: String): Task<QuerySnapshot> {
        return collectionRef
            .whereEqualTo("userId", userId)
            .get()
    }

    // Delete a list
    fun deleteList(documentId: String): Task<Void> {
        return delete(documentId)
    }

    // Get list by ID with full details
    fun getListWithDetails(documentId: String): Task<ListModel?> {
        return getById(documentId)
            .continueWith { task ->
                if (task.isSuccessful && task.result.exists()) {
                    val document = task.result
                    ListModel.fromMap(document.id, document.data ?: emptyMap())
                } else {
                    null
                }
            }
    }
}