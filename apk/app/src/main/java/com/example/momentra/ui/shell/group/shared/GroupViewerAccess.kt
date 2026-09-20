package com.example.momentra.ui.shell.group.shared

import com.example.momentra.data.api.GroupParticipantDto
import java.util.Locale

/** Viewer (`OBSERVER` / legacy `VIEWER`) is read-only for Quick Add creates and expense entry. */
object GroupViewerAccess {
    fun isReadOnlyRole(roleCode: String?): Boolean {
        val upper = roleCode?.trim()?.uppercase(Locale.US).orEmpty()
        return upper == "OBSERVER" || upper == "VIEWER"
    }

    fun isViewer(participants: List<GroupParticipantDto>, currentUserId: String?): Boolean {
        if (currentUserId.isNullOrBlank()) return false
        val me = participants.firstOrNull { it.userId == currentUserId } ?: return false
        return isReadOnlyRole(me.roleCode)
    }
}
