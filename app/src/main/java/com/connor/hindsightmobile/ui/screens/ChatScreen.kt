package com.connor.hindsightmobile.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.foundation.layout.imePadding


@Composable
fun ChatScreen(navController: NavController) {
    val messages = remember { mutableStateListOf<Message>() }
    val newMessage = remember { mutableStateOf("") }

    Scaffold(
        topBar = { ChatTopBar(title = "Chat", onNavigationClick = { navController.popBackStack() }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // Add this modifier
        ) {

            // Message List
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(messages) { message ->
                    MessageItem(message)
                }
            }

            // Input Field and Send Button
            Row(modifier = Modifier.padding(8.dp)) {
                TextField(
                    value = newMessage.value,
                    onValueChange = { newMessage.value = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") }
                )
                IconButton(onClick = {
                    if (newMessage.value.isNotBlank()) {
                        messages.add(Message(newMessage.value))
                        newMessage.value = ""
                    }
                }) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

data class Message(val content: String)


@Composable
fun MessageItem(message: Message) {
    Card(modifier = Modifier.padding(4.dp)) {
        Text(text = message.content, modifier = Modifier.padding(8.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(title: String, onNavigationClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { /* Handle search action */ }) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { /* Handle more options */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
