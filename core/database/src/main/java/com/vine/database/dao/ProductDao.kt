package com.vine.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vine.database.entity.ProductBarcodeEntity
import com.vine.database.entity.ProductEntity
import com.vine.database.relation.ProductWithBarcodes
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: ProductBarcodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcodes(barcodes: List<ProductBarcodeEntity>)

    @Update
    suspend fun updateBarcode(barcode: ProductBarcodeEntity)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun findProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE product_code = :productCode LIMIT 1")
    suspend fun findProductByCode(productCode: String): ProductEntity?

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun findProductWithBarcodesById(id: Long): ProductWithBarcodes?

    @Transaction
    @Query("SELECT * FROM products WHERE product_code = :productCode LIMIT 1")
    suspend fun findProductWithBarcodesByCode(productCode: String): ProductWithBarcodes?

    @Transaction
    @Query(
        """
        SELECT p.* FROM products p
        INNER JOIN product_barcodes pb ON pb.product_id = p.id
        WHERE pb.barcode = :barcode
          AND p.is_active = 1
          AND pb.is_active = 1
        LIMIT 1
        """
    )
    suspend fun findProductByBarcode(barcode: String): ProductEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM products
        WHERE is_active = 1
        ORDER BY product_code ASC
        """
    )
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM products
        WHERE is_active = 1
          AND (
            product_code LIKE '%' || :keyword || '%'
            OR product_name LIKE '%' || :keyword || '%'
          )
        ORDER BY product_code ASC
        """
    )
    fun observeActiveProducts(keyword: String): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM product_barcodes
        WHERE product_id = :productId
        ORDER BY is_primary DESC, barcode ASC
        """
    )
    suspend fun findBarcodesByProductId(productId: Long): List<ProductBarcodeEntity>

    @Query(
        """
        DELETE FROM product_barcodes
        WHERE product_id = :productId
        """
    )
    suspend fun deleteBarcodesByProductId(productId: Long)

    @Query(
        """
    SELECT *
    FROM products
    ORDER BY product_code
    LIMIT :limit
    """
    )
    suspend fun allProducts(limit: Int): List<ProductEntity>

    @Query(
        """
    SELECT *
    FROM products
    WHERE product_code LIKE '%' || :keyword || '%'
       OR product_name LIKE '%' || :keyword || '%'
    ORDER BY product_code
    LIMIT :limit
    """
    )
    suspend fun searchProducts(
        keyword: String,
        limit: Int,
    ): List<ProductEntity>
}