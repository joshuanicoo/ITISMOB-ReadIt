package com.mobdeve.s17.group39.itismob_mco.ui.viewbook.review

data class ReviewModel(
    val userPfpResId: Int,
    val username: String,
    val userRating: Float,
    val reviewBody: String,
    val isLikedByCurrentUser: Boolean,
    val likesCount: Int,
) {
}