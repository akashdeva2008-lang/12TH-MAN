package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Umpire
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UmpireRepository {
    fun getUmpires(): Flow<List<Umpire>> = flow {
        emit(MockData.umpires)
    }

    fun getUmpireById(id: String): Umpire? {
        return MockData.umpires.find { it.id == id }
    }

    fun searchUmpires(query: String): List<Umpire> {
        return MockData.umpires.filter { umpire ->
            umpire.name.contains(query, ignoreCase = true) ||
                    umpire.location.contains(query, ignoreCase = true) ||
                    umpire.certificationLevel.contains(query, ignoreCase = true)
        }
    }
}