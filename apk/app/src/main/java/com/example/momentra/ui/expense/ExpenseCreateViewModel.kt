package com.example.momentra.ui.expense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.momentra.data.api.ApiResultException
import com.example.momentra.data.repository.ExpenseCreateGateway
import com.example.momentra.data.repository.ExpenseCreateRepository
import com.example.momentra.domain.CreateExpenseOutcome
import com.example.momentra.domain.finance.ExpenseMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ExpenseCreateUiState(
    val amount: String = "",
    val currencyCode: String = "INR",
    val description: String = "",
    val merchantName: String = "",
    val effectiveAtIso: String? = null,
    val moreDetailsOpen: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val draftId: String = UUID.randomUUID().toString(),
)

class ExpenseCreateViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository: ExpenseCreateGateway = ExpenseCreateRepository(application)

    private val _state = MutableStateFlow(ExpenseCreateUiState())
    val state: StateFlow<ExpenseCreateUiState> = _state.asStateFlow()

    fun updateAmount(value: String) {
        _state.update { it.copy(amount = value, error = null) }
    }

    fun updateCurrency(code: String) {
        _state.update { it.copy(currencyCode = code.uppercase().take(3), error = null) }
    }

    fun updateDescription(value: String) {
        _state.update { it.copy(description = value) }
    }

    fun updateMerchant(value: String) {
        _state.update { it.copy(merchantName = value) }
    }

    fun setMoreDetailsOpen(open: Boolean) {
        _state.update { it.copy(moreDetailsOpen = open) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /** New logical expense — new Idempotency-Key. */
    fun resetForAnother() {
        _state.update {
            ExpenseCreateUiState(
                currencyCode = it.currencyCode,
                draftId = UUID.randomUUID().toString(),
            )
        }
    }

    fun submit(
        momentId: String,
        onSuccess: (CreateExpenseOutcome) -> Unit,
    ) {
        if (_state.value.submitting) return
        val amount = ExpenseMoney.validateForSubmit(_state.value.amount)
        if (amount == null) {
            _state.update { it.copy(error = "Enter a valid positive amount.") }
            return
        }
        val currency = _state.value.currencyCode.trim().uppercase()
        if (currency.length != 3) {
            _state.update { it.copy(error = "Currency must be a 3-letter code.") }
            return
        }

        val draftKey = "expense:$momentId:${_state.value.draftId}"
        val description = _state.value.description
        val merchant = _state.value.merchantName
        val effectiveAt = _state.value.effectiveAtIso

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            repository.createExpense(
                draftKey = draftKey,
                momentId = momentId,
                amount = amount,
                currencyCode = currency,
                description = description,
                merchantName = merchant,
                effectiveAt = effectiveAt,
            ).fold(
                onSuccess = { outcome ->
                    _state.update { it.copy(submitting = false) }
                    onSuccess(outcome)
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            submitting = false,
                            error = when (e) {
                                is ApiResultException -> e.message ?: e.toString()
                                else -> e.message ?: "Could not save expense"
                            },
                        )
                    }
                },
            )
        }
    }
}
