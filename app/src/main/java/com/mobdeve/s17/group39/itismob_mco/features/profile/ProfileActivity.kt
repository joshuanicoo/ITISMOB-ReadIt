package com.mobdeve.s17.group39.itismob_mco.features.profile

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.UsersDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.ProfileActivityLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.authentication.login.LoginActivity
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.ViewBookActivity
import com.mobdeve.s17.group39.itismob_mco.models.UserModel
import com.mobdeve.s17.group39.itismob_mco.utils.GoogleBooksApiInterface
import com.mobdeve.s17.group39.itismob_mco.utils.LoadingUtils
import com.mobdeve.s17.group39.itismob_mco.utils.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileActivityLayoutBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var favoritesAdapter: ProfileFavoritesAdapter
    private lateinit var googleBooksApi: GoogleBooksApiInterface
    private val favoriteBooks = mutableListOf<BookItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        googleBooksApi = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Show loading initially
        showLoading()

        setupFavoritesRecyclerView()
        loadUserData()
        setupClickListeners()
    }

    private fun showLoading() {
        LoadingUtils.showLoading(
            binding.loadingContainer,
            binding.mainContentContainer,
            binding.quoteText,
            binding.quoteAuthor
        )
    }

    private fun hideLoading() {
        LoadingUtils.hideLoading(binding.loadingContainer, binding.mainContentContainer)
    }

    private fun setupFavoritesRecyclerView() {
        favoritesAdapter = ProfileFavoritesAdapter(favoriteBooks) { book ->
            openBookDetails(book)
        }

        binding.favoritesRv.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(this@ProfileActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            UsersDatabase.getById(currentUser.uid)
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userModel = UserModel.fromMap(documentSnapshot.id, documentSnapshot.data ?: emptyMap())

                        // Set current user pfp
                        val photoUrl = userModel.profilePicture ?: currentUser.photoUrl?.toString()
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(binding.root.context)
                                .load(photoUrl)
                                .placeholder(R.drawable.user_pfp_placeholder)
                                .error(R.drawable.user_pfp_placeholder)
                                .circleCrop()
                                .into(binding.profilePicIv)
                        } else {
                            binding.profilePicIv.setImageResource(R.drawable.user_pfp_placeholder)
                        }

                        // Set current username
                        val username = userModel.username ?: currentUser.displayName ?: "User"
                        binding.profileNameEt.text = SpannableStringBuilder(username)

                        // Set current bio if available
                        val bio = userModel.bio ?: ""
                        binding.profileBioEt.text = SpannableStringBuilder(bio)

                        // Load favorite books
                        loadFavoriteBooks(currentUser.uid)
                    } else {
                        hideLoading()
                    }
                }
                .addOnFailureListener {
                    hideLoading()
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            hideLoading()
        }
    }

    private fun loadFavoriteBooks(userId: String) {
        UsersDatabase.getFavorites(userId)
            .addOnSuccessListener { favoriteBookIds ->
                if (favoriteBookIds.isEmpty()) {
                    showNoFavorites()
                    hideLoading()
                    return@addOnSuccessListener
                }

                println("DEBUG: Favorite book IDs: $favoriteBookIds") // Add this for debugging

                // Fetch book documents directly from Firestore books collection
                val loadedBooks = mutableListOf<BookItem>()
                var completedRequests = 0

                favoriteBookIds.forEach { bookDocumentId ->
                    // Clean up the book document ID - remove any extra quotes or spaces
                    val cleanBookId = bookDocumentId.trim().removeSurrounding("\"")
                    println("DEBUG: Fetching book with ID: $cleanBookId") // Add this for debugging

                    com.mobdeve.s17.group39.itismob_mco.database.BooksDatabase.getById(cleanBookId)
                        .addOnSuccessListener { documentSnapshot ->
                            completedRequests++

                            if (documentSnapshot.exists()) {
                                val bookData = documentSnapshot.data ?: emptyMap()
                                println("DEBUG: Found book data: $bookData") // Add this for debugging

                                // Extract the Google Books ID from the book document
                                val googleBooksId = when (val bookIdField = bookData["bookId"]) {
                                    is Long -> bookIdField.toString()
                                    is String -> bookIdField
                                    else -> null
                                }

                                if (!googleBooksId.isNullOrEmpty()) {
                                    // Fetch book details from Google Books API
                                    fetchBookDetailsFromGoogleBooks(
                                        googleBooksId,
                                        cleanBookId,
                                        loadedBooks,
                                        completedRequests,
                                        favoriteBookIds.size
                                    )
                                } else {
                                    // If no Google Books ID, try to use data from Firestore if available
                                    val bookItem = createBookItemFromFirestoreData(cleanBookId, bookData)
                                    if (bookItem.title.isNotEmpty()) {
                                        loadedBooks.add(bookItem)
                                    }

                                    if (completedRequests == favoriteBookIds.size) {
                                        updateFavoritesUI(loadedBooks)
                                    }
                                }
                            } else {
                                println("DEBUG: Book document $cleanBookId does not exist")
                                if (completedRequests == favoriteBookIds.size) {
                                    updateFavoritesUI(loadedBooks)
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            completedRequests++
                            println("DEBUG: Failed to fetch book $cleanBookId: ${exception.message}")
                            if (completedRequests == favoriteBookIds.size) {
                                updateFavoritesUI(loadedBooks)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                showNoFavorites()
                hideLoading()
                println("DEBUG: Failed to load favorites: ${exception.message}")
                Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createBookItemFromFirestoreData(bookDocumentId: String, bookData: Map<String, Any>): BookItem {
        return BookItem(
            id = bookDocumentId,
            title = bookData["title"] as? String ?: "",
            authors = bookData["authors"] as? List<String> ?: emptyList(),
            description = bookData["description"] as? String ?: "",
            averageRating = (bookData["averageRating"] as? Double) ?: 0.0,
            ratingsCount = (bookData["ratingsCount"] as? Int) ?: 0,
            categories = bookData["categories"] as? List<String> ?: emptyList(),
            thumbnailUrl = bookData["thumbnailUrl"] as? String ?: ""
        )
    }

    private fun fetchBookDetailsFromGoogleBooks(googleBooksId: String, bookDocumentId: String, loadedBooks: MutableList<BookItem>, currentCompleted: Int, totalRequests: Int) {
        println("DEBUG: Fetching from Google Books API with ID: $googleBooksId")

        googleBooksApi.getBookByVolumeId(googleBooksId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful && response.body() != null) {
                    val bookData = response.body()!!
                    val volumeInfo = bookData["volumeInfo"] as? Map<String, Any>

                    if (volumeInfo != null) {
                        val bookItem = createBookItemFromVolumeInfo(bookDocumentId, volumeInfo)
                        loadedBooks.add(bookItem)
                        println("DEBUG: Successfully loaded book: ${bookItem.title}")
                    } else {
                        println("DEBUG: No volumeInfo in Google Books response")
                    }
                } else {
                    println("DEBUG: Google Books API response failed: ${response.code()} - ${response.message()}")
                }

                // Check if all requests are completed
                if (currentCompleted == totalRequests) {
                    updateFavoritesUI(loadedBooks)
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                println("DEBUG: Google Books API call failed: ${t.message}")
                // Check if all requests are completed
                if (currentCompleted == totalRequests) {
                    updateFavoritesUI(loadedBooks)
                }
            }
        })
    }

    private fun createBookItemFromVolumeInfo(bookDocumentId: String, volumeInfo: Map<String, Any>): BookItem {
        return BookItem(
            id = bookDocumentId, // Use the Firestore document ID
            title = volumeInfo["title"] as? String ?: "Unknown Title",
            authors = (volumeInfo["authors"] as? List<String>) ?: emptyList(),
            description = volumeInfo["description"] as? String ?: "",
            averageRating = (volumeInfo["averageRating"] as? Double) ?: 0.0,
            ratingsCount = (volumeInfo["ratingsCount"] as? Int) ?: 0,
            categories = (volumeInfo["categories"] as? List<String>) ?: emptyList(),
            thumbnailUrl = ((volumeInfo["imageLinks"] as? Map<String, String>)?.get("thumbnail")) ?: ""
        )
    }

    private fun updateFavoritesUI(loadedBooks: List<BookItem>) {
        if (loadedBooks.isNotEmpty()) {
            favoriteBooks.clear()
            favoriteBooks.addAll(loadedBooks)
            favoritesAdapter.updateFavorites(loadedBooks)
            binding.favoritesRv.visibility = android.view.View.VISIBLE
            binding.noFavoritesTv.visibility = android.view.View.GONE
        } else {
            showNoFavorites()
        }
        hideLoading()
    }

    private fun showNoFavorites() {
        binding.favoritesRv.visibility = android.view.View.GONE
        binding.noFavoritesTv.visibility = android.view.View.VISIBLE
    }

    private fun openBookDetails(book: BookItem) {
        val intent = Intent(this, ViewBookActivity::class.java).apply {
            putExtra(ViewBookActivity.TITLE_KEY, book.title)
            putExtra(ViewBookActivity.AUTHOR_KEY, book.authors.joinToString(", "))
            putExtra(ViewBookActivity.DESCRIPTION_KEY, book.description)
            putExtra(ViewBookActivity.AVG_RATING_KEY, book.averageRating)
            putExtra(ViewBookActivity.RATING_COUNT_KEY, book.ratingsCount)
            putExtra(ViewBookActivity.GENRE_KEY, book.categories.joinToString(", "))
            putExtra(ViewBookActivity.IMAGE_URL, book.thumbnailUrl)
            putExtra(ViewBookActivity.ID_KEY, book.id.removePrefix("book_"))
        }
        startActivity(intent)
    }

    private fun setupClickListeners() {
        binding.updateProfileBtn.setOnClickListener {
            updateProfile()
        }

        binding.logoutBtn.setOnClickListener {
            logout()
        }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val newUsername = binding.profileNameEt.text.toString().trim()
            val newBio = binding.profileBioEt.text.toString().trim()

            if (newUsername.isEmpty()) {
                Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            val updates = hashMapOf<String, Any>(
                "username" to newUsername,
                "bio" to newBio,
                "date_updated" to com.google.firebase.Timestamp.now()
            )

            UsersDatabase.update(currentUser.uid, updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun logout() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            navigateToLogin()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Data class for displaying book items in the RecyclerView
    data class BookItem(
        val id: String,
        val title: String,
        val authors: List<String>,
        val description: String,
        val averageRating: Double,
        val ratingsCount: Int,
        val categories: List<String>,
        val thumbnailUrl: String
    )
}