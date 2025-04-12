package com.example.appbanlaptop.Activity

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel() {
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val passwordVisible = mutableStateOf(false)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    private val auth = FirebaseAuth.getInstance()

    fun onLoginClicked(onLoginSuccess: () -> Unit) {
        isLoading.value = true
        errorMessage.value = null

        auth.signInWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    errorMessage.value = task.exception?.message ?: "Login failed"
                }
            }
    }

    // Callback để thông báo khi nhấn "Sign Up"
    fun onSignUpClicked(onSignUpClicked: () -> Unit) {
        onSignUpClicked()
    }

    fun onForgetPasswordClicked() {
        if (email.value.isNotEmpty()) {
            auth.sendPasswordResetEmail(email.value)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        errorMessage.value = "Password reset email sent"
                    } else {
                        errorMessage.value = task.exception?.message ?: "Failed to send reset email"
                    }
                }
        } else {
            errorMessage.value = "Please enter your email"
        }
    }

    // Callback để thông báo khi nhấn đăng nhập bằng Google
    fun onSocialLoginClicked(provider: String, onGoogleSignInClicked: () -> Unit) {
        if (provider == "Google") {
            onGoogleSignInClicked()
        }
    }
}