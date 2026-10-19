package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.OneUIBluePrimary
import java.util.Locale

@Composable
fun PopupCalculatorDialog(
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var display by remember { mutableStateOf(if (initialValue.isBlank() || initialValue == "0") "" else initialValue) }
    var operator by remember { mutableStateOf<String?>(null) }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var clearOnNextDigit by remember { mutableStateOf(false) }

    fun calculate(op1: Double, op2: Double, op: String): Double {
        return when (op) {
            "+" -> op1 + op2
            "-" -> op1 - op2
            "×", "*" -> op1 * op2
            "÷", "/" -> if (op2 != 0.0) op1 / op2 else 0.0
            else -> op2
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "آلة حاسبة وافي One UI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OneUIBluePrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = if (display.isEmpty()) "0" else display,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad Grid
                val buttonRows = listOf(
                    listOf("C", "÷", "×", "⌫"),
                    listOf("7", "8", "9", "-"),
                    listOf("4", "5", "6", "+"),
                    listOf("1", "2", "3", "="),
                    listOf("00", "0", ".", "OK")
                )

                buttonRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            val isAction = key in listOf("÷", "×", "-", "+", "=")
                            val isOk = key == "OK"
                            val isClear = key in listOf("C", "⌫")

                            val bgColor = when {
                                isOk -> OneUIBluePrimary
                                isAction -> MaterialTheme.colorScheme.primaryContainer
                                isClear -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }

                            val textColor = when {
                                isOk -> Color.White
                                isAction -> OneUIBluePrimary
                                isClear -> Color(0xFFE5484D)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bgColor)
                                    .clickable {
                                        when (key) {
                                            "C" -> {
                                                display = ""
                                                operator = null
                                                operand1 = null
                                            }
                                            "⌫" -> {
                                                if (display.isNotEmpty()) {
                                                    display = display.dropLast(1)
                                                }
                                            }
                                            "+", "-", "×", "÷" -> {
                                                val currVal = display.toDoubleOrNull() ?: 0.0
                                                if (operand1 != null && operator != null && !clearOnNextDigit) {
                                                    val res = calculate(operand1!!, currVal, operator!!)
                                                    display = if (res % 1.0 == 0.0) res.toInt().toString() else String.format(Locale.ENGLISH, "%.2f", res)
                                                    operand1 = res
                                                } else {
                                                    operand1 = currVal
                                                }
                                                operator = key
                                                clearOnNextDigit = true
                                            }
                                            "=" -> {
                                                if (operand1 != null && operator != null) {
                                                    val currVal = display.toDoubleOrNull() ?: 0.0
                                                    val res = calculate(operand1!!, currVal, operator!!)
                                                    display = if (res % 1.0 == 0.0) res.toInt().toString() else String.format(Locale.ENGLISH, "%.2f", res)
                                                    operand1 = null
                                                    operator = null
                                                    clearOnNextDigit = true
                                                }
                                            }
                                            "OK" -> {
                                                val finalVal = display.toDoubleOrNull() ?: 0.0
                                                onConfirm(finalVal)
                                            }
                                            else -> {
                                                if (clearOnNextDigit) {
                                                    display = key
                                                    clearOnNextDigit = false
                                                } else {
                                                    if (key == "." && display.contains(".")) {
                                                        // avoid duplicate decimal
                                                    } else {
                                                        display += key
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "مسح",
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = if (key == "OK") "تأكيد" else key,
                                        fontSize = if (key == "OK") 14.sp else 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
