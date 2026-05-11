package com.benza.smartrooms.feature.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benza.smartrooms.R
import com.benza.smartrooms.data.auth.model.AuthUser
import com.benza.smartrooms.data.auth.repository.AuthRepository
import com.benza.smartrooms.data.room.model.CreateAnnouncementAttachment
import com.benza.smartrooms.data.room.model.CreateAnnouncementRequest
import com.benza.smartrooms.data.room.model.RoomAnnouncement
import com.benza.smartrooms.data.room.model.RoomAnnouncementAttachment
import com.benza.smartrooms.data.room.model.RoomOperationResult
import com.benza.smartrooms.data.room.model.UpdateAnnouncementRequest
import com.benza.smartrooms.data.room.repository.RoomRepository
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

internal data class CreatePostUiState(
    val roomId: String,
    val roomName: String,
    val announcementId: String? = null,
    val mode: CreatePostMode = CreatePostMode.CREATE,
    val titleInput: String = "",
    val messageInput: String = "",
    val attachments: List<CreatePostAttachmentUiState> = emptyList(),
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
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val currentUser = authRepository.getCurrentUser()
    private var originalRemoteAttachments: List<RoomAnnouncementAttachment> = emptyList()

    private val _uiState =
        MutableStateFlow(
            CreatePostUiState(
                roomId = roomId,
                roomName = roomName,
                announcementId = announcementId,
                mode = if (announcementId == null) CreatePostMode.CREATE else CreatePostMode.EDIT,
                isLoadingPost = announcementId != null,
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
        if (announcementId != null) {
            loadAnnouncement(announcementId)
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

    private fun populateExistingAnnouncement(announcement: RoomAnnouncement) {
        val canEdit = announcement.authorId == currentUser?.uid
        originalRemoteAttachments = announcement.attachments
        _uiState.update {
            it.copy(
                titleInput = announcement.title,
                messageInput = announcement.message,
                attachments = announcement.attachments.map(RoomAnnouncementAttachment::toUiState),
                isLoadingPost = false,
                canEdit = canEdit,
                errorMessageRes = if (canEdit) null else R.string.error_post_edit_forbidden,
                errorMessageText = null,
            )
        }
    }
}

private const val MAX_ATTACHMENTS = 10
private const val MAX_ATTACHMENT_SIZE_MB = 25
private const val MAX_ATTACHMENT_SIZE_BYTES = MAX_ATTACHMENT_SIZE_MB * 1024L * 1024L

private fun AuthUser.displayNameOrEmailName(): String =
    displayName
        ?.takeIf(String::isNotBlank)
        ?: email?.substringBefore("@").orEmpty()

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
