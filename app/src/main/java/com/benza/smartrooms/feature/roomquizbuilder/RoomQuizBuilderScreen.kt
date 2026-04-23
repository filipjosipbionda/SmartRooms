package com.benza.smartrooms.feature.roomquizbuilder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.feature.roomdetail.labelRes
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class QuizPage(val titleRes: Int) {
    SAVED(R.string.room_quizzes_tab_saved),
    CREATE(R.string.room_quizzes_tab_create)
}

@Composable
internal fun RoomQuizBuilderRouteScreen(
    roomId: String,
    roomName: String,
    roomTopic: String,
    onBackClick: () -> Unit,
    viewModel: RoomQuizBuilderViewModel = koinViewModel(
        parameters = { parametersOf(roomId, roomName, roomTopic) }
    )
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    RoomQuizBuilderScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onQuestionCountSelected = viewModel::onQuestionCountSelected,
        onQuestionTypeSelected = viewModel::onQuestionTypeSelected,
        onGenerateQuizClick = viewModel::generateQuiz,
        onInfoMessageShown = viewModel::consumeInfoMessage
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RoomQuizBuilderScreen(
    uiState: RoomQuizBuilderUiState,
    onBackClick: () -> Unit,
    onQuestionCountSelected: (Int) -> Unit,
    onQuestionTypeSelected: (QuestionType) -> Unit,
    onGenerateQuizClick: () -> Unit,
    onInfoMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { QuizPage.entries.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.infoMessageRes) {
        val messageRes = uiState.infoMessageRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(messageRes))
        onInfoMessageShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuizScreenTopBar(
                roomName = uiState.roomName,
                onBackClick = onBackClick
            )
            QuizOverviewCard(
                roomTopic = uiState.roomTopic,
                cefrLevel = uiState.cefrLevel,
                quizCount = uiState.quizzes.size
            )
            if (uiState.errorMessageRes != null) {
                AuthFeedbackBanner(
                    message = stringResource(uiState.errorMessageRes),
                    type = AuthFeedbackType.Error
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
                                maxLines = 1
                            )
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (QuizPage.entries[page]) {
                    QuizPage.SAVED -> SavedQuizzesPage(
                        quizzes = uiState.quizzes,
                        isLoading = uiState.isLoadingQuizzes
                    )
                    QuizPage.CREATE -> CreateQuizPage(
                        selectedQuestionCount = uiState.selectedQuestionCount,
                        selectedQuestionType = uiState.selectedQuestionType,
                        isGeneratingQuiz = uiState.isGeneratingQuiz,
                        onQuestionCountSelected = onQuestionCountSelected,
                        onQuestionTypeSelected = onQuestionTypeSelected,
                        onGenerateQuizClick = onGenerateQuizClick
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizScreenTopBar(
    roomName: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.room_quizzes_title, roomName),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.room_quizzes_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuizOverviewCard(
    roomTopic: String,
    cefrLevel: String,
    quizCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Coral.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surface,
                            Lagoon.copy(alpha = 0.16f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.room_detail_topic_value, roomTopic),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.room_quiz_builder_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuizMetricCard(
                    modifier = Modifier.weight(1f),
                    value = quizCount.toString(),
                    label = stringResource(R.string.room_detail_feed_quizzes_metric)
                )
                QuizMetricCard(
                    modifier = Modifier.weight(1f),
                    value = cefrLevel,
                    label = stringResource(R.string.room_detail_feed_level_metric)
                )
                QuizMetricCard(
                    modifier = Modifier.weight(1f),
                    value = stringResource(R.string.room_quizzes_teacher_note_short),
                    label = stringResource(R.string.room_quizzes_teacher_note)
                )
            }
        }
    }
}

@Composable
private fun QuizMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SavedQuizzesPage(
    quizzes: List<RoomQuizSummary>,
    isLoading: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.room_quizzes_list_title),
                style = MaterialTheme.typography.titleMedium
            )
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
                    QuizSummaryCard(quizzes[index])
                }
            }
        }
    }
}

@Composable
private fun CreateQuizPage(
    selectedQuestionCount: Int,
    selectedQuestionType: QuestionType,
    isGeneratingQuiz: Boolean,
    onQuestionCountSelected: (Int) -> Unit,
    onQuestionTypeSelected: (QuestionType) -> Unit,
    onGenerateQuizClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GenerateQuizCard(
                selectedQuestionCount = selectedQuestionCount,
                selectedQuestionType = selectedQuestionType,
                isGeneratingQuiz = isGeneratingQuiz,
                onQuestionCountSelected = onQuestionCountSelected,
                onQuestionTypeSelected = onQuestionTypeSelected,
                onGenerateQuizClick = onGenerateQuizClick
            )
        }
    }
}

@Composable
private fun PlaceholderCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GenerateQuizCard(
    selectedQuestionCount: Int,
    selectedQuestionType: QuestionType,
    isGeneratingQuiz: Boolean,
    onQuestionCountSelected: (Int) -> Unit,
    onQuestionTypeSelected: (QuestionType) -> Unit,
    onGenerateQuizClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.room_quizzes_create_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.room_quizzes_create_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.room_detail_question_count_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    questionCountOptions.forEach { option ->
                        FilterChip(
                            selected = option == selectedQuestionCount,
                            onClick = { onQuestionCountSelected(option) },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = option.toString(),
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.room_detail_question_type_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuestionType.entries.forEach { questionType ->
                        FilterChip(
                            selected = questionType == selectedQuestionType,
                            onClick = { onQuestionTypeSelected(questionType) },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = stringResource(labelRes(questionType)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
            Button(
                onClick = onGenerateQuizClick,
                enabled = !isGeneratingQuiz,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGeneratingQuiz) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(
                        if (isGeneratingQuiz) {
                            R.string.action_generating_room_quiz
                        } else {
                            R.string.action_generate_room_quiz
                        }
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuizSummaryCard(quiz: RoomQuizSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = quiz.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuizMetaChip(
                    modifier = Modifier.weight(1f),
                    value = quiz.cefrLevel,
                    label = stringResource(R.string.room_detail_feed_level_metric)
                )
                QuizMetaChip(
                    modifier = Modifier.weight(1f),
                    value = stringResource(labelRes(quiz.questionType)),
                    label = stringResource(R.string.room_detail_question_type_title)
                )
                QuizMetaChip(
                    modifier = Modifier.weight(1f),
                    value = quiz.questionCount.toString(),
                    label = stringResource(R.string.room_detail_question_count_title)
                )
            }
            Text(
                text = stringResource(
                    R.string.room_detail_generated_at,
                    quiz.createdAtEpochMillis.toRoomDateLabel()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuizMetaChip(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun Long.toRoomDateLabel(): String {
    if (this <= 0L) return "--"
    return DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun RoomQuizBuilderScreenPreview() {
    SmartRoomsTheme {
        RoomQuizBuilderScreen(
            uiState = RoomQuizBuilderUiState(
                roomId = "room-1",
                roomName = "English B1 Room",
                roomTopic = "Travel vocabulary",
                quizzes = listOf(
                    RoomQuizSummary(
                        id = "quiz-1",
                        title = "Travel Essentials Quiz",
                        cefrLevel = "B1",
                        questionType = QuestionType.MULTIPLE_CHOICE,
                        questionCount = 10,
                        createdAtEpochMillis = System.currentTimeMillis()
                    )
                ),
                isLoadingQuizzes = false
            ),
            onBackClick = {},
            onQuestionCountSelected = {},
            onQuestionTypeSelected = {},
            onGenerateQuizClick = {},
            onInfoMessageShown = {}
        )
    }
}
