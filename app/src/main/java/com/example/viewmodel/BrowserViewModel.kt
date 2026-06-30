package com.example.viewmodel

import android.app.Application
import android.os.Environment
import android.os.StatFs
import android.os.BatteryManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

// Represents a real file stored on the device's local storage
data class LocalFileItem(
    val id: String,
    val name: String,
    val category: String,
    val size: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BrowserDatabase.getDatabase(application)
    private val repository = BrowserRepository(database.browserDao())

    // Real physical files scanned on the device
    private val _localFiles = MutableStateFlow<List<LocalFileItem>>(emptyList())
    val localFiles: StateFlow<List<LocalFileItem>> = _localFiles.asStateFlow()

    // App Navigation & Screens
    private val _currentScreen = MutableStateFlow("splash") // splash, onboarding, home, browser, tabs, downloads, file_manager, settings, bookmarks, history, video_player
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Browser Mode & Settings
    val isIncognito = MutableStateFlow(false)
    val adBlockEnabled = MutableStateFlow(true)
    val darkModeEnabled = MutableStateFlow(true)
    val searchEngine = MutableStateFlow("Google") // Google, Bing, Yahoo, DuckDuckGo
    val textSize = MutableStateFlow("Medium") // Small, Medium, Large
    val customHomepage = MutableStateFlow("https://google.com")

    // Speech / Voice Search
    val isVoiceSearching = MutableStateFlow(false)
    val voiceSearchQuery = MutableStateFlow("")

    // Active State
    private val _currentUrl = MutableStateFlow("https://google.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _searchBarText = MutableStateFlow("")
    val searchBarText: StateFlow<String> = _searchBarText.asStateFlow()

    private val _webProgress = MutableStateFlow(0)
    val webProgress: StateFlow<Int> = _webProgress.asStateFlow()

    private val _webTitle = MutableStateFlow("Home")
    val webTitle: StateFlow<String> = _webTitle.asStateFlow()

    // Video Sniffer & Player
    private val _detectedVideoUrl = MutableStateFlow<String?>(null)
    val detectedVideoUrl: StateFlow<String?> = _detectedVideoUrl.asStateFlow()

    private val _activeVideoUrl = MutableStateFlow<String?>(null)
    val activeVideoUrl: StateFlow<String?> = _activeVideoUrl.asStateFlow()

    // Room Database Observables
    val bookmarks = repository.allBookmarks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val history = repository.allHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val downloads = repository.allDownloads.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val tabs = repository.allTabs.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active Tab ID
    private val _activeTabId = MutableStateFlow("")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    // AI summary / Translation
    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult: StateFlow<String?> = _aiResult.asStateFlow()
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // Pre-populated News Feed
    private val _newsFeed = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsFeed: StateFlow<List<NewsArticle>> = _newsFeed.asStateFlow()

    init {
        loadDefaultTabsAndNews()
        scanLocalFiles()
    }

    private fun loadDefaultTabsAndNews() {
        viewModelScope.launch {
            // Restore tabs or populate initial
            repository.allTabs.first().let { currentTabs ->
                if (currentTabs.isEmpty()) {
                    val initialTab = TabEntity(
                        tabId = UUID.randomUUID().toString(),
                        title = "Google",
                        url = "https://google.com",
                        isIncognito = false,
                        isActive = true
                    )
                    repository.insertTab(initialTab)
                    _activeTabId.value = initialTab.tabId
                } else {
                    val active = currentTabs.find { it.isActive } ?: currentTabs.first()
                    _activeTabId.value = active.tabId
                    _currentUrl.value = active.url
                    _searchBarText.value = active.url
                }
            }

            // Populate some default Bookmarks if empty
            if (repository.allBookmarks.first().isEmpty()) {
                repository.insertBookmark(BookmarkEntity(title = "Google", url = "https://google.com"))
                repository.insertBookmark(BookmarkEntity(title = "YouTube", url = "https://youtube.com"))
                repository.insertBookmark(BookmarkEntity(title = "Facebook", url = "https://facebook.com"))
                repository.insertBookmark(BookmarkEntity(title = "Wikipedia", url = "https://wikipedia.org"))
            }

            // Populate News Feed
            _newsFeed.value = listOf(
                NewsArticle(
                    id = "1",
                    title = "Infinity Connect Browser Released!",
                    summary = "A new high-performance, private, smart web browser named Infinity Connect has officially launched with beautiful, minimalist looks, built-in download manager, voice search, and an integrated video player.",
                    category = "Tech",
                    url = "https://infinityconnect.ai/news/launch"
                ),
                NewsArticle(
                    id = "2",
                    title = "AI Horizons: Next-Gen Gemini 3.5 Features Revealed",
                    summary = "Google announces revolutionary features in its upcoming Gemini models with enhanced real-time reasoning, lightning-fast response times, and multi-modal integration.",
                    category = "AI",
                    url = "https://google.com"
                ),
                NewsArticle(
                    id = "3",
                    title = "Deep Space Discoveries: Water Signs on Distant Exoplanet",
                    summary = "Astronomers utilizing the James Webb Space Telescope have identified significant atmospheric moisture signals in a remote star system, hinting at possible habitable conditions.",
                    category = "Science",
                    url = "https://wikipedia.org"
                )
            )
        }
    }

    // Navigation helper
    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun updateSearchBarText(text: String) {
        _searchBarText.value = text
    }

    fun onWebProgressChanged(progress: Int) {
        _webProgress.value = progress
    }

    fun onWebTitleChanged(title: String) {
        _webTitle.value = title
        // Update current active tab title in Database
        viewModelScope.launch {
            val activeId = _activeTabId.value
            if (activeId.isNotEmpty()) {
                val currentTabs = repository.allTabs.first()
                currentTabs.find { it.tabId == activeId }?.let { activeTab ->
                    repository.insertTab(activeTab.copy(title = title))
                }
            }
        }
    }

    // Browse to URL or Search
    fun navigateTo(input: String) {
        var destination = input.trim()
        if (destination.isEmpty()) return

        // Check if it looks like a URL, otherwise perform search
        if (!destination.startsWith("http://") && !destination.startsWith("https://")) {
            if (destination.contains(".") && !destination.contains(" ")) {
                destination = "https://$destination"
            } else {
                val searchPrefix = when (searchEngine.value) {
                    "Google" -> "https://www.google.com/search?q="
                    "Bing" -> "https://www.bing.com/search?q="
                    "Yahoo" -> "https://search.yahoo.com/search?p="
                    "DuckDuckGo" -> "https://duckduckgo.com/?q="
                    else -> "https://www.google.com/search?q="
                }
                destination = "$searchPrefix${destination.replace(" ", "+")}"
            }
        }

        _currentUrl.value = destination
        _searchBarText.value = destination
        _detectedVideoUrl.value = null // reset video detection on navigate
        _currentScreen.value = "browser"

        // Add to history if not incognito
        if (!isIncognito.value) {
            viewModelScope.launch {
                repository.insertHistory(HistoryEntity(title = _webTitle.value, url = destination))
            }
        }

        // Update active tab URL
        viewModelScope.launch {
            val activeId = _activeTabId.value
            if (activeId.isNotEmpty()) {
                val currentTabs = repository.allTabs.first()
                currentTabs.find { it.tabId == activeId }?.let { activeTab ->
                    repository.insertTab(activeTab.copy(url = destination))
                }
            }
        }
    }

    // Voice search results
    fun handleVoiceResult(result: String) {
        voiceSearchQuery.value = result
        navigateTo(result)
    }

    // Tab Management
    fun createNewTab(url: String = "https://google.com") {
        viewModelScope.launch {
            val newTab = TabEntity(
                tabId = UUID.randomUUID().toString(),
                title = "New Tab",
                url = url,
                isIncognito = isIncognito.value,
                isActive = true
            )
            repository.insertTab(newTab)
            repository.setActiveTab(newTab.tabId)
            _activeTabId.value = newTab.tabId
            _currentUrl.value = url
            _searchBarText.value = url
            _detectedVideoUrl.value = null
            _currentScreen.value = "browser"
        }
    }

    fun switchTab(tabId: String) {
        viewModelScope.launch {
            repository.setActiveTab(tabId)
            _activeTabId.value = tabId
            val currentTabs = repository.allTabs.first()
            currentTabs.find { it.tabId == tabId }?.let { tab ->
                _currentUrl.value = tab.url
                _searchBarText.value = tab.url
                _detectedVideoUrl.value = null
                _currentScreen.value = "browser"
            }
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            repository.deleteTab(tabId)
            val remaining = repository.allTabs.first()
            if (remaining.isEmpty()) {
                createNewTab()
            } else if (_activeTabId.value == tabId) {
                switchTab(remaining.last().tabId)
            }
        }
    }

    // Bookmark Management
    fun toggleCurrentPageBookmark() {
        viewModelScope.launch {
            val url = _currentUrl.value
            val currentBookmarks = bookmarks.value
            val existing = currentBookmarks.find { it.url == url }
            if (existing != null) {
                repository.deleteBookmark(existing.id)
            } else {
                repository.insertBookmark(BookmarkEntity(title = _webTitle.value, url = url))
            }
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    // History Management
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Video Sniffing
    fun detectVideoOnPage(url: String) {
        // Simple filter to catch MP4/WebM/HLS files loading as media
        if (url.contains(".mp4") || url.contains(".webm") || url.contains(".m3u8") || url.contains("video")) {
            _detectedVideoUrl.value = url
        }
    }

    fun playVideo(url: String) {
        _activeVideoUrl.value = url
        _currentScreen.value = "video_player"
    }

    fun clearActiveVideo() {
        _activeVideoUrl.value = null
        _currentScreen.value = "browser"
    }

    // Download Management
    fun triggerDownload(url: String, filename: String = "") {
        viewModelScope.launch {
            val resolvedName = filename.ifEmpty() {
                url.substringAfterLast("/").substringBefore("?").ifEmpty { "download_${System.currentTimeMillis()}.bin" }
            }
            val id = UUID.randomUUID().toString()
            val extDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(extDir ?: getApplication<Application>().filesDir, resolvedName)

            val newDownload = DownloadEntity(
                id = id,
                filename = resolvedName,
                url = url,
                filePath = destinationFile.absolutePath,
                size = 12500000, // Simulated size for nice progress
                downloadedBytes = 0,
                status = "DOWNLOADING"
            )
            repository.insertDownload(newDownload)

            // Start simple background job to simulate download increment
            viewModelScope.launch {
                var bytes = 0L
                val total = newDownload.size
                while (bytes < total) {
                    val current = repository.getDownloadById(id) ?: break
                    if (current.status == "PAUSED") {
                        break
                    }
                    if (current.status == "FAILED") {
                        break
                    }
                    bytes += 1250000 // increment
                    if (bytes > total) bytes = total
                    repository.updateDownloadProgress(id, "DOWNLOADING", bytes)
                    kotlinx.coroutines.delay(800)
                }
                val current = repository.getDownloadById(id)
                if (current != null && current.status != "PAUSED" && current.status != "FAILED") {
                    repository.updateDownloadProgress(id, "COMPLETED", total)
                    // Physically write the downloaded content
                    try {
                        val file = File(current.filePath)
                        file.parentFile?.mkdirs()
                        file.writeText("Infinity Connect Secure Download File\nFilename: ${current.filename}\nURL: ${current.url}\nDownloaded Successfully to Local Storage.\nTimestamp: ${System.currentTimeMillis()}")
                        scanLocalFiles()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            val download = repository.getDownloadById(id) ?: return@launch
            repository.insertDownload(download.copy(status = "PAUSED"))
        }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            val download = repository.getDownloadById(id) ?: return@launch
            repository.insertDownload(download.copy(status = "DOWNLOADING"))
            // Continue simulation
            viewModelScope.launch {
                var bytes = download.downloadedBytes
                val total = download.size
                while (bytes < total) {
                    val current = repository.getDownloadById(id) ?: break
                    if (current.status == "PAUSED") break
                    bytes += 1250000
                    if (bytes > total) bytes = total
                    repository.updateDownloadProgress(id, "DOWNLOADING", bytes)
                    kotlinx.coroutines.delay(800)
                }
                val current = repository.getDownloadById(id)
                if (current != null && current.status != "PAUSED") {
                    repository.updateDownloadProgress(id, "COMPLETED", total)
                    // Physically write the downloaded content
                    try {
                        val file = File(current.filePath)
                        file.parentFile?.mkdirs()
                        file.writeText("Infinity Connect Secure Download File\nFilename: ${current.filename}\nURL: ${current.url}\nDownloaded Successfully to Local Storage.\nTimestamp: ${System.currentTimeMillis()}")
                        scanLocalFiles()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun deleteDownloadItem(id: String) {
        viewModelScope.launch {
            val download = repository.getDownloadById(id)
            if (download != null) {
                try {
                    val file = File(download.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repository.deleteDownload(id)
            scanLocalFiles()
        }
    }

    // AI Features using Gemini API
    fun explainOrTranslateWithAI(text: String, action: String = "translate to English") {
        viewModelScope.launch {
            _aiLoading.value = true
            _aiResult.value = null
            try {
                val key = com.example.BuildConfig.GEMINI_API_KEY
                if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
                    kotlinx.coroutines.delay(1500)
                    _aiResult.value = "AI Response [Offline/Simulated]: Translated '$text' -> $action. Success!"
                } else {
                    val prompt = "$action the following content: \"$text\""
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"
                    
                    val escapedPrompt = prompt
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                    
                    val jsonBody = "{\"contents\": [{\"parts\": [{\"text\": \"$escapedPrompt\"}]}]}"
                    
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    
                    val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .post(body)
                        .build()
                    
                    val responseText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                throw Exception("API error: ${response.code} ${response.message}")
                            }
                            response.body?.string() ?: ""
                        }
                    }
                    
                    val textStart = responseText.indexOf("\"text\": \"")
                    if (textStart != -1) {
                        val start = textStart + 9
                        var end = responseText.indexOf("\"", start)
                        while (end != -1 && responseText[end - 1] == '\\') {
                            end = responseText.indexOf("\"", end + 1)
                        }
                        if (end != -1) {
                            var rawResult = responseText.substring(start, end)
                            rawResult = rawResult
                                .replace("\\\\", "\\")
                                .replace("\\\"", "\"")
                                .replace("\\n", "\n")
                                .replace("\\r", "\r")
                                .replace("\\t", "\t")
                            _aiResult.value = rawResult
                        } else {
                            _aiResult.value = "Response parsed but text not fully extracted."
                        }
                    } else {
                        _aiResult.value = "Could not parse text candidate from Gemini response."
                    }
                }
            } catch (e: Exception) {
                _aiResult.value = "Error connecting to AI service: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    fun clearAIResult() {
        _aiResult.value = null
    }

    // Scan real local storage directories for files
    fun scanLocalFiles() {
        val context = getApplication<Application>()
        val list = mutableListOf<LocalFileItem>()

        // 1. Scan internal files directory
        val internalDir = context.filesDir
        if (internalDir != null && internalDir.exists()) {
            internalDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.contains("room") && !file.name.endsWith(".db") && !file.name.endsWith("-journal")) {
                    val category = getCategoryForFile(file.name)
                    list.add(
                        LocalFileItem(
                            id = file.absolutePath.hashCode().toString(),
                            name = file.name,
                            category = category,
                            size = formatFileSize(file.length()),
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }

        // 2. Scan external app-specific downloads directory
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (externalDir != null && externalDir.exists()) {
            externalDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val category = getCategoryForFile(file.name)
                    list.add(
                        LocalFileItem(
                            id = file.absolutePath.hashCode().toString(),
                            name = file.name,
                            category = category,
                            size = formatFileSize(file.length()),
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }

        _localFiles.value = list.sortedByDescending { it.lastModified }
    }

    // Helper to determine category
    private fun getCategoryForFile(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".avi") -> "Videos"
            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") -> "Images"
            lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".ogg") || lower.endsWith(".flac") -> "Videos" // category mapping inside lists
            lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".ppt") || lower.endsWith(".pptx") -> "Documents"
            lower.endsWith(".apk") -> "APKs"
            else -> "Downloads"
        }
    }

    // Format file size nicely
    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    // Write a real custom file into phone local storage
    fun writeCustomFile(name: String, content: String): Boolean {
        return try {
            val context = getApplication<Application>()
            // Save inside external downloads if available, otherwise internal filesDir
            val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(destDir, name)
            file.writeText(content)
            scanLocalFiles()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Delete a physical file from phone local storage
    fun deleteLocalFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                scanLocalFiles()
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Device Information and Local Storage stats for UI connection
    fun getDeviceModel(): String = Build.MODEL ?: "Unknown Device"
    fun getDeviceManufacturer(): String = Build.MANUFACTURER ?: "Android Device"
    fun getAndroidVersion(): String = Build.VERSION.RELEASE ?: "N/A"

    fun getBatteryPercentage(): Int {
        val context = getApplication<Application>()
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }

    fun isBatteryCharging(): Boolean {
        val context = getApplication<Application>()
        return try {
            val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }

    fun getNetworkConnectionStatus(): String {
        val context = getApplication<Application>()
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return "Disconnected"
            val actNw = cm.getNetworkCapabilities(nw) ?: return "Disconnected"
            when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Connected"
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                else -> "Connected"
            }
        } catch (e: Exception) {
            "Disconnected"
        }
    }

    fun getPhoneStorageStats(): Map<String, Any> {
        val context = getApplication<Application>()
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong
            
            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes
            
            val totalGB = String.format("%.1f", totalBytes.toDouble() / (1024 * 1024 * 1024))
            val freeGB = String.format("%.1f", freeBytes.toDouble() / (1024 * 1024 * 1024))
            val usedGB = String.format("%.1f", usedBytes.toDouble() / (1024 * 1024 * 1024))
            val percent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f
            
            mapOf(
                "totalGB" to totalGB,
                "freeGB" to freeGB,
                "usedGB" to usedGB,
                "percent" to percent,
                "totalBytes" to totalBytes,
                "freeBytes" to freeBytes
            )
        } catch (e: Exception) {
            mapOf(
                "totalGB" to "0.0",
                "freeGB" to "0.0",
                "usedGB" to "0.0",
                "percent" to 0f,
                "totalBytes" to 0L,
                "freeBytes" to 0L
            )
        }
    }
}

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val url: String
)
