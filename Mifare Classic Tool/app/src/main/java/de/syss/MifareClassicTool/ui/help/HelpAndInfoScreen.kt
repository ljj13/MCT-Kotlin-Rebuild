package de.syss.MifareClassicTool.ui.help

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import de.syss.MifareClassicTool.R
import de.syss.MifareClassicTool.ui.components.MctTopAppBar
import java.util.Locale

@Composable
fun HelpAndInfoScreen(
    onBackClick: () -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }

    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            MctTopAppBar(
                title = stringResource(R.string.title_activity_help),
                onBackClick = {
                    if (webView?.canGoBack() == true) {
                        webView?.goBack()
                    } else {
                        onBackClick()
                    }
                }
            )
        }
    ) { innerPadding ->
        AndroidView(
            factory = { context ->
                // Resolve localized help document
                val appLocales = AppCompatDelegate.getApplicationLocales()
                val currentLocale = if (!appLocales.isEmpty) {
                    appLocales[0] ?: Locale.getDefault()
                } else {
                    Locale.getDefault()
                }
                val isChinese = currentLocale.language.startsWith("zh", ignoreCase = true)
                val helpAssetUrl = if (isChinese) {
                    "file:///android_asset/help/help_zh.html"
                } else {
                    "file:///android_asset/help/help.html"
                }

                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Set transparent background to avoid white flashing during load
                    setBackgroundColor(0)
                    webViewClient = WebViewClient()
                    settings.apply {
                        javaScriptEnabled = false
                        allowFileAccess = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }
                    loadUrl(helpAssetUrl)
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("help_webview")
        )
    }
}
