package com.benza.smartrooms.feature.roomquizplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuiz
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.repository.RoomRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue

internal data class ScrambleTile(
    val id: String,
    val letter: String,
    val isExtra: Boolean,
)

internal data class RoomQuizPlayerUiState(
    val roomId: String,
    val roomName: String,
    val quizId: String,
    val quiz: RoomQuiz? = null,
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndexes: Map<String, Int> = emptyMap(),
    val fillInAnswers: Map<String, String> = emptyMap(),
    val scrambleAnswerSlots: Map<String, List<String?>> = emptyMap(),
    val correctQuestionIds: Set<String> = emptySet(),
    val lockedQuestionIds: Set<String> = emptySet(),
    val remainingTimeSeconds: Int? = null,
    val isLoadingQuiz: Boolean = true,
    val isQuizCompleted: Boolean = false,
    val errorMessageRes: Int? = null,
) {
    val score: Int
        get() = correctQuestionIds.size
}

internal class RoomQuizPlayerViewModel(
    roomId: String,
    roomName: String,
    quizId: String,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private var questionTimerJob: Job? = null
    private var activeTimedQuestionId: String? = null

    private val _uiState =
        MutableStateFlow(
            RoomQuizPlayerUiState(
                roomId = roomId,
                roomName = roomName,
                quizId = quizId,
            ),
        )
    val uiState: StateFlow<RoomQuizPlayerUiState> = _uiState.asStateFlow()

    init {
        observeQuiz()
    }

    internal fun selectOption(
        questionId: String,
        optionIndex: Int,
    ) {
        if (isQuestionLocked(questionId)) return

        _uiState.update {
            it.copy(
                selectedOptionIndexes = it.selectedOptionIndexes + (questionId to optionIndex),
                errorMessageRes = null,
            )
        }
    }

    internal fun updateFillInAnswer(
        questionId: String,
        value: String,
    ) {
        if (isQuestionLocked(questionId)) return

        _uiState.update {
            it.copy(
                fillInAnswers = it.fillInAnswers + (questionId to value),
                errorMessageRes = null,
            )
        }
    }

    internal fun placeScrambleTile(
        questionId: String,
        tileId: String,
        slotIndex: Int,
    ) {
        if (isQuestionLocked(questionId)) return

        _uiState.update {
            val question =
                it.quiz
                    ?.questions
                    ?.filterIsInstance<RoomQuizQuestion.WordScramble>()
                    ?.firstOrNull { question -> question.id == questionId }
                    ?: return@update it
            val currentSlots =
                it.scrambleAnswerSlots[questionId]
                    ?: List(question.answerWord.length) { null }
            if (slotIndex !in currentSlots.indices) {
                it
            } else {
                val updatedSlots =
                    currentSlots
                        .map { placedTileId -> if (placedTileId == tileId) null else placedTileId }
                        .toMutableList()
                updatedSlots[slotIndex] = tileId
                it.copy(
                    scrambleAnswerSlots = it.scrambleAnswerSlots + (questionId to updatedSlots),
                    errorMessageRes = null,
                )
            }
        }
    }

    internal fun clearScrambleSlot(
        questionId: String,
        slotIndex: Int,
    ) {
        if (isQuestionLocked(questionId)) return

        _uiState.update {
            val question =
                it.quiz
                    ?.questions
                    ?.filterIsInstance<RoomQuizQuestion.WordScramble>()
                    ?.firstOrNull { question -> question.id == questionId }
                    ?: return@update it
            val currentSlots =
                it.scrambleAnswerSlots[questionId]
                    ?: List(question.answerWord.length) { null }
            if (slotIndex !in currentSlots.indices || currentSlots[slotIndex] == null) {
                it
            } else {
                val updatedSlots = currentSlots.toMutableList()
                updatedSlots[slotIndex] = null
                it.copy(
                    scrambleAnswerSlots = it.scrambleAnswerSlots + (questionId to updatedSlots),
                    errorMessageRes = null,
                )
            }
        }
    }

    internal fun goToPreviousQuestion() {
        _uiState.update { state ->
            state.copy(
                currentQuestionIndex = (state.currentQuestionIndex - 1).coerceAtLeast(0),
                lockedQuestionIds = state.lockedQuestionIds.addIfNotNull(state.currentTimedQuestionId()),
            )
        }
        syncTimerWithCurrentQuestion()
    }

    internal fun goToNextQuestion() {
        val maxIndex =
            (
                _uiState.value.quiz
                    ?.questions
                    ?.lastIndex ?: 0
            ).coerceAtLeast(0)
        _uiState.update { state ->
            state.copy(
                currentQuestionIndex = (state.currentQuestionIndex + 1).coerceAtMost(maxIndex),
                lockedQuestionIds = state.lockedQuestionIds.addIfNotNull(state.currentTimedQuestionId()),
            )
        }
        syncTimerWithCurrentQuestion()
    }

    internal fun finishQuiz() {
        val quiz = _uiState.value.quiz ?: return
        val correctQuestionIds =
            quiz.questions
                .filter(::isQuestionAnsweredCorrectly)
                .map(RoomQuizQuestion::id)
                .toSet()

        _uiState.update {
            it.copy(
                isQuizCompleted = true,
                correctQuestionIds = correctQuestionIds,
                remainingTimeSeconds = null,
                errorMessageRes = null,
            )
        }
        clearTimer()
    }

    internal fun retryQuiz() {
        _uiState.update {
            it.copy(
                currentQuestionIndex = 0,
                selectedOptionIndexes = emptyMap(),
                fillInAnswers = emptyMap(),
                scrambleAnswerSlots = emptyMap(),
                correctQuestionIds = emptySet(),
                lockedQuestionIds = emptySet(),
                remainingTimeSeconds = null,
                isQuizCompleted = false,
                errorMessageRes = null,
            )
        }
        syncTimerWithCurrentQuestion()
    }

    private fun observeQuiz() {
        viewModelScope.launch {
            roomRepository
                .observeQuiz(
                    roomId = _uiState.value.roomId,
                    quizId = _uiState.value.quizId,
                ).collect { result ->
                    when (result) {
                        is RoomOperationResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    quiz = result.data,
                                    isLoadingQuiz = false,
                                    errorMessageRes = null,
                                    remainingTimeSeconds =
                                        if (it.remainingTimeSeconds ==
                                            0
                                        ) {
                                            0
                                        } else {
                                            it.remainingTimeSeconds
                                        },
                                    currentQuestionIndex =
                                        it.currentQuestionIndex
                                            .coerceAtMost(
                                                result.data.questions.lastIndex
                                                    .coerceAtLeast(0),
                                            ),
                                )
                            }
                            syncTimerWithCurrentQuestion()
                        }

                        is RoomOperationResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoadingQuiz = false,
                                    remainingTimeSeconds = null,
                                    errorMessageRes = result.messageRes,
                                )
                            }
                            clearTimer()
                        }
                    }
                }
        }
    }

    private fun isQuestionAnsweredCorrectly(question: RoomQuizQuestion): Boolean =
        when (question) {
            is RoomQuizQuestion.MultipleChoice -> {
                _uiState.value.selectedOptionIndexes[question.id] == question.correctOptionIndex
            }

            is RoomQuizQuestion.FillInBlank -> {
                normalizeAnswer(_uiState.value.fillInAnswers[question.id]) == normalizeAnswer(question.answerText)
            }

            is RoomQuizQuestion.WordScramble -> {
                val tilesById = question.buildScrambleTiles().associateBy(ScrambleTile::id)
                normalizeAnswer(
                    _uiState.value.scrambleAnswerSlots[question.id]
                        .orEmpty()
                        .mapNotNull(tilesById::get)
                        .map(ScrambleTile::letter)
                        .joinToString(separator = ""),
                ) == normalizeAnswer(question.answerWord)
            }
        }

    private fun isQuestionLocked(questionId: String): Boolean {
        val state = _uiState.value
        return state.isQuizCompleted || questionId in state.lockedQuestionIds
    }

    private fun syncTimerWithCurrentQuestion() {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion()
        if (state.isQuizCompleted || currentQuestion == null) {
            clearTimer(clearRemainingTime = true)
            return
        }

        val timeLimitSeconds = currentQuestion.timeLimitSeconds
        if (timeLimitSeconds == null) {
            clearTimer(clearRemainingTime = true)
            return
        }

        if (currentQuestion.id in state.lockedQuestionIds) {
            clearTimer()
            _uiState.update { it.copy(remainingTimeSeconds = 0) }
            return
        }

        if (activeTimedQuestionId == currentQuestion.id && questionTimerJob?.isActive == true) {
            return
        }

        clearTimer()
        activeTimedQuestionId = currentQuestion.id
        _uiState.update { it.copy(remainingTimeSeconds = timeLimitSeconds) }
        questionTimerJob =
            viewModelScope.launch {
                var remaining = timeLimitSeconds
                while (remaining > 0) {
                    delay(1000L)
                    remaining -= 1
                    _uiState.update { currentState ->
                        if (
                            currentState.isQuizCompleted ||
                            currentState.currentQuestion()?.id != currentQuestion.id ||
                            currentQuestion.id in currentState.lockedQuestionIds
                        ) {
                            currentState
                        } else {
                            currentState.copy(remainingTimeSeconds = remaining)
                        }
                    }
                }
                handleQuestionTimeout(currentQuestion.id)
            }
    }

    private fun handleQuestionTimeout(questionId: String) {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion()
        if (state.isQuizCompleted || currentQuestion?.id != questionId) return

        val maxIndex = (state.quiz?.questions?.lastIndex ?: 0).coerceAtLeast(0)
        val isLastQuestion = state.currentQuestionIndex >= maxIndex
        _uiState.update {
            it.copy(
                lockedQuestionIds = it.lockedQuestionIds + questionId,
                currentQuestionIndex = if (isLastQuestion) it.currentQuestionIndex else it.currentQuestionIndex + 1,
                remainingTimeSeconds = null,
            )
        }

        if (isLastQuestion) {
            finishQuiz()
        } else {
            syncTimerWithCurrentQuestion()
        }
    }

    private fun clearTimer(clearRemainingTime: Boolean = false) {
        questionTimerJob?.cancel()
        questionTimerJob = null
        activeTimedQuestionId = null
        if (clearRemainingTime) {
            _uiState.update { it.copy(remainingTimeSeconds = null) }
        }
    }

    override fun onCleared() {
        clearTimer()
        super.onCleared()
    }
}

private fun RoomQuizPlayerUiState.currentQuestion(): RoomQuizQuestion? =
    quiz?.questions?.getOrNull(currentQuestionIndex)

private fun RoomQuizPlayerUiState.currentTimedQuestionId(): String? {
    val currentQuestion = currentQuestion() ?: return null
    return if (currentQuestion.timeLimitSeconds != null) currentQuestion.id else null
}

private fun Set<String>.addIfNotNull(value: String?): Set<String> = if (value == null) this else this + value

internal fun RoomQuizQuestion.WordScramble.buildScrambleTiles(): List<ScrambleTile> {
    val baseTiles =
        shuffledLetters.mapIndexed { index, letter ->
            ScrambleTile(
                id = "base-$index",
                letter = letter.matchAnswerWordCase(answerWord),
                isExtra = false,
            )
        }
    val extraTiles =
        answerWord
            .buildExtraScrambleLetters()
            .mapIndexed { index, letter ->
                ScrambleTile(
                    id = "extra-$index",
                    letter = letter,
                    isExtra = true,
                )
            }

    return (baseTiles + extraTiles)
}

private fun String.buildExtraScrambleLetters(): List<String> {
    val normalizedAnswer = lowercase(Locale.ROOT)
    val seed = normalizedAnswer.sumOf(Char::code).absoluteValue
    val answerLetters = normalizedAnswer.map(Char::toString).toSet()
    val extras = mutableListOf<String>()
    val renderUppercase = shouldRenderUppercase()
    var cursor = seed

    while (extras.size < EXTRA_SCRAMBLE_LETTER_COUNT) {
        val candidateLowercase = ('a'.code + (cursor % ALPHABET_SIZE)).toChar().toString()
        if (candidateLowercase !in answerLetters && candidateLowercase !in extras.map(String::lowercase)) {
            val candidate =
                if (renderUppercase) {
                    candidateLowercase.uppercase(Locale.ROOT)
                } else {
                    candidateLowercase
                }
            extras += candidate
        }
        cursor += EXTRA_SCRAMBLE_CURSOR_STEP
    }

    return extras
}

private fun String.matchAnswerWordCase(answerWord: String): String =
    if (answerWord.shouldRenderUppercase()) {
        uppercase(Locale.ROOT)
    } else {
        lowercase(Locale.ROOT)
    }

private fun String.shouldRenderUppercase(): Boolean {
    val letterChars = filter(Char::isLetter)
    return letterChars.isNotEmpty() && letterChars.all(Char::isUpperCase)
}

private fun normalizeAnswer(value: String?): String =
    value
        .orEmpty()
        .trim()
        .lowercase(Locale.ROOT)
        .replace("\\s+".toRegex(), " ")

private const val EXTRA_SCRAMBLE_LETTER_COUNT = 2
private const val ALPHABET_SIZE = 26
private const val EXTRA_SCRAMBLE_CURSOR_STEP = 7
