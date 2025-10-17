package com.mobdeve.s17.group39.itismob_mco.ui.viewbook.list

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s17.group39.itismob_mco.databinding.AddToListLayoutBinding


class AddToListActivity : AppCompatActivity() {

    private lateinit var addToListVB: AddToListLayoutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addToListVB = AddToListLayoutBinding.inflate(layoutInflater)
        setContentView(addToListVB.root)

        // Recycler view for lists
        val dataLists = arrayListOf(
            "Currently Reading",
            "Want to Read",
            "Read",
            "Favorites",
            "To Buy",
            "Summer Reading",
            "Classics",
            "Non-Fiction"
        )
        this.addToListVB.addToListRv.adapter = AddToListAdapter(dataLists)
        this.addToListVB.addToListRv.layoutManager = LinearLayoutManager(this)
    }
}