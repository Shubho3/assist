package com.assist.assist

import com.google.gson.annotations.SerializedName

data class SearchResult(

    @SerializedName("candidates") var candidates: ArrayList<Candidates> = arrayListOf()

)

data class Candidates(

    @SerializedName("output") var output: String? = null,
    @SerializedName("safetyRatings") var safetyRatings: ArrayList<SafetyRatings> = arrayListOf()

)

data class SafetyRatings(

    @SerializedName("category") var category: String? = null,
    @SerializedName("probability") var probability: String? = null

)

data class SearchRequest(
    val prompt: Prompt
)

data class Prompt(
    val query: String
)