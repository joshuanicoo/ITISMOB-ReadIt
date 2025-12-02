package com.mobdeve.s17.group39.itismob_mco.features.homepage

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s17.group39.itismob_mco.databinding.HomeActivityBinding
import com.mobdeve.s17.group39.itismob_mco.features.profile.ProfileActivity
import com.mobdeve.s17.group39.itismob_mco.features.savedbooks.SavedListsActivity
import com.mobdeve.s17.group39.itismob_mco.features.scanner.ScannerActivity
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.ViewBookActivity
import com.mobdeve.s17.group39.itismob_mco.utils.GoogleBooksApiInterface
import com.mobdeve.s17.group39.itismob_mco.utils.GoogleBooksResponse
import com.mobdeve.s17.group39.itismob_mco.utils.LoadingUtils
import com.mobdeve.s17.group39.itismob_mco.utils.RetrofitInstance
import com.mobdeve.s17.group39.itismob_mco.utils.Volume
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.collections.HashSet

@ExperimentalGetImage
class HomeActivity : AppCompatActivity() {
    private lateinit var data: GoogleBooksResponse
    private lateinit var binding: HomeActivityBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface
    private lateinit var handler: Handler
    private lateinit var genreAdapter: HomeGenreAdapter

    // Store original books data for filtering
    private var allBooks: List<Volume> = emptyList()
    private var currentQuery: String = ""
    private var currentGenre: String? = null
    private var selectedGenrePosition: Int = 0 // Track which genre is selected
    private var isFilteringFromSearch: Boolean = false // Track if we're filtering search results

    // Add a variable to store the scanned ISBN
    private var scannedISBN: String? = null

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val isbn = result.data?.getStringExtra(ScannerActivity.SCANNED_ISBN_RESULT)
            isbn?.let {
                searchBookByIsbn(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getApiInterface()
        setupClickListeners()
        setupRecyclerView()
        setupGenreRecyclerView()
        showLoading()
        getBooks()

        handler = Handler(Looper.getMainLooper())

        this.binding.bookSv.clearFocus()
        binding.bookSv.isFocusable = true
        binding.bookSv.isClickable = true

        this.binding.bookSv.setOnClickListener { view ->
            binding.bookSv.onActionViewExpanded()
        }

        this.binding.bookSv.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchText = query?.lowercase(Locale.getDefault()) ?: ""
                if (searchText.isNotEmpty()) {
                    handler.removeCallbacksAndMessages(null)
                    currentQuery = searchText
                    currentGenre = null
                    selectedGenrePosition = 0 // Reset to "All" when searching
                    genreAdapter.setSelectedPosition(0)
                    searchBooks(searchText, 40, "books")
                } else {
                    currentQuery = ""
                    getBooks()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                val searchText = query?.lowercase(Locale.getDefault()) ?: ""
                handler.removeCallbacksAndMessages(null)
                if (searchText.isNotEmpty()) {
                    handler.postDelayed({
                        currentQuery = searchText
                        currentGenre = null
                        selectedGenrePosition = 0 // Reset to "All" when searching
                        genreAdapter.setSelectedPosition(0)
                        searchBooks(searchText, 40, "books")
                    }, 500)
                } else {
                    currentQuery = ""
                    getBooks()
                }
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        this.adapter = HomeAdapter(mutableListOf())
        this.binding.bookListRv.adapter = adapter
        this.binding.bookListRv.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupGenreRecyclerView() {
        val dataGenre = ArrayList<String>()
        dataGenre.add("All")
        this.genreAdapter = HomeGenreAdapter(dataGenre) { selectedGenre: String, position: Int ->
            onGenreSelected(selectedGenre, position)
        }
        this.binding.genreListRv.adapter = genreAdapter
        this.binding.genreListRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun onGenreSelected(genre: String, position: Int) {
        selectedGenrePosition = position
        genreAdapter.setSelectedPosition(position)

        if (genre == "All") {
            currentGenre = null
            isFilteringFromSearch = false

            // Show all books (either from search results or initial load)
            if (currentQuery.isNotEmpty()) {
                // We're in search mode, show all search results
                adapter.updateData(allBooks)
            } else {
                // We're in normal mode, show all books
                adapter.updateData(allBooks)
            }
        } else {
            currentGenre = genre
            isFilteringFromSearch = (currentQuery.isNotEmpty())
            filterBooksByGenre(genre)
        }
    }

    private fun filterBooksByGenre(genre: String) {
        // Always filter from the original source (allBooks), not from already filtered results
        val filteredBooks = allBooks.filter { volume ->
            hasMatchingGenre(volume, genre)
        }

        adapter.updateData(filteredBooks)

        // Show message if no books found for this genre
        if (filteredBooks.isEmpty()) {
            Toast.makeText(this, "No books found in $genre category", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasMatchingGenre(volume: Volume, genre: String): Boolean {
        return volume.volumeInfo.categories?.any { category ->
            // Handle different category formats:
            // 1. "Fiction/Mystery/Thriller" -> split by "/"
            // 2. "Fiction, Mystery, Thriller" -> split by ","
            // 3. Check if category contains the genre (case-insensitive)
            category.split("/", ",").any {
                it.trim().equals(genre, ignoreCase = true)
            }
        } ?: false
    }

    private fun extractGenresFromBooks(books: List<Volume>): List<String> {
        val genresSet = HashSet<String>()

        books.forEach { volume ->
            volume.volumeInfo.categories?.forEach { category ->
                // Split categories by common separators
                category.split("/", ",").forEach { genrePart ->
                    val trimmedGenre = genrePart.trim()
                    if (trimmedGenre.isNotEmpty() && !trimmedGenre.equals("General", ignoreCase = true)) {
                        genresSet.add(trimmedGenre)
                    }
                }
            }
        }

        // Get top 10 most common genres or all if less than 10
        val genreCounts = HashMap<String, Int>()
        books.forEach { volume ->
            volume.volumeInfo.categories?.forEach { category ->
                category.split("/", ",").forEach { genrePart ->
                    val trimmedGenre = genrePart.trim()
                    if (trimmedGenre.isNotEmpty()) {
                        genreCounts[trimmedGenre] = genreCounts.getOrDefault(trimmedGenre, 0) + 1
                    }
                }
            }
        }

        // Sort by frequency and get top genres
        val topGenres = genreCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        return listOf("All") + topGenres.sorted()
    }

    private fun showLoading() {
        LoadingUtils.showLoading(
            binding.loadingContainer,
            binding.bookListRv,
            binding.quoteText,
            binding.quoteAuthor
        )
    }

    private fun hideLoading() {
        LoadingUtils.hideLoading(binding.loadingContainer, binding.bookListRv)
    }

    private fun getBooks() {
        val call = apiInterface.getBooks()
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    data = response.body()!!

                    // Check if items is not null before using it
                    val items = data.items
                    if (items != null) {
                        allBooks = items // Store original books
                        adapter.updateData(items)

                        // Extract and display genres from the books
                        val genres = extractGenresFromBooks(items)
                        genreAdapter.updateGenres(genres)
                    } else {
                        // Items is null (no books found)
                        allBooks = emptyList()
                        adapter.updateData(emptyList())
                        genreAdapter.updateGenres(listOf("All"))
                        Toast.makeText(this@HomeActivity, "No books found", Toast.LENGTH_SHORT).show()
                    }

                    hideLoading()
                } else {
                    hideLoading()
                    Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                t.printStackTrace()
                hideLoading()
                Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchBooks(query : String, size: Int, printType: String){
        showLoading()
        val call = apiInterface.searchBooks(query, size, printType)
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    data = response.body()!!

                    // Check if items is not null before using it
                    val items = data.items
                    if (items != null) {
                        allBooks = items // Store original search results

                        // Extract and update genres from search results
                        val genres = extractGenresFromBooks(items)
                        genreAdapter.updateGenres(genres)

                        // Apply genre filter if one is selected (from previous state)
                        if (currentGenre != null && isFilteringFromSearch) {
                            filterBooksByGenre(currentGenre!!)
                        } else {
                            adapter.updateData(items)
                        }
                    } else {
                        // Items is null (no books found)
                        allBooks = emptyList()
                        adapter.updateData(emptyList())
                        genreAdapter.updateGenres(listOf("All"))
                        Toast.makeText(this@HomeActivity, "No books found for '$query'", Toast.LENGTH_SHORT).show()
                    }

                    hideLoading()
                } else {
                    hideLoading()
                    Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                t.printStackTrace()
                hideLoading()
                Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchByISBN(isbn: String) {
        showLoading()
        // Try using the searchBooks method
        val call = apiInterface.searchBooks(isbn, 1, "books")
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        data = body

                        // Debug logging
                        Log.d("ISBN_SEARCH", "Response received. Total items: ${body.totalItems}")
                        Log.d("ISBN_SEARCH", "Items list: ${body.items}")

                        // Check if items is not null before using it
                        val items = body.items
                        if (items != null && items.isNotEmpty()) {
                            allBooks = items // Store original results

                            // If we found a book with this ISBN, open it directly
                            adapter.updateData(items)

                            // Extract and display genres from the book(s)
                            val genres = extractGenresFromBooks(items)
                            genreAdapter.updateGenres(genres)

                            // Open the first book (should be the one matching the ISBN)
                            val volume = items[0]
                            Log.d("ISBN_SEARCH", "Found book via search: ${volume.volumeInfo.title}")
                            openBookDetailsDirectly(volume)
                            hideLoading()
                            return
                        }

                        // If we get here, items is null or empty
                        hideLoading()

                        // Check if totalItems indicates a book exists
                        if (body.totalItems > 0) {
                            Log.d("ISBN_SEARCH", "totalItems > 0 (${body.totalItems}) but items is null/empty")
                            Toast.makeText(this@HomeActivity, "Book found but data unavailable. Try searching by title.", Toast.LENGTH_LONG).show()
                        } else {
                            adapter.updateData(emptyList())
                            val genres = extractGenresFromBooks(emptyList())
                            genreAdapter.updateGenres(genres)
                            Toast.makeText(this@HomeActivity, "No book found with ISBN: $scannedISBN", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Body is null
                        hideLoading()
                        Log.d("ISBN_SEARCH", "Response body is null")
                        Toast.makeText(this@HomeActivity, "Invalid response from server", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    hideLoading()
                    Log.d("ISBN_SEARCH", "Response not successful: ${response.code()}")
                    Toast.makeText(this@HomeActivity, "Failed to load books. Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                t.printStackTrace()
                hideLoading()
                Log.d("ISBN_SEARCH", "Network failure: ${t.message}")
                Toast.makeText(this@HomeActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openBookDetailsDirectly(volume: Volume) {
        // Get the categories from the volume directly (no additional API call)
        val categories = volume.volumeInfo.categories ?: emptyList()
        val genreString = if (categories is List<*>) {
            categories.filterIsInstance<String>().joinToString(", ")
        } else if (categories is String) {
            categories
        } else {
            ""
        }

        // Get image URL directly from the volume
        val imageUrl = getImageUrlFromVolume(volume)

        // Create intent and add extras
        val intent = Intent(this@HomeActivity, ViewBookActivity::class.java)
        intent.putExtra(ViewBookActivity.ID_KEY, volume.id)
        intent.putExtra(ViewBookActivity.TITLE_KEY, volume.volumeInfo.title ?: "")
        intent.putExtra(ViewBookActivity.AUTHOR_KEY, volume.volumeInfo.authors?.joinToString(", ") ?: "")
        intent.putExtra(ViewBookActivity.DESCRIPTION_KEY, volume.volumeInfo.description ?: "")
        intent.putExtra(ViewBookActivity.AVG_RATING_KEY, volume.volumeInfo.averageRating ?: 0.0)
        intent.putExtra(ViewBookActivity.RATING_COUNT_KEY, volume.volumeInfo.ratingsCount ?: 0)
        intent.putExtra(ViewBookActivity.GENRE_KEY, genreString)
        intent.putExtra(ViewBookActivity.IMAGE_URL, imageUrl)

        // Start the activity immediately (no waiting for additional API calls)
        startActivity(intent)
    }

    private fun getImageUrlFromVolume(volume: Volume): String? {
        val imageLinks = volume.volumeInfo?.imageLinks ?: return null

        // Try different image sizes in order of preference
        val baseUrl = imageLinks.thumbnail?.takeIf { it.isNotEmpty() }
            ?: imageLinks.smallThumbnail?.takeIf { it.isNotEmpty() }
            ?: imageLinks.medium?.takeIf { it.isNotEmpty() }
            ?: imageLinks.large?.takeIf { it.isNotEmpty() }
            ?: imageLinks.extraLarge?.takeIf { it.isNotEmpty() }
            ?: imageLinks.small?.takeIf { it.isNotEmpty() }
            ?: return null

        return enhanceGoogleBooksUrl(baseUrl)
    }

    private fun enhanceGoogleBooksUrl(url: String): String {
        var enhancedUrl = url.replace("http://", "https://")

        // Remove common unwanted parameters
        enhancedUrl = enhancedUrl.replace("&edge=curl", "")

        // Handle Google Books specific URLs
        when {
            enhancedUrl.contains("googleapis.com") -> {
                // For Google Books API images
                enhancedUrl = enhancedUrl.replace("zoom=1", "zoom=2")
                enhancedUrl = enhancedUrl.replace("imgmax=128", "imgmax=512")

                // Ensure we have proper parameters
                if (!enhancedUrl.contains("imgmax=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&imgmax=512" else "?imgmax=512"
                }
                if (!enhancedUrl.contains("zoom=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&zoom=2" else "?zoom=2"
                }
            }

            enhancedUrl.contains("books.google.com") -> {
                // For direct Google Books links
                enhancedUrl = enhancedUrl.replace("&printsec=frontcover", "&printsec=frontcover&img=1&zoom=2")

                // Add missing parameters for better quality
                if (!enhancedUrl.contains("img=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&img=1" else "?img=1"
                }
                if (!enhancedUrl.contains("zoom=")) {
                    enhancedUrl += "&zoom=2"
                }
            }

            enhancedUrl.contains("gstatic.com") -> {
                // No changes needed for gstatic.com URLs
            }
        }

        return enhancedUrl
    }

    private fun processCategories(categoriesData: Any?): List<String> {
        return when (categoriesData) {
            is List<*> -> {
                categoriesData.filterIsInstance<String>()
            }
            is String -> {
                if (categoriesData.contains(",")) {
                    categoriesData.split(",").map { it.trim() }
                } else {
                    listOf(categoriesData.trim())
                }
            }
            else -> emptyList()
        }
    }

    private fun setupClickListeners() {
        binding.navScanBtn.setOnClickListener {
            openScanner()
        }

        binding.navSavedBtn.setOnClickListener {
            val intent = Intent(this, SavedListsActivity::class.java)
            startActivity(intent)
        }

        binding.navProfileBtn.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    private fun searchBookByIsbn(isbn: String) {
        if (isbn.isNotEmpty()) {
            scannedISBN = isbn
            Toast.makeText(this, "Searching for ISBN: $isbn", Toast.LENGTH_SHORT).show()

            // Clean the ISBN - remove spaces and dashes
            val cleanISBN = isbn.replace(" ", "").replace("-", "")

            // Try multiple ISBN search formats
            tryMultipleISBNFormats(cleanISBN)
        } else {
            Toast.makeText(this, "Invalid ISBN scanned", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryMultipleISBNFormats(cleanISBN: String) {
        // Create a list of different ISBN formats to try
        val isbnFormats = mutableListOf<String>()

        // Check if it's a valid ISBN-10 or ISBN-13
        val isbnLength = cleanISBN.length

        when {
            isbnLength == 10 -> {
                // Could be ISBN-10
                isbnFormats.add("isbn:$cleanISBN")
                // Try converting to ISBN-13
                val isbn13 = convertISBN10ToISBN13(cleanISBN)
                if (isbn13 != null) {
                    isbnFormats.add("isbn:$isbn13")
                }
            }
            isbnLength == 13 -> {
                // Could be ISBN-13
                isbnFormats.add("isbn:$cleanISBN")
                // Check if it starts with 978 (common ISBN-13 prefix for books)
                if (cleanISBN.startsWith("978")) {
                    // Try converting to ISBN-10
                    val isbn10 = convertISBN13ToISBN10(cleanISBN)
                    if (isbn10 != null) {
                        isbnFormats.add("isbn:$isbn10")
                    }
                }
            }
            else -> {
                // Not a standard ISBN length, but try anyway
                isbnFormats.add("isbn:$cleanISBN")
            }
        }

        // Also try searching without the "isbn:" prefix as a regular search
        isbnFormats.add("\"$cleanISBN\"")

        Log.d("ISBN_SEARCH", "Trying ISBN formats: $isbnFormats")

        // Try the first format
        if (isbnFormats.isNotEmpty()) {
            tryISBNFormatWithFallback(isbnFormats, 0)
        }
    }

    private fun tryISBNFormatWithFallback(formats: List<String>, index: Int) {
        if (index >= formats.size) {
            // All formats tried and failed
            adapter.updateData(emptyList())
            val genres = extractGenresFromBooks(emptyList())
            genreAdapter.updateGenres(genres)
            Toast.makeText(this@HomeActivity, "No book found with ISBN: $scannedISBN", Toast.LENGTH_LONG).show()
            return
        }

        val currentFormat = formats[index]
        Log.d("ISBN_SEARCH", "Trying format $index: $currentFormat")

        showLoading()
        val call = apiInterface.searchBooks(currentFormat, 1, "books")
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("ISBN_SEARCH", "Format $currentFormat: totalItems = ${body.totalItems}")

                        val items = body.items
                        if (items != null && items.isNotEmpty()) {
                            // Success! Found book with this format
                            allBooks = items
                            adapter.updateData(items)

                            val genres = extractGenresFromBooks(items)
                            genreAdapter.updateGenres(genres)

                            val volume = items[0]
                            Log.d("ISBN_SEARCH", "Found book with format $currentFormat: ${volume.volumeInfo.title}")
                            openBookDetailsDirectly(volume)
                            hideLoading()
                        } else if (body.totalItems > 0) {
                            // Items is null but totalItems > 0, try next format
                            Log.d("ISBN_SEARCH", "Format $currentFormat has totalItems > 0 but no items, trying next format")
                            hideLoading()
                            tryISBNFormatWithFallback(formats, index + 1)
                        } else {
                            // No results with this format, try next
                            Log.d("ISBN_SEARCH", "Format $currentFormat returned 0 results")
                            hideLoading()
                            tryISBNFormatWithFallback(formats, index + 1)
                        }
                    } else {
                        hideLoading()
                        tryISBNFormatWithFallback(formats, index + 1)
                    }
                } else {
                    hideLoading()
                    tryISBNFormatWithFallback(formats, index + 1)
                }
            }

            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                hideLoading()
                Log.d("ISBN_SEARCH", "Format $currentFormat failed: ${t.message}")
                tryISBNFormatWithFallback(formats, index + 1)
            }
        })
    }

    private fun convertISBN10ToISBN13(isbn10: String): String? {
        // Check if it's a valid ISBN-10
        if (isbn10.length != 10) return null

        // Add 978 prefix and remove check digit
        val isbnWithoutCheck = "978" + isbn10.substring(0, 9)

        // Calculate new check digit for ISBN-13
        var sum = 0
        for (i in isbnWithoutCheck.indices) {
            val digit = isbnWithoutCheck[i].digitToIntOrNull() ?: return null
            sum += if (i % 2 == 0) digit else digit * 3
        }

        val checkDigit = (10 - (sum % 10)) % 10
        return isbnWithoutCheck + checkDigit
    }

    private fun convertISBN13ToISBN10(isbn13: String): String? {
        // Check if it's a valid ISBN-13 starting with 978
        if (isbn13.length != 13 || !isbn13.startsWith("978")) return null

        // Remove 978 prefix and check digit
        val isbnWithoutPrefix = isbn13.substring(3, 12)

        // Calculate check digit for ISBN-10
        var sum = 0
        for (i in isbnWithoutPrefix.indices) {
            val digit = isbnWithoutPrefix[i].digitToIntOrNull() ?: return null
            sum += digit * (10 - i)
        }

        val remainder = sum % 11
        val checkDigit = when (remainder) {
            0 -> '0'
            1 -> 'X'
            else -> (11 - remainder).toString().first()
        }

        return isbnWithoutPrefix + checkDigit
    }

    private fun getApiInterface() {
        apiInterface = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)
    }
}