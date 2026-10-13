package com.utilitylab.easydebloat.privilege

import android.content.Context
import android.os.Process
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader

class EasyDebloatUserService : IEasyDebloatService.Stub {
    constructor() : super()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : super()

    override fun exec(command: String): String {
        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val exitCode = process.waitFor()
            "$exitCode\n$output"
        } catch (t: Throwable) {
            "-1\n${t.javaClass.simpleName}: ${t.message.orEmpty()}"
        }
    }

    override fun uid(): Int = Process.myUid()

    override fun destroy() {
        kotlin.system.exitProcess(0)
    }
}
