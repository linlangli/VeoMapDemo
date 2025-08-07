package com.github.linlangli.veomapdemo.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.linlangli.veomapdemo.BuildConfig
import com.github.linlangli.veomapdemo.model.Leg
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

enum class NavigationState {
    IDLE, // 空闲状态
    STARTED, // 导航已开始
    ARRIVED, // 已到达目的地
}

class MapViewModel(app: Application): AndroidViewModel(app) {
    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationInfo?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _directionLeg = MutableStateFlow<Leg?>(null)
    val directionLeg = _directionLeg.asStateFlow()

    private val _startLocation = MutableStateFlow<LocationInfo?>(null)
    val startLocation = _startLocation.asStateFlow()

    private val _endLocation = MutableStateFlow<LocationInfo?>(null)
    val endLocation = _endLocation.asStateFlow()

    private val _navigationState = MutableStateFlow(NavigationState.IDLE)
    val navigationState = _navigationState.asStateFlow()

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
                val latLng = LatLng(it.latitude, it.longitude)
                reverseGeocode(
                    latLng,
                    BuildConfig.MAPS_API_KEY
                ) { address ->
                    _navigationState.value = NavigationState.STARTED
                    _currentLocation.value = LocationInfo(
                        latLng,
                        address ?: "未知位置"
                    )
                    if (hasArrived(latLng, _endLocation.value?.latLng)) {
                        Log.i("GoogleMapScreen", "🚗 已到达目的地: ${_endLocation.value?.title}")
                        // 停止导航、提示用户
                        _navigationState.value = NavigationState.ARRIVED
                        stopLocationUpdates()
                    } else {
                        Log.i("GoogleMapScreen", "🚗 还未到达目的地: $latLng")
                    }
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

    @SuppressLint("MissingPermission") // 前提是权限已检查
    fun initStartLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                reverseGeocode(
                    latLng,
                    BuildConfig.MAPS_API_KEY
                ) { address ->
                    _startLocation.value = LocationInfo(
                        latLng,
                        address ?: "未知位置"
                    )
                }
            }
        }
    }

    /**
     * 获取两点之间的行车路线
     * @param origin 起点坐标，格式为 "lat,lng"
     * @param destination 终点坐标，格式为 "lat,lng"
     * @param apiKey Google Maps API Key
     */
    fun fetchDirections(origin: String, destination: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val response = MapUtil.directionService.getDirections(
                    origin = origin,
                    destination = destination,
                    apiKey = apiKey
                )
                _directionLeg.value = response.routes[0].legs[0]
                if (response.routes.isNotEmpty()) {
                    val polyline = response.routes[0].overviewPolyline.points
//                    if (_directionLeg.value != null) {
//                        _startLocation.value = LocationInfo(
//                            LatLng(_directionLeg.value!!.start_location.lat, _directionLeg.value!!.start_location.lng),
//                            response.routes[0].legs[0].start_address
//                        )
//                        _endLocation.value = LocationInfo(
//                            LatLng(_directionLeg.value!!.end_location.lat, _directionLeg.value!!.end_location.lng),
//                            _directionLeg.value!!.end_address
//                        )
//                    }
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

    fun hasArrived(currentLatLng: LatLng, destinationLatLng: LatLng?, thresholdInMeters: Float = 30f): Boolean {
        if (destinationLatLng == null) {
            return false
        }
        val result = FloatArray(1)
        Location.distanceBetween(
            currentLatLng.latitude,
            currentLatLng.longitude,
            destinationLatLng.latitude,
            destinationLatLng.longitude,
            result
        )
        return result[0] <= thresholdInMeters
    }

    fun startNavigation() {
        _navigationState.value = NavigationState.STARTED
        startLocationUpdates()
    }

    fun stopNavigation() {
        stopLocationUpdates()
        _navigationState.value = NavigationState.IDLE
        _endLocation.value = null
        _directionLeg.value = null
        clearRoute()
    }

    fun setEndLocation(endLocation: LocationInfo) {
        _endLocation.value = endLocation
    }

    fun clearRoute() {
        _routePoints.value = emptyList()
    }
}