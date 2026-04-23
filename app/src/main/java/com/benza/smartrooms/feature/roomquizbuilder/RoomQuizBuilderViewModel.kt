package com.benza.smartrooms.feature.roomquizbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class RoomQuizBuilderUiState(
    val roomId: String,
    val roomName: String,
    val roomTopic: String,
    val cefrLevel: String = DEFAULT_CEFR_LEVEL,
    val selectedQuestionCount: Int = DEFAULT_QUESTION_COUNT,
    val selectedQuestionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val quizzes: List<RoomQuizSummary> = emptyList(),
    val isLoadingQuizzes: Boolean = true,
    val isGeneratingQuiz: Boolean = false,
    val errorMessageRes: Int? = null,
    val infoMessageRes: Int? = null
)

internal class RoomQuizBuilderViewModel(
    roomId: String,
    roomName: String,
    roomTopic: String,
    private val roomRepository: RoomRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        RoomQuizBuilderUiState(
            roomId = roomId,
            roomName = roomName,
            roomTopic = roomTopic
        )
    )
    internal val uiState: StateFlow<RoomQuizBuilderUiState> = _uiState.asStateFlow()

    init {
        observeQuizzes()
    }

    internal fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessageRes = null) }
    }

    internal fun onQuestionCountSelected(value: Int) {
        _uiState.update {
            it.copy(
                selectedQuestionCount = value,
                errorMessageRes = null,
                infoMessageRes = null
            )
        }
    }

    internal fun onQuestionTypeSelected(value: QuestionType) {
        _uiState.update {
            it.copy(
                selectedQuestionType = value,
                errorMessageRes = null,
                infoMessageRes = null
            )
        }
    }

    internal fun generateQuiz() {
        if (_uiState.value.isGeneratingQuiz) return

        _uiState.update {
            it.copy(
                isGeneratingQuiz = true,
                errorMessageRes = null,
                infoMessageRes = null
            )
        }

        viewModelScope.launch {
            when (
                val result = roomRepository.generateQuiz(
                    GenerateQuizRequest(
                        roomId = _uiState.value.roomId,
                        cefrLevel = _uiState.value.cefrLevel,
                        questionCount = _uiState.value.selectedQuestionCount,
                        questionType = _uiState.value.selectedQuestionType
                    )
                )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                            infoMessageRes = R.string.room_detail_quiz_generated
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                            errorMessageRes = result.messageRes
                        )
                    }
                }
            }
        }
    }

    private fun observeQuizzes() {
        viewModelScope.launch {
            roomRepository.observeQuizzes(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                quizzes = result.data,
                                isLoadingQuizzes = false,
                                errorMessageRes = null
                            )
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingQuizzes = false,
                                errorMessageRes = result.messageRes
                            )
                        }
                    }
                }
            }
        }
    }
}

internal val questionCountOptions = listOf(5, 10, 15)

private const val DEFAULT_CEFR_LEVEL = "B1"
private const val DEFAULT_QUESTION_COUNT = 5
