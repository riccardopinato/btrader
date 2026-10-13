package com.utilitylab.easydebloat.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.model.DebloatPackage
import com.utilitylab.easydebloat.model.DefaultMode
import com.utilitylab.easydebloat.model.HistoryEntry
import com.utilitylab.easydebloat.model.HomeFilter
import com.utilitylab.easydebloat.model.PackageState
import com.utilitylab.easydebloat.model.RiskLevel
import com.utilitylab.easydebloat.privilege.ShizukuController
import com.utilitylab.easydebloat.ui.theme.EasyDebloatTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MainSection(val label: String, val glyph: String) {
    CLEAN("Pulisci", "✦"), HISTORY("Cronologia", "↶"), SETTINGS("Impostazioni", "⚙")
}

private data class ConfirmAction(
    val packages: List<DebloatPackage>,
    val action: DebloatAction
)

@Composable
fun EasyDebloatApp(viewModel: EasyDebloatViewModel) {
    val state by viewModel.state.collectAsState()
    EasyDebloatTheme(dynamicColor = state.settings.dynamicColor) {
        var section by remember { mutableStateOf(MainSection.CLEAN) }
        val snackbarHost = remember { SnackbarHostState() }

        LaunchedEffect(state.snackbar) {
            val event = state.snackbar ?: return@LaunchedEffect
            val result = snackbarHost.showSnackbar(
                message = event.message,
                actionLabel = if (event.undoAction != null) "ANNULLA" else null
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
            viewModel.consumeSnackbar()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                NavigationBar {
                    MainSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = section == item,
                            onClick = { section = item },
                            icon = { Text(item.glyph, fontSize = 19.sp) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (section) {
                    MainSection.CLEAN -> CleanScreen(state, viewModel)
                    MainSection.HISTORY -> HistoryScreen(state.history)
                    MainSection.SETTINGS -> SettingsScreen(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun CleanScreen(state: EasyDebloatUiState, viewModel: EasyDebloatViewModel) {
    if (state.shizuku.status != ShizukuController.Status.READY) {
        ShizukuSetupScreen(state.shizuku, viewModel::requestShizukuPermission, viewModel::retryShizuku)
        return
    }

    var detail by remember { mutableStateOf<DebloatPackage?>(null) }
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Easy Debloat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Pulisci il telefono senza root.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill("● Pronto", MaterialTheme.colorScheme.tertiary)
            }

            Spacer(Modifier.height(12.dp))
            DeviceSummaryCard(state)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Cerca app o package") },
                trailingIcon = {
                    if (state.scanning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("⌕", fontSize = 22.sp)
                }
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = state.filter == HomeFilter.RECOMMENDED, onClick = { viewModel.setFilter(HomeFilter.RECOMMENDED) }, label = { Text("Consigliate") })
                FilterChip(selected = state.filter == HomeFilter.ALL, onClick = { viewModel.setFilter(HomeFilter.ALL) }, label = { Text("Tutte") })
                FilterChip(selected = state.filter == HomeFilter.ATTENTION, onClick = { viewModel.setFilter(HomeFilter.ATTENTION) }, label = { Text("Attenzione") })
                FilterChip(selected = state.filter == HomeFilter.MANAGED, onClick = { viewModel.setFilter(HomeFilter.MANAGED) }, label = { Text("Gestite") })
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                ErrorCard(state.error, viewModel::clearError)
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("${state.visiblePackages.size} elementi", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::selectRecommendedVisible) { Text("Seleziona sicure") }
                TextButton(onClick = viewModel::scan, enabled = !state.scanning) { Text("Riscansiona") }
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            items(state.visiblePackages, key = { it.packageName }) { pkg ->
                PackageRow(
                    pkg = pkg,
                    checked = pkg.packageName in state.selectedPackages,
                    onChecked = { viewModel.toggleSelection(pkg) },
                    onOpen = { detail = pkg }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        if (state.selected.isNotEmpty()) {
            SelectionBar(
                count = state.selected.size,
                defaultMode = state.settings.defaultMode,
                busy = state.operationRunning,
                onClear = viewModel::clearSelection,
                onDisable = { confirm = ConfirmAction(state.selected, DebloatAction.DISABLE) },
                onRemove = { confirm = ConfirmAction(state.selected, DebloatAction.REMOVE) }
            )
        }
    }

    detail?.let { pkg ->
        PackageDetailSheet(
            pkg = pkg,
            busy = state.operationRunning,
            onDismiss = { detail = null },
            onAction = { action ->
                if (action == DebloatAction.REMOVE || action == DebloatAction.DISABLE) {
                    confirm = ConfirmAction(listOf(pkg), action)
                } else {
                    viewModel.execute(pkg, action)
                    detail = null
                }
            }
        )
    }

    confirm?.let { request ->
        ConfirmDialog(
            request = request,
            onDismiss = { confirm = null },
            onConfirm = {
                if (request.packages.size == 1) viewModel.execute(request.packages.first(), request.action)
                else viewModel.executeSelected(request.action)
                detail = null
                confirm = null
            }
        )
    }
}

@Composable
private fun DeviceSummaryCard(state: EasyDebloatUiState) {
    val device = state.device
    val safe = state.packages.count { it.risk == RiskLevel.SAFE && it.state == PackageState.INSTALLED }
    val managed = state.packages.count { it.state == PackageState.DISABLED || it.state == PackageState.REMOVED_FOR_USER }
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (device == null) {
                Text("Dispositivo", fontWeight = FontWeight.SemiBold)
                Text("Scansione in corso…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("${device.manufacturer} · ${device.model}", fontWeight = FontWeight.SemiBold)
                Text("${device.romName} ${device.romVersion} · Android ${device.androidVersion}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric(state.packages.size.toString(), "APP SISTEMA")
                SummaryMetric(safe.toString(), "SICURE")
                SummaryMetric(managed.toString(), "GESTITE")
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PackageRow(pkg: DebloatPackage, checked: Boolean, onChecked: () -> Unit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onChecked() }, enabled = !pkg.isProtected)
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(pkg.appName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(pkg.appName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pkg.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(pkg.category, style = MaterialTheme.typography.labelSmall)
                Text("·", color = MaterialTheme.colorScheme.outline)
                Text(stateLabel(pkg.state), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        RiskBadge(pkg.risk)
    }
    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun RiskBadge(risk: RiskLevel) {
    val (label, color) = when (risk) {
        RiskLevel.SAFE -> "● Sicura" to Color(0xFF2E7D32)
        RiskLevel.CAUTION -> "● Attenzione" to Color(0xFFF9A825)
        RiskLevel.DANGEROUS -> "● Critica" to MaterialTheme.colorScheme.error
        RiskLevel.UNKNOWN -> "● ?" to MaterialTheme.colorScheme.secondary
    }
    Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SelectionBar(
    count: Int,
    defaultMode: DefaultMode,
    busy: Boolean,
    onClear: () -> Unit,
    onDisable: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(shadowElevation = 8.dp, tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$count selezionate", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("Deseleziona") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (defaultMode == DefaultMode.DISABLE) {
                    Button(onClick = onDisable, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Disabilita") }
                    OutlinedButton(onClick = onRemove, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Rimuovi") }
                } else {
                    OutlinedButton(onClick = onDisable, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Disabilita") }
                    Button(onClick = onRemove, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Rimuovi") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageDetailSheet(pkg: DebloatPackage, busy: Boolean, onDismiss: () -> Unit, onAction: (DebloatAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pkg.appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(pkg.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RiskBadge(pkg.risk)
            }
            Spacer(Modifier.height(14.dp))
            Text(pkg.description)
            pkg.warning?.let {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
                    Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Stato: ${stateLabel(pkg.state)}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            if (pkg.isProtected) {
                Text("Componente protetto da Easy Debloat.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            } else {
                when (pkg.state) {
                    PackageState.INSTALLED -> {
                        FilledTonalButton(onClick = { onAction(DebloatAction.DISABLE) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("DISABILITA") }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onAction(DebloatAction.REMOVE) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("RIMUOVI PER QUESTO UTENTE") }
                    }
                    PackageState.DISABLED -> {
                        Button(onClick = { onAction(DebloatAction.ENABLE) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("RIABILITA") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { onAction(DebloatAction.REMOVE) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("RIMUOVI PER QUESTO UTENTE") }
                    }
                    PackageState.REMOVED_FOR_USER -> Button(onClick = { onAction(DebloatAction.RESTORE) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("RIPRISTINA") }
                    PackageState.UNKNOWN -> Unit
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(request: ConfirmAction, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val caution = request.packages.count { it.risk == RiskLevel.CAUTION || it.risk == RiskLevel.UNKNOWN }
    val isRemove = request.action == DebloatAction.REMOVE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isRemove) "Rimuovere ${request.packages.size} applicazioni?" else "Disabilitare ${request.packages.size} applicazioni?") },
        text = {
            Column {
                Text(if (isRemove) "Le app verranno rimosse solo dal profilo Android corrente. Gli APK di sistema restano normalmente disponibili per il ripristino." else "Le app resteranno installate ma non potranno essere eseguite finché non le riabiliti.")
                if (caution > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text("$caution componenti richiedono attenzione e possono rimuovere funzioni che utilizzi.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("CONTINUA") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ANNULLA") } }
    )
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer, maxLines = 3, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

@Composable
private fun ShizukuSetupScreen(state: ShizukuController.State, requestPermission: () -> Unit, retry: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Attiva l'accesso avanzato", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Easy Debloat usa Shizuku per gestire le app di sistema senza root.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        SetupStep("1", "Installa Shizuku")
        SetupStep("2", "Attiva Opzioni sviluppatore e Debug wireless")
        SetupStep("3", "Avvia Shizuku")
        SetupStep("4", "Torna qui e concedi l'accesso")
        Spacer(Modifier.height(20.dp))

        when (state.status) {
            ShizukuController.Status.NOT_INSTALLED -> {
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) }, modifier = Modifier.fillMaxWidth()) { Text("INSTALLA SHIZUKU") }
            }
            ShizukuController.Status.PERMISSION_REQUIRED -> {
                Button(onClick = requestPermission, modifier = Modifier.fillMaxWidth()) { Text("CONCEDI ACCESSO") }
                Spacer(Modifier.height(8.dp))
                OpenShizukuButton()
            }
            ShizukuController.Status.CONNECTING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Connessione al servizio privilegiato…")
                }
            }
            ShizukuController.Status.NOT_RUNNING -> OpenShizukuButton()
            ShizukuController.Status.ERROR -> {
                Text(state.message ?: "Errore Shizuku", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = retry, modifier = Modifier.fillMaxWidth()) { Text("RIPROVA") }
            }
            ShizukuController.Status.READY -> Unit
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = retry, modifier = Modifier.fillMaxWidth()) { Text("RIPROVA") }
    }
}

@Composable
private fun OpenShizukuButton() {
    val context = LocalContext.current
    Button(
        onClick = {
            val launch = context.packageManager.getLaunchIntentForPackage(ShizukuController.MANAGER_PACKAGE)
            if (launch != null) context.startActivity(launch)
            else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/")))
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("APRI SHIZUKU") }
}

@Composable
private fun SetupStep(number: String, text: String) {
    Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(number, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
private fun HistoryScreen(history: List<HistoryEntry>) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text("Cronologia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Ultime operazioni eseguite sul dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nessuna operazione ancora.") }
        } else {
            LazyColumn {
                items(history, key = { "${it.timestamp}-${it.packageName}-${it.action}" }) { item ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.appName, fontWeight = FontWeight.SemiBold)
                            Text(item.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(actionLabel(item), fontWeight = FontWeight.SemiBold, color = if (item.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text(formatTime(item.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: EasyDebloatUiState, viewModel: EasyDebloatViewModel) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(viewModel.buildBackupText()) }
            }.onSuccess { Toast.makeText(context, "Backup esportato", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Esportazione non riuscita", Toast.LENGTH_SHORT).show() }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("Modalità predefinita", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.settings.defaultMode == DefaultMode.DISABLE, onClick = { viewModel.setDefaultMode(DefaultMode.DISABLE) }, label = { Text("Disabilita") })
            FilterChip(selected = state.settings.defaultMode == DefaultMode.REMOVE, onClick = { viewModel.setDefaultMode(DefaultMode.REMOVE) }, label = { Text("Rimuovi") })
        }
        Text("Disabilita è la scelta più conservativa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(18.dp))
        SettingSwitch("Mostra pacchetti non classificati", state.settings.showUnknown, viewModel::setShowUnknown)
        SettingSwitch("Mostra componenti critici", state.settings.showCritical, viewModel::setShowCritical)
        SettingSwitch("Colori dinamici", state.settings.dynamicColor, viewModel::setDynamicColor)
        SettingSwitch("Feedback aptico", state.settings.haptics, viewModel::setHaptics)

        Spacer(Modifier.height(20.dp))
        Text("Shizuku", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Stato: ${state.shizuku.status}")
        Text("Server: v${state.shizuku.serverVersion ?: "—"} · UID ${state.shizuku.serverUid ?: "—"}")
        Text("UserService UID: ${state.shizuku.serviceUid ?: "—"}")
        Spacer(Modifier.height(8.dp))
        OpenShizukuButton()

        Spacer(Modifier.height(20.dp))
        Text("Dati", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { exportLauncher.launch("easy-debloat-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.txt") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ESPORTA BACKUP") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::clearHistory, modifier = Modifier.fillMaxWidth()) { Text("CANCELLA CRONOLOGIA") }

        Spacer(Modifier.height(20.dp))
        Text("Easy Debloat 0.1.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Offline-first · nessun account · nessuna telemetria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

private fun stateLabel(state: PackageState): String = when (state) {
    PackageState.INSTALLED -> "Installata"
    PackageState.DISABLED -> "Disabilitata"
    PackageState.REMOVED_FOR_USER -> "Rimossa"
    PackageState.UNKNOWN -> "Sconosciuto"
}

private fun actionLabel(entry: HistoryEntry): String = when (entry.action) {
    DebloatAction.DISABLE -> if (entry.success) "Disabilitata" else "Errore disable"
    DebloatAction.ENABLE -> if (entry.success) "Riabilitata" else "Errore enable"
    DebloatAction.REMOVE -> if (entry.success) "Rimossa" else "Errore rimozione"
    DebloatAction.RESTORE -> if (entry.success) "Ripristinata" else "Errore ripristino"
}

private fun formatTime(timestamp: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
