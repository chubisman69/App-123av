package com.ejemplo.app123av

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Lista de dominios publicitarios y redirecciones
    private val adDomains = setOf(
        "popads.net", "popcash.net", "exoclick.com", "juicyads.com",
        "adsterra.com", "propellerads.com", "doubleclick.net", "googlesyndication.com", 
        "bet365", "1xbet", "ad-delivery", "popunder", "redirect", "adserver", 
        "onclickads", "bet", "tsyndicate", "realsrv", "bongacams", "chaturbate", 
        "stripchat", "trafficjunky", "adxad", "clickadu", "ero-advertising"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        setupWebView()
        webView.loadUrl("https://123av.com")
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false // Permite que inicie el video
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Desactivar apertura automática de pop-ups y múltiples ventanas
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        // --- SISTEMA DE DESCARGA DIRECTA DE VIDEOS ---
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setTitle(fileName)
                request.setDescription("Descargando video...")
                
                // Muestra la notificación de descarga y guarda en la carpeta pública
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)

                Toast.makeText(applicationContext, "Iniciando descarga: $fileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Error al iniciar la descarga", Toast.LENGTH_SHORT).show()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            // Cancelar peticiones a dominios publicitarios antes de cargarlos
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: ""
                if (isAdUrl(url)) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }

            // Permitir que cargue el reproductor, pero bloquear redirecciones externas del marco principal
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val isMainFrame = request.isForMainFrame
                
                if (isAdUrl(url)) {
                    return true 
                }

                if (isMainFrame && !url.contains("123av") && !url.startsWith("blob:")) {
                    return true 
                }
                
                return false
            }

            // Script inyectado para destruir capas transparentes de publicidad y anular pop-ups
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = """
                    javascript:(function() {
                        window.open = function() { return null; };
                        var divs = document.getElementsByTagName('div');
                        for (var i = 0; i < divs.length; i++) {
                            var style = window.getComputedStyle(divs[i]);
                            if ((style.position === 'absolute' || style.position === 'fixed') && parseInt(style.zIndex, 10) > 90) {
                                divs[i].style.pointerEvents = 'none'; 
                            }
                        }
                    })()
                """.trimIndent()
                view?.loadUrl(js)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Manejo de Reproducción en Pantalla Completa
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    onHideCustomView()
                    return
                }
                customView = view
                fullscreenContainer.addView(
                    customView, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
                customViewCallback = callback
                
                // Ocultar barra de estado para modo inmersivo
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }

            override fun onHideCustomView() {
                if (customView == null) return
                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
                
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    private fun isAdUrl(url: String): Boolean {
        return try {
            val lowerUrl = url.lowercase()
            adDomains.any { adDomain -> lowerUrl.contains(adDomain) }
        } catch (e: Exception) {
            false
        }
    }

    // Comportamiento del botón de regresar del teléfono
    override fun onBackPressed() {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
