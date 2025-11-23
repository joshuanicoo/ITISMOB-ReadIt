package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.SavedListsLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.NewListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ListModel
import com.mobdeve.s17.group39.itismob_mco.utils.SharedPrefsManager

class SavedListsActivity : AppCompatActivity() {

    private lateinit var binding: SavedListsLayoutBinding
    private lateinit var adapter: SavedListsAdapter
    private var listsListener: ListenerRegistration? = null
    private val sharedPrefs = SharedPrefsManager(this)

    // Get current user ID safely
    private val currentUserId: String
        get() = sharedPrefs.getCurrentUserId() ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SavedListsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view your lists", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupClickListeners()
        setupRealTimeListener()
    }

    private fun setupRecyclerView() {
        adapter = SavedListsAdapter(emptyList()) { list ->
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

    private fun setupRealTimeListener() {
        listsListener = ListsDatabase.getAll()
            .whereEqualTo("user_id", currentUserId)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading lists: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val lists = querySnapshot?.documents?.mapNotNull { document ->
                    try {
                        ListModel.fromMap(document.id, document.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                updateUI(lists)
            }
    }

    private fun updateUI(lists: List<ListModel>) {
        val savedLists = lists.map { list ->
            SavedList(list.listName, list.books.size, list.id)
        }
        adapter.updateData(savedLists)

        // Check if we have the emptyStateTv in layout, if not use alternative approach
        if (lists.isEmpty()) {
            // If emptyStateTv exists, use it
            try {
                binding.emptyStateTv.visibility = android.view.View.VISIBLE
                binding.savedListsRv.visibility = android.view.View.GONE
            } catch (e: Exception) {
                // If emptyStateTv doesn't exist, show a toast
                Toast.makeText(this, "You don't have any lists yet", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                binding.emptyStateTv.visibility = android.view.View.GONE
                binding.savedListsRv.visibility = android.view.View.VISIBLE
            } catch (e: Exception) {
                // emptyStateTv doesn't exist, just show the RecyclerView
                binding.savedListsRv.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun openBooksInList(savedList: SavedList) {
        val intent = Intent(this, BooksInListActivity::class.java)
        intent.putExtra(BooksInListActivity.LIST_NAME_KEY, savedList.name)
        intent.putExtra(BooksInListActivity.LIST_ID_KEY, savedList.id)
        intent.putExtra(BooksInListActivity.BOOK_COUNT_KEY, savedList.bookCount)
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
                createNewList(listName, dialog)
            } else {
                Toast.makeText(this, "Please enter a list name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelNewListBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createNewList(listName: String, dialog: Dialog) {
        ListsDatabase.createList(listName, currentUserId)
            .addOnSuccessListener {
                Toast.makeText(this, "List '$listName' created successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to create list: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listsListener?.remove()
    }
}

data class SavedList(
    val name: String,
    val bookCount: Int,
    val id: String = ""
)