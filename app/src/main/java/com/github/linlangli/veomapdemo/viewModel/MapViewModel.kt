package com.github.linlangli.veomapdemo.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.linlangli.veomapdemo.utils.MapUtil
import com.github.linlangli.veomapdemo.utils.MapUtil.decodePolyline
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel: ViewModel() {
    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    /**
     * 获取两点之间的行车路线
     * @param origin 起点坐标，格式为 "lat,lng"
     * @param destination 终点坐标，格式为 "lat,lng"
     * @param apiKey Google Maps API Key
     */
    fun fetchDirections(origin: String, destination: String, apiKey: String) {
        println("🚗origin: $origin, destination: $destination, apiKey: $apiKey")
        viewModelScope.launch {
            try {
                val response = MapUtil.directionService.getDirections(
                    origin = origin,
                    destination = destination,
                    apiKey = apiKey
                )

                println("🚗response: $response")

                if (response.routes.isNotEmpty()) {
                    val polyline = response.routes[0].overviewPolyline.points
                    _routePoints.value = decodePolyline(polyline)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reverseGeocode(latLng: LatLng, apiKey: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Log.i("GoogleMapScreen", "reverseGeocode, latLng: $latLng")
                val response = MapUtil.geocodingService.reverseGeocode(
                    latlng = "${latLng.latitude},${latLng.longitude}",
                    apiKey = apiKey
                )
                val address = response.results.firstOrNull()?.formattedAddress
                onResult(address)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }
}