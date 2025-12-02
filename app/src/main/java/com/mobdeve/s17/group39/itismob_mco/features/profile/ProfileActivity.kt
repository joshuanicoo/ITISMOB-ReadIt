package com.mobdeve.s17.group39.itismob_mco.features.profile

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
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
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileActivityLayoutBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var favoritesAdapter: ProfileFavoritesAdapter
    private lateinit var googleBooksApi: GoogleBooksApiInterface
    private val favoriteBooks = mutableListOf<BookItem>()
    private var bannerBookUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAuth()
        setupFavorites()
        loadUser()
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        val currentUser = auth.currentUser
        currentUser?.let {
            loadFavorites(it.uid)
        }
    }

    private fun initAuth() {
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

    private fun loadUser() {
        val currentUser = auth.currentUser ?: return

        UsersDatabase.getById(currentUser.uid)
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val userModel = UserModel.fromMap(doc.id, doc.data ?: emptyMap())
                    setupProfile(userModel, currentUser)
                }
            }
    }

    private fun setupProfile(userModel: UserModel, currentUser: FirebaseUser) {
        val photoUrl = userModel.profilePicture ?: currentUser.photoUrl?.toString()
        Glide.with(binding.root.context)
            .load(photoUrl)
            .placeholder(R.drawable.user_pfp_placeholder)
            .error(R.drawable.user_pfp_placeholder)
            .circleCrop()
            .into(binding.profilePicIv)

        binding.profileNameEt.text = SpannableStringBuilder(
            userModel.username ?: currentUser.displayName ?: "User"
        )
        binding.profileBioEt.text = SpannableStringBuilder(userModel.bio ?: "")

        loadFavorites(currentUser.uid)
    }

    private fun loadFavorites(userId: String) {
        UsersDatabase.getFavorites(userId)
            .addOnSuccessListener { favoriteIds ->
                if (favoriteIds.isEmpty()) {
                    binding.favoritesRv.visibility = android.view.View.GONE
                    binding.noFavoritesTv.visibility = android.view.View.VISIBLE
                    hideLoading()
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {
                    val books = loadBooks(favoriteIds)
                    updateUI(books)
                    setBanner(books)
                }
            }
            .addOnFailureListener {
                binding.favoritesRv.visibility = android.view.View.GONE
                binding.noFavoritesTv.visibility = android.view.View.VISIBLE
                hideLoading()
                Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupFavorites() {
        favoritesAdapter = ProfileFavoritesAdapter(favoriteBooks, ::openBook)
        binding.favoritesRv.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(
                this@ProfileActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    // Sets a random favorite book cover as the banner
    private fun setBanner(books: List<BookItem>) {
        if (books.isNotEmpty()) {
            val randomBook = books[Random.nextInt(books.size)]
            bannerBookUrl = randomBook.thumbnailUrl

            if (!randomBook.thumbnailUrl.isNullOrEmpty()) {
                loadBanner(randomBook.thumbnailUrl)
            } else {
                val bookWithImage = books.firstOrNull { !it.thumbnailUrl.isNullOrEmpty() }
                if (bookWithImage != null) {
                    bannerBookUrl = bookWithImage.thumbnailUrl
                    loadBanner(bookWithImage.thumbnailUrl)
                }
            }
        }
    }

    private fun loadBanner(imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .transform(
                        CenterCrop(),
                        BlurTransformation(25, 3),
                        ColorFilterTransformation(Color.parseColor("#66000000"))
                    )
                    .placeholder(android.R.color.transparent)
                    .error(R.drawable.book_placeholder)
            )
            .into(binding.profileBannerIv)
    }

    private suspend fun loadBooks(bookIds: List<String>): List<BookItem> = withContext(Dispatchers.IO) {
        val books = mutableListOf<BookItem>()

        bookIds.forEachIndexed { _, bookDocId ->
            try {
                val cleanId = bookDocId.trim().removeSurrounding("\"")
                val doc = BooksDatabase.getById(cleanId).await()

                if (doc.exists()) {
                    val googleId = getBookId(doc.data?.get("bookId"))
                    if (!googleId.isNullOrEmpty()) {
                        val book = fetchBook(googleId, cleanId)
                        books.add(book)
                    } else {
                        books.add(createPlaceholder(cleanId))
                    }
                } else {
                    books.add(createPlaceholder(cleanId))
                }
            } catch (e: Exception) {
                val cleanId = bookDocId.trim().removeSurrounding("\"")
                books.add(createPlaceholder(cleanId))
            }
        }

        return@withContext books
    }

    private suspend fun fetchBook(googleId: String, bookDocId: String): BookItem = withContext(Dispatchers.IO) {
        try {
            val response = googleBooksApi.getBookByVolumeId(googleId).await()

            if (response.isSuccessful) {
                response.body()?.let { data ->
                    val volumeInfo = data["volumeInfo"] as? Map<String, Any>
                    volumeInfo?.let {
                        return@withContext createBook(bookDocId, it)
                    }
                }
            }
        } catch (e: Exception) {
        }

        return@withContext createPlaceholder(bookDocId)
    }

    private fun createPlaceholder(bookDocId: String): BookItem {
        return BookItem(
            id = bookDocId,
            title = "Unknown Book",
            authors = emptyList(),
            description = "",
            averageRating = 0.0,
            ratingsCount = 0,
            categories = emptyList(),
            thumbnailUrl = ""
        )
    }

    private fun getBookId(bookIdField: Any?): String? = when (bookIdField) {
        is Long -> bookIdField.toString()
        is String -> bookIdField
        else -> null
    }

    private fun createBook(bookDocId: String, volumeInfo: Map<String, Any>): BookItem {
        val categories = when (val cats = volumeInfo["categories"]) {
            is List<*> -> cats.filterIsInstance<String>()
            is String -> parseCategories(cats)
            else -> emptyList()
        }

        val rawDesc = volumeInfo["description"] as? String ?: ""
        val cleanDesc = cleanDescription(rawDesc)

        return BookItem(
            id = bookDocId,
            title = volumeInfo["title"] as? String ?: "Unknown Title",
            authors = (volumeInfo["authors"] as? List<String>) ?: emptyList(),
            description = cleanDesc,
            averageRating = (volumeInfo["averageRating"] as? Double) ?: 0.0,
            ratingsCount = (volumeInfo["ratingsCount"] as? Int) ?: 0,
            categories = categories,
            thumbnailUrl = getThumbnail(volumeInfo)
        )
    }

    // Another difference with searching by book name vs book id is the formatting of the description
    private fun cleanDescription(description: String): String {
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

    private fun parseCategories(categoriesString: String): List<String> = when {
        categoriesString.contains(",") -> categoriesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else -> listOf(categoriesString.trim())
    }

    private fun getThumbnail(volumeInfo: Map<String, Any>): String {
        val imageLinks = volumeInfo["imageLinks"] as? Map<String, Any>
        val baseUrl = imageLinks?.let { links ->
            listOf("thumbnail", "smallThumbnail", "medium", "large", "extraLarge", "small")
                .mapNotNull { links[it] as? String }
                .firstOrNull { it.isNotEmpty() }
        } ?: ""

        return if (baseUrl.isEmpty()) "" else enhanceUrl(baseUrl)
    }

    private fun enhanceUrl(url: String): String {
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

    private fun updateUI(books: List<BookItem>) {
        if (books.isNotEmpty()) {
            favoriteBooks.clear()
            favoriteBooks.addAll(books)
            favoritesAdapter.updateFavorites(books)
            binding.favoritesRv.visibility = android.view.View.VISIBLE
            binding.noFavoritesTv.visibility = android.view.View.GONE
            hideLoading()
        } else {
            binding.favoritesRv.visibility = android.view.View.GONE
            binding.noFavoritesTv.visibility = android.view.View.VISIBLE
            hideLoading()
        }
    }

    // Send to ViewBookActivity
    private fun openBook(book: BookItem) {
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

    private fun setupClicks() {
        binding.updateProfileBtn.setOnClickListener { updateProfile() }
        binding.logoutBtn.setOnClickListener { logout() }

        binding.profileBannerIv.setOnClickListener {
            refreshBanner()
        }
    }

    private fun refreshBanner() {
        if (favoriteBooks.isNotEmpty()) {
            setBanner(favoriteBooks)
            Toast.makeText(this, "Banner refreshed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser ?: return

        val newUsername = binding.profileNameEt.text.toString().trim()
        val newBio = binding.profileBioEt.text.toString().trim()

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    UsersDatabase.update(currentUser.uid, mapOf(
                        "username" to newUsername,
                        "bio" to newBio,
                        "date_updated" to com.google.firebase.Timestamp.now()
                    )).addOnSuccessListener {
                        hideLoading()
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        loadUser()
                    }.addOnFailureListener { e ->
                        hideLoading()
                        Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    hideLoading()
                    Toast.makeText(this, "Failed to update auth profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun logout() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            goToLogin()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToLogin() {
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

// So we can use await
// For firestore calls
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

// For retrofit calls
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