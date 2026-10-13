package com.utilitylab.easydebloat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utilitylab.easydebloat.data.ActionHistoryStore
import com.utilitylab.easydebloat.data.DebloatRepository
import com.utilitylab.easydebloat.data.SettingsStore
import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.model.DebloatPackage
import com.utilitylab.easydebloat.model.DefaultMode
import com.utilitylab.easydebloat.model.HomeFilter
import com.utilitylab.easydebloat.model.RiskLevel
import com.utilitylab.easydebloat.privilege.ShizukuController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EasyDebloatViewModel(
    private val repository: DebloatRepository,
    private val shizukuController: ShizukuController,
    private val historyStore: ActionHistoryStore,
    private val settingsStore: SettingsStore
) : ViewModel() {
    private val _state = MutableStateFlow(EasyDebloatUiState())
    val state: StateFlow<EasyDebloatUiState> = _state.asStateFlow()
    private var lastReady = false

    init {
        viewModelScope.launch {
            shizukuController.state.collectLatest { shizuku ->
                _state.update { it.copy(shizuku = shizuku) }
                val ready = shizuku.status == ShizukuController.Status.READY
                if (ready && !lastReady) scan()
                lastReady = ready
            }
        }
        viewModelScope.launch {
            settingsStore.settings.collectLatest { settings -> _state.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch {
            historyStore.history.collectLatest { history -> _state.update { it.copy(history = history) } }
        }
    }

    fun requestShizukuPermission() = shizukuController.requestPermission()
    fun retryShizuku() = shizukuController.retry()

    fun scan() {
        if (_state.value.scanning || _state.value.shizuku.status != ShizukuController.Status.READY) return
        viewModelScope.launch {
            _state.update { it.copy(scanning = true, error = null) }
            runCatching { repository.scan() }
                .onSuccess { result ->
                    _state.update { current ->
                        current.copy(
                            scanning = false,
                            packages = result.packages,
                            device = result.device,
                            selectedPackages = current.selectedPackages.intersect(result.packages.map { it.packageName }.toSet())
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(scanning = false, error = error.message ?: "Scansione non riuscita") } }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun setFilter(filter: HomeFilter) = _state.update { it.copy(filter = filter) }

    fun toggleSelection(pkg: DebloatPackage) {
        if (pkg.isProtected) return
        _state.update { state ->
            val selected = state.selectedPackages.toMutableSet()
            if (!selected.add(pkg.packageName)) selected.remove(pkg.packageName)
            state.copy(selectedPackages = selected)
        }
    }

    fun clearSelection() = _state.update { it.copy(selectedPackages = emptySet()) }

    fun selectRecommendedVisible() {
        _state.update { state ->
            val safe = state.visiblePackages.filter { it.risk == RiskLevel.SAFE && !it.isProtected }.map { it.packageName }.toSet()
            state.copy(selectedPackages = state.selectedPackages + safe)
        }
    }

    fun execute(pkg: DebloatPackage, action: DebloatAction) {
        viewModelScope.launch { executeInternal(pkg, action, rescan = true) }
    }

    fun executeSelected(action: DebloatAction) {
        val items = _state.value.selected
        if (items.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(operationRunning = true, error = null) }
            var ok = 0
            val failures = mutableListOf<String>()
            for (pkg in items) {
                val result = when (action) {
                    DebloatAction.DISABLE -> repository.disable(pkg)
                    DebloatAction.REMOVE -> repository.remove(pkg)
                    DebloatAction.ENABLE -> repository.enable(pkg)
                    DebloatAction.RESTORE -> repository.restore(pkg)
                }
                if (result.success) ok++ else failures += "${pkg.appName}: ${result.output.lineSequence().firstOrNull().orEmpty()}"
            }
            _state.update {
                it.copy(
                    operationRunning = false,
                    selectedPackages = emptySet(),
                    snackbar = SnackbarEvent("Completate $ok/${items.size} operazioni"),
                    error = failures.takeIf { it.isNotEmpty() }?.joinToString("\n")
                )
            }
            scan()
        }
    }

    fun undo(event: SnackbarEvent) {
        val pkg = event.undoPackage ?: return
        val action = event.undoAction ?: return
        execute(pkg, action)
    }

    fun consumeSnackbar() = _state.update { it.copy(snackbar = null) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun setDefaultMode(mode: DefaultMode) = viewModelScope.launch { settingsStore.setDefaultMode(mode) }
    fun setShowUnknown(value: Boolean) = viewModelScope.launch { settingsStore.setShowUnknown(value) }
    fun setShowCritical(value: Boolean) = viewModelScope.launch { settingsStore.setShowCritical(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { settingsStore.setDynamicColor(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { settingsStore.setHaptics(value) }
    fun clearHistory() = viewModelScope.launch { historyStore.clear() }

    fun buildBackupText(): String {
        val current = _state.value
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        return buildString {
            appendLine("EASY DEBLOAT BACKUP")
            appendLine("version=0.1.0")
            appendLine("timestamp=$stamp")
            current.device?.let {
                appendLine("manufacturer=${it.manufacturer}")
                appendLine("model=${it.model}")
                appendLine("rom=${it.romName} ${it.romVersion}".trim())
            }
            appendLine()
            appendLine("[REMOVED]")
            current.packages.filter { it.state == com.utilitylab.easydebloat.model.PackageState.REMOVED_FOR_USER }
                .forEach { appendLine(it.packageName) }
            appendLine()
            appendLine("[DISABLED]")
            current.packages.filter { it.state == com.utilitylab.easydebloat.model.PackageState.DISABLED }
                .forEach { appendLine(it.packageName) }
            appendLine()
            appendLine("[HISTORY]")
            current.history.forEach { h ->
                appendLine("${h.timestamp}|${h.action}|${h.packageName}|${h.success}")
            }
        }
    }

    private suspend fun executeInternal(pkg: DebloatPackage, action: DebloatAction, rescan: Boolean) {
        if (_state.value.operationRunning) return
        _state.update { it.copy(operationRunning = true, error = null) }
        val result = when (action) {
            DebloatAction.DISABLE -> repository.disable(pkg)
            DebloatAction.ENABLE -> repository.enable(pkg)
            DebloatAction.REMOVE -> repository.remove(pkg)
            DebloatAction.RESTORE -> repository.restore(pkg)
        }
        val undo = when (action) {
            DebloatAction.DISABLE -> DebloatAction.ENABLE
            DebloatAction.REMOVE -> DebloatAction.RESTORE
            DebloatAction.ENABLE -> DebloatAction.DISABLE
            DebloatAction.RESTORE -> DebloatAction.REMOVE
        }
        _state.update {
            it.copy(
                operationRunning = false,
                snackbar = if (result.success) SnackbarEvent(successMessage(pkg, action), pkg, undo) else null,
                error = if (result.success) null else result.output.ifBlank { "Operazione non riuscita" }
            )
        }
        if (rescan) scan()
    }

    private fun successMessage(pkg: DebloatPackage, action: DebloatAction): String = when (action) {
        DebloatAction.DISABLE -> "${pkg.appName} disabilitata"
        DebloatAction.ENABLE -> "${pkg.appName} riabilitata"
        DebloatAction.REMOVE -> "${pkg.appName} rimossa per questo utente"
        DebloatAction.RESTORE -> "${pkg.appName} ripristinata"
    }

    class Factory(
        private val repository: DebloatRepository,
        private val shizukuController: ShizukuController,
        private val historyStore: ActionHistoryStore,
        private val settingsStore: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EasyDebloatViewModel(repository, shizukuController, historyStore, settingsStore) as T
        }
    }
}
