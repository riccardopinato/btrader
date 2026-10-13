package com.utilitylab.easydebloat.util

object PackageListParser {
    fun parse(output: String): Set<String> = output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").substringBefore('=') }
        .filter { it.isNotBlank() }
        .toSet()
}
