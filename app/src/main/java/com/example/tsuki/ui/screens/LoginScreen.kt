package com.example.tsuki.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tsuki.R
import com.example.tsuki.auth.YouTubeAuthManager
import com.example.tsuki.network.TSukiInnerTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val LOGIN_URL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"

private const val YTCFG_EXTRAS_SCRIPT =
    "(function(){try{var c=window.ytcfg;var g=function(k){try{return c.get(k)||null}catch(e){return null}};" +
        "return JSON.stringify({visitorData:g('VISITOR_DATA'),dataSyncId:g('DATASYNC_ID')})}catch(e){return '{}'}})();"

private fun collectMergedCookies(cookieManager: CookieManager, sources: List<String>): String {
    val merged = LinkedHashMap<String, String>()
    for (source in sources) {
        source.split(";").forEach { pair ->
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val key = pair.take(eq).trim()
                val value = pair.substring(eq + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) merged[key] = value
            }
        }
    }
    return merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    authManager: YouTubeAuthManager,
    innerTubeClient: TSukiInnerTubeClient = remember { TSukiInnerTubeClient.getInstance() },
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pendingVisitorData by remember { mutableStateOf<String?>(null) }
    var pendingDataSyncId by remember { mutableStateOf<String?>(null) }

    fun finalizeSession(view: WebView?, combinedCookies: String) {
        if (isAuthenticating) return
        isAuthenticating = true
        val persistSession: (String?, String?) -> Unit = { visitorData, dataSyncId ->
            scope.launch {
                val accountInfo = withContext(Dispatchers.IO) {
                    innerTubeClient.fetchAccountInfo(combinedCookies)
                }
                authManager.saveSession(
                    cookie = combinedCookies,
                    visitorData = visitorData,
                    dataSyncId = dataSyncId,
                    accountInfo = accountInfo
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Conectado como ${accountInfo?.name ?: "Usuario"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLoginSuccess()
                }
            }
        }
        if (view == null) {
            persistSession(pendingVisitorData, pendingDataSyncId)
            return
        }
        view.post {
            view.evaluateJavascript(YTCFG_EXTRAS_SCRIPT) { result ->
                var visitorData = pendingVisitorData
                var dataSyncId = pendingDataSyncId
                try {
                    val extras = JSONObject(result)
                    if (!extras.isNull("visitorData")) visitorData = extras.optString("visitorData")
                    if (!extras.isNull("dataSyncId")) dataSyncId = extras.optString("dataSyncId")
                } catch (_: Exception) {}
                persistSession(visitorData, dataSyncId)
            }
        }
    }

    BackHandler(enabled = true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.login_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    val cookieManager = CookieManager.getInstance()
                    val combinedCookies = collectMergedCookies(
                        cookieManager,
                        listOf(
                            cookieManager.getCookie("https://accounts.google.com") ?: "",
                            cookieManager.getCookie("https://www.youtube.com") ?: "",
                            cookieManager.getCookie(webViewInstance?.url ?: "") ?: "",
                            cookieManager.getCookie("https://music.youtube.com") ?: ""
                        )
                    )
                    if (combinedCookies.isNotBlank()) {
                        finalizeSession(webViewInstance, combinedCookies)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.common_confirm),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                val combinedCookies = collectMergedCookies(
                                    cookieManager,
                                    listOf(
                                        cookieManager.getCookie("https://accounts.google.com") ?: "",
                                        cookieManager.getCookie("https://www.youtube.com") ?: "",
                                        cookieManager.getCookie(url ?: "") ?: "",
                                        cookieManager.getCookie("https://music.youtube.com") ?: ""
                                    )
                                )

                                view?.evaluateJavascript(YTCFG_EXTRAS_SCRIPT) { result ->
                                    try {
                                        val extras = JSONObject(result)
                                        if (!extras.isNull("visitorData")) pendingVisitorData = extras.optString("visitorData")
                                        if (!extras.isNull("dataSyncId")) pendingDataSyncId = extras.optString("dataSyncId")
                                    } catch (_: Exception) {}
                                }

                                if (combinedCookies.contains("SAPISID") || combinedCookies.contains("__Secure-3PAPISID")) {
                                    finalizeSession(view, combinedCookies)
                                }
                            }
                        }

                        loadUrl(LOGIN_URL)
                        webViewInstance = this
                    }
                },
                update = { webViewInstance = it }
            )

            if (isLoading || isAuthenticating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = if (isAuthenticating) 0.8f else 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        if (isAuthenticating) {
                            Text(
                                text = stringResource(R.string.login_syncing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.statusBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }
}
