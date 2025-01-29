package com.connor.hindsightmobile.models

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.connor.hindsightmobile.R
import com.connor.hindsightmobile.services.BackgroundRecorderService
import com.connor.hindsightmobile.services.RecorderService
import com.connor.hindsightmobile.services.UserActivityTrackingService
import com.connor.hindsightmobile.utils.PermissionHelper

class RecorderModel : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    private var recorderService: RecorderService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            recorderService = (service as RecorderService.LocalBinder).getService()
            recorderService?.start()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            recorderService = null
        }
    }

    fun startScreenRecorderService(context: Context) {
        stopRecording(context)

        val keyTrackingIntent = Intent(context, UserActivityTrackingService::class.java)
        context.startService(keyTrackingIntent)

        Log.d("RecorderModel", "Start Recorder Service")
    }

    fun stopRecording(context: Context) {
        // Doesn't work if app is reopened through notification
        Log.d("RecorderModel", "Stop Recorder Service")
        listOfNotNull(
            UserActivityTrackingService::class.java
        ).forEach {
            context.stopService(Intent(context, it))
        }
    }

    fun startBackgroundRecorderService(context: Context) {
        runCatching {
            context.unbindService(connection)
        }

        stopBackgroundRecorderService(context)

        val backgroundRecordIntent = Intent(context, BackgroundRecorderService::class.java)
        ContextCompat.startForegroundService(context, backgroundRecordIntent)

        context.bindService(backgroundRecordIntent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopBackgroundRecorderService(context: Context) {
        listOfNotNull(
            BackgroundRecorderService::class.java
        ).forEach {
            runCatching {
                context.stopService(Intent(context, it))
            }
        }
    }

    fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        try {
            colonSplitter.setString(enabledServices)
        } catch (e: Exception) {
            return false
        }
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(
                    ComponentName(context, service).flattenToString(),
                    ignoreCase = true
                )
            ) {
                return true
            }
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        Toast.makeText(
            context,
            "Please enable our accessibility service.",
            Toast.LENGTH_LONG
        ).show()
    }

    @SuppressLint("NewApi")
    fun hasAccessibilityPermissions(context: Context): Boolean {
        val requiredPermissions = arrayListOf<String>()

        // Get Accessibility access (needed to get active app)
        if (!isAccessibilityServiceEnabled(context, UserActivityTrackingService::class.java)) {
            Log.d("RecorderModel", "Accessibility Service Not Enabled")
            openAccessibilitySettings(context)
            return false
        } else {
            Log.d("RecorderModel", "Accessibility Service Enabled")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (requiredPermissions.isEmpty()) return true

        val granted = PermissionHelper.checkPermissions(context, requiredPermissions.toTypedArray())
        if (!granted) {
            Toast.makeText(
                context,
                context.getString(R.string.no_sufficient_permissions),
                Toast.LENGTH_SHORT
            ).show()
        }
        return granted
    }

    companion object {
        const val SCREEN_RECORDER_PERMISSION_DENIED =
            "com.connor.hindsightmobile.SCREEN_RECORDER_PERMISSION_DENIED"
        const val BACKGROUND_RECORDER_PERMISSION_DENIED =
            "com.connor.hindsightmobile.LOCATION_TRACKING_PERMISSION_DENIED"
    }
}
