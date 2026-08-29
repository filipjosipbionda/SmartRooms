package com.benza.smartrooms.feature.roomquizplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuiz
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.model.RoomQuizQuestionResult
import com.benza.smartrooms.data.room.model.RoomQuizResult
import com.benza.smartrooms.data.room.model.RoomQuizScoring
import com.benza.smartrooms.data.room.repository.RoomRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val questionResultsById: Map<String, RoomQuizQuestionResult> = emptyMap(),
    val lockedQuestionIds: Set<String> = emptySet(),
    val timedOutQuestionIds: Set<String> = emptySet(),
    val remainingTimeSeconds: Int? = null,
    val isLoadingQuiz: Boolean = true,
    val isSavingResult: Boolean = false,
    val isQuizCompleted: Boolean = false,
    val errorMessageRes: Int? = null,
) {
    val score: Int
        get() = questionResultsById.values.sumOf(RoomQuizQuestionResult::score)

    val maxScore: Int
        get() = quiz?.questions.orEmpty().sumOf(RoomQuizScoring::maxScore)
}

internal sealed interface RoomQuizPlayerEvent {
    data object ExitCompleted : RoomQuizPlayerEvent
}

internal class RoomQuizPlayerViewModel(
    roomId: String,
    roomName: String,
    quizId: String,
    private val roomRepository: RoomRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    private val currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
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

    private val _events =
        MutableSharedFlow<RoomQuizPlayerEvent>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<RoomQuizPlayerEvent> = _events.asSharedFlow()

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

    internal fun submitCurrentAnswer() {
        val state = _uiState.value
        val currentQuestion = state.currentQuestion() ?: return
        val maxIndex = (state.quiz?.questions?.lastIndex ?: 0).coerceAtLeast(0)
        val isLastQuestion = state.currentQuestionIndex >= maxIndex
        val questionResult =
            state.questionResultsById[currentQuestion.id]
                ?: state.buildQuestionResult(currentQuestion)

        _uiState.update {
            it.copy(
                correctQuestionIds =
                    if (questionResult.isCorrect) {
                        it.correctQuestionIds + currentQuestion.id
                    } else {
                        it.correctQuestionIds - currentQuestion.id
                    },
                questionResultsById = it.questionResultsById + (currentQuestion.id to questionResult),
                lockedQuestionIds = it.lockedQuestionIds + currentQuestion.id,
                currentQuestionIndex = if (isLastQuestion) it.currentQuestionIndex else it.currentQuestionIndex + 1,
                remainingTimeSeconds = null,
                errorMessageRes = null,
            )
        }
        clearTimer()

        if (isLastQuestion) {
            finishQuiz()
        } else {
            syncTimerWithCurrentQuestion()
        }
    }

    internal fun finishQuiz() {
        val result = completeCurrentAttempt() ?: return
        saveQuizResult(result, exitAfterSave = false)
    }

    internal fun finishCurrentAttemptAndExit() {
        val state = _uiState.value
        if (state.isSavingResult) return
        if (currentUserId.isBlank()) {
            _events.tryEmit(RoomQuizPlayerEvent.ExitCompleted)
            return
        }

        val result = completeCurrentAttempt() ?: return
        saveQuizResult(result, exitAfterSave = true)
    }

    private fun completeCurrentAttempt(): RoomQuizResult? {
        val state = _uiState.value
        val quiz = state.quiz ?: return null
        val questionResultsById =
            quiz.questions.associate { question ->
                question.id to (state.questionResultsById[question.id] ?: state.buildQuestionResult(question))
            }
        val correctQuestionIds =
            questionResultsById.values
                .filter(RoomQuizQuestionResult::isCorrect)
                .map(RoomQuizQuestionResult::questionId)
                .toSet()
        val questionResults = quiz.questions.mapNotNull { questionResultsById[it.id] }

        _uiState.update {
            it.copy(
                isQuizCompleted = true,
                correctQuestionIds = correctQuestionIds,
                questionResultsById = questionResultsById,
                remainingTimeSeconds = null,
                errorMessageRes = null,
            )
        }
        clearTimer()

        return state.toResult(
            userId = currentUserId,
            questionResults = questionResults,
        )
    }

    private fun saveQuizResult(
        result: RoomQuizResult,
        exitAfterSave: Boolean,
    ) {
        if (currentUserId.isBlank()) return
        _uiState.update { it.copy(isSavingResult = true, errorMessageRes = null) }

        viewModelScope.launch {
            when (val saveResult = roomRepository.saveQuizResult(result)) {
                is RoomOperationResult.Success -> {
                    _uiState.update { it.copy(isSavingResult = false) }
                    if (exitAfterSave) {
                        _events.tryEmit(RoomQuizPlayerEvent.ExitCompleted)
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingResult = false,
                            errorMessageRes = saveResult.messageRes,
                        )
                    }
                }
            }
        }
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
        val currentQuestion = state.currentQuestion() ?: return
        if (state.isQuizCompleted || currentQuestion.id != questionId) return

        _uiState.update {
            it.copy(
                lockedQuestionIds = it.lockedQuestionIds + questionId,
                timedOutQuestionIds = it.timedOutQuestionIds + questionId,
                questionResultsById =
                    it.questionResultsById +
                        (questionId to it.buildQuestionResult(currentQuestion, isTimedOut = true)),
                remainingTimeSeconds = 0,
            )
        }
        clearTimer()
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

private fun RoomQuizPlayerUiState.toResult(
    userId: String,
    questionResults: List<RoomQuizQuestionResult>,
): RoomQuizResult =
    RoomQuizResult(
        roomId = roomId,
        quizId = quizId,
        userId = userId,
        scoringVersion = RoomQuizScoring.VERSION,
        answeredQuestionCount = questionResults.count(RoomQuizQuestionResult::isAnswered),
        questionCount = quiz?.questionCount ?: questionResults.size,
        score = questionResults.sumOf(RoomQuizQuestionResult::score),
        maxScore = questionResults.sumOf(RoomQuizQuestionResult::maxScore),
        questionResults = questionResults,
        completedAtEpochMillis = System.currentTimeMillis(),
    )

private fun RoomQuizPlayerUiState.buildQuestionResult(
    question: RoomQuizQuestion,
    isTimedOut: Boolean = question.id in timedOutQuestionIds,
): RoomQuizQuestionResult {
    val timeLimitSeconds = question.timeLimitSeconds
    val remainingSeconds =
        if (timeLimitSeconds == null) {
            null
        } else if (isTimedOut) {
            0
        } else if (currentQuestion()?.id == question.id) {
            remainingTimeSeconds?.coerceIn(0, timeLimitSeconds)
        } else {
            null
        }
    val isAnswered = isQuestionAnswered(question)
    val isCorrect = isAnswered && isQuestionAnsweredCorrectly(question) && !isTimedOut
    val score =
        RoomQuizScoring.score(
            question = question,
            isCorrect = isCorrect,
            remainingTimeSeconds = remainingSeconds,
            isTimedOut = isTimedOut,
        )

    return RoomQuizQuestionResult(
        questionId = question.id,
        isAnswered = isAnswered,
        isCorrect = isCorrect,
        score = score,
        maxScore = RoomQuizScoring.maxScore(question),
        timeLimitSeconds = timeLimitSeconds,
        remainingTimeSeconds = remainingSeconds,
    )
}

private fun RoomQuizPlayerUiState.isQuestionAnsweredCorrectly(question: RoomQuizQuestion): Boolean =
    when (question) {
        is RoomQuizQuestion.MultipleChoice -> selectedOptionIndexes[question.id] == question.correctOptionIndex
        is RoomQuizQuestion.FillInBlank ->
            normalizeAnswer(fillInAnswers[question.id]) ==
                normalizeAnswer(question.answerText)
        is RoomQuizQuestion.WordScramble -> {
            val tilesById = question.buildScrambleTiles().associateBy(ScrambleTile::id)
            normalizeAnswer(
                scrambleAnswerSlots[question.id]
                    .orEmpty()
                    .mapNotNull(tilesById::get)
                    .map(ScrambleTile::letter)
                    .joinToString(separator = ""),
            ) == normalizeAnswer(question.answerWord)
        }
    }

private fun RoomQuizPlayerUiState.isQuestionAnswered(question: RoomQuizQuestion): Boolean =
    when (question) {
        is RoomQuizQuestion.MultipleChoice -> question.id in selectedOptionIndexes
        is RoomQuizQuestion.FillInBlank -> fillInAnswers[question.id]?.isNotBlank() == true
        is RoomQuizQuestion.WordScramble -> scrambleAnswerSlots[question.id]?.any { it != null } == true
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
