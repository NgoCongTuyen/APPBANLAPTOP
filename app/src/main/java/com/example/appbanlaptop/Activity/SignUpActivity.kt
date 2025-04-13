package com.example.appbanlaptop.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appbanlaptop.MainActivity
import com.example.appbanlaptop.ui.theme.APPBANLAPTOPTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : ComponentActivity() {
    private lateinit var viewModel: SignUpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)

        setContent {
            APPBANLAPTOPTheme {
                SignUpScreen(
                    viewModel = viewModel,
                    onSignUpSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onLoginClicked = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

class SignUpViewModel : ViewModel() {
    var username = mutableStateOf("")
    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var passwordVisible = mutableStateOf(false)
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun onSignUpClicked(onSignUpSuccess: () -> Unit) {
        if (username.value.isBlank() || email.value.isBlank() || password.value.isBlank()) {
            errorMessage.value = "Please fill in all fields"
            return
        }

        isLoading.value = true
        errorMessage.value = null

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.let {
                        // Cập nhật displayName cho FirebaseAuth user
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(username.value)
                            .build()
                        user.updateProfile(profileUpdates).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                // Lưu thông tin người dùng vào Realtime Database
                                val database = FirebaseDatabase.getInstance()
                                val userRef = database.getReference("users").child(it.uid)
                                val userData = mapOf(
                                    "username" to username.value,
                                    "email" to it.email,
                                    "displayName" to username.value,
                                    "createdAt" to System.currentTimeMillis()
                                )
                                userRef.setValue(userData)
                                    .addOnSuccessListener {
                                        onSignUpSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage.value = "Failed to save user data: ${e.message}"
                                    }
                            } else {
                                errorMessage.value = "Failed to update profile: ${updateTask.exception?.message}"
                            }
                        }
                    }
                } else {
                    errorMessage.value = task.exception?.message ?: "Sign Up failed"
                }
                isLoading.value = false
            }
    }

    fun onLoginClicked(onLoginClicked: () -> Unit) {
        onLoginClicked()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onSignUpSuccess: () -> Unit,
    onLoginClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign Up",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Blue,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = viewModel.username.value,
            onValueChange = { viewModel.username.value = it },
            label = { Text("Username") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Username Icon",
                    tint = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Blue,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Blue,
                unfocusedLabelColor = Color.Gray
            ),
            enabled = !viewModel.isLoading.value
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.email.value,
            onValueChange = { viewModel.email.value = it },
            label = { Text("Your email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = "Email Icon",
                    tint = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Blue,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Blue,
                unfocusedLabelColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !viewModel.isLoading.value
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.password.value,
            onValueChange = { viewModel.password.value = it },
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Password Icon",
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                IconButton(onClick = { viewModel.passwordVisible.value = !viewModel.passwordVisible.value }) {
                    Icon(
                        imageVector = if (viewModel.passwordVisible.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color.Gray
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            visualTransformation = if (viewModel.passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Blue,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Blue,
                unfocusedLabelColor = Color.Gray
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !viewModel.isLoading.value
        )
        Spacer(modifier = Modifier.height(16.dp))

        viewModel.errorMessage.value?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.onSignUpClicked(onSignUpSuccess) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            enabled = !viewModel.isLoading.value
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Sign Up",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Already have an account? Login",
            fontSize = 14.sp,
            color = Color.Blue,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.onLoginClicked(onLoginClicked) },
            textAlign = TextAlign.Center
        )
    }
}