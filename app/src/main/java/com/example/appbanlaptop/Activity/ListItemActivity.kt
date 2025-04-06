package com.example.appbanlaptop.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appbanlaptop.Model.ProductItem
import com.google.firebase.database.*

class ListItemActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryId = intent.getStringExtra("CATEGORY_ID") ?: "0"
        setContent {
            ListItemScreen(categoryId = categoryId, onBackClick = { finish() })
        }
    }
}

@Composable
fun ListItemScreen(categoryId: String, onBackClick: () -> Unit) {
    val productItems = remember { mutableStateListOf<ProductItem>() }
    val categoryItems = remember { mutableStateListOf<CategoryItem>() }
    var categoryTitle by remember { mutableStateOf(categoryId) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance("https://appbanlaptop-default-rtdb.firebaseio.com/")
        val categoriesRef = database.getReference("Category")
        categoriesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryItems.clear()
                for (item in snapshot.children) {
                    try {
                        val categoryItem = item.getValue(CategoryItem::class.java)
                        if (categoryItem != null) {
                            categoryItems.add(categoryItem)
                            if (categoryItem.id.toString() == categoryId) {
                                categoryTitle = categoryItem.title ?: categoryId
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ListItemScreen", "Error parsing category: ${item.key}, error: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("ListItemScreen", "Firebase error: ${error.message}")
            }
        })

        val itemsRef = database.getReference("Items")
        itemsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productItems.clear()
                for (item in snapshot.children) {
                    try {
                        val productItem = item.getValue(ProductItem::class.java)
                        if (productItem != null && productItem.categoryId == categoryId) {
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
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("$categoryTitle")
                    }
                },
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
                                    .clickable {
                                        val intent = Intent(context, DetailsItemsActivity::class.java).apply {
                                            putExtra("PRODUCT_TITLE", product.title)
                                            putExtra("PRODUCT_DESCRIPTION", product.description)
                                            putExtra("PRODUCT_PRICE", product.price ?: 0L)
                                            putExtra("PRODUCT_RATING", product.rating ?: 0.0)
                                            putExtra("PRODUCT_PIC_URL", product.picUrl?.firstOrNull())
                                            putExtra("PRODUCT_MODELS", product.model?.toTypedArray())
                                        }
                                        context.startActivity(intent)
                                    }
                            ) {
                                ProductItem(product = product)
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