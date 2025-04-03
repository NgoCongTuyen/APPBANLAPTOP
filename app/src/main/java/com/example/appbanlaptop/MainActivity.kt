package com.example.appbanlaptop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.appbanlaptop.Model.CategoryModel
import com.example.appbanlaptop.Model.ItemsModel
import com.example.appbanlaptop.Screen.CartScreen
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.NonDisposableHandle.parent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    MainActivityScreen(onCartClick = {
                        navController.navigate("cart") // Điều hướng sang giỏ hàng
                    })
                }
                composable("cart") {
                    CartScreen(navController)
                }
            }
        }
    }
}

@OptIn(InternalCoroutinesApi::class)
@Composable
fun MainActivityScreen(onCartClick: () -> Unit) {
    val categories = listOf(
        CategoryModel("Laptop", R.drawable.cat1),
        CategoryModel("IPhone", R.drawable.cat2),
        CategoryModel("Earphone", R.drawable.cat3),
        CategoryModel("Gaming", R.drawable.cat4),
        CategoryModel("Camera", R.drawable.cat5),
        CategoryModel("Clock", R.drawable.cat6)
    )
    val products = listOf(
        ItemsModel("Headphone 12A", 4.6f, 95.0, R.drawable.cat1_1),
        ItemsModel("Business Laptop", 4.7f, 550.0, R.drawable.cat2_1),
        ItemsModel("laptop B2003", 4.7f, 550.0, R.drawable.cat2_2),
        ItemsModel("laptop B2003", 4.7f, 550.0, R.drawable.cat2_3),
    )

    // Sử dụng Scaffold để cố định BottomMenu
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomMenu(
                modifier = Modifier
                    .fillMaxWidth(),
                onItemClick = onCartClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues) // Tôn trọng khoảng cách từ Scaffold
                .padding(top = 48.dp)
        ) {
            // Welcome Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome Back",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Jackie",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Row {
                        Image(
                            painter = painterResource(R.drawable.fav_icon),
                            contentDescription = "Favorite",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Banner Section
            item {
                AutoSlidingCarousel()
            }

            // Categories Section
            item {
                SectionTitle("Categories", "See All")
            }
            item {
                CategoryList(categories)
            }

            // Products Section
            item {
                SectionTitle("Recommendation", "See All")
            }
            items(products.chunked(2)) { pair ->
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
                            ProductItem(product = product)
                        }
                    }
                    if (pair.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryList(categories: List<CategoryModel>) {
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
                }
            )
        }
    }
}

@Composable
fun CategoryItem(
    item: CategoryModel,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onItemClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = item.iconResId),
            contentDescription = item.title,
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = if (isSelected) Color(0xFF6200EE) else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentScale = ContentScale.Inside,
            colorFilter = if (isSelected) ColorFilter.tint(Color.White) else null
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
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
fun ProductItem(product: ItemsModel) {
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
            AsyncImage(
                model = product.imageRes,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⭐ ${product.rating}",
                fontSize = 12.sp,
                color = Color(0xFFFFC107)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$${product.price}",
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