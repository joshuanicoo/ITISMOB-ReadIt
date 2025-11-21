package com.mobdeve.s17.group39.itismob_mco.models
data class ReviewModel (
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePicture: String? = null,
    val rating: Int = 0,
    val comment: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "bookId" to bookId,
            "userId" to userId,
            "username" to username,
            "userProfilePicture" to (userProfilePicture ?: ""),
            "rating" to rating,
            "comment" to comment,
            "likes" to likes,
            "likedBy" to likedBy,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): ReviewModel {
            return ReviewModel(
                id = id,
                bookId = map["bookId"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                username = map["username"] as? String ?: "Anonymous",
                userProfilePicture = map["userProfilePicture"] as? String,
                rating = (map["rating"] as? Long)?.toInt() ?: 0,
                comment = map["comment"] as? String ?: "",
                likes = (map["likes"] as? Long)?.toInt() ?: 0,
                likedBy = map["likedBy"] as? List<String> ?: emptyList(),
                createdAt = map["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
            )
        }
    }
}