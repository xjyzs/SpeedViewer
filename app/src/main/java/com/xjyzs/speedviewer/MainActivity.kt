package com.xjyzs.speedviewer

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.xjyzs.speedviewer.ui.theme.SpeedViewerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            SpeedViewerTheme {
                Surface(
                    Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    MainUI()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainUI() {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    val scope = rememberCoroutineScope()
    var speed by remember { mutableFloatStateOf(0f) }
    var distM by remember { mutableDoubleStateOf(0.0) }
    var startTime by remember { mutableLongStateOf(-1) }
    var maxSpeed by remember { mutableFloatStateOf(0f) }
    var timeMillis by remember { mutableLongStateOf(0) }
    var lastGpsLocation by remember { mutableStateOf<Location?>(null) }

    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    val locationListener = remember {
        LocationListener { newLocation ->
            location = newLocation
            speed = location!!.speed
            if (speed > 0.3) {
                val localLast = lastGpsLocation
                if (localLast != null) {
                    val deltaDistance = newLocation.distanceTo(localLast)
                    if (deltaDistance < 500) {
                        distM += deltaDistance
                    }
                }
                lastGpsLocation = newLocation
                if (startTime == -1L) startTime = System.currentTimeMillis()
                if (speed > maxSpeed) maxSpeed = speed
            } else {
                lastGpsLocation = null

                location = newLocation
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                context as Activity, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                } else {
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }, 2
            )
        }
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates(locationManager, locationListener, context)
        }
        scope.launch {
            var cnt = 0
            while (true) {
                delay(1000.milliseconds)
                if (lastLocation == location) cnt += 1
                else cnt = 0
                if (cnt > 2) {
                    speed = 0f
                    cnt = 0
                }
                lastLocation = location
            }
        }
    }
    LaunchedEffect(startTime) {
        if (startTime != -1L) {
            while (true) {
                delay(1000.milliseconds)
                timeMillis = System.currentTimeMillis() - startTime
            }
        }
    }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (location != null) {
            Text("%.1f".format(speed * 3.6), fontSize = 96.sp, fontWeight = FontWeight.Bold)
            Text("km/h")
            Spacer(Modifier.size(10.dp))
            Column(Modifier.width(280.dp)) {
                Row {
                    WhiteCard(Modifier.weight(1f)) {
                        Text("纬度", color = MaterialTheme.colorScheme.secondary)
                        Text("%.6f".format(location?.latitude), fontSize = 20.sp)
                    }
                    Spacer(Modifier.size(6.dp))
                    WhiteCard(Modifier.weight(1f)) {
                        Text("经度", color = MaterialTheme.colorScheme.secondary)
                        Text("%.6f".format(location?.longitude), fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.size(6.dp))
                Row {
                    WhiteCard(Modifier.weight(1f)) {
                        Text("累计距离", color = MaterialTheme.colorScheme.secondary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                if (distM < 1000) "${distM.roundToLong()}" else "%.2f".format(distM / 1000),
                                fontSize = 20.sp
                            )
                            Text(if (distM < 1000) " m" else " km")
                        }
                    }
                    Spacer(Modifier.size(6.dp))
                    WhiteCard(Modifier.weight(1f)) {
                        Text("平均速度", color = MaterialTheme.colorScheme.secondary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.2f".format(
                                    if (startTime != -1L) distM / (System.currentTimeMillis() - startTime) * 3600
                                    else 0.0
                                ), fontSize = 20.sp
                            )
                            Text(" km/h")
                        }
                    }
                }
                Spacer(Modifier.size(6.dp))
                Row {
                    WhiteCard(Modifier.weight(1f)) {
                        Text("时间", color = MaterialTheme.colorScheme.secondary)
                        Text(
                            formatTimeMillis(timeMillis), fontSize = 20.sp
                        )
                    }
                    Spacer(Modifier.size(6.dp))
                    WhiteCard(Modifier.weight(1f)) {
                        Text("最高速度", color = MaterialTheme.colorScheme.secondary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.2f".format(maxSpeed * 3.6), fontSize = 20.sp
                            )
                            Text(" km/h")
                        }
                    }
                }
            }
        } else {
            LoadingIndicator(Modifier.size(70.dp))
            Spacer(Modifier.size(10.dp))
            Text("正在获取定位...", fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}


private fun startLocationUpdates(
    manager: LocationManager, listener: LocationListener, context: Context
) {
    try {
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 0f, listener
            )
        } else {
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 5000L, 10f, listener
            )
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "权限异常${e}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WhiteCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(6.dp, 10.dp)
    ) {
        content()
    }
}

fun formatTimeMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
}