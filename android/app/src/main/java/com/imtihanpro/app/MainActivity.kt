package com.imtihanpro.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imtihanpro.app.data.sync.SyncScheduler
import com.imtihanpro.app.ui.navigation.ImtihanNavGraph
import com.imtihanpro.app.ui.reader.PdfTextSearchEngine
import com.imtihanpro.app.ui.theme.ImtihanProTheme
import com.imtihanpro.app.ui.viewmodel.ImtihanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PdfTextSearchEngine.ensureInitialized(applicationContext)
        SyncScheduler.schedulePeriodicSync(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val viewModel: ImtihanViewModel = viewModel(
                factory = ImtihanViewModel.provideFactory(applicationContext)
            )
            val shellState by viewModel.shellState.collectAsState()

            DisposableEffect(shellState.isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = if (shellState.isDarkMode) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (shellState.isDarkMode) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            ImtihanProTheme(darkTheme = shellState.isDarkMode) {
                ImtihanNavGraph(viewModel = viewModel)
            }
        }
    }
}
