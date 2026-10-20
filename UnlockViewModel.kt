package com.example.pcunlocker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow"
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val message: String) : UiState
}

class UnlockViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("pc_unlocker_prefs", Context.MODE_PRIVATE)
    private val unlockClient = UnlockClient()

    // Saved IP Address
    private val _ipAddress = MutableStateFlow(prefs.getString("saved_ip", "") ?: "")
    val ipAddress = _ipAddress.asStateFlow()

    // Saved Secret Key
    private val _secretKey = MutableStateFlow(prefs.getString("saved_secret", "MySuperSecretKey123") ?: "MySuperSecretKey123")
    val secretKey = _secretKey.asStateFlow()

    // Last Unlock History/Log
    private val _lastUnlockLog = MutableStateFlow(prefs.getString("last_log", "No history yet") ?: "No history yet")
    val lastUnlockLog = _lastUnlockLog.asStateFlow()

    // UI States
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Biometric Trigger Event
    private val _triggerBiometrics = MutableSharedFlow<Unit>()
    val triggerBiometrics = _triggerBiometrics.asSharedFlow()

    fun saveSettings(ip: String, secret: String) {
        prefs.edit()
            .putString("saved_ip", ip.trim())
            .putString("saved_secret", secret.trim())
            .apply()
        _ipAddress.value = ip.trim()
        _secretKey.value = secret.trim()
        _uiState.value = UiState.Success("Settings saved successfully!")
    }

    fun onUnlockClicked() {
        if (_ipAddress.value.isBlank()) {
            _uiState.value = UiState.Error("Please set and save a valid IP address first.")
            return
        }
        viewModelScope.launch {
            _triggerBiometrics.emit(Unit)
        }
    }

    fun executeUnlockRequest() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = unlockClient.sendUnlockRequest(_ipAddress.value, _secretKey.value)
            result.fold(
                onSuccess = { message ->
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val logMessage = "Unlocked at $timestamp"
                    prefs.edit().putString("last_log", logMessage).apply()
                    _lastUnlockLog.value = logMessage
                    _uiState.value = UiState.Success(message)
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unknown Error")
                }
            )
        }
    }

    fun dismissState() {
        _uiState.value = UiState.Idle
    }
}