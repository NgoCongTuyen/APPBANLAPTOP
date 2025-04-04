package com.example.appbanlaptop.payment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appbanlaptop.ui.theme.PaymentTheme
import java.text.DecimalFormat
import android.util.Log
import android.os.Parcelable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.util.copy
import com.example.appbanlaptop.paymment.Product
import kotlinx.parcelize.Parcelize

// Định nghĩa nguồn dữ liệu chung
object AddressState {
    val newAddress = mutableStateOf<Address?>(null)
    val addresses = mutableStateListOf(
        Address("Nguyễn Thanh Đức", "(+84)03****3838", "59/22C Mã Lò, Bình Trị Đông A, Bình Tân, Hồ Chí Minh, Việt Nam", true),
        Address("Hương", "(+84)84****30", "ngõ 87 ngách 43 số nhà 10, Yên Nghĩa, Hà Đông, Hà Nội, Việt Nam", false),
        Address("Mai Hương", "(+84)84****30", "số 1 ngõ 25, thôn Bảo Lộc 4, Võng Xuyên, Phúc Thọ, Hà Nội, Việt Nam", false)
    )
}

class PaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaymentTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "payment") {
                    composable("payment") {
                        PaymentScreen(navController)
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
                }
            }
        }
    }
}

@Composable
fun PaymentScreen(navController: NavController) {
    var selectedAddress by remember {
        mutableStateOf(AddressState.addresses.firstOrNull { it.isDefault } ?: AddressState.addresses.firstOrNull())
    }

    val backStackEntry = navController.currentBackStackEntry
    val selectedFromList = backStackEntry?.savedStateHandle?.get<Address>("selected_address")

    LaunchedEffect(selectedFromList) {
        selectedFromList?.let { address ->
            Log.d("PaymentScreen", "Selected address from AddressListScreen: $address")
            selectedAddress = address
            backStackEntry.savedStateHandle.remove<Address>("selected_address")
        }
    }

    Log.d("PaymentScreen", "PaymentScreen recomposed")

    AddressState.newAddress.value?.let { newAddress ->
        Log.d("PaymentScreen", "New address detected: $newAddress")
        if (newAddress.isDefault) {
            AddressState.addresses.forEachIndexed { index, address ->
                AddressState.addresses[index] = address.copy(isDefault = false)
            }
        }
        AddressState.addresses.add(newAddress)
        selectedAddress = newAddress
        AddressState.newAddress.value = null
        Log.d("PaymentScreen", "Addresses updated: ${AddressState.addresses}")
    }

    val products = remember {
        mutableStateListOf(
            Product("Bàn phím cơ Gaming ZIYOU K550 V...", "Trắng Đen", "348874", 1),
            Product("Chuột Silent Gaming Atlas F20 Không d...", "F20 Đen", "207326", 1)
        )
    }

    val totalPrice by remember(products) {
        derivedStateOf {
            products.sumOf { product -> product.price.toInt() * product.quantity }
        }
    }

    Scaffold(
        bottomBar = {
            TotalAndCheckoutButton(productsSize = products.size, totalPrice = totalPrice)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding( bottom = paddingValues.calculateBottomPadding()),
            //contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                ShippingInfo(
                    selectedAddress = selectedAddress,
                    onAddressClick = {
                        navController.navigate("address_list")
                    }
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    Box(modifier = Modifier.weight(1f).background(Color(0xFFFF0000)))
                    Box(modifier = Modifier.weight(1f).background(Color(0xFF00C4FF)))
                }
            }
            item { StoreHeader() }
            items(products) { product ->
                ProductItem(
                    product = product,
                    onQuantityChange = { newQuantity ->
                        val index = products.indexOf(product)
                        if (newQuantity >= 1) {
                            products[index] = product.copy(quantity = newQuantity)
                        }
                    }
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                    Box(modifier = Modifier.weight(1f).background(Color(0xFFFF0000)))
                    Box(modifier = Modifier.weight(1f).background(Color(0xFF00C4FF)))
                }
            }
            item { PaymentMethod() }
        }
    }
}

@Composable
fun ShippingInfo(selectedAddress: Address?, onAddressClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        TextField(
            value = selectedAddress?.let { "${it.name}\n${it.phone}\n${it.addressDetail}" } ?: "Chưa chọn địa chỉ",
            onValueChange = { /* Không cho chỉnh sửa trực tiếp */ },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C2526), RoundedCornerShape(8.dp))
                .clickable { onAddressClick() },
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF1C2526),
                disabledTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Địa chỉ của bạn", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("<", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate("add_address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
            ) {
                Text("Thêm địa chỉ mới", color = Color.White, fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            itemsIndexed(AddressState.addresses) { index, address ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                Log.d("AddressListScreen", "Selected address: $address")
                                navController.previousBackStackEntry?.savedStateHandle?.set("selected_address", address)
                                navController.popBackStack()
                            }
                    ) {
                        Text(
                            text = address.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = address.phone,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = address.addressDetail,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        if (address.isDefault) {
                            Text(
                                text = "Mặc định",
                                color = Color(0xFFFF0000),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = "Chỉnh sửa",
                        color = Color(0xFFFF0000),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                Log.d("AddressListScreen", "Edit address clicked: $address")
                                navController.navigate("edit_address/$index")
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(navController: NavController, addressToEdit: Address?) {
    // Nếu addressToEdit không null, điền thông tin hiện tại để chỉnh sửa
    var newName by remember { mutableStateOf(addressToEdit?.name ?: "") }
    var newPhone by remember { mutableStateOf(addressToEdit?.phone ?: "") }
    var newProvince by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(3) ?: "") }
    var newDistrict by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(2) ?: "") }
    var newWard by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(1) ?: "") }
    var newAddressDetail by remember { mutableStateOf(addressToEdit?.addressDetail?.split(", ")?.getOrNull(0) ?: "") }
    var isDefault by remember { mutableStateOf(addressToEdit?.isDefault ?: false) }

    val isNameValid = newName.isNotBlank()
    val isPhoneValid = newPhone.isNotBlank() && newPhone.length == 10 && newPhone.startsWith("0") && newPhone.all { it.isDigit() }
    val isProvinceValid = newProvince.isNotBlank()
    val isDistrictValid = newDistrict.isNotBlank()
    val isWardValid = newWard.isNotBlank()
    val isAddressDetailValid = newAddressDetail.isNotBlank()
    val isFormValid = isNameValid && isPhoneValid && isProvinceValid && isDistrictValid && isWardValid && isAddressDetailValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (addressToEdit == null) "Thêm địa chỉ mới" else "Chỉnh sửa địa chỉ", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("<", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    Log.d("AddAddressScreen", "Bước 1: Nút Lưu được nhấn")
                    val updatedAddress = Address(
                        name = newName,
                        phone = newPhone,
                        addressDetail = "$newAddressDetail, $newWard, $newDistrict, $newProvince",
                        isDefault = isDefault
                    )
                    Log.d("AddAddressScreen", "Bước 2: Địa chỉ đã cập nhật: $updatedAddress")
                    if (addressToEdit == null) {
                        // Thêm mới
                        AddressState.newAddress.value = updatedAddress
                        Log.d("AddAddressScreen", "Bước 3: Đã lưu new_address vào AddressState: ${AddressState.newAddress.value}")
                        navController.popBackStack("payment", inclusive = false)
                    } else {
                        // Chỉnh sửa
                        val index = AddressState.addresses.indexOf(addressToEdit)
                        if (index != -1) {
                            if (updatedAddress.isDefault) {
                                AddressState.addresses.forEachIndexed { i, addr ->
                                    AddressState.addresses[i] = addr.copy(isDefault = false)
                                }
                            }
                            AddressState.addresses[index] = updatedAddress
                            Log.d("AddAddressScreen", "Bước 3: Đã cập nhật địa chỉ tại index $index: ${AddressState.addresses}")
                            navController.popBackStack("address_list", inclusive = false)
                        }
                    }
                    Log.d("AddAddressScreen", "Bước 4: Đã gọi popBackStack")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) Color(0xFFFF0000) else Color.Gray,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Lưu", color = Color.White, fontSize = 16.sp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Tên", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            if (!isNameValid && newName.isEmpty()) {
                Text(
                    text = "Vui lòng nhập tên",
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newPhone,
                onValueChange = { newPhone = it },
                label = { Text("Số điện thoại", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            if (!isPhoneValid) {
                Text(
                    text = when {
                        newPhone.isEmpty() -> "Vui lòng nhập số điện thoại"
                        newPhone.length != 10 || !newPhone.startsWith("0") || !newPhone.all { it.isDigit() } -> "Số điện thoại không hợp lệ"
                        else -> ""
                    },
                    color = Color(0xFFFF0000),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newProvince,
                onValueChange = { newProvince = it },
                label = { Text("Tỉnh/Thành phố", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newDistrict,
                onValueChange = { newDistrict = it },
                label = { Text("Quận/Huyện", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newWard,
                onValueChange = { newWard = it },
                label = { Text("Phường/Xã", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newAddressDetail,
                onValueChange = { newAddressDetail = it },
                label = { Text("Địa chỉ cụ thể", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C2526),
                    unfocusedContainerColor = Color(0xFF1C2526),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFF0000),
                        uncheckedColor = Color.White
                    )
                )
                Text("Đặt làm mặc định", color = Color.White)
            }
        }
    }
}

@Composable
fun StoreHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1C2526)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "APPLE STORE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProductItem(product: Product, onQuantityChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C2526))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(80.dp).background(Color.Gray))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                color = Color.White,
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
                modifier = Modifier.background(Color(0xFF3A3A3A), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
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
            Text(text = "-", color = Color.White, modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity - 1) })
            Text(text = product.quantity.toString(), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
            Text(text = "+", color = Color.White, modifier = Modifier.padding(4.dp).clickable { onQuantityChange(product.quantity + 1) })
        }
    }
}

@Composable
fun PaymentMethod() {
    var selectedMethod by remember { mutableStateOf("Thanh toán khi nhận hàng") }
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1C2526)).padding(16.dp)) {
        Text(text = "Phương thức thanh toán", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        PaymentOption("Thanh toán khi nhận hàng", selectedMethod == "Thanh toán khi nhận hàng") { selectedMethod = "Thanh toán khi nhận hàng" }
        PaymentOption("Thẻ tín dụng", selectedMethod == "Thẻ tín dụng") { selectedMethod = "Thẻ tín dụng" }
        PaymentOption("Ví Momo", selectedMethod == "Ví Momo") { selectedMethod = "Ví Momo" }
        PaymentOption("ZaloPay", selectedMethod == "ZaloPay") { selectedMethod = "ZaloPay" }
    }
}

@Composable
fun PaymentOption(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF3A3A3A) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = if (isSelected) Color(0xFFFF0000) else Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TotalAndCheckoutButton(productsSize: Int, totalPrice: Int) {
    val formatter = DecimalFormat("#,###")
    val formattedTotal = "${formatter.format(totalPrice)}đ"
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1C2526)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "$productsSize mặt hàng, tổng cộng", color = Color.White, fontSize = 14.sp)
            Text(text = formattedTotal, color = Color(0xFFFF0000), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { /* Xử lý đặt hàng */ },
            modifier = Modifier.height(48.dp).clip(RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
        ) {
            Text(text = "Đặt hàng", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}