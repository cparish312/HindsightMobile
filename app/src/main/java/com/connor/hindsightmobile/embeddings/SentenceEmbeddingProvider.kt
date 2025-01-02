package com.connor.hindsightmobile.embeddings

import android.content.Context
import com.connor.hindsightmobile.utils.getAssetFile
import com.ml.shubham0204.sentence_embeddings.SentenceEmbedding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SentenceEmbeddingProvider(private val context: Context) {

    private val sentenceEmbedding = SentenceEmbedding()
    private val initCompleted = CompletableDeferred<Unit>()

    init {
        val modelFile = getAssetFile(context, "all-MiniLM-L6-V2.onnx")
        val tokenizerFile = getAssetFile(context, "tokenizer.json")
        val tokenizerBytes = tokenizerFile.readBytes()
        CoroutineScope(Dispatchers.IO).launch {
            sentenceEmbedding.init(
                modelFilepath = modelFile.absolutePath,
                tokenizerBytes = tokenizerBytes,
                useTokenTypeIds = false,
                outputTensorName = "sentence_embedding",
                useFP16 = false,
                useXNNPack = false,
                normalizeEmbeddings = true
            )
            initCompleted.complete(Unit)
        }
    }

    suspend fun encodeText(text: String): FloatArray = withContext(Dispatchers.IO) {
        initCompleted.await()
        sentenceEmbedding.encode(text)
    }

    fun close() {
        sentenceEmbedding.close()
    }
}
