package com.connor.hindsightmobile

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.content.ContextCompat
import com.connor.hindsightmobile.models.RecorderModel
import com.connor.hindsightmobile.obj.IngestAlarmReceiver
import com.connor.hindsightmobile.services.IngestScreenshotsService
import com.connor.hindsightmobile.services.RecorderService
import com.connor.hindsightmobile.services.BackgroundRecorderService
import com.connor.hindsightmobile.ui.screens.AppNavigation
import com.connor.hindsightmobile.ui.theme.HindsightMobileTheme
import com.connor.hindsightmobile.utils.Preferences
import java.util.Calendar
import com.connor.hindsightmobile.databinding.ContentMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private val recorderModel: RecorderModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContent {
//            HindsightMobileTheme {
//                AppNavigation()
//            }
//        }
        setContentView(
            ComposeView(this).apply {
                consumeWindowInsets = false
                setContent {
                    AndroidViewBinding(ContentMainBinding::inflate)
                }
            }
        )

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
            scheduleIngestAlarm(this)
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
        val uploadIntent = Intent(this@MainActivity, IngestScreenshotsService::class.java)
        ContextCompat.startForegroundService(this@MainActivity, uploadIntent)
    }
}

fun scheduleIngestAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Intent to trigger the BroadcastReceiver
    val alarmIntent = Intent(context, IngestAlarmReceiver::class.java).let { intent ->
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    val autoIngestTime = Preferences.prefs.getInt(Preferences.autoingesttime, 2)
    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, autoIngestTime)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DAY_OF_MONTH, 1) // Set for the next day if time has passed
        }
    }

    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        alarmIntent
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HindsightMobileTheme {
        AppNavigation()
    }
}