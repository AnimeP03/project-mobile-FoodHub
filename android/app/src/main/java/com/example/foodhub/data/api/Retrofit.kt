package com.example.foodhub.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient{
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val retrofit = Retrofit.Builder()
        // quanto uso il mio telefono
        //.baseUrl("http://192.168.1.15:3000/")
        .baseUrl("http://10.55.254.146:3000/")
        // quando si usa il emulatore
        //.baseUrl("http://10.0.2.2:3000/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: ApiService by lazy{
        retrofit.create(ApiService::class.java)
    }
}