package com.mobdeve.s17.group39.itismob_mco.models

data class ListModel(
    val documentId: String = "",
    val listName: String = "",
    val userId: String = "",
    val books: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "listName" to listName,
            "userId" to userId,
            "books" to books
        )
    }

    companion object {
        fun fromMap(documentId: String, map: Map<String, Any>): ListModel {
            return ListModel(
                documentId = documentId,
                listName = map["listName"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                books = map["books"] as? List<String> ?: emptyList()
            )
        }
    }
}