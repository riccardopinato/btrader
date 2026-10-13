package com.utilitylab.easydebloat.util

import com.utilitylab.easydebloat.model.PackageState

object PackageStateResolver {
    fun resolve(packageName: String, installed: Set<String>, disabled: Set<String>): PackageState = when {
        packageName !in installed -> PackageState.REMOVED_FOR_USER
        packageName in disabled -> PackageState.DISABLED
        else -> PackageState.INSTALLED
    }
}
