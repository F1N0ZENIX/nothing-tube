package com.f1n0.nothingtube

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var searchInput: EditText
    private var webView: WebView? = null
    private var exoPlayer: ExoPlayer? = null
    private var nativePlayerView: PlayerView? = null
    private var playerVisible = false
    private var fullscreenActive = false
    private var exitFullscreenAction: (() -> Unit)? = null
    private var homeVisible = true
    private var activeSearch = ""
    private var searchProgress: ProgressBar? = null
    private var resultsContainer: LinearLayout? = null
    private val networkExecutor = Executors.newFixedThreadPool(4)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val backgroundColor = Color.rgb(8, 8, 8)
    private val surfaceColor = Color.rgb(25, 25, 25)
    private val accentColor = Color.rgb(229, 57, 53)
    private val textColor = Color.WHITE
    private val mutedColor = Color.rgb(150, 150, 150)
    private val preferences by lazy { getSharedPreferences("nothing_tube", MODE_PRIVATE) }
    private val historyKey = "search_history"
    private val maxHistoryItems = 8
    private var continuationToken: String? = null
    private var innertubeApiKey: String? = null
    private var innertubeContext: JSONObject? = null
    private var isLoadingMore = false
    private var newPipeInitialized = false
    private val displayedVideoIds = mutableSetOf<String>()

    private data class VideoResult(
        val id: String,
        val title: String,
        val channel: String,
        val views: String,
        val duration: String,
        val thumbnail: String
    )

    private data class VideoComment(
        val author: String,
        val text: String,
        val likes: String,
        val published: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        showHome()
    }

    private fun bindHome() {
        searchInput = findViewById(R.id.searchInput)
        findViewById<Button>(R.id.searchButton).setOnClickListener { searchVideos() }
        findViewById<Button>(R.id.trendingButton).setOnClickListener {
            showInternalPage("https://www.youtube.com/feed/trending", "EXPLORAR")
        }
        findViewById<Button>(R.id.musicButton).setOnClickListener {
            showInternalPage("https://music.youtube.com/", "MÚSICA")
        }
        findViewById<TextView>(R.id.clearHistoryButton).setOnClickListener {
            preferences.edit().remove(historyKey).apply()
            renderSearchHistory()
            Toast.makeText(this, "Histórico de pesquisa apagado.", Toast.LENGTH_SHORT).show()
        }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO) {
                searchVideos()
                true
            } else false
        }
        renderSearchHistory()
    }

    private fun getSearchHistory(): MutableList<String> {
        return preferences.getStringSet(historyKey, emptySet())
            ?.toList()
            ?.sortedByDescending { it.substringBefore("|").toLongOrNull() ?: 0L }
            ?.map { it.substringAfter("|", it) }
            ?.toMutableList() ?: mutableListOf()
    }

    private fun saveSearch(query: String) {
        val old = preferences.getStringSet(historyKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        old.removeAll { it.substringAfter("|", it).equals(query, ignoreCase = true) }
        old.add("${System.currentTimeMillis()}|$query")
        val trimmed = old.sortedByDescending { it.substringBefore("|").toLongOrNull() ?: 0L }
            .take(maxHistoryItems)
            .toSet()
        preferences.edit().putStringSet(historyKey, trimmed).apply()
        renderSearchHistory()
    }

    private fun renderSearchHistory() {
        val container = findViewById<LinearLayout?>(R.id.historyContainer) ?: return
        container.removeAllViews()
        val history = getSearchHistory()
        if (history.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Suas pesquisas recentes vão aparecer aqui."
                textSize = 12f
                setTextColor(mutedColor)
                setPadding(dp(2), dp(12), dp(2), dp(12))
            }
            container.addView(empty)
            return
        }

        history.forEach { query ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(4), dp(8), dp(4))
                background = roundedBackground(surfaceColor, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
                ).apply { bottomMargin = dp(7) }
                setOnClickListener {
                    searchInput.setText(query)
                    searchInput.setSelection(searchInput.text.length)
                    searchVideos()
                }
            }
            val icon = TextView(this).apply {
                text = "↻"
                textSize = 19f
                setTextColor(accentColor)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(40))
            }
            val label = TextView(this).apply {
                text = query
                textSize = 13f
                setTextColor(textColor)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(dp(8), 0, dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
            }
            val arrow = TextView(this).apply {
                text = "↗"
                textSize = 16f
                setTextColor(mutedColor)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(40))
            }
            row.addView(icon)
            row.addView(label)
            row.addView(arrow)
            container.addView(row)
        }
    }

    private fun searchVideos() {
        val query = searchInput.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Digite o que você quer assistir.", Toast.LENGTH_SHORT).show()
            searchInput.requestFocus()
            return
        }
        saveSearch(query)
        loadNativeSearch(query)
    }

    /**
     * Searches YouTube's public results page and renders video cards natively.
     * This is an unofficial parser and may need maintenance if YouTube changes its HTML.
     */
    private fun loadNativeSearch(query: String) {
        activeSearch = query
        showNativeResults(query)
        setSearchMessage("BUSCANDO VÍDEOS…", true)

        networkExecutor.execute {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val connection = (URL("https://www.youtube.com/results?search_query=$encoded").openConnection()
                    as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
                    setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.7")
                    instanceFollowRedirects = true
                }
                val html = try {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally {
                    connection.disconnect()
                }
                val initialData = extractInitialData(html)
                    ?: throw IllegalStateException("O YouTube não retornou os dados da pesquisa.")
                val videos = mutableListOf<VideoResult>()
                collectVideoRenderers(initialData, videos)
                val unique = videos.distinctBy { it.id }
                val nextToken = findNextContinuationToken(initialData)
                val apiKey = Regex("\"INNERTUBE_API_KEY\":\"([^\"]+)\"").find(html)?.groupValues?.getOrNull(1)
                val context = extractJsonObjectAfter(html, "\"INNERTUBE_CONTEXT\"")
                mainHandler.post {
                    if (activeSearch != query || isFinishing || isDestroyed) return@post
                    searchProgress?.visibility = View.GONE
                    resultsContainer?.removeAllViews()
                    displayedVideoIds.clear()
                    continuationToken = nextToken
                    innertubeApiKey = apiKey
                    innertubeContext = context
                    isLoadingMore = false
                    if (unique.isEmpty()) {
                        setSearchMessage("Nenhum vídeo encontrado. Tente outra pesquisa.", false)
                    } else {
                        val count = TextView(this).apply {
                            text = "RESULTADOS PARA “$query”"
                            textSize = 10f
                            letterSpacing = 0.08f
                            setTextColor(mutedColor)
                            setPadding(0, dp(6), 0, dp(12))
                        }
                        resultsContainer?.addView(count)
                        unique.forEach { video -> if (displayedVideoIds.add(video.id)) resultsContainer?.addView(createVideoCard(video)) }
                        if (continuationToken == null) addEndOfResultsMessage()
                    }
                }
            } catch (error: Exception) {
                mainHandler.post {
                    if (activeSearch != query || isFinishing || isDestroyed) return@post
                    searchProgress?.visibility = View.GONE
                    setSearchMessage(
                        "Não foi possível carregar os resultados agora. Verifique a conexão e tente novamente.",
                        false
                    )
                }
            }
        }
    }

    private fun extractInitialData(html: String): JSONObject? = extractJsonObjectAfter(html, "ytInitialData")

    private fun extractJsonObjectAfter(source: String, marker: String): JSONObject? {
        val markerIndex = source.indexOf(marker)
        if (markerIndex < 0) return null
        val start = source.indexOf('{', markerIndex + marker.length)
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until source.length) {
            val c = source[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
            } else {
                when (c) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return try { JSONObject(source.substring(start, i + 1)) } catch (_: Exception) { null }
                    }
                }
            }
        }
        return null
    }

    private fun findNextContinuationToken(value: Any?): String? {
        when (value) {
            is JSONObject -> {
                val renderer = value.optJSONObject("continuationItemRenderer")
                if (renderer != null) {
                    val token = renderer.optJSONObject("continuationEndpoint")?.optJSONObject("continuationCommand")?.optString("token")
                    if (!token.isNullOrBlank() && token != "null") return token
                }
                val keys = value.keys()
                while (keys.hasNext()) {
                    val found = findNextContinuationToken(value.opt(keys.next()))
                    if (!found.isNullOrBlank()) return found
                }
            }
            is JSONArray -> for (i in 0 until value.length()) {
                val found = findNextContinuationToken(value.opt(i))
                if (!found.isNullOrBlank()) return found
            }
        }
        return null
    }

    private fun loadMoreVideos() {
        val token = continuationToken ?: return
        val apiKey = innertubeApiKey ?: return
        val context = innertubeContext ?: return
        if (isLoadingMore || activeSearch.isBlank()) return
        isLoadingMore = true
        searchProgress?.visibility = View.VISIBLE
        networkExecutor.execute {
            try {
                val body = JSONObject().put("context", context).put("continuation", token).toString()
                val connection = (URL("https://www.youtube.com/youtubei/v1/search?key=$apiKey").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Origin", "https://www.youtube.com")
                }
                val response = try {
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally { connection.disconnect() }
                val data = JSONObject(response)
                val videos = mutableListOf<VideoResult>()
                collectVideoRenderers(data, videos)
                val nextToken = findNextContinuationToken(data)
                mainHandler.post {
                    if (isFinishing || isDestroyed) return@post
                    isLoadingMore = false
                    searchProgress?.visibility = View.GONE
                    videos.distinctBy { it.id }.filter { displayedVideoIds.add(it.id) }.forEach {
                        resultsContainer?.addView(createVideoCard(it))
                    }
                    continuationToken = nextToken
                    if (nextToken == null) addEndOfResultsMessage()
                }
            } catch (_: Exception) {
                mainHandler.post {
                    isLoadingMore = false
                    searchProgress?.visibility = View.GONE
                    Toast.makeText(this, "Não foi possível carregar mais vídeos. Tente rolar novamente.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addEndOfResultsMessage() {
        if (resultsContainer?.findViewWithTag<View>("end-of-results") != null) return
        resultsContainer?.addView(TextView(this).apply {
            tag = "end-of-results"
            text = "FIM DOS RESULTADOS DISPONÍVEIS"
            textSize = 10f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(mutedColor)
            setPadding(dp(8), dp(20), dp(8), dp(24))
        })
    }

    private fun collectVideoRenderers(value: Any?, output: MutableList<VideoResult>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    if (key == "videoRenderer" && child is JSONObject) {
                        parseVideo(child)?.let { output.add(it) }
                    } else {
                        collectVideoRenderers(child, output)
                    }
                }
            }
            is JSONArray -> for (i in 0 until value.length()) {
                collectVideoRenderers(value.opt(i), output)
            }
        }
    }

    private fun parseVideo(item: JSONObject): VideoResult? {
        val id = item.optString("videoId").takeIf { it.isNotBlank() } ?: return null
        val title = textFromRuns(item.optJSONObject("title")).ifBlank {
            item.optJSONObject("title")?.optString("simpleText").orEmpty()
        }.ifBlank { "Vídeo do YouTube" }
        val channel = textFromRuns(item.optJSONObject("ownerText")).ifBlank {
            textFromRuns(item.optJSONObject("shortBylineText"))
        }
        val views = item.optJSONObject("viewCountText")?.optString("simpleText").orEmpty()
            .ifBlank { textFromRuns(item.optJSONObject("viewCountText")) }
        val duration = item.optJSONObject("lengthText")?.optString("simpleText").orEmpty()
            .ifBlank {
                item.optJSONObject("lengthText")?.optJSONObject("accessibility")?.optJSONObject("accessibilityData")?.optString("label").orEmpty()
            }
        val thumbs = item.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumbnail = if (thumbs != null && thumbs.length() > 0) {
            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url").orEmpty()
        } else ""
        return VideoResult(id, title, channel, views, duration, thumbnail)
    }

    private fun textFromRuns(obj: JSONObject?): String {
        if (obj == null) return ""
        val runs = obj.optJSONArray("runs") ?: return obj.optString("simpleText").orEmpty()
        return buildString {
            for (i in 0 until runs.length()) {
                if (i > 0) append("")
                append(runs.optJSONObject(i)?.optString("text").orEmpty())
            }
        }
    }

    private fun showNativeResults(query: String) {
        homeVisible = false
        webView?.stopLoading()
        webView?.destroy()
        webView = null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }
        applySystemInsets(root)
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            background = roundedBackground(surfaceColor, 16)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            contentDescription = "Voltar para o início"
            setOnClickListener { showHome() }
        }
        searchInput = EditText(this).apply {
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText(query)
            setSelection(text.length)
            hint = "Pesquisar vídeos…"
            textSize = 14f
            setTextColor(textColor)
            setHintTextColor(mutedColor)
            background = roundedBackground(surfaceColor, 14)
            setPadding(dp(14), 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                marginStart = dp(10)
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO) {
                    searchVideos()
                    true
                } else false
            }
        }
        val go = TextView(this).apply {
            text = "→"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBackground(accentColor, 14)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                marginStart = dp(8)
            }
            setOnClickListener { searchVideos() }
        }
        toolbar.addView(back)
        toolbar.addView(searchInput)
        toolbar.addView(go)
        root.addView(toolbar)

        root.addView(View(this).apply {
            setBackgroundColor(Color.rgb(42, 42, 42))
            layoutParams = LinearLayout.LayoutParams(-1, dp(1))
        })
        searchProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            isIndeterminate = true
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accentColor)
            layoutParams = LinearLayout.LayoutParams(-1, dp(2))
        }
        root.addView(searchProgress)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            setOnScrollChangeListener { view: View, _, scrollY, _, _ ->
                val scroller = view as ScrollView
                if (continuationToken != null && !isLoadingMore && scrollY + scroller.height >= scroller.getChildAt(0).height - dp(500)) loadMoreVideos()
            }
        }
        resultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }
        scroll.addView(resultsContainer)
        root.addView(scroll)
        setContentView(root)
    }

    private fun setSearchMessage(message: String, loading: Boolean) {
        resultsContainer?.removeAllViews()
        val text = TextView(this).apply {
            this.text = message
            textSize = 13f
            setTextColor(mutedColor)
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(36), dp(24), dp(36))
        }
        resultsContainer?.addView(text)
        searchProgress?.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun createVideoCard(video: VideoResult): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(surfaceColor, 14)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(16)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { playVideo(video) }
        }

        val thumbnailFrame = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(Color.rgb(35, 35, 35), 10)
            layoutParams = LinearLayout.LayoutParams(-1, dp(184))
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(35, 35, 35))
            contentDescription = video.title
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        thumbnailFrame.addView(image)
        card.addView(thumbnailFrame)

        val title = TextView(this).apply {
            text = video.title
            textSize = 15f
            setTextColor(textColor)
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(12), dp(12), dp(12), dp(4))
        }
        card.addView(title)

        val details = listOf(video.channel, listOf(video.views, video.duration).filter { it.isNotBlank() }.joinToString("  • "))
            .filter { it.isNotBlank() }
            .joinToString("  • ")
        val meta = TextView(this).apply {
            text = details.ifBlank { "YouTube" }
            textSize = 11f
            setTextColor(mutedColor)
            maxLines = 2
            setPadding(dp(12), 0, dp(12), dp(14))
        }
        card.addView(meta)

        if (video.thumbnail.isNotBlank()) {
            networkExecutor.execute {
                try {
                    val connection = URL(video.thumbnail).openConnection().apply {
                        connectTimeout = 10000
                        readTimeout = 12000
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    val bitmap = connection.getInputStream().use { BitmapFactory.decodeStream(it) }
                    if (bitmap != null) mainHandler.post {
                        if (!isFinishing && !isDestroyed) image.setImageBitmap(bitmap)
                    }
                } catch (_: Exception) { }
            }
        }
        return card
    }

    /**
     * Plays the selected YouTube video in a native Media3/ExoPlayer view.
     * Invidious is used only to resolve a playable media URL; playback itself is native.
     */
    private fun playVideo(video: VideoResult) {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        releaseNativePlayer()
        homeVisible = false
        playerVisible = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }
        applySystemInsets(root)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val back = TextView(this).apply {
            text = "‹"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            background = roundedBackground(surfaceColor, 16)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            contentDescription = "Voltar aos resultados"
            setOnClickListener { closeNativePlayer() }
        }
        val heading = TextView(this).apply {
            text = "NOW PLAYING"
            textSize = 12f
            letterSpacing = 0.14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
        }
        val optionsButton = TextView(this).apply {
            text = "⋮"
            textSize = 28f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            background = roundedBackground(surfaceColor, 14)
            contentDescription = "Menu de opções do vídeo"
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(44))
            setOnClickListener { anchor ->
                PopupMenu(this@MainActivity, anchor).apply {
                    menu.add(0, 1, 0, "Compartilhar vídeo")
                    menu.add(0, 2, 1, "Copiar link")
                    menu.add(0, 3, 2, "Abrir no YouTube")
                    menu.add(0, 4, 3, "Recarregar reprodução")
                    setOnMenuItemClickListener { item ->
                        val videoUrl = "https://www.youtube.com/watch?v=${video.id}"
                        when (item.itemId) {
                            1 -> {
                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, videoUrl)
                                }, "Compartilhar vídeo"))
                                true
                            }
                            2 -> {
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Link do vídeo", videoUrl))
                                Toast.makeText(this@MainActivity, "Link copiado.", Toast.LENGTH_SHORT).show()
                                true
                            }
                            3 -> {
                                showInternalPage(videoUrl, "YOUTUBE")
                                true
                            }
                            4 -> {
                                playVideo(video)
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            }
        }
        val fullscreenButton = TextView(this).apply {
            text = "⛶"
            textSize = 23f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            background = roundedBackground(surfaceColor, 14)
            contentDescription = "Alternar tela cheia"
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(44)).apply { marginEnd = dp(6) }
        }
        toolbar.addView(back)
        toolbar.addView(heading)
        toolbar.addView(fullscreenButton)
        toolbar.addView(optionsButton)
        root.addView(toolbar)
        val toolbarDivider = View(this).apply {
            setBackgroundColor(Color.rgb(42, 42, 42))
            layoutParams = LinearLayout.LayoutParams(-1, dp(1))
        }
        root.addView(toolbarDivider)

        val player = PlayerView(this).apply {
            setBackgroundColor(Color.BLACK)
            useController = true
            controllerAutoShow = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        nativePlayerView = player
        val playerFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(-1, dp(236))
        }
        playerFrame.addView(player)
        val fullscreenOverlayButton = TextView(this).apply {
            text = "⤢"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedBackground(Color.argb(180, 25, 25, 25), 12)
            contentDescription = "Sair da tela cheia"
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(12)
                marginEnd = dp(12)
            }
        }
        playerFrame.addView(fullscreenOverlayButton)
        root.addView(playerFrame)

        val detailsScroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }
        detailsScroll.addView(details)
        root.addView(detailsScroll)

        val title = TextView(this).apply {
            text = video.title
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textColor)
            setPadding(dp(18), dp(20), dp(18), dp(8))
        }
        details.addView(title)
        val channel = TextView(this).apply {
            text = video.channel.ifBlank { "YouTube" }
            textSize = 13f
            setTextColor(mutedColor)
            setPadding(dp(18), 0, dp(18), dp(12))
        }
        details.addView(channel)

        val status = TextView(this).apply {
            text = "CONECTANDO AO STREAM…"
            textSize = 12f
            letterSpacing = 0.04f
            setTextColor(mutedColor)
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(24), dp(22), dp(24))
        }
        details.addView(status)

        val retryButton = Button(this).apply {
            text = "TENTAR NOVAMENTE"
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(44, 44, 44))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply {
                setMargins(dp(18), dp(4), dp(18), dp(18))
            }
            setOnClickListener { playVideo(video) }
        }
        details.addView(retryButton)


        val sectionDivider = View(this).apply {
            setBackgroundColor(Color.rgb(42, 42, 42))
            layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply {
                setMargins(dp(18), dp(4), dp(18), dp(12))
            }
        }
        details.addView(sectionDivider)
        val descriptionHeading = TextView(this).apply {
            text = "DESCRIÇÃO"
            textSize = 11f
            letterSpacing = 0.12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(accentColor)
            setPadding(dp(18), dp(4), dp(18), dp(8))
        }
        details.addView(descriptionHeading)
        val descriptionText = TextView(this).apply {
            text = "Carregando descrição…"
            textSize = 13f
            setTextColor(Color.rgb(205, 205, 205))
            setPadding(dp(18), 0, dp(18), dp(18))
        }
        details.addView(descriptionText)

        val commentsDivider = View(this).apply {
            setBackgroundColor(Color.rgb(42, 42, 42))
            layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply {
                setMargins(dp(18), dp(4), dp(18), dp(12))
            }
        }
        details.addView(commentsDivider)
        val commentsHeading = TextView(this).apply {
            text = "COMENTÁRIOS"
            textSize = 11f
            letterSpacing = 0.12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(accentColor)
            setPadding(dp(18), dp(4), dp(18), dp(8))
        }
        details.addView(commentsHeading)
        val commentsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), dp(18))
        }
        details.addView(commentsContainer)

        var fullscreen = false
        fun toggleFullscreen() {
            fullscreen = !fullscreen
            fullscreenActive = fullscreen
            toolbar.visibility = if (fullscreen) View.GONE else View.VISIBLE
            toolbarDivider.visibility = if (fullscreen) View.GONE else View.VISIBLE
            detailsScroll.visibility = if (fullscreen) View.GONE else View.VISIBLE
            playerFrame.layoutParams = LinearLayout.LayoutParams(-1, if (fullscreen) 0 else dp(236), if (fullscreen) 1f else 0f)
            fullscreenOverlayButton.visibility = if (fullscreen) View.VISIBLE else View.GONE
            fullscreenButton.text = if (fullscreen) "⤢" else "⛶"
            window.decorView.systemUiVisibility = if (fullscreen) {
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else View.SYSTEM_UI_FLAG_VISIBLE
            player.showController()
        }
        fullscreenButton.setOnClickListener { toggleFullscreen() }
        fullscreenOverlayButton.setOnClickListener { if (fullscreen) toggleFullscreen() }
        exitFullscreenAction = { if (fullscreen) toggleFullscreen() }

        setContentView(root)

        networkExecutor.execute {
            val description = fetchVideoDescription(video.id)
            mainHandler.post {
                if (!isFinishing && !isDestroyed && playerVisible && nativePlayerView === player) {
                    descriptionText.text = description.ifBlank { "Descrição indisponível. Tente novamente mais tarde." }
                }
            }
        }
        networkExecutor.execute {
            val comments = fetchVideoComments(video.id)
            mainHandler.post {
                if (isFinishing || isDestroyed || !playerVisible || nativePlayerView !== player) return@post
                commentsContainer.removeAllViews()
                if (comments.isEmpty()) {
                    commentsContainer.addView(TextView(this).apply {
                        text = "Não foi possível carregar os comentários. Eles podem estar desativados ou o YouTube pode ter limitado o acesso."
                        textSize = 13f
                        setTextColor(mutedColor)
                        setPadding(0, 0, 0, dp(12))
                    })
                } else {
                    comments.take(30).forEach { comment ->
                        val item = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(0, dp(10), 0, dp(12))
                        }
                        item.addView(TextView(this).apply {
                            text = comment.author + if (comment.published.isNotBlank()) "  ·  " + comment.published else ""
                            textSize = 12f
                            setTypeface(null, Typeface.BOLD)
                            setTextColor(Color.rgb(220, 220, 220))
                        })
                        item.addView(TextView(this).apply {
                            text = comment.text
                            textSize = 13f
                            setTextColor(Color.rgb(205, 205, 205))
                            setPadding(0, dp(5), 0, dp(4))
                        })
                        if (comment.likes.isNotBlank()) item.addView(TextView(this).apply {
                            text = "♡  " + comment.likes
                            textSize = 11f
                            setTextColor(mutedColor)
                        })
                        commentsContainer.addView(item)
                        commentsContainer.addView(View(this).apply {
                            setBackgroundColor(Color.rgb(42, 42, 42))
                            layoutParams = LinearLayout.LayoutParams(-1, dp(1))
                        })
                    }
                }
            }
        }

        // Keep playback fully native. If extraction is slow, expose retry instead of opening a WebView.
        mainHandler.postDelayed({
            if (!isFinishing && !isDestroyed && playerVisible && nativePlayerView === player &&
                status.visibility == View.VISIBLE && status.text.toString().contains("CONECTANDO")) {
                status.text = "A EXTRAÇÃO ESTÁ DEMORANDO. TENTE NOVAMENTE."
                retryButton.visibility = View.VISIBLE
            }
        }, 25_000L)

        networkExecutor.execute {
            val resolved = resolvePlayableStream(video.id)
            mainHandler.post {
                if (isFinishing || isDestroyed || !playerVisible || nativePlayerView !== player) return@post
                if (resolved == null) {
                    status.text = "NÃO FOI POSSÍVEL CONECTAR AO STREAM"
                    retryButton.visibility = View.VISIBLE
                    return@post
                }
                status.visibility = View.GONE
                retryButton.visibility = View.GONE
                try {
                    val httpFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent("Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(20000)
                        .setDefaultRequestProperties(mapOf("Referer" to "https://www.youtube.com/"))
                    val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
                    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                    val newPlayer = ExoPlayer.Builder(this)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .build()
                    exoPlayer = newPlayer
                    player.player = newPlayer
                    val item = if (resolved.endsWith(".m3u8", ignoreCase = true)) {
                        MediaItem.Builder().setUri(resolved).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                    } else {
                        MediaItem.fromUri(resolved)
                    }
                    newPlayer.setMediaItem(item)
                    newPlayer.prepare()
                    newPlayer.playWhenReady = true
                    newPlayer.addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            if (!isFinishing && !isDestroyed) {
                                status.visibility = View.VISIBLE
                                status.text = "O STREAM FALHOU OU EXPIROU"
                                retryButton.visibility = View.VISIBLE
                            }
                        }
                    })
                } catch (_: Exception) {
                    status.visibility = View.VISIBLE
                    status.text = "NÃO FOI POSSÍVEL INICIAR O PLAYER"
                    retryButton.visibility = View.VISIBLE
                }
            }
        }
    }

    @Synchronized
    private fun resolveWithNewPipe(videoId: String): String? {
        if (!newPipeInitialized) {
            org.schabi.newpipe.extractor.NewPipe.init(object : org.schabi.newpipe.extractor.downloader.Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                    val connection = (URL(request.url()).openConnection() as HttpURLConnection).apply {
                        requestMethod = request.httpMethod()
                        connectTimeout = 9000
                        readTimeout = 12000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0")
                        request.headers()?.forEach { (key, values) ->
                            values.forEach { value -> setRequestProperty(key, value) }
                        }
                        request.dataToSend()?.let { data ->
                            doOutput = true
                            outputStream.use { it.write(data) }
                        }
                    }
                    try {
                        val statusCode = connection.responseCode
                        val body = (if (statusCode >= 400) connection.errorStream else connection.inputStream)
                            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                        val headers = connection.headerFields.filterKeys { it != null }
                            .mapKeys { it.key!! }
                            .mapValues { it.value ?: emptyList<String>() }
                        return org.schabi.newpipe.extractor.downloader.Response(
                            statusCode,
                            connection.responseMessage.orEmpty(),
                            headers,
                            body,
                            connection.url.toString()
                        )
                    } finally {
                        connection.disconnect()
                    }
                }
            })
            newPipeInitialized = true
        }

        val info = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
            org.schabi.newpipe.extractor.ServiceList.YouTube,
            "https://www.youtube.com/watch?v=$videoId"
        )
        val streams = info.videoStreams ?: emptyList()
        val usable = streams.filter { it.content.startsWith("https://") && !it.isVideoOnly }
        return usable.maxByOrNull { stream ->
            Regex("(\\d+)p").find(stream.resolution)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        }?.content
    }

    private fun fetchVideoDescription(videoId: String): String {
        try {
            val connection = (URL("https://www.youtube.com/watch?v=$videoId&hl=pt-BR").openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                setRequestProperty("Cookie", "CONSENT=YES+")
            }
            val html = try { connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() } } finally { connection.disconnect() }
            val playerResponse = extractJsonObjectAfter(html, "ytInitialPlayerResponse") ?: extractJsonObjectAfter(html, "playerResponse")
            val description = playerResponse?.optJSONObject("videoDetails")?.optString("shortDescription").orEmpty()
            if (description.isNotBlank() && description != "null") return description
        } catch (_: Exception) { }
        for (instance in listOf("https://inv.nadeko.net", "https://invidious.nerdvpn.de", "https://iv.datura.network")) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$instance/api/v1/videos/$videoId").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 7000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "application/json")
                }
                if (connection.responseCode !in 200..299) continue
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                val description = json.optString("description").takeIf { it.isNotBlank() && it != "null" }
                if (description != null) return description
            } catch (_: Exception) {
            } finally { connection?.disconnect() }
        }
        return ""
    }

    private fun fetchVideoComments(videoId: String): List<VideoComment> {
        var pageConnection: HttpURLConnection? = null
        try {
            pageConnection = (URL("https://www.youtube.com/watch?v=$videoId&hl=pt-BR").openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                setRequestProperty("Cookie", "CONSENT=YES+")
            }
            val html = pageConnection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val apiKey = html.substringAfter("INNERTUBE_API_KEY", "").substringAfter(":", "")
                .let { Regex("[A-Za-z0-9_-]{20,}").find(it)?.value }
            val context = extractJsonObjectAfter(html, "INNERTUBE_CONTEXT")
            val initialData = extractJsonObjectAfter(html, "ytInitialData")
            val comments = mutableListOf<VideoComment>()
            if (initialData != null) collectComments(initialData, comments)
            val continuation = initialData?.let { findCommentsContinuation(it) }
            pageConnection.disconnect()
            pageConnection = null
            if (comments.size >= 20 || apiKey.isNullOrBlank() || context == null || continuation.isNullOrBlank()) {
                return comments.distinctBy { it.author + it.text }.take(30)
            }
            val body = JSONObject().put("context", context).put("continuation", continuation).toString()
            val connection = (URL("https://www.youtube.com/youtubei/v1/next?key=$apiKey").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 12000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Origin", "https://www.youtube.com")
            }
            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                collectComments(JSONObject(response), comments)
            } finally { connection.disconnect() }
            return comments.distinctBy { it.author + it.text }.take(30)
        } catch (_: Exception) {
            return emptyList()
        } finally { pageConnection?.disconnect() }
    }

    private fun findCommentsContinuation(data: Any?): String? {
        when (data) {
            is JSONObject -> {
                if (data.optString("sectionIdentifier") == "comment-item-section") return findNextContinuationToken(data)
                val keys = data.keys()
                while (keys.hasNext()) {
                    val found = findCommentsContinuation(data.opt(keys.next()))
                    if (!found.isNullOrBlank()) return found
                }
            }
            is JSONArray -> for (i in 0 until data.length()) {
                val found = findCommentsContinuation(data.opt(i))
                if (!found.isNullOrBlank()) return found
            }
        }
        return null
    }

    private fun collectComments(data: Any?, output: MutableList<VideoComment>) {
        when (data) {
            is JSONObject -> {
                val thread = data.optJSONObject("commentThreadRenderer")
                val renderer = thread?.optJSONObject("comment")?.optJSONObject("commentRenderer")
                if (renderer != null) {
                    val author = textFromRuns(renderer.optJSONObject("authorText")).ifBlank { "Usuário do YouTube" }
                    val body = textFromRuns(renderer.optJSONObject("contentText"))
                    val likes = renderer.optJSONObject("voteCount")?.optString("simpleText").orEmpty()
                    val published = renderer.optJSONObject("publishedTimeText")?.optString("simpleText").orEmpty()
                    if (body.isNotBlank()) output.add(VideoComment(author, body, likes, published))
                }
                val keys = data.keys()
                while (keys.hasNext()) collectComments(data.opt(keys.next()), output)
            }
            is JSONArray -> for (i in 0 until data.length()) collectComments(data.opt(i), output)
        }
    }

    private fun resolvePlayableStream(videoId: String): String? {
        // First try an on-device extractor instead of relying on public Piped/Invidious instances.
        try {
            val extracted = resolveWithNewPipe(videoId)
            if (!extracted.isNullOrBlank()) return extracted
        } catch (_: Exception) {
            // Continue to public API fallbacks below.
        }

        // Piped exposes muxed MP4 streams that ExoPlayer can play without combining DASH tracks.
        // Public instances are best-effort and can go offline or be rate-limited.
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.rivo.lol",
            "https://pipedapi.leptons.xyz"
        )
        for (instance in pipedInstances) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$instance/streams/$videoId").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3500
                    readTimeout = 4500
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Accept", "application/json")
                }
                if (connection.responseCode !in 200..299) continue
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                val streams = json.optJSONArray("videoStreams")
                if (streams != null) {
                    val candidates = (0 until streams.length()).mapNotNull { index ->
                        val item = streams.optJSONObject(index) ?: return@mapNotNull null
                        val url = item.optString("url").takeIf { it.startsWith("https://") } ?: return@mapNotNull null
                        val mime = item.optString("mimeType")
                        val format = item.optString("format")
                        if (item.optBoolean("videoOnly", false)) return@mapNotNull null
                        if (!(mime.contains("video/mp4", true) || format.equals("MPEG_4", true))) return@mapNotNull null
                        Triple(url, item.optInt("height", 0), item.optLong("bitrate", 0L))
                    }
                    val chosen = candidates.filter { it.second in 1..720 }
                        .maxWithOrNull(compareBy<Triple<String, Int, Long>> { it.second }.thenBy { it.third })
                        ?: candidates.maxWithOrNull(compareBy<Triple<String, Int, Long>> { it.second }.thenBy { it.third })
                    if (chosen != null) return chosen.first
                }
                val hls = json.optString("hls", json.optString("hlsUrl"))
                    .takeIf { it.startsWith("https://") && it != "null" && it.isNotBlank() }
                if (hls != null) return hls
            } catch (_: Exception) {
                // Continue to another instance.
            } finally {
                connection?.disconnect()
            }
        }

        // Invidious fallback. Some instances return direct MP4 URLs, others only metadata.
        val invidiousInstances = listOf(
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://iv.datura.network"
        )
        for (instance in invidiousInstances) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("$instance/api/v1/videos/$videoId").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3500
                    readTimeout = 4500
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Accept", "application/json")
                }
                if (connection.responseCode !in 200..299) continue
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                val formats = json.optJSONArray("formatStreams")
                if (formats != null) {
                    val candidates = (0 until formats.length()).mapNotNull { index ->
                        val item = formats.optJSONObject(index) ?: return@mapNotNull null
                        val url = item.optString("url").takeIf { it.startsWith("https://") } ?: return@mapNotNull null
                        if (!item.optString("type").contains("video/mp4", true)) return@mapNotNull null
                        val height = item.optString("resolution").substringAfter("x", "").toIntOrNull()
                            ?: Regex("(\\d+)p").find(item.optString("qualityLabel"))?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: 0
                        Triple(url, height, item.optLong("bitrate", 0L))
                    }
                    val chosen = candidates.filter { it.second in 1..720 }
                        .maxWithOrNull(compareBy<Triple<String, Int, Long>> { it.second }.thenBy { it.third })
                        ?: candidates.maxWithOrNull(compareBy<Triple<String, Int, Long>> { it.second }.thenBy { it.third })
                    if (chosen != null) return chosen.first
                }
                val hls = json.optString("hlsUrl").takeIf { it.startsWith("https://") && it != "null" }
                if (hls != null) return hls
            } catch (_: Exception) {
                // Continue to another instance.
            } finally {
                connection?.disconnect()
            }
        }
        return null
    }

    private fun closeNativePlayer() {
        if (fullscreenActive) {
            fullscreenActive = false
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        exitFullscreenAction = null
        releaseNativePlayer()
        playerVisible = false
        if (activeSearch.isNotBlank()) {
            loadNativeSearch(activeSearch)
        } else {
            showHome()
        }
    }

    private fun releaseNativePlayer() {
        nativePlayerView?.player = null
        nativePlayerView = null
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun showInternalPage(url: String, heading: String) {
        homeVisible = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        applySystemInsets(root)
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
        root.addView(View(this).apply {
            setBackgroundColor(Color.rgb(42, 42, 42))
            layoutParams = LinearLayout.LayoutParams(-1, dp(1))
        })
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accentColor)
            layoutParams = LinearLayout.LayoutParams(-1, dp(2))
        }
        root.addView(progress)

        val browser = WebView(this).apply {
            setBackgroundColor(backgroundColor)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = true
            settings.allowContentAccess = true
            settings.allowFileAccess = false
            settings.setSupportZoom(false)
            android.webkit.CookieManager.getInstance().setAcceptCookie(true)
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val scheme = request.url.scheme?.lowercase()
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
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        webView?.stopLoading()
        webView?.destroy()
        webView = browser
        root.addView(browser)
        setContentView(root)
        // Load the real mobile watch page instead of a data-URL iframe.
        // YouTube can reject embedded players in WebView with "Video not found" / error 153.
        browser.loadUrl(url)
    }

    private fun showHome() {
        playerVisible = false
        releaseNativePlayer()
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        searchProgress = null
        resultsContainer = null
        homeVisible = true
        setContentView(R.layout.activity_main)
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { applySystemInsets(it) }
        bindHome()
    }

    @Suppress("DEPRECATION")
    private fun applySystemInsets(view: View) {
        val initialLeft = view.paddingLeft
        val initialRight = view.paddingRight
        view.setOnApplyWindowInsetsListener { target, insets ->
            target.setPadding(initialLeft, insets.systemWindowInsetTop, initialRight, insets.systemWindowInsetBottom)
            insets
        }
        view.requestApplyInsets()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (playerVisible && fullscreenActive) {
            exitFullscreenAction?.invoke()
            return
        }
        if (playerVisible) {
            closeNativePlayer()
            return
        }
        val browser = webView
        if (!homeVisible && browser != null) {
            if (browser.canGoBack()) browser.goBack() else showHome()
        } else if (!homeVisible) {
            showHome()
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
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        fullscreenActive = false
        exitFullscreenAction = null
        releaseNativePlayer()
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
        networkExecutor.shutdownNow()
        super.onDestroy()
    }
}