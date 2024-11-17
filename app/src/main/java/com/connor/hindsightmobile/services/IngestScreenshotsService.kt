package com.connor.hindsightmobile.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.MainActivity
import com.connor.hindsightmobile.utils.NotificationHelper
import com.connor.hindsightmobile.R
import com.connor.hindsightmobile.embeddings.SentenceEmbeddingProvider
import com.connor.hindsightmobile.obj.MyObjectBox
import com.connor.hindsightmobile.obj.OCRResult
import com.connor.hindsightmobile.obj.ObjectBoxFrame
import com.connor.hindsightmobile.obj.ObjectBoxFrame_
import com.connor.hindsightmobile.obj.ObjectBoxStore
import com.connor.hindsightmobile.utils.getImageFiles
import com.connor.hindsightmobile.utils.getUnprocessedScreenshotsDirectory
import com.connor.hindsightmobile.utils.getVideoFilesDirectory
import com.connor.hindsightmobile.utils.parseScreenshotFilePath
import com.connor.hindsightmobile.utils.processOCRResultsIngest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.objectbox.Box
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.tasks.await

class IngestScreenshotsService : LifecycleService() {
    val notificationTitle: String = "Hindsight Ingest Screenshots"
    private lateinit var unprocessedScreenshotsDirectory: File
    private lateinit var videoFilesDirectory: File
    private lateinit var dbHelper: DB
    private var stopIngest: Boolean = false

    private val isTest = false

    private lateinit var framesBox: Box<ObjectBoxFrame>

    private val ingesterReceiver = object : BroadcastReceiver() {
        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(IngestScreenshotsService.ACTION_EXTRA_KEY)) {
                STOP_ACTION -> {
                    onDestroy()
                }
            }
        }
    }

    override fun onCreate() {
        Log.d("IngestScreenshotsService", "onCreate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.INGEST_SCREENSHOTS_NOTIFICATION_ID,
                buildNotification().build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                NotificationHelper.INGEST_SCREENSHOTS_NOTIFICATION_ID,
                buildNotification().build()
            )
        }

        isRunning.value = true

        runCatching {
            unregisterReceiver(ingesterReceiver)
        }

        val intentFilter = IntentFilter().apply {
            addAction(IngestScreenshotsService.INGEST_SCREENSHOTS_INTENT_ACTION)
        }
        ContextCompat.registerReceiver(
            this,
            ingesterReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        sendBroadcast(Intent(INGEST_SCREENSHOTS_STARTED))

        unprocessedScreenshotsDirectory = getUnprocessedScreenshotsDirectory(this)

        if (isTest) {
            val dbFile = this.getDatabasePath("hindsight_test.db")
//            if (dbFile.exists()) {
//                if (dbFile.delete()) {
//                    Log.d("IngestScreenshotsService", "Deleted existing database file: ${dbFile.path}")
//                } else {
//                    Log.e("IngestScreenshotsService", "Failed to delete database file: ${dbFile.path}")
//                }
//            }
            dbHelper = DB.getInstance(this@IngestScreenshotsService, "hindsight_test.db")
            videoFilesDirectory = File(getVideoFilesDirectory(this).parent, getVideoFilesDirectory(this).name + "_test")
            if (!videoFilesDirectory.exists()) videoFilesDirectory.mkdirs()

            val testDirectory = File(this.filesDir, "objectbox-test")
//            if (testDirectory.exists()) {
//                testDirectory.deleteRecursively()
//                Log.d("IngestScreenshotsService", "Deleted existing ObjectBox test database")
//            }
            val objectBoxStore = MyObjectBox.builder()
                .androidContext(this)
                .directory(testDirectory)
                .build()

            framesBox = objectBoxStore!!.boxFor(ObjectBoxFrame::class.java)
        } else{
            dbHelper = DB.getInstance(this@IngestScreenshotsService)
            videoFilesDirectory = getVideoFilesDirectory(this)
            framesBox = ObjectBoxStore.store.boxFor(ObjectBoxFrame::class.java)
        }

        lifecycleScope.launch {
            ingestScreenshots()
            onDestroy()
        }

        super.onCreate()
    }

    private fun ingestScreenshotsIntoFrames(screenshotFiles: List<File>){
        screenshotFiles.forEach{ file ->
            if (file.length() > 0) {
                val (fileApplication, fileTimestamp) = parseScreenshotFilePath(file.name)
                if (fileTimestamp != null) {
                    dbHelper.insertFrame(
                        fileTimestamp, fileApplication, null,
                        null
                    )
                }
            } else {
                file.delete()
            }
        }
    }

    private suspend fun runOCR(screenshotFile: File, frameId: Int, recognition: TextRecognizer) {
        Log.d("IngestScreenshotsService", "Running OCR on Screenshot: $screenshotFile")
        val image = InputImage.fromFilePath(this, screenshotFile.toUri())
        try {
            val ocrResult = recognition.process(image).await()
            Log.d("IngestScreenshotsService", "OCR Result Screenshot: $screenshotFile")
            val ocrResults = mutableListOf<OCRResult>()
            var blockNum = 0
            for (block in ocrResult.textBlocks) {
                for (line in block.lines) {
                    val lineText = line.text
                    val lineFrame = line.boundingBox
                    val lineX = lineFrame?.left ?: 0
                    val lineY = lineFrame?.top ?: 0
                    val lineW = lineFrame?.width() ?: 0
                    val lineH = lineFrame?.height() ?: 0
                    val lineConfidence = line.confidence ?: 0f
                    ocrResults.add(OCRResult(lineText, lineX, lineY, lineW, lineH, lineConfidence, blockNum))
                }
                blockNum++
            }
            dbHelper.insertOCRResults(frameId, ocrResults)
        } catch (e: Exception) {
            Log.e("IngestScreenshotsService", "Error performing OCR", e)
        }
    }

    private suspend fun runAllOCR(){
        val framesWithoutOCR = dbHelper.getFramesWithoutOCRResults()
        Log.d("IngestScreenshotsService", "Frames without OCR: ${framesWithoutOCR.size}")

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        framesWithoutOCR.forEach { frame ->
            val frameId = frame[DB.COLUMN_ID] as Int
            val timestamp = frame[DB.COLUMN_TIMESTAMP] as Long
            val application = frame[DB.COLUMN_APPLICATION] as String?
            val applicationDashes = application?.replace(".", "-")
            val screenshotFile = File(unprocessedScreenshotsDirectory, "${applicationDashes}_${timestamp}.webp")

            if (screenshotFile.exists()) {
                try {
                    runOCR(screenshotFile, frameId, recognizer)
                } catch (e: Exception) {
                    Log.e("IngestScreenshotsService", "OCR failed for frameId $frameId", e)
                }
            }
            delay(100)
        }
    }

    private suspend fun embedScreenshot(frameId: Int, timestamp: Long,  application: String, frameText: String,
                                        sentenceEncoder: SentenceEmbeddingProvider) {
        val embedding: FloatArray = sentenceEncoder.encodeText(frameText)
        framesBox.put(ObjectBoxFrame(frameId = frameId, timestamp = timestamp,
            frameText = frameText, embedding = embedding, application = application))
        Log.d("IngestScreenshotsService", "Added Embedding for frameId $frameId")
    }

    private suspend fun embedScreenshots() {
        val ingestedFrameIdsArray: IntArray? = framesBox.query()
                .build()
                .property(ObjectBoxFrame_.frameId)
                .findInts()

        val ingestedFrameIds = ingestedFrameIdsArray?.toList() ?: emptyList()
        val framesWithOCR = dbHelper.getFramesWithOCRResultsNotIn(ingestedFrameIds)
        Log.d("IngestScreenshotsService", "Running embedding on ${framesWithOCR.size} frames")
        val sentenceEncoder = SentenceEmbeddingProvider(this@IngestScreenshotsService)

        for (frame in framesWithOCR) {
            val frameId = frame["frame_id"] as Int
            val timestamp = frame["timestamp"] as Long
            val ocrResults = frame["ocr_results"] as List<Map<String, Any?>>
            val application = frame["application"] as String

            val combinedOCR = processOCRResultsIngest(ocrResults, application)

            embedScreenshot(frameId, timestamp, application, combinedOCR, sentenceEncoder)
            delay(100)
        }
    }

    private fun addVideoChunkToDatabase(screenshotFiles: List<File>, videoFile: File) {
        // Insert video file into the video_chunks table
        val videoChunkId = dbHelper.insertVideoChunk(videoFile.path)
        if (videoChunkId != -1L) {
            // Update each frame with the video chunk ID and offset
            screenshotFiles.forEachIndexed { index, file ->
                val (application, timestamp) = parseScreenshotFilePath(file.name)
                val frameId = dbHelper.getFrameIdByTimestampAndApp(timestamp, application)
                if (frameId != null) {
                    dbHelper.updateFrameWithVideoChunk(frameId, videoChunkId.toInt(), index)
                }
            }
            Log.d("IngestScreenshotsService", "Video chunk and offsets updated for frames")
        } else {
            Log.e("IngestScreenshotsService", "Failed to insert video chunk into database")
        }
    }

    private fun createVideoFromScreenshots(screenshotFiles: List<File>, outputFile: File) {
        // Create a temporary text file listing all screenshot file paths
        val fileList = File(cacheDir, "screenshot_list.txt")
        BufferedWriter(FileWriter(fileList)).use { writer ->
            screenshotFiles.forEach { file ->
                writer.write("file '${file.path}'\n")
                writer.write("duration 2\n")
            }
        }

        val ffmpegCommand = "-f concat -safe 0 -i ${fileList.path} -c:v h264 -pix_fmt yuv420p -r 1 ${outputFile.path}"

        FFmpegKit.execute(ffmpegCommand).also { session ->
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                Log.d("IngestScreenshotsService", "Video created successfully: ${outputFile.path}")
                addVideoChunkToDatabase(screenshotFiles, outputFile)

                screenshotFiles.forEach { file ->
                    if (file.exists()) {
                        if (file.delete()) {
                            Log.d("IngestScreenshotsService", "Deleted screenshot file: ${file.path}")
                        } else {
                            Log.e("IngestScreenshotsService", "Failed to delete screenshot file: ${file.path}")
                        }
                    }
                }
            } else {
                Log.e("IngestScreenshotsService", "Error creating video: ${session.returnCode}")
            }
        }

        // Clean up the temporary file
        fileList.delete()
    }

    private suspend fun compressScreenshotsIntoVideos() {
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = dateFormatter.format(Calendar.getInstance().time)

        val groupedScreenshots = getImageFiles(unprocessedScreenshotsDirectory).groupBy { file ->
            val (application, timestamp) = parseScreenshotFilePath(file.name)
            val date = dateFormatter.format(Date(timestamp ?: 0L))
            Pair(date, application)
        }

        groupedScreenshots.forEach { (dateAppPair, screenshotFiles) ->
            val (date, application) = dateAppPair

            if (date < today && screenshotFiles.isNotEmpty()) { // Don't run on screenshots from today
                // Sort screenshots by timestamp to ensure chronological order
                val sortedScreenshots = screenshotFiles.sortedBy { file ->
                    val (_, timestamp) = parseScreenshotFilePath(file.name)
                    timestamp ?: 0L
                }

                val applicationDashes = application?.replace(".", "-")
                val videoFile = File(videoFilesDirectory, "${applicationDashes}_$date.mp4")
                createVideoFromScreenshots(sortedScreenshots, videoFile)
            }
        }
        Log.d("IngestScreenshotsService", "Compression into videos completed")
    }

    private suspend fun ingestScreenshots() {
        try {
            val screenshotFiles = getImageFiles(unprocessedScreenshotsDirectory)
            val sortedScreenshots = screenshotFiles.sortedBy { file ->
                val (_, timestamp) = parseScreenshotFilePath(file.name)
                timestamp ?: 0L
            }
            Log.d("IngestScreenshotsService", "Ingesting screenshots")
            ingestScreenshotsIntoFrames(sortedScreenshots)
            runAllOCR()
            embedScreenshots()
            compressScreenshotsIntoVideos()

        } catch (e: Exception) {
            Log.e("IngestScreenshotsService", "Error during ingestion", e)
        }
    }

    private fun getPendingIntent(intent: Intent, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun getActivityIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            6,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(): NotificationCompat.Builder {
        val stopIntent = Intent(IngestScreenshotsService.INGEST_SCREENSHOTS_INTENT_ACTION).putExtra(
            IngestScreenshotsService.ACTION_EXTRA_KEY,
            IngestScreenshotsService.STOP_ACTION
        )
        val stopAction = NotificationCompat.Action.Builder(
            null,
            getString(R.string.stop),
            getPendingIntent(stopIntent, 2)
        )

        return NotificationCompat.Builder(
            this,
            NotificationHelper.RECORDING_NOTIFICATION_CHANNEL
        )
            .setContentTitle(notificationTitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(!stopIngest)
            .addAction(stopAction.build())
            .setUsesChronometer(true)
            .setContentIntent(getActivityIntent())
    }

    override fun onDestroy() {
        Log.d("IngestScreenshotsService", "onDestroy")
        sendBroadcast(Intent(INGEST_SCREENSHOTS_FINISHED))
        stopIngest = true
        isRunning.value = false

        lifecycleScope.launch {
            runCatching {
                unregisterReceiver(ingesterReceiver)
            }

            ServiceCompat.stopForeground(this@IngestScreenshotsService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            Log.d("IngestScreenshotsService", "onDestroy")
            stopSelf()
            super.onDestroy()
        }
    }

    companion object {
        const val INGEST_SCREENSHOTS_INTENT_ACTION = "com.connor.hindsightmobile.INGEST_SCREENSHOTS_ACTION"
        const val ACTION_EXTRA_KEY = "action"
        const val STOP_ACTION = "STOP"
        const val INGEST_SCREENSHOTS_FINISHED = "com.connor.hindsightmobile.INGEST_SCREENSHOTS_FINISHED"
        const val INGEST_SCREENSHOTS_STARTED = "com.connor.hindsightmobile.INGEST_SCREENSHOTS_STARTED"
        val isRunning = MutableStateFlow(false)
    }
}
