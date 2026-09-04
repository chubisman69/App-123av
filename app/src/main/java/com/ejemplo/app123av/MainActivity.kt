package com.ejemplo.appxd

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    // Lista ampliada para bloquear redes de anuncios, redirecciones y popunders
    private val adDomains = setOf(
        "popads", "popcash", "exoclick", "juicyads", "adsterra",
        "propellerads", "doubleclick", "googlesyndication", "bet365", "1xbet",
        "ad-delivery", "popunder", "redirect", "adserver", "onclickads",
        "tsyndicate", "realsrv", "bongacams", "chaturbate", "stripchat",
        "trafficjunky", "adxad", "clickadu", "ero-advertising", "traffichunt"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        setupWebView()
        
        // Evita que la pantalla se apague mientras se reproduce multimedia
        webView.keepScreenOn = true 
        
        webView.loadUrl("https://www.pornhub.com")
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        
        // Ajustes para manejo táctil cómodo en celular
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(true)
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Bloqueo estricto de ventanas emergentes (pestañas nuevas)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)

        // Habilitar sistema de descargas
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.startsWith("blob:")) {
                Toast.makeText(this, "Esta descarga está encriptada por la web y no se puede bajar directamente.", Toast.LENGTH_SHORT).show()
                return@setDownloadListener
            }
            iniciarDescargaManual(url, contentDisposition, mimeType, userAgent)
        }

        webView.webViewClient = object : WebViewClient() {
            
            // Interceptar y destruir recursos de redes publicitarias antes de que carguen
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString()?.lowercase() ?: ""
                if (isAdUrl(url)) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }

            // Control exacto de qué ocurre al hacer clic en cualquier lado
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val lowerUrl = url.lowercase()

                // 1. Manejar intents (evita que la app crashee si un anuncio intenta abrir otra app)
                if (url.startsWith("intent://") || url.startsWith("market://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent != null) {
                            view?.context?.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        // Ignorar si la app de destino no existe
                    }
                    return true
                }

                // 2. Atrapar clics directos a archivos de video para descargarlos
                if (lowerUrl.contains(".mp4") || lowerUrl.contains("download")) {
                    iniciarDescargaManual(url, "", "", "")
                    return true
                }

                // 3. Bloquear redirecciones hacia basura publicitaria
                if (isAdUrl(lowerUrl)) {
                    return true 
                }

                // 4. Bloquear salida a páginas web externas, permitiendo dominios oficiales (phncdn es su servidor de videos/imágenes)
                val isMainFrame = request.isForMainFrame
                if (isMainFrame && !lowerUrl.contains("pornhub") && !lowerUrl.contains("phncdn")) {
                    return true 
                }

                return false // Permitir navegación limpia dentro del sitio
            }

            // Inyección JS para limpiar la experiencia al terminar de cargar
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = """
                    javascript:(function() {
                        // Anular los pop-ups de scripts
                        window.open = function() { return null; };
                        
                        // Forzar a que todos los enlaces abran en la misma vista, evitando bugs de clics ciegos
                        var links = document.getElementsByTagName('a');
                        for (var i = 0; i < links.length; i++) {
                            if (links[i].getAttribute('target') === '_blank') {
                                links[i].removeAttribute('target');
                            }
                        }
                    })()
                """.trimIndent()
                view?.loadUrl(js)
            }
        }

        // Manejo de la Pantalla Completa nativa
        webView.webChromeClient = object : WebChromeClient() {
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

                // Modo Inmersivo: esconde la barra de notificaciones y los botones de navegación de Android
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

                // Devuelve las barras de Android a la normalidad
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    private fun iniciarDescargaManual(url: String, contentDisposition: String, mimeType: String, userAgent: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            
            if (mimeType.isNotEmpty()) {
                request.setMimeType(mimeType)
            }
            if (userAgent.isNotEmpty()) {
                request.addRequestHeader("User-Agent", userAgent)
            }
            
            request.setTitle(fileName)
            request.setDescription("Descargando archivo...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            Toast.makeText(applicationContext, "Iniciando descarga: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "No se pudo iniciar la descarga. Archivo inaccesible.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAdUrl(url: String): Boolean {
        return try {
            adDomains.any { adDomain -> url.contains(adDomain) }
        } catch (e: Exception) {
            false
        }
    }

    // Funcionalidad impecable del botón 'Atrás' del celular
    override fun onBackPressed() {
        if (customView != null) {
            // 1. Si hay un video en pantalla completa, ciérralo
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            // 2. Si hay historial de navegación, retrocede una página
            webView.goBack()
        } else {
            // 3. Si no hay más historial, cierra la app de forma normal
            super.onBackPressed()
        }
    }
}
