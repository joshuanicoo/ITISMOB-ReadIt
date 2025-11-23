package com.mobdeve.s17.group39.itismob_mco.models

import com.google.firebase.Timestamp

data class UserModel(
    val documentId: String = "",
    val username: String = "",
    val email: String = "",
    val profilePicture: String? = null,
    val bio: String? = null,
    val favorites: List<String> = emptyList(),
    val dateCreated: Timestamp? = null,
    val dateUpdated: Timestamp? = null
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "username" to username,
            "email" to email,
            "profilePicture" to (profilePicture ?: ""),
            "bio" to (bio ?: ""),
            "favorites" to favorites,
            "dateCreated" to (dateCreated ?: Timestamp.now()),
            "dateUpdated" to (dateUpdated ?: Timestamp.now())
        )
    }

    companion object {
        fun fromMap(documentId: String, map: Map<String, Any>): UserModel {
            val favoritesList = when (val favs = map["favorites"]) {
                is List<*> -> favs.mapNotNull {
                    when (it) {
                        is Long -> it.toString()    // Convert Long to String
                        is String -> it             // Keep as String
                        else -> null
                    }
                }
                else -> emptyList()
            }

            return UserModel(
                documentId = documentId,
                username = map["username"] as? String ?: "",
                email = map["email"] as? String ?: "",
                profilePicture = map["profilePicture"] as? String,
                bio = map["bio"] as? String,
                favorites = favoritesList,
                dateCreated = map["dateCreated"] as? Timestamp,
                dateUpdated = map["dateUpdated"] as? Timestamp
            )
        }
    }
}