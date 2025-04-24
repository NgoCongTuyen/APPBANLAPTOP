package com.example.appbanlaptop.Admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.appbanlaptop.Activity.LoginActivity
import com.example.appbanlaptop.Model.*
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            APPBANLAPTOPTheme {
                AdminDashboard()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ProductManager.cleanup()
        UserManager.cleanup()
        CategoryManager.cleanup()
        OrderManager.cleanup()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Items", "Users", "Categories", "Revenue")
    val context = LocalContext.current // Lấy context ở đây để sử dụng trong TopAppBar

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        // Đăng xuất khỏi Firebase Authentication
                        FirebaseAuth.getInstance().signOut()
                        // Điều hướng về LoginActivity
                        context.startActivity(Intent(context, LoginActivity::class.java))
                        // Đóng AdminActivity
                        (context as Activity).finish()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.ShoppingCart, contentDescription = null)
                                1 -> Icon(Icons.Default.Person, contentDescription = null)
                                2 -> Icon(Icons.Default.Category, contentDescription = null)
                                3 -> Icon(Icons.Default.Money, contentDescription = null)
                            }
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> AdminItemsScreen(Modifier.padding(padding))
            1 -> AdminUsersScreen(Modifier.padding(padding))
            2 -> AdminCategoriesScreen(Modifier.padding(padding))
            3 -> AdminRevenueScreen(Modifier.padding(padding))
        }
    }
}

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

    val totalItems by remember(items) { derivedStateOf { items.size } }
    val totalValue by remember(items) { derivedStateOf { items.sumOf { it.price ?: 0.0 } } }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showAddDialog = true }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(modifier: Modifier = Modifier) {
    val users by UserManager.usersFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newUserUid by remember { mutableStateOf("") }
    var newUserDisplayName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf("user") }
    var editingUser by remember { mutableStateOf<User?>(null) }
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
                        text = "Total Users: ${users.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        items(users, key = { it.uid ?: it.hashCode().toString() }) { user ->
            UserItem(
                user = user,
                onDelete = {
                    UserManager.removeUser(
                        userId = user.uid,
                        onSuccess = {
                            Toast.makeText(context, "Deleted user: ${user.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onEdit = {
                    editingUser = user
                    newUserUid = user.uid ?: ""
                    newUserDisplayName = user.displayName ?: ""
                    newUserEmail = user.email ?: ""
                    newUserRole = user.role ?: "user"
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
            Icon(Icons.Default.Add, contentDescription = "Add User")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New User") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUserUid,
                        onValueChange = { newUserUid = it },
                        label = { Text("User UID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUserDisplayName,
                        onValueChange = { newUserDisplayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Role:", modifier = Modifier.align(Alignment.CenterVertically))
                        RadioButton(
                            selected = newUserRole == "user",
                            onClick = { newUserRole = "user" }
                        )
                        Text("User", modifier = Modifier.align(Alignment.CenterVertically))
                        RadioButton(
                            selected = newUserRole == "admin",
                            onClick = { newUserRole = "admin" }
                        )
                        Text("Admin", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUserUid.isNotBlank() && newUserDisplayName.isNotBlank() && newUserEmail.isNotBlank()) {
                            val user = User(
                                uid = newUserUid,
                                displayName = newUserDisplayName,
                                email = newUserEmail,
                                role = newUserRole,
                                createdAt = System.currentTimeMillis()
                            )
                            UserManager.addUser(
                                user = user,
                                onSuccess = {
                                    Toast.makeText(context, "Added user: $newUserDisplayName", Toast.LENGTH_SHORT).show()
                                    newUserUid = ""
                                    newUserDisplayName = ""
                                    newUserEmail = ""
                                    newUserRole = "user"
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

    if (showEditDialog && editingUser != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit User") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUserDisplayName,
                        onValueChange = { newUserDisplayName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Role:", modifier = Modifier.align(Alignment.CenterVertically))
                        RadioButton(
                            selected = newUserRole == "user",
                            onClick = { newUserRole = "user" }
                        )
                        Text("User", modifier = Modifier.align(Alignment.CenterVertically))
                        RadioButton(
                            selected = newUserRole == "admin",
                            onClick = { newUserRole = "admin" }
                        )
                        Text("Admin", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUserDisplayName.isNotBlank() && newUserEmail.isNotBlank()) {
                            val updatedUser = editingUser!!.copy(
                                displayName = newUserDisplayName,
                                email = newUserEmail,
                                role = newUserRole
                            )
                            UserManager.updateUser(
                                user = updatedUser,
                                onSuccess = {
                                    Toast.makeText(context, "Updated user: $newUserDisplayName", Toast.LENGTH_SHORT).show()
                                    newUserUid = ""
                                    newUserDisplayName = ""
                                    newUserEmail = ""
                                    newUserRole = "user"
                                    editingUser = null
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
fun UserItem(
    user: User,
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
                    text = user.displayName ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.email ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Role: ${user.role}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRevenueScreen(modifier: Modifier = Modifier) {
    val orders by OrderManager.ordersFlow.collectAsState()
    val totalRevenue by remember(orders) { derivedStateOf { orders.sumOf { it.totalPrice ?: 0.0 } } }
    val totalOrders by remember(orders) { derivedStateOf { orders.size } }
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
                        text = "Revenue Statistics",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Total Orders: $totalOrders",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Total Revenue: $${totalRevenue}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        items(orders, key = { it.id ?: it.hashCode().toString() }) { order ->
            OrderItem(order = order)
        }
    }
}

@Composable
fun OrderItem(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Order ID: ${order.id}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "User ID: ${order.userId}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Total: $${order.totalPrice}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Status: ${order.status}",
                style = MaterialTheme.typography.bodySmall
            )
            order.items?.forEach { item ->
                Text(
                    text = "${item.title}: ${item.quantity} x $${item.price}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}