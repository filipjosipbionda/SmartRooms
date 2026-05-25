package com.benza.smartrooms.feature.roomquizbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.GenerateQuizRequest
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.QuizKind
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.data.room.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

internal data class RoomQuizBuilderUiState(
    val roomId: String,
    val roomName: String,
    val roomTopic: String,
    val selectedQuizKind: QuizKind = QuizKind.GRAMMAR,
    val titleInput: String = "",
    val topicInput: String,
    val vocabularyWordsInput: String = "",
    val cefrLevel: String = "",
    val questionCountInput: String = DEFAULT_QUESTION_COUNT.toString(),
    val selectedQuestionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val hasQuestionTimeLimit: Boolean = false,
    val questionTimeLimitInput: String = "",
    val pendingQuizSummary: RoomQuizSummary? = null,
    val retryingQuizId: String? = null,
    val selectedQuizIds: Set<String> = emptySet(),
    val pendingQuizDeletion: PendingQuizDeletion? = null,
    val quizzes: List<RoomQuizSummary> = emptyList(),
    val isLoadingQuizzes: Boolean = true,
    val isGeneratingQuiz: Boolean = false,
    val isDeletingQuiz: Boolean = false,
    val titleErrorRes: Int? = null,
    val topicErrorRes: Int? = null,
    val vocabularyWordsErrorRes: Int? = null,
    val questionCountErrorRes: Int? = null,
    val questionTimeLimitErrorRes: Int? = null,
    val errorMessageRes: Int? = null,
    val errorMessageText: String? = null,
    val infoMessageRes: Int? = null,
) {
    val isSelectionMode: Boolean
        get() = selectedQuizIds.isNotEmpty()
}

internal data class PendingQuizDeletion(
    val quizIds: Set<String>,
    val mode: QuizDeletionMode,
    val quizTitle: String? = null,
)

internal enum class QuizDeletionMode {
    SINGLE,
    SELECTED,
    ALL,
}

internal class RoomQuizBuilderViewModel(
    roomId: String,
    roomName: String,
    roomTopic: String,
    private val roomRepository: RoomRepository,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            RoomQuizBuilderUiState(
                roomId = roomId,
                roomName = roomName,
                roomTopic = roomTopic,
                topicInput = roomTopic,
            ),
        )
    val uiState: StateFlow<RoomQuizBuilderUiState> = _uiState.asStateFlow()

    init {
        observeRoom()
        observeQuizzes()
    }

    internal fun consumeInfoMessage() {
        _uiState.update { it.copy(infoMessageRes = null) }
    }

    internal fun requestDeleteQuiz(quizId: String) {
        val quiz = _uiState.value.quizzes.firstOrNull { it.id == quizId } ?: return
        if (!quiz.canBeDeleted()) return
        _uiState.update {
            it.copy(
                pendingQuizDeletion =
                    PendingQuizDeletion(
                        quizIds = setOf(quiz.id),
                        mode = QuizDeletionMode.SINGLE,
                        quizTitle = quiz.title,
                    ),
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun requestDeleteSelectedQuizzes() {
        val selectedQuizIds =
            _uiState.value.selectedQuizIds.intersect(
                _uiState.value.quizzes
                    .filter(RoomQuizSummary::canBeDeleted)
                    .map(RoomQuizSummary::id)
                    .toSet(),
            )
        if (selectedQuizIds.isEmpty()) return
        _uiState.update {
            it.copy(
                pendingQuizDeletion =
                    PendingQuizDeletion(
                        quizIds = selectedQuizIds,
                        mode = QuizDeletionMode.SELECTED,
                    ),
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun requestDeleteAllQuizzes() {
        val quizIds =
            _uiState.value.quizzes
                .filter(RoomQuizSummary::canBeDeleted)
                .map(RoomQuizSummary::id)
                .toSet()
        if (quizIds.isEmpty()) return
        _uiState.update {
            it.copy(
                pendingQuizDeletion =
                    PendingQuizDeletion(
                        quizIds = quizIds,
                        mode = QuizDeletionMode.ALL,
                    ),
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun dismissDeleteQuizDialog() {
        _uiState.update {
            it.copy(
                pendingQuizDeletion = null,
                isDeletingQuiz = false,
            )
        }
    }

    internal fun onQuizLongPress(quizId: String) {
        if (_uiState.value.isDeletingQuiz) return
        updateSelectedQuizzes(quizId, allowStartSelection = true)
    }

    internal fun toggleQuizSelection(quizId: String) {
        if (_uiState.value.isDeletingQuiz) return
        updateSelectedQuizzes(quizId, allowStartSelection = false)
    }

    internal fun clearQuizSelection() {
        _uiState.update {
            it.copy(selectedQuizIds = emptySet())
        }
    }

    internal fun selectAllQuizzes() {
        _uiState.update { state ->
            val deletableQuizIds =
                state.quizzes
                    .filter(RoomQuizSummary::canBeDeleted)
                    .map(RoomQuizSummary::id)
                    .toSet()
            if (deletableQuizIds.isEmpty() || state.selectedQuizIds == deletableQuizIds) {
                return@update state
            }

            state.copy(
                selectedQuizIds = deletableQuizIds,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun onTitleChanged(value: String) {
        _uiState.update {
            it.copy(
                titleInput = value,
                titleErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun onQuestionCountInputChanged(value: String) {
        if (_uiState.value.selectedQuizKind == QuizKind.VOCABULARY) return
        _uiState.update {
            it.copy(
                questionCountInput = value.filter(Char::isDigit),
                questionCountErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun onQuestionTypeSelected(value: QuestionType) {
        if (_uiState.value.selectedQuizKind == QuizKind.VOCABULARY || value == QuestionType.WORD_SCRAMBLE) return
        _uiState.update {
            it.copy(
                selectedQuestionType = value,
                topicErrorRes = null,
                vocabularyWordsErrorRes = null,
                questionCountErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun onQuestionTimeLimitEnabledChanged(value: Boolean) {
        _uiState.update {
            it.copy(
                hasQuestionTimeLimit = value,
                questionTimeLimitErrorRes = null,
                topicErrorRes = null,
                vocabularyWordsErrorRes = null,
                questionCountErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun onQuestionTimeLimitInputChanged(value: String) {
        _uiState.update {
            it.copy(
                questionTimeLimitInput = value.filter(Char::isDigit),
                questionTimeLimitErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun onTopicChanged(value: String) {
        _uiState.update {
            it.copy(
                topicInput = value,
                topicErrorRes = null,
                vocabularyWordsErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun onVocabularyWordsChanged(value: String) {
        _uiState.update {
            it.copy(
                vocabularyWordsInput = value,
                vocabularyWordsErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun onQuizKindSelected(value: QuizKind) {
        _uiState.update {
            it.copy(
                selectedQuizKind = value,
                topicErrorRes = null,
                vocabularyWordsErrorRes = null,
                questionCountErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = null,
            )
        }
    }

    internal fun generateQuiz() {
        if (_uiState.value.isGeneratingQuiz) return
        val state = _uiState.value
        val title = state.titleInput.trim()
        val topic = state.topicInput.trim()
        val vocabularyWords = state.vocabularyWordsInput.toVocabularyWords()
        val cefrLevel = state.cefrLevel.trim()
        val questionCount = state.questionCountInput.toQuestionCountOrNull()
        val questionTimeLimitSeconds = state.questionTimeLimitInput.toTimeLimitSecondsOrNull()
        val clientRequestId = UUID.randomUUID().toString()

        if (cefrLevel.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessageRes = R.string.error_quiz_room_cefr_missing,
                    errorMessageText = null,
                )
            }
            return
        }
        if (title.isBlank()) {
            _uiState.update {
                it.copy(
                    titleErrorRes = R.string.error_quiz_title_required,
                    errorMessageRes = R.string.error_quiz_generation_fix_fields,
                    errorMessageText = null,
                )
            }
            return
        }

        if (state.hasQuestionTimeLimit) {
            if (state.questionTimeLimitInput.isBlank()) {
                _uiState.update {
                    it.copy(
                        questionTimeLimitErrorRes = R.string.error_quiz_time_limit_required,
                        errorMessageRes = R.string.error_quiz_generation_fix_fields,
                        errorMessageText = null,
                    )
                }
                return
            }
            if (questionTimeLimitSeconds == null) {
                _uiState.update {
                    it.copy(
                        questionTimeLimitErrorRes = R.string.error_quiz_time_limit_invalid,
                        errorMessageRes = R.string.error_quiz_generation_fix_fields,
                        errorMessageText = null,
                    )
                }
                return
            }
        }

        when (state.selectedQuizKind) {
            QuizKind.GRAMMAR -> {
                if (topic.isBlank()) {
                    _uiState.update {
                        it.copy(
                            topicErrorRes = R.string.error_quiz_topic_required,
                            errorMessageRes = R.string.error_quiz_generation_fix_fields,
                            errorMessageText = null,
                        )
                    }
                    return
                }
                if (state.questionCountInput.isBlank()) {
                    _uiState.update {
                        it.copy(
                            questionCountErrorRes = R.string.error_quiz_question_count_required,
                            errorMessageRes = R.string.error_quiz_generation_fix_fields,
                            errorMessageText = null,
                        )
                    }
                    return
                }
                if (questionCount == null) {
                    _uiState.update {
                        it.copy(
                            questionCountErrorRes = R.string.error_quiz_question_count_invalid,
                            errorMessageRes = R.string.error_quiz_generation_fix_fields,
                            errorMessageText = null,
                        )
                    }
                    return
                }
            }

            QuizKind.VOCABULARY -> {
                if (vocabularyWords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            vocabularyWordsErrorRes = R.string.error_vocabulary_words_required,
                            errorMessageRes = R.string.error_quiz_generation_fix_fields,
                            errorMessageText = null,
                        )
                    }
                    return
                }
                if (vocabularyWords.size > MAX_AI_QUESTION_COUNT) {
                    _uiState.update {
                        it.copy(
                            vocabularyWordsErrorRes = R.string.error_vocabulary_words_limit,
                            errorMessageRes = R.string.error_quiz_generation_fix_fields,
                            errorMessageText = null,
                        )
                    }
                    return
                }
            }
        }

        val pendingQuizSummary =
            buildPendingQuizSummary(
                clientRequestId = clientRequestId,
                title = title,
                quizKind = state.selectedQuizKind,
                topic = topic,
                vocabularyWords = vocabularyWords,
                cefrLevel = cefrLevel,
                questionCount =
                    if (state.selectedQuizKind == QuizKind.VOCABULARY) {
                        vocabularyWords.size
                    } else {
                        questionCount ?: DEFAULT_QUESTION_COUNT
                    },
                questionType =
                    if (state.selectedQuizKind == QuizKind.VOCABULARY) {
                        QuestionType.WORD_SCRAMBLE
                    } else {
                        state.selectedQuestionType
                    },
                hasTimer = state.hasQuestionTimeLimit,
            )

        _uiState.update {
            it.copy(
                isGeneratingQuiz = true,
                titleErrorRes = null,
                topicErrorRes = null,
                vocabularyWordsErrorRes = null,
                questionCountErrorRes = null,
                questionTimeLimitErrorRes = null,
                pendingQuizSummary = pendingQuizSummary,
                quizzes =
                    listOf(pendingQuizSummary) +
                        it.quizzes.filterNot { quiz ->
                            quiz.id == pendingQuizSummary.id
                        },
                errorMessageRes = null,
                errorMessageText = null,
                infoMessageRes = R.string.room_quiz_generation_started,
            )
        }

        viewModelScope.launch {
            when (
                val result =
                    roomRepository.generateQuiz(
                        GenerateQuizRequest(
                            clientRequestId = clientRequestId,
                            roomId = state.roomId,
                            title = title,
                            quizKind = state.selectedQuizKind,
                            topic = if (state.selectedQuizKind == QuizKind.GRAMMAR) topic else "",
                            vocabularyWords = vocabularyWords,
                            cefrLevel = cefrLevel,
                            questionCount =
                                if (state.selectedQuizKind == QuizKind.VOCABULARY) {
                                    vocabularyWords.size
                                } else {
                                    questionCount ?: DEFAULT_QUESTION_COUNT
                                },
                            questionType =
                                if (state.selectedQuizKind == QuizKind.VOCABULARY) {
                                    QuestionType.WORD_SCRAMBLE
                                } else {
                                    state.selectedQuestionType
                                },
                            questionTimeLimitSeconds =
                                if (state.hasQuestionTimeLimit) {
                                    questionTimeLimitSeconds
                                } else {
                                    null
                                },
                        ),
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGeneratingQuiz = false,
                            pendingQuizSummary = null,
                            errorMessageRes = result.messageRes,
                            errorMessageText = result.debugMessage,
                        )
                    }
                }
            }
        }
    }

    internal fun retryQuiz(quizId: String) {
        if (_uiState.value.isGeneratingQuiz || _uiState.value.retryingQuizId != null) return

        _uiState.update {
            it.copy(
                retryingQuizId = quizId,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }

        viewModelScope.launch {
            when (val result = roomRepository.retryQuizGeneration(_uiState.value.roomId, quizId)) {
                is RoomOperationResult.Success -> {
                    _uiState.update { it.copy(retryingQuizId = null) }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            retryingQuizId = null,
                            errorMessageRes = result.messageRes,
                            errorMessageText = result.debugMessage,
                        )
                    }
                }
            }
        }
    }

    internal fun confirmDeleteQuiz() {
        val pendingDeletion = _uiState.value.pendingQuizDeletion ?: return
        if (_uiState.value.isDeletingQuiz) return

        _uiState.update {
            it.copy(
                isDeletingQuiz = true,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }

        viewModelScope.launch {
            when (val result = roomRepository.deleteQuizzes(_uiState.value.roomId, pendingDeletion.quizIds)) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeletingQuiz = false,
                            pendingQuizDeletion = null,
                            selectedQuizIds = it.selectedQuizIds - pendingDeletion.quizIds,
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isDeletingQuiz = false,
                            errorMessageRes = result.messageRes,
                            errorMessageText = result.debugMessage,
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
                            val pendingQuizSummary = it.pendingQuizSummary
                            val shouldClearPending =
                                pendingQuizSummary != null &&
                                    result.data.containsServerVersionOf(pendingQuizSummary)
                            val retryingQuizId = it.retryingQuizId
                            val shouldClearRetrying =
                                retryingQuizId != null &&
                                    result.data.any { quiz ->
                                        quiz.id == retryingQuizId && quiz.status != RoomQuizStatus.FAILED
                                    }
                            val quizzes =
                                if (shouldClearPending) {
                                    result.data
                                } else {
                                    pendingQuizSummary?.let { pending -> listOf(pending) + result.data } ?: result.data
                                }
                            val availableQuizIds = quizzes.map(RoomQuizSummary::id).toSet()
                            val pendingDeletion =
                                it.pendingQuizDeletion?.let { deletion ->
                                    deletion.quizIds
                                        .intersect(availableQuizIds)
                                        .takeIf { remainingIds -> remainingIds.isNotEmpty() }
                                        ?.let { remainingIds -> deletion.copy(quizIds = remainingIds) }
                                }
                            it.copy(
                                quizzes = quizzes,
                                retryingQuizId = if (shouldClearRetrying) null else retryingQuizId,
                                pendingQuizSummary = if (shouldClearPending) null else pendingQuizSummary,
                                selectedQuizIds = it.selectedQuizIds.intersect(availableQuizIds),
                                pendingQuizDeletion = pendingDeletion,
                                isLoadingQuizzes = false,
                                errorMessageRes = null,
                                errorMessageText = null,
                            )
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingQuizzes = false,
                                errorMessageRes = result.messageRes,
                                errorMessageText = result.debugMessage,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeRoom() {
        viewModelScope.launch {
            roomRepository.observeRoom(_uiState.value.roomId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        _uiState.update {
                            it.copy(
                                roomName = result.data.name,
                                roomTopic = result.data.topic,
                                cefrLevel = result.data.cefrLevel,
                            )
                        }
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                errorMessageRes = result.messageRes,
                                errorMessageText = result.debugMessage,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateSelectedQuizzes(
        quizId: String,
        allowStartSelection: Boolean,
    ) {
        _uiState.update { state ->
            val targetQuiz = state.quizzes.firstOrNull { it.id == quizId } ?: return@update state
            if (!targetQuiz.canBeDeleted()) {
                return@update state
            }
            if (!state.isSelectionMode && !allowStartSelection) {
                return@update state
            }

            val selectedQuizIds = state.selectedQuizIds.toMutableSet()
            if (!selectedQuizIds.add(quizId)) {
                selectedQuizIds.remove(quizId)
            }

            state.copy(
                selectedQuizIds = selectedQuizIds,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }
}

private fun RoomQuizSummary.canBeDeleted(): Boolean = status != RoomQuizStatus.GENERATING

private const val DEFAULT_QUESTION_COUNT = 5
private const val MIN_QUESTION_COUNT = 1
private const val MAX_AI_QUESTION_COUNT = 50
private const val MAX_QUESTION_TIME_LIMIT_SECONDS = 3600

private fun String.toVocabularyWords(): List<String> =
    split(",", "\n", ";")
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()

private fun String.toTimeLimitSecondsOrNull(): Int? {
    val value = trim().toIntOrNull() ?: return null
    return value.takeIf { it in 1..MAX_QUESTION_TIME_LIMIT_SECONDS }
}

private fun String.toQuestionCountOrNull(): Int? {
    val value = trim().toIntOrNull() ?: return null
    return value.takeIf { it in MIN_QUESTION_COUNT..MAX_AI_QUESTION_COUNT }
}

private fun buildPendingQuizSummary(
    clientRequestId: String,
    title: String,
    quizKind: QuizKind,
    topic: String,
    vocabularyWords: List<String>,
    cefrLevel: String,
    questionCount: Int,
    questionType: QuestionType,
    hasTimer: Boolean,
): RoomQuizSummary {
    val now = System.currentTimeMillis()
    return RoomQuizSummary(
        id = "pending-$now",
        clientRequestId = clientRequestId,
        title = title,
        quizKind = quizKind,
        topic =
            if (quizKind == QuizKind.GRAMMAR) {
                topic
            } else {
                buildVocabularyTopic(vocabularyWords)
            },
        vocabularyWords = vocabularyWords,
        cefrLevel = cefrLevel,
        questionType = questionType,
        questionCount = questionCount,
        hasTimer = hasTimer,
        status = RoomQuizStatus.GENERATING,
        createdAtEpochMillis = now,
    )
}

private fun List<RoomQuizSummary>.containsServerVersionOf(pendingQuizSummary: RoomQuizSummary): Boolean {
    if (pendingQuizSummary.clientRequestId.isNotBlank()) {
        return any { quiz -> quiz.clientRequestId == pendingQuizSummary.clientRequestId }
    }

    return any { quiz ->
        quiz.status in
            setOf(
                RoomQuizStatus.GENERATING,
                RoomQuizStatus.REVIEW,
                RoomQuizStatus.READY,
                RoomQuizStatus.FAILED,
            ) &&
            quiz.quizKind == pendingQuizSummary.quizKind &&
            quiz.questionType == pendingQuizSummary.questionType &&
            quiz.questionCount == pendingQuizSummary.questionCount &&
            quiz.cefrLevel == pendingQuizSummary.cefrLevel &&
            quiz.topic == pendingQuizSummary.topic &&
            quiz.createdAtEpochMillis >= pendingQuizSummary.createdAtEpochMillis - PENDING_QUIZ_CLOCK_SKEW_MILLIS
    }
}

private fun buildVocabularyTopic(vocabularyWords: List<String>): String =
    "Vocabulary spelling: ${vocabularyWords.joinToString(separator = ", ")}"

private const val PENDING_QUIZ_CLOCK_SKEW_MILLIS = 30_000L
