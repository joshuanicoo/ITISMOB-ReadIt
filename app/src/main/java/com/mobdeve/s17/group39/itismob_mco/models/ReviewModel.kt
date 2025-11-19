package com.mobdeve.s17.group39.itismob_mco.models

data class ReviewModel (
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "book_id" to bookId,
            "user_id" to userId,
            "rating" to rating,
            "comment" to comment,
            "likes" to likes,
            "liked_by" to likedBy
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): ReviewModel {
            return ReviewModel(
                id = id,
                bookId = map["book_id"] as? String ?: "",
                userId = map["user_id"] as? String ?: "",
                rating = (map["rating"] as? Long)?.toInt() ?: 0,
                comment = map["comment"] as? String ?: "",
                likes = (map["likes"] as? Long)?.toInt() ?: 0,
                likedBy = map["liked_by"] as? List<String> ?: emptyList()
            )
        }
    }
}