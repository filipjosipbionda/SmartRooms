package com.benza.smartrooms.feature.roomdetail

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.RoomInvitationAccess
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.ui.components.AttachmentPreview
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.components.attachmentTypeLabel
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun RoomDetailRouteScreen(
    roomId: String,
    roomName: String,
    roomTopic: String,
    roomCefrLevel: String,
    onBackClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onEditPostClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    onOpenQuizzesClick: () -> Unit,
    viewModel: RoomDetailViewModel =
        koinViewModel(
            parameters = { parametersOf(roomId, roomName, roomTopic, roomCefrLevel) },
        ),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    RoomDetailScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onCreatePostClick = onCreatePostClick,
        onEditPostClick = onEditPostClick,
        onOpenQuizClick = onOpenQuizClick,
        onOpenQuizzesClick = onOpenQuizzesClick,
        onShowInviteDialog = viewModel::showInviteDialog,
        onDismissInviteDialog = viewModel::dismissInviteDialog,
        onInviteSearchQueryChange = viewModel::onInviteSearchQueryChanged,
        onSendInviteClick = viewModel::sendInvite,
        onRequestAnnouncementDeletionClick = viewModel::requestAnnouncementDeletion,
        onDismissAnnouncementDeletion = viewModel::dismissAnnouncementDeletion,
        onDeleteAnnouncementClick = viewModel::deleteAnnouncement,
        onInfoMessageShown = viewModel::consumeInfoMessage,
    )
}

@Composable
internal fun RoomDetailScreen(
    uiState: RoomDetailUiState,
    onBackClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onEditPostClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    onOpenQuizzesClick: () -> Unit,
    onShowInviteDialog: () -> Unit,
    onDismissInviteDialog: () -> Unit,
    onInviteSearchQueryChange: (String) -> Unit,
    onSendInviteClick: (InviteUserUiState) -> Unit,
    onRequestAnnouncementDeletionClick: (RoomFeedItemUiState.Announcement) -> Unit,
    onDismissAnnouncementDeletion: () -> Unit,
    onDeleteAnnouncementClick: () -> Unit,
    onInfoMessageShown: () -> Unit,
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
                    isLoadingRoom = uiState.isLoadingRoom,
                    isLoadingFeed = uiState.isLoadingFeed,
                    onCreateAnnouncementClick = onCreatePostClick,
                    onOpenQuizzesClick = onOpenQuizzesClick,
                    onInviteClick = onShowInviteDialog,
                    showInviteAction = uiState.isCurrentUserOwner,
                )
            }
            item {
                if (uiState.errorMessageRes != null) {
                    AuthFeedbackBanner(
                        message = stringResource(uiState.errorMessageRes),
                        type = AuthFeedbackType.Error,
                    )
                }
            }
            item {
                FeedSectionHeader()
            }
            when {
                uiState.isLoadingFeed && uiState.feedItems.isEmpty() -> {
                    item {
                        FeedLoadingSkeleton()
                    }
                }

                uiState.feedItems.isEmpty() -> {
                    item {
                        FeedPlaceholderCard(text = stringResource(R.string.room_detail_feed_empty))
                    }
                }

                else -> {
                    items(
                        items = uiState.feedItems,
                        key = ::feedItemKey,
                        contentType = ::feedItemContentType,
                    ) { item ->
                        when (item) {
                            is RoomFeedItemUiState.Announcement ->
                                AnnouncementFeedCard(
                                    item = item,
                                    onEditClick = { onEditPostClick(item.id) },
                                    onDeleteClick = { onRequestAnnouncementDeletionClick(item) },
                                )

                            is RoomFeedItemUiState.Quiz ->
                                QuizFeedCard(
                                    item = item,
                                    onOpenQuizClick = onOpenQuizClick,
                                )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (uiState.isInviteDialogOpen) {
        RoomInviteDialog(
            uiState = uiState,
            onSearchQueryChange = onInviteSearchQueryChange,
            onSendInviteClick = onSendInviteClick,
            onDismiss = onDismissInviteDialog,
        )
    }

    if (uiState.pendingAnnouncementDeletion != null) {
        DeleteAnnouncementDialog(
            pendingDeletion = uiState.pendingAnnouncementDeletion,
            isDeleting = uiState.isDeletingAnnouncement,
            onDismiss = onDismissAnnouncementDeletion,
            onConfirm = onDeleteAnnouncementClick,
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
                text = roomName,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.room_detail_feed_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    isLoadingRoom: Boolean,
    isLoadingFeed: Boolean,
    onCreateAnnouncementClick: () -> Unit,
    onOpenQuizzesClick: () -> Unit,
    onInviteClick: () -> Unit,
    showInviteAction: Boolean,
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
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Lagoon.copy(alpha = 0.16f),
                                        MaterialTheme.colorScheme.surface,
                                        Coral.copy(alpha = 0.14f),
                                    ),
                            ),
                    ).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isLoadingRoom && roomTopic.isBlank()) {
                    SkeletonLine(
                        widthFraction = 0.62f,
                        height = 20.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.room_detail_topic_value, roomTopic),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.room_detail_feed_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isLoadingFeed) {
                    SummaryMetricSkeletonCard(
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetricSkeletonCard(
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    SummaryMetricCard(
                        modifier = Modifier.weight(1f),
                        value = postCount.toString(),
                        label = stringResource(R.string.room_detail_feed_posts_metric),
                    )
                    SummaryMetricCard(
                        modifier = Modifier.weight(1f),
                        value = quizCount.toString(),
                        label = stringResource(R.string.room_detail_feed_quizzes_metric),
                    )
                }
                if (isLoadingRoom) {
                    SummaryMetricSkeletonCard(
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    SummaryMetricCard(
                        modifier = Modifier.weight(1f),
                        value =
                            cefrLevel.ifBlank {
                                stringResource(R.string.room_detail_level_not_set)
                            },
                        label = stringResource(R.string.room_detail_feed_level_metric),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onCreateAnnouncementClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.action_create_post),
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onOpenQuizzesClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.action_open_quizzes),
                        maxLines = 1,
                    )
                }
            }
            if (showInviteAction) {
                OutlinedButton(
                    onClick = onInviteClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(stringResource(R.string.action_invite_to_room))
                }
            } else if (isLoadingRoom) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonLine(
                            widthFraction = 0.42f,
                            height = 14.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
private fun SummaryMetricSkeletonCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SkeletonLine(
                widthFraction = 0.44f,
                height = 20.dp,
            )
            SkeletonLine(
                widthFraction = 0.66f,
                height = 10.dp,
            )
        }
    }
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "roomSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.82f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "roomSkeletonAlpha",
    )

    Surface(
        modifier = modifier.fillMaxWidth(widthFraction),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.12f),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(height),
        )
    }
}

@Composable
private fun FeedSectionHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.room_detail_feed_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.room_detail_feed_timeline_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun feedItemKey(item: RoomFeedItemUiState): String =
    when (item) {
        is RoomFeedItemUiState.Announcement -> "announcement:${item.id}"
        is RoomFeedItemUiState.Quiz -> "quiz:${item.id}"
    }

private fun feedItemContentType(item: RoomFeedItemUiState): String =
    when (item) {
        is RoomFeedItemUiState.Announcement -> "announcement"
        is RoomFeedItemUiState.Quiz -> "quiz"
    }

@Composable
private fun FeedPlaceholderCard(text: String) {
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
private fun FeedLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FeedSkeletonCard(
            accentBrush =
                Brush.horizontalGradient(
                    listOf(
                        Lagoon.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            showAction = false,
        )
        FeedSkeletonCard(
            accentBrush =
                Brush.horizontalGradient(
                    listOf(
                        Coral.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            showAction = true,
        )
    }
}

@Composable
private fun FeedSkeletonCard(
    accentBrush: Brush,
    showAction: Boolean,
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
                    .background(accentBrush)
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonLine(
                    widthFraction = 0.28f,
                    height = 32.dp,
                )
                SkeletonLine(
                    widthFraction = 0.18f,
                    height = 12.dp,
                )
            }
            SkeletonLine(
                widthFraction = 0.52f,
                height = 20.dp,
            )
            SkeletonLine(
                widthFraction = 0.92f,
                height = 14.dp,
            )
            SkeletonLine(
                widthFraction = 0.74f,
                height = 14.dp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SkeletonLine(
                                widthFraction = 0.46f,
                                height = 16.dp,
                            )
                            SkeletonLine(
                                widthFraction = 0.68f,
                                height = 10.dp,
                            )
                        }
                    }
                }
            }
            if (showAction) {
                SkeletonLine(
                    widthFraction = 1f,
                    height = 40.dp,
                )
            }
        }
    }
}

@Composable
private fun AnnouncementFeedCard(
    item: RoomFeedItemUiState.Announcement,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var isMenuExpanded by remember(item.id) { mutableStateOf(false) }
    FeedCardShell(
        icon = Icons.Outlined.Campaign,
        label = stringResource(R.string.room_detail_feed_post),
        createdAt = item.createdAtEpochMillis.toRoomDateLabel(),
        accentBrush =
            Brush.horizontalGradient(
                listOf(
                    Lagoon.copy(alpha = 0.14f),
                    MaterialTheme.colorScheme.surface,
                ),
            ),
        actions =
            if (item.canManage) {
                {
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.room_detail_post_actions_menu),
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            shape = MaterialTheme.shapes.extraLarge,
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 10.dp,
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                    )
                                },
                                text = { Text(stringResource(R.string.action_edit_post)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onEditClick()
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                    )
                                },
                                text = { Text(stringResource(R.string.action_delete_post)) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDeleteClick()
                                },
                            )
                        }
                    }
                }
            } else {
                null
            },
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.message,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (item.attachments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.attachments.forEach { attachment ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AttachmentPreview(
                                modifier = Modifier.size(56.dp),
                                model = attachment.downloadUrl,
                                mimeType = attachment.mimeType,
                                fileName = attachment.name,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachment.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = attachmentTypeLabel(attachment.name, attachment.mimeType),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.room_detail_post_author, item.authorName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuizFeedCard(
    item: RoomFeedItemUiState.Quiz,
    onOpenQuizClick: (String) -> Unit,
) {
    val isReady = item.status == RoomQuizStatus.READY
    FeedCardShell(
        icon = Icons.Outlined.Quiz,
        label = stringResource(R.string.room_detail_feed_quiz),
        createdAt = item.createdAtEpochMillis.toRoomDateLabel(),
        accentBrush =
            Brush.horizontalGradient(
                listOf(
                    Coral.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.surface,
                ),
            ),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.room_quizzes_topic_value, item.topic),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeedMetaCard(
                modifier = Modifier.weight(1f),
                value = item.cefrLevel,
                label = stringResource(R.string.room_detail_feed_level_metric),
            )
            FeedMetaCard(
                modifier = Modifier.weight(1f),
                value = stringResource(item.questionTypeLabelRes),
                label = stringResource(R.string.room_detail_question_type_title),
            )
            FeedMetaCard(
                modifier = Modifier.weight(1f),
                value = item.questionCount.toString(),
                label = stringResource(R.string.room_detail_question_count_title),
            )
        }
        QuizFeedStatusChip(status = item.status)
        Button(
            onClick = { onOpenQuizClick(item.id) },
            enabled = isReady,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(item.status.actionLabelRes()))
        }
    }
}

@Composable
private fun QuizFeedStatusChip(status: RoomQuizStatus) {
    val labelRes =
        when (status) {
            RoomQuizStatus.GENERATING -> R.string.room_quiz_status_generating
            RoomQuizStatus.REVIEW -> R.string.room_quiz_status_review
            RoomQuizStatus.READY -> R.string.room_quiz_status_ready
            RoomQuizStatus.FAILED -> R.string.room_quiz_status_failed
        }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
    ) {
        Text(
            text = stringResource(labelRes),
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
        RoomQuizStatus.FAILED -> R.string.room_quiz_status_failed
    }

@Composable
private fun FeedCardShell(
    icon: ImageVector,
    label: String,
    createdAt: String,
    accentBrush: Brush,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
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
                    .background(accentBrush)
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedLabelChip(icon = icon, label = label)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = createdAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    actions?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
private fun FeedLabelChip(
    icon: ImageVector,
    label: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FeedMetaCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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

@Composable
private fun DeleteAnnouncementDialog(
    pendingDeletion: PendingAnnouncementDeletion,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.delete_post_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text =
                        stringResource(
                            R.string.delete_post_dialog_message,
                            pendingDeletion.announcementTitle,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isDeleting,
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isDeleting,
                    ) {
                        Text(stringResource(R.string.action_delete_post))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomInviteDialog(
    uiState: RoomDetailUiState,
    onSearchQueryChange: (String) -> Unit,
    onSendInviteClick: (InviteUserUiState) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.room_invite_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.room_invite_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = uiState.inviteSearchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSendingInvite,
                    label = { Text(stringResource(R.string.label_user_search)) },
                    singleLine = true,
                )
                when {
                    uiState.inviteSearchQuery.length < 2 -> {
                        Text(
                            text = stringResource(R.string.room_invite_search_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    uiState.isSearchingInviteUsers -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = stringResource(R.string.room_invite_search_loading),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    uiState.inviteSearchResults.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.room_invite_search_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.inviteSearchResults.forEach { user ->
                                InviteUserResultCard(
                                    user = user,
                                    isSendingInvite = uiState.isSendingInvite,
                                    onSendInviteClick = { onSendInviteClick(user) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isSendingInvite,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun InviteUserResultCard(
    user: InviteUserUiState,
    isSendingInvite: Boolean,
    onSendInviteClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            R.string.room_invite_access_summary,
                            stringResource(user.roleLabelRes()),
                            stringResource(user.accessLabelRes()),
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onSendInviteClick,
                enabled = !isSendingInvite,
            ) {
                Text(stringResource(R.string.action_send_invite))
            }
        }
    }
}

private fun InviteUserUiState.roleLabelRes(): Int =
    when (role) {
        UserRole.TEACHER -> R.string.profile_role_professor
        UserRole.STUDENT -> R.string.profile_role_student
        null -> R.string.profile_role_not_selected
    }

private fun InviteUserUiState.accessLabelRes(): Int =
    when (access) {
        RoomInvitationAccess.COLLABORATOR -> R.string.room_invite_access_collaborator
        RoomInvitationAccess.MEMBER -> R.string.room_invite_access_member
    }

private fun Long.toRoomDateLabel(): String {
    if (this <= 0L) return "--"
    return DateTimeFormatter
        .ofPattern("d MMM, HH:mm", Locale.ENGLISH)
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun RoomDetailScreenPreview() {
    SmartRoomsTheme {
        RoomDetailScreen(
            uiState =
                RoomDetailUiState(
                    roomId = "room-1",
                    roomName = "English Grammar Room",
                    roomTopic = "Travel vocabulary",
                    feedItems =
                        listOf(
                            RoomFeedItemUiState.Announcement(
                                id = "announcement-1",
                                createdAtEpochMillis = System.currentTimeMillis(),
                                title = "Homework",
                                message = "Review unit 3 and prepare five new travel expressions.",
                                authorName = "Prof. Benza",
                                attachments = emptyList(),
                                canManage = true,
                            ),
                            RoomFeedItemUiState.Quiz(
                                id = "quiz-1",
                                createdAtEpochMillis = System.currentTimeMillis() - 3_600_000L,
                                title = "Travel Essentials Quiz",
                                topic = "Travel vocabulary",
                                cefrLevel = "B2",
                                status = RoomQuizStatus.READY,
                                questionTypeLabelRes = labelRes(QuestionType.MULTIPLE_CHOICE),
                                questionCount = 10,
                            ),
                        ),
                    isLoadingRoom = false,
                    isLoadingFeed = false,
                ),
            onBackClick = {},
            onCreatePostClick = {},
            onEditPostClick = {},
            onOpenQuizClick = {},
            onOpenQuizzesClick = {},
            onShowInviteDialog = {},
            onDismissInviteDialog = {},
            onInviteSearchQueryChange = {},
            onSendInviteClick = {},
            onRequestAnnouncementDeletionClick = {},
            onDismissAnnouncementDeletion = {},
            onDeleteAnnouncementClick = {},
            onInfoMessageShown = {},
        )
    }
}
