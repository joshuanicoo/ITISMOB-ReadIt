package com.mobdeve.s17.group39.itismob_mco.ui.savedbooks

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s17.group39.itismob_mco.databinding.SavedListsLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.NewListLayoutBinding

class SavedListsActivity : AppCompatActivity() {

    private lateinit var binding: SavedListsLayoutBinding
    private lateinit var adapter: SavedListsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SavedListsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        val sampleLists = arrayListOf(
            SavedList("Currently Reading", 5),
            SavedList("Want to Read", 12),
            SavedList("Read", 8),
            SavedList("Favorites", 3),
            SavedList("To Buy", 7),
            SavedList("Summer Reading", 4),
            SavedList("Classics", 6),
            SavedList("Non-Fiction", 9)
        )

        adapter = SavedListsAdapter(sampleLists) { list ->
            openBooksInList(list)
        }

        binding.savedListsRv.adapter = adapter
        binding.savedListsRv.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        binding.createNewListBtn.setOnClickListener {
            showNewListDialog()
        }
    }

    private fun openBooksInList(list: SavedList) {
        val intent = Intent(this, BooksInListActivity::class.java)
        intent.putExtra(BooksInListActivity.LIST_NAME_KEY, list.name)
        intent.putExtra(BooksInListActivity.BOOK_COUNT_KEY, list.bookCount)
        startActivity(intent)
    }

    private fun showNewListDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        val binding = NewListLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        val heightInPixels = (200 * resources.displayMetrics.density).toInt()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, heightInPixels)

        binding.saveNewListBtn.setOnClickListener {
            val listName = binding.newListNameEt.text.toString().trim()
            if (listName.isNotEmpty()) {
                // Add new list logic here
                Toast.makeText(this, "List '$listName' created", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a list name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelNewListBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}

data class SavedList(
    val name: String,
    val bookCount: Int
)