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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import com.example.appbanlaptop.Model.CategoryModel
import com.example.appbanlaptop.Model.ItemsModel
import com.example.appbanlaptop.ViewModel.MainViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.database
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContent {
                MainActivityScreen()
        }
    }
}




@Composable
fun MainActivityScreen() {

    val categories = listOf(
        CategoryModel("Computer", R.drawable.cat1),
        CategoryModel("Phone", R.drawable.cat2),
        CategoryModel("Earphone", R.drawable.cat3), // Thêm dấu phẩy ở đây
        CategoryModel("Gaming", R.drawable.cat4),
        CategoryModel("Camera", R.drawable.cat5),
        CategoryModel("Clock", R.drawable.cat6)
    )
    val products = listOf(
        ItemsModel("Headphone 12A", 4.6f, 95.0, R.drawable.cat1_1),
        ItemsModel("Business Laptop", 4.7f, 550.0, R.drawable.cat2_1),
        ItemsModel("laptop B2003", 4.7f, 550.0, R.drawable.cat2_2),
        ItemsModel("laptop B2003", 4.7f, 550.0, R.drawable.cat2_3)
    )

    ConstraintLayout(modifier = Modifier.background(Color.White)) {
        val (scrollList) = createRefs()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(scrollList) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Welcome Back", color = Color.Black)

                        Text(
                            "Jackie", color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        Image(
                            painter = painterResource(R.drawable.fav_icon),
                            contentDescription = "fav icon"
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Image(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = "search icon",
                        )
                    }
                }
            }
            // Thêm Slider vào đây
            item {
                AutoSlidingCarousel()
            }
            item {
                SectionTitle("Categories", "See all")
            }
            item {
                CategoryList(categories)
            }
            item {
                SectionTitle("Products", "See all") // Đổi tiêu đề cho rõ ràng
            }
            item {
                ItemsList(products)
            }
        }
    }
}


///category+

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
    Row(
        modifier = Modifier
            .clickable(onClick = onItemClick)
            .background(
                color = if (isSelected) colorResource(R.color.purple) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = item.iconResId),
            contentDescription = item.title,
            modifier = Modifier
                .size(45.dp)
                .background(
                    color = if (isSelected) Color.Transparent else colorResource(R.color.grey),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentScale = ContentScale.Inside,
            colorFilter = if (isSelected) {
                ColorFilter.tint(Color.White)
            } else {
                ColorFilter.tint(Color.Black)
            }
        )
        if (isSelected) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, actionText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = actionText,
            color = colorResource(R.color.purple),
        )
    }
}



@Composable
fun ItemsList(products: List<ItemsModel>) {
    LazyRow (
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(products.size) { index ->
            ProductItem(product = products[index])
            Spacer(modifier = Modifier.height(16.dp)) // Thêm khoảng cách giữa các item
        }
    }
}


@Composable
fun ProductItem(product: ItemsModel) {
    Column(
        modifier = Modifier
            .height(225.dp)
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally // Căn giữa ảnh theo chiều ngang
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), // Giảm chiều cao để ảnh nhỏ hơn ô
            contentAlignment = Alignment.Center // Căn giữa ảnh trong Box
        ) {
            AsyncImage(
                model = product.imageRes,
                contentDescription = product.name,
                modifier = Modifier
                    .size(90.dp) // Giảm kích thước ảnh từ 120.dp → 90.dp
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp)) // Tạo khoảng cách giữa ảnh và nội dung
        Text(text = product.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "⭐ ${product.rating}", fontSize = 12.sp, color = Color.Yellow)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$${product.price}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
        }
    }
}






/// slide
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
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            count = banners.size,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) { page ->
            Image(
                painter = painterResource(id = banners[page]),
                contentDescription = "Banner Image",
                modifier = Modifier
                    .fillMaxSize()
            )
        }
        Spacer(modifier=Modifier.height(-10.dp))
        DotIndicator(totalDots = banners.size, selectedIndex = pagerState.currentPage)
    }
}

@Composable
fun DotIndicator(
    totalDots: Int,
    selectedIndex: Int,
    dotSize: Dp = 8.dp,
    selectedColor: Color = Color.Blue,
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
                    .size(if (index == selectedIndex) dotSize * 1f else dotSize) // Chấm lớn hơn khi được chọn
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




