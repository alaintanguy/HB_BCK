package com.healthbridge

    import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class LocationService : Service() {

    private val CHANNEL_ID = "location_channel"
    private val NOTIFICATION_ID = 1
    private var gpsTracker: GpsTracker? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        gpsTracker = GpsTracker(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Starting GPS..."))

        serviceScope.launch {
            while (isActive) {
                val lat = gpsTracker?.getLatitude()
                val lon = gpsTracker?.getLongitude()

                if (lat != null && lon != null) {
                    Log.d("LocationService", "Location: $lat, $lon")
                    updateNotification("Lat: $lat, Lon: $lon")
                    PushoverSender.send("Location update: $lat, $lon")
                } else {
                    Log.d("LocationService", "Waiting for GPS...")
                    updateNotification("Waiting for GPS...")
                }

                delay(300_000) // every 5 minutes
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        gpsTracker?.stopUsingGPS()
        Log.d("LocationService", "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HealthBridge GPS")
            .setContentText(text)
           // .setSmallIcon(R.drawable.ic_heart)
             .setSmallIcon(android.R.drawable.ic_dialog_info)
            //.setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Location Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
