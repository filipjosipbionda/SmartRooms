package com.benza.smartrooms.feature.profile

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.userprofile.model.TeacherApprovalStatus
import com.benza.smartrooms.data.userprofile.model.UserRole
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import org.koin.androidx.compose.koinViewModel

/**
 * Route-level profile entry that binds [ProfileViewModel] to the stateless screen.
 */
@Composable
internal fun ProfileRouteScreen(
    onBackClick: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onLogoutClick = {
            viewModel.logout()
            onLoggedOut()
        },
    )
}

/**
 * Stateless profile UI.
 */
@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ProfileTopBar(onBackClick = onBackClick)
        }
        item {
            ProfileHeroCard(uiState = uiState)
        }
        if (uiState.errorMessageRes != null) {
            item {
                AuthFeedbackBanner(
                    message = stringResource(uiState.errorMessageRes),
                    type = AuthFeedbackType.Error,
                )
            }
        }
        item {
            ProfileRoleCard(uiState = uiState)
        }
        item {
            ProfileRoomStatsCard(uiState = uiState)
        }
        item {
            ProfileAccountCard(onLogoutClick = onLogoutClick)
        }
    }
}

@Composable
private fun ProfileTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun ProfileHeroCard(uiState: ProfileUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(88.dp)
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
                    text = uiState.initials,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = uiState.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (uiState.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = uiState.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileRoleCard(uiState: ProfileUiState) {
    ProfileSectionCard {
        Text(
            text = stringResource(R.string.profile_role_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(14.dp))
        ProfileInfoSurface(
            label = stringResource(R.string.profile_role_label),
            value =
                when (uiState.role) {
                    UserRole.TEACHER -> stringResource(R.string.profile_role_professor)
                    UserRole.STUDENT -> stringResource(R.string.profile_role_student)
                    null -> stringResource(R.string.profile_role_not_selected)
                },
            loading = uiState.isLoadingProfile,
        )
        if (uiState.role == UserRole.TEACHER || uiState.teacherApprovalStatus != TeacherApprovalStatus.NONE) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileInfoSurface(
                label = stringResource(R.string.profile_teacher_status_label),
                value =
                    when (uiState.teacherApprovalStatus) {
                        TeacherApprovalStatus.APPROVED -> stringResource(R.string.profile_teacher_status_approved)
                        TeacherApprovalStatus.PENDING -> stringResource(R.string.profile_teacher_status_pending)
                        TeacherApprovalStatus.REJECTED -> stringResource(R.string.profile_teacher_status_rejected)
                        TeacherApprovalStatus.NONE -> stringResource(R.string.profile_teacher_status_none)
                    },
                loading = uiState.isLoadingProfile,
            )
        }
    }
}

@Composable
private fun ProfileRoomStatsCard(uiState: ProfileUiState) {
    ProfileSectionCard {
        Text(
            text = stringResource(R.string.profile_rooms_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (uiState.role == UserRole.TEACHER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileInfoSurface(
                    label = stringResource(R.string.profile_rooms_owned_count),
                    value = uiState.ownedRoomCount.toString(),
                    loading = uiState.isLoadingRooms,
                    modifier = Modifier.weight(1f),
                )
                ProfileInfoSurface(
                    label = stringResource(R.string.profile_rooms_collaborating_count),
                    value = uiState.collaboratingRoomCount.toString(),
                    loading = uiState.isLoadingRooms,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            ProfileInfoSurface(
                label = stringResource(R.string.profile_rooms_member_count),
                value = uiState.memberRoomCount.toString(),
                loading = uiState.isLoadingRooms,
            )
        }
    }
}

@Composable
private fun ProfileAccountCard(onLogoutClick: () -> Unit) {
    ProfileSectionCard {
        Text(
            text = stringResource(R.string.profile_account_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_account_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_log_out))
        }
    }
}

@Composable
private fun ProfileSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            content = content,
        )
    }
}

@Composable
private fun ProfileInfoSurface(
    label: String,
    value: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 82.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
