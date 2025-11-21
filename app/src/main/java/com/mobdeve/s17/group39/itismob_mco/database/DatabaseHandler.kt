package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query

abstract class DatabaseHandler<T : Any>(protected val collectionRef: CollectionReference) {

    // CRUD Operations
    fun create(obj: T): Task<DocumentReference> {
        return collectionRef.add(obj)
    }

    fun createWithId(documentId: String, obj: T): Task<Void> {
        return collectionRef.document(documentId).set(obj)
    }

    // Update specific fields
    fun update(documentId: String, updates: Map<String, Any>): Task<Void> {
        return collectionRef.document(documentId).update(updates)
    }

    // Update a single field
    fun updateField(documentId: String, field: String, value: Any): Task<Void> {
        return collectionRef.document(documentId).update(field, value)
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