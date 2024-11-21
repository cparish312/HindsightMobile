package com.connor.hindsightmobile.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.connor.hindsightmobile.MainActivity
import com.connor.hindsightmobile.ui.viewmodels.SettingsViewModel
import com.connor.hindsightmobile.utils.getAppDiskUsage
import com.connor.hindsightmobile.utils.observeLastIngestTime
import com.connor.hindsightmobile.utils.observeNumFrames
import dev.jeziellago.compose.markdowntext.MarkdownText


@Composable
fun SettingsScreen(navController: NavController,
                   settingsViewModel: SettingsViewModel = viewModel(),
){
    val context = LocalContext.current

    val numFrames = observeNumFrames(context).collectAsState(initial = 0)
    val diskUsage = getAppDiskUsage(context)
    val lastIngestTime = observeLastIngestTime().collectAsState(initial = "")

    LaunchedEffect(key1 = settingsViewModel) {
        settingsViewModel.events.collect { event ->
            when (event) {
                SettingsViewModel.UIEvent.RequestScreenCapturePermission -> {
                    if (context is MainActivity) {
                        context.requestScreenCapturePermission()
                    }
                }
                SettingsViewModel.UIEvent.StopScreenRecording -> {
                    if (context is MainActivity) {
                        Log.d("MainScreen", "Stopping screen recording")
                        context.stopScreenRecording()
                    }
                }
            }
        }
    }

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
                        |# Hindsight Mobile Settings
                    """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )

                MarkdownText(
                    markdown = """
                        | ## Stats
                        | ### Total Ingested Frames: ${numFrames.value}
                        | ### Disk Usage: $diskUsage
                        | ### Last Ingest: ${lastIngestTime.value}
                    """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)

                )

                MarkdownText(
                    markdown = """
                    |## Control Center
                    |### Ingest Screenshots
                    |* Run a manual ingestion of screenshots.
                    
                """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)

                    )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (context is MainActivity) {
                                context.ingestScreenshots()
                            }
                        },
                        modifier = Modifier.padding(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Ingest")
                    }
                    if (settingsViewModel.isIngesting.collectAsState().value) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                MarkdownText(
                    markdown = """
                    ### Manage Recordings
                    * Navigate to manage recordings, select apps to record, and delete all associated content for an app.
                    
                """.trimIndent(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,

                    )

                Button(
                    onClick = { navController.navigate("manageRecordings") },
                    modifier = Modifier.padding(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("Manage Recordings")
                }

                Spacer(modifier = Modifier.height(16.dp))

                MarkdownText(
                    markdown = """
                    ### Chat
                    * Go to the chat screen.
                    
                """.trimIndent(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,

                    )

                Button(
                    onClick = { navController.navigate("chat") },
                    modifier = Modifier.padding(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("Chat")
                }

                Spacer(modifier = Modifier.height(16.dp))
                MarkdownText(
                    markdown = """
                    |## Settings
                    |### Screen Recording
                    |* Start a screen recording background process. Stop it through the notification bar.
                    
                """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)

                    )

                Switch(
                    checked = settingsViewModel.screenRecordingEnabled.collectAsState().value,
                    onCheckedChange = {
                        settingsViewModel.toggleScreenRecording()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary, // More contrasting color when checked
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // Visible track when checked
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, // Thumb color when unchecked
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.padding(start = 25.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                MarkdownText(
                    markdown = """
                    ### Auto Ingest
                    * Automatically runs ingestion every 15 minutes
                """.trimIndent(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,

                    )

                Switch(
                    checked = settingsViewModel.autoIngestEnabled.collectAsState().value,
                    onCheckedChange = {
                        settingsViewModel.toggleAutoIngest()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary, // More contrasting color when checked
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // Visible track when checked
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, // Thumb color when unchecked
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.padding(start = 25.dp)
                )

                MarkdownText(
                    markdown = """
                    ### Auto Ingest When Not Charging
                    * Run ingestion even when the device is not charging
                """.trimIndent(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,

                    )

                Switch(
                    checked = settingsViewModel.autoIngestWhenNotCharging.collectAsState().value,
                    onCheckedChange = {
                        settingsViewModel.toggleAutoIngestWhenNotCharging()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary, // More contrasting color when checked
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // Visible track when checked
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, // Thumb color when unchecked
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.padding(start = 25.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))

                MarkdownText(
                    markdown = """
                    ### **Record New Apps By Default**
                    * Automatically start recording new apps when they are accessed for the first time.
                    
                """.trimIndent(),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,

                    )

                Switch(
                    checked = settingsViewModel.defaultRecordApps.collectAsState().value,
                    onCheckedChange = {
                        settingsViewModel.toggleDefaultRecordApps()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary, // More contrasting color when checked
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer, // Visible track when checked
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, // Thumb color when unchecked
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.padding(start = 25.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}