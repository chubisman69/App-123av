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

    // Lista extendida de dominios de publicidad (AdBlock nativo)
    private val adDomains = setOf(
        "popads.net", "popcash.net", "exoclick.com", "juicyads.com",
        "adsterra.com", "propellerads.com", "doubleclick.net",
        "googlesyndication.com", "bet365", "1xbet", "ad-delivery",
        "popunder", "redirect", "adserver", "onclickads", "bet",
        "adservice", "yandex", "histats", "traffaus"
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

        // Desactivar apertura automática de pop-ups y múltiples ventanas
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

            // Bloquear redirecciones maliciosas/publicitarias y forzar permanencia en el sitio
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Si la URL es de publicidad o NO pertenece al sitio principal, se bloquea
                if (isAdUrl(url) || (!url.contains("123av.com") && !url.startsWith("blob:"))) {
                    return true // Interceptar y cancelar navegación
                }
                return false
            }

            // Inyectar script al terminar de cargar la página para anular Pop-ups al hacer clic en los videos
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = """
                    javascript:(function() {
                        window.open = function() { return null; };
                        var badElements = document.querySelectorAll('iframe[src*="ad"], div[id*="pop"], div[class*="ad"]');
                        for (var i = 0; i < badElements.length; i++) { badElements[i].remove(); }
                    })()
                """.trimIndent()
                view?.loadUrl(js)
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
            val lowerUrl = url.lowercase()
            adDomains.any { adDomain -> lowerUrl.contains(adDomain) }
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
