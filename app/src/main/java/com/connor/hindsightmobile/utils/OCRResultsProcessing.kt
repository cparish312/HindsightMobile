package com.connor.hindsightmobile.utils

private fun isNonMeaningfulText(text: String): Boolean {
    val nonMeaningfulWords = setOf(
        "OK", "Cancel", "Yes", "No", "Back", "Home", "Settings",
        "AM", "PM", "Battery", "Search", "Send", "Recent", "More"
    )

    val nonMeaningfulPatterns = listOf(
        Regex("^\\d{1,2}:\\d{2}\$"),             // Time like 12:34
        Regex("^\\d{1,2}:\\d{2}\\s?(AM|PM)\$"),  // Time like 12:34 PM
        Regex("^\\d{1,3}%\$"),                   // Battery percentage like 100%
        Regex("^\\d{1,2}/\\d{1,2}/\\d{2,4}\$"),  // Date like 12/31/2023
        Regex("^\\s*$")                          // Empty or whitespace-only strings
    )

    return text in nonMeaningfulWords || nonMeaningfulPatterns.any { it.matches(text) }
}


fun processOCRResultsIngest(ocrResults: List<Map<String, Any?>>, appPackageName: String): String {
    // Filter out non-meaningful text
    val meaningfulTexts = ocrResults.filter { ocrResult ->
        val text = (ocrResult["text"] as? String)?.trim() ?: ""
        text.isNotEmpty() && !isNonMeaningfulText(text)
    }

    // Group texts by block_num
    val textsByBlockNum = meaningfulTexts.groupBy { it["block_num"] as Int }

    // Sort the blocks by block_num to maintain order
    val sortedBlocks = textsByBlockNum.entries.sortedBy { entry ->
        entry.value.minOf { it["y"] as Int }
    }

    val paragraphs = sortedBlocks.map { (_, blockTexts) ->
        val sortedBlockTexts = blockTexts.sortedWith(
            compareBy({ it["y"] as Int }, { it["x"] as Int })
        )
        sortedBlockTexts.joinToString(separator = " ") { it["text"] as String }
    }

    val ocrText = paragraphs.joinToString(separator = "\n\n")

    return "Screenshot of $appPackageName\n\n$ocrText"
}

fun processOCRResultsRetrieveContext(ocrResults: List<Map<String, Any?>>,
                                     appPackageName: String,
                                     timestamp: Long): String {
    // Filter out non-meaningful text
    val meaningfulTexts = ocrResults.filter { ocrResult ->
        val text = (ocrResult["text"] as? String)?.trim() ?: ""
        text.isNotEmpty() && !isNonMeaningfulText(text)
    }

    // Group texts by block_num
    val textsByBlockNum = meaningfulTexts.groupBy { it["block_num"] as Int }

    // Sort the blocks by block_num to maintain order
    val sortedBlocks = textsByBlockNum.entries.sortedBy { entry ->
        entry.value.minOf { it["y"] as Int }
    }

    val paragraphs = sortedBlocks.map { (_, blockTexts) ->
        val sortedBlockTexts = blockTexts.sortedWith(
            compareBy({ it["y"] as Int }, { it["x"] as Int })
        )
        sortedBlockTexts.joinToString(separator = " ") { it["text"] as String }
    }

    val ocrText = paragraphs.joinToString(separator = "\n\n")
    val localTime = convertToLocalTime(timestamp)

    return "Screenshot of $appPackageName at $localTime\n\n$ocrText"
}

//fun processOCRResultsRetrieveContext(
//    ocrResults: List<Map<String, Any?>>,
//    appPackageName: String,
//    timestamp: Long
//): String {
//    // Filter out non-meaningful text
//    val meaningfulTexts = ocrResults.filter { ocrResult ->
//        val text = (ocrResult["text"] as? String)?.trim() ?: ""
//        text.isNotEmpty() && !isNonMeaningfulText(text)
//    }
//
//    // Remove sequences of 3 or more consecutive results with text length < 8
//    val filteredTexts = mutableListOf<Map<String, Any?>>()
//    var shortTextCount = 0
//
//    for (ocrResult in meaningfulTexts) {
//        val text = (ocrResult["text"] as? String)?.trim() ?: ""
//        if (text.length < 8) {
//            shortTextCount++
//        } else {
//            shortTextCount = 0
//        }
//
//        // Include the result only if the short text count is less than 3
//        if (shortTextCount < 3) {
//            filteredTexts.add(ocrResult)
//        } else if (shortTextCount == 3) {
//            // Remove the last two entries when we detect a streak of 3
//            repeat(2) { filteredTexts.removeLastOrNull() }
//        }
//    }
//
//    // Define thresholds for line grouping
//    val yThreshold = 10  // Adjust based on OCR precision
//    val xThreshold = 10  // For detecting columns
//
//    // Group texts into lines
//    val lines = mutableListOf<MutableList<Map<String, Any?>>>()
//
//    for (ocrResult in filteredTexts.sortedBy { it["y"] as Int }) {
//        val y = ocrResult["y"] as Int
//
//        // Try to find a line this text belongs to
//        val line = lines.find { line ->
//            val lineY = line.last()["y"] as Int
//            abs(lineY - y) <= yThreshold
//        }
//
//        if (line != null) {
//            line.add(ocrResult)
//        } else {
//            // Start a new line
//            lines.add(mutableListOf(ocrResult))
//        }
//    }
//
//    // Detect columns based on x positions
//    val columns = mutableListOf<MutableList<Map<String, Any?>>>()
//    for (line in lines) {
//        // Sort texts in the line by x position
//        val sortedLine = line.sortedBy { it["x"] as Int }
//        val lineTexts = mutableListOf<MutableList<Map<String, Any?>>>()
//
//        for (textElement in sortedLine) {
//            val x = textElement["x"] as Int
//
//            // Try to find a column this text belongs to
//            val column = lineTexts.find { col ->
//                val colX = col.last()["x"] as Int
//                abs(colX - x) <= xThreshold
//            }
//
//            if (column != null) {
//                column.add(textElement)
//            } else {
//                // Start a new column in this line
//                lineTexts.add(mutableListOf(textElement))
//            }
//        }
//
//        // Add all columns from this line to the columns list
//        columns.addAll(lineTexts)
//    }
//
//    // Sort columns left to right
//    val sortedColumns = columns.sortedBy { col -> col.first()["x"] as Int }
//
//    // Build the final text
//    val paragraphs = sortedColumns.map { column ->
//        column.sortedByDescending { it["y"] as Int}
//            .joinToString(separator = " ") { it["text"] as String }
//    }
//
//    val ocrText = paragraphs.joinToString(separator = "\n\n")
//    val localTime = convertToLocalTime(timestamp)
//
//    return "Screenshot of $appPackageName at $localTime\n\n$ocrText"
//}