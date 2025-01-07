package com.connor.hindsightmobile.ui.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.models.RecorderModel
import com.connor.hindsightmobile.services.BackgroundRecorderService
import com.connor.hindsightmobile.services.IngestScreenshotsService
import com.connor.hindsightmobile.services.ServerUploadService
import com.connor.hindsightmobile.utils.Preferences
import com.connor.hindsightmobile.utils.deleteRecentScreenshotsData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SettingsViewModel(val app: Application) : AndroidViewModel(app) {
    private val dbHelper: DB = DB.getInstance(app)

    private val _screenRecordingEnabled = MutableStateFlow(
        Preferences.prefs.getBoolean(Preferences.screenrecordingenabled, false)
    )
    val screenRecordingEnabled = _screenRecordingEnabled.asStateFlow()

    private val _locationTrackingEnabled = MutableStateFlow(
        Preferences.prefs.getBoolean(Preferences.locationtrackingenabled, false)
    )
    val locationTrackingEnabled = _locationTrackingEnabled.asStateFlow()

    private val _defaultRecordApps = MutableStateFlow(
        Preferences.prefs.getBoolean(Preferences.defaultrecordapps, false)
    )
    val defaultRecordApps = _defaultRecordApps.asStateFlow()

    private val _autoIngestEnabled = MutableStateFlow(
        Preferences.prefs.getBoolean(Preferences.autoingestenabled, false)
    )
    val autoIngestEnabled = _autoIngestEnabled.asStateFlow()

    private val _autoIngestWhenNotCharging = MutableStateFlow(
        Preferences.prefs.getBoolean(Preferences.autoingestwhennotcharging, false)
    )
    val autoIngestWhenNotCharging = _autoIngestWhenNotCharging.asStateFlow()

    val isIngesting = IngestScreenshotsService.isRunning.asStateFlow()

    val isUploading = ServerUploadService.isRunning.asStateFlow()

    private val _eventChannel = Channel<UIEvent>()
    val events = _eventChannel.receiveAsFlow()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BackgroundRecorderService.SCREEN_RECORDER_STOPPED -> {
                    Log.d("MainViewModel", "SCREEN_RECORDER_STOPPED")
                    _screenRecordingEnabled.value = false
                    Preferences.prefs.edit().putBoolean(
                        Preferences.screenrecordingenabled,
                        _screenRecordingEnabled.value
                    ).apply()
                }
                RecorderModel.SCREEN_RECORDER_PERMISSION_DENIED -> {
                    Log.d("MainViewModel", RecorderModel.SCREEN_RECORDER_PERMISSION_DENIED)
                    _screenRecordingEnabled.value = false
                    Preferences.prefs.edit().putBoolean(
                        Preferences.screenrecordingenabled,
                        _screenRecordingEnabled.value
                    ).apply()
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter().apply {
            addAction(BackgroundRecorderService.SCREEN_RECORDER_STOPPED)
            addAction(RecorderModel.SCREEN_RECORDER_PERMISSION_DENIED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                broadcastReceiver,
                intentFilter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            getApplication<Application>().registerReceiver(broadcastReceiver, intentFilter,
                Context.RECEIVER_EXPORTED)
        }
    }

    fun toggleScreenRecording() {
        _screenRecordingEnabled.value = !_screenRecordingEnabled.value
        Preferences.prefs.edit().putBoolean(Preferences.screenrecordingenabled, _screenRecordingEnabled.value)
            .apply()

        viewModelScope.launch {
            if (_screenRecordingEnabled.value) {
                _eventChannel.send(UIEvent.RequestScreenCapturePermission)
            } else {
                _eventChannel.send(UIEvent.StopScreenRecording)
            }
        }
    }

    fun toggleLocationTracking() {
        _locationTrackingEnabled.value = !_locationTrackingEnabled.value
        Preferences.prefs.edit().putBoolean(Preferences.locationtrackingenabled, _locationTrackingEnabled.value)
            .apply()
    }

    fun toggleDefaultRecordApps() {
        _defaultRecordApps.value = !_defaultRecordApps.value
        Preferences.prefs.edit().putBoolean(Preferences.defaultrecordapps, _defaultRecordApps.value)
            .apply()

        val intent = Intent(ManageRecordingsViewModel.RECORDING_SETTTINGS_UPDATED)
        getApplication<Application>().sendBroadcast(intent)
    }

    fun toggleAutoIngest() {
        _autoIngestEnabled.value = !_autoIngestEnabled.value
        Preferences.prefs.edit().putBoolean(Preferences.autoingestenabled, _autoIngestEnabled.value)
            .apply()
    }

    fun toggleAutoIngestWhenNotCharging() {
        _autoIngestWhenNotCharging.value = !_autoIngestWhenNotCharging.value
        Preferences.prefs.edit().putBoolean(Preferences.autoingestwhennotcharging, _autoIngestWhenNotCharging.value)
            .apply()
    }

    fun deleteRecentScreenshots(millisecondsToDelete: Long){
        deleteRecentScreenshotsData(millisecondsToDelete, getApplication(), dbHelper)
    }

    sealed class UIEvent {
        object RequestScreenCapturePermission : UIEvent()
        object StopScreenRecording : UIEvent()
    }
}