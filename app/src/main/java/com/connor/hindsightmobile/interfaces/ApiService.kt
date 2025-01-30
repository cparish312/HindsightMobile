package com.connor.hindsightmobile.interfaces

import com.connor.hindsightmobile.obj.OCRResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @GET("get_last_timestamp")
    suspend fun getLastTimestamp(@Query("table") tableName: String): Response<TimestampResponse>

    @GET("get_last_id")
    suspend fun getLastId(@Query("source") source: String,
                               @Query("table") tableName: String = "frames"): Response<LastIdResponse>

    @POST("sync_db")
    suspend fun syncDB(@Body syncDBDate: syncDBData): Response<ResponseBody>

    @Multipart
    @POST("upload_video")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part,
        @Part("source") source: RequestBody,
        @Part("video_chunk_id") videoChunkId: RequestBody,
        @Part("frame_ids") frameIds: RequestBody
    ): Response<ResponseBody>

    @GET("ping")
    fun pingServer(): Call<ResponseBody>
}

data class syncDBData(
    val source: String,
    val frames: List<Frame> = emptyList(),
    val locations: List<Location> = emptyList(),
)

data class Frame(
    val id: Int,
    val timestamp: Long,
    val application: String,
    val ocr_results: List<OCRResult>,
)

data class Location(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class TimestampResponse(
    val last_timestamp: Long?
)

data class LastIdResponse(
    val last_id: Int?
)

data class VideoChunk(
    val id: Int,
    val path: String,
    val frames: List<Frame>
)