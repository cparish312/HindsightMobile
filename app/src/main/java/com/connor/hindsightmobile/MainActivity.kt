package com.connor.hindsightmobile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.connor.hindsightmobile.models.RecorderModel
import com.connor.hindsightmobile.obj.IngestWorker
import com.connor.hindsightmobile.services.IngestScreenshotsService
import com.connor.hindsightmobile.services.RecorderService
import com.connor.hindsightmobile.services.BackgroundRecorderService
import com.connor.hindsightmobile.ui.screens.AppNavigation
import com.connor.hindsightmobile.ui.theme.HindsightMobileTheme
import com.connor.hindsightmobile.utils.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private val recorderModel: RecorderModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HindsightMobileTheme {
                AppNavigation()
            }
        }

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                recorderModel.startVideoRecorder(this, result)
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

        if (Preferences.prefs.getBoolean(Preferences.screenrecordingenabled, false)) {
            if (!intent.getBooleanExtra(RecorderService.FROM_RECORDER_SERVICE, false)) {
                Log.d("MainActivity", "Starting Recording From MainActivity")
                requestScreenCapturePermission()
            }
        }

        if (Preferences.prefs.getBoolean(Preferences.autoingestenabled, false)) {
            lifecycleScope.launch {
                schedulePeriodicIngestion(this@MainActivity)
            }
        }
    }

    fun requestScreenCapturePermission() {
        if (BackgroundRecorderService.isRunning) {
            Log.d(
                "MainActivity",
                "Ran requestScreenCapturePermission but BackgroundRecorderService is running"
            )
            return
        }
        if (recorderModel.hasScreenRecordingPermissions(this)) {
            Log.d("MainActivity", "hasScreenRecordingPermissions")
            var captureIntent: Intent? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // So default val of prompt is entire screen
                val config = MediaProjectionConfig.createConfigForDefaultDisplay()
                captureIntent = mediaProjectionManager.createScreenCaptureIntent(config)
            } else {
                captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            }
            screenCaptureLauncher.launch(captureIntent)
        }
    }

    fun stopScreenRecording() {
        recorderModel.stopRecording(this)
    }

    fun ingestScreenshots(){
        Log.d("MainActivity", "Ingesting screenshots")
        if (IngestScreenshotsService.isRunning.value){
            Log.d("MainActivity", "IngestScreenshotsService is already running")
            return
        }
        val ingestIntent = Intent(this@MainActivity, IngestScreenshotsService::class.java)
        ContextCompat.startForegroundService(this@MainActivity, ingestIntent)
    }

    @SuppressLint("IdleBatteryChargingConstraints")
    suspend fun schedulePeriodicIngestion(context: Context) {
        delay(5000)
        val constraints = if (Preferences.prefs.getBoolean(Preferences.autoingestwhennotcharging, false)) {
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true) // Add idle constraint
                .build()
        } else {
            Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true) // Add idle constraint
                .build()
        }

        val periodicWorkRequest = PeriodicWorkRequestBuilder<IngestWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "IngestPeriodicWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
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