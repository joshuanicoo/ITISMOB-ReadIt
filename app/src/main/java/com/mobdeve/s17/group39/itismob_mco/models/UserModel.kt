package com.mobdeve.s17.group39.itismob_mco.models

import com.google.firebase.Timestamp

data class UserModel(
    val documentId: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String? = null,
    val profilePicture: String? = null,
    val favorites: List<String> = emptyList(),
    val dateCreated: Timestamp = Timestamp.now(),
    val dateUpdated: Timestamp = Timestamp.now()
) {
    // Helper method to convert to Map for Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "username" to username,
            "email" to email,
            "bio" to bio,
            "profile_picture" to profilePicture,
            "favorites" to favorites,
            "date_created" to dateCreated,
            "date_updated" to dateUpdated
        )
    }

    companion object {
        // Helper method to create User from Firestore document
        fun fromMap(id: String, map: Map<String, Any>): UserModel {
            return UserModel(
                username = map["username"] as? String ?: "",
                email = map["email"] as? String ?: "",
                bio = map["bio"] as? String,
                profilePicture = map["profile_picture"] as? String,
                favorites = map["favorites"] as? List<String> ?: emptyList(),
                dateCreated = map["date_created"] as? Timestamp ?: Timestamp.now(),
                dateUpdated = map["date_updated"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}