package com.utilitylab.easydebloat.ui

import com.utilitylab.easydebloat.model.AppSettings
import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.model.DebloatPackage
import com.utilitylab.easydebloat.model.DeviceProfile
import com.utilitylab.easydebloat.model.HistoryEntry
import com.utilitylab.easydebloat.model.HomeFilter
import com.utilitylab.easydebloat.model.RiskLevel
import com.utilitylab.easydebloat.model.PackageState
import com.utilitylab.easydebloat.privilege.ShizukuController

data class SnackbarEvent(
    val message: String,
    val undoPackage: DebloatPackage? = null,
    val undoAction: DebloatAction? = null
)

data class EasyDebloatUiState(
    val shizuku: ShizukuController.State = ShizukuController.State(),
    val packages: List<DebloatPackage> = emptyList(),
    val device: DeviceProfile? = null,
    val history: List<HistoryEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val query: String = "",
    val filter: HomeFilter = HomeFilter.RECOMMENDED,
    val selectedPackages: Set<String> = emptySet(),
    val scanning: Boolean = false,
    val operationRunning: Boolean = false,
    val error: String? = null,
    val snackbar: SnackbarEvent? = null
) {
    val visiblePackages: List<DebloatPackage>
        get() {
            val q = query.trim().lowercase()
            return packages.asSequence()
                .filter { settings.showCritical || it.risk != RiskLevel.DANGEROUS }
                .filter { settings.showUnknown || it.risk != RiskLevel.UNKNOWN }
                .filter { pkg ->
                    when (filter) {
                        HomeFilter.RECOMMENDED -> pkg.risk == RiskLevel.SAFE && pkg.state == PackageState.INSTALLED
                        HomeFilter.ALL -> true
                        HomeFilter.ATTENTION -> pkg.risk == RiskLevel.CAUTION || pkg.risk == RiskLevel.UNKNOWN
                        HomeFilter.MANAGED -> pkg.state == PackageState.DISABLED || pkg.state == PackageState.REMOVED_FOR_USER
                    }
                }
                .filter { pkg ->
                    q.isBlank() || listOf(pkg.appName, pkg.packageName, pkg.description, pkg.category)
                        .plus(pkg.tags)
                        .any { it.lowercase().contains(q) }
                }
                .toList()
        }

    val selected: List<DebloatPackage>
        get() = packages.filter { it.packageName in selectedPackages && !it.isProtected }
}
