package com.connor.hindsightmobile.utils

import android.util.Log
import com.connor.hindsightmobile.interfaces.ApiService
import com.connor.hindsightmobile.obj.RetrofitClient
import okhttp3.ResponseBody

interface ServerConnectionCallback {
    fun onServerStatusChecked(isConnected: Boolean)
}
fun checkServerConnection(serverUrl: String, callback: ServerConnectionCallback) {
    val retrofit = RetrofitClient.getInstance(serverUrl)
    val client = retrofit.create(ApiService::class.java)
    val call = client.pingServer()

    call.enqueue(object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(
            call: retrofit2.Call<ResponseBody>,
            response: retrofit2.Response<ResponseBody>
        ) {
            if (response.isSuccessful) {
                Log.d("PostService", "Server is reachable. Initializing service.")
                callback.onServerStatusChecked(true)
            } else {
                Log.e("PostService", "Server connection failed. Cannot start upload.")
                callback.onServerStatusChecked(false)
            }
        }

        override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
            Log.e("PostService", "Failed to connect to server: ${t.message}")
            callback.onServerStatusChecked(false)
        }
    })
}
