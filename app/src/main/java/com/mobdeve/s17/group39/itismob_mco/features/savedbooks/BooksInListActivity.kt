package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.mobdeve.s17.group39.itismob_mco.database.BooksDatabase
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.database.ReviewsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksInListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.homepage.HomeAdapter
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.ViewBookActivity
import com.mobdeve.s17.group39.itismob_mco.models.ListModel
import com.mobdeve.s17.group39.itismob_mco.models.ReviewModel
import com.mobdeve.s17.group39.itismob_mco.utils.GoogleBooksApiInterface
import com.mobdeve.s17.group39.itismob_mco.utils.ImageUtils
import com.mobdeve.s17.group39.itismob_mco.utils.LoadingUtils
import com.mobdeve.s17.group39.itismob_mco.utils.RetrofitInstance
import com.mobdeve.s17.group39.itismob_mco.utils.Volume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.jsoup.Jsoup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

class BooksInListActivity : AppCompatActivity() {

    companion object {
        const val LIST_NAME_KEY = "LIST_NAME_KEY"
        const val LIST_ID_KEY = "LIST_ID_KEY"
        const val BOOK_COUNT_KEY = "BOOK_COUNT_KEY"
        private const val TAG = "BooksInListActivity"
    }

    private lateinit var binding: BooksInListLayoutBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface
    private var listListener: ListenerRegistration? = null
    private var currentListId: String = ""
    private val loadedBooks = mutableListOf<Volume>()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BooksInListLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentListId = intent.getStringExtra(LIST_ID_KEY) ?: ""
        if (currentListId.isEmpty()) {
            Toast.makeText(this, "Invalid list", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        getApiInterface()
        showLoading()
        setupListListener()
    }

    private fun setupListListener() {
        val documentRef = ListsDatabase.getDocumentReference(currentListId)
        listListener = documentRef.addSnapshotListener { documentSnapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Error loading list: ${error.message}", Toast.LENGTH_SHORT).show()
                hideLoading()
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val list = documentSnapshot.toObject(ListModel::class.java)
                list?.let { updateListUI(it) }
            } else {
                Toast.makeText(this, "List not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateListUI(list: ListModel) {
        binding.listTitleTv.text = list.listName
        val bookCount = list.books.size
        binding.bookCountHeaderTv.text = "$bookCount ${if (bookCount == 1) "book" else "books"}"
        Log.d(TAG, "List books: ${list.books}")
        if (list.books.isNotEmpty()) {
            loadBooksFromList(list.books)
        } else {
            Log.d(TAG, "List is empty, showing empty state")
            showEmptyState()
        }
    }

    private fun loadBooksFromList(bookIds: List<String>) {
        showLoading()
        loadedBooks.clear()
        adapter.updateData(emptyList())

        if (bookIds.isEmpty()) {
            showEmptyState()
            return
        }

        val totalBooks = bookIds.size
        val processedCount = AtomicInteger(0)
        bookIds.forEachIndexed { index, bookId ->
            val cleanBookId = cleanBookId(bookId)
            loadBookFromGoogleBooks(cleanBookId, bookId, processedCount, totalBooks)
        }
    }

    private fun loadBookFromGoogleBooks(cleanBookId: String, originalBookId: String,
                                        processedCount: AtomicInteger, totalBooks: Int) {
        apiInterface.getBookByVolumeId(cleanBookId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val jsonString = gson.toJson(response.body())
                        val volume = gson.fromJson(jsonString, Volume::class.java)
                        val cleanedVolume = createCleanedVolume(volume)
                        loadBookReviewsAndRatings(originalBookId, cleanedVolume, processedCount, totalBooks)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing book response for ID: $originalBookId", e)
                        incrementProcessedCount(processedCount, totalBooks)
                    }
                } else {
                    Log.w(TAG, "Failed to load book ID: $originalBookId, Response code: ${response.code()}")
                    incrementProcessedCount(processedCount, totalBooks)
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e(TAG, "API call failed for book ID: $originalBookId", t)
                incrementProcessedCount(processedCount, totalBooks)
            }
        })
    }

    private fun createCleanedVolume(volume: Volume): Volume {
        val cleanedDescription = volume.volumeInfo.description?.let { removeHtmlTags(it) }
        val cleanedVolumeInfo = volume.volumeInfo.copy(
            description = cleanedDescription
        )
        return volume.copy(
            volumeInfo = cleanedVolumeInfo
        )
    }

    private fun loadBookReviewsAndRatings(bookId: String, volume: Volume,
                                          processedCount: AtomicInteger, totalBooks: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val googleBooksId = cleanBookId(bookId)
                val reviewsQuery = ReviewsDatabase.getReviewsByBookId(googleBooksId).await()
                var reviews = mutableListOf<ReviewModel>()
                if (reviewsQuery?.documents?.isNotEmpty() == true) {
                    reviews = reviewsQuery.documents.mapNotNull { document ->
                        try {
                            val review = ReviewModel.fromMap(document.id, document.data ?: emptyMap())
                            review
                        } catch (e: Exception) {
                            null
                        }
                    }.toMutableList()
                } else {
                    val reviewsQuery2 = ReviewsDatabase.getReviewsByBookId(bookId).await()
                    if (reviewsQuery2?.documents?.isNotEmpty() == true) {
                        reviews = reviewsQuery2.documents.mapNotNull { document ->
                            try {
                                val review = ReviewModel.fromMap(document.id, document.data ?: emptyMap())
                                review
                            } catch (e: Exception) {
                                null
                            }
                        }.toMutableList()
                    }
                }

                if (reviews.isNotEmpty()) {
                    val firstReview = reviews.first()
                }
                val averageRating = if (reviews.isNotEmpty()) {
                    val avg = reviews.map { it.rating }.average().toFloat()
                    avg
                } else {
                    null
                }
                val reviewsCount = reviews.size
                val updatedVolume = if (averageRating != null || reviewsCount > 0) {
                    createVolumeWithReviewData(volume, averageRating?.toDouble(), reviewsCount)
                } else {
                    volume
                }
                withContext(Dispatchers.Main) {
                    synchronized(loadedBooks) {
                        loadedBooks.add(updatedVolume)
                    }
                }
                checkBookInDatabase(bookId, updatedVolume)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    synchronized(loadedBooks) {
                        loadedBooks.add(volume)
                    }
                }
            } finally {
                incrementProcessedCount(processedCount, totalBooks)
            }
        }
    }

    private fun createVolumeWithReviewData(
        volume: Volume,
        averageRating: Double?,
        ratingsCount: Int
    ): Volume {
        val updatedVolumeInfo = volume.volumeInfo.copy(
            averageRating = averageRating ?: volume.volumeInfo.averageRating,
            ratingsCount = if (ratingsCount > 0) ratingsCount else volume.volumeInfo.ratingsCount
        )
        return volume.copy(
            volumeInfo = updatedVolumeInfo
        )
    }
    private fun checkBookInDatabase(bookId: String, volume: Volume) {
        // Check if this book exists in our BooksDatabase collection
        BooksDatabase.getById(bookId).addOnSuccessListener { documentSnapshot ->
            if (!documentSnapshot.exists()) {
                BooksDatabase.createBookWithGoogleId(bookId, cleanBookId(bookId))
                    .addOnSuccessListener {
                        Log.d(TAG, "Created book in database: $bookId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to create book in database: $bookId", e)
                    }
            } else {
                Log.d(TAG, "Book already exists in database: $bookId")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error checking book in database: $bookId", e)
        }
    }

    private fun incrementProcessedCount(processedCount: AtomicInteger, totalBooks: Int) {
        val currentProcessed = processedCount.incrementAndGet()

        if (currentProcessed == totalBooks) {
            runOnUiThread {
                checkAndDisplayBooks()
            }
        }
    }

    private fun checkAndDisplayBooks() {
        hideLoading()

        if (loadedBooks.isEmpty()) {
            showEmptyState()
            Toast.makeText(
                this,
                "Failed to load books. Please check your internet connection and try again.",
                Toast.LENGTH_LONG
            ).show()
        } else {

            displayBooks()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val listDoc = ListsDatabase.getDocumentReference(currentListId).get().await()
                    val totalBooksInList = listDoc?.toObject(ListModel::class.java)?.books?.size ?: 0

                    if (loadedBooks.size < totalBooksInList) {
                        val failedCount = totalBooksInList - loadedBooks.size
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting list document", e)
                }
            }
        }
    }

    private fun cleanBookId(bookId: String): String {
        return if (bookId.startsWith("book_")) {
            bookId.substring(5)
        } else {
            bookId
        }
    }

    private fun removeHtmlTags(html: String): String {
        return try {
            Jsoup.parse(html).text()
        } catch (e: Exception) {
            html
        }
    }

    private fun displayBooks() {
        hideLoading()
        binding.booksInListRv.visibility = View.VISIBLE
        binding.emptyStateTv.visibility = View.GONE

        val sortedBooks = loadedBooks.sortedBy { it.volumeInfo.title }
        adapter.updateData(sortedBooks)

    }

    private fun showLoading() {
        LoadingUtils.showLoading(
            loadingContainer = binding.loadingContainer,
            mainContentContainer = binding.mainContentContainer,
            quoteText = binding.quoteText,
            quoteAuthor = binding.quoteAuthor
        )
    }

    private fun hideLoading() {
        LoadingUtils.hideLoading(
            loadingContainer = binding.loadingContainer,
            mainContentContainer = binding.mainContentContainer
        )
    }

    private fun showEmptyState() {
        hideLoading()
        binding.emptyStateTv.visibility = View.VISIBLE
        binding.emptyStateTv.text = "No books in this list yet"
        binding.booksInListRv.visibility = View.GONE
    }

    private fun setupUI() {
        val listName = intent.getStringExtra(LIST_NAME_KEY) ?: "List"
        val bookCount = intent.getIntExtra(BOOK_COUNT_KEY, 0)

        binding.listTitleTv.text = listName
        binding.bookCountHeaderTv.text = "$bookCount ${if (bookCount == 1) "book" else "books"}"
    }

    private fun setupRecyclerView() {
        adapter = HomeAdapter(emptyList())
        binding.booksInListRv.adapter = adapter
        binding.booksInListRv.layoutManager = GridLayoutManager(this, 2)

        adapter.setOnItemClickListener { volume, position ->
            openBookDetails(volume, position)
        }
    }

    private fun getApiInterface() {
        apiInterface = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)
    }

    private fun openBookDetails(volume: Volume, position: Int) {
        val intent = Intent(this, ViewBookActivity::class.java)
        val authorsString = volume.volumeInfo.authors?.joinToString(", ")
        val genreString = volume.volumeInfo.categories?.joinToString(", ")
        val imageUrl = ImageUtils.getEnhancedImageUrl(volume)
        val cleanDescription = volume.volumeInfo.description

        intent.putExtra(ViewBookActivity.TITLE_KEY, volume.volumeInfo.title)
        intent.putExtra(ViewBookActivity.AUTHOR_KEY, authorsString)
        intent.putExtra(ViewBookActivity.DESCRIPTION_KEY, cleanDescription ?: "")
        intent.putExtra(ViewBookActivity.AVG_RATING_KEY, volume.volumeInfo.averageRating)
        intent.putExtra(ViewBookActivity.RATING_COUNT_KEY, volume.volumeInfo.ratingsCount)
        intent.putExtra(ViewBookActivity.GENRE_KEY, genreString)
        intent.putExtra(ViewBookActivity.POSITION_KEY, position)
        intent.putExtra(ViewBookActivity.IMAGE_URL, imageUrl)
        intent.putExtra(ViewBookActivity.ID_KEY, volume.id)

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        listListener?.remove()
    }
}