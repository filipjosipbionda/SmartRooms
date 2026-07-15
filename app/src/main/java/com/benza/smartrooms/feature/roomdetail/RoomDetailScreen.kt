package com.benza.smartrooms.feature.roomdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.QuestionType
import com.benza.smartrooms.data.room.model.QuizKind
import com.benza.smartrooms.data.room.model.RoomMemberAccountRole
import com.benza.smartrooms.data.room.model.RoomMemberRole
import com.benza.smartrooms.data.room.model.RoomQuizResult
import com.benza.smartrooms.data.room.model.RoomQuizStatus
import com.benza.smartrooms.data.room.model.RoomQuizSummary
import com.benza.smartrooms.ui.components.AttachmentPreview
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.components.UserAvatar
import com.benza.smartrooms.ui.components.attachmentTypeLabel
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class RoomDetailPage(
    val labelRes: Int,
    val icon: ImageVector,
) {
    FEED(R.string.room_detail_nav_feed, Icons.Outlined.Campaign),
    QUIZZES(R.string.room_detail_nav_quizzes, Icons.Outlined.Quiz),
}

private enum class RoomDetailTopPanel {
    LEADERBOARD,
    MEMBERS,
}

@Composable
internal fun RoomDetailRouteScreen(
    roomId: String,
    roomName: String,
    roomTopic: String,
    roomCefrLevel: String,
    onBackClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onOpenPostClick: (String) -> Unit,
    onCommentPostClick: (String) -> Unit,
    onEditPostClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    onOpenQuizManagerClick: () -> Unit,
    onOpenInviteClick: () -> Unit,
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
        onOpenPostClick = onOpenPostClick,
        onCommentPostClick = onCommentPostClick,
        onEditPostClick = onEditPostClick,
        onOpenQuizClick = onOpenQuizClick,
        onOpenQuizManagerClick = onOpenQuizManagerClick,
        onOpenInviteClick = onOpenInviteClick,
        onRequestAnnouncementDeletionClick = viewModel::requestAnnouncementDeletion,
        onDismissAnnouncementDeletion = viewModel::dismissAnnouncementDeletion,
        onDeleteAnnouncementClick = viewModel::deleteAnnouncement,
        onRequestMemberRemovalClick = viewModel::requestMemberRemoval,
        onDismissMemberRemoval = viewModel::dismissMemberRemoval,
        onRemoveMemberClick = viewModel::removePendingMember,
        onInfoMessageShown = viewModel::consumeInfoMessage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoomDetailScreen(
    uiState: RoomDetailUiState,
    onBackClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onOpenPostClick: (String) -> Unit,
    onCommentPostClick: (String) -> Unit,
    onEditPostClick: (String) -> Unit,
    onOpenQuizClick: (String) -> Unit,
    onOpenQuizManagerClick: () -> Unit,
    onOpenInviteClick: () -> Unit,
    onRequestAnnouncementDeletionClick: (RoomAnnouncementCardUiState) -> Unit,
    onDismissAnnouncementDeletion: () -> Unit,
    onDeleteAnnouncementClick: () -> Unit,
    onRequestMemberRemovalClick: (RoomMemberUiState) -> Unit,
    onDismissMemberRemoval: () -> Unit,
    onRemoveMemberClick: () -> Unit,
    onInfoMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { RoomDetailPage.entries.size })
    val scope = rememberCoroutineScope()
    var isFeedActionsExpanded by remember { mutableStateOf(false) }
    var activeTopPanel by remember { mutableStateOf<RoomDetailTopPanel?>(null) }
    val topPanelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val infoMessage = uiState.infoMessageRes?.let { stringResource(it) }

    LaunchedEffect(infoMessage) {
        val message = infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onInfoMessageShown()
    }

    LaunchedEffect(pagerState.currentPage) {
        isFeedActionsExpanded = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (RoomDetailPage.entries[pagerState.currentPage]) {
                RoomDetailPage.FEED ->
                    FeedActionsFab(
                        expanded = isFeedActionsExpanded,
                        showInviteAction = uiState.isCurrentUserOwner,
                        onExpandedChange = { isFeedActionsExpanded = it },
                        onCreatePostClick = onCreatePostClick,
                        onInviteClick = onOpenInviteClick,
                    )

                RoomDetailPage.QUIZZES ->
                    if (uiState.canManageQuizzes) {
                        ExtendedFloatingActionButton(
                            onClick = onOpenQuizManagerClick,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Quiz,
                                    contentDescription = null,
                                )
                            },
                            text = { Text(stringResource(R.string.action_manage_quizzes)) },
                        )
                    }
            }
        },
        bottomBar = {
            NavigationBar {
                RoomDetailPage.entries.forEachIndexed { index, page ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(page.labelRes)) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RoomDetailTopBar(
                roomName = uiState.roomName,
                onBackClick = onBackClick,
                onLeaderboardClick = { activeTopPanel = RoomDetailTopPanel.LEADERBOARD },
                onMembersClick = { activeTopPanel = RoomDetailTopPanel.MEMBERS },
            )
            if (uiState.errorMessageRes != null) {
                AuthFeedbackBanner(
                    message = stringResource(uiState.errorMessageRes),
                    type = AuthFeedbackType.Error,
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (RoomDetailPage.entries[page]) {
                    RoomDetailPage.FEED ->
                        FeedPage(
                            announcements = uiState.announcements,
                            isLoading = uiState.isLoadingFeed,
                            onOpenAnnouncementClick = onOpenPostClick,
                            onCommentAnnouncementClick = onCommentPostClick,
                            onEditAnnouncementClick = onEditPostClick,
                            onDeleteAnnouncementClick = onRequestAnnouncementDeletionClick,
                        )

                    RoomDetailPage.QUIZZES ->
                        QuizzesPage(
                            quizzes = uiState.quizzes,
                            quizResultsByQuizId = uiState.quizResultsByQuizId,
                            isLoading = uiState.isLoadingQuizzes,
                            canManageQuizzes = uiState.canManageQuizzes,
                            onOpenQuizClick = onOpenQuizClick,
                        )
                }
            }
        }
    }

    if (uiState.pendingAnnouncementDeletion != null) {
        DeleteAnnouncementDialog(
            pendingDeletion = uiState.pendingAnnouncementDeletion,
            isDeleting = uiState.isDeletingAnnouncement,
            onDismiss = onDismissAnnouncementDeletion,
            onConfirm = onDeleteAnnouncementClick,
        )
    }

    if (uiState.pendingMemberRemoval != null) {
        RemoveMemberDialog(
            pendingRemoval = uiState.pendingMemberRemoval,
            isRemoving = uiState.removingMemberUserId != null,
            onDismiss = onDismissMemberRemoval,
            onConfirm = onRemoveMemberClick,
        )
    }

    if (activeTopPanel != null) {
        ModalBottomSheet(
            onDismissRequest = { activeTopPanel = null },
            sheetState = topPanelSheetState,
        ) {
            when (activeTopPanel) {
                RoomDetailTopPanel.LEADERBOARD ->
                    LeaderboardSheetContent(
                        rows = uiState.quizLeaderboardRows,
                        totalQuizCount = uiState.quizLeaderboardTotalQuizCount,
                        isLoading = uiState.isLoadingQuizLeaderboard,
                    )

                RoomDetailTopPanel.MEMBERS ->
                    MembersSheetContent(
                        members = uiState.members,
                        isLoading = uiState.isLoadingMembers,
                        removingMemberUserId = uiState.removingMemberUserId,
                        onRequestMemberRemovalClick = onRequestMemberRemovalClick,
                    )

                null -> Unit
            }
        }
    }
}

@Composable
private fun FeedActionsFab(
    expanded: Boolean,
    showInviteAction: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreatePostClick: () -> Unit,
    onInviteClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showInviteAction) {
                    SmallFabAction(
                        icon = Icons.Outlined.PersonAdd,
                        label = stringResource(R.string.action_invite_to_room),
                        onClick = {
                            onExpandedChange(false)
                            onInviteClick()
                        },
                    )
                }
                SmallFabAction(
                    icon = Icons.Outlined.Campaign,
                    label = stringResource(R.string.action_create_post),
                    onClick = {
                        onExpandedChange(false)
                        onCreatePostClick()
                    },
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            icon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = stringResource(R.string.room_detail_fab_actions),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (expanded) {
                            R.string.action_cancel
                        } else {
                            R.string.room_detail_fab_actions_short
                        },
                    ),
                )
            },
        )
    }
}

@Composable
private fun SmallFabAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        text = { Text(label) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun RoomDetailTopBar(
    roomName: String,
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onMembersClick: () -> Unit,
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
        Text(
            text = roomName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onLeaderboardClick) {
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = stringResource(R.string.room_detail_nav_leaderboard),
            )
        }
        IconButton(onClick = onMembersClick) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = stringResource(R.string.room_detail_nav_members),
            )
        }
    }
}

@Composable
private fun FeedPage(
    announcements: List<RoomAnnouncementCardUiState>,
    isLoading: Boolean,
    onOpenAnnouncementClick: (String) -> Unit,
    onCommentAnnouncementClick: (String) -> Unit,
    onEditAnnouncementClick: (String) -> Unit,
    onDeleteAnnouncementClick: (RoomAnnouncementCardUiState) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionIntro(
                title = stringResource(R.string.room_detail_feed_title),
                subtitle = stringResource(R.string.room_detail_feed_posts_subtitle),
            )
        }
        when {
            isLoading -> {
                item { FeedLoadingSkeleton() }
            }

            announcements.isEmpty() -> {
                item {
                    FeedPlaceholderCard(
                        text = stringResource(R.string.room_detail_feed_empty_posts),
                    )
                }
            }

            else -> {
                items(
                    items = announcements,
                    key = RoomAnnouncementCardUiState::id,
                ) { item ->
                    AnnouncementCard(
                        item = item,
                        onClick = { onOpenAnnouncementClick(item.id) },
                        onCommentClick = { onCommentAnnouncementClick(item.id) },
                        onEditClick = { onEditAnnouncementClick(item.id) },
                        onDeleteClick = { onDeleteAnnouncementClick(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizzesPage(
    quizzes: List<RoomQuizSummary>,
    quizResultsByQuizId: Map<String, RoomQuizResult>,
    isLoading: Boolean,
    canManageQuizzes: Boolean,
    onOpenQuizClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionIntro(
                title = stringResource(R.string.room_detail_quizzes_title),
                subtitle =
                    stringResource(
                        if (canManageQuizzes) {
                            R.string.room_detail_quizzes_subtitle_manager
                        } else {
                            R.string.room_detail_quizzes_subtitle_student
                        },
                    ),
            )
        }
        when {
            isLoading -> {
                item { QuizLoadingSkeleton() }
            }

            quizzes.isEmpty() -> {
                item {
                    FeedPlaceholderCard(
                        text =
                            stringResource(
                                if (canManageQuizzes) {
                                    R.string.room_detail_quizzes_empty_manager
                                } else {
                                    R.string.room_detail_quizzes_empty_student
                                },
                            ),
                    )
                }
            }

            else -> {
                items(
                    items = quizzes,
                    key = RoomQuizSummary::id,
                ) { quiz ->
                    QuizCard(
                        quiz = quiz,
                        result = quizResultsByQuizId[quiz.id],
                        onOpenQuizClick = onOpenQuizClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MembersSheetContent(
    members: List<RoomMemberUiState>,
    isLoading: Boolean,
    removingMemberUserId: String?,
    onRequestMemberRemovalClick: (RoomMemberUiState) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionIntro(
                title = stringResource(R.string.room_members_title),
                subtitle = stringResource(R.string.room_members_subtitle),
            )
        }
        when {
            isLoading -> {
                item { MembersLoadingSkeleton() }
            }

            members.isEmpty() -> {
                item {
                    FeedPlaceholderCard(text = stringResource(R.string.room_members_empty))
                }
            }

            else -> {
                items(
                    items = members,
                    key = RoomMemberUiState::userId,
                ) { member ->
                    RoomMemberCard(
                        member = member,
                        isRemoving = removingMemberUserId == member.userId,
                        onRequestMemberRemovalClick = { onRequestMemberRemovalClick(member) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomMemberCard(
    member: RoomMemberUiState,
    isRemoving: Boolean,
    onRequestMemberRemovalClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Lagoon.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                displayName = member.displayName,
                photoUrl = member.photoUrl,
                modifier = Modifier.size(52.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        if (member.isProfileLoaded && member.email.isNotBlank()) {
                            member.email
                        } else {
                            stringResource(R.string.room_members_profile_loading)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MemberRoleChip(
                        text = stringResource(member.roomRole.labelRes()),
                        isPrimary = member.roomRole == RoomMemberRole.OWNER,
                    )
                    member.accountRole?.let { accountRole ->
                        MemberRoleChip(
                            text = stringResource(accountRole.labelRes()),
                            isPrimary = false,
                        )
                    }
                }
            }
            if (member.canKick) {
                OutlinedButton(
                    onClick = onRequestMemberRemovalClick,
                    enabled = !isRemoving,
                ) {
                    Text(
                        text =
                            stringResource(
                                if (isRemoving) {
                                    R.string.action_removing_member
                                } else {
                                    R.string.action_kick_member
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRoleChip(
    text: String,
    isPrimary: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color =
            if (isPrimary) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
            },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color =
                if (isPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun MembersLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            FeedSkeletonCard(
                accentBrush =
                    Brush.horizontalGradient(
                        listOf(
                            Lagoon.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun LeaderboardSheetContent(
    rows: List<RoomQuizLeaderboardRowUiState>,
    totalQuizCount: Int,
    isLoading: Boolean,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionIntro(
                title = stringResource(R.string.room_quiz_leaderboard_title),
                subtitle = stringResource(R.string.room_quiz_leaderboard_subtitle),
            )
        }
        item {
            QuizLeaderboardCard(
                rows = rows,
                totalQuizCount = totalQuizCount,
                isLoading = isLoading,
            )
        }
    }
}

@Composable
private fun QuizLeaderboardCard(
    rows: List<RoomQuizLeaderboardRowUiState>,
    totalQuizCount: Int,
    isLoading: Boolean,
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
                        Brush.horizontalGradient(
                            listOf(
                                Coral.copy(alpha = 0.14f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.room_quiz_leaderboard_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        if (totalQuizCount > 0) {
                            pluralStringResource(
                                R.plurals.room_quiz_leaderboard_quiz_count,
                                totalQuizCount,
                                totalQuizCount,
                            )
                        } else {
                            stringResource(R.string.room_quiz_leaderboard_empty_quizzes)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                isLoading -> {
                    QuizLeaderboardLoadingRows()
                }

                totalQuizCount == 0 -> {
                    Text(
                        text = stringResource(R.string.room_quiz_leaderboard_empty_quizzes_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                rows.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.room_quiz_leaderboard_empty_students),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { row ->
                            QuizLeaderboardRow(row = row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizLeaderboardRow(row: RoomQuizLeaderboardRowUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.room_quiz_leaderboard_rank, row.rank),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                UserAvatar(
                    displayName = row.displayName,
                    photoUrl = row.photoUrl,
                    modifier = Modifier.size(44.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = row.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (row.email.isNotBlank()) {
                        Text(
                            text = row.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                LeaderboardScorePill(
                    score = row.score,
                    maxScore = row.maxScore,
                    isComplete = row.isComplete,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeaderboardStatusChip(isComplete = row.isComplete)
                Text(
                    text =
                        stringResource(
                            R.string.room_quiz_leaderboard_solved_value,
                            row.solvedQuizCount,
                            row.totalQuizCount,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LeaderboardScorePill(
    score: Int,
    maxScore: Int,
    isComplete: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color =
            if (isComplete) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            },
    ) {
        Text(
            text = stringResource(R.string.room_quiz_result_score_value, score, maxScore),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun LeaderboardStatusChip(isComplete: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color =
            if (isComplete) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
            },
    ) {
        Text(
            text =
                stringResource(
                    if (isComplete) {
                        R.string.room_quiz_leaderboard_complete
                    } else {
                        R.string.room_quiz_leaderboard_incomplete
                    },
                ),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color =
                if (isComplete) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
    }
}

@Composable
private fun QuizLeaderboardLoadingRows() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            SkeletonLine(
                widthFraction = 1f,
                height = 54.dp,
            )
        }
    }
}

@Composable
private fun SectionIntro(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnnouncementCard(
    item: RoomAnnouncementCardUiState,
    onClick: () -> Unit,
    onCommentClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var isMenuExpanded by remember(item.id) { mutableStateOf(false) }
    FeedCardShell(
        createdAt = item.createdAtEpochMillis.toRoomDateLabel(),
        backgroundColor = Lagoon.copy(alpha = 0.08f),
        authorName = item.authorName,
        onClick = onClick,
        actions =
            if (item.canManage) {
                {
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(
                            modifier = Modifier.size(36.dp),
                            onClick = { isMenuExpanded = true },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.room_detail_post_actions_menu),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.width(220.dp),
                            shape = MaterialTheme.shapes.large,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
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
                                colors =
                                    MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSurface,
                                        leadingIconColor = MaterialTheme.colorScheme.primary,
                                    ),
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
                                colors =
                                    MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.error,
                                        leadingIconColor = MaterialTheme.colorScheme.error,
                                    ),
                            )
                        }
                    }
                }
            } else {
                null
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.attachments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item.attachments.take(FEED_ATTACHMENT_PREVIEW_LIMIT).forEach { attachment ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                val hiddenAttachmentCount = item.attachments.size - FEED_ATTACHMENT_PREVIEW_LIMIT
                if (hiddenAttachmentCount > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text =
                                pluralStringResource(
                                    R.plurals.room_detail_more_attachments,
                                    hiddenAttachmentCount,
                                    hiddenAttachmentCount,
                                ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (item.latestComment != null) {
            LatestCommentPreview(preview = item.latestComment)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCommentClick) {
                Text(
                    text =
                        if (item.commentCount > 0) {
                            pluralStringResource(
                                R.plurals.action_comment_post_with_count,
                                item.commentCount,
                                item.commentCount,
                            )
                        } else {
                            stringResource(R.string.action_comment_post)
                        },
                )
            }
        }
    }
}

@Composable
private fun LatestCommentPreview(preview: RoomCommentPreviewUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.room_latest_comment_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    stringResource(
                        R.string.room_latest_comment_value,
                        preview.authorName,
                        preview.message,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuizCard(
    quiz: RoomQuizSummary,
    result: RoomQuizResult?,
    onOpenQuizClick: (String) -> Unit,
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.room_quizzes_topic_value, quiz.topic),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = quiz.createdAtEpochMillis.toRoomDateLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                QuizTimerIconBadge(hasTimer = quiz.hasTimer)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeedMetaCard(
                    modifier = Modifier.weight(1f),
                    value = stringResource(labelRes(quiz.quizKind)),
                    label = stringResource(R.string.room_quizzes_kind_title),
                )
                FeedMetaCard(
                    modifier = Modifier.weight(1f),
                    value =
                        quiz.cefrLevel.ifBlank {
                            stringResource(R.string.room_detail_level_not_set)
                        },
                    label = stringResource(R.string.room_detail_feed_level_metric),
                )
                FeedMetaCard(
                    modifier = Modifier.weight(1f),
                    value = stringResource(labelRes(quiz.questionType)),
                    label = stringResource(R.string.room_detail_question_type_title),
                )
                FeedMetaCard(
                    modifier = Modifier.weight(1f),
                    value = quiz.questionCount.toString(),
                    label = stringResource(R.string.room_detail_question_count_title),
                )
            }

            if (result != null) {
                val displayMaxScore = maxOf(result.maxScore, quiz.maxScore)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeedMetaCard(
                        modifier = Modifier.weight(1f),
                        value =
                            pluralStringResource(
                                R.plurals.room_quiz_result_progress_value,
                                result.answeredQuestionCount,
                                result.answeredQuestionCount,
                                result.questionCount,
                            ),
                        label = stringResource(R.string.room_quiz_result_progress_label),
                    )
                    FeedMetaCard(
                        modifier = Modifier.weight(1f),
                        value =
                            stringResource(
                                R.string.room_quiz_result_score_value,
                                result.score,
                                displayMaxScore,
                            ),
                        label = stringResource(R.string.room_quiz_result_score_label),
                    )
                }
            } else if (quiz.status == RoomQuizStatus.READY) {
                Button(
                    onClick = { onOpenQuizClick(quiz.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_solve_quiz))
                }
            }
        }
    }
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
        )
        FeedSkeletonCard(
            accentBrush =
                Brush.horizontalGradient(
                    listOf(
                        Lagoon.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
        )
    }
}

@Composable
private fun QuizLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FeedSkeletonCard(
            accentBrush =
                Brush.horizontalGradient(
                    listOf(
                        Coral.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
        )
        FeedSkeletonCard(
            accentBrush =
                Brush.horizontalGradient(
                    listOf(
                        Coral.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
        )
    }
}

@Composable
private fun FeedSkeletonCard(accentBrush: Brush) {
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
            SkeletonLine(
                widthFraction = 0.28f,
                height = 32.dp,
            )
            SkeletonLine(
                widthFraction = 0.58f,
                height = 20.dp,
            )
            SkeletonLine(
                widthFraction = 0.92f,
                height = 14.dp,
            )
            SkeletonLine(
                widthFraction = 0.76f,
                height = 14.dp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    SkeletonLine(
                        widthFraction = 1f,
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
private fun FeedCardShell(
    icon: ImageVector? = null,
    label: String? = null,
    createdAt: String,
    accentBrush: Brush? = null,
    backgroundColor: Color? = null,
    authorName: String? = null,
    onClick: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedBackgroundColor = backgroundColor ?: MaterialTheme.colorScheme.surface
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (accentBrush != null) {
                            Modifier.background(accentBrush)
                        } else {
                            Modifier.background(resolvedBackgroundColor)
                        },
                    ).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                if (!authorName.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = createdAt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        actions?.invoke()
                    }
                    PostAuthorHeader(authorName = authorName)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (icon != null && label != null) {
                            FeedLabelChip(
                                icon = icon,
                                label = label,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = createdAt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        actions?.invoke()
                    }
                }
                content()
            },
        )
    }
}

@Composable
private fun PostAuthorHeader(
    authorName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            displayName = authorName,
            photoUrl = null,
            modifier = Modifier.size(34.dp),
        )
        Text(
            text = authorName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FeedLabelChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
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
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FeedMetaCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f),
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
private fun QuizTimerIconBadge(
    hasTimer: Boolean,
    containerColor: Color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
    iconTint: Color =
        if (hasTimer) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
) {
    val label =
        if (hasTimer) {
            stringResource(R.string.room_quiz_timer_enabled_value)
        } else {
            stringResource(R.string.room_quiz_timer_none_value)
        }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
    ) {
        Icon(
            imageVector =
                if (hasTimer) {
                    Icons.Outlined.Timer
                } else {
                    Icons.Outlined.TimerOff
                },
            contentDescription = label,
            modifier =
                Modifier
                    .padding(8.dp)
                    .size(18.dp),
            tint = iconTint,
        )
    }
}

@Composable
private fun DeleteAnnouncementDialog(
    pendingDeletion: PendingAnnouncementDeletion,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_post_dialog_title)) },
        text = {
            Text(
                text =
                    stringResource(
                        R.string.delete_post_dialog_message,
                        pendingDeletion.announcementTitle,
                    ),
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
            ) {
                Text(stringResource(R.string.action_delete_post))
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
private fun RemoveMemberDialog(
    pendingRemoval: PendingMemberRemoval,
    isRemoving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_member_dialog_title)) },
        text = {
            Text(
                text =
                    stringResource(
                        R.string.remove_member_dialog_message,
                        pendingRemoval.displayName,
                    ),
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isRemoving,
            ) {
                Text(stringResource(R.string.action_kick_member))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRemoving,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private const val FEED_ATTACHMENT_PREVIEW_LIMIT = 3

private fun RoomMemberRole.labelRes(): Int =
    when (this) {
        RoomMemberRole.OWNER -> R.string.room_member_role_owner
        RoomMemberRole.COLLABORATOR -> R.string.room_member_role_collaborator
        RoomMemberRole.MEMBER -> R.string.room_member_role_member
    }

private fun RoomMemberAccountRole.labelRes(): Int =
    when (this) {
        RoomMemberAccountRole.TEACHER -> R.string.profile_role_professor
        RoomMemberAccountRole.STUDENT -> R.string.profile_role_student
    }

private fun labelRes(quizKind: QuizKind): Int =
    when (quizKind) {
        QuizKind.GRAMMAR -> R.string.room_quiz_kind_grammar
        QuizKind.VOCABULARY -> R.string.room_quiz_kind_vocabulary
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
                    cefrLevel = "B2",
                    isCurrentUserOwner = true,
                    announcements =
                        listOf(
                            RoomAnnouncementCardUiState(
                                id = "announcement-1",
                                createdAtEpochMillis = System.currentTimeMillis(),
                                title = "Homework",
                                message = "Review unit 3 and prepare five new travel expressions.",
                                authorName = "Prof. Benza",
                                attachments = emptyList(),
                                commentCount = 0,
                                latestComment = null,
                                canManage = true,
                            ),
                        ),
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
                                createdAtEpochMillis = System.currentTimeMillis() - 3_600_000L,
                            ),
                        ),
                    isLoadingRoom = false,
                    isLoadingFeed = false,
                    isLoadingQuizzes = false,
                ),
            onBackClick = {},
            onCreatePostClick = {},
            onOpenPostClick = {},
            onCommentPostClick = {},
            onEditPostClick = {},
            onOpenQuizClick = {},
            onOpenQuizManagerClick = {},
            onOpenInviteClick = {},
            onRequestAnnouncementDeletionClick = {},
            onDismissAnnouncementDeletion = {},
            onDeleteAnnouncementClick = {},
            onRequestMemberRemovalClick = {},
            onDismissMemberRemoval = {},
            onRemoveMemberClick = {},
            onInfoMessageShown = {},
        )
    }
}
