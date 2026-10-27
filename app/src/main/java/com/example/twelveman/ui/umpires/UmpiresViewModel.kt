package com.example.twelveman.ui.umpires

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Umpire
import com.example.twelveman.data.repository.UmpireRepository
import kotlinx.coroutines.launch

class UmpiresViewModel : ViewModel() {
    private val repository = UmpireRepository()

    private val _umpiresList = MutableLiveData<List<Umpire>>()
    val umpiresList: LiveData<List<Umpire>> = _umpiresList

    private val _selectedUmpire = MutableLiveData<Umpire?>()
    val selectedUmpire: LiveData<Umpire?> = _selectedUmpire

    init {
        fetchUmpires()
    }

    fun fetchUmpires(query: String = "") {
        viewModelScope.launch {
            repository.getUmpires().collect { umpires ->
                if (query.isBlank()) {
                    _umpiresList.value = umpires
                } else {
                    _umpiresList.value = repository.searchUmpires(query)
                }
            }
        }
    }

    fun loadUmpireDetails(id: String) {
        _selectedUmpire.value = repository.getUmpireById(id)
    }
}