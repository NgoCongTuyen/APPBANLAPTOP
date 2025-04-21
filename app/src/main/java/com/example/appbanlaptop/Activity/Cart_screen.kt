//package com.example.appbanlaptop.Activity
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import coil.compose.AsyncImage
//import coil.request.CachePolicy
//import coil.request.ImageRequest
//import com.example.appbanlaptop.Model.CartItem
//import com.example.appbanlaptop.Model.CartManager
//import com.example.appbanlaptop.R
//
//class CartScreenActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            CartScreen(
//                navController = null,
//                onBackClick = { finish() }
//            )
//        }
//    }
//}
//
//data class Product(val title: String, val price: String)
//
//@OptIn(ExperimentalMaterial3Api::class)
//@SuppressLint("UnrememberedMutableState")
//@Composable
//fun CartScreen(navController: NavController?, onBackClick: () -> Unit) {
//    var cartItems by remember { mutableStateOf(CartManager.getCartItems()) }
//
//    val selectedItemsTotal by derivedStateOf {
//        cartItems.filter { it.isSelected }.sumOf { it.price * it.quantity }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.Center,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            painter = painterResource(R.drawable.icon_cart),
//                            contentDescription = "Cart Icon",
//                            modifier = Modifier.size(40.dp),
//                            tint = Color.Unspecified
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text("Cart", fontWeight = FontWeight.Bold)
//                    }
//                },
//                navigationIcon = {
//                    androidx.compose.material.IconButton(onClick = onBackClick) {
//                        androidx.compose.material.Icon(
//                            painter = painterResource(R.drawable.back),
//                            contentDescription = "Back",
//                            modifier = Modifier.size(40.dp),
//                            tint = Color.Unspecified
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.White)
//            )
//        },
//        content = { paddingValues ->
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color(0xFFF6F6F6))
//                    .padding(paddingValues)
//            ) {
//                // Phần "Selected items" và "Total"
//                item {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 16.dp, vertical = 8.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            text = "Selected: ${cartItems.count { it.isSelected }} items",
//                            fontSize = 14.sp
//                        )
//                        // Định dạng tổng giá với dấu chấm phân tách hàng nghìn
//                        val formattedTotal = selectedItemsTotal.toLong().toString()
//                            .reversed()
//                            .chunked(3)
//                            .joinToString(".")
//                            .reversed()
//                        Text(
//                            text = "Total: $formattedTotal đ",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Bold,
//                            color = Color(0xFF4A6FF0)
//                        )
//                    }
//                }
//
//                // Danh sách sản phẩm
//                items(cartItems) { cartItem ->
//                    CartItemCard(
//                        cartItem = cartItem,
//                        onSelectChange = { isSelected ->
//                            val updatedItem = cartItem.copy(isSelected = isSelected)
//                            cartItems = cartItems.map {
//                                if (it.id == cartItem.id) updatedItem else it
//                            }
//                            CartManager.updateCartItem(updatedItem) // Đồng bộ với CartManager
//                        },
//                        onQuantityChange = { newQuantity ->
//                            val updatedItem = cartItem.copy(quantity = newQuantity)
//                            cartItems = cartItems.map {
//                                if (it.id == cartItem.id) updatedItem else it
//                            }
//                            CartManager.updateCartItem(updatedItem) // Đồng bộ với CartManager
//                        },
//                        onDelete = {
//                            cartItems = cartItems.filter { it.id != cartItem.id }
//                            CartManager.removeCartItem(cartItem.id) // Đồng bộ với CartManager
//                        }
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//
//                // Nút "Check Out"
//                item {
//                    Button(
//                        onClick = {
//                            // Only proceed with selected items
//                            val itemsToCheckout = cartItems.filter { it.isSelected }
//                            // TODO: Handle checkout
//                        },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp),
//                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6FF0)),
//                        enabled = cartItems.any { it.isSelected }
//                    ) {
//                        // Định dạng giá với dấu chấm phân tách hàng nghìn
//                        val formattedTotal = selectedItemsTotal.toLong().toString()
//                            .reversed()
//                            .chunked(3)
//                            .joinToString(".")
//                            .reversed()
//                        Text(
//                            text = "Check Out ($formattedTotal đ)",
//                            color = Color.White,
//                            fontSize = 16.sp
//                        )
//                    }
//                }
//            }
//        }
//    )
//}
//
//@Composable
//fun CartItemCard(
//    cartItem: CartItem,
//    onSelectChange: (Boolean) -> Unit,
//    onQuantityChange: (Int) -> Unit,
//    onDelete: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(100.dp),
//        shape = RoundedCornerShape(8.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//
//    ) {
//        Row(
//            modifier = Modifier.padding(8.dp),
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            // Selection checkbox
//            Checkbox(
//                checked = cartItem.isSelected,
//                onCheckedChange = onSelectChange,
//                modifier = Modifier.padding(end = 4.dp)
//            )
//
//            // Hiển thị hình ảnh sản phẩm
//            if (cartItem.imageUrl != null && cartItem.imageUrl.isNotEmpty()) {
//                AsyncImage(
//                    model = ImageRequest.Builder(LocalContext.current)
//                        .data(cartItem.imageUrl)
//                        .memoryCachePolicy(CachePolicy.ENABLED)
//                        .diskCachePolicy(CachePolicy.ENABLED)
//                        .build(),
//                    contentDescription = cartItem.title,
//                    modifier = Modifier
//                        .size(70.dp)
//                        .clip(RoundedCornerShape(8.dp)),
//                    placeholder = painterResource(R.drawable.loadding),
//                    error = painterResource(R.drawable.error)
//                )
//            } else {
//                Image(
//                    painter = painterResource(id = R.drawable.cat1),
//                    contentDescription = "Cart Item Image",
//                    modifier = Modifier.size(80.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            Column(modifier = Modifier.weight(0.9f)) {
//                Text(
//                    text = cartItem.title,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp,
//                    maxLines = 1,
//                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
//                )
//
//                Spacer(modifier = Modifier.height(4.dp))
//
//                // Đặt giá tiền và quantity controls trong một Column
//                Column(
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    // Giá tiền
//                    val totalPrice = cartItem.price * cartItem.quantity
//                    val formattedPrice = totalPrice.toLong().toString()
//                        .reversed()
//                        .chunked(3)
//                        .joinToString(".")
//                        .reversed()
//                    Text(
//                        text = "$formattedPrice đ",
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 16.sp,
//                        color = Color.Red,
//                        maxLines = 1
//                    )
//
//                    Spacer(modifier = Modifier.height(4.dp)) // Khoảng cách giữa giá và quantity controls
//
//                    // Quantity controls
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(8.dp),
//                        modifier = Modifier
//                            .wrapContentWidth()
//                            .width(IntrinsicSize.Max)
//                    ) {
//                        IconButton(
//                            onClick = {
//                                if (cartItem.quantity > 1) {
//                                    onQuantityChange(cartItem.quantity - 1)
//                                }
//                            },
//                            modifier = Modifier
//                                .size(32.dp)
//                                .minimumInteractiveComponentSize()
//                        ) {
//                            Text(
//                                text = "-",
//                                fontSize = 20.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
//
//                        Text(
//                            text = cartItem.quantity.toString(),
//                            fontSize = 18.sp,
//                            fontWeight = FontWeight.Bold
//                        )
//
//                        IconButton(
//                            onClick = { onQuantityChange(cartItem.quantity + 1) },
//                            modifier = Modifier
//                                .size(32.dp)
//                                .minimumInteractiveComponentSize()
//                        ) {
//                            Text(
//                                text = "+",
//                                fontSize = 20.sp,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
//                    }
//                }
//            }
//
//            // Delete button
//            IconButton(onClick = onDelete) {
//                Icon(
//                    imageVector = Icons.Default.Delete,
//                    contentDescription = "Delete",
//                    tint = Color.Red
//                )
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun CartScreenPreview() {
//    CartScreen(navController = null, onBackClick = {})
//}