package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.WafiViewModel
import com.example.ui.components.PopupCalculatorDialog
import com.example.ui.theme.CreditorOliveGreen
import com.example.ui.theme.DebtorCoralRed
import com.example.ui.theme.OneUIBluePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddTransactionScreen(
    initialPersonId: Long?,
    viewModel: WafiViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val allPersons by viewModel.repository.allPersonsFlow.collectAsState(initial = emptyList())
    val preferredCurrency by viewModel.preferredCurrency.collectAsState()

    var personName by remember {
        mutableStateOf(allPersons.firstOrNull { it.id == initialPersonId }?.name ?: "")
    }
    var personPhone by remember {
        mutableStateOf(allPersons.firstOrNull { it.id == initialPersonId }?.phone ?: "")
    }
    var selectedPersonId by remember { mutableStateOf(initialPersonId) }

    var selectedCurrency by remember { mutableStateOf(preferredCurrency) }
    var amountText by remember { mutableStateOf("") }
    var detailsText by remember { mutableStateOf("") }
    var showCalculator by remember { mutableStateOf(false) }

    // Matching persons for auto-suggest
    val matchingPersons = if (personName.isNotBlank() && selectedPersonId == null) {
        allPersons.filter { it.name.contains(personName.trim(), ignoreCase = true) }
    } else emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "إضافة قيد / حركة جديدة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Contact Name field & suggestions
                item {
                    Text(
                        text = "جهة التعامل (الشخص / العميل)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = personName,
                        onValueChange = {
                            personName = it
                            selectedPersonId = allPersons.firstOrNull { p -> p.name.equals(it.trim(), ignoreCase = true) }?.id
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("أدخل اسم الشخص أو اختر من السجل...") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = OneUIBluePrimary)
                        },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = OneUIBluePrimary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Auto-suggest chips
                    if (matchingPersons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(matchingPersons.take(5)) { match ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(OneUIBluePrimary.copy(alpha = 0.12f))
                                        .clickable {
                                            personName = match.name
                                            personPhone = match.phone
                                            selectedPersonId = match.id
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = match.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OneUIBluePrimary
                                    )
                                }
                            }
                        }
                    }

                    // Optional Phone input for new person
                    if (selectedPersonId == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = personPhone,
                            onValueChange = { personPhone = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("رقم الهاتف للتواصل وإرسال الكشف (اختياري)...") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = OneUIBluePrimary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Currency & Commodity Selector Tabs
                item {
                    Text(
                        text = "نوع العملة أو الصنف",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val currencyList = listOf(
                        "YER" to "محلي",
                        "SAR" to "سعودي",
                        "USD" to "دولار",
                        "GOLD_21K" to "ذهب عيار 21",
                        "SILVER" to "فضة"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currencyList) { (code, label) ->
                            val isSelected = selectedCurrency == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) OneUIBluePrimary else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedCurrency = code }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Amount Field with Popup Calculator Trigger
                item {
                    val unitLabel = when (selectedCurrency) {
                        "GOLD_21K", "SILVER" -> "الوزن بالغرام (g)"
                        else -> "المبلغ المالي"
                    }

                    Text(
                        text = unitLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        trailingIcon = {
                            // Calculator trigger icon
                            IconButton(
                                onClick = { showCalculator = true },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(OneUIBluePrimary.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "فتح الآلة الحاسبة",
                                    tint = OneUIBluePrimary
                                )
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = OneUIBluePrimary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Transaction Details / Note
                item {
                    Text(
                        text = "البيان أو تفاصيل المعاملة",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = detailsText,
                        onValueChange = { detailsText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثال: دفعة حساب، شراء خاتم ذهب، فاتورة بضاعة...") },
                        shape = RoundedCornerShape(20.dp),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = OneUIBluePrimary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val todayStr = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date())
                    Text(
                        text = "تاريخ المعاملة: اليوم ($todayStr)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dual Thumb-Accessible Bottom Buttons: "عليه" (Coral Red) and "له" (Olive Green)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // "عليه" Button (Coral Red - مستحق لك)
                    Button(
                        onClick = {
                            saveTransaction(
                                isDebtor = true,
                                name = personName,
                                phone = personPhone,
                                personId = selectedPersonId,
                                amountStr = amountText,
                                currency = selectedCurrency,
                                details = detailsText,
                                viewModel = viewModel,
                                context = context,
                                onDone = onBack
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DebtorCoralRed)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("عليه", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("(مستحق لك)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }

                    // "له" Button (Olive Green - التزام عليك / سداد)
                    Button(
                        onClick = {
                            saveTransaction(
                                isDebtor = false,
                                name = personName,
                                phone = personPhone,
                                personId = selectedPersonId,
                                amountStr = amountText,
                                currency = selectedCurrency,
                                details = detailsText,
                                viewModel = viewModel,
                                context = context,
                                onDone = onBack
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditorOliveGreen)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("له", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("(التزام عليك)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }
        }

        // Popup Calculator Dialog
        if (showCalculator) {
            PopupCalculatorDialog(
                initialValue = amountText,
                onDismiss = { showCalculator = false },
                onConfirm = { calculatedAmount ->
                    amountText = if (calculatedAmount % 1.0 == 0.0) {
                        calculatedAmount.toLong().toString()
                    } else {
                        String.format(Locale.ENGLISH, "%.2f", calculatedAmount)
                    }
                    showCalculator = false
                }
            )
        }
    }
}

private fun saveTransaction(
    isDebtor: Boolean,
    name: String,
    phone: String,
    personId: Long?,
    amountStr: String,
    currency: String,
    details: String,
    viewModel: WafiViewModel,
    context: android.content.Context,
    onDone: () -> Unit
) {
    if (name.isBlank()) {
        Toast.makeText(context, "يرجى إدخال اسم الشخص أولاً", Toast.LENGTH_SHORT).show()
        return
    }

    val amount = amountStr.toDoubleOrNull()
    if (amount == null || amount <= 0.0) {
        Toast.makeText(context, "يرجى إدخال مبلغ صالح أكبر من الصفر", Toast.LENGTH_SHORT).show()
        return
    }

    val type = if (isDebtor) TransactionEntity.TYPE_DEBTOR_HIM else TransactionEntity.TYPE_CREDITOR_ME

    if (personId != null && personId > 0) {
        viewModel.addTransaction(
            personId = personId,
            type = type,
            amount = amount,
            currency = currency,
            details = details.trim(),
            date = System.currentTimeMillis(),
            onDone = {
                Toast.makeText(context, "تم حفظ المعاملة بنجاح", Toast.LENGTH_SHORT).show()
                onDone()
            }
        )
    } else {
        // Create person first then transaction
        viewModel.addPerson(
            name = name.trim(),
            phone = phone.trim(),
            creditLimit = 0.0,
            notes = "",
            onDone = { newPersonId ->
                viewModel.addTransaction(
                    personId = newPersonId,
                    type = type,
                    amount = amount,
                    currency = currency,
                    details = details.trim(),
                    date = System.currentTimeMillis(),
                    onDone = {
                        Toast.makeText(context, "تم إضافة الحساب وحفظ المعاملة بنجاح", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
                )
            }
        )
    }
}
