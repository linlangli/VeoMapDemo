package com.github.linlangli.veomapdemo.service

import com.github.linlangli.veomapdemo.model.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface IGeocodingService {
    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
}