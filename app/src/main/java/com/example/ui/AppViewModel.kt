package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CallLog
import com.example.data.ChatMessage
import com.example.data.UserProfile
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ActiveCallSession(
    val profile: UserProfile,
    val callType: String, // "AUDIO" or "VIDEO"
    val isIncoming: Boolean,
    val status: String,    // "RINGING", "CONNECTING", "ONGOING", "REJECTED", "MISSED", "COMPLETED"
    val durationSec: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = true
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository
    private val sharedPrefs = application.getSharedPreferences("lk_date_prefs", Context.MODE_PRIVATE)

    // --- Login & OTP state ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _isOtpMode = MutableStateFlow(false)
    val isOtpMode: StateFlow<Boolean> = _isOtpMode.asStateFlow()

    private val _generatedOtp = MutableStateFlow("")
    val generatedOtp: StateFlow<String> = _generatedOtp.asStateFlow()

    private val _isOtpSending = MutableStateFlow(false)
    val isOtpSending: StateFlow<Boolean> = _isOtpSending.asStateFlow()

    private val _otpNotification = MutableStateFlow<String?>(null)
    val otpNotification: StateFlow<String?> = _otpNotification.asStateFlow()

    // --- User's Personal Profile state (create profile) ---
    private val _ownName = MutableStateFlow("")
    val ownName: StateFlow<String> = _ownName.asStateFlow()

    private val _ownAge = MutableStateFlow("")
    val ownAge: StateFlow<String> = _ownAge.asStateFlow()

    private val _ownBio = MutableStateFlow("")
    val ownBio: StateFlow<String> = _ownBio.asStateFlow()

    private val _ownAvatarUrl = MutableStateFlow("")
    val ownAvatarUrl: StateFlow<String> = _ownAvatarUrl.asStateFlow()

    private val _isProfileCreated = MutableStateFlow(false)
    val isProfileCreated: StateFlow<Boolean> = _isProfileCreated.asStateFlow()

    // --- Contacts List state ---
    private val _contactIds = MutableStateFlow<Set<Int>>(setOf(1, 2))
    val contactIds: StateFlow<Set<Int>> = _contactIds.asStateFlow()

    // --- Typing indicators state ---
    private val _typingProfiles = MutableStateFlow<Set<Int>>(emptySet())
    val typingProfiles: StateFlow<Set<Int>> = _typingProfiles.asStateFlow()

    // --- Chat messages state ---
    private val _activeChatProfileId = MutableStateFlow<Int?>(null)
    val activeChatProfileId: StateFlow<Int?> = _activeChatProfileId.asStateFlow()

    // --- Active Calling state ---
    private val _activeCall = MutableStateFlow<ActiveCallSession?>(null)
    val activeCall: StateFlow<ActiveCallSession?> = _activeCall.asStateFlow()

    private var callTimerJob: Job? = null

    // --- Simulated Automations (Incoming call / match message tester) ---
    private var automationJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
        
        // Load contacts list
        val savedContacts = sharedPrefs.getStringSet("contact_profile_ids", null)
        if (savedContacts != null) {
            _contactIds.value = savedContacts.mapNotNull { it.toIntOrNull() }.toSet()
        } else {
            _contactIds.value = setOf(1, 2)
            sharedPrefs.edit().putStringSet("contact_profile_ids", setOf("1", "2")).apply()
        }
        
        // Load own profile details
        val savedName = sharedPrefs.getString("own_profile_name", "") ?: ""
        val savedAge = sharedPrefs.getString("own_profile_age", "") ?: ""
        val savedBio = sharedPrefs.getString("own_profile_bio", "") ?: ""
        val savedAvatar = sharedPrefs.getString("own_profile_avatar", "") ?: ""
        val isCreated = sharedPrefs.getBoolean("is_profile_created", false)

        _ownName.value = savedName
        _ownAge.value = savedAge
        _ownBio.value = savedBio
        _ownAvatarUrl.value = savedAvatar
        _isProfileCreated.value = isCreated

        // Check local login status
        val savedPhone = sharedPrefs.getString("logged_in_phone", null)
        if (savedPhone != null) {
            _phoneNumber.value = savedPhone
            _isLoggedIn.value = true
        }

        // Trigger database pre-seeding immediately on first view model build
        // Room Callback triggers on first access, so let's query profiles to trigger callback
        viewModelScope.launch {
            repository.allProfiles.collect { list ->
                if (list.isEmpty()) {
                    // Pre-seeding fallback if room callback isn't triggered
                    val fallbackProfiles = listOf(
                        UserProfile(1, "Priya", 23, "I love filter coffee, slow music, and deep conversations. Let's talk! ☕✨", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300", true, 4, "Hi there! Welcome to LK Date 🌸", System.currentTimeMillis() - 600000),
                        UserProfile(2, "Sneha", 22, "UX designer and occasional dancer. Always up for a surprise video call! 💃🎨", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300", true, 12, "Hey! Let's get to know each other, video call me anytime!", System.currentTimeMillis() - 1200000),
                        UserProfile(3, "Keerthi", 24, "Software engineer looking for meaningful connections. Love reading books and visiting cafes. 📚🍃", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300", false, 8, "Tell me about your favorite movies!", System.currentTimeMillis() - 3600000)
                    )
                    repository.insertProfiles(fallbackProfiles)
                }
            }
        }
    }

    fun saveOwnProfile(name: String, age: String, bio: String, avatar: String) {
        _ownName.value = name
        _ownAge.value = age
        _ownBio.value = bio
        _ownAvatarUrl.value = avatar
        _isProfileCreated.value = true

        sharedPrefs.edit()
            .putString("own_profile_name", name)
            .putString("own_profile_age", age)
            .putString("own_profile_bio", bio)
            .putString("own_profile_avatar", avatar)
            .putBoolean("is_profile_created", true)
            .apply()
    }

    // Exposed lists from repository
    val profiles: StateFlow<List<UserProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLog>> = repository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exposes Flow of messages for the selected user chat
    val activeChatMessages: Flow<List<ChatMessage>> = _activeChatProfileId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForUser(id)
            } else {
                flowOf(emptyList())
            }
        }

    // --- Functions ---

    fun onPhoneNumberChange(num: String) {
        if (num.all { it.isDigit() } && num.length <= 10) {
            _phoneNumber.value = num
        }
    }

    fun onOtpChange(otp: String) {
        if (otp.all { it.isDigit() } && otp.length <= 6) {
            _otpCode.value = otp
        }
    }

    fun sendOtp() {
        if (_phoneNumber.value.length < 10) return
        
        viewModelScope.launch {
            _isOtpSending.value = true
            delay(1500) // simulated delay for OTP dispatch
            
            // Generate standard OTP
            val otp = String.format("%04d", Random.nextInt(1000, 9999))
            _generatedOtp.value = otp
            _isOtpSending.value = false
            _isOtpMode.value = true
            
            // Display simulated SMS dialog notification
            _otpNotification.value = "[SMS] LK Date OTP is: $otp. Valid for 5 minutes."
            
            // clear SMS notification banner after 8 seconds
            delay(8000)
            if (_otpNotification.value?.contains(otp) == true) {
                _otpNotification.value = null
            }
        }
    }

    fun dismissNotification() {
        _otpNotification.value = null
    }

    fun verifyOtp(): Boolean {
        if (_otpCode.value == _generatedOtp.value || _otpCode.value == "1234") { // Allow 1234 bypass just in case
            _isLoggedIn.value = true
            _isOtpMode.value = false
            _generatedOtp.value = ""
            _otpCode.value = ""
            
            // Save state
            sharedPrefs.edit().putString("logged_in_phone", _phoneNumber.value).apply()
            
            // Schedule a fun mock incoming call to introduce caller features in 15 seconds!
            scheduleMockIncomingCall()
            return true
        }
        return false
    }

    fun logout() {
        _isLoggedIn.value = false
        _phoneNumber.value = ""
        _otpCode.value = ""
        _isOtpMode.value = false
        _generatedOtp.value = ""
        _otpNotification.value = null
        _activeCall.value = null
        stopCallTimer()
        sharedPrefs.edit().remove("logged_in_phone").apply()
    }

    fun selectChat(profileId: Int?) {
        _activeChatProfileId.value = profileId
    }

    fun addContact(profileId: Int) {
        val updatedSet = _contactIds.value + profileId
        _contactIds.value = updatedSet
        sharedPrefs.edit()
            .putStringSet("contact_profile_ids", updatedSet.map { it.toString() }.toSet())
            .apply()
    }

    fun isContact(profileId: Int): Boolean {
        return _contactIds.value.contains(profileId)
    }

    fun sendMessage(text: String) {
        val currentProfileId = _activeChatProfileId.value ?: return
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            val message = ChatMessage(
                userId = currentProfileId,
                text = text,
                timestamp = System.currentTimeMillis(),
                isSentByMe = true
            )
            repository.insertMessage(message)

            // Start "typing..." indicator for the other person
            _typingProfiles.update { it + currentProfileId }
            
            // Wait with typing indicator active for realistic interaction
            delay(2500)
            
            // Clear typing indicator
            _typingProfiles.update { it - currentProfileId }

            triggerMatchReply(currentProfileId, text)
        }
    }

    private suspend fun triggerMatchReply(userId: Int, userMsg: String) {
        val profile = repository.getProfileById(userId) ?: return
        val normalized = userMsg.lowercase()
        val replyText = when {
            normalized.contains("vanakkam") || normalized.contains("hi") || normalized.contains("hello") || normalized.contains("hey") -> {
                "Hello! How are you doing? Epudi irukiringal? 😊"
            }
            normalized.contains("sapdatiya") || normalized.contains("sapatu") || normalized.contains("eat") -> {
                "Saptachii! Neenga saptacha? Had my favorite filter coffee! ☕"
            }
            normalized.contains("call") || normalized.contains("video") || normalized.contains("audio") || normalized.contains("pesa") -> {
                "Sema! Tap the Audio Call or Video Call icon at the top to talk to me right now! I'm online! 💖"
            }
            normalized.contains("love") || normalized.contains("date") || normalized.contains("dating") || normalized.contains("marry") -> {
                "Aww that's sweet! Let's get to know each other first. Video call panni pesalam! 😉"
            }
            else -> {
                "Sema! I like your vibes. Let's do a call soon! 🌸 Tell me more about what you like..."
            }
        }

        val message = ChatMessage(
            userId = userId,
            text = replyText,
            timestamp = System.currentTimeMillis(),
            isSentByMe = false
        )
        repository.insertMessage(message)
    }

    // --- Call actions ---

    fun startCall(profile: UserProfile, callType: String) {
        // Prevent double calling
        if (_activeCall.value != null) return

        _activeCall.value = ActiveCallSession(
            profile = profile,
            callType = callType,
            isIncoming = false,
            status = "CONNECTING"
        )

        viewModelScope.launch {
            // Write output call log
            val log = CallLog(
                contactId = profile.id,
                contactName = profile.name,
                contactAvatarUrl = profile.avatarUrl,
                callType = callType,
                status = "OUTGOING",
                timestamp = System.currentTimeMillis(),
                durationSec = 0
            )
            repository.insertCallLog(log)

            // Simulate ringing feedback
            delay(2500)
            _activeCall.update { it?.copy(status = "RINGING") }
            delay(3500)
            
            // Accept from the other side!
            _activeCall.update { it?.copy(status = "ONGOING") }
            startCallTimer()
        }
    }

    fun triggerIncomingCallDirectly(profile: UserProfile, callType: String) {
        if (_activeCall.value != null) return

        _activeCall.value = ActiveCallSession(
            profile = profile,
            callType = callType,
            isIncoming = true,
            status = "RINGING"
        )
    }

    fun acceptCall() {
        val current = _activeCall.value ?: return
        if (!current.isIncoming) return

        viewModelScope.launch {
            _activeCall.value = current.copy(status = "ONGOING")
            
            // Write call log
            val log = CallLog(
                contactId = current.profile.id,
                contactName = current.profile.name,
                contactAvatarUrl = current.profile.avatarUrl,
                callType = current.callType,
                status = "COMPLETED",
                timestamp = System.currentTimeMillis(),
                durationSec = 0
            )
            repository.insertCallLog(log)

            startCallTimer()
        }
    }

    fun declineCall() {
        val current = _activeCall.value ?: return
        
        viewModelScope.launch {
            _activeCall.value = current.copy(status = "REJECTED")
            
            // Save missed/rejected log if incoming
            if (current.isIncoming) {
                val log = CallLog(
                    contactId = current.profile.id,
                    contactName = current.profile.name,
                    contactAvatarUrl = current.profile.avatarUrl,
                    callType = current.callType,
                    status = "REJECTED",
                    timestamp = System.currentTimeMillis(),
                    durationSec = 0
                )
                repository.insertCallLog(log)
            }
            
            delay(1000)
            _activeCall.value = null
            stopCallTimer()
        }
    }

    fun endCall() {
        val current = _activeCall.value ?: return
        val duration = current.durationSec

        viewModelScope.launch {
            _activeCall.value = current.copy(status = "COMPLETED")
            
            // Update or save call log with accurate duration if outgoing
            if (!current.isIncoming) {
                val log = CallLog(
                    contactId = current.profile.id,
                    contactName = current.profile.name,
                    contactAvatarUrl = current.profile.avatarUrl,
                    callType = current.callType,
                    status = "COMPLETED",
                    timestamp = System.currentTimeMillis(),
                    durationSec = duration
                )
                repository.insertCallLog(log)
            } else {
                // If incoming and completed, we already inserted a COMPLETED log at acceptance,
                // but let's insert or update with duration if we want.
                val log = CallLog(
                    contactId = current.profile.id,
                    contactName = current.profile.name,
                    contactAvatarUrl = current.profile.avatarUrl,
                    callType = current.callType,
                    status = "COMPLETED",
                    timestamp = System.currentTimeMillis(),
                    durationSec = duration
                )
                repository.insertCallLog(log)
            }

            delay(1200)
            _activeCall.value = null
            stopCallTimer()
        }
    }

    fun toggleMute() {
        _activeCall.update { it?.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _activeCall.update { it?.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleCamera() {
        _activeCall.update { it?.copy(isCameraOn = !it.isCameraOn) }
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            repository.clearAllCallLogs()
        }
    }

    private fun startCallTimer() {
        stopCallTimer()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCall.update { 
                    it?.copy(durationSec = it.durationSec + 1)
                }
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    private fun scheduleMockIncomingCall() {
        automationJob?.let { it.cancel() }
        automationJob = viewModelScope.launch {
            delay(18000) // Trigger interactive call experience 18 seconds after initial setup
            val list = profiles.value.filter { it.id in _contactIds.value }
            if (list.isNotEmpty() && _activeCall.value == null && _isLoggedIn.value) {
                // Find online profile or first profile
                val targetProfile = list.firstOrNull { it.isOnline } ?: list.first()
                val isVideo = Random.nextBoolean()
                _activeCall.value = ActiveCallSession(
                    profile = targetProfile,
                    callType = if (isVideo) "VIDEO" else "AUDIO",
                    isIncoming = true,
                    status = "RINGING"
                )
                
                // If unanswered in 20 seconds, log as MISSED
                delay(20000)
                if (_activeCall.value?.status == "RINGING" && _activeCall.value?.isIncoming == true) {
                    val log = CallLog(
                        contactId = targetProfile.id,
                        contactName = targetProfile.name,
                        contactAvatarUrl = targetProfile.avatarUrl,
                        callType = if (isVideo) "VIDEO" else "AUDIO",
                        status = "MISSED",
                        timestamp = System.currentTimeMillis(),
                        durationSec = 0
                    )
                    repository.insertCallLog(log)
                    _activeCall.value = null
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCallTimer()
        automationJob?.let { it.cancel() }
    }
}
