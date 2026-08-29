package com.benza.smartrooms.feature.roomquizreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.RoomQuizQuestion
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.feature.roomdetail.labelRes
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun RoomQuizReviewRouteScreen(
    roomId: String,
    roomName: String,
    quizId: String,
    onBackClick: () -> Unit,
    viewModel: RoomQuizReviewViewModel =
        koinViewModel(
            parameters = { parametersOf(roomId, roomName, quizId) },
        ),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RoomQuizReviewEvent.QuizPublished -> onBackClick()
            }
        }
    }

    RoomQuizReviewScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onPreviousClick = viewModel::goToPreviousQuestion,
        onNextClick = viewModel::goToNextQuestion,
        onPromptChanged = viewModel::onPromptChanged,
        onExplanationChanged = viewModel::onExplanationChanged,
        onOptionChanged = viewModel::onOptionChanged,
        onCorrectOptionSelected = viewModel::onCorrectOptionSelected,
        onAnswerChanged = viewModel::onAnswerChanged,
        onSaveQuestionClick = viewModel::saveQuestion,
        onDeleteQuestionClick = viewModel::deleteQuestion,
        onPublishQuizClick = viewModel::publishQuiz,
        onInfoMessageShown = viewModel::consumeInfoMessage,
    )
}

@Composable
internal fun RoomQuizReviewScreen(
    uiState: RoomQuizReviewUiState,
    onBackClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPromptChanged: (String) -> Unit,
    onExplanationChanged: (String) -> Unit,
    onOptionChanged: (Int, String) -> Unit,
    onCorrectOptionSelected: (Int) -> Unit,
    onAnswerChanged: (String) -> Unit,
    onSaveQuestionClick: () -> Unit,
    onDeleteQuestionClick: () -> Unit,
    onPublishQuizClick: () -> Unit,
    onInfoMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val quiz = uiState.quiz
    val question = quiz?.questions?.getOrNull(uiState.currentQuestionIndex)
    val infoMessage = uiState.infoMessageRes?.let { stringResource(it) }

    LaunchedEffect(infoMessage) {
        val message = infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onInfoMessageShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                ReviewTopBar(
                    roomName = uiState.roomName,
                    title = quiz?.title ?: stringResource(R.string.room_quiz_review_title),
                    onBackClick = onBackClick,
                )
            }
            if (uiState.errorMessageRes != null) {
                item {
                    AuthFeedbackBanner(
                        message = stringResource(uiState.errorMessageRes),
                        type = AuthFeedbackType.Error,
                    )
                }
            }
            when {
                uiState.isLoadingQuiz -> {
                    item { LoadingReviewCard() }
                }

                quiz != null && question != null -> {
                    item {
                        ReviewSummaryCard(
                            status = quiz.status,
                            questionType = question.type,
                            currentIndex = uiState.currentQuestionIndex,
                            questionCount = quiz.questionCount,
                        )
                    }
                    item {
                        ReviewQuestionEditorCard(
                            question = question,
                            promptInput = uiState.promptInput,
                            explanationInput = uiState.explanationInput,
                            optionInputs = uiState.optionInputs,
                            correctOptionIndex = uiState.correctOptionIndex,
                            answerInput = uiState.answerInput,
                            onPromptChanged = onPromptChanged,
                            onExplanationChanged = onExplanationChanged,
                            onOptionChanged = onOptionChanged,
                            onCorrectOptionSelected = onCorrectOptionSelected,
                            onAnswerChanged = onAnswerChanged,
                        )
                    }
                    item {
                        ReviewActionsCard(
                            questionCount = quiz.questionCount,
                            currentQuestionIndex = uiState.currentQuestionIndex,
                            isSavingQuestion = uiState.isSavingQuestion,
                            isDeletingQuestion = uiState.isDeletingQuestion,
                            isPublishingQuiz = uiState.isPublishingQuiz,
                            canPublish = quiz.status != RoomQuizStatus.READY,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick,
                            onSaveQuestionClick = onSaveQuestionClick,
                            onDeleteQuestionClick = onDeleteQuestionClick,
                            onPublishQuizClick = onPublishQuizClick,
                        )
                    }
                }

                quiz != null -> {
                    item {
                        FailedReviewCard(
                            status = quiz.status,
                            failureReason = quiz.failureReason,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewTopBar(
    roomName: String,
    title: String,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
            Text(
                text = stringResource(R.string.room_quiz_review_top_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LoadingReviewCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.room_quiz_review_loading))
        }
    }
}

@Composable
private fun FailedReviewCard(
    status: RoomQuizStatus,
    failureReason: String?,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text =
                    if (status == RoomQuizStatus.FAILED) {
                        stringResource(R.string.room_quiz_status_failed)
                    } else {
                        stringResource(R.string.room_quiz_review_loading)
                    },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = failureReason ?: stringResource(R.string.error_quiz_generation_generic),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (status == RoomQuizStatus.FAILED) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun ReviewSummaryCard(
    status: RoomQuizStatus,
    questionType: QuestionType,
    currentIndex: Int,
    questionCount: Int,
) {
    Card(
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
                    ).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.room_quiz_review_progress, currentIndex + 1, questionCount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ReviewMetricChip(
                    modifier = Modifier.weight(1f),
                    value = stringResource(status.reviewLabelRes()),
                    label = stringResource(R.string.room_quiz_review_status_short),
                )
                ReviewMetricChip(
                    modifier = Modifier.weight(1f),
                    value = stringResource(labelRes(questionType)),
                    label = stringResource(R.string.room_quiz_review_type_short),
                )
                ReviewMetricChip(
                    modifier = Modifier.weight(1f),
                    value = "${currentIndex + 1}/$questionCount",
                    label = stringResource(R.string.room_quiz_review_progress_short),
                )
            }
        }
    }
}

@Composable
private fun ReviewMetricChip(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
private fun ReviewQuestionEditorCard(
    question: RoomQuizQuestion,
    promptInput: String,
    explanationInput: String,
    optionInputs: List<String>,
    correctOptionIndex: Int,
    answerInput: String,
    onPromptChanged: (String) -> Unit,
    onExplanationChanged: (String) -> Unit,
    onOptionChanged: (Int, String) -> Unit,
    onCorrectOptionSelected: (Int) -> Unit,
    onAnswerChanged: (String) -> Unit,
) {
    ReviewSectionCard(
        title = stringResource(R.string.room_quiz_review_editor_title),
        subtitle = stringResource(R.string.room_quiz_review_editor_subtitle),
    ) {
        OutlinedTextField(
            value = promptInput,
            onValueChange = onPromptChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.room_quiz_review_prompt_label)) },
        )
        when (question) {
            is RoomQuizQuestion.MultipleChoice -> {
                optionInputs.forEachIndexed { index, option ->
                    val isSelected = correctOptionIndex == index
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                                    },
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = option,
                                onValueChange = { onOptionChanged(index, it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.room_quiz_review_option_label, index + 1)) },
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                if (isSelected) {
                                    Button(onClick = { onCorrectOptionSelected(index) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                        )
                                        Text(
                                            text = stringResource(R.string.room_quiz_review_correct_answer_badge),
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                } else {
                                    OutlinedButton(onClick = { onCorrectOptionSelected(index) }) {
                                        Text(stringResource(R.string.room_quiz_review_mark_as_correct))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is RoomQuizQuestion.FillInBlank,
            is RoomQuizQuestion.WordScramble,
            -> {
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = onAnswerChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringResource(
                                if (question is RoomQuizQuestion.WordScramble) {
                                    R.string.room_quiz_review_answer_word_label
                                } else {
                                    R.string.room_quiz_review_answer_text_label
                                },
                            ),
                        )
                    },
                )
            }
        }
        OutlinedTextField(
            value = explanationInput,
            onValueChange = onExplanationChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.room_quiz_review_explanation_label)) },
        )
    }
}

@Composable
private fun ReviewActionsCard(
    questionCount: Int,
    currentQuestionIndex: Int,
    isSavingQuestion: Boolean,
    isDeletingQuestion: Boolean,
    isPublishingQuiz: Boolean,
    canPublish: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSaveQuestionClick: () -> Unit,
    onDeleteQuestionClick: () -> Unit,
    onPublishQuizClick: () -> Unit,
) {
    ReviewSectionCard(
        title = stringResource(R.string.room_quiz_review_actions_title),
        subtitle = stringResource(R.string.room_quiz_review_actions_subtitle),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onPreviousClick,
                enabled = currentQuestionIndex > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_previous))
            }
            OutlinedButton(
                onClick = onNextClick,
                enabled = currentQuestionIndex < questionCount - 1,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_next))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSaveQuestionClick,
                enabled = !isSavingQuestion,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.room_quiz_review_save))
            }
            OutlinedButton(
                onClick = onDeleteQuestionClick,
                enabled = !isDeletingQuestion,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.room_quiz_review_delete))
            }
        }
        if (canPublish) {
            Button(
                onClick = onPublishQuizClick,
                enabled = !isPublishingQuiz,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.room_quiz_review_publish))
            }
        }
    }
}

@Composable
private fun ReviewSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

private fun RoomQuizStatus.reviewLabelRes(): Int =
    when (this) {
        RoomQuizStatus.GENERATING -> R.string.room_quiz_status_generating
        RoomQuizStatus.REVIEW -> R.string.room_quiz_status_review
        RoomQuizStatus.READY -> R.string.room_quiz_status_ready
        RoomQuizStatus.FAILED -> R.string.room_quiz_status_failed
    }
