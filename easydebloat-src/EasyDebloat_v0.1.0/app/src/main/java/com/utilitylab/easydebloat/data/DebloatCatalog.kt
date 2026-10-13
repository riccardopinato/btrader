package com.utilitylab.easydebloat.data

import com.utilitylab.easydebloat.model.CatalogEntry
import com.utilitylab.easydebloat.model.RiskLevel

object DebloatCatalog {
    private val hardBlocked = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.permissioncontroller",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.shell",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.android.documentsui",
        "com.android.externalstorage",
        "com.android.providers.media",
        "com.android.providers.media.module",
        "com.android.providers.downloads",
        "com.android.providers.downloads.ui",
        "com.android.providers.contacts",
        "com.android.providers.calendar",
        "com.android.keychain",
        "com.android.certinstaller",
        "com.android.networkstack",
        "com.google.android.networkstack",
        "com.android.networkstack.tethering",
        "com.google.android.networkstack.tethering",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.webview",
        "com.miui.securitycenter",
        "com.miui.home",
        "com.miui.packageinstaller",
        "com.xiaomi.xmsf",
        "com.xiaomi.xmsfkeeper",
        "com.xiaomi.account"
    )

    private fun entry(
        pkg: String,
        name: String,
        description: String,
        category: String,
        risk: RiskLevel,
        warning: String? = null,
        vararg tags: String
    ) = CatalogEntry(pkg, name, description, category, risk, warning, tags.toSet())

    private val known = listOf(
        entry("com.miui.videoplayer", "Mi Video", "Player video Xiaomi preinstallato.", "Media", RiskLevel.SAFE, null, "xiaomi", "video"),
        entry("com.miui.player", "Mi Music", "Player musicale Xiaomi preinstallato.", "Media", RiskLevel.SAFE, null, "xiaomi", "music"),
        entry("com.miui.android.fashiongallery", "Wallpaper Carousel", "Servizio Xiaomi per sfondi e contenuti promozionali.", "Personalizzazione", RiskLevel.SAFE, null, "xiaomi", "wallpaper"),
        entry("com.miui.systemAdSolution", "MIUI System Ads", "Componente collegato al sistema pubblicitario MIUI.", "Pubblicità", RiskLevel.SAFE, null, "xiaomi", "ads"),
        entry("com.miui.analytics", "MIUI Analytics", "Componente analytics Xiaomi.", "Analytics", RiskLevel.SAFE, null, "xiaomi", "analytics"),
        entry("com.miui.analytics.go", "MIUI Analytics Go", "Variante del componente analytics Xiaomi.", "Analytics", RiskLevel.SAFE, null, "xiaomi", "analytics"),
        entry("com.xiaomi.mipicks", "GetApps", "Store applicazioni Xiaomi.", "Store", RiskLevel.SAFE, null, "xiaomi", "store"),
        entry("com.mi.global.pocobbs", "POCO Community", "Applicazione community POCO.", "Community", RiskLevel.SAFE, null, "poco", "community"),
        entry("com.mi.global.pocostore", "POCO Store", "Store POCO.", "Store", RiskLevel.SAFE, null, "poco", "store"),
        entry("com.miui.hybrid", "Quick Apps", "Framework Xiaomi Quick Apps.", "Quick Apps", RiskLevel.SAFE, null, "xiaomi", "quick apps"),
        entry("com.miui.hybrid.accessory", "Quick Apps Accessory", "Componente accessorio di Xiaomi Quick Apps.", "Quick Apps", RiskLevel.SAFE, null, "xiaomi", "quick apps"),
        entry("com.miui.bugreport", "Services & Feedback", "Componente Xiaomi per feedback e segnalazioni.", "Diagnostica", RiskLevel.SAFE, null, "xiaomi", "feedback"),

        entry("com.miui.compass", "Mi Compass", "Bussola Xiaomi.", "Strumenti", RiskLevel.CAUTION, "La rimozione elimina la bussola Xiaomi."),
        entry("com.duokan.phone.remotecontroller", "Mi Remote", "Telecomando a infrarossi Xiaomi.", "Strumenti", RiskLevel.CAUTION, "Potresti perdere il controllo IR integrato."),
        entry("com.miui.screenrecorder", "Screen Recorder", "Registratore schermo Xiaomi.", "Strumenti", RiskLevel.CAUTION, "La registrazione schermo di sistema non sarà disponibile."),
        entry("com.miui.fm", "FM Radio", "Radio FM Xiaomi.", "Media", RiskLevel.CAUTION, "La radio FM non sarà disponibile."),
        entry("com.milink.service", "Mi Link / Cast", "Servizi Xiaomi per casting e comunicazione multimediale.", "Connettività", RiskLevel.CAUTION, "Il casting Xiaomi potrebbe smettere di funzionare."),
        entry("com.miui.weather2", "Weather", "Meteo Xiaomi.", "Informazioni", RiskLevel.CAUTION, "Widget e meteo Xiaomi potrebbero non funzionare."),
        entry("com.miui.notes", "Mi Notes", "Applicazione Note Xiaomi.", "Produttività", RiskLevel.CAUTION, "Verifica di aver esportato eventuali note locali."),
        entry("com.miui.calculator", "Calculator", "Calcolatrice Xiaomi.", "Strumenti", RiskLevel.CAUTION),
        entry("com.miui.yellowpage", "Yellow Pages", "Servizi identificazione e pagine gialle MIUI.", "Telefono", RiskLevel.CAUTION),
        entry("com.miui.cloudbackup", "Xiaomi Cloud Backup", "Backup cloud Xiaomi.", "Cloud", RiskLevel.CAUTION, "I backup Xiaomi Cloud potrebbero interrompersi."),
        entry("com.miui.cloudservice", "Xiaomi Cloud", "Servizio Xiaomi Cloud.", "Cloud", RiskLevel.CAUTION, "Sincronizzazione e servizi Xiaomi Cloud possono dipendere da questo pacchetto."),
        entry("com.miui.micloudsync", "Xiaomi Cloud Sync", "Sincronizzazione Xiaomi Cloud.", "Cloud", RiskLevel.CAUTION, "La sincronizzazione Xiaomi Cloud verrà compromessa."),
        entry("com.xiaomi.payment", "Xiaomi Payment", "Servizio pagamenti Xiaomi.", "Pagamenti", RiskLevel.CAUTION),
        entry("com.xiaomi.gamecenter.sdk.service", "Xiaomi Game Service", "Servizio Xiaomi Game Center.", "Gaming", RiskLevel.CAUTION)
    ).associateBy { it.packageName }

    fun classify(packageName: String): CatalogEntry {
        if (packageName in hardBlocked) {
            return CatalogEntry(
                packageName = packageName,
                appName = friendlyFallback(packageName),
                description = "Componente di sistema protetto da Easy Debloat.",
                category = "Sistema",
                risk = RiskLevel.DANGEROUS,
                warning = "Questo componente è critico e non può essere modificato da Easy Debloat."
            )
        }
        return known[packageName] ?: CatalogEntry(
            packageName = packageName,
            appName = friendlyFallback(packageName),
            description = "Easy Debloat non dispone ancora di informazioni sufficienti per classificare questo componente.",
            category = "Sistema",
            risk = RiskLevel.UNKNOWN,
            warning = "Non rimuoverlo senza sapere con certezza a cosa serve."
        )
    }

    fun isHardBlocked(packageName: String): Boolean = packageName in hardBlocked
    fun knownCount(): Int = known.size

    private fun friendlyFallback(packageName: String): String = packageName
        .substringAfterLast('.')
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
