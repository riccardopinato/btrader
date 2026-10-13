package com.utilitylab.easydebloat.data

import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.model.DebloatPackage
import com.utilitylab.easydebloat.model.HistoryEntry
import com.utilitylab.easydebloat.privilege.ShellCommandResult
import com.utilitylab.easydebloat.privilege.ShizukuController
import com.utilitylab.easydebloat.util.PackageNameValidator

class DebloatRepository(
    private val shizuku: ShizukuController,
    private val scanner: PackageScanner,
    private val history: ActionHistoryStore
) {
    suspend fun scan(): PackageScanner.ScanResult = scanner.scan()

    suspend fun disable(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.DISABLE) {
        shizuku.exec("am force-stop --user 0 ${pkg.packageName} && pm disable-user --user 0 ${pkg.packageName}")
    }

    suspend fun enable(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.ENABLE) {
        shizuku.exec("pm enable --user 0 ${pkg.packageName} && am force-stop --user 0 ${pkg.packageName}")
    }

    suspend fun remove(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.REMOVE) {
        shizuku.exec("am force-stop --user 0 ${pkg.packageName} && pm uninstall --user 0 ${pkg.packageName}")
    }

    suspend fun restore(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.RESTORE) {
        shizuku.exec("cmd package install-existing --user 0 ${pkg.packageName} && pm enable --user 0 ${pkg.packageName}")
    }

    private suspend fun operate(
        pkg: DebloatPackage,
        action: DebloatAction,
        block: suspend () -> ShellCommandResult
    ): ShellCommandResult {
        if (!PackageNameValidator.isValid(pkg.packageName)) {
            return ShellCommandResult(-1, "Nome package non valido")
        }
        if (pkg.isProtected || DebloatCatalog.isHardBlocked(pkg.packageName)) {
            return ShellCommandResult(-1, "Componente protetto da Easy Debloat")
        }
        val result = runCatching { block() }.getOrElse { ShellCommandResult(-1, it.message.orEmpty()) }
        history.add(
            HistoryEntry(
                timestamp = System.currentTimeMillis(),
                packageName = pkg.packageName,
                appName = pkg.appName,
                action = action,
                success = result.success,
                message = result.output.trim().take(500)
            )
        )
        return result
    }
}
