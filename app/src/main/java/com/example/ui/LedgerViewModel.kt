package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.BudgetEntity
import com.example.data.PocketRepository
import com.example.data.TransactionEntity
import com.example.api.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "pocket_ledger_database"
    ).fallbackToDestructiveMigration().build()

    private val repository = PocketRepository(database)

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiInsightState = MutableStateFlow<String>("")
    val aiInsightState: StateFlow<String> = _aiInsightState.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        // Collect once to seed initial onboarding data if empty
        viewModelScope.launch {
            repository.allTransactions.collect { list ->
                if (list.isEmpty()) {
                    seedInitialData()
                }
            }
        }
    }

    private suspend fun seedInitialData() {
        val initialList = listOf(
            TransactionEntity(description = "Monthly Salary", amount = 3500.0, type = "INCOME", category = "Salary", account = "Savings"),
            TransactionEntity(description = "Weekly Grocery Store", amount = 142.50, type = "EXPENSE", category = "Food", account = "Cash"),
            TransactionEntity(description = "Electricity Utilities", amount = 85.00, type = "EXPENSE", category = "Bills", account = "Card"),
            TransactionEntity(description = "Specialty Coffee Roast", amount = 6.25, type = "EXPENSE", category = "Food", account = "Cash"),
            TransactionEntity(description = "Streaming & Entertainment", amount = 15.99, type = "EXPENSE", category = "Leisure", account = "Card"),
            TransactionEntity(description = "Athletic Running Shoes", amount = 110.00, type = "EXPENSE", category = "Shopping", account = "Card"),
            TransactionEntity(description = "Evening Transit Fare", amount = 4.50, type = "EXPENSE", category = "Transport", account = "Cash")
        )
        for (tx in initialList) {
            repository.insertTransaction(tx)
        }

        val initialBudgets = listOf(
            BudgetEntity("Food", 400.0),
            BudgetEntity("Shopping", 300.0),
            BudgetEntity("Bills", 250.0),
            BudgetEntity("Transport", 120.0),
            BudgetEntity("Leisure", 100.0)
        )
        for (bg in initialBudgets) {
            repository.insertBudget(bg)
        }
    }

    fun addTransaction(description: String, amount: Double, type: String, category: String, account: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    description = description.ifBlank { "$type Transaction" },
                    amount = amount,
                    type = type,
                    category = category,
                    account = account,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun setBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.insertBudget(BudgetEntity(category, amount))
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudgetByCategory(category)
        }
    }

    fun fetchAiAdvice() {
        viewModelScope.launch {
            val txs = transactions.value
            val bgs = budgets.value
            if (txs.isEmpty()) {
                _aiInsightState.value = "You have no transactions to analyze. Please add some expenses first so I can inspect your spend layout!"
                return@launch
            }

            _isAiLoading.value = true
            _aiInsightState.value = "Analyzing ledger items..."

            val totalIncome = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalExpense = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val categoryBreakdown = txs.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .mapValues { (_, v) -> v.sumOf { it.amount } }

            val bgsSummary = bgs.joinToString { "${it.category}: Limit \$${it.limitAmount}" }
            val listSummary = txs.take(15).joinToString("\n") { 
                "- ${it.type} \$${it.amount} under ${it.category} (${it.description})" 
            }

            val summaryPrompt = """
                Total Net Worth Progress: \$${totalIncome - totalExpense}
                Aggregate Income: \$$totalIncome
                Aggregate Outflows: \$$totalExpense
                Active Budgets: $bgsSummary
                Categorized Spending Outflow: $categoryBreakdown
                Recent Records:
                $listSummary
            """.trimIndent()

            val insight = GeminiClient.getFinancialInsight(summaryPrompt)
            _aiInsightState.value = insight
            _isAiLoading.value = false
        }
    }
}
