package com.mobdeve.s17.group39.itismob_mco.database

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreDatabase {
    private val db = FirebaseFirestore.getInstance()

    // Collection references
    const val USERS_COLLECTION = "users"
    const val REVIEWS_COLLECTION = "reviews"
    const val LISTS_COLLECTION = "lists"
    const val BOOKS_COLLECTION = "books"

    // Collection getters
    val usersCollection = db.collection(USERS_COLLECTION)
    val reviewsCollection = db.collection(REVIEWS_COLLECTION)
    val listsCollection = db.collection(LISTS_COLLECTION)
}
