package com.connor.hindsightmobile

import android.app.Application
import android.util.Log
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

        // Create a crash log file
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrashToFile(throwable)
            } catch (e: Exception) {
                Log.e("App", "Error logging crash", e)
            } finally {
                // Invoke the default handler if it's not the custom handler itself
                Thread.getDefaultUncaughtExceptionHandler()?.let { defaultHandler ->
                    if (defaultHandler != Thread.currentThread().uncaughtExceptionHandler) {
                        defaultHandler.uncaughtException(thread, throwable)
                    } else {
                        Log.e("App", "No valid default handler found")
                    }
                }
            }
        }
    }

    private fun logCrashToFile(throwable: Throwable) {
        try {
            val logFile = File(getExternalFilesDir(null), "crash_logs.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
                Log.d("App", "Crash log file created successfully.")
            }
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