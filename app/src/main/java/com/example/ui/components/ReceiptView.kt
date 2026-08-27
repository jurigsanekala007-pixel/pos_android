package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.dao.TransactionWithItems
import com.example.data.local.entity.StoreSettingsEntity
import com.example.ui.theme.PosError
import com.example.ui.theme.PosSuccess
import com.example.util.FormatUtils
import com.example.util.ReceiptPaperSize
import com.example.util.ReceiptPdfGenerator

@Composable
fun ReceiptDialog(
    transactionWithItems: TransactionWithItems,
    storeSettings: StoreSettingsEntity?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trx = transactionWithItems.transaction
    val isVoided = trx.status == "VOIDED"
    var selectedPaperSize by remember { mutableStateOf(ReceiptPaperSize.THERMAL_58MM) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isVoided) PosError.copy(alpha = 0.15f) else PosSuccess.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isVoided) Icons.Default.Close else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isVoided) PosError else PosSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isVoided) "Struk Dibatalkan (VOID)" else "Struk Transaksi POS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = trx.transactionNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Paper Size Selector (58mm Thermal, 80mm Thermal, A4)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Format Kertas Printer:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedPaperSize.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReceiptPaperSize.values().forEach { sizeOption ->
                                val isSelected = selectedPaperSize == sizeOption
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPaperSize = sizeOption },
                                    label = {
                                        Text(
                                            text = sizeOption.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Thermal Paper Scrollable Preview Container
                Surface(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                    color = Color(0xFFFAFAFA)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Store Info
                        Text(
                            text = storeSettings?.storeName ?: "KASIR POS OFFLINE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF1F2937)
                        )

                        if (!storeSettings?.address.isNullOrBlank()) {
                            Text(
                                text = storeSettings!!.address,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF4B5563)
                            )
                        }

                        if (!storeSettings?.phone.isNullOrBlank()) {
                            Text(
                                text = "Telp: ${storeSettings!!.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF4B5563)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Transaction Metadata
                        ReceiptMetaRow("No. Trx", trx.transactionNumber)
                        ReceiptMetaRow("Waktu", FormatUtils.formatDate(trx.timestamp))
                        ReceiptMetaRow("Kasir", trx.cashierName)
                        ReceiptMetaRow("Pelanggan", trx.customerName)
                        ReceiptMetaRow("Metode", trx.paymentMethod)
                        if (isVoided) {
                            ReceiptMetaRow("Status", "VOID / BATAL", isHighlight = true)
                            if (trx.voidReason.isNotBlank()) {
                                ReceiptMetaRow("Alasan", trx.voidReason, isHighlight = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items list
                        for (item in transactionWithItems.items) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF111827)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity} x ${FormatUtils.formatRupiah(item.sellingPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF4B5563)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(item.subtotal),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF111827)
                                )
                            }
                            if (item.itemDiscount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "  Diskon Item",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = PosError
                                    )
                                    Text(
                                        text = "-${FormatUtils.formatRupiah(item.itemDiscount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = PosError
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        DottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Calculation Breakdown
                        ReceiptAmountRow("Subtotal", FormatUtils.formatRupiah(trx.subtotal))
                        if (trx.discountAmount > 0) {
                            ReceiptAmountRow("Diskon Transaksi", "-${FormatUtils.formatRupiah(trx.discountAmount)}", isNegative = true)
                        }
                        if (trx.taxAmount > 0) {
                            ReceiptAmountRow("PPN (${storeSettings?.taxRate ?: 0}%)", FormatUtils.formatRupiah(trx.taxAmount))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = FormatUtils.formatRupiah(trx.totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (trx.paymentMethod == "TUNAI") {
                            ReceiptAmountRow("Bayar Tunai", FormatUtils.formatRupiah(trx.cashReceived))
                            ReceiptAmountRow("Kembalian", FormatUtils.formatRupiah(trx.cashChange), isBold = true)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        DottedDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Footer Note
                        Text(
                            text = storeSettings?.receiptFooter ?: "Terima Kasih Telah Berbelanja!",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF6B7280)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "* ${trx.transactionNumber} *",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF9CA3AF),
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Primary Printing & PDF Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Row: Cetak Bluetooth / Thermal Printer & Bagikan PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Print to Bluetooth Thermal Printer / Print Service
                        Button(
                            onClick = {
                                ReceiptPdfGenerator.printReceipt(
                                    context = context,
                                    transactionWithItems = transactionWithItems,
                                    storeSettings = storeSettings,
                                    paperSize = selectedPaperSize
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_thermal_receipt_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak Printer", fontWeight = FontWeight.Bold)
                        }

                        // Share Formatted PDF Receipt File
                        FilledTonalButton(
                            onClick = {
                                ReceiptPdfGenerator.shareReceiptPdf(
                                    context = context,
                                    transactionWithItems = transactionWithItems,
                                    storeSettings = storeSettings,
                                    paperSize = selectedPaperSize
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_pdf_receipt_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim PDF", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Secondary Action Row: Buka PDF, Bagikan Teks, & Tutup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ReceiptPdfGenerator.openReceiptPdf(
                                    context = context,
                                    transactionWithItems = transactionWithItems,
                                    storeSettings = storeSettings,
                                    paperSize = selectedPaperSize
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_pdf_receipt_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka PDF", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                val receiptText = FormatUtils.generateShareableReceipt(transactionWithItems, storeSettings)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, receiptText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Bagikan Teks Struk")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_text_receipt_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Teks WA", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("done_receipt_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("Tutup", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (isHighlight) PosError else Color(0xFF6B7280)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = if (isHighlight) PosError else Color(0xFF1F2937)
        )
    }
}

@Composable
private fun ReceiptAmountRow(label: String, value: String, isNegative: Boolean = false, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF4B5563)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = if (isNegative) PosError else Color(0xFF1F2937)
        )
    }
}

@Composable
private fun DottedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFD1D5DB),
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
