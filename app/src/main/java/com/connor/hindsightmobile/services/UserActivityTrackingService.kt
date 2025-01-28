package com.connor.hindsightmobile.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.obj.UserActivityState
import com.connor.hindsightmobile.ui.viewmodels.ManageRecordingsViewModel
import com.connor.hindsightmobile.utils.Preferences
import com.connor.hindsightmobile.utils.getAccessibilityScreenshotsDirectory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

class UserActivityTrackingService : AccessibilityService() {
    private lateinit var dbHelper: DB
    private lateinit var appPackages: HashSet<String>
    private var screenshotApplication: String? = null
    private var lastScreenshotTimestamp: Long = 0

    private lateinit var appPackageToRecord: Map<String, Boolean>
    private var defaultRecordApps: Boolean = Preferences.prefs.getBoolean(
        Preferences.defaultrecordapps,
        false
    )

    private val excludedPackages = listOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.inputmethod.latin", // Trade off of knowing the keyboard is in use and ease of organizing with app being used
        "com.google.android.apps.nexuslauncher",
        "com.android.pixeldisplayservice"
    )

    private val userActivityTrackingBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ManageRecordingsViewModel.RECORDING_SETTTINGS_UPDATED -> {
                    appPackageToRecord = dbHelper.getAppPackageToRecordMap()
                    defaultRecordApps = Preferences.prefs.getBoolean(
                        Preferences.defaultrecordapps,
                        false
                    )
                    Log.d("UserActivityTrackingService", "RECORDING_SETTTINGS_UPDATED")
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    if (isCharging) {
                        Log.d("UserActivityTrackingService", "Device is charging.")
                        UserActivityState.phoneCharging = true
                    } else {
                        Log.d("UserActivityTrackingService", "Device is not charging.")
                        UserActivityState.phoneCharging = false
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        UserActivityState.userActive = true // for only recording when active
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            try {
                event.packageName?.let {
                    val packageName = event.packageName.toString()

                    if (!excludedPackages.contains(packageName)) {
                        UserActivityState.currentApplication = packageName // For storing screenshots by application
                        screenshotApplication = packageName
                    }
                    Log.d("UserActivityTrackingService", "onAccessibilityEvent: $packageName")

                    if (packageName !in appPackages) {
                        val appName = getAppNameFromPackageName(packageName)
                        val recordNewApp = Preferences.prefs.getBoolean(Preferences.defaultrecordapps, false)
                        dbHelper.insertApp(packageName, appName, recordNewApp)
                        appPackages.add(packageName)
                    }
                }

                // Issues with killing this service so double check screen recording is enabled
                val screenRecordingEnabled = Preferences.prefs.getBoolean(Preferences.screenrecordingenabled, false)
                if (!screenRecordingEnabled) {
                    Log.d("UserActivityTrackingService", "Screen Recording Disabled")
                    return
                }

                if (appPackageToRecord[screenshotApplication] == false ||
                    (!appPackageToRecord.containsKey(screenshotApplication) && !defaultRecordApps)) {
                    Log.d(
                        "UserActivityTrackingService",
                        "Not recording $screenshotApplication"
                    )
                    return
                }
                // Take a Screenshot every 2 seconds
                val currentTimestamp = System.currentTimeMillis()
                if (currentTimestamp - lastScreenshotTimestamp >= 2000) {
                    takeScreenshot()
                }
            } catch(e: Error){
                Log.d("UserActivityTrackingService", "Error on AccessibilityEvent", e)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("UserActivityTrackingService", "Service Connected")

        defaultRecordApps = Preferences.prefs.getBoolean(Preferences.defaultrecordapps, false)

        val serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL
            notificationTimeout = 100
        }
        this.serviceInfo = serviceInfo
    }

    override fun onCreate() {
        Log.d("UserActivityTrackingService", "onCreate")
        isRunning = true
        dbHelper = DB.getInstance(this)
        appPackages = dbHelper.getAppPackages()
        appPackageToRecord = dbHelper.getAppPackageToRecordMap()

        runCatching {
            unregisterReceiver(userActivityTrackingBroadcastReceiver)
        }
        val intentFilter = IntentFilter().apply {
            addAction(ManageRecordingsViewModel.RECORDING_SETTTINGS_UPDATED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            userActivityTrackingBroadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        super.onCreate()
    }

    override fun onInterrupt() {
    }

    @RequiresApi(Build.VERSION_CODES.S) // API 31+
    private fun takeScreenshot() {
        val executor = Executors.newSingleThreadExecutor()

        takeScreenshot(Display.DEFAULT_DISPLAY, executor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                val bitmap = screenshot.hardwareBuffer?.let { buffer ->
                    Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)?.copy(Bitmap.Config.ARGB_8888, true)
                }

                bitmap?.let {
                    saveImageData(it)
                    Log.d("AccessibilityService", "Screenshot captured successfully.")
                } ?: Log.e("AccessibilityService", "Failed to create Bitmap from ScreenshotResult.")
            }

            override fun onFailure(errorCode: Int) {
                Log.e("AccessibilityService", "Failed to take screenshot with error: ${errorCode}")
            }
        })
    }

    private fun saveImageData(bitmap: Bitmap) {
        val directory = getAccessibilityScreenshotsDirectory(this)
        val imageApplicationDashes = screenshotApplication?.replace(".", "-")
        val currentTimestamp = System.currentTimeMillis()
        val file = File(directory, "${imageApplicationDashes}_${currentTimestamp}.webp")
        lastScreenshotTimestamp = currentTimestamp

        val portraitBitmap = if (bitmap.width > bitmap.height) {
            val matrix = Matrix().apply {
                postRotate(90f)
            }

            Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
        } else {
            bitmap
        }

        try {
            FileOutputStream(file).use { fos ->
                portraitBitmap.compress(Bitmap.CompressFormat.WEBP, 100, fos)
                Log.d("AccessibilityService", "Screenshot saved: ${file.absolutePath}")
            }
        } catch (e: IOException) {
            Log.e("AccessibilityService", "Failed to save screenshot", e)
        }
    }

    // Doesn't work for the majority of apps due to permissions issues
    private fun getAppNameFromPackageName(packageName: String): String? {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("UserActivityTrackingService", "App not found for package: $packageName", e)
            null
        }
    }

    override fun onDestroy() {
        isRunning = false
        runCatching {
            unregisterReceiver(userActivityTrackingBroadcastReceiver)
        }
        sendBroadcast(Intent(ACCESSIBILITY_SERVICE_STOPPED))
        super.onDestroy()
    }

    companion object {
        var isRunning = false
        const val ACCESSIBILITY_SERVICE_STOPPED = "com.connor.hindsightmobile.ACCESSIBILITY_SERVICE_STOPPED"
    }
}
