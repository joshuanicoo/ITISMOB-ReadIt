package com.mobdeve.s17.group39.itismob_mco.features.homepage

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import kotlin.collections.HashSet
import android.widget.AutoCompleteTextView
import android.view.View
import android.graphics.Color
import android.view.animation.AnimationUtils

@ExperimentalGetImage
class HomeActivity : AppCompatActivity() {
    private lateinit var data: GoogleBooksResponse
    private lateinit var binding: HomeActivityBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface
    private lateinit var handler: Handler
    private lateinit var genreAdapter: HomeGenreAdapter
    private var allBooks: MutableList<Volume> = mutableListOf()
    private var currentQuery: String = ""
    private var currentGenre: String? = null
    private var selectedGenrePosition: Int = 0
    private var isFilteringFromSearch: Boolean = false
    private var scannedISBN: String? = null
    private var currentStartIndex: Int = 0
    private var maxResults: Int = 40
    private var totalItems: Int = 0
    private var hasMoreBooks: Boolean = true
    private var isSearching: Boolean = false
    private var isScannedSearch: Boolean = false
    private var isLoadingMore: Boolean = false
    private var isLastPage: Boolean = false
    private var lastVisibleItemPosition: Int = 0
    private var totalItemCount: Int = 0
    private var scrollThreshold: Int = 10
    private var isFabVisible: Boolean = false

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
        setupScrollUpButton()
        showLoading()
        getBooks(0)

        handler = Handler(Looper.getMainLooper())

        this.binding.bookSv.clearFocus()
        binding.bookSv.isFocusable = true
        binding.bookSv.isClickable = true

        this.binding.bookSv.setOnClickListener { view ->
            binding.bookSv.onActionViewExpanded()
        }
        val searchPlate = binding.bookSv.findViewById<View>(
            androidx.appcompat.R.id.search_plate
        )
        searchPlate.setBackgroundColor(Color.TRANSPARENT)
        val searchAutoComplete = binding.bookSv.findViewById<AutoCompleteTextView>(
            androidx.appcompat.R.id.search_src_text
        )
        searchAutoComplete.background = null

        this.binding.bookSv.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchText = query?.trim() ?: ""
                if (searchText.isNotEmpty()) {
                    handler.removeCallbacksAndMessages(null)
                    currentQuery = searchText
                    currentGenre = null
                    selectedGenrePosition = 0
                    genreAdapter.setSelectedPosition(0)
                    isSearching = true
                    isScannedSearch = false
                    resetPagination()

                    if (isLikelyISBN(searchText)) {
                        Toast.makeText(this@HomeActivity, "Searching by ISBN...", Toast.LENGTH_SHORT).show()
                    }

                    searchBooks(searchText, 0)
                } else {
                    currentQuery = ""
                    isSearching = false
                    resetPagination()
                    getBooks(0)
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                val searchText = query?.trim() ?: ""
                handler.removeCallbacksAndMessages(null)
                if (searchText.isNotEmpty()) {
                    handler.postDelayed({
                        currentQuery = searchText
                        currentGenre = null
                        selectedGenrePosition = 0
                        genreAdapter.setSelectedPosition(0)
                        isSearching = true
                        isScannedSearch = false
                        resetPagination()
                        searchBooks(searchText, 0)
                    }, 500)
                } else {
                    currentQuery = ""
                    isSearching = false
                    resetPagination()
                    getBooks(0)
                }
                return true
            }
        })
    }

    private fun setupScrollUpButton() {
        hideScrollUpButton()
        binding.fabScrollUp.setOnClickListener {
            scrollToTop()
        }
    }

    private fun showScrollUpButton() {
        if (!isFabVisible) {
            isFabVisible = true
            binding.fabScrollUp.visibility = View.VISIBLE
            binding.fabScrollUp.startAnimation(
                AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            )
        }
    }

    private fun hideScrollUpButton() {
        if (isFabVisible) {
            isFabVisible = false
            binding.fabScrollUp.startAnimation(
                AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
            )
            binding.fabScrollUp.visibility = View.GONE
        }
    }

    private fun scrollToTop() {
        binding.bookListRv.smoothScrollToPosition(0)
        binding.genreListRv.smoothScrollToPosition(0)
        Handler(Looper.getMainLooper()).postDelayed({
            hideScrollUpButton()
        }, 300)
    }

    private fun setupRecyclerView() {
        this.adapter = HomeAdapter(mutableListOf())
        this.binding.bookListRv.adapter = adapter

        val layoutManager = GridLayoutManager(this, 2)
        this.binding.bookListRv.layoutManager = layoutManager

        // CRITICAL: Disable item animations to prevent flickering
        this.binding.bookListRv.itemAnimator = null

        // Also disable layout transitions
        this.binding.bookListRv.setHasFixedSize(true)

        // Set up scroll listener for infinite scrolling and scroll up button
        this.binding.bookListRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                lastVisibleItemPosition = firstVisibleItemPosition
                this@HomeActivity.totalItemCount = totalItemCount
                if (firstVisibleItemPosition > scrollThreshold) {
                    showScrollUpButton()
                } else {
                    hideScrollUpButton()
                }
                if (!isLoadingMore && hasMoreBooks) {
                    if ((visibleItemCount + firstVisibleItemPosition + 5) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 10) {

                        loadMoreBooks()
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItemPosition == 0) {
                    hideScrollUpButton()
                }
            }
        })
    }
    private fun loadMoreBooks() {
        if (isLoadingMore || isLastPage) return

        isLoadingMore = true
        adapter.setLoading(true)

        currentStartIndex += maxResults

        if (isSearching) {
            searchBooks(currentQuery, currentStartIndex)
        } else {
            getBooks(currentStartIndex)
        }
    }

    private fun resetPagination() {
        allBooks.clear()
        currentStartIndex = 0
        totalItems = 0
        hasMoreBooks = true
        isLastPage = false
        isLoadingMore = false
        adapter.setLoading(false)
        binding.bookListRv.scrollToPosition(0)
        hideScrollUpButton()
    }

    private fun isLikelyISBN(query: String): Boolean {
        val cleanQuery = query.replace(" ", "").replace("-", "").replace(".", "")

        val isbn10Pattern = Regex("^\\d{9}[\\dX]\$", RegexOption.IGNORE_CASE)
        val isbn13Pattern = Regex("^\\d{13}\$")
        val isbnWithPrefixPattern = Regex("^(isbn:|ISBN:)?\\s*\\d{9}[\\dX]\$", RegexOption.IGNORE_CASE)
        val isbn13WithPrefixPattern = Regex("^(isbn:|ISBN:)?\\s*\\d{13}\$", RegexOption.IGNORE_CASE)

        return isbn10Pattern.matches(cleanQuery) ||
                isbn13Pattern.matches(cleanQuery) ||
                isbnWithPrefixPattern.matches(cleanQuery) ||
                isbn13WithPrefixPattern.matches(cleanQuery)
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
            adapter.updateData(allBooks)
        } else {
            currentGenre = genre
            isFilteringFromSearch = (currentQuery.isNotEmpty())
            filterBooksByGenre(genre)
        }
    }

    private fun filterBooksByGenre(genre: String) {
        val filteredBooks = allBooks.filter { volume ->
            hasMatchingGenre(volume, genre)
        }
        adapter.updateData(filteredBooks)
        if (filteredBooks.isEmpty()) {
            Toast.makeText(this, "No books found in $genre category", Toast.LENGTH_SHORT).show()
        }

        // Hide scroll up button when filtering
        hideScrollUpButton()
    }

    private fun hasMatchingGenre(volume: Volume, genre: String): Boolean {
        return volume.volumeInfo.categories?.any { category ->
            category.split("/", ",").any { it ->
                it.trim().equals(genre, ignoreCase = true)
            }
        } ?: false
    }

    private fun extractGenresFromBooks(books: List<Volume>): List<String> {
        val genresSet = HashSet<String>()

        books.forEach { volume ->
            volume.volumeInfo.categories?.forEach { category ->
                category.split("/", ",").forEach { genrePart ->
                    val trimmedGenre = genrePart.trim()
                    if (trimmedGenre.isNotEmpty() && !trimmedGenre.equals("General", ignoreCase = true)) {
                        genresSet.add(trimmedGenre)
                    }
                }
            }
        }
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

    private fun getBooks(startIndex: Int) {
        if (startIndex == 0) {
            showLoading()
        }

        val call = apiInterface.getBooks(startIndex)
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                handleResponse(response, startIndex)
            }

            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                t.printStackTrace()
                if (startIndex == 0) {
                    hideLoading()
                } else {
                    isLoadingMore = false
                    adapter.setLoading(false)
                }
                Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchBooks(query: String, startIndex: Int) {
        if (startIndex == 0) {
            showLoading()
        }

        if (isLikelyISBN(query) && !isScannedSearch) {
            val cleanISBN = query.replace(" ", "").replace("-", "").replace(".", "")
                .replace("isbn:", "", ignoreCase = true)
                .replace("ISBN:", "", ignoreCase = true)

            searchByISBNDedicated(cleanISBN, startIndex)
        } else {
            val call = apiInterface.searchBooks(query, maxResults, "books", startIndex)
            call.enqueue(object : Callback<GoogleBooksResponse> {
                override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                    handleResponse(response, startIndex)
                }

                override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                    t.printStackTrace()
                    if (startIndex == 0) {
                        hideLoading()
                    } else {
                        isLoadingMore = false
                        adapter.setLoading(false)
                    }
                    Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun handleResponse(response: Response<GoogleBooksResponse>, startIndex: Int) {
        if (response.isSuccessful && response.body() != null) {
            data = response.body()!!
            totalItems = data.totalItems

            val items = data.items
            if (items != null && items.isNotEmpty()) {
                if (startIndex == 0) {
                    allBooks.clear()
                }
                allBooks.addAll(items)

                // Check if there are more books to load
                hasMoreBooks = (startIndex + items.size) < totalItems
                isLastPage = !hasMoreBooks

                val genres = extractGenresFromBooks(allBooks)
                genreAdapter.updateGenres(genres)

                if (currentGenre != null && isFilteringFromSearch) {
                    filterBooksByGenre(currentGenre!!)
                } else {
                    adapter.updateData(allBooks)
                }
            } else {
                if (startIndex == 0) {
                    allBooks.clear()
                    adapter.updateData(emptyList())
                    genreAdapter.updateGenres(listOf("All"))
                    Toast.makeText(this@HomeActivity, "No books found", Toast.LENGTH_SHORT).show()
                }
            }

            if (startIndex == 0) {
                hideLoading()
            }
        } else {
            if (startIndex == 0) {
                hideLoading()
            }
            Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
        }

        // Reset loading state for infinite scroll
        if (startIndex > 0) {
            isLoadingMore = false
            adapter.setLoading(false)
        }
    }

    private fun searchByISBNDedicated(isbn: String, startIndex: Int) {
        Log.d("ISBN_SEARCH", "Searching by dedicated ISBN: $isbn, startIndex: $startIndex")

        val call = apiInterface.getBookByISBN(isbn, 1, "books", startIndex)
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("ISBN_SEARCH", "Dedicated ISBN search response. Total items: ${body.totalItems}")

                        val items = body.items
                        if (items != null && items.isNotEmpty()) {
                            allBooks = items.toMutableList()
                            val genres = extractGenresFromBooks(items)
                            genreAdapter.updateGenres(genres)
                            adapter.updateData(items)
                            if (startIndex == 0) {
                                hideLoading()
                            }
                            return
                        }

                        Log.d("ISBN_SEARCH", "Dedicated search returned no items, trying fallback")
                        searchBooksWithFallbackFormats(isbn, startIndex)
                    } else {
                        if (startIndex == 0) {
                            hideLoading()
                        }
                        searchBooksWithFallbackFormats(isbn, startIndex)
                    }
                } else {
                    if (startIndex == 0) {
                        hideLoading()
                    }
                    searchBooksWithFallbackFormats(isbn, startIndex)
                }
            }

            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                if (startIndex == 0) {
                    hideLoading()
                }
                Log.d("ISBN_SEARCH", "Dedicated ISBN search failed: ${t.message}")
                searchBooksWithFallbackFormats(isbn, startIndex)
            }
        })
    }

    private fun searchBooksWithFallbackFormats(isbn: String, startIndex: Int) {
        val formats = mutableListOf<String>()

        formats.add("isbn:$isbn")
        formats.add("ISBN:$isbn")

        if (isbn.length == 10) {
            val isbn13 = convertISBN10ToISBN13(isbn)
            if (isbn13 != null) {
                formats.add("isbn:$isbn13")
            }
        } else if (isbn.length == 13 && isbn.startsWith("978")) {
            val isbn10 = convertISBN13ToISBN10(isbn)
            if (isbn10 != null) {
                formats.add("isbn:$isbn10")
            }
        }

        formats.add(isbn)

        Log.d("ISBN_SEARCH", "Trying fallback formats: $formats")
        tryISBNFormatWithFallback(formats, 0, startIndex)
    }

    private fun tryISBNFormatWithFallback(formats: List<String>, index: Int, startIndex: Int) {
        if (index >= formats.size) {
            allBooks.clear()
            adapter.updateData(emptyList())
            val genres = extractGenresFromBooks(emptyList())
            genreAdapter.updateGenres(genres)
            if (startIndex == 0) {
                hideLoading()
            }
            Toast.makeText(this@HomeActivity, "No book found with ISBN: $scannedISBN", Toast.LENGTH_LONG).show()
            return
        }

        val currentFormat = formats[index]
        Log.d("ISBN_SEARCH", "Trying format $index: $currentFormat, startIndex: $startIndex")

        if (startIndex == 0) {
            showLoading()
        }

        val call = apiInterface.searchBooks(currentFormat, 1, "books", startIndex)
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("ISBN_SEARCH", "Format $currentFormat: totalItems = ${body.totalItems}")

                        val items = body.items
                        if (items != null && items.isNotEmpty()) {
                            allBooks = items.toMutableList()
                            adapter.updateData(items)

                            val genres = extractGenresFromBooks(items)
                            genreAdapter.updateGenres(genres)

                            val volume = items[0]
                            Log.d("ISBN_SEARCH", "Found book with format $currentFormat: ${volume.volumeInfo.title}")

                            if (isScannedSearch) {
                                openBookDetailsDirectly(volume)
                            } else {
                                Toast.makeText(this@HomeActivity, "Found: ${volume.volumeInfo.title}", Toast.LENGTH_SHORT).show()
                            }
                            if (startIndex == 0) {
                                hideLoading()
                            }
                        } else if (body.totalItems > 0) {
                            Log.d("ISBN_SEARCH", "Format $currentFormat has totalItems > 0 but no items, trying next format")
                            if (startIndex == 0) {
                                hideLoading()
                            }
                            tryISBNFormatWithFallback(formats, index + 1, startIndex)
                        } else {
                            Log.d("ISBN_SEARCH", "Format $currentFormat returned 0 results")
                            if (startIndex == 0) {
                                hideLoading()
                            }
                            tryISBNFormatWithFallback(formats, index + 1, startIndex)
                        }
                    } else {
                        if (startIndex == 0) {
                            hideLoading()
                        }
                        tryISBNFormatWithFallback(formats, index + 1, startIndex)
                    }
                } else {
                    if (startIndex == 0) {
                        hideLoading()
                    }
                    tryISBNFormatWithFallback(formats, index + 1, startIndex)
                }
            }

            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                if (startIndex == 0) {
                    hideLoading()
                }
                Log.d("ISBN_SEARCH", "Format $currentFormat failed: ${t.message}")
                tryISBNFormatWithFallback(formats, index + 1, startIndex)
            }
        })
    }

    private fun openBookDetailsDirectly(volume: Volume) {
        val categories = volume.volumeInfo.categories ?: emptyList()
        val genreString = if (categories is List<*>) {
            categories.filterIsInstance<String>().joinToString(", ")
        } else if (categories is String) {
            categories
        } else {
            ""
        }
        val imageUrl = getImageUrlFromVolume(volume)
        val intent = Intent(this@HomeActivity, ViewBookActivity::class.java)
        intent.putExtra(ViewBookActivity.ID_KEY, volume.id)
        intent.putExtra(ViewBookActivity.TITLE_KEY, volume.volumeInfo.title ?: "")
        intent.putExtra(ViewBookActivity.AUTHOR_KEY, volume.volumeInfo.authors?.joinToString(", ") ?: "")
        intent.putExtra(ViewBookActivity.DESCRIPTION_KEY, volume.volumeInfo.description ?: "")
        intent.putExtra(ViewBookActivity.AVG_RATING_KEY, volume.volumeInfo.averageRating ?: 0.0)
        intent.putExtra(ViewBookActivity.RATING_COUNT_KEY, volume.volumeInfo.ratingsCount ?: 0)
        intent.putExtra(ViewBookActivity.GENRE_KEY, genreString)
        intent.putExtra(ViewBookActivity.IMAGE_URL, imageUrl)
        startActivity(intent)
    }

    private fun getImageUrlFromVolume(volume: Volume): String? {
        val imageLinks = volume.volumeInfo?.imageLinks ?: return null
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
        enhancedUrl = enhancedUrl.replace("&edge=curl", "")
        when {
            enhancedUrl.contains("googleapis.com") -> {
                enhancedUrl = enhancedUrl.replace("zoom=1", "zoom=2")
                enhancedUrl = enhancedUrl.replace("imgmax=128", "imgmax=512")
                if (!enhancedUrl.contains("imgmax=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&imgmax=512" else "?imgmax=512"
                }
                if (!enhancedUrl.contains("zoom=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&zoom=2" else "?zoom=2"
                }
            }

            enhancedUrl.contains("books.google.com") -> {
                enhancedUrl = enhancedUrl.replace("&printsec=frontcover", "&printsec=frontcover&img=1&zoom=2")
                if (!enhancedUrl.contains("img=")) {
                    enhancedUrl += if (enhancedUrl.contains("?")) "&img=1" else "?img=1"
                }
                if (!enhancedUrl.contains("zoom=")) {
                    enhancedUrl += "&zoom=2"
                }
            }

            enhancedUrl.contains("gstatic.com") -> {
            }
        }

        return enhancedUrl
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
            isScannedSearch = true
            resetPagination()
            Toast.makeText(this, "Searching for ISBN: $isbn", Toast.LENGTH_SHORT).show()
            val cleanISBN = isbn.replace(" ", "").replace("-", "")
            tryMultipleISBNFormats(cleanISBN, 0)
        } else {
            Toast.makeText(this, "Invalid ISBN scanned", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryMultipleISBNFormats(cleanISBN: String, startIndex: Int) {
        val isbnFormats = mutableListOf<String>()
        val isbnLength = cleanISBN.length

        when {
            isbnLength == 10 -> {
                isbnFormats.add("isbn:$cleanISBN")
                val isbn13 = convertISBN10ToISBN13(cleanISBN)
                if (isbn13 != null) {
                    isbnFormats.add("isbn:$isbn13")
                }
            }
            isbnLength == 13 -> {
                isbnFormats.add("isbn:$cleanISBN")
                if (cleanISBN.startsWith("978")) {
                    val isbn10 = convertISBN13ToISBN10(cleanISBN)
                    if (isbn10 != null) {
                        isbnFormats.add("isbn:$isbn10")
                    }
                }
            }
            else -> {
                isbnFormats.add("isbn:$cleanISBN")
            }
        }
        isbnFormats.add("\"$cleanISBN\"")

        Log.d("ISBN_SEARCH", "Trying ISBN formats: $isbnFormats")
        if (isbnFormats.isNotEmpty()) {
            tryISBNFormatWithFallback(isbnFormats, 0, startIndex)
        }
    }

    private fun convertISBN10ToISBN13(isbn10: String): String? {
        if (isbn10.length != 10) return null
        val isbnWithoutCheck = "978" + isbn10.substring(0, 9)
        var sum = 0
        for (i in isbnWithoutCheck.indices) {
            val digit = isbnWithoutCheck[i].digitToIntOrNull() ?: return null
            sum += if (i % 2 == 0) digit else digit * 3
        }

        val checkDigit = (10 - (sum % 10)) % 10
        return isbnWithoutCheck + checkDigit
    }

    private fun convertISBN13ToISBN10(isbn13: String): String? {
        if (isbn13.length != 13 || !isbn13.startsWith("978")) return null
        val isbnWithoutPrefix = isbn13.substring(3, 12)
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