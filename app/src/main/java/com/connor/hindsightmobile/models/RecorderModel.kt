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
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.connor.hindsightmobile.R
import com.connor.hindsightmobile.enums.RecorderState
import com.connor.hindsightmobile.services.BackgroundRecorderService
import com.connor.hindsightmobile.services.RecorderService
import com.connor.hindsightmobile.services.UserActivityTrackingService
import com.connor.hindsightmobile.utils.PermissionHelper
import com.connor.hindsightmobile.utils.Preferences

class RecorderModel : ViewModel() {
    var recorderState by mutableStateOf(RecorderState.IDLE)
    var recordedTime by mutableStateOf<Long?>(null)
    private var activityResult: ActivityResult? = null

    @SuppressLint("StaticFieldLeak")
    private var recorderService: RecorderService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            recorderService = (service as RecorderService.LocalBinder).getService()
            recorderService?.onRecorderStateChanged = {
                recorderState = it
            }
            (recorderService as? BackgroundRecorderService)?.prepare(activityResult!!)
            recorderService?.start()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            recorderService = null
        }
    }

    fun startVideoRecorder(context: Context, result: ActivityResult) {
        activityResult = result
        val serviceIntent = Intent(context, BackgroundRecorderService::class.java)
        startRecorderService(context, serviceIntent)
    }

    private fun startRecorderService(context: Context, intent: Intent) {
        runCatching {
            context.unbindService(connection)
        }

        listOfNotNull(
            BackgroundRecorderService::class.java,
            UserActivityTrackingService::class.java
        ).forEach {
            runCatching {
                context.stopService(Intent(context, it))
            }
        }
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Initialize KeyTracking to see when user active
        if (Preferences.prefs.getBoolean(Preferences.recordwhenactive, true)) {
            val keyTrackingIntent = Intent(context, UserActivityTrackingService::class.java)
            context.startService(keyTrackingIntent)
        }

        Log.d("RecorderModel", "Start Recorder Service")
    }

    fun stopRecording(context: Context) {
        // Doesn't work if app is reopened through notification
        Log.d("RecorderModel", "Stop Recorder Service")
        listOfNotNull(
            BackgroundRecorderService::class.java,
            UserActivityTrackingService::class.java
        ).forEach {
            context.stopService(Intent(context, it))
        }
        recorderService?.onDestroy()
        recordedTime = null
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
    fun hasScreenRecordingPermissions(context: Context): Boolean {
        val requiredPermissions = arrayListOf<String>()

        // Get Accessibility access (needed to get active app)
        if (!isAccessibilityServiceEnabled(context, UserActivityTrackingService::class.java)) {
            Log.d("RecorderModel", "Accessibility Service Not Enabled")
            openAccessibilitySettings(context)
            Preferences.prefs.edit().putBoolean(Preferences.screenrecordingenabled, false)
                .apply()
            context.sendBroadcast(Intent(SCREEN_RECORDER_PERMISSION_DENIED))
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
            context.sendBroadcast(Intent(SCREEN_RECORDER_PERMISSION_DENIED))
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
    }
}
