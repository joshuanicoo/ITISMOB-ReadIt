package com.mobdeve.s17.group39.itismob_mco.models

data class BookModel(
    val documentId: String = "",
    val bookId: String = "",
    val likedBy: List<String> = emptyList(),
    val reviews: List<String> = emptyList()
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
            // Handle both string and number types when reading from Firestore
            val bookId = when (val idField = map["bookId"]) {
                is String -> idField
                is Long -> idField.toString()
                is Int -> idField.toString()
                is Double -> idField.toLong().toString()
                else -> ""
            }

            return BookModel(
                documentId = documentId,
                bookId = bookId,
                likedBy = map["likedBy"] as? List<String> ?: emptyList(),
                reviews = map["reviews"] as? List<String> ?: emptyList()
            )
        }
    }
}