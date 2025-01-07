package com.connor.hindsightmobile.obj

data class RetrievedContext(val frameId: Int, val context: String)

data class QueryResults(val contextString: String, val contextList: List<RetrievedContext>)