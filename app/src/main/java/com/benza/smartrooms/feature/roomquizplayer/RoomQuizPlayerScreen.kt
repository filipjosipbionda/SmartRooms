package com.benza.smartrooms.feature.roomquizplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.RoomQuiz
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.model.RoomQuizQuestionResult
import com.benza.smartrooms.data.room.model.RoomQuizScoring
import com.benza.smartrooms.feature.roomdetail.labelRes
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun RoomQuizPlayerRouteScreen(
    roomId: String,
    roomName: String,
    quizId: String,
    onBackClick: () -> Unit,
    viewModel: RoomQuizPlayerViewModel =
        koinViewModel(
            parameters = { parametersOf(roomId, roomName, quizId) },
        ),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RoomQuizPlayerEvent.ExitCompleted -> onBackClick()
            }
        }
    }

    RoomQuizPlayerScreen(
        uiState = uiState.value,
        onExitClick = viewModel::finishCurrentAttemptAndExit,
        onPreviousClick = viewModel::goToPreviousQuestion,
        onNextClick = viewModel::goToNextQuestion,
        onSubmitAnswerClick = viewModel::submitCurrentAnswer,
        onFinishClick = viewModel::finishQuiz,
        onOptionSelected = viewModel::selectOption,
        onFillInAnswerChanged = viewModel::updateFillInAnswer,
        onScrambleTilePlaced = viewModel::placeScrambleTile,
        onScrambleSlotCleared = viewModel::clearScrambleSlot,
    )
}

@Composable
internal fun RoomQuizPlayerScreen(
    uiState: RoomQuizPlayerUiState,
    onExitClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSubmitAnswerClick: () -> Unit,
    onFinishClick: () -> Unit,
    onOptionSelected: (String, Int) -> Unit,
    onFillInAnswerChanged: (String, String) -> Unit,
    onScrambleTilePlaced: (String, String, Int) -> Unit,
    onScrambleSlotCleared: (String, Int) -> Unit,
) {
    val quiz = uiState.quiz
    val currentQuestion = quiz?.questions?.getOrNull(uiState.currentQuestionIndex)
    val isCurrentQuestionLocked = currentQuestion?.id in uiState.lockedQuestionIds

    BackHandler(enabled = !uiState.isSavingResult) {
        onExitClick()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                QuizPlayerTopBar(
                    roomName = uiState.roomName,
                    title = quiz?.title ?: stringResource(R.string.room_quiz_player_loading_title),
                    isSavingResult = uiState.isSavingResult,
                    onExitClick = onExitClick,
                )
            }

            when {
                uiState.isLoadingQuiz -> {
                    item { LoadingQuizCard() }
                }

                uiState.errorMessageRes != null -> {
                    item {
                        AuthFeedbackBanner(
                            message = stringResource(uiState.errorMessageRes),
                            type = AuthFeedbackType.Error,
                        )
                    }
                }

                quiz != null && uiState.isQuizCompleted -> {
                    item {
                        QuizCompletionSummaryCard(
                            quiz = quiz,
                            uiState = uiState,
                            onDoneClick = onExitClick,
                        )
                    }
                }

                quiz != null && currentQuestion != null -> {
                    item {
                        QuizProgressCard(
                            quiz = quiz,
                            currentQuestionIndex = uiState.currentQuestionIndex,
                            score = uiState.score,
                            maxScore = uiState.maxScore,
                            isQuizCompleted = uiState.isQuizCompleted,
                            currentQuestionTimeLimitSeconds = currentQuestion.timeLimitSeconds,
                            remainingTimeSeconds = uiState.remainingTimeSeconds,
                            isCurrentQuestionLocked = isCurrentQuestionLocked,
                        )
                    }
                    item {
                        when (currentQuestion) {
                            is RoomQuizQuestion.MultipleChoice ->
                                MultipleChoiceQuestionCard(
                                    question = currentQuestion,
                                    selectedOptionIndex = uiState.selectedOptionIndexes[currentQuestion.id],
                                    isQuizCompleted = uiState.isQuizCompleted,
                                    isQuestionLocked = isCurrentQuestionLocked,
                                    isCorrect = currentQuestion.id in uiState.correctQuestionIds,
                                    questionScore = uiState.questionResultsById[currentQuestion.id]?.score ?: 0,
                                    questionMaxScore = RoomQuizScoring.maxScore(currentQuestion),
                                    onOptionSelected = { onOptionSelected(currentQuestion.id, it) },
                                )

                            is RoomQuizQuestion.FillInBlank ->
                                FillInBlankQuestionCard(
                                    question = currentQuestion,
                                    value = uiState.fillInAnswers[currentQuestion.id].orEmpty(),
                                    isQuizCompleted = uiState.isQuizCompleted,
                                    isQuestionLocked = isCurrentQuestionLocked,
                                    isCorrect = currentQuestion.id in uiState.correctQuestionIds,
                                    questionScore = uiState.questionResultsById[currentQuestion.id]?.score ?: 0,
                                    questionMaxScore = RoomQuizScoring.maxScore(currentQuestion),
                                    onValueChange = { onFillInAnswerChanged(currentQuestion.id, it) },
                                )

                            is RoomQuizQuestion.WordScramble ->
                                WordScrambleQuestionCard(
                                    question = currentQuestion,
                                    answerSlots =
                                        uiState.scrambleAnswerSlots[currentQuestion.id]
                                            ?: List(currentQuestion.answerWord.length) { null },
                                    isQuizCompleted = uiState.isQuizCompleted,
                                    isQuestionLocked = isCurrentQuestionLocked,
                                    isCorrect = currentQuestion.id in uiState.correctQuestionIds,
                                    questionScore = uiState.questionResultsById[currentQuestion.id]?.score ?: 0,
                                    questionMaxScore = RoomQuizScoring.maxScore(currentQuestion),
                                    onTilePlaced = { tileId, slotIndex ->
                                        onScrambleTilePlaced(currentQuestion.id, tileId, slotIndex)
                                    },
                                    onSlotCleared = { slotIndex ->
                                        onScrambleSlotCleared(currentQuestion.id, slotIndex)
                                    },
                                )
                        }
                    }
                    item {
                        QuizNavigationCard(
                            currentQuestionIndex = uiState.currentQuestionIndex,
                            questionCount = quiz.questionCount,
                            isTimedQuestion = currentQuestion.timeLimitSeconds != null,
                            isQuizCompleted = uiState.isQuizCompleted,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick,
                            onSubmitAnswerClick = onSubmitAnswerClick,
                            onFinishClick = onFinishClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizCompletionSummaryCard(
    quiz: RoomQuiz,
    uiState: RoomQuizPlayerUiState,
    onDoneClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.room_quiz_player_results_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        stringResource(
                            R.string.room_quiz_player_results_subtitle,
                            uiState.correctQuestionIds.size,
                            quiz.questionCount,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.room_quiz_player_score_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.room_quiz_player_score_value, uiState.score, uiState.maxScore),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quiz.questions.forEachIndexed { index, question ->
                    QuizResultQuestionRow(
                        questionIndex = index,
                        question = question,
                        result = uiState.questionResultsById[question.id],
                        userAnswer = question.userAnswerText(uiState),
                        correctAnswer = question.correctAnswerText(),
                    )
                }
            }

            Button(
                onClick = onDoneClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSavingResult,
            ) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}

@Composable
private fun QuizResultQuestionRow(
    questionIndex: Int,
    question: RoomQuizQuestion,
    result: RoomQuizQuestionResult?,
    userAnswer: String,
    correctAnswer: String,
) {
    val isCorrect = result?.isCorrect == true
    val statusText =
        stringResource(
            if (isCorrect) {
                R.string.room_quiz_player_answer_correct
            } else {
                R.string.room_quiz_player_answer_incorrect
            },
        )
    val statusColor =
        if (isCorrect) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.room_quiz_player_result_question_number, questionIndex + 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor,
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text =
                        stringResource(
                            R.string.room_quiz_player_question_score_short,
                            result?.score ?: 0,
                            result?.maxScore ?: RoomQuizScoring.maxScore(question),
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ResultAnswerLine(
                label = stringResource(R.string.room_quiz_player_your_answer_label),
                value = userAnswer,
            )
            ResultAnswerLine(
                label = stringResource(R.string.room_quiz_player_correct_answer_label),
                value = correctAnswer,
            )
        }
    }
}

@Composable
private fun ResultAnswerLine(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun QuizPlayerTopBar(
    roomName: String,
    title: String,
    isSavingResult: Boolean,
    onExitClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onExitClick,
            enabled = !isSavingResult,
        ) {
            if (isSavingResult) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_exit_quiz_save_progress),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = roomName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingQuizCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(text = stringResource(R.string.room_quiz_player_loading))
        }
    }
}

@Composable
private fun QuizProgressCard(
    quiz: RoomQuiz,
    currentQuestionIndex: Int,
    score: Int,
    maxScore: Int,
    isQuizCompleted: Boolean,
    currentQuestionTimeLimitSeconds: Int?,
    remainingTimeSeconds: Int?,
    isCurrentQuestionLocked: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Lagoon.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.surface,
                                    Coral.copy(alpha = 0.12f),
                                ),
                        ),
                    ).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text =
                    stringResource(
                        R.string.room_quiz_player_progress,
                        currentQuestionIndex + 1,
                        quiz.questionCount,
                    ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuizMetric(
                    modifier = Modifier.weight(1f),
                    value =
                        quiz.cefrLevel.ifBlank {
                            stringResource(R.string.room_detail_level_not_set)
                        },
                    label = stringResource(R.string.room_detail_feed_level_metric),
                )
                QuizMetric(
                    modifier = Modifier.weight(1f),
                    value = stringResource(labelRes(quiz.questionType)),
                    label = stringResource(R.string.room_detail_question_type_title),
                )
                QuizMetric(
                    modifier = Modifier.weight(1f),
                    value =
                        if (isQuizCompleted) {
                            stringResource(R.string.room_quiz_player_score_value, score, maxScore)
                        } else {
                            quiz.questionCount.toString()
                        },
                    label =
                        if (isQuizCompleted) {
                            stringResource(R.string.room_quiz_player_score_label)
                        } else {
                            stringResource(R.string.room_detail_question_count_title)
                        },
                )
            }
            QuizTimerBadge(
                currentQuestionTimeLimitSeconds = currentQuestionTimeLimitSeconds,
                remainingTimeSeconds = remainingTimeSeconds,
                isCurrentQuestionLocked = isCurrentQuestionLocked,
                isQuizCompleted = isQuizCompleted,
            )
        }
    }
}

@Composable
private fun QuizTimerBadge(
    currentQuestionTimeLimitSeconds: Int?,
    remainingTimeSeconds: Int?,
    isCurrentQuestionLocked: Boolean,
    isQuizCompleted: Boolean,
) {
    val value =
        when {
            currentQuestionTimeLimitSeconds == null -> stringResource(R.string.room_quiz_player_timer_value_none)
            !isQuizCompleted && isCurrentQuestionLocked -> stringResource(R.string.room_quiz_player_timer_value_expired)
            !isQuizCompleted && remainingTimeSeconds != null -> {
                stringResource(R.string.room_quiz_player_timer_value_seconds, remainingTimeSeconds)
            }
            else -> stringResource(R.string.room_quiz_player_timer_value_seconds, currentQuestionTimeLimitSeconds)
        }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
    ) {
        Text(
            text = stringResource(R.string.room_quiz_player_timer_label, value),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun QuizMetric(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = value, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MultipleChoiceQuestionCard(
    question: RoomQuizQuestion.MultipleChoice,
    selectedOptionIndex: Int?,
    isQuizCompleted: Boolean,
    isQuestionLocked: Boolean,
    isCorrect: Boolean,
    questionScore: Int,
    questionMaxScore: Int,
    onOptionSelected: (Int) -> Unit,
) {
    QuestionCardShell(
        questionNumberLabel = stringResource(R.string.room_detail_type_multiple_choice),
        prompt = question.prompt,
        explanation = question.explanation,
        userAnswer =
            selectedOptionIndex
                ?.let(question.options::getOrNull)
                ?: stringResource(R.string.room_quiz_player_unanswered),
        isQuizCompleted = isQuizCompleted,
        isQuestionLocked = isQuestionLocked,
        isCorrect = isCorrect,
        questionScore = questionScore,
        questionMaxScore = questionMaxScore,
    ) {
        question.options.forEachIndexed { index, option ->
            val isSelected = selectedOptionIndex == index
            val isCorrectOption = question.correctOptionIndex == index
            val containerColor =
                when {
                    isQuizCompleted && isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
                    isQuizCompleted && isSelected -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                }

            OutlinedButton(
                onClick = { onOptionSelected(index) },
                enabled = !isQuizCompleted && !isQuestionLocked,
                modifier = Modifier.fillMaxWidth(),
                border =
                    if (!isQuizCompleted && isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = containerColor,
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun FillInBlankQuestionCard(
    question: RoomQuizQuestion.FillInBlank,
    value: String,
    isQuizCompleted: Boolean,
    isQuestionLocked: Boolean,
    isCorrect: Boolean,
    questionScore: Int,
    questionMaxScore: Int,
    onValueChange: (String) -> Unit,
) {
    QuestionCardShell(
        questionNumberLabel = stringResource(R.string.room_detail_type_fill_in_blank),
        prompt = question.prompt,
        explanation = question.explanation,
        userAnswer = value.ifBlank { stringResource(R.string.room_quiz_player_unanswered) },
        isQuizCompleted = isQuizCompleted,
        isQuestionLocked = isQuestionLocked,
        isCorrect = isCorrect,
        questionScore = questionScore,
        questionMaxScore = questionMaxScore,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isQuizCompleted && !isQuestionLocked,
            label = { Text(stringResource(R.string.room_quiz_player_fill_answer_label)) },
            placeholder = { Text(stringResource(R.string.room_quiz_player_fill_answer_placeholder)) },
        )

        if (isQuizCompleted) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {
                Text(
                    text = stringResource(R.string.room_quiz_player_correct_answer, question.answerText),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun WordScrambleQuestionCard(
    question: RoomQuizQuestion.WordScramble,
    answerSlots: List<String?>,
    isQuizCompleted: Boolean,
    isQuestionLocked: Boolean,
    isCorrect: Boolean,
    questionScore: Int,
    questionMaxScore: Int,
    onTilePlaced: (String, Int) -> Unit,
    onSlotCleared: (Int) -> Unit,
) {
    val tiles = remember(question) { question.buildScrambleTiles() }
    val tilesById = remember(tiles) { tiles.associateBy(ScrambleTile::id) }
    val availableTiles = tiles.filter { tile -> tile.id !in answerSlots.filterNotNull() }
    val userAnswer =
        answerSlots
            .mapNotNull(tilesById::get)
            .joinToString(separator = "") { it.letter }
            .ifBlank { stringResource(R.string.room_quiz_player_unanswered) }
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var bankBounds by remember { mutableStateOf<Rect?>(null) }
    var containerBounds by remember { mutableStateOf<Rect?>(null) }
    var draggedTile by remember { mutableStateOf<ScrambleTile?>(null) }
    var draggedFromSlotIndex by remember { mutableStateOf<Int?>(null) }
    var dragPointerPosition by remember { mutableStateOf(Offset.Zero) }
    var dragTouchOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    containerBounds = coordinates.boundsInRoot()
                },
    ) {
        QuestionCardShell(
            questionNumberLabel = stringResource(R.string.room_detail_type_word_scramble),
            prompt = question.prompt,
            explanation = question.explanation,
            userAnswer = userAnswer,
            isQuizCompleted = isQuizCompleted,
            isQuestionLocked = isQuestionLocked,
            isCorrect = isCorrect,
            questionScore = questionScore,
            questionMaxScore = questionMaxScore,
        ) {
            Text(
                text = stringResource(R.string.room_quiz_player_scramble_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.room_quiz_player_scramble_slots_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                answerSlots.chunked(SCRAMBLE_ROW_SIZE).forEachIndexed { rowIndex, rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowSlots.forEachIndexed { localIndex, tileId ->
                            val slotIndex = rowIndex * SCRAMBLE_ROW_SIZE + localIndex
                            val slotTile = tileId?.let(tilesById::get)
                            ScrambleSlot(
                                tile = slotTile,
                                isCompleted = isQuizCompleted || isQuestionLocked,
                                isBeingDragged = draggedTile?.id == slotTile?.id,
                                onBoundsChanged = { bounds -> slotBounds[slotIndex] = bounds },
                                onDragStarted = { pointerPosition, touchOffset ->
                                    draggedTile = slotTile
                                    draggedFromSlotIndex = slotIndex
                                    dragPointerPosition = pointerPosition
                                    dragTouchOffset = touchOffset
                                },
                                onDragMoved = { dragAmount ->
                                    dragPointerPosition += dragAmount
                                },
                                onDragEnded = {
                                    val dropSlotIndex =
                                        slotBounds.entries
                                            .firstOrNull { (_, bounds) -> bounds.contains(dragPointerPosition) }
                                            ?.key
                                    when {
                                        dropSlotIndex != null && draggedTile != null -> {
                                            onTilePlaced(draggedTile!!.id, dropSlotIndex)
                                        }

                                        bankBounds?.contains(dragPointerPosition) == true &&
                                            draggedFromSlotIndex != null -> {
                                            onSlotCleared(draggedFromSlotIndex!!)
                                        }
                                    }
                                    draggedTile = null
                                    draggedFromSlotIndex = null
                                },
                            )
                        }
                        repeat(SCRAMBLE_ROW_SIZE - rowSlots.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.room_quiz_player_scramble_bank_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier =
                    Modifier.onGloballyPositioned { coordinates ->
                        bankBounds = coordinates.boundsInRoot()
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableTiles.chunked(SCRAMBLE_ROW_SIZE).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowTiles.forEach { tile ->
                            ScrambleBankTile(
                                tile = tile,
                                isCompleted = isQuizCompleted || isQuestionLocked,
                                isBeingDragged = draggedTile?.id == tile.id,
                                onDragStarted = { pointerPosition, touchOffset ->
                                    draggedTile = tile
                                    draggedFromSlotIndex = null
                                    dragPointerPosition = pointerPosition
                                    dragTouchOffset = touchOffset
                                },
                                onDragMoved = { dragAmount ->
                                    dragPointerPosition += dragAmount
                                },
                                onDragEnded = {
                                    val dropSlotIndex =
                                        slotBounds.entries
                                            .firstOrNull { (_, bounds) -> bounds.contains(dragPointerPosition) }
                                            ?.key
                                    if (dropSlotIndex != null && draggedTile != null) {
                                        onTilePlaced(draggedTile!!.id, dropSlotIndex)
                                    }
                                    draggedTile = null
                                    draggedFromSlotIndex = null
                                },
                            )
                        }
                        repeat(SCRAMBLE_ROW_SIZE - rowTiles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (isQuizCompleted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Text(
                        text = stringResource(R.string.room_quiz_player_correct_answer, question.answerWord),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        draggedTile?.let { tile ->
            Surface(
                modifier =
                    Modifier
                        .offset {
                            val containerTopLeft = containerBounds?.topLeft ?: Offset.Zero
                            IntOffset(
                                x = (dragPointerPosition.x - containerTopLeft.x - dragTouchOffset.x).toInt(),
                                y = (dragPointerPosition.y - containerTopLeft.y - dragTouchOffset.y).toInt(),
                            )
                        }.zIndex(1f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
            ) {
                Text(
                    text = tile.letter,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RowScope.ScrambleSlot(
    tile: ScrambleTile?,
    isCompleted: Boolean,
    isBeingDragged: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onDragStarted: (Offset, Offset) -> Unit,
    onDragMoved: (Offset) -> Unit,
    onDragEnded: () -> Unit,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }

    Surface(
        modifier =
            Modifier
                .weight(1f)
                .aspectRatio(1f)
                .onGloballyPositioned { coordinates ->
                    val updatedBounds = coordinates.boundsInRoot()
                    bounds = updatedBounds
                    onBoundsChanged(updatedBounds)
                }.pointerInput(tile?.id, isCompleted, bounds) {
                    if (tile == null || isCompleted || bounds == null) return@pointerInput
                    detectDragGestures(
                        onDragStart = { touchOffset ->
                            onDragStarted(bounds!!.topLeft + touchOffset, touchOffset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragMoved(Offset(dragAmount.x, dragAmount.y))
                        },
                        onDragEnd = onDragEnded,
                        onDragCancel = onDragEnded,
                    )
                },
        shape = RoundedCornerShape(16.dp),
        color =
            if (tile == null) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tile?.letter.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                    if (isBeingDragged) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

@Composable
private fun RowScope.ScrambleBankTile(
    tile: ScrambleTile,
    isCompleted: Boolean,
    isBeingDragged: Boolean,
    onDragStarted: (Offset, Offset) -> Unit,
    onDragMoved: (Offset) -> Unit,
    onDragEnded: () -> Unit,
) {
    var bounds by remember { mutableStateOf<Rect?>(null) }

    Surface(
        modifier =
            Modifier
                .weight(1f)
                .aspectRatio(1f)
                .onGloballyPositioned { coordinates ->
                    bounds = coordinates.boundsInRoot()
                }.pointerInput(tile.id, isCompleted, bounds) {
                    if (isCompleted || bounds == null) return@pointerInput
                    detectDragGestures(
                        onDragStart = { touchOffset ->
                            onDragStarted(bounds!!.topLeft + touchOffset, touchOffset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragMoved(Offset(dragAmount.x, dragAmount.y))
                        },
                        onDragEnd = onDragEnded,
                        onDragCancel = onDragEnded,
                    )
                },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tile.letter,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                    if (isBeingDragged) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

@Composable
private fun QuestionCardShell(
    questionNumberLabel: String,
    prompt: String,
    explanation: String,
    userAnswer: String,
    isQuizCompleted: Boolean,
    isQuestionLocked: Boolean,
    isCorrect: Boolean,
    questionScore: Int,
    questionMaxScore: Int,
    content: @Composable () -> Unit,
) {
    var isExplanationExpanded by remember(prompt) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = questionNumberLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()

            if (!isQuizCompleted && isQuestionLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = stringResource(R.string.room_quiz_player_time_expired_locked),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (isQuizCompleted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color =
                        if (isCorrect) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text =
                                stringResource(
                                    if (isCorrect) {
                                        R.string.room_quiz_player_answer_correct
                                    } else {
                                        R.string.room_quiz_player_answer_incorrect
                                    },
                                ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.room_quiz_player_question_score_value,
                                    questionScore,
                                    questionMaxScore,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.room_quiz_player_your_answer, userAnswer),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!isCorrect) {
                            IconButton(
                                onClick = { isExplanationExpanded = !isExplanationExpanded },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.room_quiz_player_show_explanation),
                                )
                            }
                            if (isExplanationExpanded) {
                                Text(
                                    text = stringResource(R.string.room_quiz_player_explanation, explanation),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val SCRAMBLE_ROW_SIZE = 6

@Composable
private fun RoomQuizQuestion.userAnswerText(uiState: RoomQuizPlayerUiState): String =
    when (this) {
        is RoomQuizQuestion.MultipleChoice ->
            uiState.selectedOptionIndexes[id]
                ?.let(options::getOrNull)
                ?: stringResource(R.string.room_quiz_player_unanswered)
        is RoomQuizQuestion.FillInBlank ->
            uiState.fillInAnswers[id]
                .orEmpty()
                .ifBlank { stringResource(R.string.room_quiz_player_unanswered) }
        is RoomQuizQuestion.WordScramble -> {
            val tilesById = buildScrambleTiles().associateBy(ScrambleTile::id)
            uiState.scrambleAnswerSlots[id]
                .orEmpty()
                .mapNotNull(tilesById::get)
                .joinToString(separator = "") { it.letter }
                .ifBlank { stringResource(R.string.room_quiz_player_unanswered) }
        }
    }

private fun RoomQuizQuestion.correctAnswerText(): String =
    when (this) {
        is RoomQuizQuestion.MultipleChoice -> options.getOrNull(correctOptionIndex).orEmpty()
        is RoomQuizQuestion.FillInBlank -> answerText
        is RoomQuizQuestion.WordScramble -> answerWord
    }

@Composable
private fun QuizNavigationCard(
    currentQuestionIndex: Int,
    questionCount: Int,
    isTimedQuestion: Boolean,
    isQuizCompleted: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSubmitAnswerClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isQuizCompleted && isTimedQuestion) {
                    Button(
                        onClick = onSubmitAnswerClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_submit_answer))
                    }
                } else {
                    OutlinedButton(
                        onClick = onPreviousClick,
                        enabled = currentQuestionIndex > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_previous))
                    }

                    if (currentQuestionIndex < questionCount - 1) {
                        Button(
                            onClick = onNextClick,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_next))
                        }
                    } else if (!isQuizCompleted) {
                        Button(
                            onClick = onFinishClick,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.action_finish_quiz))
                        }
                    }
                }
            }

            if (!isQuizCompleted && currentQuestionIndex == questionCount - 1) {
                Text(
                    text = stringResource(R.string.room_quiz_player_finish_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
