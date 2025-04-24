package com.example.appbanlaptop.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.appbanlaptop.Admin.AdminActivity
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
            viewModel.errorMessage.value = "Google Sign-In failed: ${e.message}"
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra trạng thái đăng nhập
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.uid)
            return
        }

        // Khởi tạo Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
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
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let {
                            checkUserRoleAndNavigate(it.uid)
                        } ?: run {
                            Toast.makeText(this, "Login failed: User not found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSignUpClicked = {
                        startActivity(Intent(this, SignUpActivity::class.java))
                        finish()
                    },
                    onGoogleSignInClicked = {
                        signInWithGoogle()
                    },
                    onLogoutClicked = {
                        // Đăng xuất khỏi Firebase
                        FirebaseAuth.getInstance().signOut()
                        // Đăng xuất khỏi Google Sign-In và thu hồi quyền truy cập
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInClient.revokeAccess().addOnCompleteListener {
                                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                // Khởi động lại LoginActivity
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                        }
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
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.let {
                        val database = FirebaseDatabase.getInstance()
                        val userRef = database.getReference("users").child(it.uid)
                        // Kiểm tra xem user đã tồn tại trong database chưa
                        userRef.get().addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()) {
                                // Nếu user chưa tồn tại, tạo mới với role mặc định là "user"
                                val userData = mapOf(
                                    "email" to it.email,
                                    "displayName" to it.displayName,
                                    "createdAt" to System.currentTimeMillis(),
                                    "role" to "user"
                                )
                                userRef.setValue(userData)
                                    .addOnSuccessListener {
                                        viewModel.errorMessage.value = null
                                        checkUserRoleAndNavigate(user.uid)
                                    }
                                    .addOnFailureListener { e ->
                                        viewModel.errorMessage.value = "Failed to save user data: ${e.message}"
                                        Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                        // Vẫn điều hướng để không làm gián đoạn trải nghiệm
                                        checkUserRoleAndNavigate(user.uid)
                                    }
                            } else {
                                // Nếu user đã tồn tại, chỉ cần điều hướng
                                checkUserRoleAndNavigate(user.uid)
                            }
                        }.addOnFailureListener { e ->
                            viewModel.errorMessage.value = "Failed to check user data: ${e.message}"
                            Toast.makeText(this, "Failed to check user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            // Điều hướng mặc định đến MainActivity
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } ?: run {
                        viewModel.errorMessage.value = "User not found after authentication"
                        Toast.makeText(this, "User not found after authentication", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.errorMessage.value = "Authentication failed: ${task.exception?.message}"
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun signInWithGoogle() {
        // Xóa tài khoản Google đã lưu trước đó (nếu có)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun checkUserRoleAndNavigate(userId: String) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val role = snapshot.child("role").getValue(String::class.java) ?: "user"
                if (role == "admin") {
                    Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    Toast.makeText(this, "Welcome User!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            } else {
                // Nếu không tìm thấy user trong database, tạo mới với role mặc định là "user"
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    val userData = mapOf(
                        "email" to it.email,
                        "displayName" to it.displayName,
                        "createdAt" to System.currentTimeMillis(),
                        "role" to "user"
                    )
                    userRef.setValue(userData).addOnSuccessListener {
                        Toast.makeText(this, "Welcome User!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to create user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        // Điều hướng mặc định đến MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }
            }
            finish()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to retrieve user role: ${e.message}", Toast.LENGTH_SHORT).show()
            // Điều hướng mặc định đến MainActivity nếu không lấy được role
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}