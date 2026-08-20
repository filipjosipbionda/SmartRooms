package com.benza.smartrooms.feature.createpost

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benza.smartrooms.R
import com.benza.smartrooms.ui.components.AttachmentPreview
import com.benza.smartrooms.ui.components.AuthFeedbackBanner
import com.benza.smartrooms.ui.components.AuthFeedbackType
import com.benza.smartrooms.ui.components.UserAvatar
import com.benza.smartrooms.ui.components.attachmentTypeLabel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun CreatePostRouteScreen(
    roomId: String,
    roomName: String,
    announcementId: String?,
    onBackClick: () -> Unit,
    onPostCompleted: () -> Unit,
    viewModel: CreatePostViewModel =
        koinViewModel(
            parameters = { parametersOf(roomId, roomName, announcementId, false) },
        ),
) {
    CreatePostRouteContent(
        viewModel = viewModel,
        onBackClick = onBackClick,
        onPostCompleted = onPostCompleted,
    )
}

@Composable
internal fun PostDetailRouteScreen(
    roomId: String,
    roomName: String,
    announcementId: String,
    focusComments: Boolean,
    onBackClick: () -> Unit,
    viewModel: CreatePostViewModel =
        koinViewModel(
            parameters = { parametersOf(roomId, roomName, announcementId, true) },
        ),
) {
    CreatePostRouteContent(
        viewModel = viewModel,
        focusComments = focusComments,
        onBackClick = onBackClick,
        onPostCompleted = onBackClick,
    )
}

@Composable
private fun CreatePostRouteContent(
    viewModel: CreatePostViewModel,
    focusComments: Boolean = false,
    onBackClick: () -> Unit,
    onPostCompleted: () -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val handlePickedUris: (List<Uri>) -> Unit = handle@{ uris ->
        if (uris.isEmpty()) return@handle

        val resolvedAttachments = mutableListOf<CreatePostAttachmentUiState>()
        val failedAttachmentNames = mutableListOf<String>()
        uris.forEach { uri ->
            runCatching {
                contentResolver.resolveAttachment(uri)
            }.onSuccess(resolvedAttachments::add)
                .onFailure {
                    failedAttachmentNames += uri.readableAttachmentName()
                }
        }

        viewModel.addAttachments(resolvedAttachments)
        if (failedAttachmentNames.isNotEmpty()) {
            viewModel.onAttachmentSelectionFailed(
                buildAttachmentSelectionErrorMessage(
                    failedAttachmentNames = failedAttachmentNames,
                    allAttachmentsFailed = resolvedAttachments.isEmpty(),
                ),
            )
        }
    }
    val attachmentsPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            handlePickedUris(uris)
        }
    val mediaPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            handlePickedUris(uris)
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CreatePostEvent.PostCompleted -> onPostCompleted()
            }
        }
    }

    CreatePostScreen(
        uiState = uiState.value,
        onBackClick = onBackClick,
        onTitleChanged = viewModel::onTitleChanged,
        onMessageChanged = viewModel::onMessageChanged,
        onPickFilesClick = { attachmentsPicker.launch(arrayOf("*/*")) },
        onPickPhotosClick = { mediaPicker.launch("image/*") },
        onRemoveAttachmentClick = viewModel::removeAttachment,
        onOpenAttachmentClick = { attachment ->
            context.openAttachment(attachment)
        },
        onDownloadAttachmentClick = { attachment ->
            context.downloadAttachment(attachment)
        },
        onSubmitClick = viewModel::submitPost,
        onDeleteClick = viewModel::deletePost,
        focusComments = focusComments,
        onCommentChanged = viewModel::onCommentChanged,
        onSubmitCommentClick = viewModel::submitComment,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatePostScreen(
    uiState: CreatePostUiState,
    onBackClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onPickFilesClick: () -> Unit,
    onPickPhotosClick: () -> Unit,
    onRemoveAttachmentClick: (String) -> Unit,
    onOpenAttachmentClick: (CreatePostAttachmentUiState) -> Unit,
    onDownloadAttachmentClick: (CreatePostAttachmentUiState) -> Unit,
    onSubmitClick: () -> Unit,
    onDeleteClick: () -> Unit,
    focusComments: Boolean,
    onCommentChanged: (String) -> Unit,
    onSubmitCommentClick: () -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var isPickerSheetOpen by remember { mutableStateOf(false) }
    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    val errorBannerMessage =
        uiState.errorMessageText
            ?: uiState.errorMessageRes?.let { stringResource(it) }
    val isWorking = uiState.isSubmittingPost || uiState.isDeletingPost
    val isInputEnabled = uiState.canEdit && !uiState.isLoadingPost && !isWorking
    val isViewMode = uiState.mode == CreatePostMode.VIEW
    val screenTitleRes =
        when (uiState.mode) {
            CreatePostMode.CREATE -> R.string.create_post_screen_title
            CreatePostMode.EDIT -> R.string.edit_post_screen_title
            CreatePostMode.VIEW -> R.string.post_detail_screen_title
        }
    val screenSubtitleRes =
        when (uiState.mode) {
            CreatePostMode.CREATE -> R.string.create_post_screen_subtitle
            CreatePostMode.EDIT -> R.string.edit_post_screen_subtitle
            CreatePostMode.VIEW -> R.string.post_detail_screen_subtitle
        }
    val submitLabelRes =
        when (uiState.mode) {
            CreatePostMode.CREATE -> R.string.action_publish_post
            CreatePostMode.EDIT -> R.string.action_save_post
            CreatePostMode.VIEW -> R.string.action_save_post
        }

    LaunchedEffect(focusComments, isViewMode, uiState.isLoadingPost) {
        if (focusComments && isViewMode && !uiState.isLoadingPost) {
            listState.animateScrollToItem(COMMENTS_SECTION_ITEM_INDEX)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
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
                            text = stringResource(screenTitleRes),
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text =
                                buildAnnotatedString {
                                    append(stringResource(R.string.create_post_room_prefix))
                                    append(" ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                        append(uiState.roomName)
                                    }
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (uiState.isLoadingPost) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    text = stringResource(R.string.edit_post_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        } else if (!isViewMode) {
                            Text(
                                text = stringResource(screenSubtitleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isViewMode) {
                            Text(
                                text = uiState.titleInput,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = uiState.messageInput,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            OutlinedTextField(
                                value = uiState.titleInput,
                                onValueChange = onTitleChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_post_title)) },
                                isError = uiState.titleErrorRes != null,
                                singleLine = true,
                                enabled = isInputEnabled,
                            )
                            OutlinedTextField(
                                value = uiState.messageInput,
                                onValueChange = onMessageChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.label_post_message)) },
                                isError = uiState.messageErrorRes != null,
                                minLines = 6,
                                enabled = isInputEnabled,
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.create_post_attachments_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!isViewMode) {
                            Text(
                                text = stringResource(R.string.create_post_attachments_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!isViewMode) {
                            OutlinedButton(
                                onClick = { isPickerSheetOpen = true },
                                enabled = isInputEnabled,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = stringResource(R.string.action_add_files),
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                        if (uiState.attachments.isEmpty()) {
                            Text(
                                text = stringResource(R.string.create_post_attachments_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                uiState.attachments.forEach { attachment ->
                                    Surface(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isViewMode && !attachment.downloadUrl.isNullOrBlank()) {
                                                        Modifier.clickable {
                                                            onOpenAttachmentClick(attachment)
                                                        }
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                        shape = MaterialTheme.shapes.large,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            AttachmentPreview(
                                                modifier = Modifier.size(56.dp),
                                                model = attachment.uriString ?: attachment.downloadUrl,
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
                                                    text =
                                                        "${attachmentTypeLabel(
                                                            attachment.name,
                                                            attachment.mimeType,
                                                        )} • " +
                                                            Formatter.formatShortFileSize(
                                                                context,
                                                                attachment.sizeBytes,
                                                            ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                )
                                            }
                                            if (isViewMode && !attachment.downloadUrl.isNullOrBlank()) {
                                                IconButton(
                                                    onClick = { onDownloadAttachmentClick(attachment) },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Download,
                                                        contentDescription =
                                                            stringResource(
                                                                R.string.attachment_download_action,
                                                                attachment.name,
                                                            ),
                                                    )
                                                }
                                            } else if (!isViewMode) {
                                                IconButton(
                                                    onClick = { onRemoveAttachmentClick(attachment.id) },
                                                    enabled = isInputEnabled,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription =
                                                            stringResource(
                                                                R.string.create_post_attachment_remove,
                                                            ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (errorBannerMessage != null) {
                item {
                    AuthFeedbackBanner(
                        message = errorBannerMessage,
                        type = AuthFeedbackType.Error,
                    )
                }
            }
            if (isViewMode) {
                item {
                    CommentsSection(
                        comments = uiState.comments,
                        commentInput = uiState.commentInput,
                        isLoadingComments = uiState.isLoadingComments,
                        isSubmittingComment = uiState.isSubmittingComment,
                        commentErrorRes = uiState.commentErrorRes,
                        focusComments = focusComments,
                        onCommentChanged = onCommentChanged,
                        onSubmitCommentClick = onSubmitCommentClick,
                    )
                }
            }
            if (!isViewMode) {
                item {
                    Button(
                        onClick = onSubmitClick,
                        enabled = isInputEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isSubmittingPost) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = stringResource(submitLabelRes),
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        } else {
                            Text(stringResource(submitLabelRes))
                        }
                    }
                }
            }
            if (uiState.mode == CreatePostMode.EDIT && uiState.canEdit) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.edit_post_delete_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.edit_post_delete_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { isDeleteDialogOpen = true },
                                enabled = !uiState.isLoadingPost && !isWorking,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (uiState.isDeletingPost) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                    Text(
                                        text = stringResource(R.string.action_delete_post),
                                        modifier = Modifier.padding(start = 10.dp),
                                    )
                                } else {
                                    Text(stringResource(R.string.action_delete_post))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isPickerSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isPickerSheetOpen = false },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.create_post_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.create_post_picker_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PickerOptionCard(
                    icon = Icons.Outlined.Folder,
                    title = stringResource(R.string.create_post_picker_files_title),
                    subtitle = stringResource(R.string.create_post_picker_files_subtitle),
                    onClick = {
                        isPickerSheetOpen = false
                        onPickFilesClick()
                    },
                )
                PickerOptionCard(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = stringResource(R.string.create_post_picker_photos_title),
                    subtitle = stringResource(R.string.create_post_picker_photos_subtitle),
                    onClick = {
                        isPickerSheetOpen = false
                        onPickPhotosClick()
                    },
                )
            }
        }
    }

    if (isDeleteDialogOpen) {
        Dialog(onDismissRequest = { isDeleteDialogOpen = false }) {
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
                        text = stringResource(R.string.delete_post_edit_screen_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { isDeleteDialogOpen = false },
                            enabled = !isWorking,
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Button(
                            onClick = {
                                isDeleteDialogOpen = false
                                onDeleteClick()
                            },
                            enabled = !isWorking,
                        ) {
                            Text(stringResource(R.string.action_delete_post))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentsSection(
    comments: List<RoomCommentUiState>,
    commentInput: String,
    isLoadingComments: Boolean,
    isSubmittingComment: Boolean,
    commentErrorRes: Int?,
    focusComments: Boolean,
    onCommentChanged: (String) -> Unit,
    onSubmitCommentClick: () -> Unit,
) {
    val commentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(focusComments) {
        if (focusComments) {
            commentFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = onCommentChanged,
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(commentFocusRequester),
                    label = { Text(stringResource(R.string.label_comment)) },
                    placeholder = { Text(stringResource(R.string.comment_input_placeholder)) },
                    minLines = 2,
                    maxLines = 5,
                    isError = commentErrorRes != null,
                    enabled = !isSubmittingComment,
                )
                Button(
                    onClick = onSubmitCommentClick,
                    enabled = !isSubmittingComment,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    if (isSubmittingComment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = stringResource(R.string.action_post_comment),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (commentErrorRes != null) {
                Text(
                    text = stringResource(commentErrorRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            when {
                isLoadingComments -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(R.string.comments_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                comments.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.comments_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        comments.forEach { comment ->
                            CommentCard(comment = comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: RoomCommentUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            UserAvatar(
                displayName = comment.authorName,
                photoUrl = comment.authorPhotoUrl,
                modifier = Modifier.size(38.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = comment.authorName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = comment.createdAtEpochMillis.toCommentDateLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    text = comment.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun ContentResolver.resolveAttachment(uri: Uri): CreatePostAttachmentUiState {
    try {
        takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
    } catch (_: UnsupportedOperationException) {
    }

    openFileDescriptor(uri, "r")?.use { }

    val attachmentFromQuery =
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else uri.lastPathSegment
                    val sizeBytes = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    CreatePostAttachmentUiState(
                        id = uri.toString(),
                        name = displayName.orEmpty().ifBlank { "attachment" },
                        mimeType = getType(uri).orEmpty().ifBlank { "application/octet-stream" },
                        sizeBytes = sizeBytes,
                        uriString = uri.toString(),
                    )
                } else {
                    null
                }
            }

    return attachmentFromQuery
        ?: CreatePostAttachmentUiState(
            id = uri.toString(),
            name = uri.readableAttachmentName(),
            mimeType = getType(uri).orEmpty().ifBlank { "application/octet-stream" },
            sizeBytes = 0L,
            uriString = uri.toString(),
        )
}

private fun buildAttachmentSelectionErrorMessage(
    failedAttachmentNames: List<String>,
    allAttachmentsFailed: Boolean,
): String {
    if (allAttachmentsFailed) {
        return "Selected files could not be opened. Try again."
    }

    val displayedNames = failedAttachmentNames.take(MAX_ATTACHMENT_SELECTION_ERROR_NAMES).joinToString(separator = ", ")
    val remainingCount = (failedAttachmentNames.size - MAX_ATTACHMENT_SELECTION_ERROR_NAMES).coerceAtLeast(0)
    val suffix = if (remainingCount > 0) ", +$remainingCount more" else ""
    return "Some files could not be added: $displayedNames$suffix."
}

private fun Uri.readableAttachmentName(): String = lastPathSegment.orEmpty().ifBlank { "attachment" }

private fun Context.openAttachment(attachment: CreatePostAttachmentUiState) {
    val downloadUrl = attachment.downloadUrl
    if (downloadUrl.isNullOrBlank()) {
        Toast.makeText(this, R.string.attachment_open_failed, Toast.LENGTH_SHORT).show()
        return
    }

    runCatching {
        val viewIntent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(downloadUrl), attachment.mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val chooser =
            Intent
                .createChooser(viewIntent, getString(R.string.attachment_open_chooser_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(chooser)
    }.onFailure {
        Toast
            .makeText(this, R.string.attachment_open_failed, Toast.LENGTH_SHORT)
            .show()
    }
}

private fun Context.downloadAttachment(attachment: CreatePostAttachmentUiState) {
    val downloadUrl = attachment.downloadUrl
    if (downloadUrl.isNullOrBlank()) {
        Toast.makeText(this, R.string.attachment_download_failed, Toast.LENGTH_SHORT).show()
        return
    }

    runCatching {
        val fileName = attachment.name.toSafeDownloadFileName()
        val request =
            DownloadManager
                .Request(Uri.parse(downloadUrl))
                .setTitle(fileName)
                .setDescription(getString(R.string.attachment_download_description))
                .setMimeType(attachment.mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadManager = getSystemService(DownloadManager::class.java)
        checkNotNull(downloadManager) { "Download service is unavailable" }
        downloadManager.enqueue(request)
    }.onSuccess {
        Toast
            .makeText(
                this,
                getString(R.string.attachment_download_started, attachment.name),
                Toast.LENGTH_SHORT,
            ).show()
    }.onFailure {
        Toast
            .makeText(this, R.string.attachment_download_failed, Toast.LENGTH_SHORT)
            .show()
    }
}

private fun String.toSafeDownloadFileName(): String =
    replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .ifBlank { "attachment" }

private fun Long.toCommentDateLabel(): String {
    if (this <= 0L) return "--"
    return DateTimeFormatter
        .ofPattern("d MMM, HH:mm", Locale.ENGLISH)
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}

private const val COMMENTS_SECTION_ITEM_INDEX = 3
private const val MAX_ATTACHMENT_SELECTION_ERROR_NAMES = 3

@Composable
private fun PickerOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
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
        }
    }
}
