package com.example.appbanlaptop.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appbanlaptop.Model.ProductItem
import com.google.firebase.database.*

class ListItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lấy categoryId từ Intent, mặc định là "0" nếu không có
        val categoryId = intent.getStringExtra("categoryID") ?: "0"

        setContent {
            ListItemScreen(categoryId = categoryId, onBackClick = { finish() })
        }
    }
}

@Composable
fun ListItemScreen(categoryId: String, onBackClick: () -> Unit) {
    val productItems = remember { mutableStateListOf<ProductItem>() }

    // Lấy dữ liệu từ Firebase
    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance("https://appbanlaptop-default-rtdb.firebaseio.com/")
        val itemsRef = database.getReference("Items")

        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productItems.clear()
                for (item in snapshot.children) {
                    try {
                        val productItem = item.getValue(ProductItem::class.java)
                        if (productItem != null && productItem.categoryID == categoryId) {
                            productItems.add(productItem)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ListItemScreen", "Error parsing item: ${item.key}, error: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("ListItemScreen", "Firebase error: ${error.message}")
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sản phẩm trong danh mục ${if (categoryId == "0") "Pc" else categoryId}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0xFF6200EE),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            if (productItems.isNotEmpty()) {
                items(productItems.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pair.forEach { product ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .width(150.dp)
                            ) {
                                ProductItem(product = product) // Hàm ProductItem từ mã cũ
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Không có sản phẩm nào trong danh mục này",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}