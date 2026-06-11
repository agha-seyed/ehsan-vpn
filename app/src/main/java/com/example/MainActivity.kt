package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
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

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    // Android System VPN Permissions Launcher
    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Consent granted, ignite connection
            viewModel.toggleVpnConnection(this, null)
        } else {
            Toast.makeText(this, "برای اتصال امن احتیاج به تایید مجوز سیستم است.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_screen")
                ) { innerPadding ->
                    VpnDashboard(
                        viewModel = viewModel,
                        onPrepareVpn = { prepareIntent ->
                            vpnPrepareLauncher.launch(prepareIntent)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

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

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isFa = (appLanguage == "fa")

    var showSplash by remember { mutableStateOf(true) }

    var showServerSheet by remember { mutableStateOf(false) }
    var showVpsAssistant by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }

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
                onDismiss = { showServerSheet = false },
                isFa = isFa
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
    onSelectProfile: (VpnProfile) -> Unit,
    onDeleteProfile: (VpnProfile) -> Unit,
    onAddNewProfileClick: () -> Unit,
    onDismiss: () -> Unit,
    isFa: Boolean = true
) {
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

                    IconButton(
                        onClick = onAddNewProfileClick,
                        modifier = Modifier.background(CyanGlow.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isFa) "افزودن سرور" else "Add Server Profile",
                            tint = CyanGlow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = Color.White.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(14.dp))

                if (profiles.isEmpty()) {
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
                                text = if (isFa) "هیچ سروری اضافه نشده است" else "No server profile found",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(profiles) { profile ->
                            val isSelected = activeProfile?.id == profile.id
                            ServerProfileItem(
                                profile = profile,
                                isSelected = isSelected,
                                onSelect = { onSelectProfile(profile) },
                                onDelete = { onDeleteProfile(profile) }
                            )
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
                        text = if (isFa) "خروج / بستن" else "Close Dashboard",
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
    onDelete: () -> Unit
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

    val protocol: String
    val remaining: String
    val displayProtocol: String

    when {
        trimmed.startsWith("vless://", ignoreCase = true) -> {
            protocol = "VLESS (Reality)"
            displayProtocol = "VLESS"
            remaining = trimmed.substring("vless://".length)
        }
        trimmed.startsWith("ss://", ignoreCase = true) -> {
            protocol = "ShadowSocks"
            displayProtocol = "ShadowSocks"
            remaining = trimmed.substring("ss://".length)
        }
        trimmed.startsWith("trojan://", ignoreCase = true) -> {
            protocol = "Trojan"
            displayProtocol = "Trojan"
            remaining = trimmed.substring("trojan://".length)
        }
        trimmed.startsWith("vmess://", ignoreCase = true) -> {
            protocol = "VLESS (Reality)" // Database storage equivalent
            displayProtocol = "VMess"
            remaining = trimmed.substring("vmess://".length)
        }
        else -> {
            val error = if (isFa) 
                "پروتکل نامعتبر است. فرمت باید با vless:// یا ss:// یا trojan:// یا vmess:// شروع شود."
                else "Unsupported protocol. Must begin with vless://, ss://, trojan://, or vmess//"
            return LinkValidationState.Invalid(error)
        }
    }

    try {
        var host = ""
        var portStr = "443"
        var secret = ""
        var sni = "www.google.com"
        var name = if (isFa) "سرور کانفیگ سریع" else "Imported Server Node"

        if (displayProtocol == "VMess") {
            val base64Clean = remaining.split("#").first().trim()
            val paddedBase64 = try {
                val padLength = (4 - base64Clean.length % 4) % 4
                base64Clean + "=".repeat(padLength)
            } catch (e: Exception) {
                base64Clean
            }
            
            val decodedBytes = try {
                android.util.Base64.decode(paddedBase64, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }

            if (decodedBytes == null) {
                val err = if (isFa) "کدگذاری Base64 کانفیگ VMess نامعتبر است." else "Invalid VMess Base64 payload."
                return LinkValidationState.Invalid(err)
            }

            val decodedJson = String(decodedBytes, Charsets.UTF_8)
            val jsonObject = try {
                org.json.JSONObject(decodedJson)
            } catch (e: Exception) {
                null
            }

            if (jsonObject == null) {
                val err = if (isFa) "فرمت JSON کانفیگ VMess نامعتبر است." else "Invalid VMess JSON format."
                return LinkValidationState.Invalid(err)
            }

            host = jsonObject.optString("add", "")
            portStr = jsonObject.optString("port", "443")
            secret = jsonObject.optString("id", "")
            sni = jsonObject.optString("sni", jsonObject.optString("host", "www.google.com"))
            name = jsonObject.optString("ps", if (isFa) "سرور ویمس استخراجی" else "Decoded VMess Node")
            
            if (host.isBlank()) {
                val err = if (isFa) "آدرس سرور (add) در تنظیمات ویمس پیدا نشد." else "Server host IP (add) parameter absent in base64 payload."
                return LinkValidationState.Invalid(err)
            }
            if (secret.isBlank()) {
                val err = if (isFa) "شناسه کاربری (id) یافت نشد." else "User credentials UUID (id) mismatch."
                return LinkValidationState.Invalid(err)
            }
        } 
        else if (displayProtocol == "ShadowSocks") {
            var rawContent = remaining
            if (remaining.contains("#")) {
                val parts = remaining.split("#", limit = 2)
                rawContent = parts[0]
                val decodedName = try {
                    java.net.URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) {
                    parts[1]
                }
                if (decodedName.isNotBlank()) name = decodedName
            }

            if (rawContent.contains("@")) {
                val parts = rawContent.split("@", limit = 2)
                val methodPassB64 = parts[0]
                val hostPort = parts[1]
                
                val decodedCreds = try {
                    val padLen = (4 - methodPassB64.length % 4) % 4
                    val padded = methodPassB64 + "=".repeat(padLen)
                    String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT), Charsets.UTF_8)
                } catch (e: Exception) {
                    methodPassB64
                }
                secret = decodedCreds

                if (hostPort.contains(":")) {
                    val addrParts = hostPort.split(":")
                    host = addrParts[0]
                    portStr = addrParts[1].split("/").firstOrNull() ?: "1080"
                } else {
                    host = hostPort
                    portStr = "1080"
                }
            } 
            else {
                val decodedAll = try {
                    val padLen = (4 - rawContent.length % 4) % 4
                    val padded = rawContent + "=".repeat(padLen)
                    String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT), Charsets.UTF_8)
                } catch (e: Exception) {
                    ""
                }

                if (decodedAll.contains("@")) {
                    val parts = decodedAll.split("@", limit = 2)
                    secret = parts[0]
                    val hostPort = parts[1]
                    if (hostPort.contains(":")) {
                        val addrParts = hostPort.split(":")
                        host = addrParts[0]
                        portStr = addrParts[1].split("/").firstOrNull() ?: "1080"
                    } else {
                        host = hostPort
                        portStr = "1080"
                    }
                } else {
                    val err = if (isFa) "قالب کانفیگ شادوساکس غیراستاندارد است." else "Unable to trace Shadowsocks port or IP from payload."
                    return LinkValidationState.Invalid(err)
                }
            }
        } 
        else {
            var mainContent = remaining
            if (remaining.contains("#")) {
                val parts = remaining.split("#", limit = 2)
                mainContent = parts[0]
                val decodedName = try {
                    java.net.URLDecoder.decode(parts[1], "UTF-8")
                } catch (e: Exception) {
                    parts[1]
                }
                if (decodedName.isNotBlank()) name = decodedName
            }

            if (mainContent.contains("?")) {
                val parts = mainContent.split("?", limit = 2)
                mainContent = parts[0]
                val queryParams = parts[1].split("&")
                for (param in queryParams) {
                    val pair = param.split("=")
                    if (pair.size == 2) {
                        val key = pair[0].lowercase().trim()
                        val value = pair[1].trim()
                        if (key == "sni" || key == "host") {
                            sni = java.net.URLDecoder.decode(value, "UTF-8")
                        }
                    }
                }
            }

            if (mainContent.contains("@")) {
                val parts = mainContent.split("@", limit = 2)
                secret = parts[0]
                val hostPort = parts[1]
                if (hostPort.contains(":")) {
                    val addrParts = hostPort.split(":")
                    host = addrParts[0]
                    portStr = addrParts[1]
                } else {
                    host = hostPort
                    portStr = "443"
                }
            } else {
                val err = if (isFa) "شناسه کاربری کانفیگ یافت نشد (کاراکتر @ مفقود است)." else "Credentials token missing (missing @ splitting symbol)."
                return LinkValidationState.Invalid(err)
            }
        }

        val cleanHost = host.trim()
        if (cleanHost.isBlank()) {
            val err = if (isFa) "آدرس آی‌پی یا دامنه میزبان ناقص است." else "Server IP or Host domain cannot be empty."
            return LinkValidationState.Invalid(err)
        }

        val portVal = portStr.trim().toIntOrNull()
        if (portVal == null || portVal !in 1..65535) {
            val err = if (isFa) "پورت غیراستاندارد است: باید بین ۱ و ۶۵۵۳۵ باشد." else "Invalid Port index (must be 1-65535)."
            return LinkValidationState.Invalid(err)
        }

        val cleanSecret = secret.trim()
        if (cleanSecret.isBlank()) {
            val err = if (isFa) "رمز عبور یا کلید دسترسی نمی‌تواند خالی باشد." else "Credentials token / passkey is empty."
            return LinkValidationState.Invalid(err)
        }

        return LinkValidationState.Valid(
            name = name.trim(),
            host = cleanHost,
            port = portVal,
            protocol = protocol,
            secret = cleanSecret,
            sni = sni.trim(),
            displayProtocol = displayProtocol
        )

    } catch (e: Exception) {
        val err = if (isFa) "خطا در پردازش اطلاعات آدرس: ${e.localizedMessage}" else "Parsing error layout: ${e.localizedMessage}"
        return LinkValidationState.Invalid(err)
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
