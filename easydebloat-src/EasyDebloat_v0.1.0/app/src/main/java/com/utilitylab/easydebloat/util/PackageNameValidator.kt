package com.utilitylab.easydebloat.util

object PackageNameValidator {
    private val pattern = Regex("^[A-Za-z0-9_.]+$")
    fun isValid(packageName: String): Boolean = packageName.isNotBlank() && pattern.matches(packageName)
}
