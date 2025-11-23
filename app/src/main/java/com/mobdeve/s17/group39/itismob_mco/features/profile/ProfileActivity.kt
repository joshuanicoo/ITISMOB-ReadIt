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
import com.google.firebase.auth.FirebaseUser
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.BooksDatabase
import com.mobdeve.s17.group39.itismob_mco.database.UsersDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.ProfileActivityLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.authentication.login.LoginActivity
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.ViewBookActivity
import com.mobdeve.s17.group39.itismob_mco.models.UserModel
import com.mobdeve.s17.group39.itismob_mco.utils.GoogleBooksApiInterface
import com.mobdeve.s17.group39.itismob_mco.utils.LoadingUtils
import com.mobdeve.s17.group39.itismob_mco.utils.RetrofitInstance
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import kotlin.coroutines.resumeWithException

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileActivityLayoutBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var favoritesAdapter: ProfileFavoritesAdapter
    private lateinit var googleBooksApi: GoogleBooksApiInterface
    private val favoriteBooks = mutableListOf<BookItem>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeAuth()
        setupFavoritesRecyclerView()
        loadUserData()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh favorites when returning to profile
        val currentUser = auth.currentUser
        currentUser?.let {
            loadFavoriteBooks(it.uid)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun initializeAuth() {
        auth = FirebaseAuth.getInstance()
        googleBooksApi = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        showLoading()
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
        favoritesAdapter = ProfileFavoritesAdapter(favoriteBooks, ::openBookDetails)
        binding.favoritesRv.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(this@ProfileActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: run {
            return
        }

        UsersDatabase.getById(currentUser.uid)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val userModel = UserModel.fromMap(documentSnapshot.id, documentSnapshot.data ?: emptyMap())
                    setupUserProfile(userModel, currentUser)
                }
            }
    }

    private fun setupUserProfile(userModel: UserModel, currentUser: FirebaseUser) {
        // Set profile picture
        val photoUrl = userModel.profilePicture ?: currentUser.photoUrl?.toString()
        Glide.with(binding.root.context)
            .load(photoUrl)
            .placeholder(R.drawable.user_pfp_placeholder)
            .error(R.drawable.user_pfp_placeholder)
            .circleCrop()
            .into(binding.profilePicIv)

        // Set username and bio
        binding.profileNameEt.text = SpannableStringBuilder(userModel.username ?: currentUser.displayName ?: "User")
        binding.profileBioEt.text = SpannableStringBuilder(userModel.bio ?: "")

        // Load favorite books
        loadFavoriteBooks(currentUser.uid)
    }

    private fun loadFavoriteBooks(userId: String) {
        UsersDatabase.getFavorites(userId)
            .addOnSuccessListener { favoriteBookIds ->
                if (favoriteBookIds.isEmpty()) {
                    binding.favoritesRv.visibility = android.view.View.GONE
                    binding.noFavoritesTv.visibility = android.view.View.VISIBLE
                    hideLoading()
                    return@addOnSuccessListener
                }

                // Use coroutines to maintain order and handle async operations properly
                coroutineScope.launch {
                    val loadedBooks = loadBooksInOrder(favoriteBookIds)
                    updateFavoritesUI(loadedBooks)
                }
            }
            .addOnFailureListener {
                binding.favoritesRv.visibility = android.view.View.GONE
                binding.noFavoritesTv.visibility = android.view.View.VISIBLE
                hideLoading()
                Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show()
            }
    }

    private suspend fun loadBooksInOrder(favoriteBookIds: List<String>): List<BookItem> = withContext(Dispatchers.IO) {
        val loadedBooks = mutableListOf<BookItem>()

        // Process books in the order they appear in favorites list
        favoriteBookIds.forEachIndexed { index, bookDocumentId ->
            try {
                val cleanBookId = bookDocumentId.trim().removeSurrounding("\"")
                val documentSnapshot = BooksDatabase.getById(cleanBookId).await()

                if (documentSnapshot.exists()) {
                    val googleBooksId = extractGoogleBooksId(documentSnapshot.data?.get("bookId"))
                    if (!googleBooksId.isNullOrEmpty()) {
                        val bookItem = fetchBookDetailsWithRetry(googleBooksId, cleanBookId)
                        loadedBooks.add(bookItem)
                    } else {
                        // Add placeholder if Google Books ID is not found
                        loadedBooks.add(createPlaceholderBookItem(cleanBookId))
                    }
                } else {
                    // Add placeholder if book document doesn't exist
                    loadedBooks.add(createPlaceholderBookItem(cleanBookId))
                }
            } catch (e: Exception) {
                // Add placeholder on error but maintain order
                val cleanBookId = bookDocumentId.trim().removeSurrounding("\"")
                loadedBooks.add(createPlaceholderBookItem(cleanBookId))
            }
        }

        return@withContext loadedBooks
    }

    private suspend fun fetchBookDetailsWithRetry(googleBooksId: String, bookDocumentId: String): BookItem = withContext(Dispatchers.IO) {
        try {
            // Using retrofit's await() for coroutines
            val response = googleBooksApi.getBookByVolumeId(googleBooksId).await()

            if (response.isSuccessful) {
                response.body()?.let { bookData ->
                    val volumeInfo = bookData["volumeInfo"] as? Map<String, Any>
                    volumeInfo?.let {
                        return@withContext createBookItemFromVolumeInfo(bookDocumentId, it)
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through to create placeholder
        }

        // Return placeholder if API call fails
        return@withContext createPlaceholderBookItem(bookDocumentId)
    }

    private fun createPlaceholderBookItem(bookDocumentId: String): BookItem {
        return BookItem(
            id = bookDocumentId,
            title = "Unknown Book",
            authors = emptyList(),
            description = "",
            averageRating = 0.0,
            ratingsCount = 0,
            categories = emptyList(),
            thumbnailUrl = ""
        )
    }

    private fun extractGoogleBooksId(bookIdField: Any?): String? = when (bookIdField) {
        is Long -> bookIdField.toString()
        is String -> bookIdField
        else -> null
    }

    private fun createBookItemFromVolumeInfo(bookDocumentId: String, volumeInfo: Map<String, Any>): BookItem {
        val categories = when (val cats = volumeInfo["categories"]) {
            is List<*> -> cats.filterIsInstance<String>()
            is String -> parseCategoriesFromString(cats)
            else -> emptyList()
        }

        // Clean up description - remove HTML tags and extra whitespace
        val rawDescription = volumeInfo["description"] as? String ?: ""
        val cleanDescription = cleanBookDescription(rawDescription)

        return BookItem(
            id = bookDocumentId,
            title = volumeInfo["title"] as? String ?: "Unknown Title",
            authors = (volumeInfo["authors"] as? List<String>) ?: emptyList(),
            description = cleanDescription,
            averageRating = (volumeInfo["averageRating"] as? Double) ?: 0.0,
            ratingsCount = (volumeInfo["ratingsCount"] as? Int) ?: 0,
            categories = categories,
            thumbnailUrl = extractThumbnailUrl(volumeInfo)
        )
    }

    // For some reason googlebooks API sends desc with html tags when searched using Id so well...
    private fun cleanBookDescription(description: String): String {
        return description
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("<p>", "\n")
            .replace("</p>", "\n")
            .replace("<[^>]*>".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .replace("\n\\s+".toRegex(), "\n")
            .trim()
    }

    private fun parseCategoriesFromString(categoriesString: String): List<String> = when {
        categoriesString.contains(",") -> categoriesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else -> listOf(categoriesString.trim())
    }

    private fun extractThumbnailUrl(volumeInfo: Map<String, Any>): String {
        val imageLinks = volumeInfo["imageLinks"] as? Map<String, Any>
        val baseUrl = imageLinks?.let { links ->
            listOf("thumbnail", "smallThumbnail", "medium", "large", "extraLarge", "small")
                .mapNotNull { links[it] as? String }
                .firstOrNull { it.isNotEmpty() }
        } ?: ""

        return if (baseUrl.isEmpty()) "" else enhanceGoogleBooksUrl(baseUrl)
    }

    private fun enhanceGoogleBooksUrl(url: String): String {
        var enhancedUrl = url.replace("http://", "https://").replace("&edge=curl", "")

        when {
            enhancedUrl.contains("googleapis.com") -> {
                enhancedUrl = enhancedUrl.replace("zoom=1", "zoom=2").replace("imgmax=128", "imgmax=512")
                if (!enhancedUrl.contains("imgmax=")) enhancedUrl += if (enhancedUrl.contains("?")) "&imgmax=512" else "?imgmax=512"
                if (!enhancedUrl.contains("zoom=")) enhancedUrl += if (enhancedUrl.contains("?")) "&zoom=2" else "?zoom=2"
            }
            enhancedUrl.contains("books.google.com") -> {
                enhancedUrl = enhancedUrl.replace("&printsec=frontcover", "&printsec=frontcover&img=1&zoom=2")
                if (!enhancedUrl.contains("img=")) enhancedUrl += if (enhancedUrl.contains("?")) "&img=1" else "?img=1"
                if (!enhancedUrl.contains("zoom=")) enhancedUrl += "&zoom=2"
            }
        }

        return enhancedUrl
    }

    private fun updateFavoritesUI(loadedBooks: List<BookItem>) {
        if (loadedBooks.isNotEmpty()) {
            favoriteBooks.clear()
            favoriteBooks.addAll(loadedBooks)
            favoritesAdapter.updateFavorites(loadedBooks)
            binding.favoritesRv.visibility = android.view.View.VISIBLE
            binding.noFavoritesTv.visibility = android.view.View.GONE
            hideLoading()
        } else {
            binding.favoritesRv.visibility = android.view.View.GONE
            binding.noFavoritesTv.visibility = android.view.View.VISIBLE
            hideLoading()
        }
    }

    private fun openBookDetails(book: BookItem) {
        startActivity(Intent(this, ViewBookActivity::class.java).apply {
            putExtra(ViewBookActivity.TITLE_KEY, book.title)
            putExtra(ViewBookActivity.AUTHOR_KEY, book.authors.joinToString(", "))
            putExtra(ViewBookActivity.DESCRIPTION_KEY, book.description)
            putExtra(ViewBookActivity.AVG_RATING_KEY, book.averageRating)
            putExtra(ViewBookActivity.RATING_COUNT_KEY, book.ratingsCount)
            putExtra(ViewBookActivity.GENRE_KEY, book.categories.joinToString(", "))
            putExtra(ViewBookActivity.IMAGE_URL, book.thumbnailUrl)
            putExtra(ViewBookActivity.ID_KEY, book.id.removePrefix("book_"))
        })
    }

    private fun setupClickListeners() {
        binding.updateProfileBtn.setOnClickListener { updateProfile() }
        binding.logoutBtn.setOnClickListener { logout() }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser ?: return

        val newUsername = binding.profileNameEt.text.toString().trim()
        val newBio = binding.profileBioEt.text.toString().trim()

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        UsersDatabase.update(currentUser.uid, mapOf(
            "username" to newUsername,
            "bio" to newBio,
            "date_updated" to com.google.firebase.Timestamp.now()
        )).addOnSuccessListener {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
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
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

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

suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result, null)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
            }
        }
    }
}

suspend fun <T> Call<T>.await(): Response<T> {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                continuation.resume(response, null)
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
}