package com.connor.hindsightmobile.utils

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.connor.hindsightmobile.R

object NotificationHelper {
    const val RECORDING_NOTIFICATION_CHANNEL = "active_recording"
    const val INGESTING_NOTIFICATION_CHANNEL = "active_ingesting"
    const val SERVER_UPLOAD_NOTIFICATION_CHANNEL = "server_upload"
    const val SCREEN_RECORDING_NOTIFICATION_CHANNEL = "active_screen_recording"
    const val RECORDING_NOTIFICATION_ID = 1
    const val INGEST_SCREENSHOTS_NOTIFICATION_ID = 2
    const val SERVER_UPLOAD_NOTIFICATION_ID = 3
    const val SCREEN_RECORDING_NOTIFICATION_ID = 4

    fun buildNotificationChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        listOf(
            RECORDING_NOTIFICATION_CHANNEL to R.string.active_recording,
            INGESTING_NOTIFICATION_CHANNEL to R.string.active_ingesting,
            SERVER_UPLOAD_NOTIFICATION_CHANNEL to R.string.server_uploading,
            SCREEN_RECORDING_NOTIFICATION_CHANNEL to R.string.active_screen_recording
        ).forEach { (channelName, stringResource) ->
            val channelCompat = NotificationChannelCompat.Builder(
                channelName,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName(context.getString(stringResource))
                .setLightsEnabled(true)
                .setShowBadge(true)
                .setVibrationEnabled(true)
                .build()

            notificationManager.createNotificationChannel(channelCompat)
        }
    }
}
