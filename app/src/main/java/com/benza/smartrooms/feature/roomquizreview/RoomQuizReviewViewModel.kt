package com.benza.smartrooms.feature.roomquizreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuiz
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class RoomQuizReviewUiState(
    val roomId: String,
    val roomName: String,
    val quizId: String,
    val quiz: RoomQuiz? = null,
    val currentQuestionIndex: Int = 0,
    val promptInput: String = "",
    val explanationInput: String = "",
    val optionInputs: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val answerInput: String = "",
    val isLoadingQuiz: Boolean = true,
    val isSavingQuestion: Boolean = false,
    val isDeletingQuestion: Boolean = false,
    val isPublishingQuiz: Boolean = false,
    val infoMessageRes: Int? = null,
    val errorMessageRes: Int? = null,
)

internal class RoomQuizReviewViewModel(
    roomId: String,
    roomName: String,
    quizId: String,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            RoomQuizReviewUiState(
                roomId = roomId,
                roomName = roomName,
                quizId = quizId,
            ),
        )
    val uiState: StateFlow<RoomQuizReviewUiState> = _uiState.asStateFlow()

    init {
        observeQuiz()
    }

    internal fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessageRes = null) }
    }

    internal fun goToPreviousQuestion() {
        val updatedIndex = (_uiState.value.currentQuestionIndex - 1).coerceAtLeast(0)
        _uiState.update { state ->
            state.copy(currentQuestionIndex = updatedIndex).withQuestionInputsFromQuiz()
        }
    }

    internal fun goToNextQuestion() {
        val lastIndex =
            (
                _uiState.value.quiz
                    ?.questions
                    ?.lastIndex ?: 0
            ).coerceAtLeast(0)
        val updatedIndex = (_uiState.value.currentQuestionIndex + 1).coerceAtMost(lastIndex)
        _uiState.update { state ->
            state.copy(currentQuestionIndex = updatedIndex).withQuestionInputsFromQuiz()
        }
    }

    internal fun onPromptChanged(value: String) {
        _uiState.update { it.copy(promptInput = value, errorMessageRes = null) }
    }

    internal fun onExplanationChanged(value: String) {
        _uiState.update { it.copy(explanationInput = value, errorMessageRes = null) }
    }

    internal fun onOptionChanged(
        index: Int,
        value: String,
    ) {
        _uiState.update { state ->
            state.copy(
                optionInputs = state.optionInputs.toMutableList().apply { set(index, value) },
                errorMessageRes = null,
            )
        }
    }

    internal fun onCorrectOptionSelected(index: Int) {
        _uiState.update { it.copy(correctOptionIndex = index, errorMessageRes = null) }
    }

    internal fun onAnswerChanged(value: String) {
        _uiState.update { it.copy(answerInput = value, errorMessageRes = null) }
    }

    internal fun saveQuestion() {
        val question =
            currentEditedQuestion() ?: run {
                _uiState.update { it.copy(errorMessageRes = R.string.error_quiz_review_invalid_question) }
                return
            }

        _uiState.update { it.copy(isSavingQuestion = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (
                val result =
                    roomRepository.updateQuizQuestion(
                        roomId = _uiState.value.roomId,
                        quizId = _uiState.value.quizId,
                        question = question,
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSavingQuestion = false,
                            infoMessageRes = R.string.room_quiz_review_saved,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update { it.copy(isSavingQuestion = false, errorMessageRes = result.messageRes) }
                }
            }
        }
    }

    internal fun deleteQuestion() {
        val questionId = currentQuestion()?.id ?: return
        _uiState.update { it.copy(isDeletingQuestion = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (
                val result =
                    roomRepository.deleteQuizQuestion(
                        roomId = _uiState.value.roomId,
                        quizId = _uiState.value.quizId,
                        questionId = questionId,
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        val nextIndex = if ((it.quiz?.questionCount ?: 0) <= 1) 0 else it.currentQuestionIndex
                        it.copy(
                            currentQuestionIndex = nextIndex,
                            isDeletingQuestion = false,
                            infoMessageRes = R.string.room_quiz_review_deleted,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update { it.copy(isDeletingQuestion = false, errorMessageRes = result.messageRes) }
                }
            }
        }
    }

    internal fun publishQuiz() {
        val quiz = _uiState.value.quiz ?: return
        if (quiz.questions.isEmpty()) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_quiz_review_empty_quiz) }
            return
        }

        _uiState.update { it.copy(isPublishingQuiz = true, errorMessageRes = null) }
        viewModelScope.launch {
            when (val result = roomRepository.publishQuiz(_uiState.value.roomId, _uiState.value.quizId)) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isPublishingQuiz = false,
                            infoMessageRes = R.string.room_quiz_review_published,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update { it.copy(isPublishingQuiz = false, errorMessageRes = result.messageRes) }
                }
            }
        }
    }

    private fun observeQuiz() {
        viewModelScope.launch {
            roomRepository.observeQuiz(_uiState.value.roomId, _uiState.value.quizId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        _uiState.update { state ->
                            state
                                .copy(
                                    quiz = result.data,
                                    isLoadingQuiz = false,
                                    errorMessageRes = null,
                                    currentQuestionIndex =
                                        state.currentQuestionIndex
                                            .coerceAtMost(
                                                result.data.questions.lastIndex
                                                    .coerceAtLeast(0),
                                            ),
                                ).withQuestionInputsFromQuiz()
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update { it.copy(isLoadingQuiz = false, errorMessageRes = result.messageRes) }
                    }
                }
            }
        }
    }

    private fun currentEditedQuestion(): RoomQuizQuestion? {
        val currentQuestion = currentQuestion() ?: return null
        val prompt = _uiState.value.promptInput.trim()
        val explanation = _uiState.value.explanationInput.trim()
        if (prompt.isBlank() || explanation.isBlank()) return null

        return when (currentQuestion) {
            is RoomQuizQuestion.MultipleChoice -> {
                val options = _uiState.value.optionInputs.map(String::trim)
                if (options.any(String::isBlank)) return null
                RoomQuizQuestion.MultipleChoice(
                    id = currentQuestion.id,
                    prompt = prompt,
                    explanation = explanation,
                    timeLimitSeconds = currentQuestion.timeLimitSeconds,
                    options = options,
                    correctOptionIndex = _uiState.value.correctOptionIndex,
                )
            }

            is RoomQuizQuestion.FillInBlank -> {
                val answer = _uiState.value.answerInput.trim()
                if (answer.isBlank()) return null
                RoomQuizQuestion.FillInBlank(
                    id = currentQuestion.id,
                    prompt = prompt,
                    explanation = explanation,
                    timeLimitSeconds = currentQuestion.timeLimitSeconds,
                    answerText = answer,
                )
            }

            is RoomQuizQuestion.WordScramble -> {
                val answer = _uiState.value.answerInput.trim()
                if (answer.isBlank()) return null
                RoomQuizQuestion.WordScramble(
                    id = currentQuestion.id,
                    prompt = prompt,
                    explanation = explanation,
                    timeLimitSeconds = currentQuestion.timeLimitSeconds,
                    answerWord = answer,
                    shuffledLetters = answer.toShuffledLetters(),
                )
            }
        }
    }

    private fun currentQuestion(): RoomQuizQuestion? =
        _uiState.value.quiz
            ?.questions
            ?.getOrNull(_uiState.value.currentQuestionIndex)
}

private fun RoomQuizReviewUiState.withQuestionInputsFromQuiz(): RoomQuizReviewUiState {
    val question = quiz?.questions?.getOrNull(currentQuestionIndex) ?: return this
    return when (question) {
        is RoomQuizQuestion.MultipleChoice ->
            copy(
                promptInput = question.prompt,
                explanationInput = question.explanation,
                optionInputs = question.options.padToFour(),
                correctOptionIndex = question.correctOptionIndex,
                answerInput = "",
            )

        is RoomQuizQuestion.FillInBlank ->
            copy(
                promptInput = question.prompt,
                explanationInput = question.explanation,
                optionInputs = listOf("", "", "", ""),
                correctOptionIndex = 0,
                answerInput = question.answerText,
            )

        is RoomQuizQuestion.WordScramble ->
            copy(
                promptInput = question.prompt,
                explanationInput = question.explanation,
                optionInputs = listOf("", "", "", ""),
                correctOptionIndex = 0,
                answerInput = question.answerWord,
            )
    }
}

private fun List<String>.padToFour(): List<String> = if (size >= 4) take(4) else this + List(4 - size) { "" }

private fun String.toShuffledLetters(): List<String> {
    val letters = trim().map { it.toString() }
    return if (letters.size <= 1) letters else letters.drop(1) + letters.take(1)
}
