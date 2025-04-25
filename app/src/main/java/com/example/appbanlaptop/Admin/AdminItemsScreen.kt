package com.example.appbanlaptop.Admin

import android.widget.Toast
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appbanlaptop.Model.Product
import com.example.appbanlaptop.Model.ProductManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminItemsScreen(modifier: Modifier = Modifier) {
    val items by ProductManager.productsFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var newItemDescription by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<Product?>(null) }
    val context = LocalContext.current

    // Trạng thái loading và lỗi
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Theo dõi trạng thái loading và lỗi
    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("Items") // Thay đổi từ "Products" thành "Items"
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

    val totalItems by remember(items) { derivedStateOf { items.size } }
    val totalValue by remember(items) { derivedStateOf { items.sumOf { it.price ?: 0.0 } } }

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
        } else if (items.isEmpty()) {
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
                                text = "Total Items: $totalItems",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Total Inventory Value: $${totalValue}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                items(items, key = { it.id ?: it.hashCode().toString() }) { item ->
                    ItemItem(
                        item = item,
                        onDelete = {
                            ProductManager.removeProduct(
                                productId = item.id,
                                onSuccess = {
                                    Toast.makeText(context, "Deleted item: ${item.title}", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        onEdit = {
                            editingItem = item
                            newItemName = item.title ?: ""
                            newItemPrice = item.price.toString()
                            newItemDescription = item.description ?: ""
                            showEditDialog = true
                        }
                    )
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
            Icon(Icons.Default.Add, contentDescription = "Add Item")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Item") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it },
                        label = { Text("Price") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemDescription,
                        onValueChange = { newItemDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newItemName.isNotBlank() && newItemPrice.isNotBlank()) {
                            val price = newItemPrice.toDoubleOrNull() ?: 0.0
                            val item = Product(
                                title = newItemName,
                                price = price,
                                description = newItemDescription
                            )
                            ProductManager.addProduct(
                                product = item,
                                onSuccess = {
                                    Toast.makeText(context, "Added item: $newItemName", Toast.LENGTH_SHORT).show()
                                    newItemName = ""
                                    newItemPrice = ""
                                    newItemDescription = ""
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

    if (showEditDialog && editingItem != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Item") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it },
                        label = { Text("Price") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemDescription,
                        onValueChange = { newItemDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newItemName.isNotBlank() && newItemPrice.isNotBlank()) {
                            val price = newItemPrice.toDoubleOrNull() ?: 0.0
                            val updatedItem = editingItem!!.copy(
                                title = newItemName,
                                price = price,
                                description = newItemDescription
                            )
                            ProductManager.updateProduct(
                                product = updatedItem,
                                onSuccess = {
                                    Toast.makeText(context, "Updated item: $newItemName", Toast.LENGTH_SHORT).show()
                                    newItemName = ""
                                    newItemPrice = ""
                                    newItemDescription = ""
                                    editingItem = null
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
fun ItemItem(
    item: Product,
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
                    text = item.title ?: "Unknown Item",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${item.price}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = item.description ?: "",
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