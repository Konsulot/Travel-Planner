package com.travelplanner.ui.screens.converter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelplanner.ui.AppViewModelProvider
import com.travelplanner.ui.components.CurrencyPickerField
import com.travelplanner.util.Currencies
import com.travelplanner.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onBack: () -> Unit,
    viewModel: ConverterViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Конвертер валют") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Обновить курсы")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.input.amountText,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Сумма") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.input.amountText.isNotBlank() && state.result == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("converter_amount"),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyPickerField(
                            currency = state.input.from,
                            onCurrencySelected = viewModel::onFromChange,
                            label = "Из",
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalIconButton(
                            onClick = viewModel::swap,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Icon(Icons.Filled.SwapVert, contentDescription = "Поменять местами")
                        }
                        CurrencyPickerField(
                            currency = state.input.to,
                            onCurrencySelected = viewModel::onToChange,
                            label = "В",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.result?.let { Formatters.money(it, state.input.to) } ?: "—",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("converter_result"),
                    )
                    Text(
                        Currencies.name(state.input.to),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.rate?.let { rate ->
                        Text(
                            "1 ${state.input.from} = ${Formatters.rate(rate)} ${state.input.to}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            RatesInfoCard(
                isFallback = state.rates.isFallback,
                updatedAt = state.rates.updatedAt,
                error = state.error,
            )

            if (state.quickConversions.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "${state.input.amountText} ${state.input.from} — это",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        state.quickConversions.forEachIndexed { index, (code, value) ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(Currencies.name(code), modifier = Modifier.weight(1f))
                                Text(Formatters.money(value, code), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatesInfoCard(isFallback: Boolean, updatedAt: Long?, error: String?) {
    val warn = isFallback || error != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (warn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (warn) Icons.Filled.CloudOff else Icons.Filled.Update, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    when {
                        isFallback -> "Используются примерные офлайн-курсы. Подключитесь к интернету и нажмите «Обновить»."
                        updatedAt != null -> "Курсы на ${formatDateTime(updatedAt)} (open.er-api.com)"
                        else -> "Курсы загружены"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))
