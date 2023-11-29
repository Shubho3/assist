package com.assist.assist

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST
    fun search(@Url url: String,@Body body: RequestBody): Call<SearchResult>

}