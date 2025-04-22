import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.appbanlaptop.Model.ProductItem

fun searchProducts(query: String, callback: (List<ProductItem>) -> Unit) {
    val database = FirebaseDatabase.getInstance().getReference("products")
    val searchResults = mutableListOf<ProductItem>()
    val cleanedQuery = query.trim().lowercase()

    // Log truy vấn để debug
    Log.d("SearchProducts", "Starting search with query: '$cleanedQuery'")

    database.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            searchResults.clear()
            Log.d("SearchProducts", "Snapshot received with ${snapshot.childrenCount} products")

            if (!snapshot.exists()) {
                Log.w("SearchProducts", "No products found in database")
                callback(emptyList())
                return
            }

            for (productSnapshot in snapshot.children) {
                try {
                    val product = productSnapshot.getValue(ProductItem::class.java)
                    if (product == null) {
                        Log.w("SearchProducts", "Product is null for snapshot: ${productSnapshot.key}")
                        continue
                    }

                    // Kiểm tra title hoặc description chứa query (không phân biệt hoa thường)
                    val titleMatch = product.title?.lowercase()?.contains(cleanedQuery) == true
                    val descriptionMatch = product.description?.lowercase()?.contains(cleanedQuery) == true

                    if (titleMatch || descriptionMatch) {
                        searchResults.add(product)
                        Log.d("SearchProducts", "Found product: ${product.title} (Title: $titleMatch, Description: $descriptionMatch)")
                    } else {
                        Log.d("SearchProducts", "Product skipped: ${product.title ?: "null"}")
                    }
                } catch (e: Exception) {
                    Log.e("SearchProducts", "Error parsing product: ${productSnapshot.key}, ${e.message}")
                }
            }

            Log.d("SearchProducts", "Search completed with ${searchResults.size} results")
            callback(searchResults)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("SearchProducts", "Database error: ${error.message}, Code: ${error.code}")
            callback(emptyList()) // Trả về danh sách rỗng nếu có lỗi
        }
    })
}