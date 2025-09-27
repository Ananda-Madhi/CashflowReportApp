package com.example.cashflowreportapp.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {
    @GET("latest")
    fun getRates(
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): Call<ExchangeResponse>
}

data class ExchangeResponse(
    val base: String,
    val rates: Map<String, Double>
)
