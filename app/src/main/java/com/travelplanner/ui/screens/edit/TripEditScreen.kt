package com.travelplanner.ui.screens.edit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelplanner.ui.AppViewModelProvider
import com.travelplanner.ui.components.CurrencyPickerField
import com.travelplanner.ui.components.DatePickerField
import com.travelplanner.ui.components.LoadingBox
import com.travelplanner.util.DateUtils
import com.travelplanner.util.Formatters

@Composable
fun TripEditScreen(
    onBack: () -> Unit,
    onSaved: (tripId: Long, isNew: Boolean) -> Unit,
    viewModel: TripEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedTripId) {
        state.savedTripId?.let { onSaved(it, viewModel.isNewTrip) }
    }

    TripEditContent(
        state = state,
        isNewTrip = viewModel.isNewTrip,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onDestinationChange = viewModel::onDestinationChange,
        onStartDateChange = viewModel::onStartDateChange,
        onEndDateChange = viewModel::onEndDateChange,
        onBudgetChange = viewModel::onBudgetChange,
        onCurrencyChange = viewModel::onCurrencyChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditContent(
    state: TripFormState,
    isNewTrip: Boolean,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onBudgetChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewTrip) "Новая поездка" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text("Название") },
                placeholder = { Text("Например, «Отпуск в Стамбуле»") },
                leadingIcon = { Icon(Icons.Filled.Luggage, contentDescription = null) },
                isError = state.errors.title != null,
                supportingText = state.errors.title?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_title"),
            )
            OutlinedTextField(
                value = state.destination,
                onValueChange = onDestinationChange,
                label = { Text("Куда") },
                placeholder = { Text("Город, страна") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                isError = state.errors.destination != null,
                supportingText = state.errors.destination?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_destination"),
            )

            DatePickerField(
                label = "Начало",
                epochDay = state.startDate,
                onDateSelected = onStartDateChange,
                leadingIcon = Icons.Filled.Event,
                errorText = state.errors.startDate,
                modifier = Modifier.fillMaxWidth(),
            )
            DatePickerField(
                label = "Окончание",
                epochDay = state.endDate,
                onDateSelected = onEndDateChange,
                leadingIcon = Icons.Filled.EventAvailable,
                errorText = state.errors.endDate,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.startDate != null && state.endDate != null && state.endDate >= state.startDate) {
                Text(
                    "Длительность: ${Formatters.days(DateUtils.durationDays(state.startDate, state.endDate).toLong())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = state.budgetText,
                    onValueChange = onBudgetChange,
                    label = { Text("Бюджет") },
                    leadingIcon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                    isError = state.errors.budget != null,
                    supportingText = state.errors.budget?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("field_budget"),
                )
                CurrencyPickerField(
                    currency = state.currency,
                    onCurrencySelected = onCurrencyChange,
                    modifier = Modifier.weight(0.75f),
                )
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text("Заметки") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_trip"),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isNewTrip) "Создать поездку" else "Сохранить")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
