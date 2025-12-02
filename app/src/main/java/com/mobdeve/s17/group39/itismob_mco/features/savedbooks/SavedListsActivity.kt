package com.mobdeve.s17.group39.itismob_mco.features.savedbooks

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.mobdeve.s17.group39.itismob_mco.R
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.DeleteConfirmationDialogBinding
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

        setupRecyclerView()
        setupClickListeners()
        setupRealTimeListener()
    }

    private fun setupRecyclerView() {
        adapter = SavedListsAdapter(emptyList(),
            onItemClick = { list ->
                if (!adapter.getDeleteMode()) {
                    openBooksInList(list)
                }
            },
            onDeleteClick = { listId ->
                showDeleteConfirmation(listId)
            }
        )
        binding.savedListsRv.adapter = adapter
        binding.savedListsRv.layoutManager = LinearLayoutManager(this)

        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun setupClickListeners() {
        binding.createNewListBtn.setOnClickListener {
            if (!adapter.getDeleteMode()) {
                showNewListDialog()
            }
        }

        // Set up delete mode toggle button
        binding.deleteModeBtn.setOnClickListener {
            toggleDeleteMode()
        }
    }

    private fun toggleDeleteMode() {
        val isDeleteMode = adapter.getDeleteMode()
        adapter.setDeleteMode(!isDeleteMode)

        if (!isDeleteMode) {
            // Entering delete mode
            binding.deleteModeBtn.setImageDrawable(
                ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel)
            )
            binding.deleteModeBtn.setColorFilter(
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            )
            binding.createNewListBtn.isEnabled = false
            binding.createNewListBtn.alpha = 0.5f
        } else {
            // Exiting delete mode
            binding.deleteModeBtn.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_delete_outline)
            )
            binding.deleteModeBtn.setColorFilter(
                ContextCompat.getColor(this, R.color.main_green)
            )
            binding.createNewListBtn.isEnabled = true
            binding.createNewListBtn.alpha = 1f
        }
    }

    private fun showDeleteConfirmation(listId: String) {
        // Create custom dialog
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        val dialogBinding = DeleteConfirmationDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Set dialog dimensions
        val heightInPixels = (220 * resources.displayMetrics.density).toInt()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, heightInPixels)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog message
        dialogBinding.dialogMessageTv.text = "Are you sure you want to delete this list?"

        // Set up cancel button
        dialogBinding.cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        // Set up delete button
        dialogBinding.deleteBtn.setOnClickListener {
            deleteList(listId)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deleteList(listId: String) {
        Log.d(TAG, "Deleting list: $listId")

        ListsDatabase.deleteList(listId)
            .addOnSuccessListener {
                // If no lists remain, exit delete mode
                if (adapter.itemCount <= 1) {
                    toggleDeleteMode()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete list: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRealTimeListener() {

        listsListener = ListsDatabase.getAll()
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        "Error loading lists: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                val lists = querySnapshot?.documents?.mapNotNull { document ->
                    try {

                        val list = ListModel.fromMap(document.id, document.data ?: emptyMap())
                        list
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
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
            binding.savedListsRv.visibility = android.view.View.GONE

            // Exit delete mode if active
            if (adapter.getDeleteMode()) {
                toggleDeleteMode()
            }

            // Try to show empty state text if it exists
            try {
                binding.emptyStateTv.visibility = android.view.View.VISIBLE
                binding.emptyStateTv.text = "No lists yet. Create your first list!"
            } catch (e: Exception) {
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

        ListsDatabase.createList(listName, currentUserId)
            .addOnSuccessListener {
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
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