package com.connor.hindsightmobile

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.connor.hindsightmobile.models.RecorderModel
import com.connor.hindsightmobile.models.RecorderModel.Companion.BACKGROUND_RECORDER_PERMISSION_DENIED
import com.connor.hindsightmobile.models.RecorderModel.Companion.SCREEN_RECORDER_PERMISSION_DENIED
import com.connor.hindsightmobile.services.BackgroundRecorderService
import com.connor.hindsightmobile.services.IngestScreenshotsService
import com.connor.hindsightmobile.services.RecorderService
import com.connor.hindsightmobile.services.ServerUploadService
import com.connor.hindsightmobile.services.UserActivityTrackingService
import com.connor.hindsightmobile.ui.screens.AppNavigation
import com.connor.hindsightmobile.ui.theme.HindsightMobileTheme
import com.connor.hindsightmobile.utils.Preferences
import com.connor.hindsightmobile.utils.ServerConnectionCallback
import com.connor.hindsightmobile.utils.checkServerConnection

class MainActivity : ComponentActivity() {
    private val recorderModel: RecorderModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HindsightMobileTheme {
                AppNavigation()
            }
        }

        Log.d("MainActivity", "screenrecordingenabled = ${Preferences.prefs.getBoolean(Preferences.screenrecordingenabled, false)}")

        if (Preferences.prefs.getBoolean(Preferences.screenrecordingenabled, false)) {
            if (!intent.getBooleanExtra(RecorderService.FROM_RECORDER_SERVICE, false)) {
                Log.d("MainActivity", "Starting Recording From MainActivity")
                startScreenRecording()
            }
        }

        if (Preferences.prefs.getBoolean(Preferences.locationtrackingenabled, false)) {
            if (!intent.getBooleanExtra(RecorderService.FROM_RECORDER_SERVICE, false)) {
                Log.d("MainActivity", "Starting Location Tracking From MainActivity")
                startBackgroundRecorder()
            }
        }
    }

    fun startScreenRecording() {
        if (UserActivityTrackingService.isRunning) {
            Log.d(
                "MainActivity",
                "Ran startScreenRecording but UserActivityTrackingService is running"
            )
            return
        }
        if (recorderModel.hasAccessibilityPermissions(this)) {
            Log.d("MainActivity", "hasAccessibilityPermissions")
            recorderModel.startScreenRecorderService(this)
        } else {
            Preferences.prefs.edit().putBoolean(Preferences.screenrecordingenabled, false)
                .apply()
            this.sendBroadcast(Intent(SCREEN_RECORDER_PERMISSION_DENIED))
        }
    }

    fun stopScreenRecording() {
        recorderModel.stopRecording(this)
    }

    fun startBackgroundRecorder() {
        if (BackgroundRecorderService.isRunning) {
            Log.d(
                "MainActivity",
                "Ran startBackgroundRecorder but BackgroundRecorderService is running"
            )
            return
        }
        if (recorderModel.hasAccessibilityPermissions(this)) {
            Log.d("MainActivity", "hasAccessibilityPermissions")
            recorderModel.startBackgroundRecorderService(this)
        } else {
            Preferences.prefs.edit().putBoolean(Preferences.locationtrackingenabled, false)
                .apply()
            this.sendBroadcast(Intent(BACKGROUND_RECORDER_PERMISSION_DENIED))
        }
    }

    fun stopBackgroundRecorder() {
        recorderModel.stopBackgroundRecorderService(this)
    }

    fun ingestScreenshots() {
        if (IngestScreenshotsService.isRunning.value) {
            Log.d("MainActivity", "Foreground ingestion service is already running")
            return
        }
        val ingestIntent = Intent(this@MainActivity, IngestScreenshotsService::class.java)
        ContextCompat.startForegroundService(this@MainActivity, ingestIntent)
    }

    fun uploadToServer() {
        Log.d("MainActivity", "uploadToServer")
        if (ServerUploadService.isRunning.value) {
            Log.d(
                "MainActivity",
                "Ran uploadToServer but ServerUploadService is running"
            )
            return
        }
        val primaryUrl: String = Preferences.prefs.getString(
            Preferences.interneturl,
            ""
        ).toString()

        checkServerConnection(serverUrl = primaryUrl, object : ServerConnectionCallback {
            override fun onServerStatusChecked(isConnected: Boolean) {
                if (isConnected) {
                    Log.d(
                        "MainActivity",
                        "Connection successful, proceeding with service initialization."
                    )
                    val uploadIntent = Intent(this@MainActivity, ServerUploadService::class.java)
                    ContextCompat.startForegroundService(this@MainActivity, uploadIntent)
                } else {
                    Log.d("MainActivity", "No server connection, aborting upload.")
                }
            }
        })
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HindsightMobileTheme {
        AppNavigation()
    }
}