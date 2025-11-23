package com.mobdeve.s17.group39.itismob_mco.utils

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleBooksApiInterface {
    @GET("volumes?q=subject:fiction&maxResults=40&printType=books&orderBy=relevance")
    fun getBooks(): Call<GoogleBooksResponse>

    @GET("volumes")
    fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 40,
        @Query("printType") printType: String = "books"
    ): Call<GoogleBooksResponse>

    @GET("volumes")
    fun getBookByISBN(
        @Query("q") isbn: String,
        @Query("maxResults") maxResults: Int = 40,
        @Query("printType") printType: String = "books"
    ): Call<GoogleBooksResponse>

    @GET("volumes/{volumeId}")
    fun getBookByVolumeId(
        @Path("volumeId") volumeId: String
    ): Call<Map<String, Any>>
}