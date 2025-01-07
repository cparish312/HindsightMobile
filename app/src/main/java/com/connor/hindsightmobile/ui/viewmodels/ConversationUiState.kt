package com.connor.hindsightmobile.ui.viewmodels

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.toMutableStateList
import com.connor.hindsightmobile.R

class ConversationUiState(
    initialMessages: List<Message>
) {
    private val _messages: MutableList<Message> = initialMessages.toMutableStateList()
    val messages: List<Message> = _messages

    fun addMessage(msg: Message) {
        _messages.add(msg) // Add to the end of the list
    }

    fun updateLastMessage(msg: String) {
        val message = _messages.last()
        _messages[_messages.size - 1] = message.copy(content = msg)
    }

    fun addPromptToLastMessage(prompt: String) {
        val message = _messages.last()
        _messages[_messages.size - 1] = message.copy(prompt = prompt)
    }
}

@Immutable
data class Message(
    val author: String,
    val content: String,
    val image: Int? = null,
    val prompt: String? = null,
    val authorImage: Int = if (author == "User")
        R.drawable.ic_baseline_person
    else
        R.drawable.hindsight_icon
)
