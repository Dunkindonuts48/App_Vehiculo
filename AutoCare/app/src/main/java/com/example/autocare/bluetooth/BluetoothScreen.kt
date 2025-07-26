package com.example.autocare.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.autocare.AppHeader

@Composable
fun BluetoothScreen(
    vehicleId: Int,
    onDeviceSelected: (BluetoothDevice) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    var hasBtPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                        PackageManager.PERMISSION_GRANTED
            else
                true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBtPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasBtPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    if (!hasBtPermission) {
        Scaffold(
            topBar = {
                AppHeader(
                    title = "Vincular Bluetooth",
                    onBack = { navController.popBackStack() }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    "Esta función requiere permiso BLUETOOTH_CONNECT.\n" +
                            "Por favor, concédelo para continuar."
                )
            }
        }
        return
    }

    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = remember { btManager.adapter }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    LaunchedEffect(adapter) {
        devices = adapter.bondedDevices.orEmpty().toList()
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Vincular Bluetooth",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            if (devices.isEmpty()) {
                Text("No hay dispositivos emparejados.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices) { dev ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDeviceSelected(dev)
                                    navController.popBackStack()
                                }
                        ) {
                            Text(
                                text = "${dev.name.orEmpty()} — ${dev.address}",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}