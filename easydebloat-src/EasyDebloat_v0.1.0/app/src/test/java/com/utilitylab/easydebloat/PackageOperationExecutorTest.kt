package com.utilitylab.easydebloat

import com.utilitylab.easydebloat.data.PackageOperationExecutor
import com.utilitylab.easydebloat.model.DebloatAction
import com.utilitylab.easydebloat.privilege.PrivilegedCommandExecutor
import com.utilitylab.easydebloat.privilege.ShellCommandResult
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageOperationExecutorTest {
    @Test
    fun disableContinuesAfterBestEffortForceStopFailure() {
        val shell = FakeShell(
            mapOf(
                "am force-stop --user 0 com.example.app" to ShellCommandResult(1, "force-stop denied"),
                "pm disable-user --user 0 com.example.app" to ShellCommandResult(0, "disabled")
            )
        )
        val result = runSuspend {
            PackageOperationExecutor(shell).execute("com.example.app", DebloatAction.DISABLE)
        }

        assertTrue(result.success)
        assertEquals(
            listOf(
                "am force-stop --user 0 com.example.app",
                "pm disable-user --user 0 com.example.app"
            ),
            shell.commands
        )
        assertTrue(result.output.contains("warning exit=1"))
    }

    @Test
    fun enableNoLongerForceStopsAfterSuccessfulStateChange() {
        val shell = FakeShell(
            mapOf("pm enable --user 0 com.example.app" to ShellCommandResult(0, "enabled"))
        )

        val result = runSuspend {
            PackageOperationExecutor(shell).execute("com.example.app", DebloatAction.ENABLE)
        }

        assertTrue(result.success)
        assertEquals(listOf("pm enable --user 0 com.example.app"), shell.commands)
    }

    @Test
    fun restoreStopsWhenInstallExistingFails() {
        val shell = FakeShell(
            mapOf(
                "cmd package install-existing --user 0 com.example.app" to
                    ShellCommandResult(5, "not installed")
            )
        )

        val result = runSuspend {
            PackageOperationExecutor(shell).execute("com.example.app", DebloatAction.RESTORE)
        }

        assertFalse(result.success)
        assertEquals(
            listOf("cmd package install-existing --user 0 com.example.app"),
            shell.commands
        )
    }

    @Test
    fun restoreSurfacesEnableFailureAfterInstallSucceeded() {
        val shell = FakeShell(
            mapOf(
                "cmd package install-existing --user 0 com.example.app" to ShellCommandResult(0, "installed"),
                "pm enable --user 0 com.example.app" to ShellCommandResult(7, "enable denied")
            )
        )

        val result = runSuspend {
            PackageOperationExecutor(shell).execute("com.example.app", DebloatAction.RESTORE)
        }

        assertFalse(result.success)
        assertEquals(7, result.exitCode)
        assertTrue(result.output.contains("enable fallito"))
        assertEquals(2, shell.commands.size)
    }

    @Test
    fun invalidPackageNeverReachesPrivilegedShell() {
        val shell = FakeShell(emptyMap())

        val result = runSuspend {
            PackageOperationExecutor(shell).execute("com.example;reboot", DebloatAction.REMOVE)
        }

        assertFalse(result.success)
        assertTrue(shell.commands.isEmpty())
    }

    private class FakeShell(
        private val results: Map<String, ShellCommandResult>
    ) : PrivilegedCommandExecutor {
        val commands = mutableListOf<String>()

        override suspend fun exec(command: String): ShellCommandResult {
            commands += command
            return results[command] ?: ShellCommandResult(0, "")
        }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        val latch = CountDownLatch(1)
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(value: Result<T>) {
                    result = value
                    latch.countDown()
                }
            }
        )
        latch.await()
        return result!!.getOrThrow()
    }
}
