package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.SavedListsLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.databinding.NewListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ListModel

class SavedListsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SavedListsActivity"
    }

    private lateinit var binding: SavedListsLayoutBinding
    private lateinit var adapter: SavedListsAdapter
    private var listsListener: ListenerRegistration? = null
    private val auth = FirebaseAuth.getInstance()

    // Get current user ID from Firebase Auth
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SavedListsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is logged in
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view your lists", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "Current User ID: $currentUserId")

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

        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun setupClickListeners() {
        binding.createNewListBtn.setOnClickListener {
            showNewListDialog()
        }
    }

    private fun setupRealTimeListener() {
        Log.d(TAG, "Setting up real-time listener for user: $currentUserId")

        listsListener = ListsDatabase.getAll()
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading lists", error)
                    Toast.makeText(this, "Error loading lists: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                Log.d(TAG, "Snapshot received. Document count: ${querySnapshot?.documents?.size ?: 0}")

                val lists = querySnapshot?.documents?.mapNotNull { document ->
                    try {
                        Log.d(TAG, "Processing document: ${document.id}")
                        Log.d(TAG, "Document data: ${document.data}")

                        val list = ListModel.fromMap(document.id, document.data ?: emptyMap())
                        Log.d(TAG, "Parsed list: $list")
                        list
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document ${document.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Total lists parsed: ${lists.size}")
                updateUI(lists)
            }
    }

    private fun updateUI(lists: List<ListModel>) {
        Log.d(TAG, "Updating UI with ${lists.size} lists")

        val savedLists = lists.map { list ->
            SavedList(list.listName, list.books.size, list.documentId)
        }

        adapter.updateData(savedLists)

        if (lists.isEmpty()) {
            Log.d(TAG, "No lists found - showing empty state")
            binding.savedListsRv.visibility = android.view.View.GONE

            // Try to show empty state text if it exists
            try {
                binding.emptyStateTv.visibility = android.view.View.VISIBLE
                binding.emptyStateTv.text = "No lists yet. Create your first list!"
            } catch (e: Exception) {
                Log.w(TAG, "emptyStateTv not found in layout", e)
                Toast.makeText(this, "You don't have any lists yet. Create one!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Showing ${lists.size} lists")
            binding.savedListsRv.visibility = android.view.View.VISIBLE

            try {
                binding.emptyStateTv.visibility = android.view.View.GONE
            } catch (e: Exception) {
                // Empty state view doesn't exist, which is fine
            }
        }
    }

    private fun openBooksInList(savedList: SavedList) {
        Log.d(TAG, "Opening list: ${savedList.name} (ID: ${savedList.id})")

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
        val dialogBinding = NewListLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val heightInPixels = (200 * resources.displayMetrics.density).toInt()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, heightInPixels)

        dialogBinding.saveNewListBtn.setOnClickListener {
            val listName = dialogBinding.newListNameEt.text.toString().trim()
            if (listName.isNotEmpty()) {
                createNewList(listName, dialog)
            } else {
                Toast.makeText(this, "Please enter a list name", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.cancelNewListBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createNewList(listName: String, dialog: Dialog) {
        Log.d(TAG, "Creating new list: $listName for user: $currentUserId")

        ListsDatabase.createList(listName, currentUserId)
            .addOnSuccessListener {
                Log.d(TAG, "List created successfully: $listName")
                Toast.makeText(this, "List '$listName' created successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create list: $listName", e)
                Toast.makeText(this, "Failed to create list: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listsListener?.remove()
        Log.d(TAG, "Activity destroyed, listener removed")
    }
}

data class SavedList(
    val name: String,
    val bookCount: Int,
    val id: String = ""
)