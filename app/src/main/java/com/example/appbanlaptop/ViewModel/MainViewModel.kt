package com.example.appbanlaptop.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.appbanlaptop.CategoryItem
import com.example.appbanlaptop.Model.ProductItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainViewModel : ViewModel() {
    val productItems = mutableStateListOf<ProductItem>()
    val categories = mutableStateListOf<CategoryItem>()
    var isLoadingItems by mutableStateOf(true)
    var isLoadingCategories by mutableStateOf(true)

    private val database = FirebaseDatabase.getInstance("https://appbanlaptop-default-rtdb.firebaseio.com/")
    private val itemsRef = database.getReference("Items")
    private val categoriesRef = database.getReference("Category")

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
            isLoadingItems = false
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("MainViewModel", "Firebase error: ${error.message}")
            isLoadingItems = false
            // Thử lại sau 3 giây
            android.os.Handler().postDelayed({
                itemsRef.addValueEventListener(this)
            }, 3000)
        }
    }

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
            isLoadingCategories = false
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("MainViewModel", "Firebase error: ${error.message}")
            isLoadingCategories = false
            // Thử lại sau 3 giây
            android.os.Handler().postDelayed({
                categoriesRef.addValueEventListener(this)
            }, 3000)
        }
    }

    init {
        itemsRef.addValueEventListener(itemsListener)
        categoriesRef.addValueEventListener(categoriesListener)
    }

    override fun onCleared() {
        itemsRef.removeEventListener(itemsListener)
        categoriesRef.removeEventListener(categoriesListener)
        super.onCleared()
    }
}