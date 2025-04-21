package com.example.appbanlaptop.cart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.appbanlaptop.Model.CartItem
import com.example.appbanlaptop.Model.CartManager
import com.example.appbanlaptop.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.example.appbanlaptop.Activity.BottomActivity
import com.example.appbanlaptop.payment.PaymentActivity
import java.text.NumberFormat
import java.util.Locale

class CartScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra xem người dùng đã đăng nhập chưa
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem giỏ hàng", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Thiết lập userId cho CartManager
        CartManager.setUserId(currentUser.uid)

        setContent {
            CartScreen(
                navController = null,
                onBackClick = { finish() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CartManager.cleanup() // Dọn dẹp listener khi Activity bị hủy
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController? = null, onBackClick: () -> Unit) {
    // Đồng bộ cartItems với CartManager
    var cartItems by remember { mutableStateOf(CartManager.getCartItems()) }
    val context = LocalContext.current
    val locale = Locale("vi", "VN")
    val numberFormat = NumberFormat.getCurrencyInstance(locale)

    // Lấy userId từ FirebaseAuth
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "Unknown User"

    // Lắng nghe thay đổi từ CartManager sử dụng Flow
    LaunchedEffect(Unit) {
        CartManager.cartItemsFlow.collectLatest { updatedItems ->
            Log.d("CartScreen", "Cart items updated: size=${updatedItems.size}, items=$updatedItems")
            cartItems = updatedItems
        }
    }

    // Tính tổng giá của các sản phẩm được chọn (dùng cho nút Check Out)
    val selectedItemsTotal by remember(cartItems) {
        derivedStateOf {
            cartItems.filter { it.isSelected }.sumOf { it.price * it.quantity }
        }
    }

    // Tính tổng giá của tất cả sản phẩm
    val allItemsTotal by remember(cartItems) {
        derivedStateOf {
            cartItems.sumOf { it.price * it.quantity }
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
                        Icon(
                            painter = painterResource(R.drawable.icon_cart),
                            contentDescription = "Cart Icon",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cart",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "Back",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomActivity.BottomMenu(
                onItemClick = {

                }
            )
        },
        content = { paddingValues ->
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF6F6F6))
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Giỏ hàng của bạn đang trống",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF6F6F6))
                        .padding(paddingValues)
                ) {
                    // Hiển thị userId, tổng số sản phẩm và tổng giá của tất cả sản phẩm
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total items: ${cartItems.size}",
                                    fontSize = 14.sp
                                )
                                val formattedTotal = numberFormat.format(allItemsTotal)
                                Text(
                                    text = "Total: $formattedTotal",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A6FF0)
                                )
                            }
                        }
                    }

                    // Danh sách sản phẩm
                    items(cartItems, key = { it.firebaseKey ?: it.id }) { cartItem ->
                        CartItemCard(
                            cartItem = cartItem,
                            onSelectChange = { isSelected ->
                                Log.d("CartScreen", "Updating selection for item: ${cartItem.title}, isSelected=$isSelected")
                                val updatedItem = cartItem.copy(isSelected = isSelected)
                                CartManager.updateCartItem(updatedItem)
                            },
                            onQuantityChange = { newQuantity ->
                                Log.d("CartScreen", "Changing quantity for item: ${cartItem.title}, newQuantity=$newQuantity")
                                if (newQuantity <= 0) {
                                    CartManager.removeCartItem(cartItem.firebaseKey)
                                    Toast.makeText(context, "Đã xóa ${cartItem.title} khỏi giỏ hàng", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updatedItem = cartItem.copy(quantity = newQuantity)
                                    CartManager.updateCartItem(updatedItem)
                                }
                            },
                            onDelete = {
                                Log.d("CartScreen", "Deleting item: ${cartItem.title}")
                                CartManager.removeCartItem(cartItem.firebaseKey)
                                Toast.makeText(context, "Đã xóa ${cartItem.title} khỏi giỏ hàng", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Trong LazyColumn của CartScreen
                    item {
                        Button(
                            onClick = {
                                val itemsToCheckout = cartItems.filter { it.isSelected }
                                Log.d("CartScreen", "Check Out clicked, selected items: ${itemsToCheckout.size}, items: $itemsToCheckout")
                                if (itemsToCheckout.isNotEmpty()) {
                                    Log.d("CartScreen", "Navigating to PaymentActivity with total: $selectedItemsTotal")
                                    val intent = Intent(context, PaymentActivity::class.java).apply {
                                        putParcelableArrayListExtra("CHECKOUT_ITEMS", ArrayList(itemsToCheckout))
                                        putExtra("TOTAL_PRICE", selectedItemsTotal) // selectedItemsTotal là Double
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Log.d("CartScreen", "No items selected for checkout")
                                    Toast.makeText(context, "Vui lòng chọn ít nhất một sản phẩm để thanh toán", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(48.dp) // Đảm bảo nút đủ lớn
                                .zIndex(1f), // Đảm bảo nút không bị che
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6FF0)),
                            enabled = cartItems.any { it.isSelected }.also {
                                Log.d("CartScreen", "Check Out button enabled: $it, selected items: ${cartItems.filter { it.isSelected }}")
                            }
                        ) {
                            val formattedTotal = numberFormat.format(selectedItemsTotal)
                            Text(
                                text = "Check Out ($formattedTotal)",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
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
    val context = LocalContext.current
    val locale = Locale("vi", "VN")
    val numberFormat = NumberFormat.getCurrencyInstance(locale)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Selection checkbox
            Checkbox(
                checked = cartItem.isSelected,
                onCheckedChange = { isChecked ->
                    Log.d("CartItemCard", "Checkbox clicked for ${cartItem.title}, new state: $isChecked, firebaseKey: ${cartItem.firebaseKey}")
                    onSelectChange(isChecked)
                },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(28.dp) // Tăng kích thước để dễ nhấn
                    .background(Color.Transparent, RoundedCornerShape(4.dp)) // Đảm bảo không bị che
            )

            // Hiển thị hình ảnh sản phẩm
            if (cartItem.imageUrl != null && cartItem.imageUrl.isNotEmpty()) {
                var isImageLoading by remember(cartItem.imageUrl) { mutableStateOf(true) }
                var isImageError by remember(cartItem.imageUrl) { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cartItem.imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = cartItem.title,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    placeholder = painterResource(R.drawable.loadding),
                    error = painterResource(R.drawable.error),
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false; isImageError = false },
                    onError = { isImageLoading = false; isImageError = true }
                )

                if (isImageLoading) {
                    Text(
                        text = "Đang tải...",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.cat1),
                    contentDescription = "Cart Item Image",
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Đặt giá tiền và quantity controls trong một Column
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Giá tiền (chỉ hiển thị tổng giá)
                    val totalPrice = cartItem.price * cartItem.quantity
                    val formattedPrice = numberFormat.format(totalPrice)
                    Text(
                        text = formattedPrice,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Red,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quantity controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .wrapContentWidth()
                            .width(IntrinsicSize.Max)
                    ) {
                        IconButton(
                            onClick = {
                                if (cartItem.quantity > 1) {
                                    onQuantityChange(cartItem.quantity - 1)
                                } else {
                                    onQuantityChange(0) // Xóa ngay khi số lượng về 0
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = "-",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = cartItem.quantity.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (cartItem.quantity < cartItem.maxStock) {
                                    onQuantityChange(cartItem.quantity + 1)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Đã đạt số lượng tối đa (${cartItem.maxStock})",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = "+",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
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

@Preview(showBackground = true)
@Composable
fun CartScreenPreview() {
    CartScreen(onBackClick = {})
}