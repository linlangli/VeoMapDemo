package com.github.linlangli.veomapdemo

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.linlangli.veomapdemo.utils.MapUtil.createTextMarkerIcon
import com.github.linlangli.veomapdemo.viewModel.LocationInfo
import com.github.linlangli.veomapdemo.viewModel.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun GoogleMapScreen() {
    val viewModel: MapViewModel = viewModel()
    val context = LocalContext.current

    val routePoints by viewModel.routePoints.collectAsState()
    val startLocation by viewModel.startLocation.collectAsState()
    val endLocation by viewModel.endLocation.collectAsState()

    var hasLocationPermission by remember { mutableStateOf(false) }
    var startMarkerInfo by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var endMarkerInfo by remember { mutableStateOf<BitmapDescriptor?>(null) }

    // 默认北京
    val defaultLocation = LatLng(39.9042, 116.4074)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // 权限 & 当前定位处理
    LaunchedEffect(Unit) {
        val fineLocation =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
            viewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(startLocation) {
        startLocation?.let {
            Log.i("GoogleMapScreen", "startLocation: ${it.latLng}, title: ${it.title}")
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it.latLng, 12f))
            if (startMarkerInfo == null) {
                startMarkerInfo = createTextMarkerIcon(context, it.title)
            }
        } ?: run {
            Log.i("GoogleMapScreen", "使用默认位置")
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            onMapClick = { latLng ->
                Log.i("GoogleMapScreen", "latLng: $latLng")
                viewModel.reverseGeocode(latLng, BuildConfig.MAPS_API_KEY) { address ->
                    val location = LocationInfo(latLng, address ?: "未知位置")
                    viewModel.setEndLocation(location)
                    endMarkerInfo = createTextMarkerIcon(context, location.title)
                }
                viewModel.clearRoute()
            },
        ) {
            // 当前定位
            startLocation?.let {
                Log.i("GoogleMapScreen", "🚗 startLocation: ${it.latLng}, title: ${it.title}")
                Marker(
                    state = MarkerState(position = it.latLng),
                    title = it.title,
                )
            } ?: run {
                Marker(
                    state = MarkerState(position = defaultLocation),
                    title = "默认位置（北京）"
                )
            }
            startMarkerInfo?.let {
                startLocation?.let {
                    Marker(
                        state = MarkerState(position = it.latLng),
                        icon = startMarkerInfo,
                        anchor = Offset(0.5f, 3f) // 图标底部对准坐标
                    )
                }
            }
            // 用户点击位置
            endLocation?.let {
                Marker(
                    state = MarkerState(position = it.latLng),
                )
            }
            endMarkerInfo?.let {
                endLocation?.let {
                    Marker(
                        state = MarkerState(position = it.latLng),
                        icon = endMarkerInfo,
                        anchor = Offset(0.5f, 3f) // 图标底部对准坐标
                    )
                }
            }
            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    color = Color.Blue,
                    width = 8f
                )
            }
        }

        Button(
            onClick = {
                val origin = startLocation ?: LocationInfo(defaultLocation, "默认位置")
                val dest = endLocation ?: return@Button
                viewModel.fetchDirections(
                    origin = "${origin.latLng.latitude},${origin.latLng.longitude}",
                    destination = "${dest.latLng.latitude},${dest.latLng.longitude}",
                    apiKey = BuildConfig.MAPS_API_KEY
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("开始导航")
        }
    }
}