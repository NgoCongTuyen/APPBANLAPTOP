package com.example.appbanlaptop.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.appbanlaptop.MainActivity
import com.example.appbanlaptop.R
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var viewModel: LoginViewModel

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("GoogleSignInError", "Google Sign-In failed: ${e.statusCode} - ${e.message}", e)
            viewModel.errorMessage.value = "Google Sign-In failed: ${e.statusCode} - ${e.message}"        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("312176967841-vi09lepvd4htvomtt475t5krk8qhd71b.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        setContent {
            APPBANLAPTOPTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onSignUpClicked = {
                        startActivity(Intent(this, SignUpActivity::class.java))
                        finish()
                    },
                    onGoogleSignInClicked = {
                        signInWithGoogle()
                    }
                )
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Google Sign-In successful")
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.let {
                        // Lưu thông tin người dùng vào Realtime Database
                        val database = FirebaseDatabase.getInstance()
                        val userRef = database.getReference("users").child(it.uid)
                        val userData = mapOf(
                            "email" to it.email,
                            "displayName" to it.displayName,
                            "createdAt" to System.currentTimeMillis()
                        )
                        userRef.setValue(userData)
                            .addOnSuccessListener {
                                viewModel.errorMessage.value = null
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                viewModel.errorMessage.value = "Failed to save user data: ${e.message}"
                            }
                    }
                } else {
                    Log.e("FirebaseAuth", "Authentication failed: ${task.exception?.message}", task.exception)
                    viewModel.errorMessage.value = "Authentication failed: ${task.exception?.message}"                }
            }
    }

    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
}