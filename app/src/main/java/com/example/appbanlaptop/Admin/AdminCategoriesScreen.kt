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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.appbanlaptop.Model.Category
import com.example.appbanlaptop.Model.CategoryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCategoriesScreen(modifier: Modifier = Modifier) {
    val categories by CategoryManager.categoriesFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newCategoryTitle by remember { mutableStateOf("") }
    var newCategoryPicUrl by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                        text = "Total Categories: ${categories.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        items(categories, key = { it.id ?: it.hashCode() }) { category ->
            CategoryItem(
                category = category,
                onDelete = {
                    CategoryManager.removeCategory(
                        categoryId = category.id,
                        onSuccess = {
                            Toast.makeText(context, "Deleted category: ${category.title}", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = {
                    editingCategory = category
                    newCategoryTitle = category.title ?: ""
                    newCategoryPicUrl = category.picUrl ?: ""
                    showEditDialog = true
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showAddDialog = true }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Category")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryTitle,
                        onValueChange = { newCategoryTitle = it },
                        label = { Text("Category Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryPicUrl,
                        onValueChange = { newCategoryPicUrl = it },
                        label = { Text("Picture URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryTitle.isNotBlank() && newCategoryPicUrl.isNotBlank()) {
                            val category = Category(
                                title = newCategoryTitle,
                                picUrl = newCategoryPicUrl
                            )
                            CategoryManager.addCategory(
                                category = category,
                                onSuccess = {
                                    Toast.makeText(context, "Added category: $newCategoryTitle", Toast.LENGTH_SHORT).show()
                                    newCategoryTitle = ""
                                    newCategoryPicUrl = ""
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

    if (showEditDialog && editingCategory != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryTitle,
                        onValueChange = { newCategoryTitle = it },
                        label = { Text("Category Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryPicUrl,
                        onValueChange = { newCategoryPicUrl = it },
                        label = { Text("Picture URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryTitle.isNotBlank() && newCategoryPicUrl.isNotBlank()) {
                            val updatedCategory = editingCategory!!.copy(
                                title = newCategoryTitle,
                                picUrl = newCategoryPicUrl
                            )
                            CategoryManager.updateCategory(
                                category = updatedCategory,
                                onSuccess = {
                                    Toast.makeText(context, "Updated category: $newCategoryTitle", Toast.LENGTH_SHORT).show()
                                    newCategoryTitle = ""
                                    newCategoryPicUrl = ""
                                    editingCategory = null
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
fun CategoryItem(
    category: Category,
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
                    text = category.title ?: "Unknown Category",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = category.picUrl ?: "",
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