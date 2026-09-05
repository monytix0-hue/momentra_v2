package com.example.momentra.ui.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.momentra.data.api.ApiClient
import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.api.CreateMomentParticipantBody
import com.example.momentra.data.api.DomainSetupPrefillDto
import com.example.momentra.data.api.GroupSetupBlockDto
import com.example.momentra.data.api.GroupSetupPrefillDto
import com.example.momentra.data.api.GroupInviteDto
import com.example.momentra.data.api.PatchPersonalSetupBody
import com.example.momentra.data.repository.AccountRepository
import com.example.momentra.data.repository.GroupSliceRepository
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
import java.util.UUID

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
        editingMomentStatus: String? = null,
        status: String? = null,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        val catalog = PersonalSetupCatalog.forKind(kind)
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val resolvedTitle = title ?: catalog.defaultTitle
            val prefs = SetupPreferenceFilter.filterToCatalogKeys(
                preferences,
                PersonalSetupCatalog.allowedKeys(kind),
            )
            if (editingMomentId != null) {
                submitPersonalEdit(
                    momentId = editingMomentId,
                    title = resolvedTitle,
                    momentTypeCode = catalog.momentTypeCode,
                    preferences = prefs,
                    activateAfter = status == "ACTIVE" && editingMomentStatus.equals("DRAFT", ignoreCase = true),
                    onSuccess = onSuccess,
                )
                return@launch
            }
            repository.createPersonalMoment(
                systemCode = kind.systemCode,
                momentTypeCode = catalog.momentTypeCode,
                title = resolvedTitle,
                preferences = prefs,
                status = status,
            ).fold(
                onSuccess = { dto -> finishCreateSuccess(dto, catalog.momentTypeCode, onSuccess) },
                onFailure = { e -> finishCreateFailure(e) },
            )
        }
    }

    fun submitBusinessSetup(
        kind: BusinessSetupKind,
        companyId: String,
        preferences: Map<String, Any>,
        title: String? = null,
        editingMomentId: String? = null,
        editingMomentStatus: String? = null,
        status: String? = null,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        val catalog = BusinessSetupCatalog.forKind(kind)
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val resolvedTitle = title ?: catalog.defaultTitle
            val prefs = SetupPreferenceFilter.filterToCatalogKeys(
                preferences,
                BusinessSetupCatalog.allowedKeys(kind),
            )
            if (editingMomentId != null) {
                submitBusinessEdit(
                    momentId = editingMomentId,
                    title = resolvedTitle,
                    momentTypeCode = catalog.momentTypeCode,
                    activateAfter = status == "ACTIVE" && editingMomentStatus.equals("DRAFT", ignoreCase = true),
                    onSuccess = onSuccess,
                )
                return@launch
            }
            repository.createBusinessMoment(
                companyId = companyId,
                familyCode = kind.familyCode,
                momentTypeCode = catalog.momentTypeCode,
                title = resolvedTitle,
                preferences = prefs,
                status = status,
            ).fold(
                onSuccess = { dto -> finishCreateSuccess(dto, catalog.momentTypeCode, onSuccess) },
                onFailure = { e -> finishCreateFailure(e) },
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
        editingMomentStatus: String? = null,
        status: String? = null,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        val (apiType, customLabel) = resolveGroupTypeForApi(section, momentTypeCode, title)
        viewModelScope.launch {
            if (editingMomentId != null) {
                submitGroupEdit(
                    momentId = editingMomentId,
                    title = title,
                    momentTypeCode = apiType,
                    groupSetup = groupSetup,
                    activateAfter = status == "ACTIVE" && editingMomentStatus.equals("DRAFT", ignoreCase = true),
                    onSuccess = onSuccess,
                )
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
                status = status,
            ).fold(
                onSuccess = { dto -> finishCreateSuccess(dto, apiType, onSuccess) },
                onFailure = { e -> finishCreateFailure(e) },
            )
        }
    }

    fun discardMomentDraft(
        momentId: String,
        onSuccess: () -> Unit,
    ) {
        if (_state.value.submitting) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            repository.discardMomentDraft(momentId).fold(
                onSuccess = {
                    _state.update { it.copy(submitting = false, error = null) }
                    onSuccess()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = (e as? ApiResultException)?.message ?: e.message ?: "Discard failed",
                        )
                    }
                },
            )
        }
    }

    private suspend fun submitGroupEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        groupSetup: GroupSetupBlockDto?,
        activateAfter: Boolean,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        val lifecycle = MomentLifecycleRepository()
        val accountRepo = AccountRepository()
        val groupRepo = GroupSliceRepository()
        lifecycle.getVersion(momentId).fold(
            onSuccess = { version ->
                lifecycle.rename(momentId, title, version).fold(
                    onSuccess = { dto ->
                        groupSetup?.let { setup ->
                            val primary = setup.budgets?.firstOrNull { it.isPrimary == true }
                                ?: setup.budgets?.firstOrNull()
                            val amount = primary?.amount ?: setup.budgetAmount
                            val currency = primary?.currencyCode ?: setup.budgetCurrencyCode
                            if (amount != null && currency != null) {
                                runCatching {
                                    groupRepo.patchGroupBudget(momentId, amount, currency)
                                }
                            }
                            setup.budgets?.drop(1)?.forEach { b ->
                                runCatching {
                                    groupRepo.patchGroupBudget(momentId, b.amount, b.currencyCode)
                                }
                            }
                            setup.reminderPreferences?.let { rem ->
                                runCatching {
                                    accountRepo.patchMomentNotificationPreferences(momentId, true, rem)
                                }
                            }
                        }
                        if (activateAfter) {
                            repository.activateMoment(momentId).fold(
                                onSuccess = { activated ->
                                    finishCreateSuccess(activated, momentTypeCode, onSuccess)
                                },
                                onFailure = { e -> finishCreateFailure(e) },
                            )
                        } else {
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
                        }
                    },
                    onFailure = { e -> finishCreateFailure(e) },
                )
            },
            onFailure = { e -> finishCreateFailure(e) },
        )
    }

    private suspend fun submitPersonalEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        preferences: Map<String, Any>,
        activateAfter: Boolean,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        try {
            val setups = ApiClient.apiService.getPersonalSetups().data
            val setup = setups.items.firstOrNull { it.momentId == momentId }
                ?: throw ApiResultException.NotFound("Personal setup not found for this moment.")
            val setupVersion = setup.version
                ?: throw ApiResultException.Validation(message = "Personal setup version missing.")
            ApiClient.apiService.patchPersonalSetup(
                setupId = setup.setupId,
                idempotencyKey = UUID.randomUUID().toString(),
                body = PatchPersonalSetupBody(
                    expectedVersion = setupVersion,
                    title = title,
                    preferences = preferences,
                ),
            )
            val lifecycle = MomentLifecycleRepository()
            lifecycle.getVersion(momentId).fold(
                onSuccess = { version ->
                    lifecycle.rename(momentId, title, version).fold(
                        onSuccess = { dto ->
                            if (activateAfter) {
                                repository.activateMoment(momentId).fold(
                                    onSuccess = { activated ->
                                        finishCreateSuccess(
                                            activated,
                                            momentTypeCode,
                                            onSuccess,
                                            setupId = setup.setupId,
                                        )
                                    },
                                    onFailure = { e -> finishCreateFailure(e) },
                                )
                            } else {
                                _state.update { it.copy(submitting = false, error = null) }
                                onSuccess(
                                    CreateMomentOutcome(
                                        momentId = dto.momentId,
                                        title = dto.title,
                                        domainCode = dto.domainCode,
                                        status = dto.status,
                                        version = dto.version,
                                        momentTypeCode = momentTypeCode,
                                        setupId = setup.setupId,
                                        projectionHints = emptyList(),
                                    ),
                                )
                            }
                        },
                        onFailure = { e -> finishCreateFailure(e) },
                    )
                },
                onFailure = { e -> finishCreateFailure(e) },
            )
        } catch (e: Exception) {
            finishCreateFailure(e)
        }
    }

    private suspend fun submitBusinessEdit(
        momentId: String,
        title: String,
        momentTypeCode: String,
        activateAfter: Boolean,
        onSuccess: (CreateMomentOutcome) -> Unit,
    ) {
        val lifecycle = MomentLifecycleRepository()
        lifecycle.getVersion(momentId).fold(
            onSuccess = { version ->
                lifecycle.rename(momentId, title, version).fold(
                    onSuccess = { dto ->
                        if (activateAfter) {
                            repository.activateMoment(momentId).fold(
                                onSuccess = { activated ->
                                    finishCreateSuccess(activated, momentTypeCode, onSuccess)
                                },
                                onFailure = { e -> finishCreateFailure(e) },
                            )
                        } else {
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
                        }
                    },
                    onFailure = { e -> finishCreateFailure(e) },
                )
            },
            onFailure = { e -> finishCreateFailure(e) },
        )
    }

    private fun finishCreateSuccess(
        dto: com.example.momentra.data.api.CreateMomentResultDto,
        momentTypeCode: String,
        onSuccess: (CreateMomentOutcome) -> Unit,
        setupId: String? = dto.setupId,
    ) {
        _state.update { it.copy(submitting = false, error = null) }
        onSuccess(
            CreateMomentOutcome(
                momentId = dto.momentId,
                title = dto.title,
                domainCode = dto.domainCode,
                status = dto.status,
                version = dto.version,
                momentTypeCode = momentTypeCode,
                setupId = setupId,
                projectionHints = emptyList(),
            ),
        )
    }

    private fun finishCreateFailure(e: Throwable) {
        _state.update {
            it.copy(
                submitting = false,
                error = (e as? ApiResultException)?.message ?: e.message ?: "Request failed",
            )
        }
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

    suspend fun getGroupSetupPrefill(momentId: String): GroupSetupPrefillDto? =
        repository.getGroupSetupPrefill(momentId).getOrNull()

    suspend fun getDomainSetupPrefill(momentId: String): DomainSetupPrefillDto? =
        repository.getDomainSetupPrefill(momentId).getOrNull()
}
