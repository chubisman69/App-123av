package com.ejemplo.app123av // (Ojo: asegúrate de que esto coincida con tu package original)

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

    // Lista agresiva con redes de adultos, pop-ups y redirecciones nativas de este tipo de webs
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
        settings.mediaPlaybackRequiresUserGesture = false // Permite que el reproductor inicie sin problemas
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Bloqueo estricto del sistema contra pestañas múltiples
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        webView.webViewClient = object : WebViewClient() {
            
            // Interceptar peticiones para que ni siquiera descargue la publicidad
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

            // AQUI ESTÁ LA MAGIA: Permitir el reproductor, pero bloquear redirecciones
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val isMainFrame = request.isForMainFrame // Detecta si es la página entera o solo el marco del video
                
                // Si la URL es basura o publicidad reconocida, bloquearla siempre
                if (isAdUrl(url)) {
                    return true 
                }

                // Si la PÁGINA PRINCIPAL intenta llevarte fuera de 123av.com, cancela el viaje.
                // Como "isForMainFrame" es verdadero solo en la pestaña, los "iframes" (reproductores) 
                // sí podrán cargar desde cualquier servidor libremente.
                if (isMainFrame && !url.contains("123av") && !url.startsWith("blob:")) {
                    return true 
                }
                
                return false
            }

            // Inyectar JavaScript especializado en romper "Capas invisibles" (Clickjacking)
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = """
                    javascript:(function() {
                        // Desactiva abrir nuevas ventanas
                        window.open = function() { return null; };
                        
                        // Busca capas invisibles superpuestas de gran tamaño y desactiva que se puedan clickear
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
            // Pantalla Completa
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
