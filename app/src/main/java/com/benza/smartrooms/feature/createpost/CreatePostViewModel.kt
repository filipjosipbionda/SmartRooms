package com.benza.smartrooms.feature.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.CreateAnnouncementAttachment
import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.CreateRoomCommentRequest
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.RoomAnnouncementAttachment
import com.benza.smartrooms.data.room.model.RoomComment
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.UpdateAnnouncementRequest
import com.benza.smartrooms.data.room.repository.RoomRepository
import com.benza.smartrooms.data.userprofile.model.UserProfile
import com.benza.smartrooms.data.userprofile.model.UserProfileOperationResult
import com.benza.smartrooms.data.userprofile.repository.UserProfileRepository
import com.benza.smartrooms.util.orPrettyEmailLocalPart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class CreatePostMode {
    CREATE,
    EDIT,
    VIEW,
}

internal data class CreatePostAttachmentUiState(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uriString: String? = null,
    val storagePath: String? = null,
    val downloadUrl: String? = null,
)

internal data class RoomCommentUiState(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorPhotoUrl: String?,
    val message: String,
    val createdAtEpochMillis: Long,
)

internal data class CreatePostUiState(
    val roomId: String,
    val roomName: String,
    val announcementId: String? = null,
    val mode: CreatePostMode = CreatePostMode.CREATE,
    val titleInput: String = "",
    val messageInput: String = "",
    val attachments: List<CreatePostAttachmentUiState> = emptyList(),
    val comments: List<RoomCommentUiState> = emptyList(),
    val commentInput: String = "",
    val isLoadingComments: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val commentErrorRes: Int? = null,
    val titleErrorRes: Int? = null,
    val messageErrorRes: Int? = null,
    val errorMessageRes: Int? = null,
    val errorMessageText: String? = null,
    val isLoadingPost: Boolean = false,
    val isSubmittingPost: Boolean = false,
    val isDeletingPost: Boolean = false,
    val canEdit: Boolean = true,
)

internal sealed interface CreatePostEvent {
    data object PostCompleted : CreatePostEvent
}

internal class CreatePostViewModel(
    roomId: String,
    roomName: String,
    announcementId: String?,
    private val viewOnly: Boolean,
    private val roomRepository: RoomRepository,
    authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()
    private var originalRemoteAttachments: List<RoomAnnouncementAttachment> = emptyList()
    private var originalAnnouncement: RoomAnnouncement? = null
    private var currentUserProfile: UserProfile? = null
    private var latestComments: List<RoomComment> = emptyList()
    private var latestCommentProfilesById: Map<String, UserProfile> = emptyMap()
    private var observedCommentAuthorIds: List<String> = emptyList()
    private var commentProfilesJob: Job? = null

    private val _uiState =
        MutableStateFlow(
            CreatePostUiState(
                roomId = roomId,
                roomName = roomName,
                announcementId = announcementId,
                mode =
                    when {
                        announcementId == null -> CreatePostMode.CREATE
                        viewOnly -> CreatePostMode.VIEW
                        else -> CreatePostMode.EDIT
                    },
                isLoadingPost = announcementId != null,
                canEdit = !viewOnly,
            ),
        )
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _events =
        MutableSharedFlow<CreatePostEvent>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<CreatePostEvent> = _events.asSharedFlow()

    init {
        observeCurrentUserProfile()
        if (announcementId != null) {
            loadAnnouncement(announcementId)
            if (viewOnly) {
                observeComments(announcementId)
            }
        }
    }

    internal fun onTitleChanged(value: String) {
        if (!_uiState.value.canEdit) return
        _uiState.update {
            it.copy(
                titleInput = value,
                titleErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun onMessageChanged(value: String) {
        if (!_uiState.value.canEdit) return
        _uiState.update {
            it.copy(
                messageInput = value,
                messageErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun addAttachments(attachments: List<CreatePostAttachmentUiState>) {
        if (attachments.isEmpty() || !_uiState.value.canEdit) return
        _uiState.update { state ->
            val existingIds = state.attachments.map(CreatePostAttachmentUiState::id).toSet()
            val uniqueAttachments = attachments.distinctBy(CreatePostAttachmentUiState::id)
            val newAttachments = uniqueAttachments.filterNot { it.id in existingIds }
            if (newAttachments.isEmpty()) {
                return@update state.copy(
                    errorMessageRes = null,
                    errorMessageText = null,
                )
            }

            val remainingSlots = (MAX_ATTACHMENTS - state.attachments.size).coerceAtLeast(0)
            if (remainingSlots == 0) {
                return@update state.copy(
                    errorMessageRes = null,
                    errorMessageText = "You can attach up to $MAX_ATTACHMENTS files per post.",
                )
            }

            val acceptedAttachments = mutableListOf<CreatePostAttachmentUiState>()
            val oversizedFileNames = mutableListOf<String>()
            newAttachments.forEach { attachment ->
                if (attachment.sizeBytes > MAX_ATTACHMENT_SIZE_BYTES) {
                    oversizedFileNames += attachment.name
                } else if (acceptedAttachments.size < remainingSlots) {
                    acceptedAttachments += attachment
                }
            }

            val mergedAttachments = state.attachments + acceptedAttachments
            val skippedCount =
                (newAttachments.size - acceptedAttachments.size - oversizedFileNames.size).coerceAtLeast(0)
            state.copy(
                attachments = mergedAttachments,
                errorMessageRes = null,
                errorMessageText =
                    buildList {
                        if (oversizedFileNames.isNotEmpty()) {
                            add(
                                "These files are too large: ${oversizedFileNames.toReadableNameList()}. " +
                                    "Maximum size is ${MAX_ATTACHMENT_SIZE_MB} MB per file.",
                            )
                        }
                        if (skippedCount > 0) {
                            add("Only $MAX_ATTACHMENTS files can be attached to one post.")
                        }
                    }.joinToString(separator = " ").ifBlank { null },
            )
        }
    }

    internal fun onAttachmentSelectionFailed(message: String) {
        _uiState.update {
            it.copy(
                errorMessageRes = null,
                errorMessageText =
                    listOfNotNull(it.errorMessageText, message)
                        .distinct()
                        .joinToString(separator = " "),
            )
        }
    }

    internal fun removeAttachment(attachmentId: String) {
        if (!_uiState.value.canEdit) return
        _uiState.update {
            it.copy(
                attachments = it.attachments.filterNot { attachment -> attachment.id == attachmentId },
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun submitPost() {
        val user =
            currentUser ?: run {
                _uiState.update {
                    it.copy(
                        errorMessageRes = R.string.error_room_auth_required,
                        errorMessageText = null,
                    )
                }
                return
            }
        val state = _uiState.value
        if (state.isLoadingPost || state.isSubmittingPost || state.isDeletingPost || !state.canEdit) return

        val title = state.titleInput.trim()
        val message = state.messageInput.trim()
        val titleError = if (title.isBlank()) R.string.error_announcement_title_required else null
        val messageError = if (message.isBlank()) R.string.error_announcement_message_required else null

        _uiState.update {
            it.copy(
                titleInput = title,
                messageInput = message,
                titleErrorRes = titleError,
                messageErrorRes = messageError,
                errorMessageRes =
                    if (titleError == null && messageError == null) {
                        null
                    } else {
                        R.string.error_announcement_fix_fields
                    },
                errorMessageText = null,
            )
        }

        if (titleError != null || messageError != null) return

        _uiState.update {
            it.copy(
                isSubmittingPost = true,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }

        viewModelScope.launch {
            when (state.mode) {
                CreatePostMode.CREATE -> createPost(user, title, message)
                CreatePostMode.EDIT -> updatePost(title, message)
                CreatePostMode.VIEW -> Unit
            }
        }
    }

    internal fun deletePost() {
        val state = _uiState.value
        val announcementId = state.announcementId ?: return
        if (state.mode != CreatePostMode.EDIT ||
            state.isLoadingPost ||
            state.isSubmittingPost ||
            state.isDeletingPost ||
            !state.canEdit
        ) {
            return
        }

        _uiState.update {
            it.copy(
                isDeletingPost = true,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }

        viewModelScope.launch {
            when (
                val result =
                    roomRepository.deleteAnnouncement(
                        roomId = state.roomId,
                        announcementId = announcementId,
                        attachments = state.attachments.mapNotNull(CreatePostAttachmentUiState::toRoomAttachment),
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update { it.copy(isDeletingPost = false) }
                    _events.tryEmit(CreatePostEvent.PostCompleted)
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isDeletingPost = false,
                            errorMessageRes = result.messageRes,
                            errorMessageText = result.debugMessage,
                        )
                    }
                }
            }
        }
    }

    internal fun onCommentChanged(value: String) {
        if (_uiState.value.mode != CreatePostMode.VIEW) return

        _uiState.update {
            it.copy(
                commentInput = value,
                commentErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }
    }

    internal fun submitComment() {
        val user =
            currentUser ?: run {
                _uiState.update { it.copy(errorMessageRes = R.string.error_room_auth_required) }
                return
            }
        val state = _uiState.value
        val announcementId = state.announcementId ?: return
        if (state.mode != CreatePostMode.VIEW || state.isSubmittingComment) return

        val message = state.commentInput.trim()
        val commentError = if (message.isBlank()) R.string.error_comment_required else null
        _uiState.update {
            it.copy(
                commentInput = message,
                commentErrorRes = commentError,
                errorMessageRes = commentError,
                errorMessageText = null,
            )
        }
        if (commentError != null) return

        _uiState.update {
            it.copy(
                isSubmittingComment = true,
                commentErrorRes = null,
                errorMessageRes = null,
                errorMessageText = null,
            )
        }

        viewModelScope.launch {
            val profile = currentUserProfile
            when (
                val result =
                    roomRepository.createAnnouncementComment(
                        CreateRoomCommentRequest(
                            roomId = state.roomId,
                            announcementId = announcementId,
                            authorId = user.uid,
                            authorName = profile?.displayName ?: user.displayNameOrEmailName(),
                            authorPhotoUrl = profile?.photoUrl ?: user.photoUrl,
                            message = message,
                        ),
                    )
            ) {
                is RoomOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingComment = false,
                            commentInput = "",
                        )
                    }
                }

                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingComment = false,
                            errorMessageRes = result.messageRes,
                            errorMessageText = result.debugMessage,
                        )
                    }
                }
            }
        }
    }

    private suspend fun createPost(
        user: AuthUser,
        title: String,
        message: String,
    ) {
        when (
            val result =
                roomRepository.createAnnouncement(
                    CreateAnnouncementRequest(
                        roomId = _uiState.value.roomId,
                        authorId = user.uid,
                        authorName = user.displayNameOrEmailName(),
                        title = title,
                        message = message,
                        attachments =
                            _uiState.value.attachments.mapNotNull(
                                CreatePostAttachmentUiState::toCreateAttachment,
                            ),
                    ),
                )
        ) {
            is RoomOperationResult.Success -> {
                _uiState.update { it.copy(isSubmittingPost = false) }
                _events.tryEmit(CreatePostEvent.PostCompleted)
            }

            is RoomOperationResult.Error -> {
                _uiState.update {
                    it.copy(
                        isSubmittingPost = false,
                        errorMessageRes = result.messageRes,
                        errorMessageText = result.debugMessage,
                    )
                }
            }
        }
    }

    private suspend fun updatePost(
        title: String,
        message: String,
    ) {
        val state = _uiState.value
        val announcementId = state.announcementId ?: return
        val keptRemoteAttachments = state.attachments.mapNotNull(CreatePostAttachmentUiState::toRoomAttachment)
        val removedRemoteAttachments =
            originalRemoteAttachments.filterNot { original ->
                keptRemoteAttachments.any { retained -> retained.id == original.id }
            }

        when (
            val result =
                roomRepository.updateAnnouncement(
                    UpdateAnnouncementRequest(
                        roomId = state.roomId,
                        announcementId = announcementId,
                        title = title,
                        message = message,
                        existingAttachments = keptRemoteAttachments,
                        newAttachments = state.attachments.mapNotNull(CreatePostAttachmentUiState::toCreateAttachment),
                        removedAttachments = removedRemoteAttachments,
                    ),
                )
        ) {
            is RoomOperationResult.Success -> {
                _uiState.update { it.copy(isSubmittingPost = false) }
                _events.tryEmit(CreatePostEvent.PostCompleted)
            }

            is RoomOperationResult.Error -> {
                _uiState.update {
                    it.copy(
                        isSubmittingPost = false,
                        errorMessageRes = result.messageRes,
                        errorMessageText = result.debugMessage,
                    )
                }
            }
        }
    }

    private fun loadAnnouncement(announcementId: String) {
        viewModelScope.launch {
            when (val result = roomRepository.getAnnouncement(_uiState.value.roomId, announcementId)) {
                is RoomOperationResult.Success -> populateExistingAnnouncement(result.data)
                is RoomOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingPost = false,
                            canEdit = false,
                            errorMessageRes = result.messageRes,
                            errorMessageText = result.debugMessage,
                        )
                    }
                }
            }
        }
    }

    private fun observeComments(announcementId: String) {
        _uiState.update { it.copy(isLoadingComments = true) }
        viewModelScope.launch {
            roomRepository.observeAnnouncementComments(_uiState.value.roomId, announcementId).collect { result ->
                when (result) {
                    is RoomOperationResult.Success -> {
                        latestComments = result.data
                        _uiState.update {
                            it.copy(
                                isLoadingComments = false,
                                errorMessageRes = null,
                                errorMessageText = null,
                            )
                        }
                        publishComments()
                        refreshCommentProfilesObserver()
                    }

                    is RoomOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingComments = false,
                                errorMessageRes = result.messageRes,
                                errorMessageText = result.debugMessage,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun populateExistingAnnouncement(announcement: RoomAnnouncement) {
        originalAnnouncement = announcement
        val canEdit = !viewOnly && canEditAnnouncement(announcement)
        originalRemoteAttachments = announcement.attachments
        _uiState.update {
            it.copy(
                titleInput = announcement.title,
                messageInput = announcement.message,
                attachments = announcement.attachments.map(RoomAnnouncementAttachment::toUiState),
                isLoadingPost = false,
                canEdit = canEdit,
                errorMessageRes = if (viewOnly || canEdit) null else R.string.error_post_edit_forbidden,
                errorMessageText = null,
            )
        }
    }

    private fun observeCurrentUserProfile() {
        val user = currentUser ?: return

        viewModelScope.launch {
            userProfileRepository.observeProfile(user).collect { result ->
                when (result) {
                    is UserProfileOperationResult.Success -> {
                        currentUserProfile = result.data
                    }

                    is UserProfileOperationResult.Error -> {
                        currentUserProfile = null
                    }
                }
            }
        }
    }

    private fun refreshCommentProfilesObserver() {
        val authorIds =
            latestComments
                .map(RoomComment::authorId)
                .distinct()
                .filter(String::isNotBlank)
        if (authorIds == observedCommentAuthorIds && commentProfilesJob != null) return

        commentProfilesJob?.cancel()
        observedCommentAuthorIds = authorIds
        latestCommentProfilesById = emptyMap()
        publishComments()
        if (authorIds.isEmpty()) {
            commentProfilesJob = null
            return
        }

        commentProfilesJob =
            viewModelScope.launch {
                userProfileRepository.observeProfiles(authorIds).collect { result ->
                    if (result is UserProfileOperationResult.Success) {
                        latestCommentProfilesById = result.data.associateBy(UserProfile::uid)
                        publishComments()
                    }
                }
            }
    }

    private fun publishComments() {
        _uiState.update { state ->
            state.copy(
                comments =
                    latestComments.map { comment ->
                        comment.toUiState(latestCommentProfilesById[comment.authorId])
                    },
            )
        }
    }

    private fun canEditAnnouncement(announcement: RoomAnnouncement): Boolean = announcement.authorId == currentUser?.uid
}

private const val MAX_ATTACHMENTS = 10
private const val MAX_ATTACHMENT_SIZE_MB = 25
private const val MAX_ATTACHMENT_SIZE_BYTES = MAX_ATTACHMENT_SIZE_MB * 1024L * 1024L

private fun AuthUser.displayNameOrEmailName(): String =
    displayName
        ?.takeIf(String::isNotBlank)
        ?: email.orPrettyEmailLocalPart(email)

private fun List<String>.toReadableNameList(): String =
    when {
        isEmpty() -> ""
        size <= MAX_ERROR_FILE_NAME_COUNT -> joinToString(separator = ", ")
        else ->
            take(MAX_ERROR_FILE_NAME_COUNT).joinToString(separator = ", ") +
                ", +${size - MAX_ERROR_FILE_NAME_COUNT} more"
    }

private fun RoomAnnouncementAttachment.toUiState(): CreatePostAttachmentUiState =
    CreatePostAttachmentUiState(
        id = id,
        name = name,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        storagePath = storagePath,
        downloadUrl = downloadUrl,
    )

private fun RoomComment.toUiState(currentProfile: UserProfile?): RoomCommentUiState =
    RoomCommentUiState(
        id = id,
        authorId = authorId,
        authorName = currentProfile?.displayName ?: authorName,
        authorPhotoUrl = currentProfile?.photoUrl ?: authorPhotoUrl,
        message = message,
        createdAtEpochMillis = createdAtEpochMillis,
    )

private fun CreatePostAttachmentUiState.toCreateAttachment(): CreateAnnouncementAttachment? =
    uriString?.let { localUri ->
        CreateAnnouncementAttachment(
            uriString = localUri,
            name = name,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
        )
    }

private fun CreatePostAttachmentUiState.toRoomAttachment(): RoomAnnouncementAttachment? {
    val attachmentStoragePath = storagePath ?: return null
    val attachmentDownloadUrl = downloadUrl ?: return null
    return RoomAnnouncementAttachment(
        id = id,
        name = name,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        storagePath = attachmentStoragePath,
        downloadUrl = attachmentDownloadUrl,
    )
}

private const val MAX_ERROR_FILE_NAME_COUNT = 3
