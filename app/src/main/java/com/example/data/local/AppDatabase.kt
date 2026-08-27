package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CashierShiftDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StockAdjustmentDao
import com.example.data.local.dao.StoreSettingsDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.CashierShiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockAdjustmentEntity
import com.example.data.local.entity.StoreSettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StockAdjustmentEntity::class,
        StoreSettingsEntity::class,
        CashierShiftEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun cashierShiftDao(): CashierShiftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pos_kasir_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val productDao = database.productDao()
            val settingsDao = database.storeSettingsDao()

            // Default Store Profile
            settingsDao.insertOrUpdate(
                StoreSettingsEntity(
                    id = 1,
                    storeName = "Kopi & Mart Berkah",
                    tagline = "Kasir POS Modern & Manajemen Stok",
                    address = "Jl. Sudirman No. 45, Jakarta",
                    phone = "0812-3456-7890",
                    receiptFooter = "Terima Kasih Telah Berkunjung!\nStruk ini adalah bukti pembayaran sah.",
                    taxRate = 0.0,
                    taxEnabled = false,
                    cashierName = "Kasir Utama"
                )
            )

            // Initial Products with diverse categories, cost and selling prices, and stock
            val initialProducts = listOf(
                ProductEntity(
                    name = "Kopi Susu Gula Aren",
                    sku = "KOP-001",
                    category = "Kopi & Minuman",
                    costPrice = 8000.0,
                    sellingPrice = 18000.0,
                    stock = 45,
                    minStockAlert = 10,
                    unit = "cup",
                    colorHex = "#8D6E63",
                    iconName = "coffee"
                ),
                ProductEntity(
                    name = "Espresso Single Shot",
                    sku = "KOP-002",
                    category = "Kopi & Minuman",
                    costPrice = 4000.0,
                    sellingPrice = 12000.0,
                    stock = 60,
                    minStockAlert = 10,
                    unit = "cup",
                    colorHex = "#5D4037",
                    iconName = "coffee"
                ),
                ProductEntity(
                    name = "Matcha Latte Ice",
                    sku = "MIN-001",
                    category = "Kopi & Minuman",
                    costPrice = 9500.0,
                    sellingPrice = 22000.0,
                    stock = 30,
                    minStockAlert = 5,
                    unit = "cup",
                    colorHex = "#689F38",
                    iconName = "local_cafe"
                ),
                ProductEntity(
                    name = "Teh Manis Dingin (Jumbo)",
                    sku = "MIN-002",
                    category = "Kopi & Minuman",
                    costPrice = 2000.0,
                    sellingPrice = 6000.0,
                    stock = 100,
                    minStockAlert = 15,
                    unit = "cup",
                    colorHex = "#F57C00",
                    iconName = "emoji_food_beverage"
                ),
                ProductEntity(
                    name = "Air Mineral 600ml",
                    sku = "MIN-003",
                    category = "Kopi & Minuman",
                    costPrice = 2500.0,
                    sellingPrice = 5000.0,
                    stock = 4, // Low stock demo
                    minStockAlert = 10,
                    unit = "botol",
                    colorHex = "#0288D1",
                    iconName = "water_drop"
                ),
                ProductEntity(
                    name = "Nasi Goreng Spesial Telur",
                    sku = "MAK-001",
                    category = "Makanan",
                    costPrice = 12000.0,
                    sellingPrice = 25000.0,
                    stock = 25,
                    minStockAlert = 5,
                    unit = "porsi",
                    colorHex = "#D32F2F",
                    iconName = "restaurant"
                ),
                ProductEntity(
                    name = "Mie Goreng Jawa",
                    sku = "MAK-002",
                    category = "Makanan",
                    costPrice = 10000.0,
                    sellingPrice = 22000.0,
                    stock = 20,
                    minStockAlert = 5,
                    unit = "porsi",
                    colorHex = "#E64A19",
                    iconName = "ramen_dining"
                ),
                ProductEntity(
                    name = "Ayam Geprek Sambal Bawang",
                    sku = "MAK-003",
                    category = "Makanan",
                    costPrice = 11000.0,
                    sellingPrice = 20000.0,
                    stock = 18,
                    minStockAlert = 5,
                    unit = "porsi",
                    colorHex = "#C2185B",
                    iconName = "lunch_dining"
                ),
                ProductEntity(
                    name = "Croissant Butter",
                    sku = "SNK-001",
                    category = "Snack & Bakery",
                    costPrice = 9000.0,
                    sellingPrice = 19000.0,
                    stock = 3, // Low stock
                    minStockAlert = 5,
                    unit = "pcs",
                    colorHex = "#FFA000",
                    iconName = "bakery_dining"
                ),
                ProductEntity(
                    name = "Kentang Goreng Crispy",
                    sku = "SNK-002",
                    category = "Snack & Bakery",
                    costPrice = 7000.0,
                    sellingPrice = 15000.0,
                    stock = 35,
                    minStockAlert = 10,
                    unit = "porsi",
                    colorHex = "#FBC02D",
                    iconName = "fastfood"
                ),
                ProductEntity(
                    name = "Minyak Goreng 1 Liter",
                    sku = "SMB-001",
                    category = "Sembako",
                    costPrice = 14500.0,
                    sellingPrice = 17500.0,
                    stock = 20,
                    minStockAlert = 5,
                    unit = "pouch",
                    colorHex = "#388E3C",
                    iconName = "shopping_basket"
                ),
                ProductEntity(
                    name = "Beras Premium 5 Kg",
                    sku = "SMB-002",
                    category = "Sembako",
                    costPrice = 64000.0,
                    sellingPrice = 73000.0,
                    stock = 12,
                    minStockAlert = 4,
                    unit = "sak",
                    colorHex = "#7B1FA2",
                    iconName = "inventory_2"
                )
            )

            productDao.insertProducts(initialProducts)
        }
    }
}
