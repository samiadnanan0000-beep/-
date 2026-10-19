package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WafiViewModel
import com.example.ui.theme.CreditorOliveGreen
import com.example.ui.theme.DebtorCoralRed
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.OneUIBluePrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: WafiViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val isAdsRemoved by viewModel.billingManager.isAdsRemoved.collectAsState()
    val isSecurityEnabled by viewModel.securityManager.isSecurityEnabled.collectAsState()
    val isBiometricEnabled by viewModel.securityManager.isBiometricEnabled.collectAsState()
    val preferredCurrency by viewModel.preferredCurrency.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinText by remember { mutableStateOf("") }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Top Bar
            item {
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
                        text = "الإعدادات والترقيات والأمان",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Section 1: In-App Purchases & Upgrade
            item {
                SettingsSectionHeader(title = "نظام الترقية وإزالة الإعلانات")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(GoldAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = GoldAmber)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "النسخة الصافية مدى الحياة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAdsRemoved) "مفعلة مدى الحياة ✨" else "بدون إعلانات وميزات متقدمة",
                                        fontSize = 12.sp,
                                        color = if (isAdsRemoved) CreditorOliveGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isAdsRemoved) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "مفعل",
                                    tint = CreditorOliveGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isAdsRemoved) {
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        viewModel.billingManager.launchPurchaseFlow(activity)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAmber)
                            ) {
                                Text("شراء النسخة الخالصة (شراء دائم)", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Restore Purchases Button (Samsung One UI Pill design)
                        Button(
                            onClick = {
                                viewModel.billingManager.restorePurchases { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = OneUIBluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "استعادة المشتريات السابقة",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUIBluePrimary
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Security & App Lock
            item {
                SettingsSectionHeader(title = "الأمان والقفل")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Toggle Security
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(OneUIBluePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = OneUIBluePrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("تفعيل القفل وحماية السجل", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("حماية حساباتك برمز PIN أو البصمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Switch(
                                checked = isSecurityEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (viewModel.securityManager.hasPin()) {
                                            viewModel.securityManager.setSecurityEnabled(true)
                                        } else {
                                            showPinDialog = true
                                        }
                                    } else {
                                        viewModel.securityManager.setSecurityEnabled(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OneUIBluePrimary)
                            )
                        }

                        // Change PIN button if enabled
                        if (isSecurityEnabled || viewModel.securityManager.hasPin()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { showPinDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تغيير رمز المرور PIN", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("إعادة تعيين", fontSize = 12.sp, color = OneUIBluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Biometric Toggle if device supports it
                        if (viewModel.securityManager.canUseBiometrics()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(CreditorOliveGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CreditorOliveGreen, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("تسجيل الدخول بالبصمة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("فتح سريع عبر بصمة الإصبع أو الوجه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { viewModel.securityManager.setBiometricEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CreditorOliveGreen)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Currency & General
            item {
                SettingsSectionHeader(title = "العملة الأساسية وسقوف الائتمان")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCurrencyDialog = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(OneUIBluePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = OneUIBluePrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("العملة الرئيسية الافتراضية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("العملة المعتمدة لحساب الصافي العام", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = getCurrencyLabel(preferredCurrency),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUIBluePrimary
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Auto Backup & Search / Restore
            item {
                SettingsSectionHeader(title = "النسخ الاحتياطي والاستعادة")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "يتم حفظ النسخ الاحتياطية تلقائياً في مجلد آمن لا يتأثر بحذف التطبيق:\nDocuments/WafiBackup/",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Backup Now Button
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = viewModel.backupManager.createBackup()
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "تم إنشاء النسخة الاحتياطية بنجاح في مجلد المستندات", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "تعذر إنشاء النسخة: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OneUIBluePrimary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Backup, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنشاء نسخة احتياطية يدوية الآن", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search and Restore Button
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = viewModel.backupManager.scanAndRestoreBackup()
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "تمت استعادة كافة البيانات بنجاح من النسخة الاحتياطية ✨", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "لم يتم العثور على نسخة", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("البحث عن نسخة احتياطية على الهاتف واستعادتها", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("تعيين رمز المرور PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("أدخل رمز مرور سري مكون من 4 إلى 6 أرقام لحماية سجل وافي:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { if (it.length <= 6) newPinText = it },
                        placeholder = { Text("••••") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinText.length >= 4) {
                            viewModel.securityManager.setPin(newPinText)
                            viewModel.securityManager.setSecurityEnabled(true)
                            showPinDialog = false
                            newPinText = ""
                            Toast.makeText(context, "تم تعيين رمز المرور وتفعيل الحماية بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يجب أن يتكون الرمز من 4 أرقام على الأقل", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OneUIBluePrimary)
                ) {
                    Text("حفظ وتفعيل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Choose Currency Dialog
    if (showCurrencyDialog) {
        val currencyOptions = listOf("YER", "SAR", "USD", "GOLD_21K")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("اختر العملة الأساسية الافتراضية", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    currencyOptions.forEach { code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setPreferredCurrency(code)
                                    showCurrencyDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = getCurrencyLabel(code), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (preferredCurrency == code) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OneUIBluePrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = OneUIBluePrimary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 4.dp)
    )
}
