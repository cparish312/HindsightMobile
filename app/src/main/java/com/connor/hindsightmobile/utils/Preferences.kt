package com.connor.hindsightmobile.utils

import android.content.Context
import android.content.SharedPreferences

object Preferences {
    private const val PREF_FILE_NAME = "Hindsight"
    lateinit var prefs: SharedPreferences

    const val screenrecordingenabled = "ScreenRecordingEnabled"
    const val recordwhenactive = "RecordWhenActive"
    const val autoingestenabled = "AutoIngest"
    const val autoingestwhennotcharging = "AutoIngestWhenNotCharging"
    const val defaultllmname = "DefaultLLMName"
    const val defaultrecordapps = "DefaultRecordApps"
    const val lastingesttimestamp = "LastingestTimestamp"
    const val locationtrackingenabled = "LocationTrackingEnabled"
    const val apikey = "ApiKey"
    const val interneturl = "InternetUrl"
    const val sourcename = "SourceName"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

        if (!prefs.contains(screenrecordingenabled)) {
            prefs.edit().putBoolean(screenrecordingenabled, false).apply()
        }

        if (!prefs.contains(recordwhenactive)) {
            prefs.edit().putBoolean(recordwhenactive, true).apply()
        }

        if (!prefs.contains(autoingestenabled)) {
            prefs.edit().putBoolean(autoingestenabled, true).apply()
        }

        if (!prefs.contains(autoingestwhennotcharging)) {
            prefs.edit().putBoolean(autoingestwhennotcharging, false).apply()
        }

        if (!prefs.contains(defaultrecordapps)) {
            prefs.edit().putBoolean(defaultrecordapps, false).apply()
        }

        if (!prefs.contains(lastingesttimestamp)) {
            prefs.edit().putLong(lastingesttimestamp, 0).apply()
        }

        if (!prefs.contains(locationtrackingenabled)) {
            prefs.edit().putBoolean(locationtrackingenabled, false).apply()
        }

        if (!prefs.contains(apikey)) {
            prefs.edit().putString(apikey, "").apply()
        }

        if (!prefs.contains(interneturl)) {
            prefs.edit().putString(interneturl, "").apply()
        }

        if (!prefs.contains(sourcename)) {
            prefs.edit().putString(sourcename, "hindsightmobile").apply()
        }
    }
}