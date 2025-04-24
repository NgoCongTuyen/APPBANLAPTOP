package com.example.appbanlaptop.Admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.appbanlaptop.Model.Product
import com.example.appbanlaptop.Model.ProductManager
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen() {
    val products by ProductManager.productsFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }
    var newProductPrice by remember { mutableStateOf("") }
    var newProductDescription by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    val context = LocalContext.current

    // Thống kê
    val totalProducts by remember(products) { derivedStateOf { products.size } }
    val totalValue by remember(products) { derivedStateOf { products.sumOf { it.price } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Management") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hiển thị thống kê
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Statistics",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Total Products: $totalProducts",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Total Inventory Value: $${totalValue}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Danh sách sản phẩm
            items(products, key = { it.id ?: it.hashCode().toString() }) { product ->
                ProductItem(
                    product = product,
                    onDelete = {
                        ProductManager.removeProduct(
                            productId = product.id,
                            onSuccess = {
                                Toast.makeText(context, "Deleted product: ${product.title}", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onEdit = {
                        editingProduct = product
                        newProductName = product.title ?: ""
                        newProductPrice = product.price.toString()
                        newProductDescription = product.description ?: ""
                        showEditDialog = true
                    }
                )
            }
        }
    }

    // Dialog thêm sản phẩm
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Product") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProductPrice,
                        onValueChange = { newProductPrice = it },
                        label = { Text("Price") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProductDescription,
                        onValueChange = { newProductDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProductName.isNotBlank() && newProductPrice.isNotBlank()) {
                            val price = newProductPrice.toDoubleOrNull() ?: 0.0
                            val product = Product(
                                title = newProductName,
                                price = price,
                                description = newProductDescription
                            )
                            ProductManager.addProduct(
                                product = product,
                                onSuccess = {
                                    Toast.makeText(context, "Added product: $newProductName", Toast.LENGTH_SHORT).show()
                                    newProductName = ""
                                    newProductPrice = ""
                                    newProductDescription = ""
                                    showAddDialog = false
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog chỉnh sửa sản phẩm
    if (showEditDialog && editingProduct != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Product") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProductPrice,
                        onValueChange = { newProductPrice = it },
                        label = { Text("Price") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProductDescription,
                        onValueChange = { newProductDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProductName.isNotBlank() && newProductPrice.isNotBlank()) {
                            val price = newProductPrice.toDoubleOrNull() ?: 0.0
                            val updatedProduct = editingProduct!!.copy(
                                title = newProductName,
                                price = price,
                                description = newProductDescription
                            )
                            ProductManager.updateProduct(
                                product = updatedProduct,
                                onSuccess = {
                                    Toast.makeText(context, "Updated product: $newProductName", Toast.LENGTH_SHORT).show()
                                    newProductName = ""
                                    newProductPrice = ""
                                    newProductDescription = ""
                                    editingProduct = null
                                    showEditDialog = false
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title ?: "Unknown Product",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${product.price}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = product.description ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}