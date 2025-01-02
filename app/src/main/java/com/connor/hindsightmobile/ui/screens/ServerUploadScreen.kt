package com.connor.hindsightmobile.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.connor.hindsightmobile.utils.Preferences
import dev.jeziellago.compose.markdowntext.MarkdownText


@Composable
fun ServerUploadScreen(navController: NavController,
                   settingsViewModel: SettingsViewModel = viewModel(),
){
    val context = LocalContext.current

    val serverUrl = remember {
        mutableStateOf(
            Preferences.prefs.getString(Preferences.interneturl, "").toString()
        )
    }

    val serverApiKey = remember {
        mutableStateOf(
            Preferences.prefs.getString(Preferences.apikey, "").toString()
        )
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
                        |# Server Upload Settings
                        |* See server setup instructions on github.
                    """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )

                MarkdownText(
                    markdown = """
                        |### Server Upload
                        |* Uploads OCR results to computer server.
                    
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
                                context.uploadToServer()
                            }
                        },
                        modifier = Modifier.padding(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Server Upload")
                    }
                    if (settingsViewModel.isUploading.collectAsState().value) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                MarkdownText(
                    markdown = """
                    |## Server URL
                    
                """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)

                )

                TextField(
                    value = serverUrl.value,
                    onValueChange = {
                        val trimmedInput = it.trimEnd { char -> char == '\n' }
                        serverUrl.value = trimmedInput
                        Preferences.prefs.edit().putString(
                            Preferences.interneturl,
                            trimmedInput
                        ).apply()
                    },
                    label = { Text("Server Url") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                MarkdownText(
                    markdown = """
                    |## Server API Key
                    
                """.trimMargin(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)

                )

                TextField(
                    value = serverApiKey.value,
                    onValueChange = {
                        val trimmedInput = it.trimEnd { char -> char == '\n' }
                        serverApiKey.value = trimmedInput
                        Preferences.prefs.edit().putString(
                            Preferences.apikey,
                            trimmedInput
                        ).apply()
                    },
                    label = { Text("API Key") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
                )

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