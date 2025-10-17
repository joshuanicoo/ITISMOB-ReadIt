package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiInterface {
    @GET("volumes?q=there's+no+freaking+way&maxResults=10&printType=books")
    fun getBooks(): Call<GoogleBooksResponse>

    @GET("volumes")
    fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("printType") printType: String = "books"
    ): Call<GoogleBooksResponse>
}