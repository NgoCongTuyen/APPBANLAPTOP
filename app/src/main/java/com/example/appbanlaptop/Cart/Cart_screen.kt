package com.example.appbanlaptop.Cart

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.appbanlaptop.R

class CartScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CartScreen(navController = null)
        }
    }
}

data class CartItem(
    val id: Int,
    val title: String,
    val details: String,
    val price: Double,
    var quantity: Int = 1,
    var isSelected: Boolean = false
)

data class Product(val title: String, val price: String)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun CartScreen(navController: NavController?) {
    var cartItems by remember { mutableStateOf(initialCartItems()) }
    var recommendedProducts by remember { mutableStateOf(initialRecommendedProducts()) }

    val selectedItemsTotal by derivedStateOf {
        cartItems.filter { it.isSelected }.sumOf { it.price * it.quantity }
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
                        Icon(
                            painter = painterResource(R.drawable.icon_cart),
                            contentDescription = "Cart Icon",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cart", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back",
                            tint = Color.Unspecified
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
                // Selected items count and total price
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Selected: ${cartItems.count { it.isSelected }} items",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Total: $${"%.2f".format(selectedItemsTotal)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A6FF0)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(cartItems) { cartItem ->
                        CartItemCard(
                            cartItem = cartItem,
                            onSelectChange = { isSelected ->
                                cartItems = cartItems.map {
                                    if (it.id == cartItem.id) it.copy(isSelected = isSelected) else it
                                }
                            },
                            onQuantityChange = { newQuantity ->
                                cartItems = cartItems.map {
                                    if (it.id == cartItem.id) it.copy(quantity = newQuantity) else it
                                }
                            },
                            onDelete = {
                                cartItems = cartItems.filter { it.id != cartItem.id }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

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
                Button(
                    onClick = {
                        // Only proceed with selected items
                        val itemsToCheckout = cartItems.filter { it.isSelected }
                        // TODO: Handle checkout
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6FF0)),
                    enabled = cartItems.any { it.isSelected }
                ) {
                    Text(
                        text = "Check Out ($${"%.2f".format(selectedItemsTotal)})",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    )
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onSelectChange: (Boolean) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            Checkbox(
                checked = cartItem.isSelected,
                onCheckedChange = onSelectChange,
                modifier = Modifier.padding(end = 8.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.cat1),
                contentDescription = "Cart Item Image",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(cartItem.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(cartItem.details, fontSize = 14.sp, color = Color.Gray)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "$${"%.2f".format(cartItem.price * cartItem.quantity)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    // Quantity controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (cartItem.quantity > 1) {
                                    onQuantityChange(cartItem.quantity - 1)
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("-", fontSize = 18.sp)
                        }

                        Text(cartItem.quantity.toString(), fontSize = 16.sp)

                        IconButton(
                            onClick = { onQuantityChange(cartItem.quantity + 1) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text("+", fontSize = 18.sp)
                        }
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier.width(150.dp).height(160.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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

// Helper functions to initialize data
fun initialCartItems(): List<CartItem> = listOf(
    CartItem(
        id = 1,
        title = "Apple 2023 MacBook Pro",
        details = "13.6 inch - Apple M1\n256 GB - 8 GB",
        price = 1000.0
    ),
    CartItem(
        id = 2,
        title = "Asus ROG Strix G15",
        details = "15.6 inch - Ryzen 9\n1TB SSD - 16GB RAM",
        price = 1200.0
    ),
    CartItem(
        id = 3,
        title = "Dell XPS 13",
        details = "13.3 inch - Intel i7\n512 GB SSD - 16 GB RAM",
        price = 1100.0
    ),
    CartItem(
        id = 4,
        title = "Lenovo Legion 5",
        details = "15.6 inch - Ryzen 7\n512GB SSD - 16GB RAM",
        price = 999.0
    )
)

fun initialRecommendedProducts(): List<Product> = listOf(
    Product("iPhone 15 Pro Max", "$1,000"),
    Product("MacBook Pro 14\"", "$1,000"),
    Product("Samsung Galaxy S23", "$950"),
    Product("iPad Pro 12.9\"", "$1,200"),
    Product("Sony WH-1000XM5", "$350"),
    Product("Logitech MX Master 3", "$100")
)

@Preview(showBackground = true)
@Composable
fun CartScreenPreview() {
    CartScreen(navController = null)
}