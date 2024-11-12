package com.connor.hindsightmobile

import android.app.Application
import com.connor.hindsightmobile.obj.ObjectBoxStore
import com.connor.hindsightmobile.utils.NotificationHelper
import com.connor.hindsightmobile.utils.Preferences
import com.connor.llamacpp.LlamaCpp

class App : Application() {
    val llamaCpp = LlamaCpp()

    override fun onCreate() {
        super.onCreate()
        Preferences.init(this)
        NotificationHelper.buildNotificationChannels(this)
        ObjectBoxStore.init(this)
        llamaCpp.init()
    }
}