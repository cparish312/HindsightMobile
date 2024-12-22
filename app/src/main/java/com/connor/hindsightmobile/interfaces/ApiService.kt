package com.connor.hindsightmobile.interfaces

import com.connor.hindsightmobile.obj.OCRResult
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("get_last_timestamp")
    suspend fun getLastTimestamp(@Query("table") tableName: String): Response<TimestampResponse>

    @POST("add_frames")
    suspend fun syncDB(@Body addFramesData: AddFramesData): Response<ResponseBody>

    @GET("ping")
    fun pingServer(): Call<ResponseBody>
}

data class AddFramesData(
    val frames: List<Frame>,
    val source: String = "hindsightmobile",
)

data class Frame(
    val id: Int,
    val timestamp: Long,
    val application: String,
    val ocr_results: List<OCRResult>,
)

data class TimestampResponse(
    val last_timestamp: Long?
)