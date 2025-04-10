package com.example.appbanlaptop.ViewModel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.appbanlaptop.CategoryItem
import com.example.appbanlaptop.Model.ProductItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainViewModel : ViewModel() {
    // Danh sách sản phẩm và danh mục
    val productItems = mutableStateListOf<ProductItem>()
    val categories = mutableStateListOf<CategoryItem>()

    private val database = FirebaseDatabase.getInstance("https://appbanlaptop-default-rtdb.firebaseio.com/")
    private val itemsRef = database.getReference("Items")
    private val categoriesRef = database.getReference("Category")

    // Listener cho Items
    private val itemsListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            productItems.clear()
            for (item in snapshot.children) {
                try {
                    val productItem = item.getValue(ProductItem::class.java)
                    if (productItem != null) {
                        productItems.add(productItem)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing item: ${item.key}, error: ${e.message}")
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("MainViewModel", "Firebase error: ${error.message}")
        }
    }

    // Listener cho Categories
    private val categoriesListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            categories.clear()
            for (item in snapshot.children) {
                try {
                    val categoryItem = item.getValue(CategoryItem::class.java)
                    if (categoryItem != null) {
                        categories.add(categoryItem)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing category: ${item.key}, error: ${e.message}")
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("MainViewModel", "Firebase error: ${error.message}")
        }
    }

    init {
        // Gắn listener khi ViewModel được khởi tạo
        itemsRef.addValueEventListener(itemsListener)
        categoriesRef.addValueEventListener(categoriesListener)
    }

    override fun onCleared() {
        // Gỡ listener khi ViewModel bị hủy để tránh rò rỉ bộ nhớ
        itemsRef.removeEventListener(itemsListener)
        categoriesRef.removeEventListener(categoriesListener)
        super.onCleared()
    }
}