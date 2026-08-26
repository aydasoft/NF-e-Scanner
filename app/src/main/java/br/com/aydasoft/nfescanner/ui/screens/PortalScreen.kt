package br.com.aydasoft.nfescanner.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import br.com.aydasoft.nfescanner.BuildConfig
import br.com.aydasoft.nfescanner.R
import br.com.aydasoft.nfescanner.model.NFeAccessKey
import org.json.JSONObject
import java.util.Locale

private const val PORTAL_URL =
    "https://www.nfe.fazenda.gov.br/portal/consultaRecaptcha.aspx" +
        "?tipoConsulta=resumo&tipoConteudo=7PhJ+gAVw2g="

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(
    accessKey: NFeAccessKey,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val currentAccessKey by rememberUpdatedState(accessKey)

    var loading by remember { mutableStateOf(true) }
    var mainFrameError by remember { mutableStateOf(false) }
    var fieldFillFailed by remember { mutableStateOf(false) }

    val webView = remember(context, accessKey.value) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            val portalWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(portalWebView, true)
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    loading = true
                    mainFrameError = false
                    fieldFillFailed = false
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading = false

                    val uri = url?.let(Uri::parse) ?: return
                    if (isPortalConsultPage(uri)) {
                        fillAccessKeyWithRetry(
                            webView = view,
                            accessKey = currentAccessKey,
                            onFinished = { filled -> fieldFillFailed = !filled },
                        )
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (!request.isForMainFrame) return false
                    if (isTrustedPortalUri(request.url)) return false

                    if (request.url.scheme.equals("https", ignoreCase = true)) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        }
                    }
                    return true
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        loading = false
                        mainFrameError = true
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                        loading = false
                        mainFrameError = true
                    }
                }
            }
        }
    }

    LaunchedEffect(webView, accessKey.value) {
        webView.loadUrl(PORTAL_URL)
    }

    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
            CookieManager.getInstance().flush()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.portal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView.reload() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reload),
                        )
                    }
                    IconButton(
                        onClick = {
                            copyAccessKey(context, accessKey)
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, PORTAL_URL.toUri()),
                            )
                        },
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = stringResource(R.string.open_browser),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (mainFrameError) {
                    PortalErrorContent(
                        onRetry = {
                            mainFrameError = false
                            webView.loadUrl(PORTAL_URL)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AndroidView(
                        factory = { webView },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (fieldFillFailed && !mainFrameError) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.field_fill_failed),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp),
    ) {
        Text(
            text = stringResource(R.string.portal_error),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 48.dp, bottom = 20.dp),
        )
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.reload),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun fillAccessKeyWithRetry(
    webView: WebView,
    accessKey: NFeAccessKey,
    attempt: Int = 0,
    onFinished: (Boolean) -> Unit,
) {
    val quotedValue = JSONObject.quote(accessKey.value)
    val script = """
        (() => {
            const inputs = Array.from(document.querySelectorAll('input'));
            const field =
                document.getElementById('ctl00_ContentPlaceHolder1_txtChaveAcessoResumo') ||
                inputs.find(element =>
                    element.id.endsWith('_txtChaveAcessoResumo') ||
                    element.name.endsWith('txtChaveAcessoResumo') ||
                    (
                        element.maxLength === 44 &&
                        (element.type === 'text' || element.type === '')
                    )
                );

            if (!field) return false;

            const valueSetter = Object.getOwnPropertyDescriptor(
                window.HTMLInputElement.prototype,
                'value'
            ).set;
            valueSetter.call(field, $quotedValue);
            field.dispatchEvent(new Event('input', { bubbles: true }));
            field.dispatchEvent(new Event('change', { bubbles: true }));
            field.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return true;
        })();
    """.trimIndent()

    webView.evaluateJavascript(script) { result ->
        when {
            result == "true" -> onFinished(true)
            attempt < MAX_FILL_ATTEMPTS -> {
                webView.postDelayed(
                    {
                        val currentUri = webView.url?.let(Uri::parse)
                        if (currentUri != null && isPortalConsultPage(currentUri)) {
                            fillAccessKeyWithRetry(
                                webView = webView,
                                accessKey = accessKey,
                                attempt = attempt + 1,
                                onFinished = onFinished,
                            )
                        }
                    },
                    FILL_RETRY_DELAY_MS,
                )
            }

            else -> onFinished(false)
        }
    }
}

private fun isPortalConsultPage(uri: Uri): Boolean {
    return isTrustedPortalUri(uri) &&
        uri.path.orEmpty().endsWith("/consultaRecaptcha.aspx", ignoreCase = true)
}

private fun isTrustedPortalUri(uri: Uri): Boolean {
    val host = uri.host?.lowercase(Locale.ROOT) ?: return false
    val trustedHost = host == "nfe.fazenda.gov.br" ||
        host.endsWith(".nfe.fazenda.gov.br")

    return uri.scheme.equals("https", ignoreCase = true) && trustedHost
}

private fun copyAccessKey(context: Context, accessKey: NFeAccessKey) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            context.getString(R.string.access_key_label),
            accessKey.value,
        ),
    )
}

private const val MAX_FILL_ATTEMPTS = 4
private const val FILL_RETRY_DELAY_MS = 350L
