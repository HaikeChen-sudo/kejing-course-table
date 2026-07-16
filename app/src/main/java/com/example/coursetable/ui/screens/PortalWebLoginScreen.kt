package com.example.coursetable.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.coursetable.core.design.KejingColors
import com.example.coursetable.core.design.KejingShape
import com.example.coursetable.data.remote.NcstPortal
import com.example.coursetable.data.remote.PortalSchoolConfig
import com.example.coursetable.data.remote.XzcitPortal
import kotlinx.coroutines.delay

private const val MobileChromeUserAgent =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

private const val DesktopChromeUserAgent =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
@Composable
fun PortalWebLoginScreen(
    onBack: () -> Unit,
    onCapturePage: (url: String, title: String, html: String) -> Unit,
    schoolCode: String = NcstPortal.SCHOOL_CODE,
) {
    val schoolConfig = remember { PortalSchoolConfig.findByCode(schoolCode) ?: PortalSchoolConfig.default }
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by rememberSaveable { mutableStateOf(schoolConfig.schoolName) }
    var currentUrl by rememberSaveable { mutableStateOf(schoolConfig.entryUrl) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var useDesktopMode by rememberSaveable { mutableStateOf(true) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    LaunchedEffect(webView, useDesktopMode) {
        webView?.settings?.userAgentString = if (useDesktopMode) {
            DesktopChromeUserAgent
        } else {
            MobileChromeUserAgent
        }
    }

    LaunchedEffect(isLoading, currentUrl) {
        if (isLoading) {
            delay(15000)
            if (isLoading) {
                errorMessage = "页面加载超时，可刷新或切换兼容模式重试"
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webView?.destroy()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = KejingColors.Ink,
        contentColor = KejingColors.Porcelain,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KejingColors.Ink,
                    titleContentColor = KejingColors.Porcelain,
                    navigationIconContentColor = KejingColors.Orange,
                    actionIconContentColor = KejingColors.Orange,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = {
                    Text(
                        text = pageTitle.ifBlank { schoolConfig.schoolName },
                        maxLines = 1,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInBrowser,
                            contentDescription = "用浏览器打开",
                        )
                    }
                    IconButton(
                        onClick = {
                            errorMessage = null
                            webView?.reload()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "刷新",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val view = webView ?: return@ExtendedFloatingActionButton
                    val url = view.url ?: currentUrl
                    val title = view.title ?: pageTitle
                    view.evaluateJavascript(
                        """
                        (function(){
                          function cleanInline(text){ return (text || '').replace(/\s+/g, ' ').trim(); }
                          function cleanBlock(text){
                            return (text || '')
                              .replace(/\r/g, '')
                              .split('\n')
                              .map(function(line){ return line.replace(/[ \t\f\v]+/g, ' ').trim(); })
                              .filter(function(line){ return line.length > 0; })
                              .join('\n');
                          }
                          function readTables(doc, source){
                            var result = [];
                            try {
                              var tableElements = doc.querySelectorAll('table');
                              for (var t = 0; t < tableElements.length; t++) {
                                var table = tableElements[t];
                                var caption = table.caption ? cleanInline(table.caption.innerText || table.caption.textContent || '') : '';
                                var label = caption || cleanInline(
                                  (table.getAttribute('summary') || '') + ' ' +
                                  (table.id || '') + ' ' +
                                  (table.className || '')
                                );
                                var rows = [];
                                var trElements = table.querySelectorAll('tr');
                                for (var r = 0; r < trElements.length; r++) {
                                  var row = trElements[r];
                                  var cells = [];
                                  var cellElements = row.querySelectorAll('td, th');
                                  for (var c = 0; c < cellElements.length; c++) {
                                    var cell = cellElements[c];
                                    cells.push({
                                      text: cleanBlock(cell.innerText || cell.textContent || ''),
                                      html: cell.innerHTML || '',
                                      rowSpan: cell.rowSpan || 1,
                                      colSpan: cell.colSpan || 1
                                    });
                                  }
                                  rows.push(cells);
                                }
                                result.push({
                                  source: source,
                                  label: label,
                                  id: table.id || '',
                                  rows: rows
                                });
                              }
                            } catch(e) {}
                            return result;
                          }
                          var tables = readTables(document, 'main');
                          try {
                            var iframes = document.querySelectorAll('iframe, frame');
                            for (var i = 0; i < iframes.length; i++) {
                              try {
                                var doc = iframes[i].contentDocument || (iframes[i].contentWindow && iframes[i].contentWindow.document);
                                if (doc) {
                                  tables = tables.concat(readTables(doc, 'frame-' + i));
                                }
                              } catch(e) {}
                            }
                          } catch(e) {}
                          var tableRows = tables.map(function(table){
                            return table.rows;
                          });
                          return JSON.stringify({
                            url: location.href,
                            title: document.title || '',
                            text: cleanBlock(document.body ? document.body.innerText : ''),
                            html: document.documentElement.outerHTML || '',
                            tables: tableRows
                          });
                        })()
                        """.trimIndent(),
                    ) { encodedHtml ->
                        onCapturePage(url, title, encodedHtml)
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                    )
                },
                text = { Text("读取并导入") },
                containerColor = KejingColors.Orange,
                contentColor = KejingColors.Black,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        setBackgroundColor(Color.BLACK)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.blockNetworkImage = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.textZoom = 100
                        settings.allowFileAccess = false
                        settings.allowContentAccess = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.userAgentString = DesktopChromeUserAgent
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = true
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            settings.forceDark = WebSettings.FORCE_DARK_OFF
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                isLoading = newProgress < 100
                                pageTitle = view?.title.orEmpty().ifBlank { pageTitle }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                errorMessage = null
                                currentUrl = url ?: currentUrl
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                currentUrl = url ?: currentUrl
                                pageTitle = view?.title.orEmpty().ifBlank { pageTitle }
                                CookieManager.getInstance().flush()
                                view?.evaluateJavascript(
                                    "(function(){var h=document.documentElement.outerHTML||'';" +
                                        "var t=document.body?document.body.innerText.trim():'';" +
                                        "return h.length + ':' + t.length;})()",
                                ) { metrics ->
                                    val htmlLength = metrics
                                        .trim('"')
                                        .substringBefore(":")
                                        .toIntOrNull()
                                        ?: 0
                                    if (htmlLength < 200 && errorMessage == null) {
                                        errorMessage = "页面内容为空，建议切换兼容模式或用浏览器打开"
                                    }
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    errorMessage = error?.description?.toString()
                                        ?: "页面加载失败，请检查网络后重试"
                                }
                            }
                        }
                        loadUrl(schoolConfig.entryUrl)
                    }
                },
            )
            errorMessage?.let { message ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(KejingColors.Black, KejingShape.Widget)
                        .border(1.dp, KejingColors.Graphite, KejingShape.Widget)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "网页登录加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = KejingColors.Porcelain,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KejingColors.Graphite,
                    )
                    Button(
                        onClick = {
                            errorMessage = null
                            webView?.loadUrl(schoolConfig.entryUrl)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KejingColors.Orange,
                            contentColor = KejingColors.Black,
                        ),
                    ) {
                        Text("重试")
                    }
                    Button(
                        onClick = {
                            useDesktopMode = !useDesktopMode
                            errorMessage = null
                            webView?.loadUrl(schoolConfig.entryUrl)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KejingColors.Graphite,
                            contentColor = KejingColors.Black,
                        ),
                    ) {
                        Text(if (useDesktopMode) "切回手机模式" else "兼容模式")
                    }
                }
            }
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = KejingColors.Orange,
                    trackColor = KejingColors.Black,
                )
            }
        }
    }
}
