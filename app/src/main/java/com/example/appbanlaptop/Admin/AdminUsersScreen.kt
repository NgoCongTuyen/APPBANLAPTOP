package com.example.appbanlaptop.Admin

import android.util.Log
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
import com.example.appbanlaptop.Model.User
import com.example.appbanlaptop.Model.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(modifier: Modifier = Modifier) {
    val users by UserManager.usersFlow.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var newUserUid by remember { mutableStateOf("") }
    var newUserDisplayName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf("user") }
    var editingUser by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var isAdmin by remember { mutableStateOf(false) }
    var isCheckingRole by remember { mutableStateOf(true) }

    // Kiểm tra vai trò admin
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            isCheckingRole = false
            errorMessage = "Vui lòng đăng nhập để tiếp tục"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        } else {
            FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.uid)
                .child("role")
                .get()
                .addOnSuccessListener { snapshot ->
                    isAdmin = snapshot.getValue(String::class.java) == "admin"
                    isCheckingRole = false
                    if (!isAdmin) {
                        errorMessage = "Bạn không có quyền admin"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    isCheckingRole = false
                    errorMessage = "Lỗi khi kiểm tra vai trò: ${it.message}"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
        }
    }

    // Tải danh sách người dùng
    LaunchedEffect(isAdmin, isCheckingRole) {
        if (!isCheckingRole && isAdmin) {
            UserManager.fetchAllUsers(
                onSuccess = { fetchedUsers ->
                    isLoading = false
                    Log.d("AdminUsersScreen", "Fetched ${fetchedUsers.size} users")
                    Toast.makeText(context, "Đã tải ${fetchedUsers.size} người dùng", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    isLoading = false
                    errorMessage = error
                    Log.e("AdminUsersScreen", "Error fetching users: $error")
                    Toast.makeText(context, "Lỗi khi tải người dùng: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Log khi danh sách người dùng thay đổi
    LaunchedEffect(users) {
        Log.d("AdminUsersScreen", "UI received ${users.size} users")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quản lý người dùng",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        when {
            isCheckingRole -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            !isAdmin || currentUser == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Bạn cần đăng nhập với tài khoản admin",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Lỗi: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                UserManager.fetchAllUsers(
                                    onSuccess = { fetchedUsers ->
                                        isLoading = false
                                        Toast.makeText(context, "Đã tải ${fetchedUsers.size} người dùng", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        isLoading = false
                                        errorMessage = error
                                        Toast.makeText(context, "Lỗi khi tải người dùng: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }
            users.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có người dùng nào",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
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
                                    text = "Thống kê",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Tổng số người dùng: ${users.size}",
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
                                        Toast.makeText(context, "Đã xóa người dùng: ${user.displayName}", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
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
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm người dùng")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm người dùng mới") },
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
                        label = { Text("Tên hiển thị") },
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
                        Text("Vai trò:", modifier = Modifier.align(Alignment.CenterVertically))
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
                                    Toast.makeText(context, "Đã thêm người dùng: $newUserDisplayName", Toast.LENGTH_SHORT).show()
                                    newUserUid = ""
                                    newUserDisplayName = ""
                                    newUserEmail = ""
                                    newUserRole = "user"
                                    showAddDialog = false
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showEditDialog && editingUser != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Chỉnh sửa người dùng") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUserDisplayName,
                        onValueChange = { newUserDisplayName = it },
                        label = { Text("Tên hiển thị") },
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
                        Text("Vai trò:", modifier = Modifier.align(Alignment.CenterVertically))
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
                                    Toast.makeText(context, "Đã cập nhật người dùng: $newUserDisplayName", Toast.LENGTH_SHORT).show()
                                    newUserUid = ""
                                    newUserDisplayName = ""
                                    newUserEmail = ""
                                    newUserRole = "user"
                                    editingUser = null
                                    showEditDialog = false
                                },
                                onError = { error ->
                                    Toast.makeText(context, "Lỗi: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Hủy")
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
                    text = user.displayName ?: "Không xác định",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = user.email ?: "Không có email",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Vai trò: ${user.role ?: "user"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa")
                }
            }
        }
    }
}