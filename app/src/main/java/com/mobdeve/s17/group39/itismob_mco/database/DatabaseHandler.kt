package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query

abstract class DatabaseHandler<T : Any>(protected val collectionRef: CollectionReference) {

    // CRUD Operations
    fun create(obj: T): Task<DocumentReference> {
        return collectionRef.add(obj)
    }

    fun createWithId(documentId: String, obj: T): Task<Void> {
        return collectionRef.document(documentId).set(obj)
    }

    fun update(documentId: String, obj: T): Task<Void> {
        return collectionRef.document(documentId).set(obj)
    }

    fun delete(documentId: String): Task<Void> {
        return collectionRef.document(documentId).delete()
    }

    fun getById(documentId: String): Task<DocumentSnapshot> {
        return collectionRef.document(documentId).get()
    }

    fun getAll(): Query {
        return collectionRef
    }

    fun getDocumentReference(documentId: String): DocumentReference {
        return collectionRef.document(documentId)
    }
}
