package com.example.appbanlaptop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.appbanlaptop.Activity.DetailsItemsActivity
import com.example.appbanlaptop.Activity.ListItemActivity
import com.example.appbanlaptop.Cart.CartScreenActivity

import com.example.appbanlaptop.Model.ProductItem
import com.example.appbanlaptop.ViewModel.MainViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Khởi tạo ViewModel
    private val viewModel: MainViewModel by viewModels()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val username = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Thiết lập giao diện thanh trạng thái
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContent {
            MainActivityScreen(
                productItems = viewModel.productItems,
                categories = viewModel.categories,
                onCartClick = {
                    val intent = Intent(this, CartScreenActivity::class.java)
                    startActivity(intent)
                },
                username = username,
            )
        }
    }
}

data class CategoryItem(
    val id: Int? = null,
    val picUrl: String? = null,
    val title: String? = null
)


@Composable
fun MainActivityScreen(
    productItems: List<ProductItem>,
    categories: List<CategoryItem>,
    username: String, // Thêm tham số username để hiển thị tên tài khoản Google
    onCartClick: @Composable () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomMenu(
                onItemClick = {
                    val intent = Intent(context, CartScreenActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(top = 18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Welcome Back", fontSize = 16.sp, color = Color.Gray)
                        Text(
                            text = username, // Sử dụng username từ tham số
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Row {
                        Image(painter = painterResource(R.drawable.fav_icon), contentDescription = "Favorite", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(painter = painterResource(R.drawable.search_icon), contentDescription = "Search", modifier = Modifier.size(24.dp))
                    }
                }
            }

            item { AutoSlidingCarousel() }

            item { SectionTitle("Categories", "See All") }
            item { CategoryList(categories, context) }

            item { SectionTitle("Recommendation", "See All") }
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
                        text = "Đang tải danh sách sản phẩm...",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryList(categories: List<CategoryItem>, context: Context) {
    var selectedIndex by remember { mutableStateOf(-1) }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        items(categories.size) { index ->
            CategoryItem(
                item = categories[index],
                isSelected = selectedIndex == index,
                onItemClick = {
                    selectedIndex = index
                    val intent = Intent(context, ListItemActivity::class.java).apply {
                        putExtra("CATEGORY_ID", categories[index].id.toString())
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun CategoryItem(
    item: CategoryItem,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onItemClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = if (isSelected) Color(0xFF6200EE) else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            item.picUrl?.let { imageUrl ->
                var isImageLoading by remember { mutableStateOf(true) }
                var isImageError by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED) // Bật bộ nhớ đệm
                        .diskCachePolicy(CachePolicy.ENABLED)   // Bật bộ nhớ đệm trên đĩa
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.loadding),
                    error = painterResource(R.drawable.error),
                    colorFilter = if (isSelected) ColorFilter.tint(Color.White) else null,
                    onLoading = { isImageLoading = true },
                    onSuccess = {
                        isImageLoading = false
                        isImageError = false
                    },
                    onError = {
                        isImageLoading = false
                        isImageError = true
                    }
                )

                if (isImageLoading) {
                    Text(
                        text = "Đang tải...",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi",
                        fontSize = 10.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                }
            } ?: run {
                Image(
                    painter = painterResource(R.drawable.loadding),
                    contentDescription = "Placeholder",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isSelected) ColorFilter.tint(Color.White) else null
                )
            }
        }
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title ?: "Không có tiêu đề",
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, actionText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = actionText,
            color = Color(0xFF6200EE),
            fontSize = 16.sp,
            modifier = Modifier.clickable { /* TODO: Handle See All */ }
        )
    }
}

@Composable
fun ProductItem(product: ProductItem) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .height(220.dp)
            .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            product.picUrl?.takeIf { it.isNotEmpty() }?.let { imageUrls ->
                val imageUrl = imageUrls[0] // Lấy URL đầu tiên trong danh sách
                var isImageLoading by remember { mutableStateOf(true) }
                var isImageError by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED) // Bật bộ nhớ đệm
                        .diskCachePolicy(CachePolicy.ENABLED)   // Bật bộ nhớ đệm trên đĩa
                        .build(),
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .clip(RoundedCornerShape(8.dp)),
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
                        Log.e("ProductItem", "Error loading image: $imageUrl, error: ${state.result.throwable.message}")
                    }
                )

                if (isImageLoading) {
                    Text(
                        text = "Đang tải...",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(4.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi tải hình ảnh",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.White.copy(alpha = 0.7f))
                            .padding(4.dp)
                    )
                }
            } ?: run {
                Image(
                    painter = painterResource(R.drawable.loadding),
                    contentDescription = "Placeholder",
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "Không có hình ảnh",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.title ?: "Không có tiêu đề",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "⭐ ${product.rating?.toFloat() ?: 0f}",
                fontSize = 12.sp,
                color = Color(0xFFFFC107)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${product.price ?: 0} đ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun AutoSlidingCarousel(
    modifier: Modifier = Modifier,
    autoSlideDuration: Long = 3000L
) {
    val banners = listOf(
        R.drawable.banner1,
        R.drawable.banner2
    )
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        while (true) {
            delay(autoSlideDuration)
            coroutineScope.launch {
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            count = banners.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp)) // Giữ viền bo góc
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF6200EE)) // Giữ background nếu cần
            ) {
                Image(
                    painter = painterResource(id = banners[page]),
                    contentDescription = "Banner Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        DotIndicator(totalDots = banners.size, selectedIndex = pagerState.currentPage)
    }
}

@Composable
fun DotIndicator(
    totalDots: Int,
    selectedIndex: Int,
    dotSize: Dp = 8.dp,
    selectedColor: Color = Color(0xFF6200EE),
    unselectedColor: Color = Color.Gray,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalDots) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) dotSize else dotSize * 0.5f)
                    .background(
                        color = if (index == selectedIndex) selectedColor else unselectedColor,
                        shape = CircleShape
                    )
            )
            if (index != totalDots - 1) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}


@Composable
fun BottomMenu(
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .background(
                color = colorResource(R.color.purple),
                shape = RoundedCornerShape(10.dp)
            ),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomMenuItem(
            icon = painterResource(R.drawable.btn_1),
            text = "Explore"
        )
        BottomMenuItem(
            icon = painterResource(R.drawable.btn_2),
            text = "Cart",
            onItemClick = onItemClick
        )
        BottomMenuItem(
            icon = painterResource(R.drawable.btn_3),
            text = "Favorite"
        )
        BottomMenuItem(
            icon = painterResource(R.drawable.btn_4),
            text = "Order"
        )
        BottomMenuItem(
            icon = painterResource(R.drawable.btn_5),
            text = "Profile"
        )
    }
}

@Composable
fun BottomMenuItem(
    icon: Painter,
    text: String,
    onItemClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .height(60.dp)
            .clickable(
                enabled = onItemClick != null,
                onClick = { onItemClick?.invoke() }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}