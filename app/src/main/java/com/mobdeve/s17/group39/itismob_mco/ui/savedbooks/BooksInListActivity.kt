package com.mobdeve.s17.group39.itismob_mco.ui.savedbooks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksInListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.ui.homepage.*
import com.mobdeve.s17.group39.itismob_mco.ui.viewbook.ViewBookActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BooksInListActivity : AppCompatActivity() {

    companion object {
        const val LIST_NAME_KEY = "LIST_NAME_KEY"
        const val BOOK_COUNT_KEY = "BOOK_COUNT_KEY"
    }

    private lateinit var binding: BooksInListLayoutBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BooksInListLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        getApiInterface()
        fetchBooksForList()
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

    private fun fetchBooksForList() {
        val listName = intent.getStringExtra(LIST_NAME_KEY) ?: "List"

        val searchQuery = when (listName) {
            "Currently Reading" -> "popular fiction"
            "Want to Read" -> "bestsellers 2024"
            "Read" -> "classic literature"
            "Favorites" -> "award winning books"
            "To Buy" -> "new releases"
            "Summer Reading" -> "beach reads"
            "Classics" -> "classic novels"
            "Non-Fiction" -> "non-fiction bestsellers"
            else -> "popular books"
        }

        val call = apiInterface.searchBooks(
            query = searchQuery,
            maxResults = 6
        )

        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(
                call: Call<GoogleBooksResponse>,
                response: Response<GoogleBooksResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val booksResponse = response.body()!!
                    if (booksResponse.items.isNotEmpty()) {
                        adapter.updateData(booksResponse.items)
                        // update soon w actual count
                        binding.bookCountHeaderTv.text = "${booksResponse.items.size} books"
                    } else {
                        // fallback
                        showFallbackBooks()
                    }
                } else {
                    showFallbackBooks()
                }
            }

            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@BooksInListActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
                showFallbackBooks()
            }
        })
    }

    private fun showFallbackBooks() {
        val fallbackBooks = listOf(
            Volume(
                id = "1",
                kind = "books#volume",
                etag = "fallback1",
                selfLink = "",
                volumeInfo = VolumeInfo(
                    title = "Sample Book 1",
                    authors = listOf("Author One"),
                    publisher = "Sample Publisher",
                    publishedDate = "2023",
                    description = "This is a sample book description for demonstration purposes.",
                    categories = listOf("Fiction"),
                    averageRating = 4.0,
                    ratingsCount = 100,
                    imageLinks = ImageLinks(
                        smallThumbnail = "https://via.placeholder.com/128x196/4CAF50/FFFFFF?text=Book+1",
                        thumbnail = "https://via.placeholder.com/128x196/4CAF50/FFFFFF?text=Book+1",
                        small = null,
                        medium = null,
                        large = null,
                        extraLarge = null
                    ),
                    pageCount = 300,
                    language = "en",
                    // Add other required nullable parameters with null values
                    industryIdentifiers = null,
                    readingModes = null,
                    printedPageCount = null,
                    printType = null,
                    maturityRating = null,
                    allowAnonLogging = null,
                    contentVersion = null,
                    panelizationSummary = null,
                    previewLink = null,
                    infoLink = null,
                    canonicalVolumeLink = null,
                    subtitle = null
                ),
                saleInfo = null,
                accessInfo = null,
                searchInfo = null
            ),
            Volume(
                id = "2",
                kind = "books#volume",
                etag = "fallback2",
                selfLink = "",
                volumeInfo = VolumeInfo(
                    title = "Sample Book 2",
                    authors = listOf("Author Two"),
                    publisher = "Sample Publisher",
                    publishedDate = "2023",
                    description = "Another sample book for the collection.",
                    categories = listOf("Non-Fiction"),
                    averageRating = 4.5,
                    ratingsCount = 150,
                    imageLinks = ImageLinks(
                        smallThumbnail = "https://via.placeholder.com/128x196/2196F3/FFFFFF?text=Book+2",
                        thumbnail = "https://via.placeholder.com/128x196/2196F3/FFFFFF?text=Book+2",
                        small = null,
                        medium = null,
                        large = null,
                        extraLarge = null
                    ),
                    pageCount = 250,
                    language = "en",
                    // Add other required nullable parameters with null values
                    industryIdentifiers = null,
                    readingModes = null,
                    printedPageCount = null,
                    printType = null,
                    maturityRating = null,
                    allowAnonLogging = null,
                    contentVersion = null,
                    panelizationSummary = null,
                    previewLink = null,
                    infoLink = null,
                    canonicalVolumeLink = null,
                    subtitle = null
                ),
                saleInfo = null,
                accessInfo = null,
                searchInfo = null
            )
        )
        adapter.updateData(fallbackBooks)
        binding.bookCountHeaderTv.text = "${fallbackBooks.size} books"
    }

    private fun openBookDetails(volume: Volume, position: Int) {
        val intent = Intent(this, ViewBookActivity::class.java)
        val authorsString = volume.volumeInfo.authors?.joinToString(", ")
        val genreString = volume.volumeInfo.categories?.joinToString(", ")
        val imageUrl = volume.volumeInfo.imageLinks?.thumbnail

        intent.putExtra(ViewBookActivity.TITLE_KEY, volume.volumeInfo.title)
        intent.putExtra(ViewBookActivity.AUTHOR_KEY, authorsString)
        intent.putExtra(ViewBookActivity.DESCRIPTION_KEY, volume.volumeInfo.description)
        intent.putExtra(ViewBookActivity.AVG_RATING_KEY, volume.volumeInfo.averageRating)
        intent.putExtra(ViewBookActivity.RATING_COUNT_KEY, volume.volumeInfo.ratingsCount)
        intent.putExtra(ViewBookActivity.GENRE_KEY, genreString)
        intent.putExtra(ViewBookActivity.POSITION_KEY, position)
        intent.putExtra(ViewBookActivity.IMAGE_URL, imageUrl)

        startActivity(intent)
    }
}