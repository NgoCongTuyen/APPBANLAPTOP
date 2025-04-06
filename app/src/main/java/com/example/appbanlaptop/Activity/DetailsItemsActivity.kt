package com.example.appbanlaptop.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

class DetailsItemsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("PRODUCT_TITLE") ?: "Unknown Product"
        val description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "No description available"
        val price = intent.getStringExtra("PRODUCT_PRICE") ?: "0 đ" // Lấy giá trị String, mặc định là "0 VNĐ"
        val rating = intent.getDoubleExtra("PRODUCT_RATING", 0.0)
        val picUrl = intent.getStringExtra("PRODUCT_PIC_URL")
        val models = intent.getStringArrayExtra("PRODUCT_MODELS")?.toList() ?: emptyList()

        setContent {
            DetailsItemsScreen(
                title = title,
                description = description,
                price = price,
                rating = rating,
                picUrl = picUrl,
                models = models,
                onBackClick = { finish() }
            )
        }
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
    onBackClick: () -> Unit
) {
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = Color(0xFF6200EE),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hình ảnh sản phẩm
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                picUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                } ?: Text("No Image", color = Color.Gray, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tiêu đề
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Giá và đánh giá
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$price",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6200EE)
                )
                Text(
                    text = "⭐ $rating",
                    fontSize = 18.sp,
                    color = Color(0xFFFFC107)
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Mô tả
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

            Spacer(modifier = Modifier.height(16.dp))

            // Các mô hình (model)
            if (models.isNotEmpty()) {
                Text(
                    text = "Available Models",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(models) { model ->
                        Surface(
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
    }
}