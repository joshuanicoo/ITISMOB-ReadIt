package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET

interface GoogleBooksApiInterface {
    @GET("volumes?q=machine+learning&maxResults=5&printType=books")
    fun getBooks(): Call<GoogleBooksResponse>
}