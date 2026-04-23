package com.benza.smartrooms.feature.roomdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun RoomDetailRouteScreen(
    roomId: String,
    roomName: String,
    roomTopic: String,
    onBackClick: () -> Unit,
    onOpenQuizzesClick: () -> Unit,
    viewModel: RoomDetailViewModel = koinViewModel(
        parameters = { parametersOf(roomId, roomName, roomTopic) }
    )
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    RoomDetailScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onOpenQuizzesClick = onOpenQuizzesClick,
        onShowCreateAnnouncementDialog = viewModel::showCreateAnnouncementDialog,
        onDismissCreateAnnouncementDialog = viewModel::dismissCreateAnnouncementDialog,
        onAnnouncementTitleChange = viewModel::onAnnouncementTitleChanged,
        onAnnouncementMessageChange = viewModel::onAnnouncementMessageChanged,
        onCreateAnnouncementClick = viewModel::createAnnouncement,
        onInfoMessageShown = viewModel::consumeInfoMessage
    )
}

@Composable
internal fun RoomDetailScreen(
    uiState: RoomDetailUiState,
    onBackClick: () -> Unit,
    onOpenQuizzesClick: () -> Unit,
    onShowCreateAnnouncementDialog: () -> Unit,
    onDismissCreateAnnouncementDialog: () -> Unit,
    onAnnouncementTitleChange: (String) -> Unit,
    onAnnouncementMessageChange: (String) -> Unit,
    onCreateAnnouncementClick: () -> Unit,
    onInfoMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.infoMessageRes) {
        val messageRes = uiState.infoMessageRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(messageRes))
        onInfoMessageShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RoomDetailTopBar(
                    roomName = uiState.roomName,
                    onBackClick = onBackClick,
                )
            }
            item {
                RoomSummaryCard(
                    roomTopic = uiState.roomTopic,
                    cefrLevel = uiState.cefrLevel,
                    postCount = uiState.feedItems.count { it is RoomFeedItemUiState.Announcement },
                    quizCount = uiState.feedItems.count { it is RoomFeedItemUiState.Quiz },
                    onCreateAnnouncementClick = onShowCreateAnnouncementDialog,
                    onOpenQuizzesClick = onOpenQuizzesClick
                )
            }
            item {
                if (uiState.errorMessageRes != null) {
                    AuthFeedbackBanner(
                        message = stringResource(uiState.errorMessageRes),
                        type = AuthFeedbackType.Error
                    )
                }
            }
            item {
                FeedSection(
                    feedItems = uiState.feedItems,
                    isLoading = uiState.isLoadingFeed
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (uiState.isCreateAnnouncementDialogOpen) {
        CreateAnnouncementDialog(
            uiState = uiState,
            onTitleChange = onAnnouncementTitleChange,
            onMessageChange = onAnnouncementMessageChange,
            onDismiss = onDismissCreateAnnouncementDialog,
            onConfirm = onCreateAnnouncementClick
        )
    }
}

@Composable
private fun RoomDetailTopBar(
    roomName: String,
    onBackClick: () -> Unit,
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
                text = roomName,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.room_detail_feed_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoomSummaryCard(
    roomTopic: String,
    cefrLevel: String,
    postCount: Int,
    quizCount: Int,
    onCreateAnnouncementClick: () -> Unit,
    onOpenQuizzesClick: () -> Unit
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
                            Lagoon.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface,
                            Coral.copy(alpha = 0.14f)
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
                    text = stringResource(R.string.room_detail_feed_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    value = postCount.toString(),
                    label = stringResource(R.string.room_detail_feed_posts_metric)
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    value = quizCount.toString(),
                    label = stringResource(R.string.room_detail_feed_quizzes_metric)
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    value = cefrLevel,
                    label = stringResource(R.string.room_detail_feed_level_metric)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreateAnnouncementClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.action_create_post),
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = onOpenQuizzesClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.action_open_quizzes),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
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
                maxLines = 1
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
private fun FeedSection(
    feedItems: List<RoomFeedItemUiState>,
    isLoading: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.room_detail_feed_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.room_detail_feed_timeline_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            isLoading -> {
                FeedPlaceholderCard(text = stringResource(R.string.room_detail_feed_loading))
            }

            feedItems.isEmpty() -> {
                FeedPlaceholderCard(text = stringResource(R.string.room_detail_feed_empty))
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    feedItems.forEach { item ->
                        when (item) {
                            is RoomFeedItemUiState.Announcement -> AnnouncementFeedCard(item)
                            is RoomFeedItemUiState.Quiz -> QuizFeedCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedPlaceholderCard(text: String) {
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
private fun AnnouncementFeedCard(item: RoomFeedItemUiState.Announcement) {
    FeedCardShell(
        icon = Icons.Outlined.Campaign,
        label = stringResource(R.string.room_detail_feed_post),
        createdAt = item.createdAtEpochMillis.toRoomDateLabel(),
        accentBrush = Brush.horizontalGradient(
            listOf(
                Lagoon.copy(alpha = 0.14f),
                MaterialTheme.colorScheme.surface
            )
        )
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.message,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.room_detail_post_author, item.authorName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuizFeedCard(item: RoomFeedItemUiState.Quiz) {
    FeedCardShell(
        icon = Icons.Outlined.Quiz,
        label = stringResource(R.string.room_detail_feed_quiz),
        createdAt = item.createdAtEpochMillis.toRoomDateLabel(),
        accentBrush = Brush.horizontalGradient(
            listOf(
                Coral.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.surface
            )
        )
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeedMetaCard(
                modifier = Modifier.weight(1f),
                value = item.cefrLevel,
                label = stringResource(R.string.room_detail_feed_level_metric)
            )
            FeedMetaCard(
                modifier = Modifier.weight(1f),
                value = stringResource(item.questionTypeLabelRes),
                label = stringResource(R.string.room_detail_question_type_title)
            )
            FeedMetaCard(
                modifier = Modifier.weight(1f),
                value = item.questionCount.toString(),
                label = stringResource(R.string.room_detail_question_count_title)
            )
        }
    }
}

@Composable
private fun FeedCardShell(
    icon: ImageVector,
    label: String,
    createdAt: String,
    accentBrush: Brush,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentBrush)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedLabelChip(icon = icon, label = label)
                Text(
                    text = createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            content()
        }
    }
}

@Composable
private fun FeedLabelChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.88f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeedMetaCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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

@Composable
private fun CreateAnnouncementDialog(
    uiState: RoomDetailUiState,
    onTitleChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_post_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.create_post_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = uiState.announcementTitleInput,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_post_title)) },
                    isError = uiState.announcementTitleErrorRes != null,
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.announcementMessageInput,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_post_message)) },
                    isError = uiState.announcementMessageErrorRes != null,
                    minLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !uiState.isCreatingAnnouncement
            ) {
                Text(stringResource(R.string.action_publish_post))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun Long.toRoomDateLabel(): String {
    if (this <= 0L) return "--"
    return DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun RoomDetailScreenPreview() {
    SmartRoomsTheme {
        RoomDetailScreen(
            uiState = RoomDetailUiState(
                roomId = "room-1",
                roomName = "English B1 Room",
                roomTopic = "Travel vocabulary",
                feedItems = listOf(
                    RoomFeedItemUiState.Announcement(
                        id = "announcement-1",
                        createdAtEpochMillis = System.currentTimeMillis(),
                        title = "Homework",
                        message = "Review unit 3 and prepare five new travel expressions.",
                        authorName = "Prof. Benza"
                    ),
                    RoomFeedItemUiState.Quiz(
                        id = "quiz-1",
                        createdAtEpochMillis = System.currentTimeMillis() - 3_600_000L,
                        title = "Travel Essentials Quiz",
                        cefrLevel = "B1",
                        questionTypeLabelRes = labelRes(QuestionType.MULTIPLE_CHOICE),
                        questionCount = 10
                    )
                ),
                isLoadingFeed = false
            ),
            onBackClick = {},
            onOpenQuizzesClick = {},
            onShowCreateAnnouncementDialog = {},
            onDismissCreateAnnouncementDialog = {},
            onAnnouncementTitleChange = {},
            onAnnouncementMessageChange = {},
            onCreateAnnouncementClick = {},
            onInfoMessageShown = {}
        )
    }
}
