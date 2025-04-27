package com.example.appbanlaptop.Admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.appbanlaptop.Activity.AdminProductScreen
import com.example.appbanlaptop.Activity.LoginActivity
import com.example.appbanlaptop.Model.*
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContent {
            APPBANLAPTOPTheme {
                AdminDashboard()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            UserManager.cleanup()
            CategoryManager.cleanup()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tabs = listOf(
        TabItem("Sản phẩm", Icons.Default.ShoppingCart, "products"),
        TabItem("Người dùng", Icons.Default.Person, "users"),
        TabItem("Danh mục", Icons.Default.Category, "categories"),
        TabItem("Đơn hàng", Icons.Default.Checklist, "orders"),
        TabItem("Doanh thu", Icons.Default.Money, "revenue")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Admin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        FirebaseAuth.getInstance().signOut()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        (context as Activity).finish()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Đăng xuất")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(tabs) { tab ->
                        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                        val isSelected = currentRoute == tab.route

                        Column(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tab.title,
                                style = TextStyle(fontSize = 12.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "products",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("products") {
                AdminProductScreen(Modifier.navigationBarsPadding())
            }
            composable("users") {
                AdminUsersScreen(Modifier.navigationBarsPadding())
            }
            composable("categories") {
                AdminCategoriesScreen(Modifier.navigationBarsPadding())
            }
            composable("orders") {
                AdminOrderScreen(Modifier.navigationBarsPadding())
            }
            composable("revenue") {
                AdminRevenueScreen(Modifier.navigationBarsPadding())
            }
        }
    }
}

data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)