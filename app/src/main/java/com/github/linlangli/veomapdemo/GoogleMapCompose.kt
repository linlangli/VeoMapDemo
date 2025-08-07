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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.linlangli.veomapdemo.viewModel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun GoogleMapScreen() {
    val viewModel: MapViewModel = viewModel()
    val context = LocalContext.current

    val routePoints by viewModel.routePoints.collectAsState()

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var destination by remember { mutableStateOf<LatLng?>(null) }
    var destinationTitle by remember { mutableStateOf<String?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }

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
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        currentLocation = latLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                    }
                }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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
                destination = latLng
                viewModel.reverseGeocode(latLng, BuildConfig.MAPS_API_KEY) { address ->
                    destinationTitle = address ?: "未知位置"
                    Log.i("GoogleMapScreen", "地址: $address")
                }
            }
        ) {
            // 当前定位
            currentLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "当前位置"
                )
            } ?: run {
                Marker(
                    state = MarkerState(position = defaultLocation),
                    title = "默认位置（北京）"
                )
            }
            // 用户点击位置
            destination?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = destinationTitle ?: "目的地",
                    snippet = "点击开始导航",
                )
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
                val origin = currentLocation ?: defaultLocation
                val dest = destination ?: return@Button
                viewModel.fetchDirections(
                    origin = "${origin.latitude},${origin.longitude}",
                    destination = "${dest.latitude},${dest.longitude}",
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