package com.mobdeve.s17.group39.itismob_mco.ui.homepage

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query

object RetrofitInstance{
    private const val BASE_URL="https://www.googleapis.com/books/v1/"

    fun getInstance() : Retrofit {
        return Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
    }



}