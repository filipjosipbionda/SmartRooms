package com.benza.smartrooms.feature.auth.roleselection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.data.userprofile.model.TeacherApprovalStatus
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.components.AuthScaffold
import com.benza.smartrooms.ui.theme.Coral
import com.benza.smartrooms.ui.theme.Lagoon
import com.benza.smartrooms.ui.theme.SmartRoomsTheme
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun RoleSelectionRouteScreen(
    onRoleSaved: () -> Unit,
    onLogout: () -> Unit,
    viewModel: RoleSelectionViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RoleSelectionEvent.NavigateToHome -> onRoleSaved()
                RoleSelectionEvent.NavigateToLogin -> onLogout()
            }
        }
    }

    RoleSelectionScreen(
        uiState = uiState.value,
        onTeacherRequestClick = viewModel::submitTeacherRequest,
        onStudentClick = viewModel::selectStudentRole,
        onLogoutClick = viewModel::logout,
    )
}

@Composable
internal fun RoleSelectionScreen(
    uiState: RoleSelectionUiState,
    onTeacherRequestClick: () -> Unit,
    onStudentClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.role_selection_title),
        subtitle =
            stringResource(
                R.string.role_selection_subtitle,
                uiState.displayName.ifBlank { uiState.email },
            ),
    ) {
        if (uiState.errorMessageRes != null) {
            AuthFeedbackBanner(
                message = stringResource(uiState.errorMessageRes),
                type = AuthFeedbackType.Error,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isRefreshing) {
            CircularProgressIndicator()
        } else {
            when (uiState.teacherApprovalStatus) {
                TeacherApprovalStatus.PENDING -> {
                    PendingTeacherApprovalCard()
                }

                TeacherApprovalStatus.REJECTED -> {
                    AuthFeedbackBanner(
                        message = stringResource(R.string.role_teacher_rejected_message),
                        type = AuthFeedbackType.Error,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RoleOptions(
                        isLoading = uiState.isLoading,
                        onTeacherRequestClick = onTeacherRequestClick,
                        onStudentClick = onStudentClick,
                    )
                }

                else -> {
                    RoleOptions(
                        isLoading = uiState.isLoading,
                        onTeacherRequestClick = onTeacherRequestClick,
                        onStudentClick = onStudentClick,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onLogoutClick,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.action_log_out))
        }
    }
}

@Composable
private fun RoleOptions(
    isLoading: Boolean,
    onTeacherRequestClick: () -> Unit,
    onStudentClick: () -> Unit,
) {
    RoleOptionCard(
        title = stringResource(R.string.role_teacher_title),
        description = stringResource(R.string.role_teacher_request_description),
        accentBrush =
            Brush.linearGradient(
                colors =
                    listOf(
                        Lagoon.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surface,
                    ),
            ),
        buttonText = stringResource(R.string.action_request_teacher_access),
        enabled = !isLoading,
        onClick = onTeacherRequestClick,
    )
    Spacer(modifier = Modifier.height(14.dp))
    RoleOptionCard(
        title = stringResource(R.string.role_student_title),
        description = stringResource(R.string.role_student_description),
        accentBrush =
            Brush.linearGradient(
                colors =
                    listOf(
                        Coral.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surface,
                    ),
            ),
        buttonText = stringResource(R.string.action_continue_as_student),
        enabled = !isLoading,
        onClick = onStudentClick,
    )
}

@Composable
private fun PendingTeacherApprovalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                                    Lagoon.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                        ),
                    ).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.role_teacher_pending_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.role_teacher_pending_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            Text(
                text = stringResource(R.string.role_teacher_pending_live_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    description: String,
    accentBrush: Brush,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(accentBrush)
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoleSelectionScreenPreview() {
    SmartRoomsTheme {
        RoleSelectionScreen(
            uiState =
                RoleSelectionUiState(
                    displayName = "Filip",
                    email = "filip@example.com",
                ),
            onTeacherRequestClick = {},
            onStudentClick = {},
            onLogoutClick = {},
        )
    }
}
