package com.connor.hindsightmobile

import android.app.Application
import com.connor.hindsightmobile.obj.ObjectBoxStore
import com.connor.hindsightmobile.utils.NotificationHelper
import com.connor.hindsightmobile.utils.Preferences
import com.connor.llamacpp.LlamaCpp
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.Date

class App : Application() {
    val llamaCpp = LlamaCpp()

    override fun onCreate() {
        super.onCreate()
        Preferences.init(this)
        NotificationHelper.buildNotificationChannels(this)
        ObjectBoxStore.init(this)
        llamaCpp.init()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrashToFile(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logCrashToFile(throwable: Throwable) {
        try {
            val logFile = File(getExternalFilesDir(null), "crash_logs.txt")
            val logWriter = PrintWriter(FileOutputStream(logFile, true))
            logWriter.println("Crash Log: ${Date()}")
            throwable.printStackTrace(logWriter)
            logWriter.println()
            logWriter.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}