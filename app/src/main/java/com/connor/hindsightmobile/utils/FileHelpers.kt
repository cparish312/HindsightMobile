package com.connor.hindsightmobile.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun getImageFiles(directory: File): List<File> {
    return directory.listFiles { _, name -> name.endsWith(".webp") }?.toList() ?: emptyList()
}

fun getUnprocessedScreenshotsDirectory(context: Context): File {
    val directory = File(context.filesDir, "unprocessed_screenshot_images")
    if (!directory.exists()) directory.mkdirs() // Ensure the directory exists
    return directory
}

fun getAccessibilityScreenshotsDirectory(context: Context): File {
    val directory = File(context.filesDir, "accessibility_screenshot_images")
    if (!directory.exists()) directory.mkdirs() // Ensure the directory exists
    return directory
}

fun getVideoFilesDirectory(context: Context): File {
    val directory = File(context.filesDir, "video_files")
    if (!directory.exists()) directory.mkdirs() // Ensure the directory exists
    return directory
}

fun parseScreenshotFilePath(filePath: String): Pair<String?, Long?> {
    val fileName = filePath.substringAfterLast("/")
    val parts = fileName.removeSuffix(".webp").split("_")

    return if (parts.size == 2) {
        val application = parts[0].replace("-", ".")
        val timestamp = parts[1].toLongOrNull()
        Pair(application, timestamp)
    } else {
        Pair(null, null)
    }
}

fun getAssetFile(context: Context, assetName: String): File {
    val file = File(context.filesDir, assetName)
    if (!file.exists()) {
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    return file
}

fun getDirectorySize(directory: File): Long {
    var size: Long = 0
    if (directory.isDirectory) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                size += if (file.isDirectory) getDirectorySize(file) else file.length()
            }
        }
    } else {
        size += directory.length()
    }
    return size
}

fun formatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$size B"
    }
}

data class AppDiskUsage(
    val screenshotDiskUsage: String,
    val videoDiskUsage: String,
    val totalDiskUsage: String
)

fun getAppDiskUsage(context: Context): AppDiskUsage {
    val screenshotsDir = getUnprocessedScreenshotsDirectory(context)
    val screenshotsDiskUsage = formatSize(getDirectorySize(screenshotsDir))

    val videoFilesDir = getVideoFilesDirectory(context)
    val videoDiskUsage = formatSize(getDirectorySize(videoFilesDir))

    val allAppDirs = listOf(
        context.filesDir, // Internal storage files
        context.cacheDir, // Internal storage cache
        context.getExternalFilesDir(null), // External storage files
        context.externalCacheDir // External storage cache
    )

    var totalSize: Long = 0
    for (dir in allAppDirs) {
        if (dir != null && dir.exists()) {
            totalSize += getDirectorySize(dir)
        }
    }

    val formattedTotalSize = formatSize(totalSize)
    return AppDiskUsage(screenshotsDiskUsage, videoDiskUsage, formattedTotalSize)
}
