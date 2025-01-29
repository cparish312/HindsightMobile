package com.connor.hindsightmobile.obj

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object UserActivityState {
    @Volatile var userActive: Boolean = false

    @Volatile var screenOn: Boolean = true

    @Volatile var phoneCharging: Boolean = false

    @Volatile var currentApplication: String? = "screenshot"

    private val _lastScreenshotTimestamp = MutableStateFlow<Long>(0L)
    val lastScreenshotTimestamp: StateFlow<Long> = _lastScreenshotTimestamp

    fun updateLastScreenshotTimestamp(timestamp: Long) {
        _lastScreenshotTimestamp.value = timestamp
    }

    private val _lastLocationTimestamp = MutableStateFlow<Long>(0L)
    val lastLocationTimestamp: StateFlow<Long> = _lastLocationTimestamp

    fun updateLastLocationTimestamp(timestamp: Long) {
        _lastLocationTimestamp.value = timestamp
    }
}
