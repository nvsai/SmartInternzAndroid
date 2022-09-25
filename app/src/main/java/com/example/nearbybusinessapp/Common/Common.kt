package com.example.nearbybusinessapp.Common

import com.example.nearbybusinessapp.Remote.IGoogleAPIService
import com.example.nearbybusinessapp.Remote.RetrofitClient

object Common {
    private val GOOGLE_API_URL="https://maps.googleapis.com/"

    val googleaApiService:IGoogleAPIService
    get() = RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)
}