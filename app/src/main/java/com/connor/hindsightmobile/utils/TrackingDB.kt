package com.connor.hindsightmobile.utils

import android.content.Context
import com.connor.hindsightmobile.DB
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


fun observeNumFrames(context: Context): Flow<Int> = flow {
    val dbHelper = DB.getInstance(context)
    while (true) {
        dbHelper.getNumFrames()?.let { numFrames ->
            emit(numFrames)
        }
        delay(1000)
    }
}

fun observeLastIngestTime(): Flow<String> = flow {
    while (true) {
        val lastTimestamp = Preferences.prefs.getLong(Preferences.lastingesttimestamp, 0L)
        val lastIngestTime = convertToLocalTime(lastTimestamp)
        emit(lastIngestTime)
        delay(3000L) // Poll every second (adjust as needed)
    }
}