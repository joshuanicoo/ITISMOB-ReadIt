package com.mobdeve.s17.group39.itismob_mco.models

data class BookModel(
    val id: String = "",
    val bookId: Int = 0,
    val likedBy: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "bookId" to bookId,
            "likedBy" to likedBy
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): BookModel {
            return BookModel(
                id = id,
                bookId = (map["bookId"] as? Long)?.toInt() ?: 0,
                likedBy = map["likedBy"] as? List<String> ?: emptyList()
            )
        }
    }
}