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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.appbanlaptop.Model.Product
import com.example.appbanlaptop.Model.ProductManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductScreen(modifier: Modifier = Modifier) {
    val products by ProductManager.productsFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Trạng thái loading và lỗi
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Theo dõi trạng thái loading và lỗi
    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("Products")
        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                errorMessage = error.message
            }
        })
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: $errorMessage",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        } else if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No products available",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(products, key = { it.id ?: it.hashCode().toString() }) { product ->
                    ProductCard(product = product)
                }
            }
        }

        // Nút thêm sản phẩm
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .alpha(if (isLoading || errorMessage != null) 0f else 1f)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    // Dialog thêm sản phẩm
    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onAddProduct = { newProduct ->
                ProductManager.addProduct(
                    product = newProduct,
                    onSuccess = {
                        Toast.makeText(context, "Added product: ${newProduct.title}", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    },
                    onError = { error ->
                        Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hiển thị hình ảnh (lấy hình đầu tiên từ picUrl)
            val imageUrl = product.picUrl?.firstOrNull()
            if (imageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = product.title,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 16.dp)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Image",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // Hiển thị tên và giá
            Column {
                Text(
                    text = product.title ?: "Unknown Product",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Price: $${product.price}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAddProduct: (Product) -> Unit
) {
    // Trạng thái cho các trường nhập liệu
    var categoryId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var picUrl by remember { mutableStateOf("") } // Nhập URL dưới dạng text, có thể cải thiện sau
    var rating by remember { mutableStateOf("") }
    var showRecommended by remember { mutableStateOf(false) }
    var model by remember { mutableStateOf("") } // Nhập model dưới dạng text, có thể cải thiện sau

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Add New Product") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = categoryId,
                    onValueChange = { categoryId = it },
                    label = { Text("Category ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = picUrl,
                    onValueChange = { picUrl = it },
                    label = { Text("Image URL (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rating,
                    onValueChange = { rating = it },
                    label = { Text("Rating (0-5)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showRecommended,
                        onCheckedChange = { showRecommended = it }
                    )
                    Text("Show Recommended")
                }
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Models (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && price.isNotBlank()) {
                        val newProduct = Product(
                            title = title,
                            price = price.toDoubleOrNull() ?: 0.0,
                            description = description,
                            picUrl = if (picUrl.isNotBlank()) picUrl.split(",").map { it.trim() } else emptyList(),
                            categoryId = categoryId,
                            rating = rating.toDoubleOrNull() ?: 0.0,
                            showRecommended = showRecommended,
                            model = if (model.isNotBlank()) model.split(",").map { it.trim() } else emptyList()
                        )
                        onAddProduct(newProduct)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}