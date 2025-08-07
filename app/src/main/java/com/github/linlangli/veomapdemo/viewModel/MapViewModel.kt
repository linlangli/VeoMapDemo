package com.github.linlangli.veomapdemo.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.linlangli.veomapdemo.BuildConfig
import com.github.linlangli.veomapdemo.utils.MapUtil
import com.github.linlangli.veomapdemo.utils.MapUtil.decodePolyline
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.launch

data class LocationInfo(
    val latLng: LatLng,
    val title: String = "",
)

class MapViewModel(app: Application): AndroidViewModel(app) {
    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    private val _currentLatLng = MutableStateFlow<LocationInfo?>(null)
    val currentLatLng = _currentLatLng.asStateFlow()

    private val _startLatLng = MutableStateFlow<LocationInfo?>(null)
    val startLatLng = _startLatLng.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(app)
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5000L // 每5秒请求一次位置
    ).apply {
        setMinUpdateIntervalMillis(2000L) // 最快每2秒更新一次
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location? = result.lastLocation
            location?.let {
                reverseGeocode(
                    LatLng(it.latitude, it.longitude),
                    BuildConfig.MAPS_API_KEY
                ) { address ->
                    if (_startLatLng.value == null) {
                        _startLatLng.value = LocationInfo(
                            LatLng(it.latitude, it.longitude),
                            address ?: "未知位置"
                        )
                    }
                    Log.i("GoogleMapScreen", "??Address: $address")
                    _currentLatLng.value = LocationInfo(
                        LatLng(it.latitude, it.longitude),
                        address ?: "未知位置"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // 前提是权限已检查
    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * 获取两点之间的行车路线
     * @param origin 起点坐标，格式为 "lat,lng"
     * @param destination 终点坐标，格式为 "lat,lng"
     * @param apiKey Google Maps API Key
     */
    fun fetchDirections(origin: String, destination: String, apiKey: String) {
        Log.i("GoogleMapScreen", "fetchDirections")
        viewModelScope.launch {
            try {
                val response = MapUtil.directionService.getDirections(
                    origin = origin,
                    destination = destination,
                    apiKey = apiKey
                )

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

    fun clearRoute() {
        _routePoints.value = emptyList()
    }
}