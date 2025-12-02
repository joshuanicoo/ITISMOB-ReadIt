package com.mobdeve.s17.group39.itismob_mco.features.homepage

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
                    allBooks = data.items // Store original books
                    adapter.updateData(data.items)

                    // Extract and display genres from the books
                    val genres = extractGenresFromBooks(data.items)
                    genreAdapter.updateGenres(genres)

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
                    allBooks = data.items // Store original search results

                    // Extract and update genres from search results
                    val genres = extractGenresFromBooks(data.items)
                    genreAdapter.updateGenres(genres)

                    // Apply genre filter if one is selected (from previous state)
                    if (currentGenre != null && isFilteringFromSearch) {
                        filterBooksByGenre(currentGenre!!)
                    } else {
                        adapter.updateData(data.items)
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

    private fun searchByISBN(query: String){
        showLoading()
        val call = apiInterface.getBookByISBN(query)
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    data = response.body()!!
                    allBooks = data.items // Store original results
                    adapter.updateData(data.items)

                    // Extract and display genres from the book(s)
                    val genres = extractGenresFromBooks(data.items)
                    genreAdapter.updateGenres(genres)

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
            Toast.makeText(this, "Searching for ISBN: $isbn", Toast.LENGTH_SHORT).show()
            val concatISBN = "isbn:$isbn"
            searchByISBN(concatISBN)
        } else {
            Toast.makeText(this, "Invalid ISBN scanned", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getApiInterface() {
        apiInterface = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)
    }
}