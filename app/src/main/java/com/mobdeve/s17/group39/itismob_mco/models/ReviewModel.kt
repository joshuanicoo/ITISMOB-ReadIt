package com.mobdeve.s17.group39.itismob_mco.models

import com.google.firebase.Timestamp

data class ReviewModel(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val authorLikedBook: Boolean = false,

    val username: String = "",
    val userProfilePicture: String? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "bookId" to bookId,
            "userId" to userId,
            "rating" to rating,
            "comment" to comment,
            "likes" to likes,
            "likedBy" to likedBy,
            "createdAt" to createdAt,
            "authorLikedBook" to authorLikedBook
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): ReviewModel {
            return ReviewModel(
                id = id,
                bookId = map["bookId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                rating = (map["rating"] as? Double)?.toFloat() ?: 0f,
                comment = map["comment"] as? String ?: "",
                likes = (map["likes"] as? Long)?.toInt() ?: 0,
                likedBy = map["likedBy"] as? List<String> ?: emptyList(),
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                authorLikedBook = map["authorLikedBook"] as? Boolean ?: false,
                username = "",
                userProfilePicture = null
            )
        }
    }

    fun withUserData(username: String, userProfilePicture: String? = null): ReviewModel {
        return this.copy(
            username = username,
            userProfilePicture = userProfilePicture
        )
    }
}