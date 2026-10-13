package com.utilitylab.easydebloat.privilege

interface PrivilegedCommandExecutor {
    suspend fun exec(command: String): ShellCommandResult
}
