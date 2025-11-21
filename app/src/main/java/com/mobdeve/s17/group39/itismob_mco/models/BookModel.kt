package com.mobdeve.s17.group39.itismob_mco.models

data class BookModel(
    val documentId: String = "",                // Firestore document ID
    val bookId: Int = 0,                        // Google Books API ID as Int
    val likedBy: List<String> = emptyList(),    // Array of user documentIds
    val reviews: List<String> = emptyList()     // Array of review documentIds
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "bookId" to bookId,
            "likedBy" to likedBy,
            "reviews" to reviews
        )
    }

    companion object {
        fun fromMap(documentId: String, map: Map<String, Any>): BookModel {
            return BookModel(
                documentId = documentId,
                bookId = (map["bookId"] as? Long)?.toInt() ?: 0,
                likedBy = map["likedBy"] as? List<String> ?: emptyList(),
                reviews = map["reviews"] as? List<String> ?: emptyList()
            )
        }
    }
}