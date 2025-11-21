package com.mobdeve.s17.group39.itismob_mco.database

import com.google.android.gms.tasks.Tasks
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel
import com.mobdeve.s17.group39.itismob_mco.models.UserModel

object ReviewService {

    // Get reviews for a book with user data
    fun getReviewsWithUserData(bookId: String): com.google.android.gms.tasks.Task<List<ReviewModel>> {
        return ReviewsDatabase.getReviewsByBookId(bookId)
            .continueWithTask { reviewTask ->
                if (reviewTask.isSuccessful) {
                    val reviews = reviewTask.result.documents.map { document ->
                        ReviewModel.fromMap(document.id, document.data!!)
                    }

                    // Fetch user data for each review
                    val userTasks = reviews.map { review ->
                        UsersDatabase.getById(review.userId)
                            .continueWith { userTask ->
                                if (userTask.isSuccessful && userTask.result.exists()) {
                                    val userData = userTask.result.data!!
                                    review.copy(
                                        username = userData["username"] as? String ?: "Anonymous",
                                        userProfilePicture = userData["profile_picture"] as? String
                                    )
                                } else {
                                    review
                                }
                            }
                    }

                    Tasks.whenAllSuccess<ReviewModel>(userTasks)
                } else {
                    Tasks.forResult(emptyList<ReviewModel>())
                }
            }
    }
}