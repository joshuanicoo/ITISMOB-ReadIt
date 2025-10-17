package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mobdeve.s17.group39.itismob_mco.databinding.HomeActivityBinding
import com.mobdeve.s17.group39.itismob_mco.ui.scanner.ScannerActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {
    private lateinit var data: GoogleBooksResponse
    private lateinit var binding: HomeActivityBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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
        getBooks()
        setupClickListeners()

        this.adapter = HomeAdapter(mutableListOf())
        this.binding.bookListRv.adapter = adapter
        this.binding.bookListRv.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupClickListeners() {
        binding.scanIsbnBtn.setOnClickListener {
            openScanner()
        }
    }

    private fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    private fun searchBookByIsbn(isbn: String) {
        if (isbn.isNotEmpty()) {
            Toast.makeText(this, "Searching for ISBN: $isbn", Toast.LENGTH_SHORT).show()

            // update this api for isbn search (backend)
            // toast temporary
        } else {
            Toast.makeText(this, "Invalid ISBN scanned", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getApiInterface() {
        apiInterface = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)
    }

    private fun getBooks() {
        val call = apiInterface.getBooks()
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse(call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    data = response.body()!!
                    adapter.updateData(data.items)
                }
            }
            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@HomeActivity, "Failed to load books", Toast.LENGTH_SHORT).show()
            }
        })
    }
}