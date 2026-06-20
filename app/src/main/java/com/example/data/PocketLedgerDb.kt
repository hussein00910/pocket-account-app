package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // "Food", "Shopping", "Salary", "Transport", "Bills", "Leisure", "Other"
    val timestamp: Long = System.currentTimeMillis(),
    val account: String // "Cash", "Card", "Savings"
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val category: String,
    val limitAmount: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudgetByCategory(category: String)
}

@Database(entities = [TransactionEntity::class, BudgetEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
}

class PocketRepository(private val db: AppDatabase) {
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactionsFlow()
    val allBudgets: Flow<List<BudgetEntity>> = db.budgetDao().getAllBudgetsFlow()

    suspend fun insertTransaction(transaction: TransactionEntity) = db.transactionDao().insertTransaction(transaction)
    suspend fun updateTransaction(transaction: TransactionEntity) = db.transactionDao().updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = db.transactionDao().deleteTransaction(transaction)
    suspend fun deleteTransactionById(id: Int) = db.transactionDao().deleteTransactionById(id)

    suspend fun insertBudget(budget: BudgetEntity) = db.budgetDao().insertBudget(budget)
    suspend fun deleteBudgetByCategory(category: String) = db.budgetDao().deleteBudgetByCategory(category)
}
