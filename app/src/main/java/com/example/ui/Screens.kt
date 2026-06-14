package com.example.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewX
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CallLog
import com.example.data.ChatMessage
import com.example.data.UserProfile
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    LOGIN,
    CREATE_PROFILE,
    DASHBOARD,
    CHAT
}

@Composable
fun AppContent(viewModel: AppViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isProfileCreated by viewModel.isProfileCreated.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }

    // Screen synchronizer
    LaunchedEffect(isLoggedIn, isProfileCreated) {
        currentScreen = if (isLoggedIn) {
            if (isProfileCreated) AppScreen.DASHBOARD else AppScreen.CREATE_PROFILE
        } else {
            AppScreen.LOGIN
        }
    }

    // Active Chat Selection state
    val activeChatId by viewModel.activeChatProfileId.collectAsState()

    // Active Call session overlay
    val activeCall by viewModel.activeCall.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Screens routing
        when (currentScreen) {
            AppScreen.LOGIN -> LoginScreen(viewModel = viewModel)
            AppScreen.CREATE_PROFILE -> CreateProfileScreen(
                viewModel = viewModel,
                onCreated = {
                    currentScreen = AppScreen.DASHBOARD
                }
            )
            AppScreen.DASHBOARD -> DashboardScreen(
                viewModel = viewModel,
                onSelectChat = { profileId ->
                    viewModel.selectChat(profileId)
                    currentScreen = AppScreen.CHAT
                }
            )
            AppScreen.CHAT -> {
                ChatScreen(
                    viewModel = viewModel,
                    onBack = {
                        viewModel.selectChat(null)
                        currentScreen = AppScreen.DASHBOARD
                    }
                )
                BackHandler {
                    viewModel.selectChat(null)
                    currentScreen = AppScreen.DASHBOARD
                }
            }
        }

        // SMS OTP Alert Overlay - Makes testing OTP incredibly fun & intuitive!
        val otpNotification by viewModel.otpNotification.collectAsState()
        notificationOverlay(
            notification = otpNotification,
            onDismiss = { viewModel.dismissNotification() }
        )

        // Calling Screen Fullscreen Overlay
        activeCall?.let { session ->
            CallingScreen(
                session = session,
                onAccept = { viewModel.acceptCall() },
                onDecline = { viewModel.declineCall() },
                onEnd = { viewModel.endCall() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onToggleCamera = { viewModel.toggleCamera() }
            )
        }
    }
}

// --- SMS NOTIFICATION DIALOG ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BoxScope.notificationOverlay(
    notification: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            .statusBarsPadding()
    ) {
        notification?.let { text ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .border(1.dp, CrimsonPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CrimsonPrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "SMS OTP",
                            tint = CrimsonPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OTP Received (Verification)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            color = OnSlateSubText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss SMS Notification",
                            tint = OnSlateSubText
                        )
                    }
                }
            }
        }
    }
}


// --- 1. LOGIN SCREEN ---
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val isOtpMode by viewModel.isOtpMode.collectAsState()
    val isOtpSending by viewModel.isOtpSending.collectAsState()
    val generatedOtp by viewModel.generatedOtp.collectAsState()

    var isVerifying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateBackground, RoseSecondary.copy(alpha = 0.12f), SlateBackground)
                )
            )
            .padding(24.dp)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Main Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(CrimsonPrimary, RoseSecondary)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .background(SlateSurface)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lk_date_logo_1781420207341),
                    contentDescription = "LK Date Modern Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Branding name
            Text(
                text = "LK Date",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "உடன் பேசுங்கள், வீடியோ கால் செய்யுங்கள் 💖",
                fontSize = 14.sp,
                color = OnSlateSubText,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Login inputs container
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isOtpMode) {
                        Text(
                            text = "Login configured with Mobile OTP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Phone text input
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { viewModel.onPhoneNumberChange(it) },
                            label = { Text("Mobile Number (கைபேசி எண்)") },
                            prefix = { Text("+91  ", color = CrimsonPrimary, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone icon", tint = CrimsonPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = SlateSurfaceVariant,
                                focusedLabelColor = CrimsonPrimary,
                                unfocusedLabelColor = OnSlateSubText,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Send OTP button
                        Button(
                            onClick = { viewModel.sendOtp() },
                            enabled = phoneNumber.length == 10 && !isOtpSending,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("send_otp_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = RoseSecondary,
                                disabledContainerColor = CrimsonPrimary.copy(alpha = 0.3f),
                                disabledContentColor = RoseSecondary.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            if (isOtpSending) {
                                CircularProgressIndicator(color = RoseSecondary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "Send OTP (கோட் அனுப்பவும்)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseSecondary
                                )
                            }
                        }
                    } else {
                        // OTP VERIFICATION STATE
                        Text(
                            text = "Verify Mobile Number",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sent 4-digit code to +91 $phoneNumber",
                            fontSize = 13.sp,
                            color = OnSlateSubText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // OTP digital fields
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { viewModel.onOtpChange(it) },
                            label = { Text("Enter 4-Digit OTP") },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = "OTP icon", tint = CrimsonPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = SlateSurfaceVariant,
                                focusedLabelColor = CrimsonPrimary,
                                unfocusedLabelColor = OnSlateSubText,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Verify & Login button
                        Button(
                            onClick = {
                                isVerifying = true
                                if (viewModel.verifyOtp()) {
                                    Toast.makeText(context, "Welcome to LK Date! 😍", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Invalid OTP code, try 1234 or look at the SMS banner", Toast.LENGTH_SHORT).show()
                                }
                                isVerifying = false
                            },
                            enabled = otpCode.length >= 4 && !isVerifying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("verify_otp_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonPrimary,
                                contentColor = RoseSecondary,
                                disabledContainerColor = CrimsonPrimary.copy(alpha = 0.3f),
                                disabledContentColor = RoseSecondary.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = "Verify and Start Dating 💖",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}


// --- 2. DASHBOARD SCREEN ---
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onSelectChat: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Discover, 1: Chats, 2: Contacts, 3: Calls
    var showOwnProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(SlateBackground)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CrimsonPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.lk_date_logo_1781420207341),
                                contentDescription = "Inline logo",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "LK Date",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showOwnProfileDialog = true },
                            modifier = Modifier.testTag("my_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "My Profile",
                                tint = OnSlateSubText,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout account",
                                tint = OnSlateSubText
                            )
                        }
                    }
                }
                
                // Segmented tab selectors with Discover, Chats, Contacts, and Calls
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SlateBackground,
                    contentColor = CrimsonPrimary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                color = CrimsonPrimary,
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Discover Tab",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 0) CrimsonPrimary else OnSlateSubText
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Discover", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Chats Tab",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 1) CrimsonPrimary else OnSlateSubText
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chats", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "Contacts Tab",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 2) CrimsonPrimary else OnSlateSubText
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Contacts", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Default.Call else Icons.Outlined.Call,
                                    contentDescription = "Calls Tab",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == 3) CrimsonPrimary else OnSlateSubText
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Calls", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    )
                }
            }
        },
        containerColor = SlateBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DiscoverTab(viewModel = viewModel, onSelectChat = onSelectChat)
                1 -> ChatsTab(viewModel = viewModel, onSelectChat = onSelectChat)
                2 -> ContactsTab(viewModel = viewModel, onSelectChat = onSelectChat)
                3 -> CallsTab(viewModel = viewModel)
            }
        }
    }

    if (showOwnProfileDialog) {
        OwnProfileDialog(
            viewModel = viewModel,
            onDismiss = { showOwnProfileDialog = false }
        )
    }
}


@Composable
fun DiscoverTab(
    viewModel: AppViewModel,
    onSelectChat: (Int) -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val swipedIds = remember { mutableStateListOf<Int>() }
    var activeMatchProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedProfileForView by remember { mutableStateOf<UserProfile?>(null) }

    val remainingProfiles = remember(profiles, swipedIds.size) {
        profiles.filter { it.id !in swipedIds }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        val filtered = profiles.filter { it.id !in swipedIds }
        if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CrimsonPrimary, RoseSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Catch up icon",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "You've Caught Up! ✨",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No more profiles remaining. Would you like to reset your filters and start again?",
                    fontSize = 14.sp,
                    color = OnSlateSubText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { swipedIds.clear() },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart Browsing", fontWeight = FontWeight.Bold, color = RoseSecondary)
                }
            }
        } else {
            val topTwoProfiles = filtered.take(2).reversed()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    topTwoProfiles.forEachIndexed { index, profile ->
                        val isTopCard = index == topTwoProfiles.lastIndex
                        SwipeCard(
                            profile = profile,
                            isTopCard = isTopCard,
                            onSwiped = { liked ->
                                swipedIds.add(profile.id)
                                if (liked) {
                                    // 75% match chance on swiping right to keep mockup super interactive and rewarding!
                                    if ((1..10).random() <= 7) {
                                        activeMatchProfile = profile
                                        viewModel.addContact(profile.id)
                                    }
                                }
                            },
                            onViewProfile = {
                                selectedProfileForView = profile
                            }
                        )
                    }
                }
            }
        }

        // Fullscreen Match Dialog Overlay
        activeMatchProfile?.let { matchedProfile ->
            MatchOverlay(
                matchedProfile = matchedProfile,
                onDismiss = { activeMatchProfile = null },
                onSendMessage = {
                    activeMatchProfile = null
                    onSelectChat(matchedProfile.id)
                }
            )
        }

        // Interactive Full Profile View of the other contact
        selectedProfileForView?.let { profile ->
            ProfileDetailDialog(
                profile = profile,
                isContact = viewModel.isContact(profile.id),
                onDismiss = { selectedProfileForView = null },
                onMessage = {
                    selectedProfileForView = null
                    onSelectChat(profile.id)
                },
                onAudioCall = {
                    selectedProfileForView = null
                    viewModel.startCall(profile, "AUDIO")
                },
                onVideoCall = {
                    selectedProfileForView = null
                    viewModel.startCall(profile, "VIDEO")
                }
            )
        }
    }
}


@Composable
fun SwipeCard(
    profile: UserProfile,
    isTopCard: Boolean,
    onSwiped: (Boolean) -> Unit,
    onViewProfile: () -> Unit
) {
    val swipeOffsetX = remember { Animatable(0f) }
    val swipeOffsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val cardModifier = if (isTopCard) {
        Modifier
            .pointerInput(profile.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.isNotEmpty()) {
                            val change = changes[0]
                            if (change.pressed) {
                                val positionChange = change.position - change.previousPosition
                                scope.launch {
                                    swipeOffsetX.snapTo(swipeOffsetX.value + positionChange.x)
                                    swipeOffsetY.snapTo(swipeOffsetY.value + positionChange.y)
                                    rotation.snapTo(swipeOffsetX.value * 0.04f)
                                }
                                change.consume()
                            } else {
                                // finger lifted!
                                val threshold = 320f
                                if (swipeOffsetX.value > threshold) {
                                    scope.launch {
                                        swipeOffsetX.animateTo(1200f, tween(250))
                                        onSwiped(true)
                                    }
                                } else if (swipeOffsetX.value < -threshold) {
                                    scope.launch {
                                        swipeOffsetX.animateTo(-1200f, tween(250))
                                        onSwiped(false)
                                    }
                                } else {
                                    scope.launch {
                                        launch { swipeOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                        launch { swipeOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                        launch { rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .graphicsLayer(
                translationX = swipeOffsetX.value,
                translationY = swipeOffsetY.value,
                rotationZ = rotation.value
            )
    } else {
        Modifier
            .scale(0.95f)
            .offset(y = 12.dp)
    }

    Box(
        modifier = cardModifier
            .fillMaxSize()
            .testTag("swipe_card_${profile.id}")
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(CrimsonPrimary.copy(alpha = 0.5f), RoseSecondary.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isTopCard) 8.dp else 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Dating profile avatar for ${profile.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 300f
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${profile.name}, ${profile.age}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        
                        if (profile.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (isTopCard) {
                            IconButton(
                                onClick = onViewProfile,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View Profile Info",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location icon",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${profile.distanceInKm} km away",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSlateSubText
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = profile.bio,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isTopCard) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        swipeOffsetX.animateTo(-1200f, tween(300))
                                        onSwiped(false)
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(SlateSurfaceVariant.copy(alpha = 0.8f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .testTag("discover_swipe_left"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dislike",
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        swipeOffsetX.animateTo(1200f, tween(300))
                                        onSwiped(true)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(SlateBackground.copy(alpha = 0.8f), CircleShape)
                                    .border(1.dp, CrimsonPrimary.copy(alpha = 0.4f), CircleShape)
                                    .testTag("discover_swipe_super"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Super Like",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        swipeOffsetX.animateTo(1200f, tween(300))
                                        onSwiped(true)
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(CrimsonPrimary, CircleShape)
                                    .testTag("discover_swipe_right"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Like",
                                    tint = RoseSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
                
                if (isTopCard) {
                    val swipeRightProgress = (swipeOffsetX.value / 300f).coerceIn(0f, 1f)
                    val swipeLeftProgress = (-swipeOffsetX.value / 300f).coerceIn(0f, 1f)
                    
                    if (swipeRightProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 40.dp, start = 30.dp)
                                .graphicsLayer(rotationZ = -15f)
                                .border(4.dp, Color(0xFF81C784), RoundedCornerShape(12.dp))
                                .background(Color(0xFF81C784).copy(alpha = 0.15f * swipeRightProgress))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "LIKE",
                                color = Color(0xFF81C784),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.scale(swipeRightProgress)
                            )
                        }
                    }
                    
                    if (swipeLeftProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 40.dp, end = 30.dp)
                                .graphicsLayer(rotationZ = 15f)
                                .border(4.dp, Color(0xFFE57373), RoundedCornerShape(12.dp))
                                .background(Color(0xFFE57373).copy(alpha = 0.15f * swipeLeftProgress))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "NOPE",
                                color = Color(0xFFE57373),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.scale(swipeLeftProgress)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MatchOverlay(
    matchedProfile: UserProfile,
    onDismiss: () -> Unit,
    onSendMessage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clickable(enabled = false, onClick = {})
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Match Sparkle",
                tint = CrimsonPrimary,
                modifier = Modifier
                    .size(64.dp)
                    .animateContentSize()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "It's a Match! 😍",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "You and ${matchedProfile.name} liked each other!",
                fontSize = 16.sp,
                color = OnSlateSubText,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush = Brush.linearGradient(colors = listOf(CrimsonPrimary, RoseSecondary)),
                            shape = CircleShape
                        )
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=300",
                        contentDescription = "Your profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.width((-20).dp))
                
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush = Brush.linearGradient(colors = listOf(RoseSecondary, CrimsonPrimary)),
                            shape = CircleShape
                        )
                ) {
                    AsyncImage(
                        model = matchedProfile.avatarUrl,
                        contentDescription = "${matchedProfile.name}'s profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(50.dp))
            
            Button(
                onClick = onSendMessage,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("match_overlay_send_message")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message icon",
                    tint = RoseSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Message Now (மெசேஜ் அனுப்பு)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("match_overlay_keep_browsing")
            ) {
                Text(
                    text = "Keep Swiping (தொடர்ந்து பார்க்கவும்)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


@Composable
fun ChatsTab(
    viewModel: AppViewModel,
    onSelectChat: (Int) -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val contactIds by viewModel.contactIds.collectAsState()
    val matchedProfiles = remember(profiles, contactIds) {
        profiles.filter { it.id in contactIds }
    }

    if (profiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CrimsonPrimary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "உங்களுக்கு பிடித்த நபர்கள் (Matches)",
                    fontSize = 14.sp,
                    color = OnSlateSubText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            if (matchedProfiles.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "இருதரப்பு விருப்பம் இல்லை (No Matches Yet)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Go to Discover and swipe right to make some connections! ✨",
                            fontSize = 12.sp,
                            color = OnSlateSubText,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(matchedProfiles) { profile ->
                    ProfileChatItem(profile = profile, onClick = { onSelectChat(profile.id) })
                }
            }
        }
    }
}

@Composable
fun ProfileChatItem(
    profile: UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("profile_item_${profile.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image bubble
            Box {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, CrimsonPrimary, CircleShape)
                )
                
                // Online indicator badge
                if (profile.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(2.dp, SlateSurface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text detail column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${profile.name}, ${profile.age}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•  ${profile.distanceInKm} km away",
                        fontSize = 11.sp,
                        color = OnSlateSubText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.lastMessageText,
                    fontSize = 13.sp,
                    color = OnSlateSubText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open chat conversation",
                tint = OnSlateSubText.copy(alpha = 0.5f)
            )
        }
    }
}


@Composable
fun CallsTab(viewModel: AppViewModel) {
    val callLogs by viewModel.callLogs.collectAsState()

    if (callLogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Empty calls log",
                    tint = OnSlateSubText.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Call history yet",
                    fontWeight = FontWeight.Bold,
                    color = OnSlateSubText,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap any chat to trigger high-quality simulated Audio or Video calling with active durations!",
                    fontWeight = FontWeight.Medium,
                    color = OnSlateSubText.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "சமீபத்திய அழைப்புகள் (Call History)",
                    fontSize = 14.sp,
                    color = OnSlateSubText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Clear Logs",
                    fontSize = 12.sp,
                    color = CrimsonPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearCallLogs() }
                )
            }

            LazyColumn(
                modifier = Modifier.fillWeightAndWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(callLogs) { log ->
                    CallLogItem(log = log, viewModel = viewModel)
                }
            }
        }
    }
}

private fun Modifier.fillWeightAndWidth() = fillMaxSize()

@Composable
fun CallLogItem(
    log: CallLog,
    viewModel: AppViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val timeFormatted = remember(log.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            AsyncImage(
                model = log.contactAvatarUrl,
                contentDescription = log.contactName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.contactName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon = when (log.status) {
                        "MISSED" -> Icons.Default.CallMissed to Color.Red
                        "REJECTED" -> Icons.Default.Cancel to Color.Red
                        "OUTGOING" -> Icons.Default.CallMade to Color(0xFF4CAF50)
                        else -> Icons.Default.CallReceived to Color(0xFF4CAF50)
                    }
                    Icon(
                        imageVector = statusIcon.first,
                        contentDescription = log.status,
                        tint = statusIcon.second,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${log.callType} call  •  $timeFormatted",
                        fontSize = 11.sp,
                        color = OnSlateSubText
                    )
                }
            }

            // Duration label
            if (log.durationSec > 0) {
                val min = log.durationSec / 60
                val sec = log.durationSec % 60
                val durationText = String.format("%02d:%02d", min, sec)
                Text(
                    text = durationText,
                    fontSize = 12.sp,
                    color = OnSlateSubText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Redial Button action
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val profile = viewModel.profiles.value.find { it.id == log.contactId } ?: UserProfile(
                            id = log.contactId,
                            name = log.contactName,
                            age = 22,
                            bio = "Connecting...",
                            avatarUrl = log.contactAvatarUrl,
                            isOnline = true,
                            distanceInKm = 5
                        )
                        viewModel.startCall(profile, log.callType)
                    }
                }
            ) {
                Icon(
                    imageVector = if (log.callType == "VIDEO") Icons.Default.VideoCall else Icons.Default.Call,
                    contentDescription = "Redial contact",
                    tint = CrimsonPrimary
                )
            }
        }
    }
}


// --- 3. CHAT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeId by viewModel.activeChatProfileId.collectAsState()
    val activeProfile = remember(profiles, activeId) {
        profiles.find { it.id == activeId }
    }

    val messages by viewModel.activeChatMessages.collectAsState(initial = emptyList())
    val typingProfiles by viewModel.typingProfiles.collectAsState()
    val partnerIsTyping = remember(typingProfiles, activeId) {
        activeId != null && activeId in typingProfiles
    }
    var currentText by remember { mutableStateOf("") }

    if (activeProfile == null) {
        onBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { /* can open bio sheet */ },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            AsyncImage(
                                model = activeProfile.avatarUrl,
                                contentDescription = activeProfile.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            )
                            if (activeProfile.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .border(1.5.dp, SlateSurface, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${activeProfile.name}, ${activeProfile.age}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (activeProfile.isOnline) "Mobile active" else "Offline",
                                fontSize = 11.sp,
                                color = OnSlateSubText
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to list",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Audio call trigger
                    IconButton(
                        onClick = { viewModel.startCall(activeProfile, "AUDIO") },
                        modifier = Modifier.testTag("audio_call_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Start Voice Call",
                            tint = CrimsonPrimary
                        )
                    }
                    
                    // Video call trigger
                    IconButton(
                        onClick = { viewModel.startCall(activeProfile, "VIDEO") },
                        modifier = Modifier.testTag("video_call_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Start Video Call",
                            tint = CrimsonPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface)
            )
        },
        containerColor = SlateBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Chat history listing
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = false
            ) {
                // Short safety notice
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔒 Messages are encrypted locally on LK Date. Talk safe.",
                                fontSize = 11.sp,
                                color = OnSlateSubText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                items(messages) { msg ->
                    BubbleMessageItem(msg = msg)
                }

                if (partnerIsTyping) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = activeProfile.avatarUrl,
                                contentDescription = "Typing...",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    TypingDotAnimation()
                                    Text(
                                        text = "typing...",
                                        fontSize = 11.sp,
                                        color = OnSlateSubText.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = SlateSurfaceVariant, thickness = 1.dp)

            // Message compose box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    placeholder = { Text("Type messaging... (மெசேஜ் செய்யவும்)", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonPrimary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SlateBackground,
                        unfocusedContainerColor = SlateBackground,
                        focusedPlaceholderColor = OnSlateSubText,
                        unfocusedPlaceholderColor = OnSlateSubText
                    ),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (currentText.trim().isNotEmpty()) CrimsonPrimary else SlateSurfaceVariant)
                        .clickable(enabled = currentText.trim().isNotEmpty()) {
                            viewModel.sendMessage(currentText)
                            currentText = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun TypingDotAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    @Composable
    fun animateDotAlpha(initialDelay: Int): Float {
        val fraction by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0.2f at initialDelay
                    1f at initialDelay + 300
                    0.2f at initialDelay + 600
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot"
        )
        return fraction
    }

    val alpha1 = animateDotAlpha(0)
    val alpha2 = animateDotAlpha(200)
    val alpha3 = animateDotAlpha(400)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = alpha1)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = alpha2)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = alpha3)))
    }
}


@Composable
fun BubbleMessageItem(msg: ChatMessage) {
    val dateText = remember(msg.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(msg.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (msg.isSentByMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isSentByMe) 16.dp else 4.dp,
                            bottomEnd = if (msg.isSentByMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (msg.isSentByMe) CrimsonPrimary else SlateSurface
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = msg.text,
                    fontSize = 15.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateText,
                fontSize = 10.sp,
                color = OnSlateSubText,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}


// --- 4. CALLING SCREEN (AUDIO / VIDEO) ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallingScreen(
    session: ActiveCallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit
) {
    val durationText = remember(session.durationSec) {
        val min = session.durationSec / 60
        val sec = session.durationSec % 60
        String.format("%02d:%02d", min, sec)
    }

    // Permission handle for real live camera preview
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        if (session.callType == "VIDEO") {
            // BACKDROP STREAMING CONTAINER
            if (session.status == "ONGOING") {
                // Large remote stream simulation
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = session.profile.avatarUrl,
                        contentDescription = "Streaming remote camera",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Semi-transparent overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                    // HD badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 40.dp, start = 20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LIVE HD • ${session.profile.name}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Ringing / Connecting visual
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SlateBackground, Color(0xFF2E0914), SlateBackground)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PulsingRingAvatar(avatarUrl = session.profile.avatarUrl, size = 150)
                    }
                }
            }

            // PIP FLOATING USER PREVIEW WITH CAMERA FEED
            if (session.status == "ONGOING") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 20.dp)
                        .size(width = 110.dp, height = 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    if (session.isCameraOn) {
                        if (cameraPermissionState.status.isGranted) {
                            CameraPreview(modifier = Modifier.fillMaxSize())
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { cameraPermissionState.launchPermissionRequest() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Enable Cam",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Camera Toggled Off placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cam Off", color = OnSlateSubText, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // AUDIO CALL BACKGROUND VIEW WITH LARGE PULSATING CENTRAL PROFILE PICTURE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF330815), SlateBackground, SlateBackground)
                        )
                    )
            ) {
                // Top status description
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LK Date voice connection",
                        fontSize = 12.sp,
                        color = OnSlateSubText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = session.profile.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (session.status == "ONGOING") durationText else session.status.uppercase(),
                        fontSize = 16.sp,
                        color = if (session.status == "ONGOING") Color.Green else RoseSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Audio Waves central visualizer
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PulsingRingAvatar(avatarUrl = session.profile.avatarUrl, size = 140)
                    
                    // Small wave visualizer lines around profile if call is ongoing
                    if (session.status == "ONGOING") {
                        AudioWaveVisualizer()
                    }
                }
            }
        }

        // FLOATING INTERACTIVE CONTROLLER BAR AT THE BOTTOM
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.6.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Description subtitle (e.g. Ringing, Video quality, or Connecting text)
            if (session.callType == "VIDEO") {
                Text(
                    text = if (session.status == "ONGOING") "Secured Video Stream  •  $durationText" else "${session.profile.name}'s video... (${session.status})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .background(SlateSurface.copy(alpha = 0.9f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.status == "RINGING" && session.isIncoming) {
                    // INCOMING RINGING ACTIONS: Accept (Green) or Decline (Red)
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Red, CircleShape)
                            .testTag("decline_call_btn")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline Incoming Call", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .testTag("accept_call_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept Incoming Call", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else {
                    // CONNECTED CALL ACTIONS: Mute, Speaker / Camera, End Call (Hang up)

                    // 1. Mute
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(50.dp)
                            .background(if (session.isMuted) Color.White.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute audio",
                            tint = if (session.isMuted) Color.Red else Color.White
                        )
                    }

                    // 2. Speaker toggle (Voice) or Camera toggling (Video)
                    if (session.callType == "VIDEO") {
                        IconButton(
                            onClick = onToggleCamera,
                            modifier = Modifier
                                .size(50.dp)
                                .background(if (!session.isCameraOn) Color.White.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (session.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Toggle camera preview",
                                tint = if (session.isCameraOn) Color.White else Color.Red
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier
                                .size(50.dp)
                                .background(if (session.isSpeakerOn) Color.White.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (session.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Toggle Speakerphone",
                                tint = if (session.isSpeakerOn) CrimsonPrimary else Color.White
                            )
                        }
                    }

                    // 3. Red End-Call Button
                    IconButton(
                        onClick = onEnd,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Red, CircleShape)
                            .testTag("end_call_btn")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Hang Up call", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun PulsingRingAvatar(avatarUrl: String, size: Int) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = size.toFloat(),
        targetValue = (size + 30).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(pulse.dp)
                .clip(CircleShape)
                .background(CrimsonPrimary.copy(alpha = 0.15f))
        )
        // Inner pulsing ring
        Box(
            modifier = Modifier
                .size((pulse - 15).dp)
                .clip(CircleShape)
                .background(CrimsonPrimary.copy(alpha = 0.25f))
        )
        // Static profile picture inside
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Contact avatar",
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .border(3.dp, CrimsonPrimary, CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}


@Composable
fun AudioWaveVisualizer() {
    val infiniteTransition = rememberInfiniteTransition()
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = 90.dp.toPx()
        val maxWaveHeight = 35.dp.toPx()

        for (i in 0 until 3) {
            val progress = (animationProgress + i / 3f) % 1f
            val waveRadius = radius + progress * maxWaveHeight
            val alpha = 1f - progress

            drawCircle(
                color = CrimsonPrimary.copy(alpha = alpha * 0.4f),
                radius = waveRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}


@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraPreviewX.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
        modifier = modifier
    )
}


// =========================================================================
// --- CUSTOM PROFILE CREATION, CONTACTS LIST & DETAILED PROFILE VIEWS ---
// =========================================================================

@Composable
fun CreateProfileScreen(
    viewModel: AppViewModel,
    onCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedAvatarIndex by remember { mutableStateOf(0) }

    val avatars = listOf(
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300", 
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300", 
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300", 
        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300", 
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300", 
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "உங்கள் சுயவிவரத்தை உருவாக்கவும்",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CrimsonPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Create Your Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Enter details to meet like-minded people nearby!",
            fontSize = 13.sp,
            color = OnSlateSubText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Selected Avatar Bubble with Ring
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(3.dp, CrimsonPrimary, CircleShape)
                .background(SlateSurface)
        ) {
            AsyncImage(
                model = avatars[selectedAvatarIndex],
                contentDescription = "Selected Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "சுயவிவரப் படம் தேர்வு செய்க / Tap to select your avatar",
            fontSize = 12.sp,
            color = OnSlateSubText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Avatar selector - Horizontal grid-like Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            avatars.forEachIndexed { index, avatarUrl ->
                val isSelected = index == selectedAvatarIndex
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .clickable { selectedAvatarIndex = index }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) CrimsonPrimary else OnSlateSubText.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar $index",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Inputs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                Column {
                    Text(
                        text = "பெயர் / Full Name",
                        fontSize = 12.sp,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g., Harish Kumar", color = OnSlateSubText.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = OnSlateSubText.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Age
                Column {
                    Text(
                        text = "வயது / Age",
                        fontSize = 12.sp,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) age = it },
                        placeholder = { Text("e.g., 24", color = OnSlateSubText.copy(alpha = 0.5f)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("profile_age_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = OnSlateSubText.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Bio
                Column {
                    Text(
                        text = "சுயவிவர குறிப்பு / Bio",
                        fontSize = 12.sp,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        placeholder = { Text("Tell other matches about you...", color = OnSlateSubText.copy(alpha = 0.5f)) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("profile_bio_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = OnSlateSubText.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.trim().isNotEmpty() && age.trim().isNotEmpty() && bio.trim().isNotEmpty()) {
                    viewModel.saveOwnProfile(
                        name = name.trim(),
                        age = age.trim(),
                        bio = bio.trim(),
                        avatar = avatars[selectedAvatarIndex]
                    )
                    onCreated()
                }
            },
            enabled = name.trim().isNotEmpty() && age.trim().isNotEmpty() && bio.trim().isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_profile_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CrimsonPrimary,
                disabledContainerColor = CrimsonPrimary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text(
                text = "சேமி & தொடரு / Save & Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun OwnProfileDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val ownName by viewModel.ownName.collectAsState()
    val ownAge by viewModel.ownAge.collectAsState()
    val ownBio by viewModel.ownBio.collectAsState()
    val ownAvatarUrl by viewModel.ownAvatarUrl.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(ownName) }
    var editAge by remember { mutableStateOf(ownAge) }
    var editBio by remember { mutableStateOf(ownBio) }
    var editAvatarUrl by remember { mutableStateOf(ownAvatarUrl) }

    val avatars = listOf(
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = SlateSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "விவரங்களை மாற்றுக / Edit Profile" else "எனது சுயவிவரம் / My Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialog", tint = OnSlateSubText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Profile Image representation
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, CrimsonPrimary, CircleShape)
                        .background(SlateBackground)
                ) {
                    AsyncImage(
                        model = editAvatarUrl,
                        contentDescription = "My avatar picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isEditing) {
                    // --- PROFILE VIEW MODE ---
                    Text(
                        text = "$ownName, $ownAge",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Verified Account • Active",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "சுயவிவர குறிப்பு / ABOUT ME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SlateBackground,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = ownBio,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("edit_profile_trigger"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("சுயவிவரத்தை திருத்தவும் / Edit Profile", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // --- PROFILE EDIT MODE ---
                    Text(
                        text = "சுயவிவரப் படம் மாற்றுக / Tap avatar to swap",
                        fontSize = 12.sp,
                        color = OnSlateSubText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Quick list of avatars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        avatars.forEach { url ->
                            val isSelected = editAvatarUrl == url
                            Box(
                                modifier = Modifier
                                    .size(35.dp)
                                    .clip(CircleShape)
                                    .clickable { editAvatarUrl = url }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) CrimsonPrimary else OnSlateSubText.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Avatar Options",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Input Form Fields
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name", color = CrimsonPrimary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = OnSlateSubText.copy(alpha = 0.3f)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_field")
                        )

                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) editAge = it },
                            label = { Text("Age", color = CrimsonPrimary) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = OnSlateSubText.copy(alpha = 0.3f)
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_age_field")
                        )

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio / About Me", color = CrimsonPrimary) },
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = OnSlateSubText.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_bio_field")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSlateSubText),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OnSlateSubText.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (editName.trim().isNotEmpty() && editAge.trim().isNotEmpty() && editBio.trim().isNotEmpty()) {
                                    viewModel.saveOwnProfile(
                                        name = editName.trim(),
                                        age = editAge.trim(),
                                        bio = editBio.trim(),
                                        avatar = editAvatarUrl
                                    )
                                    isEditing = false
                                }
                            },
                            enabled = editName.trim().isNotEmpty() && editAge.trim().isNotEmpty() && editBio.trim().isNotEmpty(),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("save_edit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsTab(
    viewModel: AppViewModel,
    onSelectChat: (Int) -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val contactIds by viewModel.contactIds.collectAsState()
    val contactProfiles = remember(profiles, contactIds) {
        profiles.filter { it.id in contactIds }
    }
    var selectedProfileForView by remember { mutableStateOf<UserProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        // Title banner
        Text(
            text = "தொடர்புகள் (Dating Contacts)",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CrimsonPrimary)
            }
        } else {
            if (contactProfiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "No contacts",
                            tint = OnSlateSubText,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "விவரங்கள் எதுவும் இல்லை (No Contacts Yet)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Swipe right in Discover to matched partners! They will appear here immediately once a match is created.",
                            fontSize = 12.sp,
                            color = OnSlateSubText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contactProfiles) { profile ->
                        ContactRowItem(
                            profile = profile,
                            onViewProfile = { selectedProfileForView = profile },
                            onMessage = { onSelectChat(profile.id) },
                            onAudioCall = { viewModel.startCall(profile, "AUDIO") },
                            onVideoCall = { viewModel.startCall(profile, "VIDEO") }
                        )
                    }
                }
            }
        }
    }

    // Interactive Full Profile View of the other contact
    selectedProfileForView?.let { profile ->
        ProfileDetailDialog(
            profile = profile,
            isContact = viewModel.isContact(profile.id),
            onDismiss = { selectedProfileForView = null },
            onMessage = {
                selectedProfileForView = null
                onSelectChat(profile.id)
            },
            onAudioCall = {
                selectedProfileForView = null
                viewModel.startCall(profile, "AUDIO")
            },
            onVideoCall = {
                selectedProfileForView = null
                viewModel.startCall(profile, "VIDEO")
            }
        )
    }
}

@Composable
fun ContactRowItem(
    profile: UserProfile,
    onViewProfile: () -> Unit,
    onMessage: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() },
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left - Avatar Representation
            Box(modifier = Modifier.size(54.dp)) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Active status dot indicator
                if (profile.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .border(2.dp, SlateSurface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center - Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = ", ${profile.age}",
                        fontSize = 15.sp,
                        color = OnSlateSubText,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = profile.lastMessageText,
                    fontSize = 12.sp,
                    color = OnSlateSubText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Right - Mini Row of actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onMessage,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SlateBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Message",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onAudioCall,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SlateBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onVideoCall,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SlateBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Videocam Call",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetailDialog(
    profile: UserProfile,
    isContact: Boolean,
    onDismiss: () -> Unit,
    onMessage: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = SlateSurface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    // Immersive Photo Header Representation
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "${profile.name}'s picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Vignette darkness for legibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                        startY = 150f
                                    )
                                )
                        )

                        // Floating dismiss button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                            .padding(12.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Profile", tint = Color.White)
                        }

                        // Bottom-placed overlay name details
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${profile.name}, ${profile.age}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Online dot indicator
                                if (profile.isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                    text = "📍 ${profile.distanceInKm} km away • ${if (profile.isOnline) "தற்போது ஆன்லைனில் / Online Now" else "ஆஃப்லைன் / Away"}",
                                    fontSize = 12.sp,
                                    color = OnSlateSubText,
                                    fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Content Padding Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "சுயவிவர அறிமுகம் / ABOUT ME",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SlateBackground,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = profile.bio,
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "தொடர்பு கொள்ள அணுகவும் / GET IN TOUCH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isContact) {
                            // Connect Actions row: MSG, AUDIO, VIDEO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Button(
                                    onClick = onMessage,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("prof_dialog_message"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateBackground),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ChatBubble, contentDescription = "Message", tint = CrimsonPrimary)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = onAudioCall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("prof_dialog_audio"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Button(
                                    onClick = onVideoCall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("prof_dialog_video"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CrimsonPrimary.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked actions",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "இருதரப்பு விருப்பம் தேவை (Match Required)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Swipe right in Discover to matched partners! You can only chat or call contacts.",
                                            fontSize = 11.sp,
                                            color = OnSlateSubText,
                                            lineHeight = 15.sp
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
