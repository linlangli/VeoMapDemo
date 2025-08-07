package com.github.linlangli.veomapdemo.model

import com.google.gson.annotations.SerializedName

data class GeocodingResponse(
    @SerializedName("results") val results: List<GeocodingResult>
)

data class GeocodingResult(
    @SerializedName("formatted_address") val formattedAddress: String
)