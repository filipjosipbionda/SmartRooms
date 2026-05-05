package com.benza.smartrooms.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.room.model.RoomInvitationAccess
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Route-level home entry that binds [HomeViewModel] to the stateless home screen.
 */
@Composable
internal fun HomeRouteScreen(
    onProfileClick: () -> Unit,
    onCreateRoomClick: () -> Unit,
    onRoomClick: (HomeRoomUiState) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState.value,
        onProfileClick = onProfileClick,
        onCreateRoomClick = onCreateRoomClick,
        onRoomClick = onRoomClick,
        onAcceptInvitationClick = viewModel::acceptInvitation,
        onRejectInvitationClick = viewModel::rejectInvitation,
        onPreviousRoomsPageClick = viewModel::goToPreviousRoomsPage,
        onNextRoomsPageClick = viewModel::goToNextRoomsPage,
    )
}

/**
 * Stateless home UI shown after authentication succeeds.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onProfileClick: () -> Unit,
    onCreateRoomClick: () -> Unit,
    onRoomClick: (HomeRoomUiState) -> Unit,
    onAcceptInvitationClick: (String) -> Unit,
    onRejectInvitationClick: (String) -> Unit,
    onPreviousRoomsPageClick: () -> Unit,
    onNextRoomsPageClick: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoomClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_create_room),
                )
            }
        },
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(it),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    HomeHeader(
                        profileInitials = uiState.profileInitials,
                        onProfileClick = onProfileClick,
                    )
                }
            }
            item {
                InvitationCarousel(
                    invitations = uiState.pendingInvitations,
                    processingInvitationIds = uiState.processingInvitationIds,
                    onAcceptClick = onAcceptInvitationClick,
                    onRejectClick = onRejectInvitationClick,
                )
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (uiState.errorMessageRes != null) {
                        AuthFeedbackBanner(
                            message = stringResource(uiState.errorMessageRes),
                            type = AuthFeedbackType.Error,
                        )
                    } else if (uiState.infoMessageRes != null) {
                        AuthFeedbackBanner(
                            message = stringResource(uiState.infoMessageRes),
                            type = AuthFeedbackType.Success,
                        )
                    }
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    JoinedRoomsSection(
                        title = stringResource(R.string.home_joined_rooms_title),
                        subtitle = stringResource(R.string.home_joined_rooms_subtitle),
                    ) {
                        when {
                            uiState.isLoadingRooms -> {
                                Text(
                                    text = stringResource(R.string.home_rooms_loading),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }

                            uiState.rooms.isEmpty() -> {
                                Text(
                                    text = stringResource(R.string.home_rooms_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            else -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    uiState.pagedRooms.forEach { room ->
                                        RoomCard(
                                            room = room,
                                            onOpenRoomClick = { onRoomClick(room) },
                                        )
                                    }
                                    if (uiState.totalPages > 1) {
                                        RoomsPagination(
                                            currentPage = uiState.currentPage,
                                            totalPages = uiState.totalPages,
                                            onPreviousClick = onPreviousRoomsPageClick,
                                            onNextClick = onNextRoomsPageClick,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

/**
 * Header section for the home screen.
 */
@Composable
private fun HomeHeader(
    profileInitials: String,
    onProfileClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_brand_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
            )
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = onProfileClick,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                brush =
                                    Brush.linearGradient(
                                        colors = listOf(Lagoon, Coral),
                                    ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profileInitials,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationCarousel(
    invitations: List<HomeInvitationUiState>,
    processingInvitationIds: Set<String>,
    onAcceptClick: (String) -> Unit,
    onRejectClick: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_invites_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.home_invites_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
            ) {
                Text(
                    text = invitations.size.coerceAtMost(99).toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
        if (invitations.isEmpty()) {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = stringResource(R.string.room_invitations_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                    ),
            ) {
                items(invitations, key = HomeInvitationUiState::id) { invitation ->
                    InvitationCard(
                        invitation = invitation,
                        isProcessing = invitation.id in processingInvitationIds,
                        onAcceptClick = { onAcceptClick(invitation.id) },
                        onRejectClick = { onRejectClick(invitation.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: HomeInvitationUiState,
    isProcessing: Boolean,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(292.dp),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Coral.copy(alpha = 0.12f),
                                        MaterialTheme.colorScheme.surface,
                                        Lagoon.copy(alpha = 0.10f),
                                    ),
                            ),
                    ).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = invitation.roomName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color =
                        if (invitation.access == RoomInvitationAccess.COLLABORATOR) {
                            Coral.copy(alpha = 0.18f)
                        } else {
                            Lagoon.copy(alpha = 0.18f)
                        },
                ) {
                    Text(
                        text = stringResource(invitation.access.labelRes()),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (invitation.access == RoomInvitationAccess.COLLABORATOR) Coral else Lagoon,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = stringResource(R.string.room_invitation_from, invitation.inviterName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRejectClick,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_reject_invite))
                }
                Button(
                    onClick = onAcceptClick,
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(stringResource(R.string.action_accept_invite))
                }
            }
        }
    }
}

private fun RoomInvitationAccess.labelRes(): Int =
    when (this) {
        RoomInvitationAccess.COLLABORATOR -> R.string.room_invite_access_collaborator
        RoomInvitationAccess.MEMBER -> R.string.room_invite_access_member
    }

/**
 * Full-width joined rooms section header and content.
 */
@Composable
private fun JoinedRoomsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/**
 * Room card rendered from Firestore data.
 */
@Composable
private fun RoomCard(
    room: HomeRoomUiState,
    onOpenRoomClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = room.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    RoomRoleBadge(role = room.role)
                }
                Text(
                    text = stringResource(R.string.home_room_topic, room.topic),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.home_room_teacher, room.ownerName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onOpenRoomClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_open_room))
            }
        }
    }
}

@Composable
private fun RoomRoleBadge(role: HomeRoomRole) {
    val labelRes =
        when (role) {
            HomeRoomRole.OWNER -> R.string.home_room_role_owner
            HomeRoomRole.COLLABORATOR -> R.string.home_room_role_collaborator
            HomeRoomRole.MEMBER -> R.string.home_room_role_member
        }
    val containerColor =
        when (role) {
            HomeRoomRole.OWNER -> Lagoon.copy(alpha = 0.22f)
            HomeRoomRole.COLLABORATOR -> Coral.copy(alpha = 0.20f)
            HomeRoomRole.MEMBER -> MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        }
    val contentColor =
        when (role) {
            HomeRoomRole.OWNER -> Lagoon
            HomeRoomRole.COLLABORATOR -> Coral
            HomeRoomRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

/**
 * Pagination controls for joined rooms.
 */
@Composable
private fun RoomsPagination(
    currentPage: Int,
    totalPages: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onPreviousClick,
            enabled = currentPage > 0,
        ) {
            Text(stringResource(R.string.action_previous))
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_pagination_indicator, currentPage + 1, totalPages),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onNextClick,
            enabled = currentPage < totalPages - 1,
        ) {
            Text(stringResource(R.string.action_next))
        }
    }
}

/**
 * Preview for the home screen.
 */
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SmartRoomsTheme {
        HomeScreen(
            uiState =
                HomeUiState(
                    profileInitials = "FB",
                    joinedRoomsCount = 6,
                    unansweredQuizCount = 7,
                    rooms =
                        listOf(
                            HomeRoomUiState(
                                id = "1",
                                name = "Filip Room 1",
                                topic = "Firebase",
                                participantCount = 1,
                                ownerName = "Filip",
                                unansweredQuizCount = 1,
                            ),
                            HomeRoomUiState(
                                id = "2",
                                name = "Filip Room 2",
                                topic = "Kotlin",
                                participantCount = 4,
                                ownerName = "Ana",
                                unansweredQuizCount = 0,
                            ),
                            HomeRoomUiState(
                                id = "3",
                                name = "Filip Room 3",
                                topic = "Databases",
                                participantCount = 5,
                                ownerName = "Marko",
                                unansweredQuizCount = 2,
                            ),
                            HomeRoomUiState(
                                id = "4",
                                name = "Filip Room 4",
                                topic = "Compose",
                                participantCount = 3,
                                ownerName = "Ivana",
                                unansweredQuizCount = 1,
                            ),
                            HomeRoomUiState(
                                id = "5",
                                name = "Filip Room 5",
                                topic = "AI",
                                participantCount = 2,
                                ownerName = "Petra",
                                unansweredQuizCount = 3,
                            ),
                            HomeRoomUiState(
                                id = "6",
                                name = "Filip Room 6",
                                topic = "Testing",
                                participantCount = 6,
                                ownerName = "Luka",
                                unansweredQuizCount = 0,
                            ),
                        ),
                    pagedRooms =
                        listOf(
                            HomeRoomUiState(
                                id = "1",
                                name = "Filip Room 1",
                                topic = "Firebase",
                                participantCount = 1,
                                ownerName = "Filip",
                                unansweredQuizCount = 1,
                            ),
                            HomeRoomUiState(
                                id = "2",
                                name = "Filip Room 2",
                                topic = "Kotlin",
                                participantCount = 4,
                                ownerName = "Ana",
                                unansweredQuizCount = 0,
                            ),
                            HomeRoomUiState(
                                id = "3",
                                name = "Filip Room 3",
                                topic = "Databases",
                                participantCount = 5,
                                ownerName = "Marko",
                                unansweredQuizCount = 2,
                            ),
                            HomeRoomUiState(
                                id = "4",
                                name = "Filip Room 4",
                                topic = "Compose",
                                participantCount = 3,
                                ownerName = "Ivana",
                                unansweredQuizCount = 1,
                            ),
                        ),
                    totalPages = 2,
                    pendingInvitations =
                        listOf(
                            HomeInvitationUiState(
                                id = "invite-1",
                                roomName = "Speaking Lab",
                                inviterName = "Prof. Benza",
                                access = RoomInvitationAccess.MEMBER,
                                createdAtEpochMillis = 0L,
                            ),
                            HomeInvitationUiState(
                                id = "invite-2",
                                roomName = "AI Workshop",
                                inviterName = "Filip",
                                access = RoomInvitationAccess.COLLABORATOR,
                                createdAtEpochMillis = 0L,
                            ),
                        ),
                    isLoadingRooms = false,
                ),
            onProfileClick = {},
            onCreateRoomClick = {},
            onRoomClick = {},
            onAcceptInvitationClick = {},
            onRejectInvitationClick = {},
            onPreviousRoomsPageClick = {},
            onNextRoomsPageClick = {},
        )
    }
}
