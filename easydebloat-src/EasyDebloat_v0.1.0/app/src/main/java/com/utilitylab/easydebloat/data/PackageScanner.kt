package com.utilitylab.easydebloat.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.utilitylab.easydebloat.model.DebloatPackage
import com.utilitylab.easydebloat.model.DeviceProfile
import com.utilitylab.easydebloat.privilege.PrivilegedCommandExecutor
import com.utilitylab.easydebloat.util.PackageListParser
import com.utilitylab.easydebloat.util.PackageStateResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class PackageScanner(
    private val context: Context,
    private val shell: PrivilegedCommandExecutor
) {
    data class ScanResult(
        val device: DeviceProfile,
        val packages: List<DebloatPackage>
    )

    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        coroutineScope {
            val allDeferred = async { packageSet("pm list packages -s -u --user 0") }
            val installedDeferred = async { packageSet("pm list packages -s --user 0") }
            val disabledDeferred = async { packageSet("pm list packages -d --user 0") }
            val propsDeferred = async { readProperties() }

            val allSystem = allDeferred.await()
            val installed = installedDeferred.await()
            val disabled = disabledDeferred.await().intersect(allSystem)
            val props = propsDeferred.await()

            val packages = allSystem.map { packageName ->
                val catalog = DebloatCatalog.classify(packageName)
                val state = PackageStateResolver.resolve(packageName, installed, disabled)
                val label = resolveLabel(packageName) ?: catalog.appName
                DebloatPackage(
                    packageName = packageName,
                    appName = label,
                    description = catalog.description,
                    category = catalog.category,
                    risk = catalog.risk,
                    state = state,
                    warning = catalog.warning,
                    tags = catalog.tags
                )
            }.sortedWith(compareBy<DebloatPackage>({ riskOrder(it) }, { it.appName.lowercase() }))

            ScanResult(device = buildDeviceProfile(props), packages = packages)
        }
    }

    private suspend fun packageSet(command: String): Set<String> {
        val result = shell.exec(command)
        if (!result.success) throw IllegalStateException(result.output.ifBlank { "Comando fallito: $command" })
        return PackageListParser.parse(result.output)
    }

    private suspend fun readProperties(): Map<String, String> {
        val result = shell.exec("getprop")
        if (!result.success) return emptyMap()
        val regex = Regex("^\\[([^]]+)]\\s*:\\s*\\[([^]]*)]$")
        return result.output.lineSequence().mapNotNull { line ->
            regex.matchEntire(line.trim())?.let { it.groupValues[1] to it.groupValues[2] }
        }.toMap()
    }

    private fun buildDeviceProfile(props: Map<String, String>): DeviceProfile {
        val manufacturer = props["ro.product.manufacturer"].orEmpty().ifBlank { Build.MANUFACTURER }
        val model = props["ro.product.model"].orEmpty().ifBlank { Build.MODEL }
        val hyper = props["ro.mi.os.version.name"].orEmpty()
        val miui = props["ro.miui.ui.version.name"].orEmpty()
        val oneUi = props["ro.build.version.oneui"].orEmpty()
        val oplus = props["ro.build.version.oplusrom"].orEmpty()
        val emui = props["ro.build.version.emui"].orEmpty()
        val romName = when {
            hyper.isNotBlank() -> "HyperOS"
            miui.isNotBlank() -> "MIUI"
            oneUi.isNotBlank() -> "One UI"
            oplus.isNotBlank() -> "ColorOS/OxygenOS"
            emui.isNotBlank() -> "EMUI"
            else -> "Android"
        }
        val romVersion = listOf(hyper, miui, oneUi, oplus, emui).firstOrNull { it.isNotBlank() }
            ?: props["ro.build.version.incremental"].orEmpty()
        return DeviceProfile(
            manufacturer = manufacturer,
            brand = Build.BRAND,
            model = model,
            androidVersion = Build.VERSION.RELEASE,
            sdk = Build.VERSION.SDK_INT,
            romName = romName,
            romVersion = romVersion,
            buildId = props["ro.build.display.id"].orEmpty().ifBlank { Build.DISPLAY }
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveLabel(packageName: String): String? {
        return runCatching {
            val pm = context.packageManager
            val info: ApplicationInfo = if (Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
            } else {
                pm.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            }
            pm.getApplicationLabel(info).toString().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    companion object {
        private fun riskOrder(pkg: DebloatPackage): Int = when (pkg.risk) {
            com.utilitylab.easydebloat.model.RiskLevel.SAFE -> 0
            com.utilitylab.easydebloat.model.RiskLevel.CAUTION -> 1
            com.utilitylab.easydebloat.model.RiskLevel.UNKNOWN -> 2
            com.utilitylab.easydebloat.model.RiskLevel.DANGEROUS -> 3
        }
    }
}
