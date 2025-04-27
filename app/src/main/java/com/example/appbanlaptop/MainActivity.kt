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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.appbanlaptop.Activity.BottomActivity.BottomMenu
import com.example.appbanlaptop.Activity.DetailsItemsActivity
import com.example.appbanlaptop.Activity.ListItemActivity
import com.example.appbanlaptop.Activity.ProfileActivity
import com.example.appbanlaptop.Activity.ThemeManager
import com.example.appbanlaptop.Model.ProductItem
import com.example.appbanlaptop.ViewModel.MainViewModel
import com.example.appbanlaptop.cart.CartScreenActivity
import com.example.appbanlaptop.ui.theme.PaymentTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isDarkMode = mutableStateOf(false)
    private val viewModel: MainViewModel by viewModels()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val username by lazy {
        currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDarkMode.value = ThemeManager.isDarkMode(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setContent {


            PaymentTheme(darkTheme = isDarkMode.value) {
                MainActivityScreen(
                    productItems = viewModel.productItems,
                    categories = viewModel.categories,
                    onCartClick = {
                        val intent = Intent(this, CartScreenActivity::class.java)
                        startActivity(intent)
                    },
                    username = username,
                    isLoadingItems = viewModel.isLoadingItems
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDarkMode.value = ThemeManager.isDarkMode(this)
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
    username: String,
    onCartClick: @Composable () -> Unit,
    isLoadingItems: Boolean
) {
    val context = LocalContext.current
    val recommendedProducts = productItems.filter { it.showRecommended == true }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ProductItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Hàm tìm kiếm sản phẩm
    fun performSearch(query: String) {
        isSearching = true
        val results = productItems.filter { product ->
            val queryLower = query.lowercase()
            product.title?.lowercase()?.contains(queryLower) == true ||
                    product.description?.lowercase()?.contains(queryLower) == true
        }
        searchResults = results
        isSearching = false
    }

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
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(top = 18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(4f)
                    ) {
                        Text(
                            text = "Welcome Back",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = username,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = "Search",
                            modifier = Modifier
                                .weight(1f)
                                .size(36.dp)
                                .clickable { showSearchBar = !showSearchBar }
                        )
                    }
                }
            }

            if (showSearchBar) {
                item {
                    androidx.compose.material3.TextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            if (query.isNotEmpty()) {
                                performSearch(query)
                            } else {
                                searchResults = emptyList()
                                isSearching = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        placeholder = { Text("Tìm kiếm sản phẩm...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (isSearching) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .wrapContentSize(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Đang tìm kiếm...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            } else if (showSearchBar && searchQuery.isNotEmpty() && searchResults.isEmpty()) {
                item {
                    Text(
                        text = "Không tìm thấy sản phẩm nào",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            } else if (showSearchBar && searchResults.isNotEmpty()) {
                items(searchResults.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                            putExtra("PRODUCT_PRICE", product.price ?: "0 đ")
                                            putExtra("PRODUCT_RATING", product.rating ?: 0.0)
                                            putExtra("PRODUCT_PIC_URL", product.picUrl?.firstOrNull())
                                            putExtra("PRODUCT_MODELS", product.model?.toTypedArray())
                                            putExtra("PRODUCT_CATEGORY_ID", product.categoryId)
                                        }
                                        context.startActivity(intent)
                                    }
                            ) {
                                ProductItem(product = product)
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(0.6f))
                        }
                    }
                }
            }

            item { AutoSlidingCarousel() }
            item { SectionTitle("Categories") }
            item { CategoryList(categories, context) }
            item { SectionTitle("Recommended Products") }
            if (isLoadingItems) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .wrapContentSize(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Đang tải danh sách sản phẩm...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            } else if (recommendedProducts.isNotEmpty()) {
                items(recommendedProducts.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                            putExtra("PRODUCT_PRICE", product.price ?: "0 đ")
                                            putExtra("PRODUCT_RATING", product.rating ?: 0.0)
                                            putExtra("PRODUCT_PIC_URL", product.picUrl?.firstOrNull())
                                            putExtra("PRODUCT_MODELS", product.model?.toTypedArray())
                                            putExtra("PRODUCT_CATEGORY_ID", product.categoryId)
                                        }
                                        context.startActivity(intent)
                                    }
                            ) {
                                ProductItem(product = product)
                            }
                        }
                        if (pair.size < 2) {
                            Spacer(modifier = Modifier.weight(0.6f))
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Không có sản phẩm được đề xuất",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            item.picUrl?.let { imageUrl ->
                var isImageLoading by remember { mutableStateOf(true) }
                var isImageError by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.loadding),
                    error = painterResource(R.drawable.error),
                    colorFilter = if (isSelected) ColorFilter.tint(MaterialTheme.colorScheme.onPrimary) else null,
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
                        fontSize = 7.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
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
                    colorFilter = if (isSelected) ColorFilter.tint(MaterialTheme.colorScheme.onPrimary) else null
                )
            }
        }
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title ?: "Không có tiêu đề",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProductItem(product: ProductItem) {
    Column(
        modifier = Modifier
            .width(155.dp)
            .height(230.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            product.picUrl?.takeIf { it.isNotEmpty() }?.let { imageUrls ->
                val imageUrl = imageUrls[0]
                var isImageLoading by remember { mutableStateOf(true) }
                var isImageError by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(4.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi tải hình ảnh",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(4.dp)
                    )
                }
            } ?: run {
                Image(
                    painter = painterResource(R.drawable.loadding),
                    contentDescription = "Placeholder",
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "Không có hình ảnh",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.title ?: "Không có tiêu đề",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
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
                text = buildAnnotatedString {
                    append("${product.price ?: 0}")
                    withStyle(
                        style = SpanStyle(
                            fontSize = 10.sp,
                            baselineShift = BaselineShift.Superscript
                        )
                    ) {
                        append("đ")
                    }
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red
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
                .clip(RoundedCornerShape(12.dp))
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF6200EE))
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
    unselectedColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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