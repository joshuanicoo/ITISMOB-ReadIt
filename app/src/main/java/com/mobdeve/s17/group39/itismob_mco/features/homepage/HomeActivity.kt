package com.mobdeve.s17.group39.itismob_mco.features.homepage

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
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
import com.mobdeve.s17.group39.itismob_mco.utils.QuoteApiInterface
import com.mobdeve.s17.group39.itismob_mco.utils.QuoteResponse
import com.mobdeve.s17.group39.itismob_mco.utils.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

@ExperimentalGetImage
class HomeActivity : AppCompatActivity() {
    private lateinit var data: GoogleBooksResponse
    private lateinit var binding: HomeActivityBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface
    private lateinit var quoteApiInterface: QuoteApiInterface
    private lateinit var handler: Handler

    private lateinit var genreAdapter: HomeGenreAdapter

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
                    searchBooks(searchText, 40, "books")
                } else {
                    getBooks()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                val searchText = query?.lowercase(Locale.getDefault()) ?: ""
                handler.removeCallbacksAndMessages(null)
                if (searchText.isNotEmpty()) {
                    handler.postDelayed({
                        searchBooks(searchText, 40, "books")
                    }, 500)
                } else {
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

        var genreString = "Fiction, Mystery, Romance, Science Fiction, Fantasy, Thriller, Biography, History, Horror, Young Adult, Comedy, Adventure"
        val dataGenre = ArrayList<String>()
        if (!genreString.isNullOrEmpty()) {
            val genres = genreString.split(",").map { it.trim() }
            dataGenre.addAll(genres)
        }
        this.genreAdapter = HomeGenreAdapter(dataGenre)
        this.binding.genreListRv.adapter = genreAdapter
        this.binding.genreListRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun showLoading() {
        binding.loadingContainer.visibility = View.VISIBLE // Show both quote and loading
        binding.bookListRv.visibility = View.GONE
        getRandomQuote()
    }

    private fun hideLoading() {
        binding.loadingContainer.visibility = View.GONE // Hide both quote and loading
        binding.bookListRv.visibility = View.VISIBLE
    }

    private fun getRandomQuote() {
        val call = quoteApiInterface.getRandomQuote()
        call.enqueue(object : Callback<List<QuoteResponse>> {
            override fun onResponse(call: Call<List<QuoteResponse>>, response: Response<List<QuoteResponse>>) {
                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    val quote = response.body()!![0]
                    displayQuote(quote.content, quote.author)
                } else {
                    // Fallback quotes in case API fails
                    displayFallbackQuote()
                }
            }

            override fun onFailure(call: Call<List<QuoteResponse>>, t: Throwable) {
                // Fallback quotes in case of network failure
                displayFallbackQuote()
            }
        })
    }

    private fun displayQuote(content: String, author: String) {
        binding.quoteText.text = "\"$content\""
        binding.quoteAuthor.text = "- $author"
    }

    private fun displayFallbackQuote() {
        val fallbackQuotes = listOf(
            Pair("The only way to do great work is to love what you do.", "Steve Jobs"),
            Pair("It does not do to dwell on dreams and forget to live.", "J.K. Rowling"),
            Pair("That's the thing about books. They let you travel without moving your feet.", "Jhumpa Lahiri"),
            Pair("Reading is essential for those who seek to rise above the ordinary.", "Jim Rohn"),
            Pair("A room without books is like a body without a soul.", "Marcus Tullius Cicero")
        )

        val randomQuote = fallbackQuotes.random()
        binding.quoteText.text = "\"${randomQuote.first}\""
        binding.quoteAuthor.text = "- ${randomQuote.second}"
    }

    private fun getBooks() {
        val call = apiInterface.getBooks()
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    data = response.body()!!
                    adapter.updateData(data.items)
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
        showLoading() // Show loading with new quote when searching
        val call = apiInterface.searchBooks(query, size, printType)
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    data = response.body()!!
                    adapter.updateData(data.items)
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
        showLoading() // Show loading with new quote when searching by ISBN
        val call = apiInterface.getBookByISBN(query)
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    data = response.body()!!
                    adapter.updateData(data.items)
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
            var concatISBN = "isbn:$isbn"
            searchByISBN(concatISBN)
        } else {
            Toast.makeText(this, "Invalid ISBN scanned", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getApiInterface() {
        apiInterface = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)

        val quoteRetrofit = Retrofit.Builder()
            .baseUrl("https://api.quotable.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        quoteApiInterface = quoteRetrofit.create(QuoteApiInterface::class.java)
    }
}