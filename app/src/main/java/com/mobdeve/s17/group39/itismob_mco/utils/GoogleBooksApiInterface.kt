package com.mobdeve.s17.group39.itismob_mco.utils

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
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

}

interface QuoteApiInterface {
    @GET("quotes/random")
    fun getRandomQuote(): Call<List<QuoteResponse>>
}

data class QuoteResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("content") val content: String,
    @SerializedName("author") val author: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("authorSlug") val authorSlug: String,
    @SerializedName("length") val length: Int,
    @SerializedName("dateAdded") val dateAdded: String,
    @SerializedName("dateModified") val dateModified: String
)