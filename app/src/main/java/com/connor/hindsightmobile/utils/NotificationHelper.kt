package com.connor.hindsightmobile.utils

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.connor.hindsightmobile.R

object NotificationHelper {
    const val RECORDING_NOTIFICATION_CHANNEL = "active_recording"
    const val INGESTING_NOTIFICATION_CHANNEL = "active_ingesting"
    const val RECORDING_NOTIFICATION_ID = 1
    const val INGEST_SCREENSHOTS_NOTIFICATION_ID = 2

    fun buildNotificationChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        listOf(
            RECORDING_NOTIFICATION_CHANNEL to R.string.active_recording,
            INGESTING_NOTIFICATION_CHANNEL to R.string.active_ingesting
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
