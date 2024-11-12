package com.connor.hindsightmobile.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.util.Base64

const val ASSISTANT_PROMPT_SCREEN = "assistantPromptScreen/{message}"

fun NavController.navigateToAssistantPrompt(message: String) {
    val encodedMessage = Base64.encodeToString(message.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
    this.navigate("assistantPromptScreen/$encodedMessage")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = "chat") {
        composable("mainSettings") {
            SettingsScreen(navController)
        }
        composable("chat") {
            ConversationScreen(navController)
        }
        composable("manageRecordings"){
            ManageRecordingsScreen(navController)
        }
        composable(
            route = ASSISTANT_PROMPT_SCREEN,
            arguments = listOf(navArgument("message") { type = NavType.StringType })
        ) { backStackEntry ->
            val message = backStackEntry.arguments?.getString("message") ?: ""
            AssistantPromptScreen(
                message = message,
                navController = navController
            )
        }
    }
}