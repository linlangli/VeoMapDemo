package com.github.linlangli.veomapdemo.service

import com.github.linlangli.veomapdemo.model.DirectionsModel
import retrofit2.http.GET
import retrofit2.http.Query

interface IDirectionService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): DirectionsModel
}