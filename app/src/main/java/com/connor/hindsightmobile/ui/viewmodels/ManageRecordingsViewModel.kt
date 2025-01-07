package com.connor.hindsightmobile.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.ui.elements.AppInfo
import com.connor.hindsightmobile.utils.deleteAppData
import kotlinx.coroutines.launch

class ManageRecordingsViewModel(val app: Application): AndroidViewModel(app) {
    private val dbHelper: DB = DB.getInstance(app)
    var appList = mutableStateListOf<AppInfo>()
        private set


    init {
        viewModelScope.launch {
            val apps = dbHelper.getAllApps()
                .sortedByDescending { it.numFrames }
            appList.addAll(apps)
        }
        Log.d("ManageRecordingsViewModel", "App List: $appList")
    }

    fun toggleAppIsRecording(app: AppInfo) {
        val newRecordingStatus = !app.isRecording

        val index = appList.indexOfFirst { it.packageName == app.packageName }
        if (index != -1) {
            appList[index] = appList[index].copy(isRecording = newRecordingStatus)
        }

        dbHelper.updateAppRecordStatus(app.packageName, newRecordingStatus)

        val intent = Intent(RECORDING_SETTTINGS_UPDATED)
        getApplication<Application>().sendBroadcast(intent)
        Log.d("ManageRecordingsViewModel", "${app.appName} is now recording: $newRecordingStatus")
    }

    // Deletes data for a specific app
    fun deleteAppData(app: AppInfo) {
        deleteAppData(app, getApplication(), dbHelper)
    }

    companion object {
        const val RECORDING_SETTTINGS_UPDATED = "com.connor.hindsightmobile.RECORDINGS_SETTTINGS_UPDATED"
    }
}