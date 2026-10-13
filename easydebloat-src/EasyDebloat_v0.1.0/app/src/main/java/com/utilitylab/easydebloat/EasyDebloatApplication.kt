package com.utilitylab.easydebloat

import android.app.Application
import com.utilitylab.easydebloat.data.ActionHistoryStore
import com.utilitylab.easydebloat.data.DebloatRepository
import com.utilitylab.easydebloat.data.PackageScanner
import com.utilitylab.easydebloat.data.SettingsStore
import com.utilitylab.easydebloat.privilege.ShizukuController

class EasyDebloatApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.shizukuController.start()
    }
}

class AppContainer(app: Application) {
    val shizukuController = ShizukuController(app)
    val scanner = PackageScanner(app, shizukuController)
    val historyStore = ActionHistoryStore(app)
    val settingsStore = SettingsStore(app)
    val repository = DebloatRepository(shizukuController, scanner, historyStore)
}
