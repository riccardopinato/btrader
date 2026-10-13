package com.utilitylab.easydebloat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import com.utilitylab.easydebloat.ui.EasyDebloatApp
import com.utilitylab.easydebloat.ui.EasyDebloatViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: EasyDebloatViewModel by viewModels {
        val container = (application as EasyDebloatApplication).container
        EasyDebloatViewModel.Factory(
            repository = container.repository,
            shizukuController = container.shizukuController,
            historyStore = container.historyStore,
            settingsStore = container.settingsStore
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EasyDebloatApp(viewModel) }
    }
}
