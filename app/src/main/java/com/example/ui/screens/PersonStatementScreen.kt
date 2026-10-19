package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PersonEntity
import com.example.data.TransactionEntity
import com.example.ui.WafiViewModel
import com.example.ui.components.ConfettiCanvas
import com.example.ui.theme.CreditorOliveGreen
import com.example.ui.theme.DebtorCoralRed
import com.example.ui.theme.OneUIBluePrimary
import com.example.util.PdfStatementExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PersonStatementScreen(
    personId: Long,
    viewModel: WafiViewModel,
    onBack: () -> Unit,
    onAddTransaction: (Long) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val allPersons by viewModel.repository.allPersonsFlow.collectAsState(initial = emptyList())
    val person = allPersons.firstOrNull { it.id == personId }

    val transactions by viewModel.repository.getTransactionsForPerson(personId)
        .collectAsState(initial = emptyList())

    val preferredCurrency by viewModel.preferredCurrency.collectAsState()
    var selectedCurrency by remember { mutableStateOf(preferredCurrency) }

    var showDeletePersonDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
    val currentBalance = currencyTransactions.sumOf {
        if (it.type == TransactionEntity.TYPE_DEBTOR_HIM) it.amount else -it.amount
    }
    val isCleared = abs(currentBalance) < 0.0001 && transactions.isNotEmpty()

    val isCreditLimitExceeded = person != null && person.creditLimit > 0 && currentBalance > person.creditLimit

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Top App Bar One UI style
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "كشف حساب تفصيلي",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { showDeletePersonDialog = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الشخص",
                            tint = Color(0xFFE5484D)
                        )
                    }
                }
            }

            // Person Profile Card
            item {
                if (person != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(OneUIBluePrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.name.take(1),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OneUIBluePrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = person.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (person.phone.isNotEmpty()) {
                                Text(
                                    text = person.phone,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Current Balance Display
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        when {
                                            isCleared -> CreditorOliveGreen.copy(alpha = 0.12f)
                                            currentBalance > 0 -> DebtorCoralRed.copy(alpha = 0.12f)
                                            else -> CreditorOliveGreen.copy(alpha = 0.12f)
                                        }
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val statusText = when {
                                        isCleared -> "الحساب مصفى تماماً"
                                        currentBalance > 0 -> "مستحق لك (عليه)"
                                        else -> "التزام عليك (له)"
                                    }
                                    val statusColor = if (currentBalance > 0 && !isCleared) DebtorCoralRed else CreditorOliveGreen

                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                    Text(
                                        text = "${formatAmount(abs(currentBalance))} ${getCurrencyLabel(selectedCurrency)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = statusColor
                                    )
                                }
                            }

                            // Credit Ceiling Warning
                            if (isCreditLimitExceeded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DebtorCoralRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = DebtorCoralRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تنبيه: تجاوز سقف الدين المحدد (${formatAmount(person.creditLimit)})!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DebtorCoralRed
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Pills Row: Call, WhatsApp, PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Call Button
                                ActionPillButton(
                                    icon = Icons.Default.Call,
                                    label = "اتصال",
                                    tint = OneUIBluePrimary,
                                    onClick = {
                                        if (person.phone.isNotEmpty()) {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                            context.startActivity(dialIntent)
                                        } else {
                                            Toast.makeText(context, "لم يتم تسجيل رقم هاتف لهذا الشخص", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                // WhatsApp Button
                                ActionPillButton(
                                    icon = Icons.Default.Chat,
                                    label = "واتساب",
                                    tint = CreditorOliveGreen,
                                    onClick = {
                                        if (person.phone.isNotEmpty()) {
                                            val cleanPhone = person.phone.replace("+", "").replace(" ", "")
                                            val balLabel = if (currentBalance > 0) "المبلغ المستحق عليكم" else "المبلغ المستحق لكم"
                                            val messageText = "السلام عليكم ورحمة الله وبركاته،\nأخي الكريم ${person.name}،\nنفيدكم بأن رصيدكم الحالي في حسابنا عبر تطبيق (وافي): $balLabel هو ${formatAmount(abs(currentBalance))} ${getCurrencyLabel(selectedCurrency)}.\nشاكرين ومقدرين حسن تعاملكم."
                                            val encodedMsg = Uri.encode(messageText)
                                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
                                            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            try {
                                                context.startActivity(waIntent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "يرجى إضافة رقم هاتف لإرسال كشف واتساب", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                // Export PDF Statement
                                ActionPillButton(
                                    icon = Icons.Default.PictureAsPdf,
                                    label = "كشف PDF",
                                    tint = Color(0xFF8B5CF6),
                                    onClick = {
                                        val res = PdfStatementExporter.generateAndSharePdf(
                                            context = context,
                                            person = person,
                                            transactions = currencyTransactions,
                                            currency = selectedCurrency
                                        )
                                        if (res.isFailure) {
                                            Toast.makeText(context, "حدث خطأ أثناء تصدير كشف PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Zero Balance Confetti Celebration Banner
            if (isCleared) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditorOliveGreen.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "حسابك وافي ومصفى ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreditorOliveGreen
                            )
                        }
                    }
                }
            }

            // Currency Switcher Pills
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    val availableCurrencies = listOf("YER", "SAR", "USD", "GOLD_21K", "SILVER")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableCurrencies) { code ->
                            val isSelected = selectedCurrency == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) OneUIBluePrimary else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedCurrency = code }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = getCurrencyLabel(code),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Cumulative Transaction Table Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل المعاملات التراكمي (${currencyTransactions.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Transactions Table Rows
            if (currencyTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد معاملات مسجلة لهذه العملة بعد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(currencyTransactions, key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        onDelete = { transactionToDelete = tx }
                    )
                }
            }
        }

        // Confetti Canvas Overlay when cleared
        if (isCleared) {
            ConfettiCanvas(modifier = Modifier.fillMaxSize())
        }

        // Floating Action Button to Add Transaction
        Button(
            onClick = { onAddTransaction(personId) },
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OneUIBluePrimary),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .height(54.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إضافة حركة جديدة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    // Delete Person Confirmation Dialog
    if (showDeletePersonDialog && person != null) {
        AlertDialog(
            onDismissRequest = { showDeletePersonDialog = false },
            title = { Text("حذف الحساب بالكامل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف ${person.name} وكافة حركاته المالية؟ لا يمكن التراجع عن هذه الخطوة.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePerson(person)
                        showDeletePersonDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D))
                ) {
                    Text("حذف نهائي", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePersonDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Single Transaction Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("حذف هذه الحركة", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف حركة ${formatAmount(tx.amount)}؟ سيتم إعادة احتساب الرصيد التراكمي آلياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D))
                ) {
                    Text("تأكيد الحذف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit
) {
    val isDebtor = transaction.type == TransactionEntity.TYPE_DEBTOR_HIM
    val typeColor = if (isDebtor) DebtorCoralRed else CreditorOliveGreen
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Type Indicator Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(typeColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = if (isDebtor) "عليه (مستحق لك)" else "له (سداد / التزام)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.details.ifEmpty { "حركة قيد" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormat.format(Date(transaction.date)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isDebtor) "+" else "-"}${formatAmount(transaction.amount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = typeColor
                    )
                    Text(
                        text = "الرصيد: ${formatAmount(transaction.balanceAfter)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف الحركة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
