package com.xiashuidaolaoshuren.allergyguard.ui.allergen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiashuidaolaoshuren.allergyguard.data.Allergen
import com.xiashuidaolaoshuren.allergyguard.data.AllergenAlias
import com.xiashuidaolaoshuren.allergyguard.data.AllergenAliasRepository
import com.xiashuidaolaoshuren.allergyguard.data.AllergenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AllergenListViewModel(
    private val repository: AllergenRepository,
    private val aliasRepository: AllergenAliasRepository
) : ViewModel() {
    private val _allergens = MutableStateFlow<List<Allergen>>(emptyList())
    val allergens: StateFlow<List<Allergen>> = _allergens.asStateFlow()
    private val _addCustomAllergenEvents = MutableSharedFlow<AddCustomAllergenEvent>()
    val addCustomAllergenEvents: SharedFlow<AddCustomAllergenEvent> = _addCustomAllergenEvents.asSharedFlow()
    private val _aliasEvents = MutableSharedFlow<AliasEvent>()
    val aliasEvents: SharedFlow<AliasEvent> = _aliasEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.allergens.collect { allergenList ->
                _allergens.value = allergenList
            }
        }
    }

    fun onAllergenToggled(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setAllergenEnabled(id, isEnabled)
        }
    }

    fun addCustomAllergen(rawName: String) {
        val name = rawName.trim()
        if (name.isBlank()) {
            viewModelScope.launch {
                _addCustomAllergenEvents.emit(AddCustomAllergenEvent.ValidationError(AddCustomAllergenError.BLANK))
            }
            return
        }

        viewModelScope.launch {
            if (repository.customAllergenNameExists(name)) {
                _addCustomAllergenEvents.emit(AddCustomAllergenEvent.ValidationError(AddCustomAllergenError.DUPLICATE))
                return@launch
            }

            val created = repository.addCustomAllergen(name)
            if (created) {
                _addCustomAllergenEvents.emit(AddCustomAllergenEvent.Added)
            } else {
                _addCustomAllergenEvents.emit(AddCustomAllergenEvent.ValidationError(AddCustomAllergenError.DUPLICATE))
            }
        }
    }

    // ── Alias management ────────────────────────────────────────────────────

    fun getAliasesForAllergen(allergenId: String): Flow<List<AllergenAlias>> =
        aliasRepository.getAliasesForAllergen(allergenId)

    fun addAlias(allergenId: String, rawAlias: String) {
        val alias = rawAlias.trim()
        if (alias.isBlank()) {
            viewModelScope.launch {
                _aliasEvents.emit(AliasEvent.ValidationError(AliasError.BLANK))
            }
            return
        }
        viewModelScope.launch {
            if (aliasRepository.aliasExists(allergenId, alias)) {
                _aliasEvents.emit(AliasEvent.ValidationError(AliasError.DUPLICATE))
                return@launch
            }
            aliasRepository.addAlias(allergenId, alias)
            _aliasEvents.emit(AliasEvent.Added)
        }
    }

    fun deleteAlias(aliasId: String) {
        viewModelScope.launch {
            aliasRepository.deleteAlias(aliasId)
        }
    }

    // ── Event types ─────────────────────────────────────────────────────────

    enum class AddCustomAllergenError { BLANK, DUPLICATE }

    sealed interface AddCustomAllergenEvent {
        data object Added : AddCustomAllergenEvent
        data class ValidationError(val error: AddCustomAllergenError) : AddCustomAllergenEvent
    }

    enum class AliasError { BLANK, DUPLICATE }

    sealed interface AliasEvent {
        data object Added : AliasEvent
        data class ValidationError(val error: AliasError) : AliasEvent
    }

    class Factory(
        private val repository: AllergenRepository,
        private val aliasRepository: AllergenAliasRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AllergenListViewModel::class.java)) {
                return AllergenListViewModel(repository, aliasRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}