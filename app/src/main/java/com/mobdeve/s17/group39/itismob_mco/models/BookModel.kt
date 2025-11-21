package com.mobdeve.s17.group39.itismob_mco.models
data class BookModel(
    val documentId: String = "",
    val bookId: Int = 0,
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
            return BookModel(
                documentId = documentId,
                bookId = (map["bookId"] as? Long)?.toInt() ?: 0,
                likedBy = map["likedBy"] as? List<String> ?: emptyList(),
                reviews = map["reviews"] as? List<String> ?: emptyList()
            )
        }
    }
}