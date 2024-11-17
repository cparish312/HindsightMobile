package com.connor.hindsightmobile.services

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.obj.UserActivityState
import com.connor.hindsightmobile.utils.Preferences

class UserActivityTrackingService : AccessibilityService() {
    private lateinit var dbHelper: DB
    private lateinit var appPackages: HashSet<String>

    private val excludedPackages = listOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.inputmethod.latin", // Trade off of knowing the keyboard is in use and ease of organizing with app being used
        "com.google.android.apps.nexuslauncher",
        "com.android.pixeldisplayservice"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        UserActivityState.userActive = true
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            try {
                event.packageName?.let {
                    val packageName = event.packageName.toString()
                    if (excludedPackages.contains(packageName)) {
                        return
                    }

                    UserActivityState.currentApplication = packageName
                    Log.d("UserActivityTrackingService", "onAccessibilityEvent: $packageName")

                    if (packageName !in appPackages) {
                        val appName = getAppNameFromPackageName(packageName)
                        val recordNewApp = Preferences.prefs.getBoolean(Preferences.defaultrecordapps, false)
                        dbHelper.insertApp(packageName, appName, recordNewApp)
                        appPackages.add(packageName)
                    }
                }
            } catch(e: Error){
                Log.d("UserActivityTrackingService", "Error getting packageName", e)
            }
        }
    }

    override fun onCreate() {
        Log.d("UserActivityTrackingService", "onCreate")
        dbHelper = DB.getInstance(this)
        appPackages = dbHelper.getAppPackages()
        super.onCreate()
    }
    override fun onInterrupt() {
    }

    private fun getAppNameFromPackageName(packageName: String): String? {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("UserActivityTrackingService", "App not found for package: $packageName", e)
            null
        }
    }
}
