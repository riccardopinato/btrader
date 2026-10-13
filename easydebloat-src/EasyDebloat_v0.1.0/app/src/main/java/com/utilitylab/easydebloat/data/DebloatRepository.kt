package com.utilitylab.easydebloat.data

import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.model.DebloatPackage
import com.utilitylab.easydebloat.model.HistoryEntry
import com.utilitylab.easydebloat.privilege.ShellCommandResult

class DebloatRepository(
    private val operations: PackageOperationExecutor,
    private val scanner: PackageScanner,
    private val history: ActionHistoryStore
) {
    suspend fun scan(): PackageScanner.ScanResult = scanner.scan()

    suspend fun disable(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.DISABLE) {
        operations.execute(pkg.packageName, DebloatAction.DISABLE)
    }

    suspend fun enable(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.ENABLE) {
        operations.execute(pkg.packageName, DebloatAction.ENABLE)
    }

    suspend fun remove(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.REMOVE) {
        operations.execute(pkg.packageName, DebloatAction.REMOVE)
    }

    suspend fun restore(pkg: DebloatPackage): ShellCommandResult = operate(pkg, DebloatAction.RESTORE) {
        operations.execute(pkg.packageName, DebloatAction.RESTORE)
    }

    private suspend fun operate(
        pkg: DebloatPackage,
        action: DebloatAction,
        block: suspend () -> ShellCommandResult
    ): ShellCommandResult {
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
