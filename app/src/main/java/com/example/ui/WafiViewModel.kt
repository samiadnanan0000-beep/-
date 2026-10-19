package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AppOpenAdManager
import com.example.backup.BackupRestoreManager
import com.example.billing.BillingManager
import com.example.data.PersonEntity
import com.example.data.PersonWithBalance
import com.example.data.TransactionEntity
import com.example.data.WafiDatabase
import com.example.data.WafiRepository
import com.example.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WafiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WafiDatabase.getDatabase(application)
    val repository = WafiRepository(db.wafiDao())
    val billingManager = BillingManager(application, viewModelScope)
    val adManager = AppOpenAdManager(application)
    val securityManager = SecurityManager(application)
    val backupManager = BackupRestoreManager(application, repository)

    private val prefs = application.getSharedPreferences("wafi_app_prefs", Context.MODE_PRIVATE)

    private val _preferredCurrency = MutableStateFlow(prefs.getString("pref_currency", "YER") ?: "YER")
    val preferredCurrency: StateFlow<String> = _preferredCurrency.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(prefs.getBoolean("first_launch", true))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("ALL") // "ALL", "DEBTOR", "CREDITOR", "CLEARED"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _currencyFilter = MutableStateFlow("ALL")
    val currencyFilter: StateFlow<String> = _currencyFilter.asStateFlow()

    // Global summary
    val personsWithBalances = repository.getPersonsWithBalancesFlow(_preferredCurrency.value)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredPersons: StateFlow<List<PersonWithBalance>> = combine(
        personsWithBalances,
        _searchQuery,
        _filterType,
        _currencyFilter,
        _preferredCurrency
    ) { list, query, filter, currFilter, baseCurrency ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.person.name.contains(query, ignoreCase = true) ||
                    item.person.phone.contains(query)

            val primaryBal = item.getPrimaryBalance(if (currFilter == "ALL") baseCurrency else currFilter)
            val matchesType = when (filter) {
                "DEBTOR" -> primaryBal > 0.001
                "CREDITOR" -> primaryBal < -0.001
                "CLEARED" -> item.isCleared()
                else -> true
            }

            val matchesCurrency = if (currFilter == "ALL") true else {
                item.balancesByCurrency.containsKey(currFilter)
            }

            matchesQuery && matchesType && matchesCurrency
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Daily backup check in background
        viewModelScope.launch {
            backupManager.checkAndPerformDailyAutoBackup()
        }
    }

    fun setPreferredCurrency(currency: String) {
        prefs.edit().putString("pref_currency", currency).apply()
        _preferredCurrency.value = currency
    }

    fun completeFirstLaunch(selectedCurrency: String) {
        prefs.edit()
            .putBoolean("first_launch", false)
            .putString("pref_currency", selectedCurrency)
            .apply()
        _preferredCurrency.value = selectedCurrency
        _isFirstLaunch.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setCurrencyFilter(curr: String) {
        _currencyFilter.value = curr
    }

    fun addPerson(name: String, phone: String, creditLimit: Double, notes: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addPerson(name, phone, creditLimit, notes)
            onDone(id)
        }
    }

    fun updatePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.updatePerson(person)
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            repository.deletePerson(person)
        }
    }

    fun addTransaction(
        personId: Long,
        type: String,
        amount: Double,
        currency: String,
        details: String,
        date: Long,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addTransaction(personId, type, amount, currency, details, date)
            onDone()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }
}
