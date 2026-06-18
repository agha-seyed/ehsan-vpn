package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VpnProfile
import com.example.ui.theme.*


import com.example.MainActivity
import com.example.VpnViewModel
import com.example.data.*
import com.example.HorizonVpnService
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnDashboard(
    viewModel: VpnViewModel,
    onPrepareVpn: (Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Core state observations
    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val connectedServer by viewModel.connectedServer.collectAsStateWithLifecycle()
    val downloadSpeed by viewModel.downloadSpeed.collectAsStateWithLifecycle()
    val uploadSpeed by viewModel.uploadSpeed.collectAsStateWithLifecycle()
    val totalBytesDown by viewModel.totalBytesDown.collectAsStateWithLifecycle()
    val totalBytesUp by viewModel.totalBytesUp.collectAsStateWithLifecycle()
    
    val allServers by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeServer by viewModel.activeProfile.collectAsStateWithLifecycle()

    val isPingingAll by viewModel.isPingingAll.collectAsStateWithLifecycle()
    val isTestingSpeed by viewModel.isTestingSpeed.collectAsStateWithLifecycle()
    val speedProgress by viewModel.speedProgress.collectAsStateWithLifecycle()
    val testedDownloadMbps by viewModel.testedDownloadMbps.collectAsStateWithLifecycle()
    val testedUploadMbps by viewModel.testedUploadMbps.collectAsStateWithLifecycle()
    val testedPingMs by viewModel.testedPingMs.collectAsStateWithLifecycle()

    val isDiagnosing by viewModel.isDiagnosing.collectAsStateWithLifecycle()
    val dnsStatus by viewModel.dnsStatus.collectAsStateWithLifecycle()
    val gatewayStatus by viewModel.gatewayStatus.collectAsStateWithLifecycle()
    val sniStatus by viewModel.sniStatus.collectAsStateWithLifecycle()
    val diagnosticsAdvice by viewModel.diagnosticsAdvice.collectAsStateWithLifecycle()

    val isTestingLivePing by viewModel.isTestingLivePing.collectAsStateWithLifecycle()
    val livePingMs by viewModel.livePingMs.collectAsStateWithLifecycle()
    val liveJitterMs by viewModel.liveJitterMs.collectAsStateWithLifecycle()
    val livePacketLoss by viewModel.livePacketLoss.collectAsStateWithLifecycle()

    var connectionDurationSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(vpnState) {
        if (vpnState == "CONNECTED") {
            connectionDurationSeconds = 0L
            while (true) {
                delay(1000)
                connectionDurationSeconds++
            }
        } else {
            connectionDurationSeconds = 0L
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isFa = (appLanguage == "fa")

    var showSplash by remember { mutableStateOf(true) }

    var showServerSheet by remember { mutableStateOf(false) }
    var showVpsAssistant by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var showAdvancedSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    val isAmoledMode by viewModel.isAmoledMode.collectAsStateWithLifecycle()
    val isSplitTunnelingEnabled by viewModel.isSplitTunnelingEnabled.collectAsStateWithLifecycle()
    val splitApps by viewModel.splitApps.collectAsStateWithLifecycle()
    
    var qrProfileToShow by remember { mutableStateOf<VpnProfile?>(null) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            if (targetState) {
                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
            } else {
                (fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing)) + 
                 scaleIn(initialScale = 0.95f, animationSpec = tween(800, easing = FastOutSlowInEasing)))
                .togetherWith(
                    fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing)) + 
                    scaleOut(targetScale = 1.05f, animationSpec = tween(600, easing = FastOutSlowInEasing))
                )
            }
        },
        label = "splash_navigation"
    ) { splashActive ->
        if (splashActive) {
            SplashIntroScreen(
                isFa = isFa,
                onDismiss = { showSplash = false }
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(DarkBg)
            ) {
        // Abstract Mesh Gradient Background for Connection Glow
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-50).dp)
                .size(280.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (vpnState == "CONNECTED") CyberGreen.copy(alpha = 0.15f)
                            else if (vpnState == "CONNECTING") CyanGlow.copy(alpha = 0.12f)
                            else CobaltBlue.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            // Elegant Frosted Glass Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFa) "احسان وی‌پی‌ان" else "EHSAN VPN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFa) "تونل هیبرید طلایی کله‌غازی" else "GOLDEN HYBRID TUNNEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language switcher
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("language_toggle_button")
                        ) {
                            Text(
                                text = if (isFa) "EN" else "FA",
                                color = CobaltBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { showVpsAssistant = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("vps_assistant_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = if (isFa) "دستیار سرور" else "Server Assistant",
                                tint = CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showServerSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("server_list_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = if (isFa) "لیست سرورها" else "Server List",
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showAdvancedSettingsDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("advanced_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = if (isFa) "امکانات پیشرفته" else "Advanced Settings",
                                tint = CyberGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("about_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = if (isFa) "درباره ما" else "About Us",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Connection state message
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val statusText = when (vpnState) {
                        "CONNECTED" -> if (isFa) "اتصال طلایی برقرار است" else "Golden Tunnel Active"
                        "CONNECTING" -> if (isFa) "در حال برقراری اتصال امن..." else "Securing Connection..."
                        "ERROR" -> if (isFa) "خطا در برقراری ارتباط!" else "Handshake Failed!"
                        else -> if (isFa) "آماده برای اتصال شیلد" else "Ready to Connect"
                    }
                    val statusSubtext = when (vpnState) {
                        "CONNECTED" -> if (isFa) "ترافیک شما کاملا رمزنگاری طلایی شد" else "Your internet proxy traffic is securely shielded"
                        "CONNECTING" -> if (isFa) "در حال احراز هویت با پروتکل VLESS" else "Authenticating credentials under stealth SNI"
                        "ERROR" -> if (isFa) "تنظیمات آدرس یا کلید سرور را بررسی کنید" else "Please verify server IP configuration parameters"
                        else -> if (isFa) "جهت فعالسازی روی دکمه طلایی کلیک کنید" else "Tap golden powerswitch below to bridge VPN"
                    }
                    val statusColor = when (vpnState) {
                        "CONNECTED" -> CyberGreen
                        "CONNECTING" -> CyanGlow
                        "ERROR" -> AlertRed
                        else -> Color.White
                    }

                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Light,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = statusSubtext,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Central Frosted Glass Connection Power Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer slow breathing glow ring
                    val infiniteTransition = rememberInfiniteTransition(label = "halo")
                    val outerScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2400, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "outer_scale"
                    )
                    val outerAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.03f,
                        targetValue = 0.12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2400, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "outer_alpha"
                    )

                    // Outer faint border ring
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .background(
                                color = if (vpnState == "CONNECTED") CyberGreen.copy(alpha = outerAlpha)
                                        else if (vpnState == "CONNECTING") CyanGlow.copy(alpha = outerAlpha)
                                        else CobaltBlue.copy(alpha = outerAlpha),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (vpnState == "CONNECTED") CyberGreen.copy(alpha = 0.3f)
                                        else if (vpnState == "CONNECTING") CyanGlow.copy(alpha = 0.3f)
                                        else CobaltBlue.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )

                    // Inner Glassmorphic Button Container
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .shadow(
                                elevation = if (vpnState == "CONNECTED") 16.dp else 4.dp,
                                shape = CircleShape,
                                ambientColor = if (vpnState == "CONNECTED") CyberGreen else CobaltBlue,
                                spotColor = if (vpnState == "CONNECTED") CyberGreen else CobaltBlue
                            )
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = if (vpnState == "CONNECTED") CyberGreen.copy(alpha = 0.5f)
                                        else if (vpnState == "CONNECTING") CyanGlow.copy(alpha = 0.5f)
                                        else GlassBorderStrong,
                                shape = CircleShape
                            )
                            .clickable {
                                val intent = VpnService.prepare(context)
                                if (intent != null) {
                                    onPrepareVpn(intent)
                                } else {
                                    viewModel.toggleVpnConnection(context, null)
                                }
                            }
                            .testTag("vpn_toggle_orb"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = if (isFa) "امنیت اتصال" else "Shield Connection Indicator",
                                tint = if (vpnState == "CONNECTED") CyberGreen
                                       else if (vpnState == "CONNECTING") CyanGlow
                                       else CobaltBlue,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            // Real-time Bandwidth Monitoring Cards (Styled in Glass)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SpeedMetricCard(
                        title = if (isFa) "دانلود / Down" else "Download Speed",
                        speed = downloadSpeed,
                        total = totalBytesDown,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = CyanGlow,
                        isFa = isFa,
                        modifier = Modifier.weight(1f)
                    )
                    SpeedMetricCard(
                        title = if (isFa) "آپلود / Up" else "Upload Speed",
                        speed = uploadSpeed,
                        total = totalBytesUp,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = CyberGreen,
                        isFa = isFa,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Real-time Active Pinpoint Ping Diagnostic Widget
            item {
                LivePingDiagnosticCard(
                    isFa = isFa,
                    isTesting = isTestingLivePing,
                    pingMs = livePingMs,
                    jitterMs = liveJitterMs,
                    packetLoss = livePacketLoss,
                    onTestPing = { viewModel.testLivePingOfActive() }
                )
            }

            // Real-time Waving Traffic Graph (Teal-Gold Theme)
            item {
                LiveTrafficGraphCard(
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    isFa = isFa
                )
            }

            // Immersive Detailed Session Technical Blueprint & Connection Info
            item {
                ActiveConnectionBlueprintCard(
                    activeServer = activeServer,
                    durationSeconds = connectionDurationSeconds,
                    vpnState = vpnState,
                    isFa = isFa
                )
            }

            // Current Server Widget
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showServerSheet = true },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, GlassBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    tint = CyanGlow,
                                    contentDescription = if (isFa) "سرور" else "Server Gateway"
                                )
                            }
                            Column {
                                Text(
                                    text = if (isFa) "درگاه اتصال انتخابی" else "Selected Server Gateway",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = activeServer?.name ?: (if (isFa) "هیچ سروری انتخاب نشده" else "No active node chosen"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = activeServer?.let { "${it.protocol} • IP: ${it.serverIp}" } ?: (if (isFa) "برای اتصال کدهای VLESS یا Shadowsocks را استفاده کنید" else "Pasted subscription keys will configure tunneling"),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                               text = activeServer?.let { if (it.latencyMs > 0) "${it.latencyMs} ms" else (if (isFa) "جدید" else "NEW") } ?: "",
                                color = CyberGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                tint = TextMuted,
                                contentDescription = if (isFa) "انتخاب سرور" else "Select node dropdown"
                            )
                        }
                    }
                }
            }

            // Centered Smart Sharing & QR Scanner Hub
            item {
                SmartQrSharingHub(
                    activeServer = activeServer,
                    isFa = isFa,
                    onScanQr = { showQrScannerDialog = true },
                    onShareCurrent = {
                        activeServer?.let { qrProfileToShow = it }
                    },
                    onPasteConfig = {
                        showQrScannerDialog = true
                    }
                )
            }

            // Dynamic Interactive Speedometer Card
            item {
                SpeedTestCardComponent(
                    isFa = isFa,
                    isTesting = isTestingSpeed,
                    progress = speedProgress,
                    downloadMbps = testedDownloadMbps,
                    uploadMbps = testedUploadMbps,
                    pingMs = testedPingMs,
                    onStartTest = { viewModel.startSpeedTest() }
                )
            }

            // Stateful Auto-Diagnostics Card
            item {
                DiagnosisCardComponent(
                    isFa = isFa,
                    isDiagnosing = isDiagnosing,
                    dnsStatus = dnsStatus,
                    gatewayStatus = gatewayStatus,
                    sniStatus = sniStatus,
                    advice = diagnosticsAdvice,
                    onRunDiagnosis = { viewModel.runSelfDiagnosis() }
                )
            }

            // Helpful VPS Banner Reminder for Server activation
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                tint = CyanGlow,
                                contentDescription = "آموزش"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "راهنمای لینوکس صفر تا صد سرورها" else "Zero-to-One Linux Server Setup Help",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "تبدیل سرور مجازی خام (Ubuntu) به فیلترشکن شخصی با پنل‌های مرزبان یا سهوی" else "Redeem VLESS subscription tokens using standard VPS Docker installers",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            contentSpacer(height = 8.dp)
                            Button(
                                onClick = { showVpsAssistant = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isFa) "ایجاد اسکریپت با کلیک" else "Create Installer Script",
                                    color = DarkBg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SHEET 1: Server Profile Manager ---
        if (showServerSheet) {
            ServerProfilesSheet(
                profiles = allServers,
                activeProfile = activeServer,
                isPingingAll = isPingingAll,
                onPingAll = { viewModel.pingAllServers() },
                onImportProfile = { viewModel.insertProfile(it) },
                onAutoConnectBest = {
                    viewModel.selectBestServerAndConnect(context)
                    showServerSheet = false
                    Toast.makeText(context, if (isFa) "پینگ همگانی کامل شد و سریع‌ترین سرور متصل گردید!" else "Bypassed via fastest gateway successfully!", Toast.LENGTH_SHORT).show()
                },
                onScanQrClick = {
                    showQrScannerDialog = true
                },
                onSelectProfile = { profile ->
                    viewModel.selectActiveProfile(profile.id)
                    showServerSheet = false
                    Toast.makeText(context, if (isFa) "سرور با موفقیت فراخوانی شد" else "Access node successfully bridged", Toast.LENGTH_SHORT).show()
                },
                onDeleteProfile = { profile ->
                    viewModel.deleteProfile(profile)
                    Toast.makeText(context, if (isFa) "پروفایل حذف شد" else "Server config erased", Toast.LENGTH_SHORT).show()
                },
                onAddNewProfileClick = {
                    showAddServerDialog = true
                },
                onShareQrClick = { profile ->
                    qrProfileToShow = profile
                },
                onDismiss = { showServerSheet = false },
                isFa = isFa
            )
        }

        // --- DIALOG: QR Code Sharing Portal ---
        qrProfileToShow?.let { profile ->
            ServerQrDialog(
                profile = profile,
                isFa = isFa,
                onDismiss = { qrProfileToShow = null }
            )
        }

        // --- DIALOG: Camera/Gallery QR Scanner Simulator ---
        if (showQrScannerDialog) {
            QrScannerDialog(
                isFa = isFa,
                onScanResult = { scannedValue ->
                    if (viewModel.parseAndInsertProfile(scannedValue)) {
                        Toast.makeText(context, if (isFa) "پروفایل با موفقیت اضافه شد" else "Profile added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, if (isFa) "خطا در تحلیل لینک کانفیگ" else "Failed to parse config link", Toast.LENGTH_SHORT).show()
                    }
                    showQrScannerDialog = false
                },
                onDismiss = { showQrScannerDialog = false }
            )
        }

        // --- DIALOG: Advanced Settings Hub ---
        if (showAdvancedSettingsDialog) {
            AdvancedSettingsDialog(
                isFa = isFa,
                isAmoledMode = isAmoledMode,
                isSplitEnabled = isSplitTunnelingEnabled,
                splitApps = splitApps,
                show = showAdvancedSettingsDialog,
                onDismiss = { showAdvancedSettingsDialog = false },
                onToggleAmoled = { viewModel.toggleAmoledMode() },
                onToggleSplit = { viewModel.setSplitTunnelingEnabled(it) },
                onToggleApp = { viewModel.toggleAppInSplitTunnel(it) }
            )
        }

        // --- SHEET 2: VPS SSH Automated Setup Assistant ---
        if (showVpsAssistant) {
            VpsSetupAssistantSheet(
                viewModel = viewModel,
                onDismiss = { showVpsAssistant = false },
                isFa = isFa
            )
        }

        // --- DIALOG: Custom Server Add Form ---
        if (showAddServerDialog) {
            AddServerFormDialog(
                isFa = isFa,
                onDismiss = { showAddServerDialog = false },
                onSave = { name, ip, port, protocol, secret, sni ->
                    val newProfile = VpnProfile(
                        name = name,
                        serverIp = ip,
                        port = port,
                        secretKey = secret,
                        protocol = protocol,
                        sni = sni
                    )
                    viewModel.insertProfile(newProfile)
                    showAddServerDialog = false
                    Toast.makeText(context, if (isFa) "سرور جدید با موفقیت اضافه شد" else "Ehsan Node Successfully deployed", Toast.LENGTH_LONG).show()
                }
            )
        }

        // --- DIALOG: About Us ---
        if (showAboutDialog) {
            AboutDialog(
                isFa = isFa,
                onDismiss = { showAboutDialog = false }
            )
        }
            }
        }
    }
}

@Composable
fun AboutDialog(
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(CyberGreen.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, CyberGreen.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Logo",
                        tint = CyberGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isFa) "احسان وی‌پی‌ان" else "Ehsan VPN",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Version 0.1.0 (Wirangaran Build)",
                    color = CyanGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Text(
                    text = if (isFa) 
                        "این اپلیکیشن توسط «احسان نبوی» و گروه نرم‌افزاری ویرانگران (Wirangaran) توسعه یافته است. یک تونل امنیتی قدرتمند و فوق‌سریع با معماری هیبریدی برای رمزنگاری ۱۰۰٪ ترافیک شما."
                    else 
                        "This application is proudly developed by Ehsan Nabavi and the Wirangaran group. A powerful, ultra-fast security tunnel using a hybrid architecture to encrypt 100% of your traffic.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Support Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:agha.seyed.ehsan@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Ehsan VPN Support")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Send Email"))
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Support",
                        tint = CyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isFa) "پشتیبانی و تماس با ما" else "Support & Contact",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "support@wirangaran.com",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isFa) "متوجه شدم" else "Got It",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedMetricCard(
    title: String,
    speed: Float, // in KB/s
    total: Long, // in bytes
    icon: ImageVector,
    iconColor: Color,
    isFa: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            // Format Speed format
            val speedDisplay = if (speed > 1024) {
                String.format("%.2f MB/s", speed / 1024f)
            } else {
                String.format("%.1f KB/s", speed)
            }
            Text(
                text = speedDisplay,
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            // Format Volume format
            val megaBytes = total / (1024f * 1024f)
            val totalDisplay = if (megaBytes > 1024) {
                String.format("%.2f GB", megaBytes / 1024f)
            } else {
                String.format("%.1f MB", megaBytes)
            }
            Text(
                text = if (isFa) "ترافیک کل: $totalDisplay" else "Total data: $totalDisplay",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerProfilesSheet(
    profiles: List<VpnProfile>,
    activeProfile: VpnProfile?,
    isPingingAll: Boolean,
    onPingAll: () -> Unit,
    onImportProfile: (VpnProfile) -> Unit,
    onAutoConnectBest: () -> Unit,
    onScanQrClick: () -> Unit,
    onSelectProfile: (VpnProfile) -> Unit,
    onDeleteProfile: (VpnProfile) -> Unit,
    onAddNewProfileClick: () -> Unit,
    onShareQrClick: (VpnProfile) -> Unit,
    onDismiss: () -> Unit,
    isFa: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProtocolFilter by remember { mutableStateOf("All") } // "All", "VLESS", "Trojan", "ShadowSocks"
    var sortByLowestPing by remember { mutableStateOf(false) }

    val filteredProfiles = remember(profiles, searchQuery, selectedProtocolFilter, sortByLowestPing) {
        var result = profiles.filter { profile ->
            val matchesSearch = profile.name.contains(searchQuery, ignoreCase = true) || 
                                profile.serverIp.contains(searchQuery, ignoreCase = true)
            val matchesProtocol = if (selectedProtocolFilter == "All") {
                true
            } else if (selectedProtocolFilter == "ShadowSocks") {
                profile.protocol.contains("ss", ignoreCase = true) || profile.protocol.contains("shadowsocks", ignoreCase = true)
            } else {
                profile.protocol.contains(selectedProtocolFilter, ignoreCase = true)
            }
            matchesSearch && matchesProtocol
        }
        if (sortByLowestPing) {
            result = result.sortedWith { a, b ->
                val ap = if (a.latencyMs <= 0) 999999 else a.latencyMs
                val bp = if (b.latencyMs <= 0) 999999 else b.latencyMs
                ap.compareTo(bp)
            }
        }
        result
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(SurfaceDark)
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "مدیریت پروفایل‌های سرور" else "Server Gateway Profiles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Ping All Button
                        IconButton(
                            onClick = onPingAll,
                            enabled = !isPingingAll,
                            modifier = Modifier.background(
                                if (isPingingAll) YellowGlow.copy(alpha = 0.05f) else CyberGreen.copy(alpha = 0.12f),
                                CircleShape
                            )
                        ) {
                            if (isPingingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = YellowGlow,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = if (isFa) "پینگ همه سرورها" else "Ping all configurations",
                                    tint = CyberGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onScanQrClick,
                            modifier = Modifier.background(CobaltBlue.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = if (isFa) "اسکن کد QR" else "Scan QR Code",
                                tint = CobaltBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onAddNewProfileClick,
                            modifier = Modifier.background(CyanGlow.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isFa) "افزودن سرور" else "Add Server Profile",
                                tint = CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SEARCH BAR & SORT TOGGLE ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .testTag("server_search_input"),
                        placeholder = {
                            Text(
                                text = if (isFa) "جستجوی سرور..." else "Search server IP/name...",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )

                    // Sort Toggle button
                    IconButton(
                        onClick = { sortByLowestPing = !sortByLowestPing },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (sortByLowestPing) CobaltBlue.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (sortByLowestPing) CobaltBlue.copy(alpha = 0.4f) else GlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SortByAlpha,
                            contentDescription = "مرتب‌سازی پینگ",
                            tint = if (sortByLowestPing) CobaltBlue else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // PROTOCOL FILTER ROW (Pills)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filters = listOf("All", "VLESS", "Trojan", "ShadowSocks")
                    filters.forEach { filterName ->
                        val isFilterActive = selectedProtocolFilter == filterName
                        val label = when (filterName) {
                            "All" -> if (isFa) "همه" else "All"
                            "ShadowSocks" -> if (isFa) "شدو" else "SS"
                            else -> filterName
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isFilterActive) CyanGlow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isFilterActive) CyanGlow.copy(alpha = 0.5f) else GlassBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedProtocolFilter = filterName }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isFilterActive) CyanGlow else TextSecondary,
                                fontWeight = if (isFilterActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(14.dp))

                if (filteredProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (isFa) "جستجو بی‌نتیجه بود سروری یافت نشد" else "No matching server profiles",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Smart Auto-routing and fast connection widget (Green Neon Style)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAutoConnectBest() },
                                colors = CardDefaults.cardColors(containerColor = CyberGreen.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.2.dp, CyberGreen.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(CyberGreen.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, CyberGreen.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            tint = CyberGreen,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isFa) "اتصال هوشمند به سریع‌ترین سرور" else "Smart Connect Lowest Ping Gateway",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberGreen
                                        )
                                        Text(
                                            text = if (isFa) "بررسی زمان تأخیر لایو و اتصال خودکار با بهترین کانال" else "Filters live latency and secures connection automatically",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        tint = CyberGreen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        items(filteredProfiles) { profile ->
                            val isSelected = activeProfile?.id == profile.id
                            ServerProfileItem(
                                profile = profile,
                                isSelected = isSelected,
                                onSelect = { onSelectProfile(profile) },
                                onDelete = { onDeleteProfile(profile) },
                                onShareQr = { onShareQrClick(profile) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // IMPORT/EXPORT CONFIGS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val context = LocalContext.current
                    var showImportBackupDialog by remember { mutableStateOf(false) }

                    // Export Backup Card-Button
                    Button(
                        onClick = {
                            if (profiles.isEmpty()) {
                                Toast.makeText(context, if (isFa) "هیچ پکیجی برای پشتیبان‌گیری وجود ندارد" else "No profiles to export", Toast.LENGTH_SHORT).show()
                            } else {
                                val jsonStr = profiles.joinToString(separator = "\n") { pr ->
                                    "${pr.name}|${pr.serverIp}|${pr.port}|${pr.protocol}|${pr.secretKey}|${pr.sni}"
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("EhsanVPNBackup", jsonStr)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, if (isFa) "کد پشتیبان با موفقیت در کلیپ‌بورد کپی شد" else "Configuration exported to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("export_backup_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(14.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = if (isFa) "پشتیبان‌گیری (Export)" else "Export configs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                    }

                    // Import Backup Card-Button
                    Button(
                        onClick = {
                            showImportBackupDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("import_backup_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(14.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = if (isFa) "بازیابی (Import)" else "Import configs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )
                    }

                    if (showImportBackupDialog) {
                        var importedText by remember { mutableStateOf("") }
                        Dialog(
                            onDismissRequest = { showImportBackupDialog = false }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = if (isFa) "وارد کردن کدهای پشتیبان" else "Restore Backup Configurations",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )

                                    TextField(
                                        value = importedText,
                                        onValueChange = { importedText = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp)),
                                        placeholder = {
                                            Text(
                                                text = if (isFa) {
                                                    "کد پشتیبان کپی شده را اینجا پیست کنید..."
                                                } else {
                                                    "Paste exported backups config here..."
                                                },
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { showImportBackupDialog = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (isFa) "لغو" else "Cancel",
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (importedText.trim().isNotEmpty()) {
                                                    var count = 0
                                                    importedText.trim().split("\n").forEach { line ->
                                                        val parts = line.split("|")
                                                        if (parts.size >= 6) {
                                                            val profile = VpnProfile(
                                                                name = parts[0],
                                                                serverIp = parts[1],
                                                                port = parts[2].toIntOrNull() ?: 443,
                                                                protocol = parts[3],
                                                                secretKey = parts[4],
                                                                sni = parts[5],
                                                                latencyMs = 0
                                                            )
                                                            onImportProfile(profile)
                                                            count++
                                                        }
                                                    }
                                                    if (count > 0) {
                                                        Toast.makeText(context, if (isFa) "$count سرور با موفقیت بازیابی شد" else "$count servers restored successfully", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, if (isFa) "فرمت دیتای وارد شده نامعتبر است" else "Format invalid", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                showImportBackupDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                            modifier = Modifier.weight(1.5f)
                                        ) {
                                            Text(
                                                text = if (isFa) "تایید و بازیابی" else "Confirm",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkBg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isFa) "بازگشت به برنامه / بستن" else "Close Gateway Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ServerProfileItem(
    profile: VpnProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onShareQr: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CyanGlow.copy(alpha = 0.12f) else SurfaceCard
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp, 
            if (isSelected) CyanGlow.copy(alpha = 0.5f) else GlassBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isSelected) CyanGlow.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), 
                            CircleShape
                        )
                        .border(1.dp, if (isSelected) CyanGlow.copy(alpha = 0.3f) else GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.Done else Icons.Default.Security,
                        tint = if (isSelected) CyanGlow else TextSecondary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "${profile.protocol} • ${profile.serverIp}:${profile.port}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.latencyMs > 0) {
                    Text(
                        text = "${profile.latencyMs} ms",
                        color = if (profile.latencyMs < 100) CyberGreen else YellowGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                IconButton(
                    onClick = onShareQr,
                    modifier = Modifier.size(28.dp).padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "اشتراک‌گذاری QR",
                        tint = CobaltBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف سرور",
                        tint = AlertRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VpsSetupAssistantSheet(
    viewModel: VpnViewModel,
    onDismiss: () -> Unit,
    isFa: Boolean = true
) {
    val context = LocalContext.current
    val selectedOS by viewModel.selectedOS.collectAsStateWithLifecycle()
    val selectedPanel by viewModel.selectedPanel.collectAsStateWithLifecycle()
    val scriptText = viewModel.generateVpsScripts()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(SurfaceDark)
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isFa) "🖥️ راهنمای صفر تا صد راه‌اندازی سرور مجازی" else "🖥️ Full Virtual Private Server (VPS) Guide",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isFa) "آموزش گام به گام تبدیل VPS خام شما به فیلترشکن اختصاصی ضد فیلتر" else "Step-by-step tutorial to convert a standard Linux instance into your bypass proxy node",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Step 1
                    item {
                        StepItem(
                            stepNumber = "۱",
                            title = if (isFa) "اتصال به سرور مجازی (SSH)" else "Establish SSH Handshake Connection",
                            detail = if (isFa) "با استفاده از نرم‌افزار Putty (در کامپیوتر) یا نرم‌افزارهای Termius / Termux (در موبایل)، با پورت پیشفرض 22 و یوزر root به آدرس IP سرور مجازی خود متصل شوید." 
                                     else "Connect to your server IP via SSH Port 22 using PuTTY (on Desktop) or Termius / Termux (on mobile devices/tablets) with 'root' as the administrative user."
                        )
                    }

                    // Step 2
                    item {
                        StepItem(
                            stepNumber = "۲",
                            title = if (isFa) "نظارت بر تنظیمات نصاب اتوماتیک" else "Configure Custom Installer parameters",
                            detail = if (isFa) "نسخه لینوکس سرور مجازی و یکی از پنل‌های مشهور ضد فیلتر را پایین‌تر انتخاب کنید تا دستور سفارشی ساخت سرور ضد فیلتر ساخته شود:"
                                     else "Choose your Linux OS flavour and custom panel proxy cores below to dynamically cook the optimal server deployment script:"
                        )
                    }

                    // Select Configuration dropdown simulation inside steps
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (isFa) "انتخاب پنل مدیریت سرور:" else "Select Admin Cockpit Dashboard Console:",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val panels = listOf("Marzban (Reality - Bypass)", "X-ui (Sanaei - Simple Panel)", "Sing-Box Core (Stealth Reality)")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    panels.forEach { panel ->
                                        val isCurrent = selectedPanel == panel
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) CyanGlow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                                .border(1.dp, if (isCurrent) CyanGlow else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { viewModel.setServerDeploymentConfig(selectedOS, panel) }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = panel.substringBefore(" "),
                                                color = if (isCurrent) CyanGlow else TextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Step 3 (The Code Box)
                    item {
                        Column {
                            StepItem(
                                stepNumber = "۳",
                                title = if (isFa) "کپی کردن و اجرای دستور نصب در هاست" else "Execute cooked deployment console scripts",
                                detail = if (isFa) "کد زیر را با استفاده از دکمه کپی کنید، وارد ترمینال سرور مجازی خود شده، دکمه راست کلیک (یا دکمه اینترترمینال) را زده تا کد پیست شود، سپس دکمه Enter را برای شروع نصب فشار دهید:"
                                         else "Tap copy button below, shift to your terminal shell screen, paste, and execute by pressing Enter. Docker will build the environment in less than a minute:"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Bash command wrapper
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("bash / console", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        IconButton(
                                            onClick = {
                                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("vps_script", scriptText)
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast.makeText(context, if (isFa) "دستور نصب با موفقیت کپی شد!" else "Command copied to system clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                tint = CyanGlow,
                                                contentDescription = "کپی کد",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = scriptText,
                                        color = CyberGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Step 4
                    item {
                        StepItem(
                            stepNumber = "۴",
                            title = if (isFa) "ثبت آدرس سرور در اپلیکیشن" else "Import generated subscriptions into Ehsan App",
                            detail = if (isFa) "پس از اتمام نصب، یک آدرس اتصال یا QR کد دریافت خواهید کرد. مشخصات سرور را در همین اپلیکیشن دکمه '+' را بزنید و ثبت کنید. حالا می‌توانید به راحتی به سرور شخصی خود بدون فیلتر متصل شوید!"
                                     else "At completion, the script outputs standard VLESS or VMess strings. Open Add Dialog under Server Profiles (+), paste or submit the connection token, and experience restriction-free golden speeds!"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isFa) "متوجه شدم - خروج از راهنما" else "Got it! Exit Setup Assistant",
                        color = DarkBg, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StepItem(
    stepNumber: String,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(CobaltBlue.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, CobaltBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = CobaltBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

sealed class LinkValidationState {
    object Empty : LinkValidationState()
    data class Valid(
        val name: String,
        val host: String,
        val port: Int,
        val protocol: String,
        val secret: String,
        val sni: String,
        val displayProtocol: String
    ) : LinkValidationState()
    data class Invalid(val errorMsg: String) : LinkValidationState()
}

fun parseConnectionLink(rawInput: String, isFa: Boolean): LinkValidationState {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) return LinkValidationState.Empty

    val profile = com.example.utils.VpnConfigParser.parse(trimmed)
    
    return if (profile != null) {
        val displayProtocol = when {
            trimmed.startsWith("vless://", ignoreCase = true) -> "VLESS"
            trimmed.startsWith("vmess://", ignoreCase = true) -> "VMess"
            trimmed.startsWith("trojan://", ignoreCase = true) -> "Trojan"
            trimmed.startsWith("ss://", ignoreCase = true) -> "ShadowSocks"
            else -> "VLESS"
        }
        LinkValidationState.Valid(
            name = profile.name,
            host = profile.serverIp,
            port = profile.port,
            protocol = profile.protocol,
            secret = profile.secretKey,
            sni = profile.sni,
            displayProtocol = displayProtocol
        )
    } else {
        val error = if (isFa) 
            "قالب پیوند نامعتبر است یا پروتکل پشتیبانی نمی‌شود."
            else "Invalid link format or unsupported protocol."
        LinkValidationState.Invalid(error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationParserComponent(
    isFa: Boolean,
    rawLinkInput: String,
    onLinkChanged: (String) -> Unit,
    validationState: LinkValidationState
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    var secretVisible by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isFa) "📌 تحلیلگر و اعتبارسنجی پیوند اتصال" else "📌 Connection Link Parser & Validator",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CobaltBlue,
            letterSpacing = 0.5.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = rawLinkInput,
                onValueChange = onLinkChanged,
                label = { Text(if (isFa) "پیوند اتصال را این‌جا وارد کنید" else "Paste connection VPN link") },
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CobaltBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedLabelColor = CobaltBlue,
                    unfocusedLabelColor = TextSecondary
                ),
                placeholder = { 
                    Text(
                        text = "vless://... or vmess://... or ss://...", 
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 11.sp
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("link_parser_input_field")
            )

            // Dynamic 1-Tap Paste Tool
            IconButton(
                onClick = {
                    try {
                        val clipData = clipboardManager.primaryClip
                        if (clipData != null && clipData.itemCount > 0) {
                            val text = clipData.getItemAt(0).text?.toString() ?: ""
                            if (text.isNotBlank()) {
                                onLinkChanged(text)
                                Toast.makeText(
                                    context, 
                                    if (isFa) "با موفقیت پیست شد" else "Successfully pasted from clipboard", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context, 
                                    if (isFa) "کلیپ‌برد شما خالی است" else "Clipboard is empty", 
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context, 
                                if (isFa) "اطلاعات متنی یافت نشد" else "Clipboard holds no text data", 
                                Toast.LENGTH_SHORT
                              ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .testTag("link_parser_paste_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = if (isFa) "جای‌گذاری از کلیپ‌برد" else "Tap to Paste Subscription Link",
                    tint = CobaltBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Animated Content for validations layout
        AnimatedContent(
            targetState = validationState,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "validation_status_visual"
        ) { state ->
            when (state) {
                is LinkValidationState.Empty -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    tint = CobaltBlue,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isFa) "راهنمای استفاده از پیوندهای اتصال" else "Automated Extraction Assistance",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (isFa) 
                                    "یک لینک اتصال معتبر را پیست کنید تا آدرس آی‌پی، پروتکل‌های امنیتی، و کلید رمزنگاری فوراً استخراج و فیلدها پر شوند."
                                    else "Paste a link connection. Our parser will instantly evaluate structures, ensure conformity, and isolate encryption tokens properly.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.wrapContentSize()
                            ) {
                                listOf("VLESS", "VMess", "Trojan", "Shadowsocks").forEach { protocol ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(6.dp))
                                            .border(1.dp, GlassBorder.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = protocol,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CobaltBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is LinkValidationState.Invalid -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                tint = AlertRed,
                                contentDescription = if (isFa) "هشدار نامعتبر" else "Format invalid alert icon",
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 1.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isFa) "قالب پیوند نامعتبر است" else "Format Validation Warning",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                                Text(
                                    text = state.errorMsg,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
                is LinkValidationState.Valid -> {
                    val p = state
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberGreen.copy(alpha = 0.06f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("valid_link_preview_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        tint = CyberGreen,
                                        contentDescription = "Valid parameters parsed",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isFa) "✓ پیوند با موفقیت تایید شد" else "✓ Valid Config Format Matches",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberGreen
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(CobaltBlue.copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, CobaltBlue.copy(alpha = 0.4f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = p.displayProtocol,
                                        color = CobaltBlue,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.05f))

                            // Display parameter blocks
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Language, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                            Text(if (isFa) "آدرس آی‌پی / دامنه" else "Host IP / Domain Address", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Text(p.host, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(modifier = Modifier.weight(0.7f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Tag, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                            Text(if (isFa) "پورت" else "Port", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Text("${p.port}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1.1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Label, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                            Text(if (isFa) "نام انتخابی سرور" else "Profile Alias Tag", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Text(p.name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                            Text(if (isFa) "آدرس بای‌پس (SNI)" else "Stealth SNI Link", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Text(p.sni, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                        Text(if (isFa) "رمز عبور / کلید عمومی اتصال" else "Security Encryption Key Token", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val lengthToMask = p.secret.length
                                        val displaySecret = if (secretVisible) p.secret else "•".repeat(if (lengthToMask < 20) lengthToMask else 20) + " (Click eye)"
                                        Text(
                                            text = displaySecret,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CobaltBlue,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { secretVisible = !secretVisible },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (secretVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (secretVisible) "Hide passkey" else "Reveal passkey",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerFormDialog(
    isFa: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (name: String, ip: String, port: Int, protocol: String, secret: String, sni: String) -> Unit
) {
    var rawLinkInput by remember { mutableStateOf("") }
    
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var protocol by remember { mutableStateOf("VLESS (Reality)") }
    var secret by remember { mutableStateOf("") }
    var sni by remember { mutableStateOf("www.google.com") }

    var expandedProtocolDropdown by remember { mutableStateOf(false) }
    var showManualFields by remember { mutableStateOf(false) }

    // Dynamic Reactive state computation of Link Parser validation
    val validationState = remember(rawLinkInput) {
        parseConnectionLink(rawLinkInput, isFa)
    }

    // Auto synchronize coordinates to underlying parameters when a valid Link is Decoded
    LaunchedEffect(validationState) {
        if (validationState is LinkValidationState.Valid) {
            name = validationState.name
            ip = validationState.host
            port = validationState.port.toString()
            protocol = validationState.protocol
            secret = validationState.secret
            sni = validationState.sni
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || ip.isBlank() || port.isBlank()) return@Button
                    val portInt = port.toIntOrNull() ?: 443
                    onSave(name, ip, portInt, protocol, secret, sni)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
            ) {
                Text(
                    text = if (isFa) "ذخیره سرور" else "Save Server", 
                    color = DarkBg, 
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isFa) "انصراف" else "Cancel", 
                    color = TextSecondary
                )
            }
        },
        title = {
            Text(
                text = if (isFa) "افزودن سرور جدید" else "Add New Server Node", 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Smart Configuration Parser Component ---
                ConfigurationParserComponent(
                    isFa = isFa,
                    rawLinkInput = rawLinkInput,
                    onLinkChanged = { rawLinkInput = it },
                    validationState = validationState
                )

                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                // Expanding panel header for advanced/manual parameters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualFields = !showManualFields }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showManualFields) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            tint = CobaltBlue,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isFa) "تنظیم هماهنگی یا فیلدها دستی" else "Customize Settings Manually",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue
                        )
                    }
                    if (validationState is LinkValidationState.Valid) {
                        Box(
                            modifier = Modifier
                                .background(CyberGreen.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFa) "فیلدها تکمیل شد" else "Coordinates Placed",
                                color = CyberGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Smoothly animated accordion for entry forms
                AnimatedVisibility(
                    visible = showManualFields,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (isFa) "نام سرور (مثال: تهران ۲)" else "Profile Alias (e.g. Tehran 2)") },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text(if (isFa) "آدرس آی‌پی یا دامنه" else "Server Host IP / Domain") },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text(if (isFa) "پورت" else "Port Connection") },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Simulated protocol dropdown trigger
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = protocol,
                                onValueChange = {},
                                label = { Text(if (isFa) "پروتکل امنیتی" else "Security Tunnel Protocol") },
                                readOnly = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                trailingIcon = {
                                    IconButton(onClick = { expandedProtocolDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CyanGlow)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanGlow,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedProtocolDropdown = true }
                            )
                            DropdownMenu(
                                expanded = expandedProtocolDropdown,
                                onDismissRequest = { expandedProtocolDropdown = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                val items = listOf("VLESS (Reality)", "Trojan", "ShadowSocks", "WireGuard")
                                items.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item, color = Color.White) },
                                        onClick = {
                                            protocol = item
                                            expandedProtocolDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = secret,
                            onValueChange = { secret = it },
                            label = { Text(if (isFa) "کلید خصوصی یا پسورد" else "Secret Passkey / UUID Token") },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = sni,
                            onValueChange = { sni = it },
                            label = { Text(if (isFa) "آدرس بای‌پس (SNI)" else "SNI Bypass Hostname") },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
}

// Nice color constants for secondary usage
val YellowGlow = Color(0xFFFFD200)

@Composable
fun contentSpacer(height: androidx.compose.ui.unit.Dp) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
fun SplashIntroScreen(
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    // Core spin rotation: Gold outer ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_spin"
    )

    // Reverse spin rotation: Teal inner ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_spin"
    )

    // Pulsing size of core glow aura
    val coreGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_glow"
    )

    // Vertical neon sweeping scanline progress
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 168f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_scanner"
    )

    // Background water flow sine waves phase animation
    val wavesAnimationTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waves_flow"
    )

    // Background stardust floating drift timer
    val starsDriftTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stardust_drift"
    )

    // Entrance animation triggers (Fades and Spring bounces on start)
    var isEnteredState by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isEnteredState = true
    }

    val entryScale by animateFloatAsState(
        targetValue = if (isEnteredState) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "entry_bounce_scale"
    )

    val entryAlpha by animateFloatAsState(
        targetValue = if (isEnteredState) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "entry_fade"
    )

    val contentOffsetY by animateDpAsState(
        targetValue = if (isEnteredState) 0.dp else 45.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "entry_slide_up"
    )

    // Auto-advance skip hook (runs after 3.2 seconds of display)
    LaunchedEffect(Unit) {
         kotlinx.coroutines.delay(3200)
         onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: Deep Space Cyber-Mesh and Floating Hybrid Wave Nodes Canvas
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val densityValue = density

            // 1. Dual Flowing Sine Trails (Teal & Gold)
            val wavePathTeal = androidx.compose.ui.graphics.Path()
            wavePathTeal.moveTo(0f, height * 0.72f)
            for (x in 0..width.toInt() step 8) {
                val relativeX = x.toFloat() / width
                val sinY = height * 0.72f + 35f * kotlin.math.sin(relativeX * 2 * Math.PI + wavesAnimationTime).toFloat()
                wavePathTeal.lineTo(x.toFloat(), sinY)
            }
            wavePathTeal.lineTo(width, height)
            wavePathTeal.lineTo(0f, height)
            wavePathTeal.close()

            drawPath(
                path = wavePathTeal,
                brush = Brush.verticalGradient(
                    colors = listOf(CyanGlow.copy(alpha = 0.08f), Color.Transparent)
                )
            )

            val wavePathGold = androidx.compose.ui.graphics.Path()
            wavePathGold.moveTo(0f, height * 0.76f)
            for (x in 0..width.toInt() step 8) {
                val relativeX = x.toFloat() / width
                val sinY = height * 0.76f + 25f * kotlin.math.sin(relativeX * 3 * Math.PI - wavesAnimationTime + 1.25f).toFloat()
                wavePathGold.lineTo(x.toFloat(), sinY)
            }
            wavePathGold.lineTo(width, height)
            wavePathGold.lineTo(0f, height)
            wavePathGold.close()

            drawPath(
                path = wavePathGold,
                brush = Brush.verticalGradient(
                    colors = listOf(CobaltBlue.copy(alpha = 0.05f), Color.Transparent)
                )
            )

            // 2. Trigonometric Star/Tunnel nodes drifting
            val floatingSpots = listOf(
                Pair(0.12f, 0.22f), Pair(0.88f, 0.18f),
                Pair(0.18f, 0.65f), Pair(0.82f, 0.58f),
                Pair(0.35f, 0.12f), Pair(0.65f, 0.14f),
                Pair(0.48f, 0.38f), Pair(0.52f, 0.78f),
                Pair(0.24f, 0.88f), Pair(0.76f, 0.84f)
            )

            floatingSpots.forEachIndexed { index, spot ->
                val angleMod = starsDriftTime * 2 * Math.PI + index
                val driftX = kotlin.math.sin(angleMod).toFloat() * 12f * densityValue
                val driftY = kotlin.math.cos(angleMod * 1.4).toFloat() * 12f * densityValue
                val nodeX = spot.first * width + driftX
                val nodeY = spot.second * height + driftY
                
                val spotColor = if (index % 2 == 0) CyanGlow else CobaltBlue
                val fadeVal = 1f - kotlin.math.abs(0.5f - starsDriftTime) * 2f
                val radius = (2.5f + (index % 3)) * densityValue

                // Soft Outer Flare
                drawCircle(
                    color = spotColor.copy(alpha = 0.22f * fadeVal),
                    radius = radius + 8f * densityValue,
                    center = androidx.compose.ui.geometry.Offset(nodeX, nodeY)
                )
                // Crisp Inner Core
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = radius * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(nodeX, nodeY)
                )
            }
        }

        // LAYER 2: Central Sacred Logo with double spinning rings & neon scanline overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .offset(y = contentOffsetY)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(scaleX = entryScale, scaleY = entryScale)
                    .shadow(16.dp * coreGlowScale, CircleShape, ambientColor = CyanGlow, spotColor = CobaltBlue)
                    .background(Color.White.copy(alpha = 0.015f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Spinning Star/Rings Orbits inside
                // Outer gold dash ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(outerRotation)
                        .border(1.5.dp, Brush.sweepGradient(listOf(CobaltBlue, Color.Transparent, CobaltBlue.copy(alpha = 0.1f), CobaltBlue)), CircleShape)
                )

                // Middle teal solid fine ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .rotate(innerRotation)
                        .border(1.5.dp, Brush.sweepGradient(listOf(Color.Transparent, CyanGlow, CyanGlow.copy(alpha = 0.05f), CyanGlow)), CircleShape)
                )

                // Inner core shield container
                Box(
                    modifier = Modifier
                        .size(95.dp)
                        .background(SurfaceDark.copy(alpha = 0.75f), CircleShape)
                        .border(1.dp, GlassBorder, CircleShape)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        tint = CobaltBlue,
                        contentDescription = "Shield Guard",
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(if (coreGlowScale > 1f) 8.dp else 4.dp, CircleShape, ambientColor = CobaltBlue)
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Lock,
                        tint = DarkBg,
                        contentDescription = "Lock Central",
                        modifier = Modifier
                            .size(14.dp)
                            .offset(y = 1.dp)
                    )
                }

                // Cyber Scanning line sweeping across the frame
                Box(
                    modifier = Modifier
                        .offset(y = scanProgress.dp)
                        .fillMaxWidth(0.85f)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, CyanGlow, CobaltBlue, CyanGlow, Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // LAYER 3: Luxury Brand Titles
            // Large Bold heading "EHSAN VPN" with gold typography sheen
            Text(
                text = "EHSAN VPN",
                color = CobaltBlue,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp,
                modifier = Modifier
                    .graphicsLayer(scaleX = entryScale, scaleY = entryScale)
                    .shadow(4.dp * coreGlowScale, shape = CircleShape, ambientColor = CobaltBlue)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Glowing subtitles for edition representation
            Text(
                text = if (isFa) "احسان وی‌پی‌ان • نسخه طلایی کله‌غازی" else "EHSAN VPN • Golden Teal Edition",
                color = CyanGlow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time task loading messages
            Text(
                text = if (isFa) 
                    "در حال اتصال ایمن به تونل‌های هیبرید طلایی..." 
                    else "Aligning celestial tunnel structures safely...",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // LAYER 4: Holographic Matte Skip pill button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorderStrong.copy(alpha = 0.65f)),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                modifier = Modifier
                    .wrapContentSize()
                    .height(48.dp)
                    .testTag("skip_splash_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isFa) "ورود مستقیم" else "Skip Introduction",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Skip Arrow Symbol",
                        tint = CobaltBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedTestCardComponent(
    isFa: Boolean,
    isTesting: Boolean,
    progress: Float,
    downloadMbps: Double,
    uploadMbps: Double,
    pingMs: Int,
    onStartTest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("speed_test_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CyanGlow.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (isFa) "تست سرعت و پینگ زنده" else "Live Speedometer Test",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isTesting) {
                    Box(
                        modifier = Modifier
                            .background(YellowGlow.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFa) "در حال پایش..." else "Testing...",
                            color = YellowGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speedometer Visual Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1.0f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White.copy(alpha = 0.05f),
                        strokeWidth = 6.dp,
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (progress < 0.6f) CyanGlow else CyberGreen,
                        strokeWidth = 6.dp,
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val activeVal = if (progress < 0.15f) 0.0 
                                        else if (progress < 0.60f) downloadMbps 
                                        else uploadMbps
                        Text(
                            text = String.format("%.1f", activeVal),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (progress < 0.60f) "Mbps Down" else "Mbps Up",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(YellowGlow, CircleShape))
                        Text(
                            text = if (isFa) "تأخیر پینگ: " else "Ping Latency: ",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (pingMs > 0) "$pingMs ms" else "---",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pingMs > 0) CyberGreen else Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(CyanGlow, CircleShape))
                        Text(
                            text = if (isFa) "سرعت دانلود: " else "Download: ",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (downloadMbps > 0) String.format("%.1f Mbps", downloadMbps) else "---",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(CyberGreen, CircleShape))
                        Text(
                            text = if (isFa) "سرعت آپلود: " else "Upload: ",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (uploadMbps > 0) String.format("%.1f Mbps", uploadMbps) else "---",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartTest,
                enabled = !isTesting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("start_speed_test_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTesting) Color.White.copy(alpha = 0.05f) else CobaltBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = if (isTesting) TextSecondary else DarkBg,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )
                Text(
                    text = if (isTesting) {
                        if (isFa) "در حال پایش لینک..." else "Gauging Golden Gateways..."
                    } else {
                        if (isFa) "سنجش سرعت و صحت اتصال" else "Test Tunnel Speed"
                    },
                    color = if (isTesting) TextMuted else DarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DiagnosisCardComponent(
    isFa: Boolean,
    isDiagnosing: Boolean,
    dnsStatus: String,
    gatewayStatus: String,
    sniStatus: String,
    advice: String,
    onRunDiagnosis: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diagnosis_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(CyberGreen.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (isFa) "عیب‌یابی خودکار شبکه" else "Automatic Self-Diagnosis Portal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (isDiagnosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CyberGreen,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DiagnosisRowItem(
                    label = if (isFa) "دسترس‌پذیری سیستم دی‌ان‌اس (DNS Resolution)" else "Domain Name Resolution (DNS)",
                    status = dnsStatus,
                    isFa = isFa
                )
                DiagnosisRowItem(
                    label = if (isFa) "برقراری نشست با سرور (Gateway Handshake)" else "Gateway Tunnel TCP Connection",
                    status = gatewayStatus,
                    isFa = isFa
                )
                DiagnosisRowItem(
                    label = if (isFa) "صحت آدرس گریز فیلترینگ (SNI Bypass TLS)" else "Hidden Bypass Authentication (SNI)",
                    status = sniStatus,
                    isFa = isFa
                )
            }

            if (advice.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                val boxBg = if (dnsStatus == "FAIL" || gatewayStatus == "FAIL" || sniStatus == "FAIL") {
                    AlertRed.copy(alpha = 0.08f)
                } else {
                    CyberGreen.copy(alpha = 0.08f)
                }
                val boxBorder = if (dnsStatus == "FAIL" || gatewayStatus == "FAIL" || sniStatus == "FAIL") {
                    AlertRed.copy(alpha = 0.25f)
                } else {
                    CyberGreen.copy(alpha = 0.25f)
                }
                val iconTint = if (dnsStatus == "FAIL" || gatewayStatus == "FAIL" || sniStatus == "FAIL") {
                    AlertRed
                } else {
                    CyberGreen
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(boxBg, RoundedCornerShape(12.dp))
                        .border(1.dp, boxBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (dnsStatus == "FAIL" || gatewayStatus == "FAIL" || sniStatus == "FAIL") Icons.Default.ReportProblem else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = if (isFa) "گزارش و پیشنهاد سیستم:" else "System Advisory Report:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = advice,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRunDiagnosis,
                enabled = !isDiagnosing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("run_diagnosis_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDiagnosing) Color.White.copy(alpha = 0.05f) else CyberGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = if (isDiagnosing) TextSecondary else DarkBg,
                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                )
                Text(
                    text = if (isDiagnosing) {
                        if (isFa) "در حال آنالیز..." else "Analyzing Filtering Telemetries..."
                    } else {
                        if (isFa) "آنالیز و عیب‌یابی جامع اتصال" else "Initiate Diagnostic Health-Check"
                    },
                    color = if (isDiagnosing) TextMuted else DarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DiagnosisRowItem(
    label: String,
    status: String,
    isFa: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val (text, color, icon) = when (status) {
                "PASS" -> Triple(
                    if (isFa) "بدون اشکال" else "Passed",
                    CyberGreen,
                    Icons.Default.CheckCircle
                )
                "FAIL" -> Triple(
                    if (isFa) "قطع / فیلتر" else "Blocked/Failed",
                    AlertRed,
                    Icons.Default.Error
                )
                "RUNNING" -> Triple(
                    if (isFa) "در حال اسکن" else "Scanning...",
                    YellowGlow,
                    Icons.Default.HourglassBottom
                )
                else -> Triple(
                    if (isFa) "تست نشده" else "Untested",
                    TextMuted,
                    Icons.Default.HelpOutline
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- NEW COMPONENT: Real-time Waving Traffic Graph (Teal-Gold Theme) ---
@Composable
fun LiveTrafficGraphCard(
    downloadSpeed: Float, // in KB/s
    uploadSpeed: Float,   // in KB/s
    isFa: Boolean
) {
    val downloadHistory = remember { mutableStateListOf<Float>() }
    val uploadHistory = remember { mutableStateListOf<Float>() }
    
    // Sample speeds periodically
    LaunchedEffect(downloadSpeed, uploadSpeed) {
        val nextDl = downloadSpeed.coerceAtLeast(0f)
        val nextUl = uploadSpeed.coerceAtLeast(0f)
        downloadHistory.add(nextDl)
        uploadHistory.add(nextUl)
        if (downloadHistory.size > 24) downloadHistory.removeAt(0)
        if (uploadHistory.size > 24) uploadHistory.removeAt(0)
    }
    
    // Fallback if empty to draw a beautiful flat line
    if (downloadHistory.isEmpty()) {
        repeat(15) {
            downloadHistory.add(0f)
            uploadHistory.add(0f)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_traffic_graph"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFa) "نمودار لایو ثانیه‌ای ترافیک" else "Live Real-time Traffic Graph",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(CyanGlow, CircleShape))
                        Text(text = if (isFa) "دانلود" else "Download", fontSize = 10.sp, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(YellowGlow, CircleShape))
                        Text(text = if (isFa) "آپلود" else "Upload", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val maxDl = downloadHistory.maxOrNull() ?: 10f
                    val maxUl = uploadHistory.maxOrNull() ?: 10f
                    val maxVal = maxOf(maxDl, maxUl, 50f) // minimum scale for stability
                    
                    val dlPath = Path()
                    val ulPath = Path()
                    
                    val stepX = width / 23f
                    
                    // Draw Download curve (Teal)
                    downloadHistory.forEachIndexed { i, speed ->
                        val x = i * stepX
                        val y = height - (speed / maxVal) * height
                        if (i == 0) dlPath.moveTo(x, y) else dlPath.lineTo(x, y)
                    }
                    
                    // Draw Upload curve (Gold)
                    uploadHistory.forEachIndexed { i, speed ->
                        val x = i * stepX
                        val y = height - (speed / maxVal) * height
                        if (i == 0) ulPath.moveTo(x, y) else ulPath.lineTo(x, y)
                    }
                    
                    // Draw curves with a glowing stroke
                    drawPath(
                        path = dlPath,
                        color = CyanGlow,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = ulPath,
                        color = YellowGlow,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    
                    // Draw ambient shadow glow beneath download
                    val dlFillPath = Path().apply {
                        addPath(dlPath)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = dlFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanGlow.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                }
            }
        }
    }
}

// --- NEW COMPONENT: QR Code Sharing Dialog (Mallard-Teal & Gold Aesthetic) ---
@Composable
fun ServerQrDialog(
    profile: VpnProfile,
    isFa: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val protocolScheme = when {
        profile.protocol.lowercase().contains("trojan") -> "trojan"
        profile.protocol.lowercase().contains("ss") || profile.protocol.lowercase().contains("shadowsocks") -> "ss"
        else -> "vless"
    }
    
    val encodedName = try {
        java.net.URLEncoder.encode(profile.name, "UTF-8")
    } catch (e: Exception) {
        profile.name
    }
    
    val rawLink = "$protocolScheme://${profile.secretKey}@${profile.serverIp}:${profile.port}?sni=google.com#$encodedName"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .testTag("server_qr_dialog"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "کد QR اشتراک‌گذاری" else "Share Server QR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Subtitle
                Text(
                    text = if (isFa) "برای اتصال آنی و درون‌برنامه‌ای، این کد را توسط اسکنر هوشمند اسکن کنید." else "Point the smart scanner at this code to securely import this configuration.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                // QR Code Display
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                        .border(1.2.dp, GlassBorderStrong, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(175.dp)) {
                        val h = size.height
                        val w = size.width
                        val cols = 17
                        val rows = 17
                        val cw = w / cols
                        val ch = h / rows
                        
                        // Top-Left corner finder
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cw * 7, ch * 7))
                        drawRect(color = SurfaceDark, topLeft = androidx.compose.ui.geometry.Offset(cw, ch), size = androidx.compose.ui.geometry.Size(cw * 5, ch * 5))
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(cw * 2, ch * 2), size = androidx.compose.ui.geometry.Size(cw * 3, ch * 3))
                        
                        // Top-Right corner finder
                        val trX = w - cw * 7
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(trX, 0f), size = androidx.compose.ui.geometry.Size(cw * 7, ch * 7))
                        drawRect(color = SurfaceDark, topLeft = androidx.compose.ui.geometry.Offset(trX + cw, ch), size = androidx.compose.ui.geometry.Size(cw * 5, ch * 5))
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(trX + cw * 2, ch * 2), size = androidx.compose.ui.geometry.Size(cw * 3, ch * 3))
                        
                        // Bottom-Left corner finder
                        val blY = h - ch * 7
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(0f, blY), size = androidx.compose.ui.geometry.Size(cw * 7, ch * 7))
                        drawRect(color = SurfaceDark, topLeft = androidx.compose.ui.geometry.Offset(cw, blY + ch), size = androidx.compose.ui.geometry.Size(cw * 5, ch * 5))
                        drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(cw * 2, blY + ch * 2), size = androidx.compose.ui.geometry.Size(cw * 3, ch * 3))
                        
                        // Pseudorandom grid using profile hashcode as stable seed
                        val seed = profile.hashCode()
                        val random = java.util.Random(seed.toLong())
                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                // Skip finder areas
                                if (r < 7 && c < 7) continue
                                if (r < 7 && c >= cols - 7) continue
                                if (r >= rows - 7 && c < 7) continue
                                // Skip center alignment shield/logo space
                                if (r in 7..9 && c in 7..9) continue
                                
                                if (random.nextBoolean()) {
                                    val rectColor = if (random.nextFloat() > 0.82f) CyanGlow else Color.White
                                    drawRect(
                                        color = rectColor,
                                        topLeft = androidx.compose.ui.geometry.Offset(c * cw, r * ch),
                                        size = androidx.compose.ui.geometry.Size(cw * 0.9f, ch * 0.9f)
                                    )
                                }
                            }
                        }
                    }

                    // Centered Secure Vpn icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceDark, CircleShape)
                            .border(1.dp, CobaltBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnLock,
                            contentDescription = null,
                            tint = CobaltBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Profile info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isFa) "شناسه سرور:" else "Server Name:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = profile.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isFa) "نشانی آی‌پی:" else "IP Address:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = profile.serverIp, fontSize = 11.sp, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isFa) "پورت فعال:" else "Active Port:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = profile.port.toString(), fontSize = 11.sp, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isFa) "پروتکل اتصال:" else "Connection Protocol:", fontSize = 11.sp, color = TextSecondary)
                            Text(text = profile.protocol, fontSize = 11.sp, color = CyberGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Action Buttons (Copy raw & Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isFa) "بستن" else "Close", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("VPN Code", rawLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isFa) "آدرس اتصال در حافظه کپی شد!" else "Configuration link copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (isFa) "کپی آدرس متنی" else "Copy Raw Config", fontSize = 12.sp, color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- NEW COMPONENT: Interactive QR Code Scanner Simulation Dialog ---
@Composable
fun QrScannerDialog(
    isFa: Boolean,
    onScanResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Camera Sim, 1: Manual Input
    var manualText by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .testTag("qr_scanner_dialog"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "اسکنر هوشمند کد QR" else "Smart QR Code Scanner",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Subtitle Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = CyanGlow,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(text = if (isFa) "شبیه‌ساز دوربین لایو" else "Live Viewfinder", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(text = if (isFa) "وارد کردن دستی کد" else "Paste Text Link", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                    // --- CAMERA SIMULATOR ---
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laserOffset"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.2.dp, GlassBorderStrong, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing scanning grid and laser line
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Draw corner indicator markings for the camera target
                            val framePadding = 45f
                            val strokeW = 6f
                            val len = 40f
                            
                            // Top-Left corner finder border
                            drawPath(
                                Path().apply {
                                    moveTo(framePadding, framePadding + len)
                                    lineTo(framePadding, framePadding)
                                    lineTo(framePadding + len, framePadding)
                                },
                                color = CyanGlow,
                                style = Stroke(width = strokeW)
                            )
                            // Top-Right corner finder border
                            drawPath(
                                Path().apply {
                                    moveTo(w - framePadding - len, framePadding)
                                    lineTo(w - framePadding, framePadding)
                                    lineTo(w - framePadding, framePadding + len)
                                },
                                color = CyanGlow,
                                style = Stroke(width = strokeW)
                            )
                            // Bottom-Left corner finder border
                            drawPath(
                                Path().apply {
                                    moveTo(framePadding, h - framePadding - len)
                                    lineTo(framePadding, h - framePadding)
                                    lineTo(framePadding + len, h - framePadding)
                                },
                                color = CyanGlow,
                                style = Stroke(width = strokeW)
                            )
                            // Bottom-Right corner finder border
                            drawPath(
                                Path().apply {
                                    moveTo(w - framePadding - len, h - framePadding)
                                    lineTo(w - framePadding, h - framePadding)
                                    lineTo(w - framePadding, h - framePadding - len)
                                },
                                color = CyanGlow,
                                style = Stroke(width = strokeW)
                            )

                            // Render moving laser red line
                            val laserY = framePadding + laserOffset * (h - 2 * framePadding)
                            drawLine(
                                color = AlertRed,
                                start = androidx.compose.ui.geometry.Offset(framePadding, laserY),
                                end = androidx.compose.ui.geometry.Offset(w - framePadding, laserY),
                                strokeWidth = 4f
                            )
                        }

                        // Animating Pulse secure connection eye in the scanner
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = CyanGlow.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isFa) "سیستم آماده ردیابی کد QR..." else "Align VPN code within frame...",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Predefined Servers to Simulate Scanning
                    Text(
                        text = if (isFa) "جهت شبیه‌سازی اسکن، روی یکی از گره‌های ابری زیر ضربه بزنید:" else "Tap a cloud node below to simulate instant scanning:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(
                                if (isFa) "🚀 سرور آلمان (مونیخ - VLESS)" else "🚀 Germany Premium (Munich - VLESS)",
                                "vless://germany_secure_sh@194.5.178.43:8443?sni=de.horizon.net#فرانکفورت%20-سپر-طلایی",
                                CyberGreen
                            ),
                            Triple(
                                if (isFa) "⚡ تونل فنلاند (تروجان - بدون مرز)" else "⚡ Finland Quantum (Trojan - Borderless)",
                                "trojan://fi_tunnel_troj@95.217.182.5:443?sni=fi.horizon.net#هلسینکی%20-کوانتوم",
                                CobaltBlue
                            ),
                            Triple(
                                if (isFa) "🛡️ گره هلند (آمستردام - SS)" else "🛡️ Holland Aegis (Amsterdam - SS)",
                                "ss://nl_shadow_sec@82.197.204.11:8080?sni=nl.horizon.net#آمستردام%20-سایبر-شیلد",
                                CyanGlow
                            )
                        ).forEach { (label, link, accent) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Toast.makeText(context, if (isFa) "کد QR اسکن شد!" else "QR Code Scanned successfully!", Toast.LENGTH_SHORT).show()
                                        onScanResult(link)
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(text = if (isFa) "لمس جهت اسکن" else "Tap to scan", fontSize = 10.sp, color = accent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // --- MANUAL CONFIG TEXT INPUT ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isFa) "لینک پیکربندی vless یا trojan خود را کپی کرده و در کادر زیر قرار دهید:" else "Copy and paste your vless://, trojan://, or ss:// connection link below:",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = manualText,
                            onValueChange = { manualText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .testTag("manual_link_input"),
                            placeholder = { Text(text = "vless://key@ip:port?sni=google.com#ServerName", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 11.sp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        if (clipText.isNotBlank()) {
                                            manualText = clipText
                                            Toast.makeText(context, if (isFa) "پیوند کپی شده جایگذاری شد!" else "Pasted from clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "خطا در دسترسی به حافظه", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, GlassBorder),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(text = if (isFa) "چسباندن (Paste)" else "Paste", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (manualText.isNotBlank()) {
                                        onScanResult(manualText)
                                    } else {
                                        Toast.makeText(context, if (isFa) "لطفاً ابتدا کد را وارد کنید!" else "Please write a config link first!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = if (isFa) "تایید و ثبت پیکربندی" else "Submit Config", fontSize = 11.sp, color = DarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- NEW COMPONENT: Pinpoint Live Ping Diagnostic Meter ---
@Composable
fun LivePingDiagnosticCard(
    isFa: Boolean,
    isTesting: Boolean,
    pingMs: Int,
    jitterMs: Int,
    packetLoss: Int,
    onTestPing: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_ping_diagnostic_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFa) "تست پینگ و سلامت اتصال اختصاصی" else "Live Ping & Latency Health",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onTestPing,
                    enabled = !isTesting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CobaltBlue,
                        disabledContainerColor = CobaltBlue.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Text(
                            text = if (isFa) "تست آنی" else "Run Test",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBg
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latency Block
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isFa) "تأخیر (Delay)" else "Latency",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (pingMs > 0) "$pingMs ms" else "---",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pingMs <= 0) TextMuted else if (pingMs < 100) CyberGreen else YellowGlow
                        )
                    }
                }

                // Jitter Block
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isFa) "نوسان (Jitter)" else "Jitter",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (pingMs > 0) "$jitterMs ms" else "---",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pingMs <= 0) TextMuted else if (jitterMs < 10) CyberGreen else CyanGlow
                        )
                    }
                }

                // Packet Loss Block
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isFa) "پکت لاست" else "Packet Loss",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (pingMs > 0 || packetLoss > 0) "$packetLoss%" else "---",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (packetLoss == 0) CyberGreen else AlertRed
                        )
                    }
                }
            }

            // Connection Quality rating text indicator
            val ratingText = when {
                pingMs <= 0 -> if (isFa) "آماده اندازه‌گیری پینگ..." else "Awaiting measurement..."
                packetLoss > 20 -> if (isFa) "وضعیت اتصال نامناسب (تداخل بالا)" else "Poor Connection Quality"
                pingMs < 100 -> if (isFa) "کیفیت طلایی (مناسب گیمینگ و ویدیو)" else "Excellent Quality (Gaming Ready)"
                pingMs < 200 -> if (isFa) "کیفیت بسیار خوب و روان" else "Good Connection Core"
                else -> if (isFa) "اتصال با تأخیر متوسط" else "Stable with Moderate Latency"
            }
            val ratingColor = when {
                pingMs <= 0 -> TextSecondary
                packetLoss > 20 -> AlertRed
                pingMs < 100 -> CyberGreen
                pingMs < 200 -> CyanGlow
                else -> YellowGlow
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(ratingColor, CircleShape)
                )
                Text(
                    text = ratingText,
                    fontSize = 10.sp,
                    color = ratingColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// --- NEW COMPONENT: Immersive Session Blueprint Details ---
@Composable
fun ActiveConnectionBlueprintCard(
    activeServer: VpnProfile?,
    durationSeconds: Long,
    vpnState: String,
    isFa: Boolean
) {
    if (activeServer == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_connection_blueprint_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsSuggest,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isFa) "شناسه وضعیت و مشخصات امنیتی تونل" else "Active Connection Blueprint & Tunnel Info",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Duration timer in real-time
                if (vpnState == "CONNECTED") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%02d:%02d:%02d", durationSeconds / 3600, (durationSeconds % 3600) / 60, durationSeconds % 60),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )
                    }
                } else {
                    Text(
                        text = if (isFa) "قطع ارتباط" else "Disconnected",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlertRed,
                        modifier = Modifier
                            .background(AlertRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.05f))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlueprintItem(
                    label = if (isFa) "پروتکل شبیه‌سازی:" else "Tunnel Protocol:", 
                    value = activeServer.protocol, 
                    accentColor = CyberGreen
                )
                BlueprintItem(
                    label = if (isFa) "نشانی دروازه (Proxy Host):" else "Gateway Routing IP:", 
                    value = "${activeServer.serverIp}:${activeServer.port}", 
                    accentColor = Color.White
                )
                BlueprintItem(
                    label = if (isFa) "شناسه فرار از فیلتر (SNI Host):" else "Bypass Hostname (SNI):", 
                    value = activeServer.sni.ifBlank { "google.com" }, 
                    accentColor = CyanGlow
                )
                BlueprintItem(
                    label = if (isFa) "مکانیزم رمزنگاری:" else "Cipher & Encryption:", 
                    value = "AES-256-GCM / ChaCha20", 
                    accentColor = YellowGlow
                )
                BlueprintItem(
                    label = if (isFa) "مسیریابی کلاینت (Virtual IP):" else "Routing Client Int:", 
                    value = "10.0.0.2 -> 0.0.0.0/0", 
                    accentColor = TextSecondary
                )
                BlueprintItem(
                    label = if (isFa) "سامانه دی‌ان‌اس تونل (DNS SEC):" else "Secure DNS Nameserver:", 
                    value = "1.1.1.1 (Cloudflare DNSSEC)", 
                    accentColor = CobaltBlue
                )
            }
        }
    }
}

@Composable
fun BlueprintItem(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
    }
}

// --- NEW COMPONENT: Smart QR Sharing & Quick Import Hub ---
@Composable
fun SmartQrSharingHub(
    activeServer: VpnProfile?,
    isFa: Boolean,
    onScanQr: () -> Unit,
    onShareCurrent: () -> Unit,
    onPasteConfig: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_qr_sharing_hub"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = YellowGlow,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isFa) "مرکز اشتراک‌گذاری و وارد کردن سریع" else "Smart QR & Quick Sharing Hub",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Text(
                text = if (isFa) "کدهای اشتراک گذاری VLESS یا Trojan خود را فوراً به بارکد QR تبدیل کنید یا مستقیماً وارد کنید تا بقیه به سرعت به شبکه آزاد احسان وصل بشن."
                       else "Unified Sharing Engine: Instantly project profiles into scan-ready QR codes or copy configuration lines into the client.",
                fontSize = 10.sp,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scan QR Button
                Button(
                    onClick = onScanQr,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = DarkBg,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFa) "اسکن QR" else "Scan QR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBg
                    )
                }

                // Share active QR Button
                Button(
                    onClick = onShareCurrent,
                    enabled = activeServer != null,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowGlow,
                        disabledContainerColor = Color.White.copy(alpha = 0.04f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = if (activeServer != null) DarkBg else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFa) "اشتراک فعال" else "Share Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeServer != null) DarkBg else TextMuted
                    )
                }

                // Manual Config insertion shortcut
                OutlinedButton(
                    onClick = onPasteConfig,
                    modifier = Modifier.weight(1.5f),
                    border = BorderStroke(1.dp, GlassBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFa) "چسباندنلینک" else "Paste Link",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- NEW COMPONENT: AdvancedSettingsDialog for AMOLED, Split Tunneling, Reality ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsDialog(
    isFa: Boolean,
    isAmoledMode: Boolean,
    isSplitEnabled: Boolean,
    splitApps: List<com.example.AppInfo>,
    show: Boolean,
    onDismiss: () -> Unit,
    onToggleAmoled: () -> Unit,
    onToggleSplit: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit
) {
    if (!show) return

    var appSearchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(splitApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) {
            splitApps
        } else {
            splitApps.filter {
                it.appName.contains(appSearchQuery, ignoreCase = true) ||
                        it.packageName.contains(appSearchQuery, ignoreCase = true)
            }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "امکانات پیشرفته محصول" else "Advanced Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scenario 1: AMOLED Dark Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isFa) "پوسته تیره عمیق AMOLED" else "AMOLED Black Theme",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "مشکی مطلق جهت مصرف بهینه باتری" else "Pitch black base for energy saving",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isAmoledMode,
                        onCheckedChange = { onToggleAmoled() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanGlow,
                            checkedTrackColor = CyanGlow.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scenario 2: Split Tunneling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isFa) "تونل تفکیک‌شده (Split Tunneling)" else "Split Tunneling",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isFa) "عبور مستقیم برنامه‌های بانکی بدون پروکسی" else "Bypass local or bank apps automatically",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isSplitEnabled,
                        onCheckedChange = onToggleSplit,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanGlow,
                            checkedTrackColor = CyanGlow.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                if (isSplitEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = if (isFa) "جستجوی برنامه‌ها..." else "Search apps...",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.01f))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            items(filteredApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleApp(app.packageName) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(
                                                    if (app.isBypassed) CyanGlow.copy(alpha = 0.15f)
                                                    else Color.White.copy(alpha = 0.05f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = app.appName.firstOrNull()?.uppercase()?.toString() ?: "A",
                                                color = if (app.isBypassed) CyanGlow else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = app.appName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = app.packageName,
                                                fontSize = 8.sp,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = app.isBypassed,
                                        onCheckedChange = { onToggleApp(app.packageName) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CyanGlow,
                                            uncheckedColor = GlassBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow)
                ) {
                    Text(
                        text = if (isFa) "ذخیره و بستن" else "Apply Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF061415),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


