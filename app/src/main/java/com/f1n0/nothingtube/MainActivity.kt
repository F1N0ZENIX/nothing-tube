package com.f1n0.nothingtube

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var searchInput: EditText
    private var webView: WebView? = null
    private var webContainer: LinearLayout? = null
    private var homeVisible = true

    private val backgroundColor = Color.rgb(8, 8, 8)
    private val surfaceColor = Color.rgb(25, 25, 25)
    private val accentColor = Color.rgb(229, 57, 53)
    private val textColor = Color.WHITE
    private val mutedColor = Color.rgb(150, 150, 150)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_main)

        searchInput = findViewById(R.id.searchInput)
        findViewById<Button>(R.id.searchButton).setOnClickListener { searchVideos() }
        findViewById<Button>(R.id.trendingButton).setOnClickListener {
            showInternalPage("https://www.youtube.com/feed/trending", "EXPLORAR")
        }
        findViewById<Button>(R.id.musicButton).setOnClickListener {
            showInternalPage("https://music.youtube.com/", "MÚSICA")
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO) {
                searchVideos()
                true
            } else false
        }
    }

    private fun searchVideos() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Digite o que você quer assistir.", Toast.LENGTH_SHORT).show()
            searchInput.requestFocus()
            return
        }
        showInternalPage(
            "https://www.youtube.com/results?search_query=" + Uri.encode(query),
            "RESULTADOS"
        )
    }

    /**
     * Opens YouTube pages in an in-app WebView. HTTP(S) navigation stays inside
     * this WebView; app/deep-link schemes are blocked so they cannot launch the
     * installed YouTube app from a search result.
     */
    private fun showInternalPage(url: String, heading: String) {
        homeVisible = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(16), dp(8))
            setBackgroundColor(backgroundColor)
        }

        val backButton = TextView(this).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            background = roundedBackground(surfaceColor, 18)
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(44))
            contentDescription = "Voltar para a tela inicial"
            setOnClickListener { showHome() }
        }

        val title = TextView(this).apply {
            text = heading
            textSize = 13f
            letterSpacing = 0.16f
            setTextColor(textColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
        }

        val brand = TextView(this).apply {
            text = "●"
            textSize = 18f
            setTextColor(accentColor)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(44))
        }

        toolbar.addView(backButton)
        toolbar.addView(title)
        toolbar.addView(brand)
        root.addView(toolbar)

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(42, 42, 42))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            )
        }
        root.addView(divider)

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accentColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(2)
            )
        }
        root.addView(progress)

        val browser = WebView(this).apply {
            setBackgroundColor(backgroundColor)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val scheme = request.url.scheme?.lowercase()
                    // Allow regular web navigation only; never hand off to YouTube app links.
                    return scheme != "http" && scheme != "https"
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val scheme = Uri.parse(url).scheme?.lowercase()
                    return scheme != "http" && scheme != "https"
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        webView?.destroy()
        webView = browser
        webContainer = root
        root.addView(browser)
        setContentView(root)
        browser.loadUrl(url)
    }

    private fun showHome() {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        webContainer = null
        homeVisible = true
        setContentView(R.layout.activity_main)
        searchInput = findViewById(R.id.searchInput)
        findViewById<Button>(R.id.searchButton).setOnClickListener { searchVideos() }
        findViewById<Button>(R.id.trendingButton).setOnClickListener {
            showInternalPage("https://www.youtube.com/feed/trending", "EXPLORAR")
        }
        findViewById<Button>(R.id.musicButton).setOnClickListener {
            showInternalPage("https://music.youtube.com/", "MÚSICA")
        }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO) {
                searchVideos()
                true
            } else false
        }
    }

    override fun onBackPressed() {
        val browser = webView
        if (!homeVisible && browser != null) {
            if (browser.canGoBack()) browser.goBack() else showHome()
        } else {
            super.onBackPressed()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun roundedBackground(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}