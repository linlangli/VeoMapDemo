package com.github.linlangli.veomapdemo.model

import com.google.gson.annotations.SerializedName

data class DirectionsModel(
    val routes: List<Route>
)

data class Route(
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline,
    val legs: List<Leg>
)

data class OverviewPolyline(val points: String)

data class Leg(
    val distance: Distance,
    val duration: Duration,
    val start_address: String,
    val end_address: String,
)

data class Distance(val text: String, val value: Int)
data class Duration(val text: String, val value: Int)