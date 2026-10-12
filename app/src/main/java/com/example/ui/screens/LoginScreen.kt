package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.AuthState
import com.example.auth.AuthViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    authState: AuthState,
    onNavigateToPin: (String) -> Unit,
    onNavigateToOtp: (String, String) -> Unit,
    sessionManager: com.example.auth.SessionManager? = null,
    onLoginSuccess: (() -> Unit)? = null,
    onNavigateToActivation: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    var showQrLoginSheet by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        visible = true
        // Zero-Trust Check: verify device activation even if someone navigated directly to Login
        try {
            if (!com.example.security.DeviceActivationRepository.isDeviceActivated(context)) {
                onNavigateToActivation?.invoke()
            }
        } catch (_: Throwable) {}
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.NavigateToPin -> {
                onNavigateToPin(authState.phone)
                viewModel.resetState()
            }
            is AuthState.NavigateToOtp -> {
                onNavigateToOtp(authState.phone, authState.authType)
                viewModel.resetState()
            }
            is AuthState.Error -> errorMessage = authState.message
            else -> {}
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val successColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    // ---------- Smart Phone Input Sanitizer & Formatter ----------
    fun sanitizeAndSetPhone(raw: String) {
        var cleaned = raw.filter { it.isDigit() }
        // If pasted with country code 8801..., strip the leading 88
        if (cleaned.startsWith("8801") && cleaned.length > 11) {
            cleaned = cleaned.removePrefix("88")
        }
        if (cleaned.length <= 11) {
            phoneInput = cleaned
            errorMessage = null
        }
    }

    fun submitLogin() {
        keyboardController?.hide()
        if (com.example.security.DeviceActivationRepository.isConfigured() &&
            !com.example.security.DeviceActivationRepository.isDeviceActivated(context)
        ) {
            errorMessage = "অননুমোদিত ডিভাইস! আগে ডিভাইস এক্সেস সক্রিয় করতে হবে।"
            onNavigateToActivation?.invoke()
            return
        }

        if (phoneInput.length < 11) {
            errorMessage = "দয়া করে সম্পূর্ণ ১১ ডিজিটের মোবাইল নম্বর দিন।"
            return
        }

        if (!phoneInput.startsWith("01")) {
            errorMessage = "সঠিক বাংলাদেশী নম্বর লিখুন (০১ দিয়ে শুরু)।"
            return
        }

        viewModel.checkUser(phoneInput)
    }

    // ---------- Ambient Animations ----------
    val ambient = rememberInfiniteTransition(label = "ambient")
    val blob1X by ambient.animateFloat(-35f, 45f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "b1x")
    val blob1Y by ambient.animateFloat(-25f, 35f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "b1y")
    val blob2X by ambient.animateFloat(30f, -40f, infiniteRepeatable(tween(8500, easing = LinearEasing), RepeatMode.Reverse), label = "b2x")
    val blob2Y by ambient.animateFloat(25f, -30f, infiniteRepeatable(tween(10500, easing = LinearEasing), RepeatMode.Reverse), label = "b2y")
    val glowAlpha by ambient.animateFloat(0.5f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val floatY by ambient.animateFloat(-6f, 6f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "floatY")
    val ringAngle by ambient.animateFloat(0f, 360f, infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "ring")

    // Animated rotating border for brand logo
    val density = LocalDensity.current
    val logoBoxPx = with(density) { 78.dp.toPx() }
    val center = logoBoxPx / 2f
    val rad = Math.toRadians(ringAngle.toDouble())
    val dx = (cos(rad) * logoBoxPx).toFloat()
    val dy = (sin(rad) * logoBoxPx).toFloat()
    val ringBrush = Brush.linearGradient(
        colors = listOf(primary, secondary, primary.copy(alpha = 0.35f), secondary.copy(alpha = 0.6f), primary),
        start = Offset(center - dx, center - dy),
        end = Offset(center + dx, center + dy)
    )

    val backgroundGradient = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF070D19), Color(0xFF030712), Color(0xFF0B1324)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEEF2FF), Color(0xFFF1F5F9)))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // Moving aurora blobs
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopStart)
                .graphicsLayer { translationX = blob1X; translationY = blob1Y }
                .offset(x = (-100).dp, y = (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary.copy(alpha = if (isDark) 0.24f else 0.16f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer { translationX = blob2X; translationY = blob2Y }
                .offset(x = 100.dp, y = 100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(secondary.copy(alpha = if (isDark) 0.22f else 0.14f), Color.Transparent)
                    )
                )
        )
        // Soft center glow behind card
        Box(
            modifier = Modifier
                .size(440.dp)
                .align(Alignment.Center)
                .graphicsLayer { alpha = glowAlpha }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primary.copy(alpha = if (isDark) 0.10f else 0.06f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // ---------- Logo (floating + rotating gradient ring) ----------
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700)) + slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .graphicsLayer { translationY = floatY }
                            .shadow(
                                elevation = if (isDark) 18.dp else 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = primary,
                                ambientColor = secondary
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isDark) Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                                else Brush.linearGradient(listOf(Color.White, Color(0xFFE2E8F0)))
                            )
                            .border(2.dp, ringBrush, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
                            contentDescription = "MaxBird Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gradient brand name
                    Text(
                        text = "MaxBird",
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(primary, secondary)),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "সহজ ও আনন্দদায়ক শেখার ডিজিটাল একাডেমি",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Verified Platform Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = primary.copy(alpha = if (isDark) 0.15f else 0.10f),
                        border = BorderStroke(1.dp, primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "অনলাইন কোর্স ও প্র্যাকটিস লার্নিং",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ---------- Phone Input Card ----------
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 150)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(800, delayMillis = 150, easing = FastOutSlowInEasing)
                )
            ) {
                val isComplete = phoneInput.length == 11

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isComplete -> successColor
                        phoneInput.isNotEmpty() -> primary
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    },
                    animationSpec = tween(300),
                    label = "borderColor"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 18.dp else 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = primary.copy(alpha = 0.35f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.2.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.75f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "মোবাইল নম্বর লিখুন",
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isComplete) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = successColor.copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, successColor.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = successColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "সম্পূর্ণ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = successColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "লগইন করতে বা নতুন অ্যাকাউন্ট তৈরি করতে আপনার ১১ ডিজিটের নম্বর দিন",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Input container with animated border
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                        .copy(alpha = if (isDark) 0.55f else 0.75f)
                                )
                                .border(
                                    width = if (phoneInput.isNotEmpty()) 1.8.dp else 1.2.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country code badge
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "+880",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )

                            TextField(
                                value = phoneInput,
                                onValueChange = { sanitizeAndSetPhone(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    errorContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = primary
                                ),
                                placeholder = {
                                    Text(
                                        "০১XXXXXXXXX",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 15.sp
                                    )
                                },
                                trailingIcon = {
                                    when {
                                        isComplete -> Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Complete",
                                            tint = successColor,
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .size(22.dp)
                                        )
                                        phoneInput.isNotEmpty() -> IconButton(onClick = { phoneInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submitLogin() }
                                ),
                                singleLine = true
                            )
                        }

                        // Progress bar + Bengali digit counter
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { phoneInput.length / 11f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = borderColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (phoneInput.isNotEmpty() && !phoneInput.startsWith("01")) "⚠️ ০১ দিয়ে শুরু হতে হবে" else "বাংলাদেশি মোবাইল নম্বর",
                                fontSize = 11.sp,
                                color = if (phoneInput.isNotEmpty() && !phoneInput.startsWith("01")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${phoneInput.length.toBengaliDigits()}/১১",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = borderColor
                            )
                        }

                        // Error message
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { -10 }),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = errorMessage ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.5.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Helper banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.35f else 0.45f),
                            border = BorderStroke(1.dp, primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "নতুন অ্যাকাউন্ট হলে ওটিপি ভেরিফিকেশনের পর আপনার নাম ও শিক্ষা প্রতিষ্ঠানের তথ্য দিয়ে অ্যাকাউন্ট সম্পূর্ণ করার সুযোগ থাকবে।",
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Gradient CTA button with glow + press animation
                        val isEnabled = authState != AuthState.Loading && phoneInput.length >= 10
                        val interaction = remember { MutableInteractionSource() }
                        val isPressed by interaction.collectIsPressedAsState()
                        val pressScale by animateFloatAsState(
                            targetValue = when {
                                isPressed -> 0.96f
                                isEnabled -> 1f
                                else -> 0.98f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "pressScale"
                        )

                        val btnBrush = if (isEnabled) {
                            Brush.linearGradient(listOf(primary, secondary))
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                                .shadow(
                                    elevation = if (isEnabled) (8.dp + 6.dp * glowAlpha) else 0.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    spotColor = primary,
                                    ambientColor = secondary
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .background(btnBrush)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    enabled = isEnabled
                                ) {
                                    submitLogin()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (authState == AuthState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "এগিয়ে যান",
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // QR / Session Code Login Button
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { showQrLoginSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "কিউআর / সেশন কোড দিয়ে লগইন",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
                    }
                }
            }

            if (showQrLoginSheet && sessionManager != null) {
                com.example.ui.dialogs.QrSessionLoginSheet(
                    sessionManager = sessionManager,
                    onLoginSuccess = {
                        showQrLoginSheet = false
                        onLoginSuccess?.invoke()
                    },
                    onDismiss = { showQrLoginSheet = false }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---------- Trust & Security Badges ----------
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 250))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrustFeatureItem(
                        icon = Icons.Default.Lock,
                        label = "সুরক্ষিত সেশন"
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    TrustFeatureItem(
                        icon = Icons.Default.CheckCircle,
                        label = "তাৎক্ষণিক ওটিপি"
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    TrustFeatureItem(
                        icon = Icons.Default.Security,
                        label = "এনক্রিপ্টেড ডাটা"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------- Terms footer ----------
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 300))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "এগিয়ে যাওয়ার মাধ্যমে আপনি MaxBird এর শর্তাবলী ও গোপনীয়তা নীতি মেনে নিচ্ছেন।",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TrustFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- Helper: English → Bengali digits ----------
private val bnDigits = mapOf(
    '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
    '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
)

private fun Int.toBengaliDigits(): String =
    toString().map { bnDigits[it] ?: it }.joinToString("")
