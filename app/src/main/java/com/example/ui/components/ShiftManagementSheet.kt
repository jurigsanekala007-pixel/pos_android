package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.ui.theme.PosError
import com.example.ui.theme.PosErrorContainer
import com.example.ui.theme.PosOnErrorContainer
import com.example.ui.theme.PosOnSuccessContainer
import com.example.ui.theme.PosOnWarningContainer
import com.example.ui.theme.PosSuccess
import com.example.ui.theme.PosSuccessContainer
import com.example.ui.theme.PosWarning
import com.example.ui.theme.PosWarningContainer
import com.example.util.FormatUtils
import com.example.util.ReceiptPaperSize
import com.example.util.ShiftReceiptPdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementBottomSheet(
    activeShift: CashierShiftEntity?,
    allShifts: List<CashierShiftEntity>,
    storeSettings: StoreSettingsEntity?,
    onStartShift: (startCash: Double, cashierName: String, notes: String) -> Unit,
    onAddCashMovement: (isCashIn: Boolean, amount: Double, note: String) -> Unit,
    onCloseShift: (actualCash: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showStartShiftDialog by remember { mutableStateOf(false) }
    var showCashMovementDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var shiftToPrint by remember { mutableStateOf<CashierShiftEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Manajemen Shift Kasir",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (activeShift != null) "Shift Aktif: ${activeShift.shiftNumber}" else "Tidak ada shift kasir yang aktif",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Shift Aktif") },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat Shift (${allShifts.size})") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Active Shift Tab
                if (activeShift != null) {
                    ActiveShiftContent(
                        shift = activeShift,
                        storeSettings = storeSettings,
                        onOpenCashMovement = { showCashMovementDialog = true },
                        onOpenCloseShift = { showCloseShiftDialog = true },
                        onPrintShift = { shiftToPrint = activeShift }
                    )
                } else {
                    NoActiveShiftContent(
                        storeSettings = storeSettings,
                        onOpenStartShift = { showStartShiftDialog = true }
                    )
                }
            } else {
                // Shift History Tab
                ShiftHistoryContent(
                    shifts = allShifts,
                    onPrintShift = { shiftToPrint = it }
                )
            }
        }
    }

    // Start Shift Dialog
    if (showStartShiftDialog) {
        StartShiftDialog(
            defaultCashierName = storeSettings?.cashierName ?: "Kasir",
            onConfirm = { startCash, cashier, note ->
                showStartShiftDialog = false
                onStartShift(startCash, cashier, note)
            },
            onDismiss = { showStartShiftDialog = false }
        )
    }

    // Cash Movement Dialog (Cash In / Out)
    if (showCashMovementDialog && activeShift != null) {
        CashMovementDialog(
            onConfirm = { isCashIn, amount, note ->
                showCashMovementDialog = false
                onAddCashMovement(isCashIn, amount, note)
            },
            onDismiss = { showCashMovementDialog = false }
        )
    }

    // Close Shift & Reconciliation Dialog
    if (showCloseShiftDialog && activeShift != null) {
        CloseShiftDialog(
            shift = activeShift,
            onConfirm = { actualCash, note ->
                showCloseShiftDialog = false
                onCloseShift(actualCash, note)
            },
            onDismiss = { showCloseShiftDialog = false }
        )
    }

    // Print Shift Dialog
    if (shiftToPrint != null) {
        PrintShiftSummaryDialog(
            shift = shiftToPrint!!,
            storeSettings = storeSettings,
            onDismiss = { shiftToPrint = null }
        )
    }
}

@Composable
private fun ActiveShiftContent(
    shift: CashierShiftEntity,
    storeSettings: StoreSettingsEntity?,
    onOpenCashMovement: () -> Unit,
    onOpenCloseShift: () -> Unit,
    onPrintShift: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PosSuccessContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = PosSuccess,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Shift Sedang Berlangsung", fontWeight = FontWeight.Bold, color = PosOnSuccessContainer)
                        Text("Kasir: ${shift.cashierName} • Buka: ${FormatUtils.formatDate(shift.startTime)}", style = MaterialTheme.typography.bodySmall, color = PosOnSuccessContainer.copy(alpha = 0.85f))
                    }
                }
            }
        }

        // Expected Cash in Drawer Hero Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Estimasi Uang Kas di Laci (Saldo Diharapkan)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = FormatUtils.formatRupiah(shift.expectedCash),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Saldo Awal (${FormatUtils.formatRupiah(shift.startCash)}) + Tunai (${FormatUtils.formatRupiah(shift.cashSales)}) + Masuk (${FormatUtils.formatRupiah(shift.cashIn)}) - Keluar (${FormatUtils.formatRupiah(shift.cashOut)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Financial Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftMetricCard(
                title = "Kas Awal (Modal)",
                value = FormatUtils.formatRupiah(shift.startCash),
                icon = Icons.Default.Payments,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            ShiftMetricCard(
                title = "Penjualan Tunai",
                value = FormatUtils.formatRupiah(shift.cashSales),
                icon = Icons.Default.Payments,
                containerColor = PosSuccessContainer.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftMetricCard(
                title = "Non-Tunai / QRIS",
                value = FormatUtils.formatRupiah(shift.nonCashSales),
                icon = Icons.Default.QrCode,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            ShiftMetricCard(
                title = "Total Transaksi",
                value = "${shift.transactionCount} Transaksi",
                icon = Icons.Default.ReceiptLong,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShiftMetricCard(
                title = "Kas Masuk (+)",
                value = FormatUtils.formatRupiah(shift.cashIn),
                icon = Icons.Default.ArrowDownward,
                containerColor = PosSuccessContainer.copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )
            ShiftMetricCard(
                title = "Kas Keluar (-)",
                value = FormatUtils.formatRupiah(shift.cashOut),
                icon = Icons.Default.ArrowUpward,
                containerColor = PosErrorContainer.copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenCashMovement,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kas Masuk/Keluar", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
                onClick = onPrintShift,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cetak Rekap", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Close Shift Main Button
        Button(
            onClick = onOpenCloseShift,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("close_shift_action_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tutup Shift & Rekonsiliasi Kas", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NoActiveShiftContent(
    storeSettings: StoreSettingsEntity?,
    onOpenStartShift: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "Belum Ada Shift Kasir yang Dibuka",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Buka shift kasir baru untuk mulai mencatat saldo modal awal di laci kasir, penjualan harian, dan rekonsiliasi keuangan penutupan toko.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onOpenStartShift,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("open_shift_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Shift Kasir Sekarang", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ShiftHistoryContent(
    shifts: List<CashierShiftEntity>,
    onPrintShift: (CashierShiftEntity) -> Unit
) {
    if (shifts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Belum ada riwayat shift yang tersimpan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shifts, key = { it.id }) { shift ->
                ShiftHistoryItemCard(
                    shift = shift,
                    onPrint = { onPrintShift(shift) }
                )
            }
        }
    }
}

@Composable
private fun ShiftHistoryItemCard(
    shift: CashierShiftEntity,
    onPrint: () -> Unit
) {
    val isOpen = shift.status == "OPEN"
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOpen) PosSuccessContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isOpen) "AKTIF" else "SELESAI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOpen) PosOnSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(shift.shiftNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }

                IconButton(onClick = onPrint, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Print, contentDescription = "Cetak Rekap", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }

            Text(
                text = "Kasir: ${shift.cashierName} • ${FormatUtils.formatDate(shift.startTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Kas Awal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatRupiah(shift.startCash), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Penjualan Tunai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatRupiah(shift.cashSales), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PosSuccess)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Kas Akhir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatRupiah(shift.actualCash ?: shift.expectedCash), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (shift.cashDifference != null && shift.cashDifference != 0.0) {
                val isSurplus = shift.cashDifference > 0
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSurplus) PosSuccessContainer else PosErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSurplus) "Surplus Kas: + ${FormatUtils.formatRupiah(shift.cashDifference)}" else "Defisit Kas: - ${FormatUtils.formatRupiah(Math.abs(shift.cashDifference))}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSurplus) PosOnSuccessContainer else PosOnErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShiftMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartShiftDialog(
    defaultCashierName: String,
    onConfirm: (startCash: Double, cashierName: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var startCashStr by remember { mutableStateOf("100000") }
    var cashierName by remember { mutableStateOf(defaultCashierName) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Shift Kasir Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Masukkan saldo modal awal yang tersedia di laci kasir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = cashierName,
                    onValueChange = { cashierName = it },
                    label = { Text("Nama Kasir Bertugas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = startCashStr,
                    onValueChange = { startCashStr = it },
                    label = { Text("Saldo Kas Awal di Laci (Modal)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_cash_input")
                )

                val quickAmounts = listOf(50000.0, 100000.0, 200000.0, 500000.0)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickAmounts.forEach { amt ->
                        OutlinedButton(
                            onClick = { startCashStr = amt.toInt().toString() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = if (startCashStr == amt.toInt().toString()) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Rp ${amt.toInt() / 1000}k", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Buka Shift (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cash = startCashStr.toDoubleOrNull() ?: 0.0
                    onConfirm(cash, cashierName.ifBlank { "Kasir" }, notes)
                },
                modifier = Modifier.testTag("confirm_open_shift_button")
            ) {
                Text("Buka Shift")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun CashMovementDialog(
    onConfirm: (isCashIn: Boolean, amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isCashIn by remember { mutableStateOf(true) }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isCashIn) "Tambah Kas Masuk (+)" else "Catat Kas Keluar (-)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isCashIn,
                        onClick = { isCashIn = true },
                        label = { Text("Kas Masuk (+)") },
                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = PosSuccess) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isCashIn,
                        onClick = { isCashIn = false },
                        label = { Text("Kas Keluar (-)") },
                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PosError) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Jumlah Uang (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan / Alasan") },
                    placeholder = { Text(if (isCashIn) "Contoh: Tambah uang kembalian" else "Contoh: Beli es batu, operasional") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(isCashIn, amount, note.ifBlank { if (isCashIn) "Kas Masuk" else "Kas Keluar" })
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun CloseShiftDialog(
    shift: CashierShiftEntity,
    onConfirm: (actualCash: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var actualCashStr by remember { mutableStateOf(shift.expectedCash.toInt().toString()) }
    var notes by remember { mutableStateOf("") }

    val actualCash = actualCashStr.toDoubleOrNull() ?: 0.0
    val diff = actualCash - shift.expectedCash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tutup Shift & Rekonsiliasi Kas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Hitung jumlah fisik uang tunai yang ada di dalam laci kasir saat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Expected by System Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estimasi Kas Sistem:")
                        Text(
                            FormatUtils.formatRupiah(shift.expectedCash),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Actual Cash Input
                OutlinedTextField(
                    value = actualCashStr,
                    onValueChange = { actualCashStr = it },
                    label = { Text("Uang Fisik Nyata di Laci (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actual_cash_input")
                )

                // Reconciliation Feedback Banner
                val bannerColor = when {
                    diff == 0.0 -> PosSuccessContainer
                    diff > 0.0 -> PosWarningContainer
                    else -> PosErrorContainer
                }
                val textColor = when {
                    diff == 0.0 -> PosOnSuccessContainer
                    diff > 0.0 -> PosOnWarningContainer
                    else -> PosOnErrorContainer
                }
                val message = when {
                    diff == 0.0 -> "✅ Saldo Pas & Cocok (Tidak Ada Selisih)"
                    diff > 0.0 -> "🟢 Surplus Kas: + ${FormatUtils.formatRupiah(diff)} (Uang Lebih)"
                    else -> "🔴 Defisit Kas: - ${FormatUtils.formatRupiah(Math.abs(diff))} (Uang Kurang)"
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bannerColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Penutupan Shift") },
                    placeholder = { Text("Alasan selisih uang atau catatan lainnya") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(actualCash, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_close_shift_button")
            ) {
                Text("Tutup Shift Sekarang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun PrintShiftSummaryDialog(
    shift: CashierShiftEntity,
    storeSettings: StoreSettingsEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPaperSize by remember { mutableStateOf(ReceiptPaperSize.THERMAL_58MM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cetak / Bagikan Rekap Shift", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Pilih format ukuran kertas:")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedPaperSize == ReceiptPaperSize.THERMAL_58MM,
                        onClick = { selectedPaperSize = ReceiptPaperSize.THERMAL_58MM },
                        label = { Text("58mm") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPaperSize == ReceiptPaperSize.THERMAL_80MM,
                        onClick = { selectedPaperSize = ReceiptPaperSize.THERMAL_80MM },
                        label = { Text("80mm") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPaperSize == ReceiptPaperSize.STANDARD_A4,
                        onClick = { selectedPaperSize = ReceiptPaperSize.STANDARD_A4 },
                        label = { Text("A4") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No. Shift: ${shift.shiftNumber}", fontWeight = FontWeight.Bold)
                        Text("Kasir: ${shift.cashierName}")
                        Text("Total Penjualan: ${FormatUtils.formatRupiah(shift.cashSales + shift.nonCashSales)}")
                        Text("Kas Akhir: ${FormatUtils.formatRupiah(shift.actualCash ?: shift.expectedCash)}")
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val pdfFile = ShiftReceiptPdfGenerator.generateShiftPdf(context, shift, storeSettings, selectedPaperSize)
                        if (pdfFile != null) {
                            ShiftReceiptPdfGenerator.shareShiftPdf(context, pdfFile, shift.shiftNumber)
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan")
                }

                Button(
                    onClick = {
                        val pdfFile = ShiftReceiptPdfGenerator.generateShiftPdf(context, shift, storeSettings, selectedPaperSize)
                        if (pdfFile != null) {
                            ShiftReceiptPdfGenerator.printShiftPdf(context, pdfFile, "Rekap Shift ${shift.shiftNumber}")
                        }
                    }
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cetak")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
