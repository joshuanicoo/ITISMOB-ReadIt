package com.mobdeve.s17.group39.itismob_mco.models

data class ReviewModel (
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val username: String = "", // Add this field
    val userProfilePicture: String? = null, // Add this field
    val rating: Int = 0,
    val comment: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now() // Add this field
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "book_id" to bookId,
            "user_id" to userId,
            "username" to username,
            "user_profile_picture" to (userProfilePicture ?: ""),
            "rating" to rating,
            "comment" to comment,
            "likes" to likes,
            "liked_by" to likedBy,
            "created_at" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): ReviewModel {
            return ReviewModel(
                id = id,
                bookId = map["book_id"] as? String ?: "",
                userId = map["user_id"] as? String ?: "",
                username = map["username"] as? String ?: "Anonymous",
                userProfilePicture = map["user_profile_picture"] as? String,
                rating = (map["rating"] as? Long)?.toInt() ?: 0,
                comment = map["comment"] as? String ?: "",
                likes = (map["likes"] as? Long)?.toInt() ?: 0,
                likedBy = map["liked_by"] as? List<String> ?: emptyList(),
                createdAt = map["created_at"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
            )
        }
    }
}