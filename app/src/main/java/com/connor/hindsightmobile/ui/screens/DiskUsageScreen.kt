package com.connor.hindsightmobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.connor.hindsightmobile.utils.getAppDiskUsage
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun DiskUsageScreen(navController: NavController) {
    val context = LocalContext.current
    val appDiskUsage = getAppDiskUsage(context)

    Surface(color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize())
    {
        LazyColumn(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MarkdownText(
                    markdown = """
                            | ## Disk Usage
                            | ### Total Disk Usage: ${appDiskUsage.totalDiskUsage}
                            | ### Video Disk Usage: ${appDiskUsage.videoDiskUsage}
                            | ### Screenshots Disk Usage: ${appDiskUsage.screenshotDiskUsage}
                        """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("Back")
                }
            }
        }
    }
}