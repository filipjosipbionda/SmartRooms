package com.benza.smartrooms.feature.roomquizbuilder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.QuizKind
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.feature.roomdetail.labelRes
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class QuizPage(
    val titleRes: Int,
) {
    SAVED(R.string.room_quizzes_tab_saved),
    CREATE(R.string.room_quizzes_tab_create),
}

@Composable
internal fun RoomQuizBuilderRouteScreen(
    roomId: String,
    roomName: String,
    roomTopic: String,
    onBackClick: () -> Unit,
    onOpenReviewClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    viewModel: RoomQuizBuilderViewModel =
        koinViewModel(
            parameters = { parametersOf(roomId, roomName, roomTopic) },
        ),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    RoomQuizBuilderScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onOpenReviewClick = onOpenReviewClick,
        onOpenQuizClick = onOpenQuizClick,
        onQuizKindSelected = viewModel::onQuizKindSelected,
        onTitleChanged = viewModel::onTitleChanged,
        onTopicChanged = viewModel::onTopicChanged,
        onVocabularyWordsChanged = viewModel::onVocabularyWordsChanged,
        onQuestionCountInputChanged = viewModel::onQuestionCountInputChanged,
        onQuestionTypeSelected = viewModel::onQuestionTypeSelected,
        onQuestionTimeLimitEnabledChanged = viewModel::onQuestionTimeLimitEnabledChanged,
        onQuestionTimeLimitInputChanged = viewModel::onQuestionTimeLimitInputChanged,
        onQuizLongPress = viewModel::onQuizLongPress,
        onToggleQuizSelection = viewModel::toggleQuizSelection,
        onClearQuizSelection = viewModel::clearQuizSelection,
        onSelectAllQuizzes = viewModel::selectAllQuizzes,
        onRetryQuizClick = viewModel::retryQuiz,
        onRequestDeleteQuizClick = viewModel::requestDeleteQuiz,
        onRequestDeleteSelectedQuizzes = viewModel::requestDeleteSelectedQuizzes,
        onDismissDeleteQuizDialog = viewModel::dismissDeleteQuizDialog,
        onConfirmDeleteQuizClick = viewModel::confirmDeleteQuiz,
        onGenerateQuizClick = viewModel::generateQuiz,
        onInfoMessageShown = viewModel::consumeInfoMessage,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoomQuizBuilderScreen(
    uiState: RoomQuizBuilderUiState,
    onBackClick: () -> Unit,
    onOpenReviewClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    onQuizKindSelected: (QuizKind) -> Unit,
    onTitleChanged: (String) -> Unit,
    onTopicChanged: (String) -> Unit,
    onVocabularyWordsChanged: (String) -> Unit,
    onQuestionCountInputChanged: (String) -> Unit,
    onQuestionTypeSelected: (QuestionType) -> Unit,
    onQuestionTimeLimitEnabledChanged: (Boolean) -> Unit,
    onQuestionTimeLimitInputChanged: (String) -> Unit,
    onQuizLongPress: (String) -> Unit,
    onToggleQuizSelection: (String) -> Unit,
    onClearQuizSelection: () -> Unit,
    onSelectAllQuizzes: () -> Unit,
    onRetryQuizClick: (String) -> Unit,
    onRequestDeleteQuizClick: (String) -> Unit,
    onRequestDeleteSelectedQuizzes: () -> Unit,
    onDismissDeleteQuizDialog: () -> Unit,
    onConfirmDeleteQuizClick: () -> Unit,
    onGenerateQuizClick: () -> Unit,
    onInfoMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { QuizPage.entries.size })
    val scope = rememberCoroutineScope()
    val infoMessage = uiState.infoMessageRes?.let { stringResource(it) }
    val errorBannerMessage =
        uiState.errorMessageText
            ?: uiState.errorMessageRes?.let { stringResource(it) }

    LaunchedEffect(infoMessage) {
        val message = infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onInfoMessageShown()
    }

    LaunchedEffect(uiState.isGeneratingQuiz) {
        if (uiState.isGeneratingQuiz) {
            pagerState.animateScrollToPage(QuizPage.SAVED.ordinal)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuizScreenTopBar(
                    roomName = uiState.roomName,
                    onBackClick = onBackClick,
                )
                if (errorBannerMessage != null) {
                    AuthFeedbackBanner(
                        message = errorBannerMessage,
                        type = AuthFeedbackType.Error,
                    )
                }
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    QuizPage.entries.forEachIndexed { index, page ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    text = stringResource(page.titleRes),
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (QuizPage.entries[page]) {
                        QuizPage.SAVED ->
                            SavedQuizzesPage(
                                quizzes = uiState.quizzes,
                                selectedQuizIds = uiState.selectedQuizIds,
                                retryingQuizId = uiState.retryingQuizId,
                                isLoading = uiState.isLoadingQuizzes,
                                onOpenReviewClick = onOpenReviewClick,
                                onOpenQuizClick = onOpenQuizClick,
                                onQuizLongPress = onQuizLongPress,
                                onToggleQuizSelection = onToggleQuizSelection,
                                onRetryQuizClick = onRetryQuizClick,
                                onRequestDeleteQuizClick = onRequestDeleteQuizClick,
                            )
                        QuizPage.CREATE ->
                            CreateQuizPage(
                                selectedQuizKind = uiState.selectedQuizKind,
                                titleInput = uiState.titleInput,
                                questionCountInput = uiState.questionCountInput,
                                selectedQuestionType = uiState.selectedQuestionType,
                                hasQuestionTimeLimit = uiState.hasQuestionTimeLimit,
                                questionTimeLimitInput = uiState.questionTimeLimitInput,
                                topicInput = uiState.topicInput,
                                vocabularyWordsInput = uiState.vocabularyWordsInput,
                                titleErrorRes = uiState.titleErrorRes,
                                topicErrorRes = uiState.topicErrorRes,
                                vocabularyWordsErrorRes = uiState.vocabularyWordsErrorRes,
                                questionCountErrorRes = uiState.questionCountErrorRes,
                                questionTimeLimitErrorRes = uiState.questionTimeLimitErrorRes,
                                isGeneratingQuiz = uiState.isGeneratingQuiz,
                                onQuizKindSelected = onQuizKindSelected,
                                onTitleChanged = onTitleChanged,
                                onTopicChanged = onTopicChanged,
                                onVocabularyWordsChanged = onVocabularyWordsChanged,
                                onQuestionCountInputChanged = onQuestionCountInputChanged,
                                onQuestionTypeSelected = onQuestionTypeSelected,
                                onQuestionTimeLimitEnabledChanged = onQuestionTimeLimitEnabledChanged,
                                onQuestionTimeLimitInputChanged = onQuestionTimeLimitInputChanged,
                                onGenerateQuizClick = onGenerateQuizClick,
                            )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = pagerState.currentPage == QuizPage.SAVED.ordinal && uiState.isSelectionMode,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            QuizSelectionOverlay(
                selectedCount = uiState.selectedQuizIds.size,
                selectableQuizCount = uiState.quizzes.count(RoomQuizSummary::canBeDeleted),
                isDeleting = uiState.isDeletingQuiz,
                onClearQuizSelection = onClearQuizSelection,
                onSelectAllQuizzes = onSelectAllQuizzes,
                onRequestDeleteSelectedQuizzes = onRequestDeleteSelectedQuizzes,
            )
        }
    }

    if (uiState.pendingQuizDeletion != null) {
        DeleteQuizDialog(
            pendingDeletion = uiState.pendingQuizDeletion,
            isDeleting = uiState.isDeletingQuiz,
            onDismiss = onDismissDeleteQuizDialog,
            onConfirm = onConfirmDeleteQuizClick,
        )
    }
}

@Composable
private fun QuizScreenTopBar(
    roomName: String,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.room_quizzes_title, roomName),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.room_quizzes_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedQuizzesPage(
    quizzes: List<RoomQuizSummary>,
    selectedQuizIds: Set<String>,
    retryingQuizId: String?,
    isLoading: Boolean,
    onOpenReviewClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    onQuizLongPress: (String) -> Unit,
    onToggleQuizSelection: (String) -> Unit,
    onRetryQuizClick: (String) -> Unit,
    onRequestDeleteQuizClick: (String) -> Unit,
) {
    val isSelectionMode = selectedQuizIds.isNotEmpty()
    var expandedMenuQuizId by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (isSelectionMode) 156.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.room_quizzes_list_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (quizzes.isNotEmpty() && !isSelectionMode) {
            item {
                QuizSelectionHintCard()
            }
        }
        when {
            isLoading -> {
                item {
                    PlaceholderCard(text = stringResource(R.string.room_quizzes_loading))
                }
            }
            quizzes.isEmpty() -> {
                item {
                    PlaceholderCard(text = stringResource(R.string.room_quizzes_empty))
                }
            }
            else -> {
                items(quizzes.size) { index ->
                    QuizSummaryCard(
                        quiz = quizzes[index],
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedQuizIds.contains(quizzes[index].id),
                        isRetrying = retryingQuizId == quizzes[index].id,
                        onOpenReviewClick = onOpenReviewClick,
                        onOpenQuizClick = onOpenQuizClick,
                        onLongPress = onQuizLongPress,
                        onSelectionToggle = onToggleQuizSelection,
                        isMenuExpanded = expandedMenuQuizId == quizzes[index].id,
                        onMenuExpandedChange = { isExpanded ->
                            expandedMenuQuizId = if (isExpanded) quizzes[index].id else null
                        },
                        onRetryQuizClick = onRetryQuizClick,
                        onRequestDeleteQuizClick = onRequestDeleteQuizClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizSelectionHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ),
    ) {
        Text(
            text = stringResource(R.string.room_quizzes_selection_hint),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuizSelectionOverlay(
    selectedCount: Int,
    selectableQuizCount: Int,
    isDeleting: Boolean,
    onClearQuizSelection: () -> Unit,
    onSelectAllQuizzes: () -> Unit,
    onRequestDeleteSelectedQuizzes: () -> Unit,
) {
    val areAllSelectableSelected = selectableQuizCount > 0 && selectedCount >= selectableQuizCount
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClearQuizSelection,
                    enabled = !isDeleting,
                    modifier = Modifier.size(SELECTION_OVERLAY_ICON_SIZE),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_clear_selection),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.room_quizzes_selection_count, selectedCount),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    modifier = Modifier.size(SELECTION_OVERLAY_ICON_SIZE),
                    color = Color.Transparent,
                ) {}
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onSelectAllQuizzes,
                    enabled = !isDeleting && !areAllSelectableSelected,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(SELECTION_OVERLAY_BUTTON_HEIGHT),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                ) {
                    Text(
                        text = stringResource(R.string.action_select_all),
                        maxLines = 1,
                    )
                }
                Button(
                    onClick = onRequestDeleteSelectedQuizzes,
                    enabled = selectedCount > 0 && !isDeleting,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(SELECTION_OVERLAY_BUTTON_HEIGHT),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.action_delete_short),
                        modifier = Modifier.padding(start = 8.dp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private val SELECTION_OVERLAY_ICON_SIZE = 36.dp
private val SELECTION_OVERLAY_BUTTON_HEIGHT = 44.dp

@Composable
private fun CreateQuizPage(
    selectedQuizKind: QuizKind,
    titleInput: String,
    questionCountInput: String,
    selectedQuestionType: QuestionType,
    hasQuestionTimeLimit: Boolean,
    questionTimeLimitInput: String,
    topicInput: String,
    vocabularyWordsInput: String,
    titleErrorRes: Int?,
    topicErrorRes: Int?,
    vocabularyWordsErrorRes: Int?,
    questionCountErrorRes: Int?,
    questionTimeLimitErrorRes: Int?,
    isGeneratingQuiz: Boolean,
    onQuizKindSelected: (QuizKind) -> Unit,
    onTitleChanged: (String) -> Unit,
    onTopicChanged: (String) -> Unit,
    onVocabularyWordsChanged: (String) -> Unit,
    onQuestionCountInputChanged: (String) -> Unit,
    onQuestionTypeSelected: (QuestionType) -> Unit,
    onQuestionTimeLimitEnabledChanged: (Boolean) -> Unit,
    onQuestionTimeLimitInputChanged: (String) -> Unit,
    onGenerateQuizClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GenerateQuizCard(
                selectedQuizKind = selectedQuizKind,
                titleInput = titleInput,
                questionCountInput = questionCountInput,
                selectedQuestionType = selectedQuestionType,
                hasQuestionTimeLimit = hasQuestionTimeLimit,
                questionTimeLimitInput = questionTimeLimitInput,
                topicInput = topicInput,
                vocabularyWordsInput = vocabularyWordsInput,
                titleErrorRes = titleErrorRes,
                topicErrorRes = topicErrorRes,
                vocabularyWordsErrorRes = vocabularyWordsErrorRes,
                questionCountErrorRes = questionCountErrorRes,
                questionTimeLimitErrorRes = questionTimeLimitErrorRes,
                isGeneratingQuiz = isGeneratingQuiz,
                onQuizKindSelected = onQuizKindSelected,
                onTitleChanged = onTitleChanged,
                onTopicChanged = onTopicChanged,
                onVocabularyWordsChanged = onVocabularyWordsChanged,
                onQuestionCountInputChanged = onQuestionCountInputChanged,
                onQuestionTypeSelected = onQuestionTypeSelected,
                onQuestionTimeLimitEnabledChanged = onQuestionTimeLimitEnabledChanged,
                onQuestionTimeLimitInputChanged = onQuestionTimeLimitInputChanged,
                onGenerateQuizClick = onGenerateQuizClick,
            )
        }
    }
}

@Composable
private fun PlaceholderCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GenerateQuizCard(
    selectedQuizKind: QuizKind,
    titleInput: String,
    questionCountInput: String,
    selectedQuestionType: QuestionType,
    hasQuestionTimeLimit: Boolean,
    questionTimeLimitInput: String,
    topicInput: String,
    vocabularyWordsInput: String,
    titleErrorRes: Int?,
    topicErrorRes: Int?,
    vocabularyWordsErrorRes: Int?,
    questionCountErrorRes: Int?,
    questionTimeLimitErrorRes: Int?,
    isGeneratingQuiz: Boolean,
    onQuizKindSelected: (QuizKind) -> Unit,
    onTitleChanged: (String) -> Unit,
    onTopicChanged: (String) -> Unit,
    onVocabularyWordsChanged: (String) -> Unit,
    onQuestionCountInputChanged: (String) -> Unit,
    onQuestionTypeSelected: (QuestionType) -> Unit,
    onQuestionTimeLimitEnabledChanged: (Boolean) -> Unit,
    onQuestionTimeLimitInputChanged: (String) -> Unit,
    onGenerateQuizClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.room_quizzes_create_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.room_quizzes_create_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.room_quizzes_kind_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuizKind.entries.forEach { quizKind ->
                        FilterChip(
                            selected = quizKind == selectedQuizKind,
                            onClick = { onQuizKindSelected(quizKind) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(quizKind.labelRes()), maxLines = 1) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = titleInput,
                onValueChange = onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label_quiz_title)) },
                singleLine = true,
                isError = titleErrorRes != null,
                supportingText =
                    if (titleErrorRes != null) {
                        {
                            Text(stringResource(titleErrorRes))
                        }
                    } else {
                        null
                    },
            )
            if (selectedQuizKind == QuizKind.GRAMMAR) {
                OutlinedTextField(
                    value = topicInput,
                    onValueChange = onTopicChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_quiz_topic)) },
                    placeholder = { Text(stringResource(R.string.room_quizzes_topic_placeholder)) },
                    singleLine = true,
                    isError = topicErrorRes != null,
                    supportingText = {
                        if (topicErrorRes != null) {
                            Text(stringResource(topicErrorRes))
                        }
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.room_detail_question_count_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    OutlinedTextField(
                        value = questionCountInput,
                        onValueChange = onQuestionCountInputChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.room_quizzes_question_count_input_label)) },
                        singleLine = true,
                        isError = questionCountErrorRes != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText =
                            if (questionCountErrorRes != null) {
                                {
                                    Text(stringResource(questionCountErrorRes))
                                }
                            } else {
                                null
                            },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.room_detail_question_type_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(QuestionType.MULTIPLE_CHOICE, QuestionType.FILL_IN_BLANK).forEach { questionType ->
                            FilterChip(
                                selected = questionType == selectedQuestionType,
                                onClick = { onQuestionTypeSelected(questionType) },
                                modifier = Modifier.weight(1f),
                                label = {
                                    Text(
                                        text = stringResource(labelRes(questionType)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = vocabularyWordsInput,
                    onValueChange = onVocabularyWordsChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_vocabulary_words)) },
                    placeholder = { Text(stringResource(R.string.room_quizzes_vocabulary_words_placeholder)) },
                    minLines = 4,
                    isError = vocabularyWordsErrorRes != null,
                    supportingText = {
                        Text(
                            text =
                                if (vocabularyWordsErrorRes != null) {
                                    stringResource(vocabularyWordsErrorRes)
                                } else {
                                    stringResource(R.string.room_quizzes_vocabulary_words_hint)
                                },
                        )
                    },
                )
            }
            QuestionTimeLimitEditor(
                isEnabled = hasQuestionTimeLimit,
                value = questionTimeLimitInput,
                errorRes = questionTimeLimitErrorRes,
                onEnabledChanged = onQuestionTimeLimitEnabledChanged,
                onValueChanged = onQuestionTimeLimitInputChanged,
            )
            Button(
                onClick = onGenerateQuizClick,
                enabled = !isGeneratingQuiz,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isGeneratingQuiz) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .padding(end = 10.dp)
                                .size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text =
                        stringResource(
                            if (isGeneratingQuiz) {
                                R.string.action_generating_room_quiz
                            } else {
                                R.string.action_generate_room_quiz
                            },
                        ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun QuestionTimeLimitEditor(
    isEnabled: Boolean,
    value: String,
    errorRes: Int?,
    onEnabledChanged: (Boolean) -> Unit,
    onValueChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.room_quizzes_time_limit_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.room_quizzes_time_limit_toggle_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChanged,
            )
        }
        if (isEnabled) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.room_quizzes_time_limit_input_label)) },
                singleLine = true,
                isError = errorRes != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText =
                    if (errorRes != null) {
                        {
                            Text(text = stringResource(errorRes))
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuizSummaryCard(
    quiz: RoomQuizSummary,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isRetrying: Boolean,
    onOpenReviewClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onSelectionToggle: (String) -> Unit,
    isMenuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onRetryQuizClick: (String) -> Unit,
    onRequestDeleteQuizClick: (String) -> Unit,
) {
    val displayStatus =
        if (isRetrying && quiz.status == RoomQuizStatus.FAILED) {
            RoomQuizStatus.GENERATING
        } else {
            quiz.status
        }
    val canDelete = quiz.canBeDeleted()
    val canSolve = displayStatus == RoomQuizStatus.READY
    val canReview = displayStatus == RoomQuizStatus.REVIEW
    val canRetry = displayStatus == RoomQuizStatus.FAILED
    val isGenerating = displayStatus == RoomQuizStatus.GENERATING
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode && canDelete) {
                            onSelectionToggle(quiz.id)
                        }
                    },
                    onLongClick = {
                        if (canDelete) {
                            onLongPress(quiz.id)
                        }
                    },
                ),
        shape = MaterialTheme.shapes.extraLarge,
        border =
            when {
                isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                displayStatus == RoomQuizStatus.FAILED ->
                    BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                else -> null
            },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (isSelectionMode && canDelete) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionToggle(quiz.id) },
                    )
                }
                Text(
                    text = quiz.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isSelectionMode && canDelete) {
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(
                            onClick = { onMenuExpandedChange(true) },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.room_quiz_actions_menu),
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { onMenuExpandedChange(false) },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete_quiz)) },
                                onClick = {
                                    onMenuExpandedChange(false)
                                    onRequestDeleteQuizClick(quiz.id)
                                },
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.room_quizzes_topic_value, quiz.topic),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuizMetaChip(
                    modifier = Modifier.weight(1f),
                    value = stringResource(quiz.quizKind.labelRes()),
                    label = stringResource(R.string.room_quizzes_kind_title),
                )
                QuizMetaChip(
                    modifier = Modifier.weight(1f),
                    value =
                        quiz.cefrLevel.ifBlank {
                            stringResource(R.string.room_detail_level_not_set)
                        },
                    label = stringResource(R.string.room_detail_feed_level_metric),
                )
                QuizMetaChip(
                    modifier = Modifier.weight(1f),
                    value = stringResource(labelRes(quiz.questionType)),
                    label = stringResource(R.string.room_detail_question_type_title),
                )
                QuizMetaChip(
                    modifier = Modifier.weight(1f),
                    value = quiz.questionCount.toString(),
                    label = stringResource(R.string.room_detail_question_count_title),
                )
            }
            QuizStatusBadge(status = displayStatus)
            if (displayStatus == RoomQuizStatus.FAILED && !quiz.failureReason.isNullOrBlank()) {
                Text(
                    text = quiz.failureReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text =
                    stringResource(
                        R.string.room_detail_generated_at,
                        quiz.createdAtEpochMillis.toRoomDateLabel(),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    when {
                        canSolve -> onOpenQuizClick(quiz.id)
                        canReview -> onOpenReviewClick(quiz.id)
                        canRetry -> onRetryQuizClick(quiz.id)
                    }
                },
                enabled = !isSelectionMode && (canSolve || canReview || canRetry),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isGenerating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(stringResource(displayStatus.actionLabelRes()))
                    }
                } else {
                    Text(stringResource(displayStatus.actionLabelRes()))
                }
            }
        }
    }
}

private fun RoomQuizSummary.canBeDeleted(): Boolean = status != RoomQuizStatus.GENERATING

@Composable
private fun QuizStatusBadge(status: RoomQuizStatus) {
    val label =
        when (status) {
            RoomQuizStatus.GENERATING -> stringResource(R.string.room_quiz_status_generating)
            RoomQuizStatus.REVIEW -> stringResource(R.string.room_quiz_status_review)
            RoomQuizStatus.READY -> stringResource(R.string.room_quiz_status_ready)
            RoomQuizStatus.FAILED -> stringResource(R.string.room_quiz_status_failed)
        }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun RoomQuizStatus.actionLabelRes(): Int =
    when (this) {
        RoomQuizStatus.GENERATING -> R.string.room_quiz_status_generating
        RoomQuizStatus.REVIEW -> R.string.action_review_quiz
        RoomQuizStatus.READY -> R.string.action_solve_quiz
        RoomQuizStatus.FAILED -> R.string.action_retry_quiz
    }

@Composable
private fun DeleteQuizDialog(
    pendingDeletion: PendingQuizDeletion,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title =
        when (pendingDeletion.mode) {
            QuizDeletionMode.SINGLE -> stringResource(R.string.delete_quiz_dialog_title)
            QuizDeletionMode.SELECTED -> stringResource(R.string.delete_selected_quizzes_dialog_title)
            QuizDeletionMode.ALL -> stringResource(R.string.delete_all_quizzes_dialog_title)
        }
    val message =
        when (pendingDeletion.mode) {
            QuizDeletionMode.SINGLE ->
                stringResource(
                    R.string.delete_quiz_dialog_message,
                    pendingDeletion.quizTitle.orEmpty(),
                )
            QuizDeletionMode.SELECTED ->
                stringResource(
                    R.string.delete_selected_quizzes_dialog_message,
                    pendingDeletion.quizIds.size,
                )
            QuizDeletionMode.ALL ->
                stringResource(
                    R.string.delete_all_quizzes_dialog_message,
                    pendingDeletion.quizIds.size,
                )
        }
    val confirmLabel =
        when (pendingDeletion.mode) {
            QuizDeletionMode.SINGLE -> stringResource(R.string.action_delete_quiz)
            QuizDeletionMode.SELECTED -> stringResource(R.string.action_delete_selected_quizzes)
            QuizDeletionMode.ALL -> stringResource(R.string.action_delete_all_quizzes)
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(message)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun QuizMetaChip(
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
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
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

private fun Long.toRoomDateLabel(): String {
    if (this <= 0L) return "--"
    return DateTimeFormatter
        .ofPattern("d MMM, HH:mm", Locale.ENGLISH)
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun RoomQuizBuilderScreenPreview() {
    SmartRoomsTheme {
        RoomQuizBuilderScreen(
            uiState =
                RoomQuizBuilderUiState(
                    roomId = "room-1",
                    roomName = "English Grammar Room",
                    roomTopic = "Travel vocabulary",
                    selectedQuizKind = QuizKind.GRAMMAR,
                    titleInput = "Travel Essentials Quiz",
                    topicInput = "Travel vocabulary",
                    vocabularyWordsInput = "",
                    questionCountInput = "10",
                    quizzes =
                        listOf(
                            RoomQuizSummary(
                                id = "quiz-1",
                                title = "Travel Essentials Quiz",
                                quizKind = QuizKind.GRAMMAR,
                                topic = "Travel vocabulary",
                                vocabularyWords = emptyList(),
                                cefrLevel = "B2",
                                questionType = QuestionType.MULTIPLE_CHOICE,
                                questionCount = 10,
                                status = RoomQuizStatus.READY,
                                createdAtEpochMillis = System.currentTimeMillis(),
                            ),
                        ),
                    isLoadingQuizzes = false,
                ),
            onBackClick = {},
            onOpenReviewClick = {},
            onOpenQuizClick = {},
            onQuizKindSelected = {},
            onTitleChanged = {},
            onTopicChanged = {},
            onVocabularyWordsChanged = {},
            onQuestionCountInputChanged = {},
            onQuestionTypeSelected = {},
            onQuestionTimeLimitEnabledChanged = {},
            onQuestionTimeLimitInputChanged = {},
            onQuizLongPress = {},
            onToggleQuizSelection = {},
            onClearQuizSelection = {},
            onSelectAllQuizzes = {},
            onRetryQuizClick = {},
            onRequestDeleteQuizClick = {},
            onRequestDeleteSelectedQuizzes = {},
            onDismissDeleteQuizDialog = {},
            onConfirmDeleteQuizClick = {},
            onGenerateQuizClick = {},
            onInfoMessageShown = {},
        )
    }
}

private fun QuizKind.labelRes(): Int =
    when (this) {
        QuizKind.GRAMMAR -> R.string.room_quiz_kind_grammar
        QuizKind.VOCABULARY -> R.string.room_quiz_kind_vocabulary
    }
