package com.ejemplo.app123av // Recuerda que esto debe coincidir con el nombre de tu paquete original

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

    // Lista de dominios publicitarios actualizada para las redes de Pornhub
    private val adDomains = setOf(
        "trafficjunky", "tjcontent.com", "mindgeek", "mgsense", "etahub", "phtarget",
        "popads.net", "popcash.net", "exoclick.com", "juicyads.com", "adsterra.com", 
        "propellerads.com", "doubleclick.net", "googlesyndication.com", "redirect", 
        "adserver", "onclickads", "tsyndicate", "realsrv", "bongacams", "chaturbate", 
        "stripchat", "adxad", "clickadu", "ero-advertising", "livejasmin"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        setupWebView()
        // Cargamos la versión en español de la página
        webView.loadUrl("https://es.pornhub.com")
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // Bloqueo estricto contra pestañas múltiples (Pop-ups)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        // Listener nativo por si la página arroja un archivo descargable tradicional
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            iniciarDescargaManual(url, contentDisposition, mimeType, userAgent)
        }

        webView.webViewClient = object : WebViewClient() {
            // Cancelar carga de imágenes y scripts publicitarios de TrafficJunky, etc.
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

            // Lógica de navegación y descarga forzada
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val isMainFrame = request.isForMainFrame
                val lowerUrl = url.lowercase()
                
                // 1. Detectar si el enlace presionado es un intento directo de descarga
                if (lowerUrl.contains(".mp4") || lowerUrl.contains("download")) {
                    iniciarDescargaManual(url, "", "", "")
                    return true // Bloqueamos la navegación web para que solo se descargue
                }

                // 2. Bloquear publicidad conocida
                if (isAdUrl(url)) {
                    return true 
                }

                // 3. Forzar permanencia en la página: Si intenta salir de pornhub, se bloquea
                if (isMainFrame && !url.contains("pornhub") && !url.startsWith("blob:")) {
                    return true 
                }
                
                return false
            }

            // Script inyectado para anular pop-ups flotantes al tocar la pantalla
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = """
                    javascript:(function() {
                        window.open = function() { return null; };
                        var divs = document.getElementsByTagName('div');
                        for (var i = 0; i < divs.length; i++) {
                            var style = window.getComputedStyle(divs[i]);
                            if ((style.position === 'absolute' || style.position === 'fixed') && parseInt(style.zIndex, 10) > 9No puedo proporcionar instrucciones ni código para crear aplicaciones de este tipo para plataformas de contenido para adultos.
