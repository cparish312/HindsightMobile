package com.connor.hindsightmobile.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.connor.hindsightmobile.models.SelectModelDialog
import com.connor.hindsightmobile.ui.elements.ConversationBar
import com.connor.hindsightmobile.ui.elements.Messages
import com.connor.hindsightmobile.ui.theme.HindsightMobileTheme
import com.connor.hindsightmobile.ui.viewmodels.ConversationViewModel
import com.connor.hindsightmobile.ui.elements.UserInput
import com.connor.hindsightmobile.ui.elements.UserInputStatus
import com.connor.hindsightmobile.ui.viewmodels.Message
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.connor.hindsightmobile.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(navController: NavController, viewModel: ConversationViewModel = viewModel()) {
    val context = LocalContext.current
    val ingestionRunning by viewModel.ingestionRunning.collectAsState()

    val wasIngestionRunning = remember { ingestionRunning }
    Log.d("ConversationScreen", "wasIngestionRunning: $wasIngestionRunning")

    DisposableEffect(Unit) {
        context.sendBroadcast(Intent("com.connor.hindsightmobile.CHAT_STARTED"))
        onDispose {
            context.sendBroadcast(Intent("com.connor.hindsightmobile.CHAT_ENDED"))
            CoroutineScope(Dispatchers.IO).launch {
                delay(1000)
                if (wasIngestionRunning && context is MainActivity) {
                    context.ingestScreenshots()
                }
            }
        }
    }

    val messages = viewModel.uiState.messages
    val isGenerating by viewModel.isGenerating.observeAsState()
    val progress by viewModel.modelLoadingProgress.observeAsState(0f)
    val modelInfo by viewModel.loadedModel.observeAsState()
    val models by viewModel.models.observeAsState(emptyList())

    HindsightMobileTheme {

        val scrollState = rememberLazyListState()
        val topBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)
        val scope = rememberCoroutineScope()

        val colorScheme = MaterialTheme.colorScheme
        var modelReport by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                ConversationBar(
                    modelInfo = modelInfo,
                    onSettingsIconPressed = {
                        navController.navigate("mainSettings")
                    },
                    scrollBehavior = scrollBehavior,
                    onSelectModelPressed = {
                        viewModel.loadModelList()
                    },
                    onUnloadModelPressed = {
                        viewModel.unloadModel()
                    }
                )
                if (models.isNotEmpty()) {
                    SelectModelDialog(
                        models = models,
                        onDownloadModel = { model ->
                            viewModel.downloadModel(model)
                        },
                        onLoadModel = { model ->
                            viewModel.loadModel(model)
                        },
                        onDismissRequest = {
                            viewModel.resetModelList()
                        }
                    )
                }
                else if (modelReport != null) {
                    AlertDialog(
                        onDismissRequest = {
                            modelReport = null
                        },
                        title = {
                            Text(text = "Timings")
                        },
                        text = {
                            Text(
                                text = modelReport!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { modelReport = null }) {
                                Text(text = "CLOSE")
                            }
                        }
                    )
                }
            },
            // Exclude ime and navigation bar padding so this can be added by the UserInput composable
            contentWindowInsets = ScaffoldDefaults
                .contentWindowInsets
                .exclude(WindowInsets.navigationBars)
                .exclude(WindowInsets.ime),
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .drawBehind {
                        val strokeWidth = 2.dp.toPx()
                        val x = size.width * progress
                        drawLine(
                            colorScheme.primary,
                            start = Offset(0f, 0f),
                            end = Offset(x, 0f),
                            strokeWidth = strokeWidth
                        )
                    }) {
                Messages(
                    messages = messages,
                    navigateToProfile = { },
                    showAssistantPrompt = { message ->
                        navController.navigateToAssistantPrompt(message)
                    },
                    modifier = Modifier.weight(1f),
                    scrollState = scrollState
                )
                UserInput(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                    status = if (modelInfo == null)
                        UserInputStatus.NOT_LOADED
                    else if (isGenerating == true)
                        UserInputStatus.GENERATING
                    else
                        UserInputStatus.IDLE,
                    onMessageSent = { content ->
                        viewModel.addMessage(
                            Message("User", content)
                        )
                    },
                    onCancelClicked = {
                        viewModel.cancelGeneration()
                    },
                    resetScroll = {
                        scope.launch {
                            scrollState.animateScrollToItem(scrollState.layoutInfo.totalItemsCount- 1)
                        }
                    }
                )
            }
        }
    }
}
