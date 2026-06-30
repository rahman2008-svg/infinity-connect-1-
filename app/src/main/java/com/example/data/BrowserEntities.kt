package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val isFolder: Boolean = false,
    val parentId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String, // typically UUID or URL hash
    val filename: String,
    val url: String,
    val filePath: String,
    val size: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String, // "PENDING", "DOWNLOADING", "PAUSED", "COMPLETED", "FAILED"
    val mimeType: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val tabId: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean = false,
    val isActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
