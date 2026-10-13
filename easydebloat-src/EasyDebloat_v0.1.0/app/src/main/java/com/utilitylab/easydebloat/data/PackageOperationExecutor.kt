package com.utilitylab.easydebloat.data

import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.privilege.PrivilegedCommandExecutor
import com.utilitylab.easydebloat.privilege.ShellCommandResult
import com.utilitylab.easydebloat.util.PackageNameValidator

class PackageOperationExecutor(
    private val shell: PrivilegedCommandExecutor
) {
    data class Step(
        val command: String,
        val required: Boolean,
        val label: String
    )

    suspend fun execute(packageName: String, action: DebloatAction): ShellCommandResult {
        if (!PackageNameValidator.isValid(packageName)) {
            return ShellCommandResult(-1, "Nome package non valido")
        }

        val log = mutableListOf<String>()
        for (step in plan(packageName, action)) {
            val result = shell.exec(step.command)
            val output = result.output.trim()
            if (output.isNotBlank()) {
                log += "${step.label}: $output"
            }

            if (!result.success && step.required) {
                val message = buildString {
                    append("${step.label} fallito (exit=${result.exitCode})")
                    if (log.isNotEmpty()) append("\n${log.joinToString("\n")}")
                }
                return ShellCommandResult(result.exitCode, message)
            }

            if (!result.success && !step.required) {
                log += "${step.label}: warning exit=${result.exitCode}"
            }
        }

        return ShellCommandResult(0, log.joinToString("\n"))
    }

    internal fun plan(packageName: String, action: DebloatAction): List<Step> = when (action) {
        DebloatAction.DISABLE -> listOf(
            Step("am force-stop --user 0 $packageName", required = false, label = "force-stop"),
            Step("pm disable-user --user 0 $packageName", required = true, label = "disable")
        )
        DebloatAction.ENABLE -> listOf(
            Step("pm enable --user 0 $packageName", required = true, label = "enable")
        )
        DebloatAction.REMOVE -> listOf(
            Step("am force-stop --user 0 $packageName", required = false, label = "force-stop"),
            Step("pm uninstall --user 0 $packageName", required = true, label = "remove")
        )
        DebloatAction.RESTORE -> listOf(
            Step("cmd package install-existing --user 0 $packageName", required = true, label = "install-existing"),
            Step("pm enable --user 0 $packageName", required = true, label = "enable")
        )
    }
}
