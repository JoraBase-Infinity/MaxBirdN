package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.BuildConfig
import com.example.auth.SessionManager
import com.example.ui.dialogs.FcmLiveTerminalDialog
import com.example.ui.dialogs.UpdateDialog
import com.example.update.AppUpdateInfo
import com.example.update.AppUpdateRepository
import com.example.utils.AppCacheManager
import com.example.utils.AvatarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToChangeSyllabus: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCourseEnrollment: () -> Unit = {},
    onNavigateToSavedItems: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToReportCard: () -> Unit = {},
    onNavigateToNotification: () -> Unit = {},
    onNavigateToHeaderWallpaper: () -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentThemeMode by sessionManager.themeModeFlow.collectAsState()
    val currentLeadTime by sessionManager.classNotificationLeadTimeFlow.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showTerminalDialog by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }

    var formattedCacheSize by remember { mutableStateOf("0.0 MB") }

    LaunchedEffect(Unit) {
        formattedCacheSize = AppCacheManager.getFormattedCacheSize(context)
    }

    val profileUpdated by sessionManager.userProfileUpdateFlow.collectAsState()
    val userAvatarFlowValue by sessionManager.userAvatarFlow.collectAsState()
    val userAvatar = userAvatarFlowValue ?: sessionManager.getUserAvatar()
    val userName = remember(profileUpdated) { sessionManager.getUserFullName() ?: "শিক্ষার্থী" }
    val userClass = remember(profileUpdated) { sessionManager.getUserClassDisplay() ?: sessionManager.getUserClassName() ?: "একাদশ শ্রেণি" }
    val userGroup = remember(profileUpdated) { sessionManager.getUserGroup() ?: "মানবিক" }
    val userBatch = remember(profileUpdated) { sessionManager.getUserBatchId() ?: "" }

    val classDisplay = remember(userClass) {
        when (userClass) {
            "C11", "Class 11" -> "একাদশ শ্রেণি"
            "C12", "Class 12" -> "দ্বাদশ শ্রেণি"
            "C10", "Class 10" -> "দশম শ্রেণি"
            "C9", "Class 9" -> "নবম শ্রেণি"
            else -> userClass.ifBlank { "একাদশ শ্রেণি" }
        }
    }

    val groupDisplay = remember(userGroup) {
        when (userGroup.lowercase()) {
            "humanities", "humanities_group", "hum" -> "মানবিক"
            "science", "science_group", "sci" -> "বিজ্ঞান"
            "business", "business_studies", "commerce", "bs" -> "ব্যবসায় শিক্ষা"
            else -> userGroup
        }
    }

    val themeLabel = remember(currentThemeMode) {
        when (currentThemeMode) {
            "light" -> "লাইট মোড"
            "dark" -> "ডার্ক মোড"
            else -> "সিস্টেম ডিফল্ট"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সেটিংস",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showTerminalDialog = true }) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "FCM লাইভ টার্মিনাল",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Ultra-Clean Hero Profile Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onNavigateToEditProfile() },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                ),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Avatar with Ring
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            val fallbackInitial = remember(userName) {
                                userName.trim().firstOrNull()?.toString()?.uppercase() ?: "U"
                            }
                            val imageRequest = remember(userAvatar, userName, context) {
                                AvatarUtils.buildImageRequest(context, userAvatar, userName)
                            }

                            SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = "User Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fallbackInitial,
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fallbackInitial,
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ) {
                                Text(
                                    text = classDisplay,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            if (groupDisplay.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                ) {
                                    Text(
                                        text = groupDisplay,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // 2. Academic Section (Clean & Punchy)
            SettingsGroupCard(title = "একাডেমিক প্রোফাইল") {
                CleanSettingsRow(
                    icon = Icons.Default.Edit,
                    iconTint = Color(0xFF8B5CF6),
                    iconBg = Color(0xFF8B5CF6).copy(alpha = 0.14f),
                    title = "প্রোফাইল এডিট",
                    badgeText = "এডিট",
                    onClick = onNavigateToEditProfile
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.SwapCalls,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    title = "সিলেবাস ও শ্রেণি",
                    badgeText = classDisplay,
                    onClick = onNavigateToChangeSyllabus
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.Person,
                    iconTint = Color(0xFF0284C7),
                    iconBg = Color(0xFF0284C7).copy(alpha = 0.14f),
                    title = "প্রোফাইল বিবরণ",
                    onClick = onNavigateToProfile
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.CardMembership,
                    iconTint = Color(0xFF10B981),
                    iconBg = Color(0xFF10B981).copy(alpha = 0.14f),
                    title = "কোর্সে ভর্তি ও সাবস্ক্রিপশন",
                    badgeText = "সক্রিয়",
                    onClick = onNavigateToCourseEnrollment
                )
            }

            // 3. Learning & Resources Section
            SettingsGroupCard(title = "লার্নিং ও রিসোর্স") {
                CleanSettingsRow(
                    icon = Icons.Default.Bookmark,
                    iconTint = Color(0xFFF59E0B),
                    iconBg = Color(0xFFF59E0B).copy(alpha = 0.14f),
                    title = "সংরক্ষিত প্রশ্ন ও নোটস",
                    onClick = onNavigateToSavedItems
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.DownloadDone,
                    iconTint = Color(0xFF059669),
                    iconBg = Color(0xFF059669).copy(alpha = 0.14f),
                    title = "অফলাইন ডাউনলোড",
                    onClick = onNavigateToDownloads
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.Assessment,
                    iconTint = Color(0xFF2563EB),
                    iconBg = Color(0xFF2563EB).copy(alpha = 0.14f),
                    title = "রিপোর্ট কার্ড ও লিডারবোর্ড",
                    badgeText = "নতুন",
                    badgeColor = Color(0xFF2563EB),
                    onClick = onNavigateToReportCard
                )
            }

            // 4. App Preferences & Notification
            SettingsGroupCard(title = "অ্যাপ প্রেফারেন্স") {
                CleanSettingsRow(
                    icon = Icons.Default.Palette,
                    iconTint = Color(0xFFEC4899),
                    iconBg = Color(0xFFEC4899).copy(alpha = 0.14f),
                    title = "হেডার ও লাইভ ওয়ালপেপার",
                    badgeText = "কাস্টমাইজ",
                    onClick = onNavigateToHeaderWallpaper
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = Color(0xFFF97316),
                    iconBg = Color(0xFFF97316).copy(alpha = 0.14f),
                    title = "ক্লাস অ্যালার্ম ও নোটিফিকেশন",
                    badgeText = "${currentLeadTime} মি. আগে",
                    onClick = onNavigateToNotification
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.DarkMode,
                    iconTint = Color(0xFF6366F1),
                    iconBg = Color(0xFF6366F1).copy(alpha = 0.14f),
                    title = "অ্যাপ থিম",
                    badgeText = themeLabel,
                    badgeColor = Color(0xFF6366F1),
                    onClick = { showThemeDialog = true }
                )
            }

            // 5. Storage & Maintenance Section
            SettingsGroupCard(title = "স্টোরেজ ও রক্ষণাবেক্ষণ") {
                CleanSettingsRow(
                    icon = Icons.Default.DeleteSweep,
                    iconTint = Color(0xFF0284C7),
                    iconBg = Color(0xFF0284C7).copy(alpha = 0.14f),
                    title = "ক্যাশ মেমোরি খালি করুন",
                    badgeText = formattedCacheSize,
                    badgeColor = if (formattedCacheSize != "0.0 MB") Color(0xFF0284C7) else Color(0xFF10B981),
                    onClick = { showClearCacheDialog = true }
                )

                CleanDivider()

                CleanSettingsRow(
                    icon = Icons.Default.SystemUpdate,
                    iconTint = Color(0xFF10B981),
                    iconBg = Color(0xFF10B981).copy(alpha = 0.14f),
                    title = "অ্যাপ আপডেট যাচাই করুন",
                    badgeText = if (isCheckingUpdate) "যাচাই হচ্ছে..." else "v6.0.5",
                    onClick = {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            scope.launch(Dispatchers.IO) {
                                val info = AppUpdateRepository.fetchLatestUpdate()
                                withContext(Dispatchers.Main) {
                                    isCheckingUpdate = false
                                    if (info != null && info.versionCode > BuildConfig.VERSION_CODE) {
                                        manualUpdateInfo = info
                                    } else {
                                        Toast.makeText(context, "আপনার অ্যাপটি ইতিমধ্যে সর্বশেষ সংস্করণে আছে!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // 6. Account & Security Section
            SettingsGroupCard(title = "অ্যাকাউন্ট") {
                CleanSettingsRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconBg = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    title = "লগআউট",
                    isDestructive = true,
                    onClick = { showLogoutDialog = true }
                )
            }

            // Minimalist Brand Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MaxBird Digital Academy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version 6.0.5 • Build ${BuildConfig.VERSION_CODE}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Modal: Visual Theme Selector
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "অ্যাপ থিম নির্বাচন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        Triple("system", "সিস্টেম ডিফল্ট", Icons.Default.BrightnessAuto),
                        Triple("light", "লাইট মোড", Icons.Default.LightMode),
                        Triple("dark", "ডার্ক মোড", Icons.Default.DarkMode)
                    ).forEach { (modeKey, modeTitle, icon) ->
                        val isSelected = currentThemeMode == modeKey
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    sessionManager.setThemeMode(modeKey)
                                    showThemeDialog = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = modeTitle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        sessionManager.setThemeMode(modeKey)
                                        showThemeDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("ঠিক আছে", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Modal: Clear Cache Confirmation
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "ক্যাশ মেমোরি খালি করবেন?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.5.sp
                )
            },
            text = {
                Text(
                    text = "বর্তমানে $formattedCacheSize অস্থায়ী ক্যাশ মেমোরি জমা রয়েছে। এটি পরিষ্কার করলে আপনার কোনো মূল ডাটা বা ডাউনলোড মুছবে না।",
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isClearingCache = true
                        AppCacheManager.clearAllAppCache(context)
                        formattedCacheSize = "0.0 MB"
                        isClearingCache = false
                        showClearCacheDialog = false
                        Toast.makeText(context, "ক্যাশ মেমোরি সম্পূর্ণ পরিষ্কার করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("খালি করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Modal: Logout Confirmation
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "লগআউট করতে চান?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে আপনার MaxBird অ্যাকাউন্ট থেকে লগআউট করতে চান?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("লগআউট", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Modal: FCM Terminal
    if (showTerminalDialog) {
        FcmLiveTerminalDialog(
            sessionManager = sessionManager,
            onDismissRequest = { showTerminalDialog = false }
        )
    }

    // Modal: App Update
    manualUpdateInfo?.let { updateInfo ->
        UpdateDialog(
            updateInfo = updateInfo,
            onDismiss = { manualUpdateInfo = null }
        )
    }
}

/**
 * Grouped Settings Surface Container
 */
@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Ultra-Clean, Non-Cluttered Settings Row
 */
@Composable
private fun CleanSettingsRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = iconBg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.7.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun CleanDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
