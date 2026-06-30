package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val browserDao: BrowserDao) {

    // Bookmarks
    val allBookmarks: Flow<List<BookmarkEntity>> = browserDao.getAllBookmarks()
    suspend fun insertBookmark(bookmark: BookmarkEntity) = browserDao.insertBookmark(bookmark)
    suspend fun updateBookmark(bookmark: BookmarkEntity) = browserDao.updateBookmark(bookmark)
    suspend fun deleteBookmark(id: Long) = browserDao.deleteBookmark(id)

    // History
    val allHistory: Flow<List<HistoryEntity>> = browserDao.getAllHistory()
    suspend fun insertHistory(history: HistoryEntity) = browserDao.insertHistory(history)
    suspend fun deleteHistory(id: Long) = browserDao.deleteHistory(id)
    suspend fun clearHistory() = browserDao.clearHistory()

    // Downloads
    val allDownloads: Flow<List<DownloadEntity>> = browserDao.getAllDownloads()
    suspend fun getDownloadById(id: String) = browserDao.getDownloadById(id)
    suspend fun insertDownload(download: DownloadEntity) = browserDao.insertDownload(download)
    suspend fun updateDownloadProgress(id: String, status: String, downloadedBytes: Long) =
        browserDao.updateDownloadProgress(id, status, downloadedBytes)
    suspend fun deleteDownload(id: String) = browserDao.deleteDownload(id)

    // Tabs
    val allTabs: Flow<List<TabEntity>> = browserDao.getAllTabs()
    suspend fun insertTab(tab: TabEntity) = browserDao.insertTab(tab)
    suspend fun deleteTab(tabId: String) = browserDao.deleteTab(tabId)
    suspend fun clearTabs() = browserDao.clearTabs()
    suspend fun setActiveTab(tabId: String) = browserDao.setActiveTab(tabId)
}
