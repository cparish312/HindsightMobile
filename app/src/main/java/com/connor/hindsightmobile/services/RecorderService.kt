package com.connor.hindsightmobile.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.connor.hindsightmobile.MainActivity
import com.connor.hindsightmobile.R
import com.connor.hindsightmobile.enums.RecorderState
import com.connor.hindsightmobile.obj.UserActivityState
import com.connor.hindsightmobile.utils.NotificationHelper
import com.connor.hindsightmobile.utils.PermissionHelper
import com.connor.hindsightmobile.utils.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class RecorderService : LifecycleService() {
    abstract val notificationTitle: String
    abstract val noticationChannel: String
    abstract val recordingNoticationID: Int
    abstract val recorderIntentActionKey: String


    private val binder = LocalBinder()
    var recorder: MediaRecorder? = null
    var fileDescriptor: ParcelFileDescriptor? = null

    var onRecorderStateChanged: (RecorderState) -> Unit = {}
    open val fgServiceType: Int? = null
    var recorderState: RecorderState = RecorderState.IDLE

    private val recorderReceiver = object : BroadcastReceiver() {
        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(ACTION_EXTRA_KEY)) {
                STOP_ACTION -> {
                    Preferences.prefs.edit().putBoolean(Preferences.screenrecordingenabled, false).apply()
                    onDestroy()
                }
                PAUSE_RESUME_ACTION -> {
                    if (recorderState == RecorderState.ACTIVE) pause() else resume()
                }
            }
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    runIngest()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    inner class LocalBinder : Binder() {
        // Return this instance of [BackgroundMode] so clients can call public methods
        fun getService(): RecorderService = this@RecorderService
    }

    override fun onCreate() {
        val notification = buildNotification()
        if (fgServiceType != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                recordingNoticationID,
                notification.build(),
                fgServiceType!!
            )
        } else {
            startForeground(recordingNoticationID, notification.build())
        }

        runCatching {
            unregisterReceiver(recorderReceiver)
        }
        val intentFilter = IntentFilter().apply {
            addAction(recorderIntentActionKey)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            this,
            recorderReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        super.onCreate()
    }

    private fun buildNotification(): NotificationCompat.Builder {
        val stopIntent = Intent(recorderIntentActionKey).putExtra(ACTION_EXTRA_KEY, STOP_ACTION)
            .putExtra(
                FROM_RECORDER_SERVICE,
                true
            )
        val stopAction = NotificationCompat.Action.Builder(
            null,
            getString(R.string.stop),
            getPendingIntent(stopIntent, 2)
        )

        val resumeOrPauseIntent = Intent(recorderIntentActionKey).putExtra(
            ACTION_EXTRA_KEY,
            PAUSE_RESUME_ACTION
        ).putExtra(
            FROM_RECORDER_SERVICE,
            true
        )
        val resumeOrPauseAction = NotificationCompat.Action.Builder(
            null,
            if (recorderState == RecorderState.ACTIVE) {
                getString(R.string.pause)
            } else {
                getString(R.string.resume)
            },
            getPendingIntent(resumeOrPauseIntent, 3)
        )

        return NotificationCompat.Builder(
            this,
            noticationChannel
        )
            .setContentTitle(notificationTitle)
            .setSmallIcon(R.drawable.ic_stat_hourglass_empty)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(recorderState == RecorderState.ACTIVE)
            .addAction(stopAction.build())
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addAction(
                        resumeOrPauseAction.build()
                    )
                }
            }
            .setUsesChronometer(true)
            .setContentIntent(getActivityIntent())
    }

    @SuppressLint("MissingPermission")
    fun updateNotification() {
        if (!PermissionHelper.hasPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            return
        }
        val notification = buildNotification().build()
        NotificationManagerCompat.from(this).notify(
            recordingNoticationID,
            notification
        )
    }

    private fun getPendingIntent(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    open fun start() {
        runCatching {
            recorderState = RecorderState.ACTIVE
            onRecorderStateChanged(recorderState)
        }
        updateNotification()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    open fun pause() {
        Log.d("ScreenRecordingService", "Recorder paused")
        recorder?.pause()
        runCatching {
            recorderState = RecorderState.PAUSED
            onRecorderStateChanged(recorderState)
        }
        updateNotification()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    open fun resume() {
        recorder?.resume()
        runCatching {
            recorderState = RecorderState.ACTIVE
            onRecorderStateChanged(recorderState)
        }
        updateNotification()
    }

    override fun onDestroy() {
        runCatching {
            recorderState = RecorderState.IDLE
            onRecorderStateChanged(recorderState)
            unregisterReceiver(recorderReceiver)
        }

        NotificationManagerCompat.from(this)
            .cancel(recordingNoticationID)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                recorder?.runCatching {
                    stop()
                    release()
                }
                recorder = null
                fileDescriptor?.close()
            }

            ServiceCompat.stopForeground(
                this@RecorderService,
                ServiceCompat.STOP_FOREGROUND_REMOVE
            )
            stopSelf()

            super.onDestroy()
        }
    }

    private fun getActivityIntent(): PendingIntent {
        Log.d("ScreenRecordingService", "Starting Main Activity from notification")
        val intent = Intent(this, MainActivity::class.java).putExtra(FROM_RECORDER_SERVICE, true)
        return PendingIntent.getActivity(
            this,
            6,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun runIngest(){
        if (!Preferences.prefs.getBoolean(Preferences.autoingestenabled, false)){
            return
        }
        // Only autoingest if screen is off
        if (screenOn) {
            return
        }
        if (!Preferences.prefs.getBoolean(Preferences.autoingestwhennotcharging, false)
            && !UserActivityState.phoneCharging) {
            return
        }
        if (IngestScreenshotsService.isRunning.value) {
            Log.d("RecorderService", "Foreground ingestion service is already running")
            return
        }
        val ingestIntent = Intent(this@RecorderService, IngestScreenshotsService::class.java)
        ContextCompat.startForegroundService(this@RecorderService, ingestIntent)
    }

    companion object {
        const val ACTION_EXTRA_KEY = "action"
        const val STOP_ACTION = "STOP"
        const val PAUSE_RESUME_ACTION = "PR"
        const val FROM_RECORDER_SERVICE = "com.connor.hindsightmobile.FROM_RECORDER_SERVICE"
        var screenOn: Boolean = true
    }
}
