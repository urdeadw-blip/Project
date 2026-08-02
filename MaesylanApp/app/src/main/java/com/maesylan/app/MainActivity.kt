package com.maesylan.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * MainActivity hosts the Maesylan website inside a full-screen WebView.
 *
 * Features:
 *  - Pull-to-refresh via [SwipeRefreshLayout]
 *  - Offline / load-error UI with a retry button
 *  - Same-domain links load inside the WebView; external domains open in the
 *    device's default browser
 *  - Back button walks the WebView history before finishing the activity
 *  - Progress bar shown while pages are loading
 *  - Responsive layout works on both phones and tablets (layouts are
 *    constraint-based and fill the available window)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorContainer: View
    private lateinit var retryButton: Button
    private lateinit var errorTitle: TextView
    private lateinit var errorSubtitle: TextView

    /** Tracks whether a page load failed so we can show the error overlay. */
    private var loadFailed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorContainer = findViewById(R.id.errorContainer)
        retryButton = findViewById(R.id.retryButton)
        errorTitle = findViewById(R.id.errorTitle)
        errorSubtitle = findViewById(R.id.errorSubtitle)

        setupWebView()
        setupSwipeRefresh()
        setupErrorScreen()
        setupBackNavigation()

        if (savedInstanceState != null) {
            // Restore the WebView state (scroll position, history, etc.)
            savedInstanceState.getBundle(WEBVIEW_STATE)?.let {
                webView.restoreState(it)
            }
        } else {
            loadWebsite()
        }
    }

    // ---------------------------------------------------------------------
    // WebView setup
    // ---------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true

            // Caching strategy: use cached content when available, otherwise network.
            cacheMode = WebSettings.LOAD_DEFAULT

            // Smooth text rendering on high-density screens.
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            // Allow inline media playback for a nicer experience.
            @Suppress("DEPRECATION")
            allowFileAccess = false
            allowContentAccess = false

            // User agent: append our app identifier so analytics can distinguish app traffic.
            userAgentString = "$userAgentString MaesylanApp/${BuildConfig.VERSION_NAME}"
        }

        webView.webViewClient = MaesylanWebViewClient()

        // Edge-to-edge scroll — no overscroll glow branding needed.
        webView.overScrollMode = View.OVER_SCROLL_NEVER

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    // ---------------------------------------------------------------------
    // Pull-to-refresh
    // ---------------------------------------------------------------------

    private fun setupSwipeRefresh() {
        // Match the brand cyan colour for the refresh spinner.
        swipeRefresh.setColorSchemeResources(R.color.cyan, R.color.cyan_dark, R.color.sky)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.white)
        swipeRefresh.setOnRefreshListener {
            // Hide error overlay and reload
            hideError()
            webView.reload()
        }
    }

    // ---------------------------------------------------------------------
    // Error / offline UI
    // ---------------------------------------------------------------------

    private fun setupErrorScreen() {
        retryButton.setOnClickListener {
            hideError()
            loadWebsite()
        }
    }

    private fun showError(title: String, subtitle: String) {
        loadFailed = true
        errorTitle.text = title
        errorSubtitle.text = subtitle
        errorContainer.visibility = View.VISIBLE
        webView.visibility = View.GONE
        progressBar.visibility = View.GONE
        swipeRefresh.isRefreshing = false
    }

    private fun hideError() {
        loadFailed = false
        errorContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }

    // ---------------------------------------------------------------------
    // Back navigation
    // ---------------------------------------------------------------------

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // Let the system handle it (finishes the activity).
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val bundle = Bundle()
        webView.saveState(bundle)
        outState.putBundle(WEBVIEW_STATE, bundle)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    private fun loadWebsite() {
        progressBar.visibility = View.VISIBLE
        webView.loadUrl(BuildConfig.WEBSITE_URL)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Decide whether a URL should be loaded inside the WebView (same domain,
     * anchors, relative links) or handed off to the device browser.
     */
    private fun isInternalUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: return false

        // The primary host (GitHub Pages) is always internal.
        if (host == BuildConfig.PRIMARY_HOST.lowercase()) return true

        // The production domain for the kennels is also treated as internal.
        if (host.endsWith("maesylankennels.co.uk")) return true

        // Anchor-only and about:blank are internal.
        if (url.startsWith("#") || url == "about:blank") return true

        return false
    }

    // ---------------------------------------------------------------------
    // WebViewClient
    // ---------------------------------------------------------------------

    private inner class MaesylanWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()

            // tel: / mailto: / whatsapp: always go to the OS.
            val scheme = request.url.scheme?.lowercase()
            if (scheme == "tel" || scheme == "mailto" || scheme == "whatsapp" || scheme == "sms") {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                }
                return true
            }

            return if (isInternalUrl(url)) {
                // Load inside the WebView — returning false lets it proceed.
                false
            } else {
                // External domain — hand off to the default browser.
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                true
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = View.VISIBLE
            // Don't hide the error overlay until the page actually starts rendering.
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false
            if (!loadFailed) {
                webView.visibility = View.VISIBLE
                errorContainer.visibility = View.GONE
            }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            // Only show the error overlay for the main-frame request.
            if (request?.isForMainFrame == true) {
                swipeRefresh.isRefreshing = false
                if (isOnline()) {
                    showError(
                        getString(R.string.error_title_generic),
                        getString(R.string.error_subtitle_generic)
                    )
                } else {
                    showError(
                        getString(R.string.error_title_offline),
                        getString(R.string.error_subtitle_offline)
                    )
                }
            }
        }
    }

    companion object {
        private const val WEBVIEW_STATE = "webview_state"
    }
}
