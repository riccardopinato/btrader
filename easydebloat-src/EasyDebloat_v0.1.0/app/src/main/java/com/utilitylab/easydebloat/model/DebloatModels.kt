package com.utilitylab.easydebloat.model

enum class RiskLevel { SAFE, CAUTION, DANGEROUS, UNKNOWN }
enum class PackageState { INSTALLED, DISABLED, REMOVED_FOR_USER, UNKNOWN }
enum class DebloatAction { DISABLE, ENABLE, REMOVE, RESTORE }
enum class DefaultMode { DISABLE, REMOVE }
enum class HomeFilter { RECOMMENDED, ALL, ATTENTION, MANAGED }

data class CatalogEntry(
    val packageName: String,
    val appName: String,
    val description: String,
    val category: String,
    val risk: RiskLevel,
    val warning: String? = null,
    val tags: Set<String> = emptySet()
)

data class DebloatPackage(
    val packageName: String,
    val appName: String,
    val description: String,
    val category: String,
    val risk: RiskLevel,
    val state: PackageState,
    val warning: String? = null,
    val tags: Set<String> = emptySet(),
    val selected: Boolean = false
) {
    val isProtected: Boolean get() = risk == RiskLevel.DANGEROUS
}

data class DeviceProfile(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val romName: String,
    val romVersion: String,
    val buildId: String
)

data class HistoryEntry(
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val action: DebloatAction,
    val success: Boolean,
    val message: String
)

data class AppSettings(
    val defaultMode: DefaultMode = DefaultMode.DISABLE,
    val showUnknown: Boolean = true,
    val showCritical: Boolean = true,
    val dynamicColor: Boolean = true,
    val haptics: Boolean = true
)
