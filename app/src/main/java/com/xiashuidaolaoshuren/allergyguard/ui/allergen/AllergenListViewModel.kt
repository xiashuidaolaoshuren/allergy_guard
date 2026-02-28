package com.xiashuidaolaoshuren.allergyguard.ui.allergen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiashuidaolaoshuren.allergyguard.data.Allergen
import com.xiashuidaolaoshuren.allergyguard.data.AllergenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AllergenListViewModel(
    private val repository: AllergenRepository
) : ViewModel() {
    private val _allergens = MutableStateFlow<List<Allergen>>(emptyList())
    val allergens: StateFlow<List<Allergen>> = _allergens.asStateFlow()

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

    class Factory(
        private val repository: AllergenRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AllergenListViewModel::class.java)) {
                return AllergenListViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}