package com.xjyzs.speedviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.xjyzs.speedviewer.ui.theme.SpeedViewerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpeedViewerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainUI(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainUI(modifier: Modifier) {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    val scope = rememberCoroutineScope()
    var speed by remember { mutableFloatStateOf(0f) }

    // 位置管理器实例
    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // 位置监听器（使用remember保持实例稳定）
    val locationListener = remember {
        object : LocationListener {
            override fun onLocationChanged(newLocation: Location) {
                location = newLocation
                speed=location!!.speed
            }

            // 其他回调方法保持空实现
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
    }

    // 启动位置更新逻辑
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(context as Activity,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 2)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(context as Activity,arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), 2)
        }
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates(locationManager, locationListener,context)
        }
        scope.launch {
            var cnt=0
            while (true){
                delay(1000)
                if (lastLocation==location) {
                    cnt+=1
                }else {
                    cnt = 0
                }
                if (cnt>2){
                    speed=0f
                    cnt=0
                }
                lastLocation=location
            }
        }
    }


    // 界面显示
    Column(modifier.padding(horizontal = 20.dp).fillMaxSize().wrapContentSize(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        if (location != null) {
            Text("当前速度")
            Text("%.2f".format(speed), fontSize = 96.sp, fontWeight = FontWeight.Bold)
            Text("km/h")
            Row {
                Text("纬度：${"%.6f".format(location?.latitude)}  ")
                Text("经度：${"%.6f".format(location?.longitude)}")
            }
        } else {
            Text("正在获取定位...")
        }
    }
}


// 启动位置更新
private fun startLocationUpdates(
    manager: LocationManager,
    listener: LocationListener,
    context: Context
) {
    try {
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,   // 1秒间隔
                0f,
                listener
            )
        } else {
            // 如果GPS未开启，使用网络定位
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                10f,
                listener
            )
        }
    } catch (e: SecurityException) {
        Toast.makeText(context,"权限异常${e}", Toast.LENGTH_SHORT).show()
    }
}