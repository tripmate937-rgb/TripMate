package uk.ac.tees.mad.tripmate.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel() : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun checkAuthStatus(): Boolean {
        return auth.currentUser != null
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _authState.value = AuthState.Loading

                if (email.isBlank() || password.isBlank()) {
                    _authState.value = AuthState.Error("Email and password cannot be empty")
                    _isLoading.value = false
                    return@launch
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    _authState.value = AuthState.Error("Invalid email format")
                    _isLoading.value = false
                    return@launch
                }

                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
                _isLoading.value = false
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _authState.value = AuthState.Loading

                if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    _authState.value = AuthState.Error("All fields are required")
                    _isLoading.value = false
                    return@launch
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    _authState.value = AuthState.Error("Invalid email format")
                    _isLoading.value = false
                    return@launch
                }

                if (password.length < 6) {
                    _authState.value = AuthState.Error("Password must be at least 6 characters")
                    _isLoading.value = false
                    return@launch
                }

                if (password != confirmPassword) {
                    _authState.value = AuthState.Error("Passwords do not match")
                    _isLoading.value = false
                    return@launch
                }

                auth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success
                _isLoading.value = false
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
                _isLoading.value = false
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}