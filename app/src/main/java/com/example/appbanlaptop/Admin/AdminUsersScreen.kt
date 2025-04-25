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
import com.example.appbanlaptop.Model.User
import com.example.appbanlaptop.Model.UserManager

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