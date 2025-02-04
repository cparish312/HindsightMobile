package com.connor.hindsightmobile.obj

import android.content.Context
import com.connor.hindsightmobile.utils.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object UserActivityState {
    @Volatile var userActive: Boolean = false

    @Volatile var screenOn: Boolean = true

    @Volatile var phoneCharging: Boolean = false

    @Volatile var currentApplication: String? = "screenshot"

    private val _lastScreenshotTimestamp = MutableStateFlow<Long>(0L)
    val lastScreenshotTimestamp: StateFlow<Long> = _lastScreenshotTimestamp

    private val _lastLocationTimestamp = MutableStateFlow<Long>(0L)
    val lastLocationTimestamp: StateFlow<Long> = _lastLocationTimestamp

    fun loadLastTimestamps(context: Context){
        _lastScreenshotTimestamp.value = Preferences.prefs.getLong(Preferences.lastscreenshottimestamp, 0L)
        _lastLocationTimestamp.value = Preferences.prefs.getLong(Preferences.lastlocationtimestamp, 0L)
    }

    fun updateLastScreenshotTimestamp(context: Context, timestamp: Long) {
        _lastScreenshotTimestamp.value = timestamp

        Preferences.prefs.edit().putLong(Preferences.lastscreenshottimestamp, timestamp).apply()
    }

    fun updateLastLocationTimestamp(context: Context, timestamp: Long) {
        _lastLocationTimestamp.value = timestamp

        Preferences.prefs.edit().putLong(Preferences.lastlocationtimestamp, timestamp).apply()
    }
}
