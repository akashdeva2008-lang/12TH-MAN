package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Umpire
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UmpireRepository {

    private val apiService = RetrofitClient.getApiService()

    fun pollUmpires(intervalMs: Long = 10000L): Flow<List<Umpire>> = flow {
        while (true) {
            try {
                val response = apiService.getUmpires()
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteUmpires = response.body()!!.data!!
                    MockData.umpires.clear()
                    MockData.umpires.addAll(remoteUmpires)
                }
            } catch (ignored: Exception) { }
            emit(MockData.umpires.toList())
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun getUmpires(): Flow<List<Umpire>> = flow {
        // Emit cached data immediately
        emit(MockData.umpires)

        try {
            val response = apiService.getUmpires()
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteUmpires = response.body()!!.data!!
                if (remoteUmpires.isNotEmpty()) {
                    MockData.umpires.clear()
                    MockData.umpires.addAll(remoteUmpires)
                    emit(remoteUmpires)
                }
            }
        } catch (e: Exception) {
            // Gracefully retain cache on network exception
        }
    }.flowOn(Dispatchers.IO)

    fun getUmpireById(id: String): Umpire? {
        return MockData.umpires.find { it.id.equals(id, ignoreCase = true) }
    }

    fun searchUmpires(query: String): List<Umpire> {
        return MockData.umpires.filter { umpire ->
            umpire.name.contains(query, ignoreCase = true) ||
                    umpire.location.contains(query, ignoreCase = true) ||
                    umpire.certificationLevel.contains(query, ignoreCase = true)
        }
    }
}
