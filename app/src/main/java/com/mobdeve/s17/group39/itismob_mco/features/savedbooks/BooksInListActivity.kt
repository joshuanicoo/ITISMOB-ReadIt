package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.BooksInListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.homepage.HomeAdapter
import com.mobdeve.s17.group39.itismob_mco.features.viewbook.ViewBookActivity
import com.mobdeve.s17.group39.itismob_mco.models.ListModel
import com.mobdeve.s17.group39.itismob_mco.utils.GoogleBooksApiInterface
import com.mobdeve.s17.group39.itismob_mco.utils.ImageUtils
import com.mobdeve.s17.group39.itismob_mco.utils.RetrofitInstance
import com.mobdeve.s17.group39.itismob_mco.utils.Volume

class BooksInListActivity : AppCompatActivity() {

    companion object {
        const val LIST_NAME_KEY = "LIST_NAME_KEY"
        const val LIST_ID_KEY = "LIST_ID_KEY"
        const val BOOK_COUNT_KEY = "BOOK_COUNT_KEY"
    }

    private lateinit var binding: BooksInListLayoutBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface
    private var listListener: ListenerRegistration? = null
    private var currentListId: String = ""

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
        // Get the DocumentReference first, then add snapshot listener
        val documentRef = ListsDatabase.getDocumentReference(currentListId)
        listListener = documentRef.addSnapshotListener { documentSnapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Error loading list: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val list = documentSnapshot.toObject(ListModel::class.java)
                list?.let { updateListUI(it) }
            } else {
                // List doesn't exist or was deleted
                Toast.makeText(this, "List not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateListUI(list: ListModel) {
        binding.listTitleTv.text = list.listName
        binding.bookCountHeaderTv.text = "${list.books.size} ${if (list.books.size == 1) "book" else "books"}"

        if (list.books.isNotEmpty()) {
            // For now, we'll show sample books since we don't have actual book data
            // In a real implementation, you would fetch book details from your BooksDatabase
            showSampleBooks()
        } else {
            showEmptyState()
        }
    }

    private fun showSampleBooks() {
        // Show some sample books for demonstration
        // In a real app, you would fetch actual book data from your database
        hideLoading()
        binding.booksInListRv.visibility = View.VISIBLE
        binding.emptyStateTv.visibility = View.GONE

        // For now, we'll just show an empty adapter since we don't have real book data
        // You would need to implement fetching book details from BooksDatabase
        adapter.updateData(emptyList())
        binding.emptyStateTv.visibility = View.VISIBLE
        binding.booksInListRv.visibility = View.GONE
        binding.emptyStateTv.text = "Book data integration needed"
    }

    private fun showLoading() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.booksInListRv.visibility = View.GONE
        binding.emptyStateTv.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.loadingProgressBar.visibility = View.GONE
    }

    private fun showEmptyState() {
        hideLoading()
        binding.booksInListRv.visibility = View.GONE
        binding.emptyStateTv.visibility = View.VISIBLE
        binding.emptyStateTv.text = "No books in this list yet"
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

    override fun onDestroy() {
        super.onDestroy()
        listListener?.remove()
    }
}