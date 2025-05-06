package com.example.appbanlaptop.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.appbanlaptop.R
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date

// Theme Manager to handle app-wide theme state
object ThemeManager {
    private const val PREFS_NAME = "AppPreferences"
    private const val KEY_DARK_MODE = "isDarkMode"

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, isDarkMode: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean(KEY_DARK_MODE, isDarkMode)
            apply()
        }
    }
}

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Load theme from ThemeManager
            val isDarkMode = remember { mutableStateOf(ThemeManager.isDarkMode(this)) }

            // Update SharedPreferences when theme changes
            LaunchedEffect(isDarkMode.value) {
                ThemeManager.setDarkMode(this@ProfileActivity, isDarkMode.value)
            }

            APPBANLAPTOPTheme(darkTheme = isDarkMode.value) {
                ProfileScreen(
                    onLogoutClicked = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(this, gso)

                        FirebaseAuth.getInstance().signOut()
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInClient.revokeAccess().addOnCompleteListener {
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    },
                    onBackClick = {
                        finish()
                    },
                    isDarkMode = isDarkMode.value,
                    onThemeToggle = { isDarkMode.value = !isDarkMode.value }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutClicked: () -> Unit,
    onBackClick: () -> Unit = {},
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val email = currentUser?.email ?: "No email"
    val photoUrl = currentUser?.photoUrl?.toString()

    // Khởi tạo username với displayName hoặc "User"
    val username = remember { mutableStateOf(currentUser?.displayName ?: "User") }
    val createdAt = remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        val userId = currentUser?.uid
        if (userId != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("Snapshot: ${snapshot.value}")
                    val dbUsername = snapshot.child("username").getValue(String::class.java)
                    println("Username from DB: $dbUsername")
                    if (!dbUsername.isNullOrEmpty()) {
                        username.value = dbUsername
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Firebase Error: ${error.message}")
                }
            })
        } else {
            println("No user logged in")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thông tin",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF121212) else Color.White
                )
            )
        },
        bottomBar = {
            BottomActivity.BottomMenu(onItemClick = {})
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5))
                .padding(24.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color.DarkGray else Color.LightGray)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.star),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color.DarkGray else Color.LightGray)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = username.value,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = email,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkMode) Color.LightGray else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Card 2: Thông tin bổ sung (ngày tham gia, số đơn hàng)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Thông tin tài khoản",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        createdAt.value?.let { timestamp ->
                            Text(
                                text = "Ngày tham gia: ${SimpleDateFormat("dd/MM/yyyy").format(Date(timestamp))}",
                                fontSize = 16.sp,
                                color = if (isDarkMode) Color.LightGray else Color.Gray
                            )
                        } ?: Text(
                            text = "Ngày tham gia: ...",
                            fontSize = 16.sp,
                            color = if (isDarkMode) Color.LightGray else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Số đơn hàng: ...",
                            fontSize = 16.sp,
                            color = if (isDarkMode) Color.LightGray else Color.Gray
                        )
                    }
                }
            }

            // Card 3: Cài đặt (bao gồm nút chuyển dark/light mode)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Cài đặt",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Thông báo: đã bật",
                            fontSize = 16.sp,
                            color = if (isDarkMode) Color.LightGray else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chế độ tối",
                                fontSize = 16.sp,
                                color = if (isDarkMode) Color.LightGray else Color.Gray
                            )
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onThemeToggle() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF6200EE),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray
                                )
                            )
                        }
                    }
                }
            }

            // Card 4: Nút đăng xuất
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(8.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var isLoggingOut by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                isLoggingOut = true
                                onLogoutClicked()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(4.dp, shape = RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF5350),
                                contentColor = Color.White
                            ),
                            enabled = !isLoggingOut
                        ) {
                            if (isLoggingOut) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = "Đăng xuất",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}