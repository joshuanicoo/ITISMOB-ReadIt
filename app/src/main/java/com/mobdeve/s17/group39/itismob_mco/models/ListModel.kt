package com.mobdeve.s17.group39.itismob_mco.models

data class ListModel(
    val id: String = "",
    val listName: String = "",
    val userId: String = "",
    val books: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "list_name" to listName,
            "user_id" to userId,
            "books" to books
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): ListModel {
            return ListModel(
                id = id,
                listName = map["list_name"] as? String ?: "",
                userId = map["user_id"] as? String ?: "",
                books = map["books"] as? List<String> ?: emptyList()
            )
        }
    }
}
