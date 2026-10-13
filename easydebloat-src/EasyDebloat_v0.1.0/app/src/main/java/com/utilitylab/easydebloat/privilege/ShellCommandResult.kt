package com.utilitylab.easydebloat.privilege

data class ShellCommandResult(
    val exitCode: Int,
    val output: String
) {
    val success: Boolean get() = exitCode == 0

    companion object {
        fun decode(raw: String): ShellCommandResult {
            val firstBreak = raw.indexOf('\n')
            if (firstBreak < 0) return ShellCommandResult(-1, raw)
            val code = raw.substring(0, firstBreak).trim().toIntOrNull() ?: -1
            return ShellCommandResult(code, raw.substring(firstBreak + 1))
        }
    }
}
