package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.ui.theme.PosError
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.util.FormatUtils

enum class DiscountType {
    NOMINAL,
    PERCENTAGE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDiscountDialog(
    cartItem: CartItem,
    onApplyDiscount: (discountAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var discountType by remember {
        mutableStateOf(if (cartItem.itemDiscount > 0) DiscountType.NOMINAL else DiscountType.PERCENTAGE)
    }
    var inputValue by remember {
        mutableStateOf(
            if (cartItem.itemDiscount > 0) cartItem.itemDiscount.toInt().toString() else ""
        )
    }

    val itemSubtotal = cartItem.subtotal
    val discountAmount = remember(discountType, inputValue, itemSubtotal) {
        val num = inputValue.toDoubleOrNull() ?: 0.0
        if (discountType == DiscountType.PERCENTAGE) {
            (itemSubtotal * (num / 100.0)).coerceIn(0.0, itemSubtotal)
        } else {
            num.coerceIn(0.0, itemSubtotal)
        }
    }
    val finalPrice = (itemSubtotal - discountAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Diskon Item Produk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(cartItem.product.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Discount Type Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = discountType == DiscountType.PERCENTAGE,
                        onClick = {
                            discountType = DiscountType.PERCENTAGE
                            inputValue = ""
                        },
                        label = { Text("Persentase (%)") },
                        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = discountType == DiscountType.NOMINAL,
                        onClick = {
                            discountType = DiscountType.NOMINAL
                            inputValue = ""
                        },
                        label = { Text("Nominal (Rp)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Preset Chips
                if (discountType == DiscountType.PERCENTAGE) {
                    val presets = listOf(5, 10, 15, 20, 25, 50)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.forEach { pct ->
                            OutlinedButton(
                                onClick = { inputValue = pct.toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = if (inputValue == pct.toString()) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text("$pct%", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    val presets = listOf(1000.0, 2000.0, 5000.0, 10000.0, 20000.0)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.forEach { amt ->
                            OutlinedButton(
                                onClick = { inputValue = amt.toInt().toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = if (inputValue == amt.toInt().toString()) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text("Rp ${amt.toInt() / 1000}k", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Custom Input TextField
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(if (discountType == DiscountType.PERCENTAGE) "Persentase Diskon (%)" else "Potongan Harga (Rp)") },
                    prefix = { if (discountType == DiscountType.NOMINAL) Text("Rp ") else null },
                    suffix = { if (discountType == DiscountType.PERCENTAGE) Text("%") else null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Calculation Preview Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PosSuccessContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Harga Awal (${cartItem.quantity}x):", style = MaterialTheme.typography.bodySmall)
                            Text(FormatUtils.formatRupiah(itemSubtotal), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Potongan Diskon:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PosError)
                            Text("- ${FormatUtils.formatRupiah(discountAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PosError)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Harga Akhir:", fontWeight = FontWeight.Bold)
                            Text(FormatUtils.formatRupiah(finalPrice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyDiscount(discountAmount)
                    onDismiss()
                },
                modifier = Modifier.testTag("apply_item_discount_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Terapkan")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cartItem.itemDiscount > 0) {
                    TextButton(
                        onClick = {
                            onApplyDiscount(0.0)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PosError)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Hapus")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDiscountDialog(
    currentDiscount: Double,
    cartSubtotal: Double,
    onApplyDiscount: (discountAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var discountType by remember {
        mutableStateOf(if (currentDiscount > 0) DiscountType.NOMINAL else DiscountType.PERCENTAGE)
    }
    var inputValue by remember {
        mutableStateOf(if (currentDiscount > 0) currentDiscount.toInt().toString() else "")
    }

    val discountAmount = remember(discountType, inputValue, cartSubtotal) {
        val num = inputValue.toDoubleOrNull() ?: 0.0
        if (discountType == DiscountType.PERCENTAGE) {
            (cartSubtotal * (num / 100.0)).coerceIn(0.0, cartSubtotal)
        } else {
            num.coerceIn(0.0, cartSubtotal)
        }
    }
    val finalTotal = (cartSubtotal - discountAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Diskon Total Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Potongan untuk seluruh keranjang", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = discountType == DiscountType.PERCENTAGE,
                        onClick = {
                            discountType = DiscountType.PERCENTAGE
                            inputValue = ""
                        },
                        label = { Text("Persentase (%)") },
                        leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = discountType == DiscountType.NOMINAL,
                        onClick = {
                            discountType = DiscountType.NOMINAL
                            inputValue = ""
                        },
                        label = { Text("Nominal (Rp)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Preset Chips
                if (discountType == DiscountType.PERCENTAGE) {
                    val presets = listOf(
                        5 to "Member 5%",
                        10 to "Promo 10%",
                        15 to "Weekend 15%",
                        20 to "Staff 20%"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.forEach { (pct, label) ->
                            OutlinedButton(
                                onClick = { inputValue = pct.toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = if (inputValue == pct.toString()) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    val presets = listOf(5000.0, 10000.0, 20000.0, 50000.0)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.forEach { amt ->
                            OutlinedButton(
                                onClick = { inputValue = amt.toInt().toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = if (inputValue == amt.toInt().toString()) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text("Rp ${amt.toInt() / 1000}k", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Custom Input TextField
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(if (discountType == DiscountType.PERCENTAGE) "Diskon Persen (%)" else "Potongan Diskon (Rp)") },
                    prefix = { if (discountType == DiscountType.NOMINAL) Text("Rp ") else null },
                    suffix = { if (discountType == DiscountType.PERCENTAGE) Text("%") else null },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Calculation Preview Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PosSuccessContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal Belanja:", style = MaterialTheme.typography.bodySmall)
                            Text(FormatUtils.formatRupiah(cartSubtotal), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Potongan Diskon:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PosError)
                            Text("- ${FormatUtils.formatRupiah(discountAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PosError)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal Setelah Diskon:", fontWeight = FontWeight.Bold)
                            Text(FormatUtils.formatRupiah(finalTotal), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyDiscount(discountAmount)
                    onDismiss()
                },
                modifier = Modifier.testTag("apply_transaction_discount_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Terapkan Diskon")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentDiscount > 0) {
                    TextButton(
                        onClick = {
                            onApplyDiscount(0.0)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PosError)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Hapus")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )
}
