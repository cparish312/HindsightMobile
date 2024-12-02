package com.connor.hindsightmobile.utils

import android.content.Context
import android.util.Log
import com.connor.hindsightmobile.DB
import com.connor.hindsightmobile.obj.ObjectBoxFrame
import com.connor.hindsightmobile.obj.ObjectBoxFrame_
import com.connor.hindsightmobile.obj.ObjectBoxStore
import com.connor.hindsightmobile.ui.elements.AppInfo

fun deleteAppData(app: AppInfo, context: Context, dbHelper: DB) {
    Log.d("DeleteAppData", "Deleting data for ${app.appName}")
    // Delete all DB data for app
    dbHelper.deleteAllDBDataForApp(app.packageName)

    // Delete all Videos and Screenshots for app
    val applicationDashes = app.packageName.replace(".", "-")
    val videoFilesDirectory = getVideoFilesDirectory(context)
    videoFilesDirectory.listFiles()?.forEach { file ->
        if (file.name.startsWith(applicationDashes)) {
            if (file.delete()) {
                Log.d("DeleteAppData", "Deleted video file: ${file.name}")
            } else {
                Log.e("DeleteAppData", "Failed to delete video file: ${file.name}")
            }
        }
    }

    val unprocessedScreenshotsDirectory = getUnprocessedScreenshotsDirectory(context)
    unprocessedScreenshotsDirectory.listFiles()?.forEach { file ->
        if (file.name.startsWith(applicationDashes)) {
            if (file.delete()) {
                Log.d("DeleteAppData", "Deleted screenshot file: ${file.name}")
            } else {
                Log.e("DeleteAppData", "Failed to delete screenshot file: ${file.name}")
            }
        }
    }

    // Delete Embeddings
    val framesBox = ObjectBoxStore.store.boxFor(ObjectBoxFrame::class.java)
    val query = framesBox.query(ObjectBoxFrame_.application.equal(app.packageName)).build()

    val objectsToDelete = query.find()
    Log.d("DeleteAppData", "Embeddings to delete: ${objectsToDelete.size}")
    framesBox.remove(objectsToDelete)

    query.close()

}

fun deleteRecentScreenshotsData(millisecondsToDelete: Long, context: Context, dbHelper: DB) {
    val cutoffTimestamp = System.currentTimeMillis() - millisecondsToDelete

    dbHelper.deleteDBDataSince(cutoffTimestamp)

    val unprocessedScreenshotsDirectory = getUnprocessedScreenshotsDirectory(context)
    unprocessedScreenshotsDirectory.listFiles()?.forEach { file ->
        val (_, fileTimestamp) = parseScreenshotFilePath(file.name)
        if (fileTimestamp != null && fileTimestamp >= cutoffTimestamp) {
            if (file.delete()) {
                Log.d("DeleteRecentScreenshots", "Deleted screenshot file: ${file.name}")
            } else {
                Log.e("DeleteRecentScreenshots", "Failed to delete screenshot file: ${file.name}")
            }
        }
    }
}