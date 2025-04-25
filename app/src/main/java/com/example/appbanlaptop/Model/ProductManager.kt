package com.example.appbanlaptop.Model

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Product(
    val id: String? = null,
    val title: String? = null,
    val price: Double = 0.0,
    val description: String? = null,
    val picUrl: List<String>? = emptyList(),
    val categoryId: String? = null,
    val rating: Double? = 0.0,
    val showRecommended: Boolean? = false,
    val model: List<String>? = emptyList()
)

object ProductManager {
    private val database = FirebaseDatabase.getInstance()
    private val productsRef = database.getReference("Items")
    private val products = mutableListOf<Product>()
    private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: StateFlow<List<Product>> = _productsFlow
    private var valueEventListener: ValueEventListener? = null

    init {
        setupListener()
    }

    fun addProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val newRef = productsRef.push()
        val productWithId = product.copy(id = newRef.key)

        newRef.setValue(productWithId)
            .addOnSuccessListener {
                Log.d("ProductManager", "Successfully added product: $productWithId")
                onSuccess()
            }
            .addOnFailureListener {
                Log.e("ProductManager", "Failed to add product: ${it.message}")
                onError(it.message ?: "Unknown error")
            }
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        product.id?.let { id ->
            productsRef.child(id).setValue(product)
                .addOnSuccessListener {
                    Log.d("ProductManager", "Successfully updated product: $product")
                    onSuccess()
                }
                .addOnFailureListener {
                    Log.e("ProductManager", "Failed to update product: ${it.message}")
                    onError(it.message ?: "Unknown error")
                }
        } ?: run {
            onError("Product ID is null")
        }
    }

    fun removeProduct(productId: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        productId?.let { id ->
            productsRef.child(id).removeValue()
                .addOnSuccessListener {
                    Log.d("ProductManager", "Successfully removed product with ID: $id")
                    onSuccess()
                }
                .addOnFailureListener {
                    Log.e("ProductManager", "Failed to remove product: ${it.message}")
                    onError(it.message ?: "Unknown error")
                }
        } ?: run {
            onError("Product ID is null")
        }
    }

    fun cleanup() {
        valueEventListener?.let {
            productsRef.removeEventListener(it)
            valueEventListener = null
            Log.d("ProductManager", "Cleaned up Firebase listener")
        }
    }

    private fun setupListener() {
        cleanup()
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ProductManager", "Data changed, snapshot: $snapshot")
                products.clear()
                if (!snapshot.exists() || snapshot.childrenCount.toInt() == 0) {
                    Log.d("ProductManager", "Product list is empty")
                    _productsFlow.value = emptyList()
                    return
                }

                for (item in snapshot.children) {
                    try {
                        val product = item.getValue(Product::class.java)
                        if (product != null) {
                            products.add(product)
                            Log.d("ProductManager", "Added product: $product")
                        }
                    } catch (e: Exception) {
                        Log.e("ProductManager", "Error parsing product: ${item.key}, error: ${e.message}")
                    }
                }
                _productsFlow.value = products.toList()
                Log.d("ProductManager", "Updated productsFlow: ${_productsFlow.value}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductManager", "Database error: ${error.message}")
                _productsFlow.value = products.toList()
            }
        }
        valueEventListener?.let {
            productsRef.addValueEventListener(it)
            Log.d("ProductManager", "Firebase listener set up")
        }
    }
}