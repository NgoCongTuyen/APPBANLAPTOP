package com.example.appbanlaptop.Screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbanlaptop.R

// Đổi tên MainActivity để tránh xung đột nếu dự án đã có MainActivity khác
class CartScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CartScreen()
        }
    }
}

data class CartItem(val title: String, val details: String, val price: String)
data class Product(val title: String, val price: String)

val cartItems = listOf(
    CartItem(
        title = "Apple 2023 MacBook Pro",
        details = "13.6 inch - Apple M1\n256 GB - 8 GB",
        price = "$1,000"
    )
)

val recommendedProducts = listOf(
    Product(title = "iPhone 15 Pro Max", price = "$1,000"),
    Product(title = "MacBook Pro 14\"", price = "$1,000")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Xử lý back */ }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.White)
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF6F6F6))
                    .padding(paddingValues)
            ) {
                // Danh sách sản phẩm trong giỏ hàng
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(cartItems) { cartItem ->
                        CartItemCard(cartItem)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                // Phần gợi ý sản phẩm
                Text(
                    text = "Recommendation for you",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                LazyRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(recommendedProducts) { product ->
                        ProductCard(product)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                // Nút Check Out
                Button(
                    onClick = { /* TODO: Xử lý check out */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6FF0))
                ) {
                    Text(text = "Check Out", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    )
}

@Composable
fun CartItemCard(cartItem: CartItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat1),
                contentDescription = "Cart Item Image",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(cartItem.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(cartItem.details, fontSize = 14.sp, color = Color.Gray)
                Text(cartItem.price, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.cat2),
                contentDescription = "Product Image",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(product.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(product.price, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CartScreenPreview() {
    CartScreen()
}
