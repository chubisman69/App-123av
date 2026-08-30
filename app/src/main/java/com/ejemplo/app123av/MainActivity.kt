package com.ejemplo.app123av

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream
import java.net.URI

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Lista de dominios de publicidad (AdBlock nativo)
    private val adDomains = setOf(
        "popads.net", "popcash.net", "exoclick.com", "juicyads.com",
        "adsterra.com", "propellerads.com", "doubleclick.net",
        "googlesyndication.com", "bet365", "1xbet", "ad-delivery"
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
        settings.mediaPlaybackRequiresUserGesture = false // Auto-play videos
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Bloqueo de ventanas emergentes (Pop-ups)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        webView.webViewClient = object : WebViewClient() {
            // AdBlocker: Interceptar carga de recursos (imágenes, scripts)
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: ""
                if (isAdUrl(url)) {
                    // Retorna recurso en blanco para cancelar el anuncio
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }

            // Bloquear redirecciones maliciosas/publicitarias
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (isAdUrl(url)) {
                    return true // Interceptar y no cargar
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Pantalla Completa: Mostrar vista
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
                
                // Ocultar barra de estado del sistema (Immersive Mode)
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }

            // Pantalla Completa: Ocultar vista
            override fun onHideCustomView() {
                if (customView == null) return
                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
                
                // Restaurar barra de estado del sistema
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    private fun isAdUrl(url: String): Boolean {
        return try {
            val host = URI(url).host?.lowercase() ?: return false
            adDomains.any { adDomain -> host.contains(adDomain) }
        } catch (e: Exception) {
            false
        }
    }

    // Funcionalidad botón físico "Atrás"
    override fun onBackPressed() {
        if (customView != null) {
            // Salir de pantalla completa si está activa
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            // Retroceder en el historial web
            webView.goBack()
        } else {
            // Salir de la app
            super.onBackPressed()
        }
    }
}
