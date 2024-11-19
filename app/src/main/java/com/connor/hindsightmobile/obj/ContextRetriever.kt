package com.connor.hindsightmobile.obj

import android.content.Context
import android.util.Log
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.embeddings.SentenceEmbeddingProvider
import com.connor.hindsightmobile.utils.convertToLocalTime
import com.connor.hindsightmobile.utils.processOCRResultsRetrieveContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ContextRetriever(context : Context){
    private val framesBox = ObjectBoxStore.store.boxFor(ObjectBoxFrame::class.java)
    private val sentenceEncoder = SentenceEmbeddingProvider(context)
    private val dbHelper: DB = DB.getInstance(context)

    suspend fun getContext(query: String, nContexts: Int = 3, nSeconds: Int = 120): QueryResults = suspendCoroutine { continuation ->
        CoroutineScope(Dispatchers.IO).launch {
            val queryEmbedding: FloatArray = sentenceEncoder.encodeText(query)
            val allQueryResults = framesBox
                .query(ObjectBoxFrame_.embedding.nearestNeighbors(queryEmbedding, 200)
                    .and(ObjectBoxFrame_.application.notEqual("com.connor.hindsightmobile"))
                    .and(ObjectBoxFrame_.application.notEqual("com.google.android.inputmethod.latin"))
                    .and(ObjectBoxFrame_.application.notEqual("com.google.android.apps.nexuslauncher"))
                    .and(ObjectBoxFrame_.application.notEqual("com.android.pixeldisplayservice"))
                )
                .build()
                .findWithScores()

            if (allQueryResults.isEmpty()) {
                continuation.resume(QueryResults("", emptyList()))
                return@launch
            }

            // Map scores to frames and sort by timestamp in descending order (most recent first)
            val results = allQueryResults
                .map { Pair(it.score.toFloat(), it.get()) }

            val qualityFilteredResults = results.filter { (_, frame) ->
                frame.frameText.toString().length >= 20 &&
                        !frame.frameText.toString().matches(Regex("^[\\s\\d.,:]*$"))
                }.sortedByDescending { it.second.timestamp }

            // Filter to keep only the first (most recent) frame in each n_seconds time group
            val filteredResults = mutableListOf<Pair<Float, ObjectBoxFrame>>()
            var lastTimestamp: Long? = null

            for ((score, frame) in qualityFilteredResults) {
                if (lastTimestamp == null || lastTimestamp!! - frame.timestamp > nSeconds * 1000) {
                    filteredResults.add(Pair(score, frame))
                    lastTimestamp = frame.timestamp
                }
            }

            val timeDecayedResults = mutableListOf<Pair<Float, ObjectBoxFrame>>()
            for ((score, frame) in filteredResults) {
                val timeAgoInHours = (System.currentTimeMillis() - frame.timestamp) / (1000.0 * 60 * 60)
                val timeDecayFactor = 1.0 / (1 + 0.001 * timeAgoInHours)
                val combinedDistance = score * (1 - timeDecayFactor)
                timeDecayedResults.add(Pair(combinedDistance.toFloat(), frame))
            }

            // Sort by score in descending order and take the top n (Note score is actually distance so we want smal)
            val topResultsByScore = timeDecayedResults
                .sortedBy { it.first }
                .take(nContexts)

//            // Get surrounding context from nearby timestamps
//            private suspend fun getContextWindow(frame: ObjectBoxFrame, windowSeconds: Int = 30): String {
//                val surroundingFrames = framesBox
//                    .query(ObjectBoxFrame_.timestamp
//                        .between(frame.timestamp - windowSeconds * 1000,
//                                frame.timestamp + windowSeconds * 1000))
//                    .build()
//                    .find()
//                return surroundingFrames
//                    .sortedBy { it.timestamp }
//                    .joinToString("\n") { it.frameText.toString() }
//            }

            // Sort the top n results by timestamp in ascending order
            val finalResults = topResultsByScore.sortedBy { it.second.timestamp }
            val contextFrameIds = finalResults.map { it.second.frameId }
            val framesWithOCR = dbHelper.getFramesWithOCRResults(contextFrameIds)

            val retrievedContextList = ArrayList<RetrievedContext>()
            var contextString = ""
            for (frame in framesWithOCR) {
                val frameId = frame["frame_id"] as Int
                val timestamp = frame["timestamp"] as Long
                val ocrResults = frame["ocr_results"] as List<Map<String, Any?>>
                val application = frame["application"] as String

                // Process the OCR results to get the formatted text
                val processedText = processOCRResultsRetrieveContext(ocrResults, application, timestamp)

                // Add the processed text to the retrieved context list
                retrievedContextList.add(RetrievedContext(frameId, processedText))

                // Append the processed text to the context string
                contextString += processedText + "\n\n"
            }

            continuation.resume(QueryResults(contextString, retrievedContextList))
        }
    }
}