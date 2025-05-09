package com.example.appbanlaptop.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appbanlaptop.MainActivity
import com.example.appbanlaptop.Model.CartItem
import com.example.appbanlaptop.R
import com.example.appbanlaptop.ui.theme.PaymentTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.text.DecimalFormat
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import com.example.appbanlaptop.Activity.ThemeManager
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

data class Province(
    val name: String,
    val code: String,
    val districts: List<District>
)

data class District(
    val name: String,
    val code: String,
    val wards: List<Ward>
)

data class Ward(
    val name: String,
    val code: String
)

// Định nghĩa nguồn dữ liệu chung
object AddressState {
    val newAddress = mutableStateOf<Address?>(null)
    val addresses = mutableStateListOf<Address>()
    val selectedAddress = mutableStateOf<Address?>(null) // Thêm trạng thái cho địa chỉ được chọn

    fun loadAddresses(userId: String, onComplete: () -> Unit = {}) {
        val database = FirebaseDatabase.getInstance()
        val addressesRef = database.getReference("addresses").child(userId)

        addressesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                addresses.clear()
                for (addressSnapshot in snapshot.children) {
                    try {
                        val addressMap = addressSnapshot.value as? Map<String, Any>
                        addressMap?.let {
                            val address = Address(
                                name = it["name"] as? String ?: "",
                                phone = it["phone"] as? String ?: "",
                                addressDetail = it["addressDetail"] as? String ?: "",
                                isDefault = it["isDefault"] as? Boolean ?: false
                            )
                            addresses.add(address)
                        }
                    } catch (e: Exception) {
                        Log.e("AddressState", "Error parsing address: ${e.message}")
                    }
                }
                onComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AddressState", "Error loading addresses: ${error.message}")
                onComplete()
            }
        })
    }

    fun selectAddress(address: Address?) {
        selectedAddress.value = address
    }

    fun saveAddress(userId: String, address: Address, onComplete: () -> Unit = {}) {
        val database = FirebaseDatabase.getInstance()
        val addressesRef = database.getReference("addresses").child(userId)

        // Nếu địa chỉ mới là mặc định, cập nhật tất cả địa chỉ khác thành không mặc định
        if (address.isDefault) {
            addressesRef.get().addOnSuccessListener { snapshot ->
                for (addressSnapshot in snapshot.children) {
                    addressSnapshot.ref.child("isDefault").setValue(false)
                }
                saveNewAddress(addressesRef, address, onComplete)
            }
        } else {
            saveNewAddress(addressesRef, address, onComplete)
        }
    }

    private fun saveNewAddress(addressesRef: DatabaseReference, address: Address, onComplete: () -> Unit) {
        val newAddressRef = addressesRef.push()
        val addressMap = mapOf(
            "name" to address.name,
            "phone" to address.phone,
            "addressDetail" to address.addressDetail,
            "isDefault" to address.isDefault
        )

        newAddressRef.setValue(addressMap)
            .addOnSuccessListener {
                Log.d("AddressState", "Address saved successfully")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("AddressState", "Error saving address: ${e.message}")
                onComplete()
            }
    }

    fun updateAddress(userId: String, oldAddress: Address, newAddress: Address, onComplete: () -> Unit = {}) {
        val database = FirebaseDatabase.getInstance()
        val addressesRef = database.getReference("addresses").child(userId)

        if (newAddress.isDefault) {
            addressesRef.get().addOnSuccessListener { snapshot ->
                for (addressSnapshot in snapshot.children) {
                    addressSnapshot.ref.child("isDefault").setValue(false)
                }
                updateExistingAddress(addressesRef, oldAddress, newAddress, onComplete)
            }
        } else {
            updateExistingAddress(addressesRef, oldAddress, newAddress, onComplete)
        }
    }

    private fun updateExistingAddress(addressesRef: DatabaseReference, oldAddress: Address, newAddress: Address, onComplete: () -> Unit) {
        addressesRef.orderByChild("addressDetail").equalTo(oldAddress.addressDetail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (addressSnapshot in snapshot.children) {
                        val addressMap = mapOf(
                            "name" to newAddress.name,
                            "phone" to newAddress.phone,
                            "addressDetail" to newAddress.addressDetail,
                            "isDefault" to newAddress.isDefault
                        )
                        addressSnapshot.ref.setValue(addressMap)
                            .addOnSuccessListener {
                                Log.d("AddressState", "Address updated successfully")
                                onComplete()
                            }
                            .addOnFailureListener { e ->
                                Log.e("AddressState", "Error updating address: ${e.message}")
                                onComplete()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddressState", "Error finding address to update: ${error.message}")
                    onComplete()
                }
            })
    }

    fun deleteAddress(userId: String, address: Address, onComplete: () -> Unit = {}) {
        val database = FirebaseDatabase.getInstance()
        val addressesRef = database.getReference("addresses").child(userId)

        addressesRef.orderByChild("addressDetail").equalTo(address.addressDetail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (addressSnapshot in snapshot.children) {
                        addressSnapshot.ref.removeValue()
                            .addOnSuccessListener {
                                Log.d("AddressState", "Address deleted successfully")
                                addresses.remove(address)
                                // Nếu địa chỉ bị xóa là địa chỉ đang được chọn, chọn địa chỉ mặc định hoặc đầu tiên
                                if (selectedAddress.value == address) {
                                    selectAddress(
                                        addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
                                    )
                                }
                                onComplete()
                            }
                            .addOnFailureListener { e ->
                                Log.e("AddressState", "Error deleting address: ${e.message}")
                                onComplete()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AddressState", "Error finding address to delete: ${error.message}")
                    onComplete()
                }
            })
    }
}

class PaymentActivity : ComponentActivity() {
    private var isDarkMode = mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDarkMode.value = ThemeManager.isDarkMode(this)
        // Nhận dữ liệu từ Intent
        val checkoutItems = intent.getParcelableArrayListExtra<CartItem>("CHECKOUT_ITEMS") ?: emptyList()
        val totalPrice = intent.getDoubleExtra("TOTAL_PRICE", 0.0)
        Log.d("PaymentActivity", "Received items: size=${checkoutItems.size}, items=$checkoutItems, totalPrice=$totalPrice")

        setContent {
            PaymentTheme(darkTheme = isDarkMode.value) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "payment") {
                    composable("payment") {
                        PaymentScreen(navController, checkoutItems, totalPrice)
                    }
                    composable("address_list") {
                        AddressListScreen(navController)
                    }
                    composable("add_address") {
                        AddAddressScreen(navController, null) // Thêm mới
                    }
                    composable("edit_address/{index}") { backStackEntry ->
                        val index = backStackEntry.arguments?.getString("index")?.toIntOrNull()
                        val addressToEdit = index?.let { AddressState.addresses.getOrNull(it) }
                        AddAddressScreen(navController, addressToEdit) // Chỉnh sửa
                    }
                    composable("order_success") {
                        OrderSuccessScreen(
                            navController = navController,
                            products = checkoutItems,
                            totalPrice = totalPrice
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDarkMode.value = ThemeManager.isDarkMode(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController, checkoutItems: List<CartItem>, totalPriceFromIntent: Double) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            AddressState.loadAddresses(userId) {
                if (AddressState.selectedAddress.value == null) {
                    AddressState.selectAddress(
                        AddressState.addresses.firstOrNull { it.isDefault } ?: AddressState.addresses.firstOrNull()
                    )
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    val backStackEntry = navController.currentBackStackEntry
    val selectedFromList = backStackEntry?.savedStateHandle?.get<Address>("selected_address")

    LaunchedEffect(selectedFromList) {
        selectedFromList?.let { address ->
            AddressState.selectAddress(address)
            backStackEntry.savedStateHandle.remove<Address>("selected_address")
        }
    }

    AddressState.newAddress.value?.let { newAddress ->
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            AddressState.saveAddress(userId, newAddress) {
                AddressState.addresses.add(newAddress)
                AddressState.selectAddress(newAddress)
                AddressState.newAddress.value = null
            }
        }
    }

    val products = remember(checkoutItems) {
        mutableStateListOf<Product>().apply {
            addAll(checkoutItems.map { Product.fromCartItem(it) })
        }
    }

    val totalPrice by remember(products) {
        derivedStateOf {
            products.sumOf { product ->
                product.price.toInt() * product.quantity
            }.toDouble()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thanh toán",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        (navController.context as? ComponentActivity)?.finish()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            TotalAndCheckoutButton(
                productsSize = products.size,
                totalPrice = totalPrice,
                products = products,
                selectedAddress = AddressState.selectedAddress.value,
                navController = navController
            )
        }
    ) { paddingValues ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không có sản phẩm để thanh toán",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("address_list") }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Địa chỉ giao hàng",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit, // Sử dụng biểu tượng Code từ Icons.Default
                                    contentDescription = "Code Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            AddressState.selectedAddress.value?.let { address ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle, // Sử dụng biểu tượng Code từ Icons.Default
                                        contentDescription = "Code Icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.name,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone, // Sử dụng biểu tượng Code từ Icons.Default
                                        contentDescription = "Phone Icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.phone,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation, // Sử dụng biểu tượng Code từ Icons.Default
                                        contentDescription = "Adress Icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.addressDetail,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            } ?: Text(
                                text = "Vui lòng chọn địa chỉ giao hàng",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ĐPT STORE",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(products) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(product.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = product.name,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = product.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${DecimalFormat("#,###").format(product.price.toInt())}đ",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (product.quantity > 1) products[products.indexOf(product)] = product.copy(quantity = product.quantity - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("-", color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = "${product.quantity}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { products[products.indexOf(product)] = product.copy(quantity = product.quantity + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("+", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Phương thức thanh toán",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = true,
                                        onClick = { },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Thanh toán khi nhận hàng",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShippingInfo(selectedAddress: Address?, onAddressClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 8.dp)
    ) {
        TextField(
            value = selectedAddress?.let { "${it.name}\n${it.phone}\n${it.addressDetail}" } ?: "Chưa chọn địa chỉ",
            onValueChange = { /* Không cho chỉnh sửa trực tiếp */ },
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .clickable { onAddressClick() },
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(navController: NavController) {
    var showDeleteDialog by remember { mutableStateOf<Address?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Địa chỉ của bạn",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate("add_address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "Add Address",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Thêm địa chỉ mới",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (AddressState.addresses.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "No Address",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bạn chưa có địa chỉ nào",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hãy thêm địa chỉ để tiếp tục",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(AddressState.addresses) { index, address ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_address", address)
                                        navController.popBackStack()
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Name",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = address.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (address.isDefault) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Mặc định",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.phone,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation,
                                        contentDescription = "Address",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.addressDetail,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            navController.navigate("edit_address/$index")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Chỉnh sửa",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { showDeleteDialog = address }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Xóa",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            showDeleteDialog?.let { address ->
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = {
                        Text(
                            "Xác nhận xóa",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            "Bạn có chắc chắn muốn xóa địa chỉ này?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                    AddressState.deleteAddress(userId, address) {
                                        showDeleteDialog = null
                                        android.widget.Toast.makeText(context, "Đã xóa địa chỉ", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Vui lòng đăng nhập để xóa địa chỉ", android.widget.Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = null
                                }
                            }
                        ) {
                            Text(
                                "Xóa",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = null }
                        ) {
                            Text(
                                "Hủy",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(navController: NavController, addressToEdit: Address?) {
    var newName by remember { mutableStateOf(addressToEdit?.name ?: "") }
    var newPhone by remember { mutableStateOf(addressToEdit?.phone ?: "") }
    var newAddressDetail by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(0) ?: "") }
    var isDefault by remember { mutableStateOf(addressToEdit?.isDefault ?: false) }
    var isLoading by remember { mutableStateOf(false) }

    var provinces by remember { mutableStateOf<List<Province>>(emptyList()) }
    var districts by remember { mutableStateOf<List<District>>(emptyList()) }
    var wards by remember { mutableStateOf<List<Ward>>(emptyList()) }

    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var selectedDistrict by remember { mutableStateOf<District?>(null) }
    var selectedWard by remember { mutableStateOf<Ward?>(null) }

    var expandedProvince by remember { mutableStateOf(false) }
    var expandedDistrict by remember { mutableStateOf(false) }
    var expandedWard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://provinces.open-api.vn/api/?depth=3")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AddAddressScreen", "Error loading provinces: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    val type = object : TypeToken<List<Province>>() {}.type
                    val loadedProvinces = Gson().fromJson<List<Province>>(jsonString, type)
                    provinces = loadedProvinces
                }
            }
        })
    }

    LaunchedEffect(selectedProvince) {
        districts = selectedProvince?.districts ?: emptyList()
        selectedDistrict = null
        selectedWard = null
    }

    LaunchedEffect(selectedDistrict) {
        wards = selectedDistrict?.wards ?: emptyList()
        selectedWard = null
    }

    val isNameValid = newName.isNotBlank()
    val isPhoneValid = newPhone.isNotBlank() && newPhone.length == 10 && newPhone.startsWith("0") && newPhone.all { it.isDigit() }
    val isProvinceValid = selectedProvince != null
    val isDistrictValid = selectedDistrict != null
    val isWardValid = selectedWard != null
    val isAddressDetailValid = newAddressDetail.isNotBlank()
    val isFormValid = isNameValid && isPhoneValid && isProvinceValid && isDistrictValid && isWardValid && isAddressDetailValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (addressToEdit == null) "Thêm địa chỉ mới" else "Chỉnh sửa địa chỉ",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    isLoading = true
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId == null) {
                        android.widget.Toast.makeText(navController.context, "Vui lòng đăng nhập để thêm địa chỉ", android.widget.Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@Button
                    }

                    val fullAddress = "$newAddressDetail, ${selectedWard?.name}, ${selectedDistrict?.name}, ${selectedProvince?.name}"
                    val updatedAddress = Address(
                        name = newName,
                        phone = newPhone,
                        addressDetail = fullAddress,
                        isDefault = isDefault
                    )

                    if (addressToEdit == null) {
                        AddressState.saveAddress(userId, updatedAddress) {
                            AddressState.addresses.add(updatedAddress)
                            AddressState.selectAddress(updatedAddress)
                            isLoading = false
                            navController.popBackStack("payment", inclusive = false)
                        }
                    } else {
                        AddressState.updateAddress(userId, addressToEdit, updatedAddress) {
                            val index = AddressState.addresses.indexOf(addressToEdit)
                            if (index != -1) {
                                AddressState.addresses[index] = updatedAddress
                            }
                            AddressState.selectAddress(updatedAddress)
                            isLoading = false
                            navController.popBackStack("address_list", inclusive = false)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "Lưu",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Tên người nhận") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Name",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        if (!isNameValid && newName.isEmpty()) {
                            Text(
                                text = "Vui lòng nhập tên",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Số điện thoại") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        if (!isPhoneValid) {
                            Text(
                                text = when {
                                    newPhone.isEmpty() -> "Vui lòng nhập số điện thoại"
                                    newPhone.length != 10 || !newPhone.startsWith("0") || !newPhone.all { it.isDigit() } -> "Số điện thoại không hợp lệ"
                                    else -> ""
                                },
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedProvince,
                            onExpandedChange = { expandedProvince = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedProvince?.name ?: "Chọn tỉnh/thành phố",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tỉnh/Thành phố") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProvince) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation,
                                        contentDescription = "Province",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedProvince,
                                onDismissRequest = { expandedProvince = false }
                            ) {
                                provinces.forEach { province ->
                                    DropdownMenuItem(
                                        text = { Text(province.name) },
                                        onClick = {
                                            selectedProvince = province
                                            expandedProvince = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedDistrict,
                            onExpandedChange = { expandedDistrict = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedDistrict?.name ?: "Chọn quận/huyện",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Quận/Huyện") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistrict) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation,
                                        contentDescription = "District",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDistrict,
                                onDismissRequest = { expandedDistrict = false }
                            ) {
                                districts.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text(district.name) },
                                        onClick = {
                                            selectedDistrict = district
                                            expandedDistrict = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedWard,
                            onExpandedChange = { expandedWard = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedWard?.name ?: "Chọn phường/xã",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Phường/Xã") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWard) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation,
                                        contentDescription = "Ward",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedWard,
                                onDismissRequest = { expandedWard = false }
                            ) {
                                wards.forEach { ward ->
                                    DropdownMenuItem(
                                        text = { Text(ward.name) },
                                        onClick = {
                                            selectedWard = ward
                                            expandedWard = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newAddressDetail,
                            onValueChange = { newAddressDetail = it },
                            label = { Text("Địa chỉ cụ thể") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AddLocation,
                                    contentDescription = "Address",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            "Đặt làm địa chỉ mặc định",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoreHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ĐPT STORE",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProductItem(product: Product, onQuantityChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hiển thị hình ảnh sản phẩm
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (product.imageUrl != null && product.imageUrl.isNotEmpty()) {
                var isImageLoading by remember(product.imageUrl) { mutableStateOf(true) }
                var isImageError by remember(product.imageUrl) { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.loadding),
                    error = painterResource(R.drawable.error),
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false; isImageError = false },
                    onError = { isImageLoading = false; isImageError = true }
                )

                if (isImageLoading) {
                    Text(
                        text = "Đang tải...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                } else if (isImageError) {
                    Text(
                        text = "Lỗi",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(2.dp)
                    )
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.cat1),
                    contentDescription = "Default Product Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.color,
                color = Color(0xFFFFD700),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(MaterialTheme.colorScheme.background, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = "${DecimalFormat("#,###").format(product.price.toInt())}đ",
                color = Color(0xFFFF0000),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = "-",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity - 1) }
            )
            Text(
                text = product.quantity.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity + 1) }
            )
        }
    }
}

@Composable
fun PaymentMethod() {
    var selectedMethod by remember { mutableStateOf("Thanh toán khi nhận hàng") }
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Text(
            text = "Phương thức thanh toán",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        PaymentOption("Thanh toán khi nhận hàng", selectedMethod == "Thanh toán khi nhận hàng") { selectedMethod = "Thanh toán khi nhận hàng" }
//        PaymentOption("Thẻ tín dụng", selectedMethod == "Thẻ tín dụng") { selectedMethod = "Thẻ tín dụng" }
//        PaymentOption("Ví Momo", selectedMethod == "Ví Momo") { selectedMethod = "Ví Momo" }
//        PaymentOption("ZaloPay", selectedMethod == "ZaloPay") { selectedMethod = "ZaloPay" }
    }
}

@Composable
fun PaymentOption(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = if (isSelected) Color(0xFFFF0000) else MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TotalAndCheckoutButton(
    productsSize: Int,
    totalPrice: Double,
    products: List<Product>,
    selectedAddress: Address?,
    navController: NavController
) {
    val formatter = DecimalFormat("#,###")
    val formattedTotal = "${formatter.format(totalPrice.toInt())}đ"
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "$productsSize mặt hàng, tổng cộng",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            Text(
                text = formattedTotal,
                color = Color(0xFFFF0000),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = {
                if (selectedAddress == null) {
                    android.widget.Toast.makeText(context, "Vui lòng chọn địa chỉ giao hàng", android.widget.Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    android.widget.Toast.makeText(context, "Vui lòng đăng nhập để đặt hàng", android.widget.Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val timestamp = System.currentTimeMillis()
                val random = (Math.random() * 1000).toInt()
                val orderId = "${timestamp.toString().takeLast(6)}${random.toString().padStart(3, '0')}"

                // Tính toán totalPrice từ danh sách products
                val calculatedTotalPrice = products.sumOf { product ->
                    try {
                        product.price.toDouble() * product.quantity
                    } catch (e: NumberFormatException) {
                        Log.e("TotalAndCheckoutButton", "Invalid price format for product: ${product.name}, price=${product.price}")
                        0.0
                    }
                }

                val order = mapOf(
                    "orderId" to orderId,
                    "address" to mapOf(
                        "name" to selectedAddress.name,
                        "phone" to selectedAddress.phone,
                        "addressDetail" to selectedAddress.addressDetail,
                        "isDefault" to selectedAddress.isDefault
                    ),
                    "products" to products.map { product ->
                        mapOf(
                            "name" to product.name,
                            "color" to product.color,
                            "price" to product.price,
                            "quantity" to product.quantity,
                            "imageUrl" to product.imageUrl
                        )
                    },
                    "totalPrice" to calculatedTotalPrice, // Đảm bảo lưu totalPrice
                    "status" to "pending",
                    "createdAt" to timestamp
                )

                val database = FirebaseDatabase.getInstance()
                val ordersRef = database.getReference("orders").child(userId)
                val newOrderRef = ordersRef.child(orderId)

                newOrderRef.setValue(order)
                    .addOnSuccessListener {
                        val cartRef = database.getReference("Cart").child(userId).child("items")
                        products.forEach { product ->
                            cartRef.orderByChild("title").equalTo(product.name)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (itemSnapshot in snapshot.children) {
                                            itemSnapshot.ref.removeValue()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("TotalAndCheckoutButton", "Error removing cart item: ${error.message}")
                                    }
                                })
                        }

                        android.widget.Toast.makeText(context, "Đặt hàng thành công!", android.widget.Toast.LENGTH_SHORT).show()
                        navController.navigate("order_success") {
                            popUpTo("payment") { inclusive = true }
                        }
                    }
                    .addOnFailureListener { e ->
                        android.widget.Toast.makeText(context, "Lỗi khi đặt hàng: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        Log.e("TotalAndCheckoutButton", "Error saving order: ${e.message}", e)
                    }
            },
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000),
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Đặt hàng",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSuccessScreen(
    navController: NavController,
    products: List<CartItem>,
    totalPrice: Double
) {
    var orderAddress by remember { mutableStateOf<Address?>(null) }
    var orderProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var orderTotalPrice by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance()
            val ordersRef = database.getReference("orders").child(userId)
            ordersRef.orderByChild("createdAt").limitToLast(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (orderSnapshot in snapshot.children) {
                                val addressMap = orderSnapshot.child("address").value as? Map<String, Any>
                                addressMap?.let {
                                    orderAddress = Address(
                                        name = it["name"] as? String ?: "",
                                        phone = it["phone"] as? String ?: "",
                                        addressDetail = it["addressDetail"] as? String ?: "",
                                        isDefault = it["isDefault"] as? Boolean ?: false
                                    )
                                }

                                val productsList = orderSnapshot.child("products").value as? List<Map<String, Any>>
                                productsList?.let { products ->
                                    orderProducts = products.map { productMap ->
                                        Product(
                                            name = productMap["name"] as? String ?: "",
                                            color = productMap["color"] as? String ?: "",
                                            price = (productMap["price"] as? String) ?: "0",
                                            quantity = (productMap["quantity"] as? Long)?.toInt() ?: 0,
                                            imageUrl = productMap["imageUrl"] as? String
                                        )
                                    }
                                }

                                val totalPriceValue = orderSnapshot.child("totalPrice").value
                                orderTotalPrice = when (totalPriceValue) {
                                    is Double -> totalPriceValue
                                    is Long -> totalPriceValue.toDouble()
                                    else -> 0.0
                                }
                            }
                        } else {
                            errorMessage = "Không tìm thấy đơn hàng"
                        }
                        isLoading = false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        errorMessage = "Lỗi khi tải đơn hàng: ${error.message}"
                        isLoading = false
                    }
                })
        } else {
            errorMessage = "Vui lòng đăng nhập"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Đơn hàng thành công",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "Lỗi không xác định",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.successful),
                                contentDescription = "Success",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(bottom = 16.dp)
                            )

                            Text(
                                text = "Đặt hàng thành công!",
                                color = Color(0xFF00C853),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Text(
                                text = "Cảm ơn bạn đã đặt hàng",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Thông tin giao hàng",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            orderAddress?.let { address ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle, // Sử dụng biểu tượng Code từ Icons.Default
                                        contentDescription = "Code Icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.name,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone, // Sử dụng biểu tượng Code từ Icons.Default
                                        contentDescription = "Code Icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.phone,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddLocation, // Sử dụng biểu tượng Code từ Icons.Default
                                        contentDescription = "Code Icon",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.addressDetail,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Chi tiết đơn hàng",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            orderProducts.forEach { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(product.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = product.name,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = product.name,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Số lượng: ${product.quantity}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${DecimalFormat("#,###").format(product.price.toInt() * product.quantity)}đ",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (product != orderProducts.last()) {
                                    Divider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }

                            Divider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tổng cộng:",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${DecimalFormat("#,###").format(orderTotalPrice.toInt())}đ",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val intent = Intent(navController.context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            navController.context.startActivity(intent)
                            (navController.context as? ComponentActivity)?.finish()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Trở về trang chủ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}