package com.example.appbanlaptop.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbanlaptop.Activity.BottomActivity.BottomMenu
import com.example.appbanlaptop.cart.CartScreenActivity
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.appbanlaptop.Model.CartItem
import com.example.appbanlaptop.Model.CartManager
import com.example.appbanlaptop.R
import com.example.appbanlaptop.payment.PaymentActivity
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import com.google.firebase.auth.FirebaseAuth

class DetailsItemsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra xem người dùng đã đăng nhập chưa
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val title = intent.getStringExtra("PRODUCT_TITLE") ?: "Unknown Product"
        val description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "No description available"
        val price = intent.getStringExtra("PRODUCT_PRICE") ?: "0 đ"
        val rating = intent.getDoubleExtra("PRODUCT_RATING", 0.0)
        val picUrl = intent.getStringExtra("PRODUCT_PIC_URL")
        val models = intent.getStringArrayExtra("PRODUCT_MODELS")?.toList() ?: emptyList()

        Log.d("DetailsItemsActivity", "picUrl: $picUrl")
        Log.d("DetailsItemsActivity", "title: $title, price: $price, models: $models")

        setContent {
            val isDarkMode = ThemeManager.isDarkMode(this)
            APPBANLAPTOPTheme(darkTheme = isDarkMode) {
                setContent {
                    DetailsItemsScreen(
                        title = title,
                        description = description,
                        price = price,
                        rating = rating,
                        picUrl = picUrl,
                        models = models,
                        onBackClick = { finish() },
                        onAddToCartClick = {
                            // Xử lý thêm vào giỏ hàng
                            val cleanedPrice = price.replace("[^0-9]".toRegex(), "")
                            Log.d("DetailsItemsActivity", "Cleaned price: $cleanedPrice")

                            val priceValue = cleanedPrice.toDoubleOrNull() ?: 0.0
                            Log.d("DetailsItemsActivity", "Parsed priceValue: $priceValue")

                            val cartItem = CartItem(
                                id = 0, // ID sẽ được tạo trong CartManager
                                title = title,
                                details = description,
                                price = priceValue,
                                imageUrl = picUrl,
                                quantity = 1,
                                isSelected = true
                            )
                            Log.d("DetailsItemsActivity", "Adding cart item: $cartItem")
                            CartManager.addCartItem(
                                cartItem,
                                onError = { errorMessage ->
                                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                                },
                                onDuplicate = { message ->
                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                },
                                onSuccess = { message ->
                                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        onBuyNowClick = {
                            // Xử lý mua ngay
                            val cleanedPrice = price.replace("[^0-9]".toRegex(), "")
                            val priceValue = cleanedPrice.toDoubleOrNull() ?: 0.0

                            val cartItem = CartItem(
                                id = 0,
                                title = title,
                                details = description,
                                price = priceValue,
                                imageUrl = picUrl,
                                quantity = 1,
                                isSelected = true
                            )

                            // Tạo danh sách sản phẩm để thanh toán
                            val checkoutItems = listOf(cartItem)

                            // Chuyển đến trang thanh toán
                            val intent = Intent(this, PaymentActivity::class.java).apply {
                                putParcelableArrayListExtra("CHECKOUT_ITEMS", ArrayList(checkoutItems))
                                putExtra("TOTAL_PRICE", priceValue)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dọn dẹp listener khi Activity bị hủy
        CartManager.cleanup()
    }
}

@Composable
fun DetailsItemsScreen(
    title: String,
    description: String,
    price: String,
    rating: Double,
    picUrl: String?,
    models: List<String>,
    onBackClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    onBuyNowClick: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(title)
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
                backgroundColor = Color(0xFF6200EE),
                contentColor = Color.White
            )
        },
        bottomBar = {
            BottomMenu(
                onItemClick = {
                    // Điều hướng đến CartScreenActivity
                    context.startActivity(Intent(context, CartScreenActivity::class.java))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hình ảnh sản phẩm
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFCCCCCC),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (picUrl != null && picUrl.isNotEmpty()) {
                        var isImageLoading by remember(picUrl) { mutableStateOf(true) }
                        var isImageError by remember(picUrl) { mutableStateOf(false) }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(picUrl)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                           contentDescription = "",
                            modifier = Modifier
                                .fillMaxSize(0.8f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White),
                            contentScale = ContentScale.Fit,
                            placeholder = painterResource(R.drawable.loadding),
                            error = painterResource(R.drawable.error),
                            onLoading = { isImageLoading = true },
                            onSuccess = {
                                isImageLoading = false
                                isImageError = false
                            },
                            onError = { state ->
                                isImageLoading = false
                                isImageError = true
                                Log.e("DetailsItemsScreen", "Error loading image: $picUrl, error: ${state.result.throwable.message}")
                            }
                        )

                        if (isImageLoading) {
                            Text(
                                text = "Đang tải...",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .padding(4.dp)
                            )
                        } else if (isImageError) {
                            Text(
                                text = "Lỗi tải hình ảnh",
                                fontSize = 14.sp,
                                color = Color.Red,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Không có hình ảnh",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            // Tiêu đề
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }

            // Giá và đánh giá
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = price,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF0000)
                    )
                    Text(
                        text = "⭐ $rating",
                        fontSize = 18.sp,
                        color = Color(0xFFFFC107)
                    )
                }
            }

            // Mô tả
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Description",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Các mô hình (model)
            if (models.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Available Models",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(models) { model ->
                            Surface(
                                modifier = Modifier.padding(5.dp),
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = model,
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Nút Add to Cart và Buy Now
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Nút Add to Cart
                    Button(
                        onClick = onAddToCartClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFCCCCCC),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Add to Cart",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Nút Buy Now
                    Button(
                        onClick = onBuyNowClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFFF0000),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Buy Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)) // Khoảng cách dưới cùng
            }
        }
    }
}