package com.example.appbanlaptop.Activity

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.appbanlaptop.Model.ProductItem
import com.example.appbanlaptop.Model.ProductManager
import com.example.appbanlaptop.R
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductScreen(modifier: Modifier = Modifier) {
    val productsMap by ProductManager.itemsFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            ProductManager.cleanup()
            Log.d("AdminProductScreen", "Đã dọn dẹp listener")
        }
    }

    LaunchedEffect(productsMap) {
        Log.d("AdminProductScreen", "Products cập nhật: ${productsMap.size} sản phẩm")
        isLoading = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Thanh tìm kiếm
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Tìm kiếm sản phẩm") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                productsMap.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có sản phẩm nào",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                else -> {
                    val filteredProducts = productsMap.filter { (_, product) ->
                        product.title?.contains(searchQuery, ignoreCase = true) == true ||
                                product.categoryId?.contains(searchQuery, ignoreCase = true) == true
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(filteredProducts.toList(), key = { it.first }) { (key, product) ->
                            ProductCard(product = product, productKey = key)
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm sản phẩm")
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onAddProduct = { newProduct ->
                ProductManager.addProduct(
                    product = newProduct,
                    onSuccess = {
                        Toast.makeText(context, "Đã thêm sản phẩm: ${newProduct.title}", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onError = { error ->
                        Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAddProduct: (ProductItem) -> Unit
) {
    var categoryId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var picUrl by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var showRecommended by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Thêm sản phẩm mới") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = categoryId,
                        onValueChange = { categoryId = it },
                        label = { Text("ID danh mục") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tên sản phẩm") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Giá (VNĐ)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = picUrl,
                        onValueChange = { picUrl = it },
                        label = { Text("URL hình ảnh") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        label = { Text("Đánh giá (0-5)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isLoading
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showRecommended,
                            onCheckedChange = { showRecommended = it },
                            enabled = !isLoading
                        )
                        Text("Hiển thị đề xuất")
                    }
                }
                item {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Mẫu mã (phân cách bằng dấu phẩy)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isLoading) return@TextButton
                    if (title.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val cleanedPrice = price.replace("[,.\\s]".toRegex(), "")
                    if (price.isBlank() || !cleanedPrice.matches("\\d+".toRegex())) {
                        Toast.makeText(context, "Vui lòng nhập giá hợp lệ (ví dụ: 2375000 hoặc 2.375.000)", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val ratingDouble = rating.toDoubleOrNull()
                    if (rating.isNotBlank() && (ratingDouble == null || ratingDouble < 0 || ratingDouble > 5)) {
                        Toast.makeText(context, "Đánh giá phải là số từ 0 đến 5", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    isLoading = true
                    val formattedPrice = cleanedPrice.toLong().let { priceLong ->
                        String.format("%,d", priceLong).replace(",", ".")
                    }
                    val newProduct = ProductItem(
                        categoryId = categoryId,
                        title = title,
                        price = formattedPrice,
                        description = description,
                        picUrl = if (picUrl.isNotBlank()) picUrl.split(",").map { it.trim() } else emptyList(),
                        rating = ratingDouble,
                        showRecommended = showRecommended,
                        model = if (model.isNotBlank()) model.split(",").map { it.trim() } else emptyList()
                    )
                    onAddProduct(newProduct)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Thêm")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isLoading) onDismiss() },
                enabled = !isLoading
            ) {
                Text("Hủy")
            }
        }
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ProductCard(product: ProductItem, productKey: String) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hình ảnh sản phẩm
            if (!product.picUrl.isNullOrEmpty()) {
                val pagerState = rememberPagerState()
                HorizontalPager(
                    count = product.picUrl.size,
                    state = pagerState,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 16.dp),
                    contentPadding = PaddingValues(0.dp),
                    itemSpacing = 4.dp
                ) { page ->
                    val imageUrl = product.picUrl[page]
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = imageUrl,
                            placeholder = painterResource(R.drawable.loadding),
                            error = painterResource(R.drawable.error)
                        ),
                        contentDescription = product.title ?: "Sản phẩm",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 16.dp)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có ảnh",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Thông tin sản phẩm
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.title ?: "Sản phẩm không xác định",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Giá: ${product.price ?: "0"} VNĐ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Đánh giá: ${product.rating ?: 0.0}/5",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Danh mục: ${product.categoryId ?: "Không có"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Nút chỉnh sửa và xóa
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa sản phẩm")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa sản phẩm")
                }
            }
        }
    }

    // Dialog xác nhận xóa
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa sản phẩm '${product.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ProductManager.removeProduct(
                            productKey = productKey,
                            onSuccess = {
                                Toast.makeText(context, "Đã xóa sản phẩm: ${product.title}", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            },
                            onError = { error ->
                                Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showEditDialog) {
        EditProductDialog(
            product = product,
            productKey = productKey,
            onDismiss = { showEditDialog = false },
            onUpdateProduct = { updatedProduct ->
                ProductManager.updateProduct(
                    product = updatedProduct,
                    productKey = productKey,
                    onSuccess = {
                        Toast.makeText(context, "Đã cập nhật sản phẩm: ${updatedProduct.title}", Toast.LENGTH_SHORT).show()
                        showEditDialog = false
                    },
                    onError = { error ->
                        Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: ProductItem,
    productKey: String,
    onDismiss: () -> Unit,
    onUpdateProduct: (ProductItem) -> Unit
) {
    var categoryId by remember { mutableStateOf(product.categoryId ?: "") }
    var title by remember { mutableStateOf(product.title ?: "") }
    var price by remember { mutableStateOf(product.price ?: "") }
    var description by remember { mutableStateOf(product.description ?: "") }
    var picUrl by remember { mutableStateOf(product.picUrl?.joinToString(", ") ?: "") }
    var rating by remember { mutableStateOf(product.rating?.toString() ?: "") }
    var showRecommended by remember { mutableStateOf(product.showRecommended) }
    var model by remember { mutableStateOf(product.model?.joinToString(", ") ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Chỉnh sửa sản phẩm") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = categoryId,
                        onValueChange = { categoryId = it },
                        label = { Text("ID danh mục") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tên sản phẩm") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Giá (VNĐ)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = picUrl,
                        onValueChange = { picUrl = it },
                        label = { Text("URL hình ảnh (phân cách bằng dấu phẩy)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
                item {
                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        label = { Text("Đánh giá (0-5)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isLoading
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showRecommended,
                            onCheckedChange = { showRecommended = it },
                            enabled = !isLoading
                        )
                        Text("Hiển thị đề xuất")
                    }
                }
                item {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Mẫu mã (phân cách bằng dấu phẩy)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isLoading) return@TextButton
                    if (title.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val cleanedPrice = price.replace("[,.\\s]".toRegex(), "")
                    if (price.isBlank() || !cleanedPrice.matches("\\d+".toRegex())) {
                        Toast.makeText(context, "Vui lòng nhập giá hợp lệ (ví dụ: 2375000 hoặc 2.375.000)", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val ratingDouble = rating.toDoubleOrNull()
                    if (rating.isNotBlank() && (ratingDouble == null || ratingDouble < 0 || ratingDouble > 5)) {
                        Toast.makeText(context, "Đánh giá phải là số từ 0 đến 5", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    isLoading = true
                    val formattedPrice = cleanedPrice.toLong().let { priceLong ->
                        String.format("%,d", priceLong).replace(",", ".")
                    }
                    val updatedProduct = ProductItem(
                        categoryId = categoryId,
                        title = title,
                        price = formattedPrice,
                        description = description,
                        picUrl = if (picUrl.isNotBlank()) picUrl.split(",").map { it.trim() } else emptyList(),
                        rating = ratingDouble,
                        showRecommended = showRecommended,
                        model = if (model.isNotBlank()) model.split(",").map { it.trim() } else emptyList()
                    )
                    onUpdateProduct(updatedProduct)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Cập nhật")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isLoading) onDismiss() },
                enabled = !isLoading
            ) {
                Text("Hủy")
            }
        }
    )
}