package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*

@Composable
fun YouTubeApp(
    viewModel: YouTubeViewModel,
    onEnterPipMode: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val currentPlayingVideo by viewModel.currentPlayingVideo.collectAsState()
    val isPlayerMinimized by viewModel.isPlayerMinimized.collectAsState()
    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val isNotificationsOpen by viewModel.isNotificationsOpen.collectAsState()
    val isCreateSheetOpen by viewModel.isCreateSheetOpen.collectAsState()
    val selectedChannelId by viewModel.selectedChannelId.collectAsState()
    val saveToPlaylistVideoId by viewModel.saveToPlaylistVideoId.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCastDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Back handler when on secondary bottom nav tabs
    BackHandler(
        enabled = selectedTab != YouTubeTab.HOME &&
            currentPlayingVideo == null &&
            !isSearchOpen &&
            !isNotificationsOpen &&
            selectedChannelId == null
    ) {
        viewModel.selectTab(YouTubeTab.HOME)
    }

    val isFullPlayerVisible = currentPlayingVideo != null && !isPlayerMinimized

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!isFullPlayerVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .navigationBarsPadding()
                ) {
                    // Docked Mini-Player Bar right above Bottom Navigation Bar
                    currentPlayingVideo?.let { activeVideo ->
                        if (isPlayerMinimized) {
                            VideoWatchOverlay(
                                video = activeVideo,
                                viewModel = viewModel,
                                onEnterPipMode = onEnterPipMode
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    YouTubeBottomNavigationBar(
                        selectedTab = selectedTab,
                        onSelectTab = { tab ->
                            viewModel.closeSearch()
                            viewModel.closeNotifications()
                            viewModel.closeChannelProfile()
                            viewModel.selectTab(tab)
                        },
                        onCreateClick = { viewModel.openCreateSheet() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Primary 4 Tabs
            when (selectedTab) {
                YouTubeTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onEnterCast = { showCastDialog = true }
                )
                YouTubeTab.SHORTS -> ShortsScreen(viewModel = viewModel)
                YouTubeTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                    viewModel = viewModel,
                    onEnterCast = { showCastDialog = true }
                )
                YouTubeTab.YOU -> YouLibraryScreen(
                    viewModel = viewModel,
                    onEnterCast = { showCastDialog = true }
                )
            }

            // Channel Profile Overlay
            selectedChannelId?.let { channelId ->
                ChannelProfileOverlayScreen(
                    channelId = channelId,
                    viewModel = viewModel
                )
            }

            // Notifications Overlay
            if (isNotificationsOpen) {
                NotificationsOverlayScreen(viewModel = viewModel)
            }

            // Search Overlay
            if (isSearchOpen) {
                SearchOverlayScreen(viewModel = viewModel)
            }

            // Full-Screen Watch Page Overlay
            currentPlayingVideo?.let { activeVideo ->
                if (!isPlayerMinimized) {
                    VideoWatchOverlay(
                        video = activeVideo,
                        viewModel = viewModel,
                        onEnterPipMode = onEnterPipMode
                    )
                }
            }
        }
    }

    // Create / Upload Bottom Sheet
    if (isCreateSheetOpen) {
        CreateStudioBottomSheet(viewModel = viewModel)
    }

    // Save to Playlist Dialog
    saveToPlaylistVideoId?.let { vidId ->
        SaveToPlaylistDialog(videoId = vidId, viewModel = viewModel)
    }

    // Cast to Device Dialog
    if (showCastDialog) {
        AlertDialog(
            onDismissRequest = { showCastDialog = false },
            title = { Text("Connect to a device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCastDialog = false
                                viewModel.showSnackbar("Connected to Living Room 4K OLED TV")
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Tv, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Living Room 4K OLED TV", fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCastDialog = false
                                viewModel.showSnackbar("Connected to Studio Chromecast Ultra")
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Speaker, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Studio Chromecast Ultra", fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCastDialog = false
                                viewModel.showSnackbar("Link with TV code opened")
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.PhonelinkSetup, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Link with TV code")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCastDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun YouTubeBottomNavigationBar(
    selectedTab: YouTubeTab,
    onSelectTab: (YouTubeTab) -> Unit,
    onCreateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
            .testTag("youtube_bottom_nav"),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = if (selectedTab == YouTubeTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
            label = "Home",
            isSelected = selectedTab == YouTubeTab.HOME,
            onClick = { onSelectTab(YouTubeTab.HOME) },
            testTag = "nav_tab_home"
        )

        BottomNavItem(
            icon = if (selectedTab == YouTubeTab.SHORTS) Icons.Filled.Bolt else Icons.Outlined.Bolt,
            label = "Shorts",
            isSelected = selectedTab == YouTubeTab.SHORTS,
            onClick = { onSelectTab(YouTubeTab.SHORTS) },
            testTag = "nav_tab_shorts"
        )

        // Authentic Center "+" Create Circle Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), CircleShape)
                .clickable { onCreateClick() }
                .testTag("nav_tab_create"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }

        BottomNavItem(
            icon = if (selectedTab == YouTubeTab.SUBSCRIPTIONS) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
            label = "Subscriptions",
            isSelected = selectedTab == YouTubeTab.SUBSCRIPTIONS,
            onClick = { onSelectTab(YouTubeTab.SUBSCRIPTIONS) },
            testTag = "nav_tab_subscriptions"
        )

        BottomNavItem(
            icon = if (selectedTab == YouTubeTab.YOU) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
            label = "You",
            isSelected = selectedTab == YouTubeTab.YOU,
            onClick = { onSelectTab(YouTubeTab.YOU) },
            testTag = "nav_tab_you"
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
