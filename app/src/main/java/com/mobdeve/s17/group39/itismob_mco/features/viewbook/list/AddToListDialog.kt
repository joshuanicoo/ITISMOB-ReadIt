package com.mobdeve.s17.group39.itismob_mco.features.viewbook.list

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.mobdeve.s17.group39.itismob_mco.database.ListsDatabase
import com.mobdeve.s17.group39.itismob_mco.databinding.AddToListLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.models.ListModel

class AddToListDialog(
    private val context: Context,
    private val bookId: String,
    private val onNewListRequested: () -> Unit
) {

    private lateinit var dialog: Dialog
    private lateinit var binding: AddToListLayoutBinding
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    fun show() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(context, "Please log in to add books to lists", Toast.LENGTH_SHORT).show()
            return
        }

        dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        binding = AddToListLayoutBinding.inflate(dialog.layoutInflater)
        dialog.setContentView(binding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        loadUserLists()
        setupClickListeners()

        dialog.show()
    }

    private fun loadUserLists() {
        if (currentUserId.isEmpty()) return

        ListsDatabase.getUserLists(currentUserId)
            .addOnSuccessListener { querySnapshot ->
                val userLists = mutableListOf<ListModel>()

                for (document in querySnapshot.documents) {
                    try {
                        val listModel = ListModel.fromMap(document.id, document.data ?: emptyMap())
                        userLists.add(listModel)
                    } catch (e: Exception) {
                        // Skip invalid documents
                    }
                }

                setupRecyclerView(userLists)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load your lists", Toast.LENGTH_SHORT).show()
                setupRecyclerView(emptyList())
            }
    }

    private fun setupRecyclerView(lists: List<ListModel>) {
        if (lists.isEmpty()) {
            binding.addToListRv.visibility = android.view.View.GONE
        } else {
            binding.addToListRv.visibility = android.view.View.VISIBLE
            binding.addToListRv.adapter = AddToListAdapter(lists, bookId) { documentId, listName, isChecked ->
                if (isChecked) {
                    addBookToList(documentId, listName)
                } else {
                    removeBookFromList(documentId, listName)
                }
            }
            binding.addToListRv.layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupClickListeners() {
        binding.newListBtn.setOnClickListener {
            dialog.dismiss()
            onNewListRequested()
        }

        binding.addToListDialogBtn.setOnClickListener {
            dialog.dismiss()
        }

        binding.cancelAddToListBtn.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun addBookToList(documentId: String, listName: String) {
        ListsDatabase.addBookToList(documentId, bookId)
            .addOnSuccessListener {
                Toast.makeText(context, "Added to '$listName'", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add to '$listName'", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeBookFromList(documentId: String, listName: String) {
        ListsDatabase.removeBookFromList(documentId, bookId)
            .addOnSuccessListener {
                Toast.makeText(context, "Removed from '$listName'", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to remove from '$listName'", Toast.LENGTH_SHORT).show()
            }
    }
}