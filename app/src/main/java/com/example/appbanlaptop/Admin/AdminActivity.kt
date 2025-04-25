package com.example.appbanlaptop.Admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.appbanlaptop.Activity.AdminProductScreen // Import AdminProductScreen
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
        UserManager.cleanup()
        CategoryManager.cleanup()
        OrderManager.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Items", "Users", "Categories", "Revenue" , "Orders")
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
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
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                1 -> Icon(Icons.Default.Person, contentDescription = null)
                                2 -> Icon(Icons.Default.Category, contentDescription = null)
                                3 -> Icon(Icons.Default.Money, contentDescription = null)
                                4 -> Icon(Icons.Default.Checklist, contentDescription = null)
                            }
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> AdminProductScreen(Modifier.padding(padding)) // Thay AdminItemsScreen bằng AdminProductScreen
            1 -> AdminUsersScreen(Modifier.padding(padding))
            2 -> AdminCategoriesScreen(Modifier.padding(padding))
            3 -> AdminRevenueScreen(Modifier.padding(padding))
            4 -> AdminOrderScreen(Modifier.padding(padding))
        }
    }
}