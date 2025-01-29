package com.connor.hindsightmobile.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.connor.hindsightmobile.DB
import java.lang.SecurityException
import com.connor.hindsightmobile.R
import com.connor.hindsightmobile.enums.RecorderState
import com.connor.hindsightmobile.obj.UserActivityState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import android.location.Location
import com.connor.hindsightmobile.utils.NotificationHelper
import com.connor.hindsightmobile.utils.Preferences
import com.google.android.gms.tasks.Task
import java.util.concurrent.Executors

class BackgroundRecorderService : RecorderService() {
    override val notificationTitle: String
        get() = getString(R.string.hindsight_recording)
    override val noticationChannel: String = NotificationHelper.RECORDING_NOTIFICATION_CHANNEL
    override val recordingNoticationID: Int = NotificationHelper.RECORDING_NOTIFICATION_ID
    override val recorderIntentActionKey: String =
        "com.connor.hindsightmobile.BackgroundRecorderService"

    private var recorderLoopStopped: Boolean = false

    private lateinit var dbHelper: DB

    private var handler: Handler? = null
    private var recordRunnable: Runnable? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLatitude: Double? = null
    private var lastKnownLongitude: Double? = null

    override val fgServiceType: Int?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            null
        }

    private val backgroundRecorderBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    if (isCharging) {
                        Log.d("BackgroundRecorderService", "Device is charging.")
                        UserActivityState.phoneCharging = true
                    } else {
                        Log.d("BackgroundRecorderService", "Device is not charging.")
                        UserActivityState.phoneCharging = false
                    }
                }
            }
        }
    }

    override fun onCreate() {
        Log.d("BackgroundRecorderService", "onCreate")
        dbHelper = DB.getInstance(this@BackgroundRecorderService)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        runCatching {
            unregisterReceiver(backgroundRecorderBroadcastReceiver)
        }
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            backgroundRecorderBroadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        super.onCreate()
    }

    override fun start() {
        super.start()
        isRunning = true

        Log.d("BackgroundRecorderService", "Starting Recorder")
        handler = Handler(Looper.getMainLooper())
        recordRunnable = object : Runnable {
            override fun run() {
                if (Preferences.prefs.getBoolean(
                        Preferences.locationtrackingenabled,
                        false
                    )) {
                    addLastKnownLocation()
                }
                postRecorderLoop(this)
            }
        }
        handler?.postDelayed(recordRunnable!!, 2000) // Start after a delay of 2 seconds
    }

    private fun postRecorderLoop(runnable: Runnable) {
        if (recorderState == RecorderState.ACTIVE) {
            recorderLoopStopped = false
            handler?.postDelayed(runnable, 2000)
        } else {
            recorderLoopStopped = true
        }
    }

    private fun selfDestroy(){
        // When stopping service from within ensure Broadcast is sent
        sendBroadcast(Intent(BACKGROUND_RECORDER_STOPPED))
        onDestroy()
    }

    override fun onDestroy() {
        Log.d("BackgroundScreenRecorderService", "Destroying Screen Recording Service")
        isRunning = false
        handler?.removeCallbacks(recordRunnable!!) // Stop the recurring record capture
        runCatching {
            unregisterReceiver(backgroundRecorderBroadcastReceiver)
        }
        super.onDestroy()
    }

    override fun resume() {
        super.resume()
        // recorderLoopStopped ensures that the previous image capture is stopped
        if (recorderState == RecorderState.ACTIVE && recorderLoopStopped) {
            handler?.postDelayed(recordRunnable!!, 2000)
        }
    }

    private fun addLastKnownLocation() {
        try {
            val locationResult: Task<Location> = fusedLocationClient.lastLocation
            val backgroundExecutor = Executors.newSingleThreadExecutor()
            locationResult.addOnCompleteListener(backgroundExecutor) { task ->
                if (task.isSuccessful && task.result != null) {
                    val lastKnownLocation: Location = task.result!!
                    if (lastKnownLatitude != lastKnownLocation.latitude ||
                        lastKnownLongitude != lastKnownLocation.longitude) {
                        lastKnownLatitude = lastKnownLocation.latitude
                        lastKnownLongitude = lastKnownLocation.longitude
                        dbHelper.addLocation(lastKnownLatitude!!, lastKnownLongitude!!)
                    }
                    UserActivityState.updateLastLocationTimestamp(System.currentTimeMillis())
                } else {
                    Log.d("BackgroundScreenRecorderService",
                        "No location detected. Make sure location is enabled on the device.")
                }
            }
        } catch (e: SecurityException) {
            // Handle the exception
            println("SecurityException: ${e.message}")
        }
    }

    companion object {
        var isRunning = false
        const val BACKGROUND_RECORDER_STOPPED = "com.connor.hindsightmobile.BACKGROUND_RECORDER_STOPPED"
    }
}
