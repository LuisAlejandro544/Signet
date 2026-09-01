package com.example.ui.delegates

import com.example.data.model.KeystoreDetails
import com.example.ui.state.KeystoreSortFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Delegado modular responsable de los criterios de búsqueda en tiempo real,
 * ordenamiento temporal y registro de almacenes consultados recientemente.
 */
class KeystoreFilterDelegate {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSortFilter = MutableStateFlow(KeystoreSortFilter.NEWEST)
    val selectedSortFilter: StateFlow<KeystoreSortFilter> = _selectedSortFilter.asStateFlow()

    private val _recentlyViewedMap = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val recentlyViewedMap: StateFlow<Map<Long, Long>> = _recentlyViewedMap.asStateFlow()

    fun buildFilteredSavedKeystoresFlow(
        savedKeystores: Flow<List<KeystoreDetails>>,
        scope: CoroutineScope
    ): StateFlow<List<KeystoreDetails>> {
        return combine(
            savedKeystores,
            _searchQuery,
            _selectedSortFilter,
            _recentlyViewedMap
        ) { all, query, filter, viewedMap ->
            val filtered = if (query.isBlank()) {
                all
            } else {
                val q = query.trim().lowercase()
                all.filter { k ->
                    k.fileName.lowercase().contains(q) ||
                    k.alias.lowercase().contains(q) ||
                    k.subjectDn.lowercase().contains(q) ||
                    k.issuerDn.lowercase().contains(q) ||
                    k.algorithm.lowercase().contains(q) ||
                    k.sha256Fingerprint.lowercase().contains(q) ||
                    k.sha1Fingerprint.lowercase().contains(q) ||
                    k.md5Fingerprint.lowercase().contains(q) ||
                    k.serialNumber.lowercase().contains(q)
                }
            }

            when (filter) {
                KeystoreSortFilter.NEWEST -> filtered.sortedByDescending { it.createdAt }
                KeystoreSortFilter.OLDEST -> filtered.sortedBy { it.createdAt }
                KeystoreSortFilter.INTERMEDIATE -> {
                    if (filtered.size <= 2) {
                        filtered
                    } else {
                        val meanTime = filtered.map { it.createdAt }.average()
                        filtered.sortedBy { kotlin.math.abs(it.createdAt - meanTime) }
                    }
                }
                KeystoreSortFilter.RECENTLY_VIEWED -> {
                    filtered.sortedWith(
                        compareByDescending<KeystoreDetails> { viewedMap[it.id] ?: 0L }
                            .thenByDescending { it.createdAt }
                    )
                }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortFilter(filter: KeystoreSortFilter) {
        _selectedSortFilter.value = filter
    }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedSortFilter.value = KeystoreSortFilter.NEWEST
    }

    fun recordKeystoreViewed(keystoreId: Long) {
        _recentlyViewedMap.value = _recentlyViewedMap.value + (keystoreId to System.currentTimeMillis())
    }
}
