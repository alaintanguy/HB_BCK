package com.healthbridge.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Scaffold
import android.util.Log

/**
 * WearMainActivity
 * 
 * Displays the HealthBridge watch UI:
 * - App name "HEALTHBRIDGE"
 * - Real heart rate reading (only if valid > 0)
 * - Watch battery percentage
 * 
 * Preserves Phase 1 tested behavior while adding Phase 2 Data Layer transmission
 */
class WearMainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "WearMainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "WearMainActivity created")
        
        // Start telemetry service
        val telemetryIntent = Intent(this, WearTelemetryService::class.java)
        startService(telemetryIntent)
        
        setContent {
            WearUI()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WearMainActivity destroyed")
        
        // Stop telemetry service
        val telemetryIntent = Intent(this, WearTelemetryService::class.java)
        stopService(telemetryIntent)
    }
}

@Composable
fun WearUI() {
    val heartRate = remember { mutableStateOf<Int?>(null) }
    val battery = remember { mutableStateOf(50) }
    val isConnected = remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Name
            Text(
                text = "HEALTHBRIDGE",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Heart Rate Display
            // Only show valid readings (> 0)
            if (heartRate.value != null && heartRate.value!! > 0) {
                Text(
                    text = "${heartRate.value} bpm",
                    color = Color.Red,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Text(
                    text = "-- bpm",
                    color = Color.Gray,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            // Battery Display
            Text(
                text = "Battery: ${battery.value}%",
                color = if (battery.value > 20) Color.Green else Color.Yellow,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            
            // Connection Status
            Text(
                text = if (isConnected.value) "Connected" else "Offline",
                color = if (isConnected.value) Color.Green else Color.Red,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
