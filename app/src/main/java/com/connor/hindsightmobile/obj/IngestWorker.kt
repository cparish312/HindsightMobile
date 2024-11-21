package com.connor.hindsightmobile.obj

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.connor.hindsightmobile.services.IngestScreenshotsService
import com.connor.hindsightmobile.utils.Preferences

class IngestWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            if (UserActivityState.phoneCharging || Preferences.prefs.getBoolean(Preferences.autoingestwhennotcharging, false)) {
                if (!IngestScreenshotsService.isRunning.value) {
                    Log.d("IngestWorker", "Starting Ingest")
                    val ingestIntent = Intent(applicationContext, IngestScreenshotsService::class.java)
                    ContextCompat.startForegroundService(applicationContext, ingestIntent)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("IngestWorker", "Error in doWork", e)
            return Result.failure()
        }
    }
}