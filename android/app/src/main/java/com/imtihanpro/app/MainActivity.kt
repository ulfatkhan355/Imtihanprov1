package com.imtihanpro.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imtihanpro.app.data.sync.PdfDownloadNotifications
import com.imtihanpro.app.data.sync.SyncScheduler
import com.imtihanpro.app.ui.navigation.ImtihanNavGraph
import com.imtihanpro.app.ui.reader.PdfTextSearchEngine
import com.imtihanpro.app.ui.theme.ImtihanProTheme
import com.imtihanpro.app.ui.viewmodel.ImtihanViewModel
import com.imtihanpro.app.ui.viewmodel.PdfDeepLinkRequest

class MainActivity : ComponentActivity() {
    private var launchDeepLink by mutableStateOf<PdfDeepLinkRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PdfTextSearchEngine.ensureInitialized(applicationContext)
        PdfDownloadNotifications.ensureChannel(applicationContext)
        SyncScheduler.registerConnectivityMonitor(applicationContext)
        SyncScheduler.schedulePeriodicSync(applicationContext)
        extractDeepLink(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val viewModel: ImtihanViewModel = viewModel(
                factory = ImtihanViewModel.provideFactory(applicationContext)
            )
            val shellState by viewModel.shellState.collectAsState()

            LaunchedEffect(launchDeepLink) {
                launchDeepLink?.let { req ->
                    viewModel.handleNotificationDeepLink(req.paperId, req.pdfId, req.title)
                    launchDeepLink = null
                }
            }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLink(intent)
    }

    private fun extractDeepLink(intent: Intent?) {
        if (intent == null) return
        val pdfId = intent.getStringExtra(PdfDownloadNotifications.EXTRA_OPEN_PDF_ID)
        if (!pdfId.isNullOrBlank()) {
            val paperId = intent.getStringExtra(PdfDownloadNotifications.EXTRA_OPEN_PAPER_ID) ?: pdfId
            val title = intent.getStringExtra(PdfDownloadNotifications.EXTRA_OPEN_PDF_TITLE) ?: "Offline PDF"
            launchDeepLink = PdfDeepLinkRequest(paperId, pdfId, title)
            intent.removeExtra(PdfDownloadNotifications.EXTRA_OPEN_PDF_ID)
        }
    }
}
