package com.example.momentra.ui.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.api.CreateMomentParticipantBody
import com.example.momentra.data.api.GroupSetupBlockDto
import com.example.momentra.data.api.GroupInviteDto
import com.example.momentra.data.repository.MomentCreateRepository
import com.example.momentra.data.repository.MomentLifecycleRepository
import com.example.momentra.domain.CreateMomentOutcome
import com.example.momentra.ui.shell.empty.BusinessSetupCatalog
import com.example.momentra.ui.shell.empty.BusinessSetupKind
import com.example.momentra.ui.shell.empty.personal.PersonalSetupCatalog
import com.example.momentra.ui.shell.empty.personal.PersonalSetupKind
import com.example.momentra.ui.setup.SetupPreferenceFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MomentCreateUiState(
    val submitting: Boolean = false,
    val error: String? = null,
)

class MomentCreateViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = MomentCreateRepository()

    private val _state = MutableStateFlow(MomentCreateUiState())
    val state: StateFlow<MomentCreateUiState> = _state.asStateFlow()

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun submitPersonalSetup(
        kind: PersonalSetupKind,
        preferences: Map<String, Any>,
        title: String? = null,
        editingMomentId: String? = null,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        val catalog = PersonalSetupCatalog.forKind(kind)
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val resolvedTitle = title ?: catalog.defaultTitle
            if (editingMomentId != null) {
                submitEdit(editingMomentId, resolvedTitle, catalog.momentTypeCode, onSuccess)
                return@launch
            }
            val prefs = SetupPreferenceFilter.filterToCatalogKeys(
                preferences,
                PersonalSetupCatalog.allowedKeys(kind),
            )
            repository.createPersonalMoment(
                systemCode = kind.systemCode,
                momentTypeCode = catalog.momentTypeCode,
                title = resolvedTitle,
                preferences = prefs,
            ).fold(
                onSuccess = { dto ->
                    _state.update { it.copy(submitting = false, error = null) }
                    onSuccess(
                        CreateMomentOutcome(
                            momentId = dto.momentId,
                            title = dto.title,
                            domainCode = dto.domainCode,
                            status = dto.status,
                            version = dto.version,
                            momentTypeCode = catalog.momentTypeCode,
                            setupId = dto.setupId,
                            projectionHints = emptyList(),
                        ),
                    )
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = (e as? ApiResultException)?.message ?: e.message ?: "Create failed",
                        )
                    }
                },
            )
        }
    }

    fun submitBusinessSetup(
        kind: BusinessSetupKind,
        companyId: String,
        preferences: Map<String, Any>,
        title: String? = null,
        editingMomentId: String? = null,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        val catalog = BusinessSetupCatalog.forKind(kind)
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val resolvedTitle = title ?: catalog.defaultTitle
            if (editingMomentId != null) {
                submitEdit(editingMomentId, resolvedTitle, catalog.momentTypeCode, onSuccess)
                return@launch
            }
            val prefs = SetupPreferenceFilter.filterToCatalogKeys(
                preferences,
                BusinessSetupCatalog.allowedKeys(kind),
            )
            repository.createBusinessMoment(
                companyId = companyId,
                familyCode = kind.familyCode,
                momentTypeCode = catalog.momentTypeCode,
                title = resolvedTitle,
                preferences = prefs,
            ).fold(
                onSuccess = { dto ->
                    _state.update { it.copy(submitting = false, error = null) }
                    onSuccess(
                        CreateMomentOutcome(
                            momentId = dto.momentId,
                            title = dto.title,
                            domainCode = dto.domainCode,
                            status = dto.status,
                            version = dto.version,
                            momentTypeCode = catalog.momentTypeCode,
                            setupId = dto.setupId,
                            projectionHints = emptyList(),
                        ),
                    )
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = (e as? ApiResultException)?.message ?: e.message ?: "Create failed",
                        )
                    }
                },
            )
        }
    }

    fun submitGroupMoment(
        section: String,
        momentTypeCode: String,
        title: String,
        description: String?,
        startAt: String?,
        endAt: String?,
        participants: List<CreateMomentParticipantBody>,
        inviteCode: String? = null,
        groupSetup: GroupSetupBlockDto? = null,
        editingMomentId: String? = null,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        val (apiType, customLabel) = resolveGroupTypeForApi(section, momentTypeCode, title)
        viewModelScope.launch {
            if (editingMomentId != null) {
                submitEdit(editingMomentId, title, apiType, onSuccess)
                return@launch
            }
            repository.createGroupMoment(
                momentTypeCode = apiType,
                title = title,
                description = description,
                startAt = startAt,
                endAt = endAt,
                participants = participants,
                inviteCode = inviteCode,
                customTypeLabel = customLabel,
                groupSetup = groupSetup,
            ).fold(
                onSuccess = { dto ->
                    _state.update { it.copy(submitting = false, error = null) }
                    onSuccess(
                        CreateMomentOutcome(
                            momentId = dto.momentId,
                            title = dto.title,
                            domainCode = dto.domainCode,
                            status = dto.status,
                            version = dto.version,
                            momentTypeCode = apiType,
                            setupId = dto.setupId,
                            projectionHints = emptyList(),
                        ),
                    )
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = (e as? ApiResultException)?.message ?: e.message ?: "Create failed",
                        )
                    }
                },
            )
        }
    }

    private suspend fun submitEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        val lifecycle = MomentLifecycleRepository()
        lifecycle.getVersion(momentId).fold(
            onSuccess = { version ->
                lifecycle.rename(momentId, title, version).fold(
                    onSuccess = { dto ->
                        _state.update { it.copy(submitting = false, error = null) }
                        onSuccess(
                            CreateMomentOutcome(
                                momentId = dto.momentId,
                                title = dto.title,
                                domainCode = dto.domainCode,
                                status = dto.status,
                                version = dto.version,
                                momentTypeCode = momentTypeCode,
                                setupId = null,
                                projectionHints = emptyList(),
                            ),
                        )
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(
                                submitting = false,
                                error = (e as? ApiResultException)?.message ?: e.message ?: "Update failed",
                            )
                        }
                    },
                )
            },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        submitting = false,
                        error = (e as? ApiResultException)?.message ?: e.message ?: "Update failed",
                    )
                }
            },
        )
    }

    /**
     * Catalog may use `CUSTOM` for purchase/living — map to seeded taxonomy + customTypeLabel.
     */
    private fun resolveGroupTypeForApi(
        section: String,
        momentTypeCode: String,
        title: String,
    ): Pair<String, String?> {
        if (momentTypeCode != "CUSTOM") return momentTypeCode to null
        return when (section) {
            "living" -> "COMMUNITY_LIVING" to title
            else -> "COMMUNITY_PURCHASE" to title
        }
    }

    suspend fun mintGroupInvite(
        title: String,
        momentTypeCode: String,
        section: String = "",
    ): GroupInviteDto? {
        val (apiType, _) = resolveGroupTypeForApi(section, momentTypeCode, title)
        return repository.mintGroupInvite(title = title, momentTypeCode = apiType).getOrNull()
    }
}
