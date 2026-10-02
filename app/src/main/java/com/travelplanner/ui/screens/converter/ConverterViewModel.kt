package com.travelplanner.ui.screens.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelplanner.data.CurrencyRepository
import com.travelplanner.model.RatesSnapshot
import com.travelplanner.util.CurrencyConverter
import com.travelplanner.util.DefaultRates
import com.travelplanner.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConverterInput(
    val amountText: String = "100",
    val from: String = "USD",
    val to: String = "RUB",
)

data class ConverterUiState(
    val input: ConverterInput = ConverterInput(),
    val result: Double? = null,
    /** Сколько единиц «to» за 1 единицу «from». */
    val rate: Double? = null,
    val rates: RatesSnapshot = RatesSnapshot(DefaultRates.ratesPerUsd, null, isFallback = true),
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /** Быстрый пересчёт суммы в популярные валюты. */
    val quickConversions: List<Pair<String, Double>> = emptyList(),
)

class ConverterViewModel(private val repository: CurrencyRepository) : ViewModel() {

    private val input = MutableStateFlow(ConverterInput())
    private val refreshState = MutableStateFlow(RefreshState())

    private data class RefreshState(val isRefreshing: Boolean = false, val error: String? = null)

    val uiState: StateFlow<ConverterUiState> =
        combine(input, repository.rates, refreshState) { input, rates, refresh ->
            buildState(input, rates, refresh.isRefreshing, refresh.error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConverterUiState())

    init {
        refresh(onlyIfStale = true)
    }

    fun onAmountChange(text: String) {
        if (text.length <= 15) input.update { it.copy(amountText = text) }
    }

    fun onFromChange(code: String) = input.update { it.copy(from = code) }

    fun onToChange(code: String) = input.update { it.copy(to = code) }

    fun swap() = input.update { it.copy(from = it.to, to = it.from) }

    fun refresh(onlyIfStale: Boolean = false) {
        if (refreshState.value.isRefreshing) return
        refreshState.value = RefreshState(isRefreshing = true)
        viewModelScope.launch {
            val result = if (onlyIfStale) repository.refreshIfStale() else repository.refresh()
            refreshState.value = RefreshState(
                isRefreshing = false,
                error = result.exceptionOrNull()?.let { "Не удалось обновить курсы. Проверьте подключение к интернету." },
            )
        }
    }

    companion object {
        private val QUICK_CURRENCIES = listOf("RUB", "USD", "EUR", "CNY", "TRY", "GEL", "AED", "THB")

        fun buildState(
            input: ConverterInput,
            rates: RatesSnapshot,
            isRefreshing: Boolean,
            error: String?,
        ): ConverterUiState {
            val amount = Formatters.parseAmount(input.amountText)
            val map = rates.ratesPerUsd
            return ConverterUiState(
                input = input,
                result = amount?.let { CurrencyConverter.convert(it, input.from, input.to, map) },
                rate = CurrencyConverter.rate(input.from, input.to, map),
                rates = rates,
                isRefreshing = isRefreshing,
                error = error,
                quickConversions = if (amount == null) {
                    emptyList()
                } else {
                    QUICK_CURRENCIES
                        .filter { it != input.from && it != input.to }
                        .mapNotNull { code -> CurrencyConverter.convert(amount, input.from, code, map)?.let { code to it } }
                },
            )
        }
    }
}
