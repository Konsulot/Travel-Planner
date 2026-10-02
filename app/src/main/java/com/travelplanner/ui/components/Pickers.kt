package com.travelplanner.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.travelplanner.util.Currencies
import com.travelplanner.util.DateUtils
import com.travelplanner.util.Formatters

/**
 * Поле «только для чтения», по нажатию на которое открывается выбор значения
 * (дата, время, валюта). Нажатие ловим через interactionSource, потому что
 * TextField сам перехватывает клики.
 */
@Composable
fun ClickableReadOnlyField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    placeholder: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) currentOnClick()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        interactionSource = interactionSource,
        modifier = modifier,
    )
}

/** Поле выбора даты с Material 3 DatePicker. Значение — epoch day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    epochDay: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    errorText: String? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    ClickableReadOnlyField(
        value = epochDay?.let { Formatters.date(it) } ?: "",
        label = label,
        placeholder = "Выберите дату",
        onClick = { showDialog = true },
        leadingIcon = leadingIcon,
        isError = errorText != null,
        supportingText = errorText,
        modifier = modifier,
    )

    if (showDialog) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (epochDay ?: DateUtils.today()).let(DateUtils::epochDayToUtcMillis),
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateSelected(DateUtils.utcMillisToEpochDay(it)) }
                        showDialog = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Поле выбора времени (необязательное). Значение — минуты от полуночи или null. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    minutes: Int?,
    onTimeSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    ClickableReadOnlyField(
        value = minutes?.let { Formatters.time(it) } ?: "",
        label = label,
        placeholder = "Без времени",
        onClick = { showDialog = true },
        leadingIcon = leadingIcon,
        trailingIcon = if (minutes != null) {
            {
                IconButton(onClick = { onTimeSelected(null) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Убрать время")
                }
            }
        } else {
            null
        },
        modifier = modifier,
    )

    if (showDialog) {
        val state = rememberTimePickerState(
            initialHour = (minutes ?: 9 * 60) / 60,
            initialMinute = (minutes ?: 0) % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Время") },
            text = { TimeInput(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(state.hour * 60 + state.minute)
                    showDialog = false
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            },
        )
    }
}

/** Универсальный выпадающий список поверх поля «только для чтения». */
@Composable
fun <T> OptionPickerField(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    selectedLabel: (T) -> String = optionLabel,
    leadingIcon: ImageVector? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        ClickableReadOnlyField(
            value = selectedLabel(selected),
            label = label,
            onClick = { expanded = true },
            leadingIcon = leadingIcon,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun CurrencyPickerField(
    currency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Валюта",
) {
    OptionPickerField(
        label = label,
        selected = currency,
        options = Currencies.all.map { it.code },
        optionLabel = { code -> "$code — ${Currencies.name(code)}" },
        selectedLabel = { code -> "$code ${Currencies.symbol(code)}" },
        onSelected = onCurrencySelected,
        modifier = modifier,
    )
}
