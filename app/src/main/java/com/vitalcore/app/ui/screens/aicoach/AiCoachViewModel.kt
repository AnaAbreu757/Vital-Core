package com.vitalcore.app.ui.screens.aicoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.ai.AiCoachRepository
import com.vitalcore.app.ai.AiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val fromUser: Boolean, val text: String)

data class AiCoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = false,
)

@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val aiCoachRepository: AiCoachRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiCoachUiState())
    val uiState: StateFlow<AiCoachUiState> = _uiState.asStateFlow()

    fun send(question: String) {
        if (question.isBlank()) return
        appendMessage(ChatMessage(fromUser = true, text = question))
        runAsk { aiCoachRepository.ask(question) }
    }

    fun explainRecovery() = quickAction("Explain my Recovery score") { aiCoachRepository.explainRecovery() }
    fun explainSleep() = quickAction("Explain my Sleep score") { aiCoachRepository.explainSleep() }
    fun explainStrain() = quickAction("Explain my Strain score") { aiCoachRepository.explainStrain() }
    fun summarizeDay() = quickAction("Summarize my day") { aiCoachRepository.summarizeDay() }

    private fun quickAction(label: String, call: suspend () -> AiResponse) {
        appendMessage(ChatMessage(fromUser = true, text = label))
        runAsk(call)
    }

    private fun runAsk(call: suspend () -> AiResponse) {
        _uiState.value = _uiState.value.copy(loading = true)
        viewModelScope.launch {
            val response = call()
            val text = when (response) {
                is AiResponse.Success -> response.text
                is AiResponse.Error -> response.message
            }
            appendMessage(ChatMessage(fromUser = false, text = text))
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    private fun appendMessage(message: ChatMessage) {
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + message)
    }
}
