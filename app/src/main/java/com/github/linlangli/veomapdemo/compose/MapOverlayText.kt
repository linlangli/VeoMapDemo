package com.github.linlangli.veomapdemo.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.model.LatLng


@Composable
fun MapOverlayText(
    latLng: LatLng,
    text: String
) {
    val density = LocalDensity.current
    var screenOffset by remember { mutableStateOf(Offset.Zero) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f) // 显示在 Marker 之上
    ) {
        Box(
            modifier = Modifier
                .offset {
                    // 调整为 marker 上方显示
                    val x = screenOffset.x.toInt() - 50 // 水平居中调整
                    val y = screenOffset.y.toInt() - 100 // 垂直偏移（上方）
                    IntOffset(x, y)
                }
                .zIndex(2f)
        ) {
            Text(
                text = text,
                color = Color.White,
                modifier = Modifier
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}