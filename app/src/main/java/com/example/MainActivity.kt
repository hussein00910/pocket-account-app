package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BudgetEntity
import com.example.data.TransactionEntity
import com.example.ui.LedgerViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PocketLedgerScreen()
                }
            }
        }
    }
}

// --- Visual & Category Metadata ---
data class CategoryConfig(
    val name: String,
    val color: Color,
    val icon: ImageVector
)

val CATEGORIES = listOf(
    CategoryConfig("Food", Color(0xFFFF7043), Icons.Rounded.Restaurant),
    CategoryConfig("Shopping", Color(0xFFEC407A), Icons.Rounded.ShoppingBag),
    CategoryConfig("Bills", Color(0xFF26A69A), Icons.Rounded.ReceiptLong),
    CategoryConfig("Transport", Color(0xFFFFCA28), Icons.Rounded.DirectionsCar),
    CategoryConfig("Leisure", Color(0xFFAB47BC), Icons.Rounded.LocalPlay),
    CategoryConfig("Salary", Color(0xFF66BB6A), Icons.Rounded.Payments),
    CategoryConfig("Other", Color(0xFF78909C), Icons.Rounded.Category)
)

fun getCategoryConfig(name: String): CategoryConfig {
    return CATEGORIES.firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: CategoryConfig("Other", Color(0xFF78909C), Icons.Rounded.Category)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketLedgerScreen(
    viewModel: LedgerViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val aiAdvice by viewModel.aiInsightState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    // Filter and search state
    var searchKeyword by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Expense, 2 = Income

    // Chart toggle: 0 = Outflow Trends, 1 = Categories Breakdown
    var selectedChartTab by remember { mutableIntStateOf(0) }

    // Calculations based on actual transaction state
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val netWorth = totalIncome - totalExpense

    val cashBalance = remember(transactions) {
        transactions.filter { it.account == "Cash" }.sumOf {
            if (it.type == "INCOME") it.amount else -it.amount
        }
    }
    val cardBalance = remember(transactions) {
        transactions.filter { it.account == "Card" }.sumOf {
            if (it.type == "INCOME") it.amount else -it.amount
        }
    }
    val savingsBalance = remember(transactions) {
        transactions.filter { it.account == "Savings" }.sumOf {
            if (it.type == "INCOME") it.amount else -it.amount
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.size(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF44474E)))
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF44474E)))
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF44474E)))
                        }
                    }
                },
                title = {
                    val currentMonthYear = remember {
                        SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
                    }
                    Text(
                        text = currentMonthYear,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        color = Color(0xFF1A1C1E)
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { showBudgetDialog = true }
                            .testTag("budget_settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JD",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF7F9FC)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Total Net Worth Banner
            item {
                NetWorthBanner(
                    netWorth = netWorth,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense
                )
            }

            // Quick Actions Segment
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button 1: Add Record
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAddDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    }

                    // Button 2: Toggle Charts Breakdown (Reports)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                selectedChartTab = if (selectedChartTab == 0) 1 else 0
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📊", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Reports", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    }

                    // Button 3: Budget Configuration modal (Transfer action representation)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showBudgetDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⇅", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Transfer", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    }

                    // Button 4: Audit check / trigger AI Coach
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.fetchAiAdvice() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧾", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Audit", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // 2. Accounts Balance Breakdown Row
            item {
                AccountsBreakdownRow(
                    cash = cashBalance,
                    card = cardBalance,
                    savings = savingsBalance
                )
            }

            // 3. Interactive Charts Panel (Canvas-drawn)
            item {
                InteractiveChartsCard(
                    transactions = transactions,
                    selectedChartTab = selectedChartTab,
                    onTabChanged = { selectedChartTab = it }
                )
            }

            // 4. AI Coach Insights Panel
            item {
                AiCoachPanel(
                    aiAdvice = aiAdvice,
                    isLoading = isAiLoading,
                    onTriggerAdvice = { viewModel.fetchAiAdvice() }
                )
            }

            // 5. Budgets Spending Limit Progress
            if (budgets.isNotEmpty() && transactions.any { it.type == "EXPENSE" }) {
                item {
                    BudgetsProgressCard(
                        budgets = budgets,
                        transactions = transactions
                    )
                }
            }

            // 6. Transaction Ledger Segment Header & Filters
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Transaction Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Search Ledger
                    OutlinedTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_search_input"),
                        placeholder = { Text("Search transactions or categories...") },
                        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchKeyword.isNotEmpty()) {
                                IconButton(onClick = { searchKeyword = "" }) {
                                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Segment Filter Choices (All / Expense / Income)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All Records", "Expenses", "Incomes").forEachIndexed { index, title ->
                            FilterChip(
                                modifier = Modifier.weight(1f).testTag("filter_chip_$index"),
                                selected = selectedFilterTab == index,
                                onClick = { selectedFilterTab = index },
                                label = { Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                            )
                        }
                    }
                }
            }

            // 7. Transaction List filtered down
            val filteredTransactions = transactions.filter { tx ->
                val matchesSearch = tx.description.contains(searchKeyword, ignoreCase = true) ||
                        tx.category.contains(searchKeyword, ignoreCase = true)
                val matchesTab = when (selectedFilterTab) {
                    1 -> tx.type == "EXPENSE"
                    2 -> tx.type == "INCOME"
                    else -> true
                }
                matchesSearch && matchesTab
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.LayersClear,
                                contentDescription = "No transactions",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchKeyword.isNotEmpty()) "No matches found" else "No transactions recorded yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }

            // Bottom spacer for edge to edge list scrolling comfort
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal quick-add form dialog
    if (showAddDialog) {
        AddTransactionModal(
            onDismiss = { showAddDialog = false },
            onConfirm = { desc, amt, type, cat, acc, date ->
                viewModel.addTransaction(desc, amt, type, cat, acc, date)
                showAddDialog = false
            }
        )
    }

    // Modal budget settings dialog
    if (showBudgetDialog) {
        BudgetSettingsModal(
            budgets = budgets,
            onDismiss = { showBudgetDialog = false },
            onSaveBudget = { cat, amt -> viewModel.setBudget(cat, amt) },
            onDeleteBudget = { cat -> viewModel.deleteBudget(cat) }
        )
    }
}

// --- Total Net Worth Banner Card ---
@Composable
fun NetWorthBanner(
    netWorth: Double,
    totalIncome: Double,
    totalExpense: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0061A4)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Total Balance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "$%,.2f", netWorth),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("net_worth_heading")
                    )
                }

                // Beautiful white/20 indicator with a customized outer border
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            // High contrast divider with Income & Spent blocks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MONTHLY INCOME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "+$%,.2f", totalIncome),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6DE19E) // Emerald green
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "MONTHLY SPENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "-$%,.2f", totalExpense),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8E94) // Rose red
                        )
                    }
                }
            }
        }
    }
}

// --- Accounts Breakdown Row ---
@Composable
fun AccountsBreakdownRow(
    cash: Double,
    card: Double,
    savings: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val accountsList = listOf(
            Triple("Cash", cash, Icons.Rounded.Paid),
            Triple("Card", card, Icons.Rounded.CreditCard),
            Triple("Savings", savings, Icons.Rounded.Savings)
        )

        accountsList.forEach { (name, balance, icon) ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                        Icon(imageVector = icon, contentDescription = name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = String.format(Locale.US, "$%,.2f", balance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFEF5350),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// --- Custom Canvas Drawn Interactive Charts Panel ---
@Composable
fun InteractiveChartsCard(
    transactions: List<TransactionEntity>,
    selectedChartTab: Int,
    onTabChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Switch tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visual Audits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Miniature Tab Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .padding(2.dp)
                ) {
                    val tabs = listOf("Weekly Outflows", "Categories Breakdown")
                    tabs.forEachIndexed { idx, label ->
                        val isSel = selectedChartTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onTabChanged(idx) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                if (transactions.none { it.type == "EXPENSE" }) {
                    // Empty chart state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = "No outflows chart",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Record some expenses to draw visual insights",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else if (selectedChartTab == 0) {
                    // Custom Bar Chart: Outflow Trends (last 7 calendar days)
                    WeeklyOutflowBarChart(transactions = transactions)
                } else {
                    // Custom Ring/Donut Chart: Categories Breakdown
                    CategoryDonutChart(transactions = transactions)
                }
            }
        }
    }
}

// --- Weekly Outflow Canvas Bar Chart ---
@Composable
fun WeeklyOutflowBarChart(transactions: List<TransactionEntity>) {
    val expenses = transactions.filter { it.type == "EXPENSE" }

    // Aggregate expense by Day of Week: Sunday = 1 to Saturday = 7
    val dayOutflows = remember(expenses) {
        val calendar = Calendar.getInstance()
        val totals = DoubleArray(7) // Mon, Tue, Wed, Thu, Fri, Sat, Sun
        val now = System.currentTimeMillis()

        for (tx in expenses) {
            // Only aggregate transactions within the last 7 days of ledger
            if (now - tx.timestamp < 7 * 24 * 60 * 60 * 1000L) {
                calendar.timeInMillis = tx.timestamp
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2, ...
                val index = when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                totals[index] += tx.amount
            }
        }
        totals
    }

    val maxVal = remember(dayOutflows) {
        val max = dayOutflows.maxOrNull() ?: 0.0
        if (max == 0.0) 100.0 else max
    }

    val labelText = listOf("M", "T", "W", "T", "F", "S", "S")
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 4.dp)
    ) {
        val width = size.width
        val height = size.height

        // Draw horizontal grid lines
        val numLines = 3
        for (i in 0..numLines) {
            val y = (height - 24.dp.toPx()) * (i.toFloat() / numLines)
            drawLine(
                color = outlineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw Bars
        val numBars = 7
        val bottomY = height - 20.dp.toPx()
        val spacing = width / numBars

        for (i in 0 until numBars) {
            val amount = dayOutflows[i]
            val barHeightFraction = (amount / maxVal).toFloat()
            val barH = (height - 28.dp.toPx()) * barHeightFraction
            val barW = 16.dp.toPx()

            val barX = i * spacing + (spacing - barW) / 2

            // Draw rounded candle bar
            drawRoundRect(
                color = if (amount > 0) barColor else barColor.copy(alpha = 0.2f),
                topLeft = Offset(barX, bottomY - barH),
                size = Size(barW, Math.max(barH, 4.dp.toPx())),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }

    // Legend markers below Canvas
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 158.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        labelText.forEachIndexed { i, label ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (dayOutflows[i] > 0) String.format(Locale.US, "$%.0f", dayOutflows[i]) else "-",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// --- Category Outflow Donut Canvas Chart ---
@Composable
fun CategoryDonutChart(transactions: List<TransactionEntity>) {
    val expenses = transactions.filter { it.type == "EXPENSE" }

    val categoryTotals = remember(expenses) {
        val sums = mutableMapOf<String, Double>()
        for (tx in expenses) {
            sums[tx.category] = (sums[tx.category] ?: 0.0) + tx.amount
        }
        sums.entries.sortedByDescending { it.value }
    }

    val totalSpent = remember(categoryTotals) {
        categoryTotals.sumOf { it.value }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Draw circular Ring on left side
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val circleDim = size.width
                var startAngle = -90f

                if (totalSpent > 0.0) {
                    categoryTotals.forEach { (catName, amount) ->
                        val sweep = ((amount / totalSpent) * 360f).toFloat()
                        val color = getCategoryConfig(catName).color

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx()),
                            size = Size(circleDim, circleDim)
                        )
                        startAngle += sweep
                    }
                } else {
                    // Empty filler
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx()),
                        size = Size(circleDim, circleDim)
                    )
                }
            }

            // Central Summary Label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total outflows", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = String.format(Locale.US, "$%.0f", totalSpent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Legend details on right side
        Column(
            modifier = Modifier
                .width(160.dp)
                .padding(start = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categoryTotals.take(4).forEach { (catName, amount) ->
                val conf = getCategoryConfig(catName)
                val percent = if (totalSpent > 0.0) (amount / totalSpent) * 100 else 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(conf.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = catName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "$%.0f (%.1f%%)", amount, percent),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (categoryTotals.size > 4) {
                Text(
                    text = "+ ${categoryTotals.size - 4} other categories",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}

// --- AI Financial Coach Insight Card ---
@Composable
fun AiCoachPanel(
    aiAdvice: String,
    isLoading: Boolean,
    onTriggerAdvice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "AI Sparkle Icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Ledger Coach",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onTriggerAdvice,
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("consult_ai_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Audit Me", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text area
            AnimatedContent(
                targetState = aiAdvice,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "AdviceAnim"
            ) { text ->
                if (text.isBlank()) {
                    Text(
                        text = "Let the AI Ledger Coach audit your books! I will check your cash balances, category overruns, and supply exactly 3 distinct tips to optimize your wallets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                } else {
                    // Custom formatting of bullets to read nicely
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        text.split("\n").forEach { line ->
                            val cleanLine = line.trim()
                            if (cleanLine.startsWith("*") || cleanLine.startsWith("-")) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = cleanLine.substring(1).trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Text(
                                    text = cleanLine,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Budgets Spending Limit Progress Card ---
@Composable
fun BudgetsProgressCard(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Budget Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            budgets.forEach { budget ->
                val spent = remember(transactions, budget.category) {
                    transactions.filter { it.type == "EXPENSE" && it.category == budget.category }.sumOf { it.amount }
                }

                val progress = if (budget.limitAmount > 0.0) (spent / budget.limitAmount).toFloat() else 0f
                val progressClamped = progress.coerceIn(0f, 1f)
                val colorConfig = getCategoryConfig(budget.category)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colorConfig.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(budget.category, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Text numeric markers
                        Text(
                            text = String.format(Locale.US, "$%,.2f / $%,.2f", spent, budget.limitAmount),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (progress >= 1.0f) Color(0xFFEF5350) else MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress status bar
                    LinearProgressIndicator(
                        progress = { progressClamped },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (progress >= 1.0f) Color(0xFFEF5350) else if (progress >= 0.8f) Color(0xFFFFB74D) else colorConfig.color,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )

                    if (progress >= 1.0f) {
                        Text(
                            text = "⚠ OVER BUDGET LIMIT!",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF5350),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Individual Transaction Ledger List Row ---
@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onDelete: () -> Unit
) {
    val categoryConfig = getCategoryConfig(transaction.category)
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }
    val formattedDate = remember(transaction.timestamp) {
        formatter.format(Date(transaction.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Coloured Category Circular Marker
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(categoryConfig.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryConfig.icon,
                        contentDescription = transaction.category,
                        tint = categoryConfig.color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Metadata Column (Desc, Wallet type, Date)
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge Account Type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(transaction.account, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = formattedDate,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Numeric Outflow / Flow amount on right + delete bin
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val textStyle = if (transaction.type == "INCOME") {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF10B981), // Beautiful emerald green
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1A1C1E), // Rich slate dark
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Text(
                    text = String.format(Locale.US, "%s$%,.2f", if (transaction.type == "INCOME") "+" else "-", transaction.amount),
                    style = textStyle,
                    modifier = Modifier.testTag("tx_item_amount_${transaction.id}")
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_tx_btn_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// --- Quick Add Transaction Dialog ---
@Composable
fun AddTransactionModal(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, String, Long) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") } // INCOME or EXPENSE
    var selectedCategory by remember { mutableStateOf("Food") }
    var selectedAccount by remember { mutableStateOf("Cash") }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "New Accounting Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Expense / Income segment toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(2.dp)
                ) {
                    listOf("EXPENSE" to "Outflow", "INCOME" to "Income").forEach { (v, lbl) ->
                        val isSel = selectedType == v
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) {
                                        if (v == "EXPENSE") Color(0xFFEF5350) else Color(0xFF66BB6A)
                                    } else Color.Transparent
                                )
                                .clickable { selectedType = v }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Numeric Amount Input
                OutlinedTextField(
                    value = amountString,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            amountString = input
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_amount_input"),
                    label = { Text("Amount ($)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                // Text Description Input
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tx_desc_input"),
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Grocery Store buy") },
                    singleLine = true
                )

                // Category Selection list choices
                Text("Select Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val relevantCategories = if (selectedType == "INCOME") {
                        listOf("Salary", "Other")
                    } else {
                        listOf("Food", "Shopping", "Bills", "Transport", "Leisure", "Other")
                    }

                    // Reset selected category to first available if not matching
                    LaunchedEffect(selectedType) {
                        if (!relevantCategories.contains(selectedCategory)) {
                            selectedCategory = relevantCategories.first()
                        }
                    }

                    // Scrollable category chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        relevantCategories.forEach { category ->
                            val isSel = selectedCategory == category
                            val conf = getCategoryConfig(category)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (isSel) conf.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, if (isSel) conf.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = conf.icon, contentDescription = category, tint = if (isSel) Color.White else conf.color, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Account selection Cash / Card / Savings
                Text("Funding Wallet Account", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "Card", "Savings").forEach { account ->
                        val isSel = selectedAccount == account
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedAccount = account }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = account,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Call buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amountString.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onConfirm(desc, amt, selectedType, selectedCategory, selectedAccount, System.currentTimeMillis())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_tx_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "EXPENSE") Color(0xFFEF5350) else Color(0xFF66BB6A)
                        )
                    ) {
                        Text("Log", color = Color.White)
                    }
                }
            }
        }
    }
}

// --- Budgets Configuration Modal Dialog ---
@Composable
fun BudgetSettingsModal(
    budgets: List<BudgetEntity>,
    onDismiss: () -> Unit,
    onSaveBudget: (String, Double) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var targetCategory by remember { mutableStateOf("Food") }
    var limitInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configure Visual Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Select category slider
                Text("Select Spending Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val spendCategories = listOf("Food", "Shopping", "Bills", "Transport", "Leisure")
                    spendCategories.forEach { cat ->
                        val isSel = targetCategory == cat
                        val conf = getCategoryConfig(cat)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) conf.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(1.2.dp, if (isSel) conf.color else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { targetCategory = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) conf.color else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Value Text Field limit
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            limitInput = input
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input"),
                    label = { Text("Monthly spending limit ($)") },
                    placeholder = { Text("e.g. 500") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Update limit
                    Button(
                        onClick = {
                            val amt = limitInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onSaveBudget(targetCategory, amt)
                                limitInput = ""
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_budget_button")
                    ) {
                        Text("Apply")
                    }

                    // Delete budget limit
                    OutlinedButton(
                        onClick = {
                            onDeleteBudget(targetCategory)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Limit")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Active Budgets listing
                Text("Active Limit Controls", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                if (budgets.isEmpty()) {
                    Text("No budgets established yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        items(budgets) { budget ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryConfig(budget.category).color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(budget.category, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = String.format(Locale.US, "$%,.2f Limit", budget.limitAmount),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}
