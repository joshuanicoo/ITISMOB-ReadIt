package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s17.group39.itismob_mco.databinding.HomeActivityBinding
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

class HomeActivity : AppCompatActivity() {
    private lateinit var data: GoogleBooksResponse
    private lateinit var binding: HomeActivityBinding
    private lateinit var adapter: HomeAdapter
    private lateinit var apiInterface: GoogleBooksApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getApiInterface()
        getBooks()


        this.adapter = HomeAdapter(data)
        this.binding.bookListRv.adapter = adapter
        this.binding.bookListRv.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

    }

    private fun getApiInterface(){
        apiInterface = RetrofitInstance.getInstance().create(GoogleBooksApiInterface::class.java)
    }

    private fun getBooks(){
        val call = apiInterface.getBooks()
        call.enqueue(object : Callback<GoogleBooksResponse> {
            override fun onResponse (call: Call<GoogleBooksResponse>, response: Response<GoogleBooksResponse>) {
                if (response.isSuccessful && response.body()!=null){
                    data = response.body()!!
                }
            }
            override fun onFailure(call: Call<GoogleBooksResponse>, t: Throwable){
                t.printStackTrace()
            }

        })
    }



}