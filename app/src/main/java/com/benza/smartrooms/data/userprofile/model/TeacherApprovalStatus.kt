package com.benza.smartrooms.data.userprofile.model

/**
 * Approval lifecycle for teacher access requests.
 */
internal enum class TeacherApprovalStatus {
    NONE,
    PENDING,
    APPROVED,
    REJECTED
}
