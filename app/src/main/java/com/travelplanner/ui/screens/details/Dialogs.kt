package com.travelplanner.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.travelplanner.model.Expense
import com.travelplanner.model.ExpenseCategory
import com.travelplanner.model.PackingCategory
import com.travelplanner.model.PackingItem
import com.travelplanner.model.Place
import com.travelplanner.model.Trip
import com.travelplanner.model.TripEvent
import com.travelplanner.ui.components.CurrencyPickerField
import com.travelplanner.ui.components.DatePickerField
import com.travelplanner.ui.components.OptionPickerField
import com.travelplanner.ui.components.TimePickerField
import com.travelplanner.util.DateUtils
import com.travelplanner.util.Formatters
import com.travelplanner.util.Validators

/**
 * Общая оболочка диалогов редактирования: заголовок, прокручиваемое содержимое,
 * кнопки «Сохранить», «Отмена» и (для существующих записей) «Удалить».
 */
@Composable
private fun EditDialog(
    title: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content() }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("dialog_save")) {
                Text(if (isNew) "Добавить" else "Сохранить")
            }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

private fun amountToText(amount: Double): String =
    if (amount > 0) Formatters.number(amount).replace(" ", "") else ""

// ------------------------------------------------------------------------------------------
// Расход
// ------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseDialog(
    draft: Expense,
    trip: Trip,
    ratesAreFallback: Boolean,
    convert: (amount: Double, currency: String) -> Double?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, currency: String, category: ExpenseCategory, date: Long, note: String) -> Unit,
    onDelete: () -> Unit,
) {
    val isNew = draft.id == 0L
    var title by remember(draft) { mutableStateOf(draft.title) }
    var amountText by remember(draft) { mutableStateOf(amountToText(draft.amount)) }
    var currency by remember(draft) { mutableStateOf(draft.currency) }
    var category by remember(draft) { mutableStateOf(draft.category) }
    var date by remember(draft) { mutableLongStateOf(draft.date) }
    var note by remember(draft) { mutableStateOf(draft.note) }
    var submitted by remember(draft) { mutableStateOf(false) }

    val titleError = Validators.validateRequired(title, "Введите название")
    val amountError = Validators.validateAmount(amountText)
    val amount = Formatters.parseAmount(amountText)

    EditDialog(
        title = if (isNew) "Новый расход" else "Расход",
        isNew = isNew,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onConfirm = {
            submitted = true
            if (titleError == null && amountError == null && amount != null) {
                onSave(title, amount, currency, category, date, note)
            }
        },
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("На что") },
            placeholder = { Text("Например, обед в кафе") },
            isError = submitted && titleError != null,
            supportingText = titleError?.takeIf { submitted }?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expense_title"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.length <= 15) amountText = it },
                label = { Text("Сумма") },
                isError = submitted && amountError != null,
                supportingText = amountError?.takeIf { submitted }?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .testTag("expense_amount"),
            )
            CurrencyPickerField(
                currency = currency,
                onCurrencySelected = { currency = it },
                modifier = Modifier.weight(1f),
            )
        }

        if (currency != trip.currency && amount != null && amount > 0) {
            val converted = convert(amount, currency)
            Text(
                text = if (converted != null) {
                    "≈ ${Formatters.money(converted, trip.currency)}" + if (ratesAreFallback) " (примерный курс)" else ""
                } else {
                    "Нет курса для пересчёта в ${trip.currency}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (converted != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }

        Text("Категория", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseCategory.entries.forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text("${option.emoji} ${option.title}") },
                )
            }
        }

        DatePickerField(
            label = "Дата",
            epochDay = date,
            onDateSelected = { date = it },
            leadingIcon = Icons.Filled.Event,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Комментарий") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ------------------------------------------------------------------------------------------
// Событие маршрута
// ------------------------------------------------------------------------------------------

@Composable
fun EventDialog(
    draft: TripEvent,
    trip: Trip,
    onDismiss: () -> Unit,
    onSave: (TripEvent) -> Unit,
    onDelete: () -> Unit,
) {
    val isNew = draft.id == 0L
    var title by remember(draft) { mutableStateOf(draft.title) }
    var date by remember(draft) { mutableLongStateOf(draft.date) }
    var time by remember(draft) { mutableStateOf(draft.time) }
    var location by remember(draft) { mutableStateOf(draft.location) }
    var note by remember(draft) { mutableStateOf(draft.note) }
    var submitted by remember(draft) { mutableStateOf(false) }
    val titleError = Validators.validateRequired(title, "Введите название события")

    val days = remember(trip.startDate, trip.endDate, draft.date) {
        (DateUtils.tripDays(trip.startDate, trip.endDate) + draft.date).distinct().sorted()
    }

    fun dayLabel(day: Long): String =
        if (day in trip.startDate..trip.endDate) {
            "День ${DateUtils.dayNumber(day, trip.startDate)} · ${Formatters.dayHeader(day)}"
        } else {
            Formatters.dayHeader(day)
        }

    EditDialog(
        title = if (isNew) "Новое событие" else "Событие",
        isNew = isNew,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onConfirm = {
            submitted = true
            if (titleError == null) {
                onSave(draft.copy(title = title, date = date, time = time, location = location.trim(), note = note.trim()))
            }
        },
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Что делаем") },
            placeholder = { Text("Например, экскурсия по центру") },
            isError = submitted && titleError != null,
            supportingText = titleError?.takeIf { submitted }?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("event_title"),
        )
        OptionPickerField(
            label = "День",
            selected = date,
            options = days,
            optionLabel = ::dayLabel,
            onSelected = { date = it },
            leadingIcon = Icons.Filled.Today,
            modifier = Modifier.fillMaxWidth(),
        )
        TimePickerField(
            label = "Время",
            minutes = time,
            onTimeSelected = { time = it },
            leadingIcon = Icons.Filled.Schedule,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Где") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Заметка") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ------------------------------------------------------------------------------------------
// Место
// ------------------------------------------------------------------------------------------

@Composable
fun PlaceDialog(
    draft: Place,
    onDismiss: () -> Unit,
    onSave: (Place) -> Unit,
    onDelete: () -> Unit,
) {
    val isNew = draft.id == 0L
    var name by remember(draft) { mutableStateOf(draft.name) }
    var address by remember(draft) { mutableStateOf(draft.address) }
    var note by remember(draft) { mutableStateOf(draft.note) }
    var isWishlist by remember(draft) { mutableStateOf(draft.isWishlist) }
    var isVisited by remember(draft) { mutableStateOf(draft.isVisited) }
    var submitted by remember(draft) { mutableStateOf(false) }
    val nameError = Validators.validateRequired(name, "Введите название места")

    EditDialog(
        title = if (isNew) "Новое место" else "Место",
        isNew = isNew,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onConfirm = {
            submitted = true
            if (nameError == null) {
                onSave(
                    draft.copy(
                        name = name,
                        address = address.trim(),
                        note = note.trim(),
                        isWishlist = isWishlist,
                        isVisited = isVisited,
                    ),
                )
            }
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            placeholder = { Text("Музей, кафе, парк…") },
            isError = submitted && nameError != null,
            supportingText = nameError?.takeIf { submitted }?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("place_name"),
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Адрес") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Заметка") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        SwitchRow("В wishlist (обязательно посетить)", isWishlist) { isWishlist = it }
        SwitchRow("Уже посетили", isVisited) { isVisited = it }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ------------------------------------------------------------------------------------------
// Вещь из списка сборов
// ------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackingDialog(
    draft: PackingItem,
    onDismiss: () -> Unit,
    onSave: (PackingItem) -> Unit,
    onDelete: () -> Unit,
) {
    val isNew = draft.id == 0L
    var name by remember(draft) { mutableStateOf(draft.name) }
    var category by remember(draft) { mutableStateOf(draft.category) }
    var quantity by remember(draft) { mutableIntStateOf(draft.quantity.coerceAtLeast(1)) }
    var submitted by remember(draft) { mutableStateOf(false) }
    val nameError = Validators.validateRequired(name, "Введите название вещи")

    EditDialog(
        title = if (isNew) "Новая вещь" else "Вещь",
        isNew = isNew,
        onDismiss = onDismiss,
        onDelete = onDelete,
        onConfirm = {
            submitted = true
            if (nameError == null) onSave(draft.copy(name = name, category = category, quantity = quantity))
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Что взять") },
            isError = submitted && nameError != null,
            supportingText = nameError?.takeIf { submitted }?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("packing_name"),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Количество", modifier = Modifier.weight(1f))
            IconButton(onClick = { if (quantity > 1) quantity-- }, enabled = quantity > 1) {
                Icon(Icons.Filled.Remove, contentDescription = "Меньше")
            }
            Text(quantity.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { if (quantity < 99) quantity++ }) {
                Icon(Icons.Filled.Add, contentDescription = "Больше")
            }
        }
        Text("Категория", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PackingCategory.entries.forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text("${option.emoji} ${option.title}") },
                )
            }
        }
    }
}
