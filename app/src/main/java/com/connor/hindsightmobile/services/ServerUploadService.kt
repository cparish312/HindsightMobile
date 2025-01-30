package com.connor.hindsightmobile.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.MainActivity
import com.connor.hindsightmobile.R
import com.connor.hindsightmobile.interfaces.ApiService
import com.connor.hindsightmobile.interfaces.syncDBData
import com.connor.hindsightmobile.obj.RetrofitClient
import com.connor.hindsightmobile.utils.NotificationHelper
import com.connor.hindsightmobile.utils.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ServerUploadService : LifecycleService() {
    val notificationTitle: String = "HindsightMobile Server Upload"
    val framesUploadLimit: Int = 10000
    private var stopUpload: Boolean = false
    val sourceName = Preferences.prefs.getString(
        Preferences.sourcename, "hindsightmobile"
    ).toString()
    private lateinit var dbHelper: DB

    private val uploaderReceiver = object : BroadcastReceiver() {
        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(ServerUploadService.ACTION_EXTRA_KEY)) {
                STOP_ACTION -> {
                    onDestroy()
                }
            }
        }
    }
    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.SERVER_UPLOAD_NOTIFICATION_ID,
                buildNotification().build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NotificationHelper.SERVER_UPLOAD_NOTIFICATION_ID,
                buildNotification().build()
            )
        }

        dbHelper = DB.getInstance(this@ServerUploadService)

        isRunning.value = true

        runCatching {
            unregisterReceiver(uploaderReceiver)
        }

        val intentFilter = IntentFilter().apply {
            addAction(ServerUploadService.UPLOADER_INTENT_ACTION)
        }
        ContextCompat.registerReceiver(
            this,
            uploaderReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        sendBroadcast(Intent(UPLOADER_STARTED))

        CoroutineScope(Dispatchers.IO).launch {
            syncDatabase()
        }

        super.onCreate()
    }

    private suspend fun getLastId(tableName: String): Int? {
        val serverUrl: String = Preferences.prefs.getString(
            Preferences.interneturl,
            ""
        ).toString()
        val retrofit = RetrofitClient.getInstance(serverUrl, numTries = 3)
        val client = retrofit.create(ApiService::class.java)

        return try {
            val response = client.getLastId(sourceName, tableName)

            if (response.isSuccessful) {
                response.body()?.last_id?.toInt()
            } else {
                println("Failed to fetch the last frame id: ${response.errorBody()?.string()}")
                null
            }

        } catch (t: Throwable) {
            println("Error fetching last frame id: ${t.message}")
            null
        }
    }

    private suspend fun getLastTimestamp(table: String): Long? {
        val serverUrl: String = Preferences.prefs.getString(
            Preferences.interneturl,
            ""
        ).toString()
        val retrofit = RetrofitClient.getInstance(serverUrl, numTries = 3)
        val client = retrofit.create(ApiService::class.java)

        return try {
            val response = client.getLastTimestamp(table)
            if (response.isSuccessful) {
                response.body()?.last_timestamp?.toLong()
            } else {
                println("Failed to fetch the timestamp: ${response.errorBody()?.string()}")
                null
            }
        } catch (t: Throwable) {
            println("Error fetching timestamp: ${t.message}")
            null
        }
    }

    private suspend fun syncWithServer(client: ApiService, syncData: syncDBData){
        try {
            val response = client.syncDB(syncData)
            if (response.isSuccessful) {
                Log.d("ServerUploadService", "DB Sync successful")
            } else {
                Log.e("ServerUploadService", "DB Sync failed: ${response.errorBody()?.string()}")
                onDestroy()
            }
        } catch (e: Exception) {
            Log.e("ServerUploadService", "Network call failed with exception: ${e.message}")
            onDestroy()
        }
    }

    suspend fun uploadVideoToServer(apiService: ApiService, file: File, videoChunkId: Int, frameIds: List<Int>) {
        val requestFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val sourcePart = sourceName.toRequestBody("text/plain".toMediaTypeOrNull())
        val videoChunkIdPart = videoChunkId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val frameIdsPart = frameIds.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())

        val response = apiService.uploadVideo(filePart, sourcePart, videoChunkIdPart, frameIdsPart)

        if (response.isSuccessful) {
            Log.d("ServerUploadService","Video $videoChunkId uploaded successfully")
        } else {
            println("Failed to upload video $videoChunkId: ${response.errorBody()?.string()}")
        }
    }

    suspend fun uploadAllVideoChunks() {
        var lastVideoChunkId = getLastId("video_chunks")
        Log.d("ServerUploadService", "Last synced Video Chunk Id: $lastVideoChunkId")

        var syncVideoChunks = dbHelper.getVideoChunks(lastVideoChunkId)
        Log.d("ServerUploadService", "Sync video chunks: ${syncVideoChunks.size}")

        if (syncVideoChunks.isEmpty()) {
            Log.d("ServerUploadService", "No new video chunks to upload")
            return
        }

        val serverUrl: String = Preferences.prefs.getString(
            Preferences.interneturl,
            ""
        ).toString()
        val retrofit = RetrofitClient.getInstance(serverUrl, numTries = 3)
        val client = retrofit.create(ApiService::class.java)

        for (chunk in syncVideoChunks) {
            val file = File(chunk.path)
            if (!file.exists()) {
                Log.e("Upload", "File not found: ${chunk.path}")
                continue
            }

            uploadVideoToServer(client, file, chunk.id, chunk.frames.map { it.id })
        }
    }

    private suspend fun syncDatabase() {

        var lastFrameId = getLastId("frames")
        Log.d("ServerUploadService", "Last synced Frame Id: $lastFrameId")
        var syncFrames = dbHelper.getNewFramesAndOCR(lastFrameId, framesUploadLimit)
        Log.d("ServerUploadService", "Sync frames: ${syncFrames.size}")

        val lastLocationsTimestamp = getLastTimestamp("locations")
        val syncLocations = dbHelper.getLocations(lastLocationsTimestamp)
        Log.d("ServerUploadService", "Sync locations: ${syncLocations.size}")

        val serverUrl: String = Preferences.prefs.getString(
            Preferences.interneturl,
            ""
        ).toString()
        val retrofit = RetrofitClient.getInstance(serverUrl, numTries = 3)
        val client = retrofit.create(ApiService::class.java)
        var syncData = syncDBData(sourceName, syncFrames, syncLocations)

        syncWithServer(client, syncData)

        // if more than framesUploadLimit frames  it will keep syncing
        // until there are no more frames to sync
        while (syncFrames.size == framesUploadLimit) {
            lastFrameId = syncFrames.lastOrNull()?.id
            Log.d("ServerUploadService", "Last synced Frame Id: $lastFrameId")
            syncFrames = dbHelper.getNewFramesAndOCR(lastFrameId, framesUploadLimit)
            Log.d("ServerUploadService", "Sync frames: ${syncFrames.size}")
            syncData = syncDBData(sourceName, syncFrames)
            syncWithServer(client, syncData)
        }

        uploadAllVideoChunks()
        onDestroy()
    }

    private fun getPendingIntent(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun getActivityIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            6,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(): NotificationCompat.Builder {
        val stopIntent = Intent(ServerUploadService.UPLOADER_INTENT_ACTION).putExtra(
            ServerUploadService.ACTION_EXTRA_KEY,
            ServerUploadService.STOP_ACTION
        )
        val stopAction = NotificationCompat.Action.Builder(
            null,
            getString(R.string.stop),
            getPendingIntent(stopIntent, 2)
        )

        return NotificationCompat.Builder(
            this,
            NotificationHelper.SERVER_UPLOAD_NOTIFICATION_CHANNEL
        )
            .setContentTitle(notificationTitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(!stopUpload)
            .addAction(stopAction.build())
            .setUsesChronometer(true)
            .setContentIntent(getActivityIntent())
    }

    override fun onDestroy() {
        stopUpload = true
        isRunning.value = false
        sendBroadcast(Intent(UPLOADER_FINISHED))

        NotificationManagerCompat.from(this)
            .cancel(NotificationHelper.SERVER_UPLOAD_NOTIFICATION_ID)

        lifecycleScope.launch {
            runCatching {
                unregisterReceiver(uploaderReceiver)
            }

            ServiceCompat.stopForeground(this@ServerUploadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            Log.d("ServerUploadService", "onDestroy")
            stopSelf()
            super.onDestroy()
        }
    }

    companion object {
        const val UPLOADER_INTENT_ACTION = "com.connor.hindsight.UPLOADER_ACTION"
        const val ACTION_EXTRA_KEY = "action"
        const val STOP_ACTION = "STOP"
        const val UPLOADER_FINISHED = "com.connor.hindsight.UPLOAD_FINISHED"
        const val UPLOADER_STARTED = "com.connor.hindsight.UPLOAD_STARTED"
        val isRunning = MutableStateFlow(false)
    }
}
