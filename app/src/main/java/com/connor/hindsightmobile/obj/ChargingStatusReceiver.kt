package com.connor.hindsightmobile.obj

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log

class ChargingStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            if (isCharging) {
                Log.d("ChargingStatus", "Device is charging.")
                UserActivityState.phoneCharging = true
            } else {
                Log.d("ChargingStatus", "Device is not charging.")
                UserActivityState.phoneCharging = false
            }
        }
    }
}
