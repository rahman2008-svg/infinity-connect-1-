package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.*
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.NewsArticle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserApp(viewModel: BrowserViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDarkTheme = viewModel.darkModeEnabled.collectAsState(initial = true).value

    // Back handler to navigate back in screens
    BackHandler(enabled = currentScreen != "home") {
        when (currentScreen) {
            "browser" -> viewModel.setScreen("home")
            "video_player" -> viewModel.clearActiveVideo()
            else -> viewModel.setScreen("home")
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "splash" -> SplashScreen(viewModel)
                    "onboarding" -> OnboardingScreen(viewModel)
                    "home" -> HomeScreen(viewModel)
                    "browser" -> WebViewBrowserScreen(viewModel)
                    "tabs" -> TabsScreen(viewModel)
                    "downloads" -> DownloadsScreen(viewModel)
                    "file_manager" -> FileManagerScreen(viewModel)
                    "settings" -> SettingsScreen(viewModel)
                    "bookmarks" -> BookmarksScreen(viewModel)
                    "history" -> HistoryScreen(viewModel)
                    "video_player" -> BuiltInVideoPlayerScreen(viewModel)
                }
            }

            // Voice Search Overlay
            val isVoiceSearching by viewModel.isVoiceSearching.collectAsState()
            if (isVoiceSearching) {
                VoiceSearchOverlay(viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Splash Screen
// -------------------------------------------------------------
@Composable
fun SplashScreen(viewModel: BrowserViewModel) {
    var progress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            while (progress < 1f) {
                progress += 0.05f
                delay(80)
            }
            // Move to Onboarding on first launch, or direct to Home
            viewModel.setScreen("onboarding")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1D4ED8),
                        Color(0xFF4F46E5)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Beautiful Infinity Logo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = "Infinity Connect Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "INFINITY CONNECT",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Text(
                text = "AI-Powered Secure Browser",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading Animation
            CircularProgressIndicator(
                progress = { progress },
                color = Color.White,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Restoring session...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// -------------------------------------------------------------
// 2. Onboarding Screen
// -------------------------------------------------------------
@Composable
fun OnboardingScreen(viewModel: BrowserViewModel) {
    var step by remember { mutableIntStateOf(0) }
    val steps = listOf(
        OnboardingData(
            title = "Fast Browsing Engine",
            desc = "Turbocharge your web surfing with Chromium core optimizations and instant-load architecture.",
            icon = Icons.Default.Speed
        ),
        OnboardingData(
            title = "Strict Privacy Mode",
            desc = "Incognito browsing with zero trace. Cookies, history, and cache auto-shred on session exit.",
            icon = Icons.Default.Security
        ),
        OnboardingData(
            title = "Smart Video Downloader",
            desc = "Auto-detect and download online videos in multiple formats directly to your library.",
            icon = Icons.Default.DownloadForOffline
        ),
        OnboardingData(
            title = "Gemini AI Translator",
            desc = "Translate, summarize, and explain web contents instantly powered by Google Gemini AI.",
            icon = Icons.Default.AutoAwesome
        )
    )

    val currentData = steps[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.setScreen("home") }) {
                    Text("Skip", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                }
            }

            // Core Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Feature Icon with gradient container
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                            )
                        )
                        .border(1.dp, Color(0xFFD0E2FF), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentData.icon,
                        contentDescription = currentData.title,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = currentData.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentData.desc,
                    fontSize = 15.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 22.sp
                )
            }

            // Bottom controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (index == step) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == step) Color(0xFF2563EB)
                                    else Color(0xFFCBD5E1)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (step < steps.size - 1) {
                            step++
                        } else {
                            viewModel.setScreen("home")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (step == steps.size - 1) "Get Started" else "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class OnboardingData(
    val title: String,
    val desc: String,
    val icon: ImageVector
)

// -------------------------------------------------------------
// 3. Home Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BrowserViewModel) {
    val searchBarText by viewModel.searchBarText.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val newsFeed by viewModel.newsFeed.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddSpeedDialDialog by remember { mutableStateOf(false) }
    var speedDialsList = remember {
        mutableStateListOf(
            SpeedDialItem("Google", "https://google.com", Icons.Default.Search),
            SpeedDialItem("YouTube", "https://youtube.com", Icons.Default.PlayArrow),
            SpeedDialItem("Facebook", "https://facebook.com", Icons.Default.ThumbUp),
            SpeedDialItem("Wikipedia", "https://wikipedia.org", Icons.Default.Book),
            SpeedDialItem("Amazon", "https://amazon.com", Icons.Default.ShoppingCart),
            SpeedDialItem("ChatGPT", "https://chatgpt.com", Icons.Default.Chat)
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                actions = {
                    IconButton(
                        onClick = { viewModel.isIncognito.value = !isIncognito },
                        modifier = Modifier.testTag("incognito_button")
                    ) {
                        Icon(
                            imageVector = if (isIncognito) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Incognito",
                            tint = if (isIncognito) Color(0xFFEF4444) else Color(0xFF64748B)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(viewModel, "home")
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 16.dp)
        ) {
            // 0. Branding Header (Professional Polish Style)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2563EB), Color(0xFF6366F1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Infinity Connect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color(0xFF1E293B)
                    )
                    
                    Text(
                        text = "AI-Powered Secure Browsing",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchBarText,
                    onValueChange = { viewModel.updateSearchBarText(it) },
                    placeholder = { Text("Search or enter web URL...", color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("search_input"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.isVoiceSearching.value = true }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = Color(0xFF2563EB)
                                )
                              }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.navigateTo(searchBarText) }
                    ),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B)
                    )
                )
            }

            // 2. Speed Dial Grid
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Speed Dial",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val list = speedDialsList.toList()
                    GridSpeedDial(list, onSelect = { viewModel.navigateTo(it.url) }, onAdd = { showAddSpeedDialDialog = true })
                }
            }

            // 3. Quick Shortcuts
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Your Shortcuts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShortcutCard(
                        title = "Bookmarks",
                        icon = Icons.Default.Bookmark,
                        count = bookmarks.size.toString(),
                        onClick = { viewModel.setScreen("bookmarks") }
                    )
                    ShortcutCard(
                        title = "History",
                        icon = Icons.Default.History,
                        count = "",
                        onClick = { viewModel.setScreen("history") }
                    )
                    ShortcutCard(
                        title = "Downloads",
                        icon = Icons.Default.Download,
                        count = "",
                        onClick = { viewModel.setScreen("downloads") }
                    )
                    ShortcutCard(
                        title = "Files",
                        icon = Icons.Default.Folder,
                        count = "",
                        onClick = { viewModel.setScreen("file_manager") }
                    )
                }
            }

            // 4. Smart AI News Feed
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Discover",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    TextButton(onClick = { /* Option to personalize */ }) {
                        Text(
                            "Personalize",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(newsFeed) { article ->
                NewsFeedCard(article, viewModel)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Add speed dial dialog
    if (showAddSpeedDialDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSpeedDialDialog = false },
            title = { Text("Add Speed Dial") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Site Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("Web URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotEmpty() && newUrl.isNotEmpty()) {
                            val formattedUrl = if (newUrl.startsWith("http")) newUrl else "https://$newUrl"
                            speedDialsList.add(SpeedDialItem(newTitle, formattedUrl, Icons.Default.Language))
                            showAddSpeedDialDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSpeedDialDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Translation/Summary Result dialog
    val aiResult by viewModel.aiResult.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    if (aiResult != null || aiLoading) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAIResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini Intelligence")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (aiLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gemini is reading and processing...", fontSize = 12.sp)
                        }
                    } else {
                        Column {
                            Text(
                                text = aiResult ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearAIResult() }) {
                    Text("Awesome")
                }
            }
        )
    }
}

data class SpeedDialItem(
    val title: String,
    val url: String,
    val icon: ImageVector
)

@Composable
fun GridSpeedDial(list: List<SpeedDialItem>, onSelect: (SpeedDialItem) -> Unit, onAdd: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(list) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(item) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.title) {
                        "Google" -> Text("G", color = Color(0xFF3B82F6), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        "YouTube" -> Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("YT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        "Facebook" -> Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1D4ED8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("f", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        "Wikipedia" -> Text("W", color = Color(0xFF1E293B), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        "Amazon" -> Text("🛒", fontSize = 20.sp)
                        "ChatGPT" -> Text("AI", color = Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        else -> {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.title,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
            }
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAdd() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(
                            width = 1.dp,
                            color = Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color(0xFF94A3B8), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "More",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
fun ShortcutCard(title: String, icon: ImageVector, count: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(26.dp)
                )
                if (count.isNotEmpty() && count != "0") {
                    Box(
                        modifier = Modifier
                            .offset(8.dp, (-4).dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(count, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NewsFeedCard(article: NewsArticle, viewModel: BrowserViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.navigateTo(article.url) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFFEFF6FF),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            article.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { viewModel.explainOrTranslateWithAI(article.summary, "summarize") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Summarize with Gemini AI",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.explainOrTranslateWithAI(article.summary, "translate to English") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Translate to English with Gemini AI",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = article.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = article.summary,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (article.category == "Tech") "TechCrunch" else if (article.category == "AI") "Inside Infinity" else "Wired",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )
                    Text(
                        text = if (article.id == "1") "2h ago" else if (article.id == "2") "5h ago" else "1d ago",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            val gradient = when (article.id) {
                "1" -> Brush.linearGradient(listOf(Color(0xFFE0E7FF), Color(0xFFDBEAFE)))
                "2" -> Brush.linearGradient(listOf(Color(0xFFCCFBF1), Color(0xFFD1FAE5)))
                else -> Brush.linearGradient(listOf(Color(0xFFFCE7F3), Color(0xFFF3E8FF)))
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (article.category == "Tech") Icons.Default.Tv
                                  else if (article.category == "AI") Icons.Default.AutoAwesome
                                  else Icons.Default.Info,
                    contentDescription = null,
                    tint = when (article.id) {
                        "1" -> Color(0xFF4F46E5)
                        "2" -> Color(0xFF0D9488)
                        else -> Color(0xFFDB2777)
                    }.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 4. WebView Screen
// -------------------------------------------------------------
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewBrowserScreen(viewModel: BrowserViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val searchBarText by viewModel.searchBarText.collectAsState()
    val webProgress by viewModel.webProgress.collectAsState()
    val detectedVideoUrl by viewModel.detectedVideoUrl.collectAsState()
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setScreen("home") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    OutlinedTextField(
                        value = searchBarText,
                        onValueChange = { viewModel.updateSearchBarText(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("browser_search_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.navigateTo(searchBarText) }),
                        trailingIcon = {
                            if (searchBarText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchBarText("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    )

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }

                // Progress Indicator
                if (webProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(viewModel, "browser")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Android WebView
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewInstance = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Web Settings configuration
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                url?.let {
                                    viewModel.updateSearchBarText(it)
                                    viewModel.onWebProgressChanged(10)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                viewModel.onWebProgressChanged(100)
                                url?.let {
                                    viewModel.onWebTitleChanged(view?.title ?: "Browser Page")
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString() ?: ""

                                // Video sniffer
                                if (reqUrl.contains(".mp4") || reqUrl.contains(".webm") || reqUrl.contains(".m3u8")) {
                                    viewModel.detectVideoOnPage(reqUrl)
                                }

                                // Simple AdBlocker
                                if (adBlockEnabled) {
                                    val adDomains = listOf("doubleclick", "googleads", "pagead", "adservice", "popads", "adcolony")
                                    for (ad in adDomains) {
                                        if (reqUrl.contains(ad)) {
                                            // Intercept & block ad by returning empty response
                                            return WebResourceResponse("text/javascript", "UTF-8", null)
                                        }
                                    }
                                }

                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                viewModel.onWebProgressChanged(newProgress)
                            }
                        }

                        loadUrl(currentUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != currentUrl) {
                        webView.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Video Sniffer floating notification button!
            if (detectedVideoUrl != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            detectedVideoUrl?.let { url ->
                                viewModel.triggerDownload(url)
                                viewModel.playVideo(url)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = Color.White
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = "Download Video")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Video", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. Tabs Switcher Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(viewModel: BrowserViewModel) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Tabs (${tabs.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.createNewTab() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add New Tab")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(viewModel, "tabs")
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tabs) { tab ->
                val isActive = tab.tabId == activeTabId
                Card(
                    modifier = Modifier
                        .height(140.dp)
                        .clickable { viewModel.switchTab(tab.tabId) }
                        .border(
                            width = if (isActive) 3.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.closeTab(tab.tabId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Tab", modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = tab.url,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (tab.isIncognito) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Incognito",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. Downloads Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: BrowserViewModel) {
    val downloads by viewModel.downloads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads Manager", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            BottomNavigationBar(viewModel, "downloads")
        }
    ) { innerPadding ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DownloadForOffline,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No downloads yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads) { download ->
                    DownloadItemCard(download, viewModel)
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(download: DownloadEntity, viewModel: BrowserViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        download.filename,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        download.url,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { viewModel.deleteDownloadItem(download.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = if (download.size > 0) download.downloadedBytes.toFloat() / download.size else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (download.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%  •  ${download.status}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Row {
                    if (download.status == "DOWNLOADING") {
                        TextButton(onClick = { viewModel.pauseDownload(download.id) }) {
                            Text("Pause")
                        }
                    } else if (download.status == "PAUSED") {
                        TextButton(onClick = { viewModel.resumeDownload(download.id) }) {
                            Text("Resume")
                        }
                    } else if (download.status == "COMPLETED") {
                        TextButton(onClick = { viewModel.playVideo(download.filePath) }) {
                            Text("Play File")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. Built-in Video Player Screen
// -------------------------------------------------------------
@Composable
fun BuiltInVideoPlayerScreen(viewModel: BrowserViewModel) {
    val activeVideoUrl by viewModel.activeVideoUrl.collectAsState()
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Gestures states
    var volume by remember { mutableFloatStateOf(0.7f) }
    var brightness by remember { mutableFloatStateOf(0.8f) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        showVolumeOverlay = false
                        showBrightnessOverlay = false
                    },
                    onDragCancel = {
                        showVolumeOverlay = false
                        showBrightnessOverlay = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val isRightSide = change.position.x > size.width / 2
                        if (isRightSide) {
                            // Volume control
                            volume = (volume - dragAmount.y / 800f).coerceIn(0f, 1f)
                            showVolumeOverlay = true
                        } else {
                            // Brightness control
                            brightness = (brightness - dragAmount.y / 800f).coerceIn(0f, 1f)
                            showBrightnessOverlay = true
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.clearActiveVideo() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Infinity Smart Player", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(activeVideoUrl ?: "", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, maxLines = 1)
                }
            }

            // Visual Center: Video frame mock
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayCircleFilled else Icons.Default.PauseCircleFilled,
                        contentDescription = null,
                        tint = Color.Cyan,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Playing: ${activeVideoUrl?.substringAfterLast("/")}", color = Color.White, fontSize = 14.sp)
                }

                // Volume Overlay
                if (showVolumeOverlay) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Vol: ${(volume * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Brightness Overlay
                if (showBrightnessOverlay) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bright: ${(brightness * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Controllers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                // Seek progress bar
                var sliderPos by remember { mutableStateOf(0.4f) }
                Slider(
                    value = sliderPos,
                    onValueChange = { sliderPos = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Cyan,
                        activeTrackColor = Color.Cyan,
                        inactiveTrackColor = Color.Gray
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("02:15 / 05:40", color = Color.White, fontSize = 12.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isPlaying = !isPlaying }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Speed selector
                        TextButton(
                            onClick = {
                                playbackSpeed = when (playbackSpeed) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.5f
                                    else -> 1.0f
                                }
                            }
                        ) {
                            Text("${playbackSpeed}x", color = Color.Cyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = { viewModel.clearActiveVideo() }) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Screen", tint = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. File Manager Screen (Connected to Phone Storage & Hardware)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(viewModel: BrowserViewModel) {
    var activeCategory by remember { mutableStateOf("All") }
    val realFiles by viewModel.localFiles.collectAsState()
    val context = LocalContext.current

    // Trigger local scan on enter
    LaunchedEffect(Unit) {
        viewModel.scanLocalFiles()
    }

    // State for create file dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    // State for viewing text file
    var showViewDialog by remember { mutableStateOf<com.example.viewmodel.LocalFileItem?>(null) }
    var viewFileContent by remember { mutableStateOf("") }

    // Dialog to create a file
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Real File on Phone", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This creates a real physical file in your mobile phone's local storage directory.", fontSize = 12.sp, color = Color(0xFF64748B))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Filename (e.g. notes.txt)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newFileContent,
                        onValueChange = { newFileContent = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            val success = viewModel.writeCustomFile(newFileName, newFileContent)
                            if (success) {
                                newFileName = ""
                                newFileContent = ""
                                showCreateDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Create File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog to view custom text file
    if (showViewDialog != null) {
        val fileItem = showViewDialog!!
        LaunchedEffect(fileItem) {
            viewFileContent = try {
                java.io.File(fileItem.path).readText()
            } catch (e: Exception) {
                "Unable to read file contents: ${e.message}"
            }
        }
        AlertDialog(
            onDismissRequest = { showViewDialog = null },
            title = { Text(fileItem.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Path: ${fileItem.path}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Divider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(viewFileContent, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = Color(0xFF334155))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showViewDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Storage & Files", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        bottomBar = {
            BottomNavigationBar(viewModel, "files")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Mobile Phone & Storage Connection Hub Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with Connected status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Connected Phone Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Phone metadata rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Phone Model
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mobile Device", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                "${viewModel.getDeviceManufacturer()} ${viewModel.getDeviceModel()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                maxLines = 1
                            )
                        }
                        // Android OS
                        Column(modifier = Modifier.weight(1f)) {
                            Text("System OS", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                "Android ${viewModel.getAndroidVersion()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Battery Status
                        val batteryPct = viewModel.getBatteryPercentage()
                        val isCharging = viewModel.isBatteryCharging()
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery Status", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                if (batteryPct >= 0) "$batteryPct% ${if (isCharging) "⚡" else ""}" else "N/A",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (batteryPct < 20 && !isCharging) Color(0xFFEF4444) else Color(0xFF334155)
                            )
                        }
                        // Network status
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Network Type", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                viewModel.getNetworkConnectionStatus(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Local Storage Statistics
                    val storageStats = viewModel.getPhoneStorageStats()
                    val totalGB = storageStats["totalGB"] as String
                    val freeGB = storageStats["freeGB"] as String
                    val usedGB = storageStats["usedGB"] as String
                    val percent = storageStats["percent"] as Float

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Local Storage Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        }
                        Text(
                            "$usedGB GB / $totalGB GB Used",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Linear Storage progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Free Space: $freeGB GB Available",
                            fontSize = 10.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${String.format("%.1f", percent)}% Used",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // 2. Interactive Storage Operations Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Text File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.scanLocalFiles()
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan storage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 3. Local Files Explorer Section
            Text(
                "Local Storage Files Explorer",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Category selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("All", "Downloads", "Images", "Videos", "Documents", "APKs")
                categories.forEach { cat ->
                    val isSelected = activeCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF2563EB) else Color.White)
                            .border(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable { activeCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            cat,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Filtered Local Files
            val filteredFiles = if (activeCategory == "All") {
                realFiles
            } else {
                realFiles.filter { it.category == activeCategory }
            }

            if (filteredFiles.isEmpty()) {
                // Empty state card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "No files in this category",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Go to search, download some media or create a custom text file above to see them in local storage.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredFiles.forEach { item ->
                        val itemIcon = when (item.category) {
                            "Videos" -> Icons.Default.Movie
                            "Images" -> Icons.Default.Image
                            "Documents" -> Icons.Default.Description
                            "APKs" -> Icons.Default.Android
                            else -> Icons.Default.FolderOpen
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.size} • Local path: ${item.path}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // View text file or play video button
                                    IconButton(
                                        onClick = {
                                            if (item.category == "Videos") {
                                                viewModel.playVideo(item.path)
                                            } else {
                                                showViewDialog = item
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Open File",
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Share button
                                    IconButton(
                                        onClick = {
                                            try {
                                                val file = java.io.File(item.path)
                                                // Create a share intent
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "*/*"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(file))
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Sharing file from Infinity Connect")
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "Look at this file: ${item.name}")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                val chooser = android.content.Intent.createChooser(shareIntent, "Share file to other phones")
                                                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(chooser)
                                            } catch (e: Exception) {
                                                // If raw file URI sharing fails on newer Androids without FileProvider, we can share text details or path
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "File Name: ${item.name}\nLocal Path: ${item.path}\nSize: ${item.size}\nSent via Infinity Connect Browser.")
                                                }
                                                val chooser = android.content.Intent.createChooser(shareIntent, "Share file details")
                                                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(chooser)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share to other phones",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteLocalFile(item.path)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete File",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// 9. Bookmarks & History Screens
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(viewModel: BrowserViewModel) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookmarks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen("home") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (bookmarks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No bookmarks added yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                items(bookmarks) { b ->
                    ListItem(
                        headlineContent = { Text(b.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(b.url, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteBookmark(b.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(b.url) }
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: BrowserViewModel) {
    val history by viewModel.history.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browsing History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.setScreen("home") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.clearAllHistory() }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("History is clean", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                items(history) { h ->
                    ListItem(
                        headlineContent = { Text(h.title.ifEmpty { "Web Page" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(h.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteHistoryItem(h.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(h.url) }
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 10. Settings Screen
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: BrowserViewModel) {
    val adBlockEnabled by viewModel.adBlockEnabled.collectAsState()
    val searchEngine by viewModel.searchEngine.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val context = LocalContext.current

    val openUri = { url: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        } catch (e: java.lang.Exception) {
            // Safe fallback
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings & About", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(viewModel, "settings")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header: Preferences
            Text(
                "Preferences",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Ad Blocker Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Built-in Ad Block", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Block intrusive ads & trackers automatically.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = adBlockEnabled,
                        onCheckedChange = { viewModel.adBlockEnabled.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFF1F5F9)
                        )
                    )
                }
            }

            // Incognito Mode Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Incognito Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Enable by default to surf secretly.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isIncognito,
                        onCheckedChange = { viewModel.isIncognito.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFF1F5F9)
                        )
                    )
                }
            }

            // Search Engine Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Primary Search Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Google", "Bing", "DuckDuckGo").forEach { engine ->
                            val isSelected = searchEngine == engine
                            Button(
                                onClick = { viewModel.searchEngine.value = engine },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(engine, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Text Size Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Text Zoom Size", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Small", "Medium", "Large").forEach { size ->
                            val isSelected = textSize == size
                            Button(
                                onClick = { viewModel.textSize.value = size },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text(size, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Section Header: About Developer
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "About Developer",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Developer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "PA",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                "Prince AR Abdur Rahman",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                "Independent App Developer",
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color(0xFFE2E8F0))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Connect & Contact",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Contact Links
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { openUri("https://wa.me/8801707424006") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp 1",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "WhatsApp (Primary): 01707424006",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { openUri("https://wa.me/8801796951709") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp 2",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "WhatsApp (Secondary): 01796951709",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { openUri("https://www.facebook.com/share/1BNn32qoJo/") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Facebook",
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Facebook Profile",
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { openUri("https://www.instagram.com/ur___abdur____rahman__2008") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Instagram",
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Instagram Profile",
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Section Header: About Company
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "About Company & Studio",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Company Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                "NexVora Lab's Ofc",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                "Digital Product Lab",
                                fontSize = 12.sp,
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEEF2F6), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Our Mission",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Section Header: Technical Information & Credits
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Technical Info & Credits",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Technical details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Application Version", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("1.0.0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                    Divider(color = Color(0xFFF1F5F9))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Developed By", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("Prince AR Abdur Rahman", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                    Divider(color = Color(0xFFF1F5F9))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Published By", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("NexVora Lab's Ofc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// Helper UI: Shared Bottom Navigation Bar
// -------------------------------------------------------------
@Composable
fun BottomNavigationBar(viewModel: BrowserViewModel, activeTab: String) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .border(width = 1.dp, color = Color(0xFFE2E8F0))
    ) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF2563EB),
            selectedTextColor = Color(0xFF2563EB),
            unselectedIconColor = Color(0xFF94A3B8),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = Color(0xFFEFF6FF)
        )

        NavigationBarItem(
            selected = activeTab == "home",
            onClick = { viewModel.setScreen("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = activeTab == "browser",
            onClick = { viewModel.setScreen("browser") },
            icon = { Icon(Icons.Default.Language, contentDescription = "Browser") },
            label = { Text("Browse", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_browse")
        )

        NavigationBarItem(
            selected = activeTab == "tabs",
            onClick = { viewModel.setScreen("tabs") },
            icon = { Icon(Icons.Default.Layers, contentDescription = "Tabs") },
            label = { Text("Tabs", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_tabs")
        )

        NavigationBarItem(
            selected = activeTab == "files",
            onClick = { viewModel.setScreen("files") },
            icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Files") },
            label = { Text("Files", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_files")
        )

        NavigationBarItem(
            selected = activeTab == "settings",
            onClick = { viewModel.setScreen("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
            colors = navItemColors,
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

// -------------------------------------------------------------
// Helper UI: Voice Search Overlay
// -------------------------------------------------------------
@Composable
fun VoiceSearchOverlay(viewModel: BrowserViewModel) {
    val scope = rememberCoroutineScope()
    var isListeningState by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            // Auto simulate voice recognition result after 3 seconds
            delay(3000)
            isListeningState = false
            viewModel.handleVoiceResult("James Webb Space Telescope findings")
            viewModel.isVoiceSearching.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone",
                tint = Color.Cyan,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isListeningState) "Listening closely..." else "Processing voice...",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Say 'Google', 'YouTube' or 'Download Video'",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(color = Color.Cyan)

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = { viewModel.isVoiceSearching.value = false }) {
                Text("Cancel", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        }
    }
}
