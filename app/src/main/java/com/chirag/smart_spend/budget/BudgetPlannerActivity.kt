package com.chirag.smart_spend.budget

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirag.smart_spend.R
import com.chirag.smart_spend.categorization.CategoryIcons
import com.chirag.smart_spend.reports.ComparisonReport
import com.chirag.smart_spend.reports.Insight
import com.chirag.smart_spend.reports.ReportDates
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.materialswitch.MaterialSwitch

class BudgetPlannerActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var userId = ""

    private var allTransactions: List<Transaction> = emptyList()
    private var budget = BudgetData(null, emptyMap())

    private lateinit var tvBudgetMonth: TextView
    private lateinit var btnEditBudget: Button
    private lateinit var tvBudgetSpent: TextView
    private lateinit var progressOverall: LinearProgressIndicator
    private lateinit var tvBudgetRemaining: TextView
    private lateinit var layoutMetrics: LinearLayout
    private lateinit var tvDaysLeft: TextView
    private lateinit var tvSafePerDay: TextView
    private lateinit var tvProjected: TextView
    private lateinit var layoutSuggestion: LinearLayout
    private lateinit var tvSuggestion: TextView
    private lateinit var btnUseSuggestion: Button
    private lateinit var btnAddCategoryBudget: Button
    private lateinit var tvCategoryEmpty: TextView
    private lateinit var rvCategories: RecyclerView
    private lateinit var layoutTips: LinearLayout
    private lateinit var switchAlerts: MaterialSwitch

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Notifications are off, so budget alerts cannot be shown",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_planner)

        db = FirebaseFirestore.getInstance()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        tvBudgetMonth = findViewById(R.id.tvBudgetMonth)
        btnEditBudget = findViewById(R.id.btnEditBudget)
        tvBudgetSpent = findViewById(R.id.tvBudgetSpent)
        progressOverall = findViewById(R.id.progressOverall)
        tvBudgetRemaining = findViewById(R.id.tvBudgetRemaining)
        layoutMetrics = findViewById(R.id.layoutMetrics)
        tvDaysLeft = findViewById(R.id.tvDaysLeft)
        tvSafePerDay = findViewById(R.id.tvSafePerDay)
        tvProjected = findViewById(R.id.tvProjected)
        layoutSuggestion = findViewById(R.id.layoutSuggestion)
        tvSuggestion = findViewById(R.id.tvSuggestion)
        btnUseSuggestion = findViewById(R.id.btnUseSuggestion)
        btnAddCategoryBudget = findViewById(R.id.btnAddCategoryBudget)
        tvCategoryEmpty = findViewById(R.id.tvCategoryEmpty)
        rvCategories = findViewById(R.id.rvBudgetCategories)
        layoutTips = findViewById(R.id.layoutTips)

        rvCategories.layoutManager = LinearLayoutManager(this)

        switchAlerts = findViewById(R.id.switchAlerts)
        switchAlerts.isChecked = BudgetAlertChecker.isEnabled(this)
        switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            BudgetAlertChecker.setEnabled(this, isChecked)
            if (isChecked) askNotificationPermission()
        }

        btnEditBudget.setOnClickListener {
            showAmountDialog(
                title = "Monthly budget",
                current = budget.monthlyBudget,
                canRemove = budget.monthlyBudget != null,
                onSave = { saveMonthlyBudget(it) },
                onRemove = { removeMonthlyBudget() }
            )
        }

        btnAddCategoryBudget.setOnClickListener { showAddCategoryDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    // ---------- Loading ----------

    private fun loadData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }
        userId = uid

        db.collection("transactions")
            .whereEqualTo("userId", uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                allTransactions = result.map { doc ->
                    doc.toObject(Transaction::class.java).also { it.id = doc.id }
                }
                loadBudget()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBudget() {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val monthly = (document.get("monthlyBudget") as? Number)?.toDouble()

                val categories = HashMap<String, Double>()
                val raw = document.get("categoryBudgets") as? Map<*, *>
                raw?.forEach { (key, value) ->
                    val limit = (value as? Number)?.toDouble()
                    if (key is String && limit != null) {
                        categories[key] = limit
                    }
                }

                budget = BudgetData(monthly, categories)
                render()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load budget: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------- Showing the data ----------

    private fun render() {
        val now = Calendar.getInstance()
        val start = ReportDates.startOfMonth(now)
        val end = ReportDates.endOfMonth(now)
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - dayOfMonth + 1

        tvBudgetMonth.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time)

        val spent = BudgetCalculator.expenseTotal(allTransactions, start, end)
        val byCategory = BudgetCalculator.expenseByCategory(allTransactions, start, end)

        renderOverall(spent, now, daysInMonth, dayOfMonth, daysLeft)

        val progress = budget.categoryBudgets
            .map { (category, limit) -> CategoryProgress(category, limit, byCategory[category] ?: 0.0) }
            .sortedByDescending { it.spent / it.limit }

        renderCategories(progress)
        renderTips(BudgetCalculator.tips(spent, budget.monthlyBudget, dayOfMonth, daysInMonth, progress))
    }

    @SuppressLint("SetTextI18n")
    private fun renderOverall(
        spent: Double,
        now: Calendar,
        daysInMonth: Int,
        dayOfMonth: Int,
        daysLeft: Int
    ) {
        val limit = budget.monthlyBudget

        if (limit == null) {
            btnEditBudget.text = "Set budget"
            tvBudgetSpent.text = "₹${ComparisonReport.money(spent)} spent so far"
            progressOverall.visibility = View.GONE
            tvBudgetRemaining.visibility = View.GONE
            layoutMetrics.visibility = View.GONE

            layoutSuggestion.visibility = View.VISIBLE
            val suggestion = BudgetCalculator.suggestedBudget(allTransactions, now)
            if (suggestion != null) {
                tvSuggestion.text = "Based on your last few months, a budget of " +
                        "₹${ComparisonReport.money(suggestion)} would fit your usual spending."
                btnUseSuggestion.text = "Use ₹${ComparisonReport.money(suggestion)}"
                btnUseSuggestion.visibility = View.VISIBLE
                btnUseSuggestion.setOnClickListener { saveMonthlyBudget(suggestion) }
            } else {
                tvSuggestion.text = "Set a monthly spending limit to see how much you can still spend."
                btnUseSuggestion.visibility = View.GONE
            }
            return
        }

        btnEditBudget.text = "Edit"
        layoutSuggestion.visibility = View.GONE
        tvBudgetSpent.text = "₹${ComparisonReport.money(spent)} of ₹${ComparisonReport.money(limit)}"

        val ratio = spent / limit
        val statusColor = ContextCompat.getColor(this, BudgetCalculator.statusColor(ratio))

        progressOverall.visibility = View.VISIBLE
        progressOverall.setProgressCompat(min(100, (ratio * 100).toInt()), true)
        progressOverall.setIndicatorColor(statusColor)

        val remaining = limit - spent
        tvBudgetRemaining.visibility = View.VISIBLE
        tvBudgetRemaining.text = if (remaining >= 0) {
            "₹${ComparisonReport.money(remaining)} left this month"
        } else {
            "Over budget by ₹${ComparisonReport.money(-remaining)}"
        }
        tvBudgetRemaining.setTextColor(statusColor)

        layoutMetrics.visibility = View.VISIBLE
        tvDaysLeft.text = daysLeft.toString()
        tvSafePerDay.text = if (remaining > 0) {
            "₹${ComparisonReport.money(remaining / daysLeft)}"
        } else {
            "₹0"
        }

        val normalColor = ContextCompat.getColor(this, R.color.sm_text_primary)
        if (dayOfMonth >= 3) {
            val projected = spent / dayOfMonth * daysInMonth
            tvProjected.text = "₹${ComparisonReport.money(projected)}"
            val projectedColor = if (projected > limit) R.color.sm_expense else R.color.sm_text_primary
            tvProjected.setTextColor(ContextCompat.getColor(this, projectedColor))
        } else {
            tvProjected.text = "-"
            tvProjected.setTextColor(normalColor)
        }
    }

    private fun renderCategories(progress: List<CategoryProgress>) {
        if (progress.isEmpty()) {
            tvCategoryEmpty.visibility = View.VISIBLE
            rvCategories.visibility = View.GONE
            return
        }

        tvCategoryEmpty.visibility = View.GONE
        rvCategories.visibility = View.VISIBLE
        rvCategories.adapter = BudgetCategoryAdapter(progress) { row ->
            showAmountDialog(
                title = "${row.category} budget",
                current = row.limit,
                canRemove = true,
                onSave = { saveCategoryBudget(row.category, it) },
                onRemove = { removeCategoryBudget(row.category) }
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderTips(tips: List<Insight>) {
        layoutTips.removeAllViews()
        val padding = (6 * resources.displayMetrics.density).toInt()

        for (tip in tips) {
            val textView = TextView(this)
            textView.text = "•  ${tip.text}"
            textView.textSize = 14f
            textView.setPadding(0, padding, 0, padding)

            val colorRes = when (tip.good) {
                true -> R.color.sm_income
                false -> R.color.sm_expense
                null -> R.color.sm_text_primary
            }
            textView.setTextColor(ContextCompat.getColor(this, colorRes))
            layoutTips.addView(textView)
        }
    }

    // ---------- Dialogs ----------

    private fun showAmountDialog(
        title: String,
        current: Double?,
        canRemove: Boolean,
        onSave: (Double) -> Unit,
        onRemove: () -> Unit
    ) {
        val padding = (20 * resources.displayMetrics.density).toInt()

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Amount in ₹"
        if (current != null) {
            input.setText(ComparisonReport.money(current))
            input.setSelection(input.text.length)
        }

        val container = FrameLayout(this)
        container.setPadding(padding, padding / 2, padding, 0)
        container.addView(input)

        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val amount = input.text.toString().trim().toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Enter an amount above zero", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(amount)
                }
            }
            .setNegativeButton("Cancel", null)

        if (canRemove) {
            builder.setNeutralButton("Remove") { _, _ -> onRemove() }
        }

        builder.show()
    }

    private fun showAddCategoryDialog() {
        val fromData = allTransactions.filter { it.type == "expense" }.map { it.category }
        val names = (CategoryIcons.expense.map { it.first } + fromData)
            .filter { it.isNotBlank() && !budget.categoryBudgets.containsKey(it) }
            .distinct()

        if (names.isEmpty()) {
            Toast.makeText(this, "Every category already has a budget", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = names.map { name ->
            val icon = CategoryIcons.iconFor(name)
            if (icon != null) "$icon  $name" else name
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Choose a category")
            .setItems(labels) { _, which ->
                val name = names[which]
                showAmountDialog(
                    title = "$name budget",
                    current = null,
                    canRemove = false,
                    onSave = { saveCategoryBudget(name, it) },
                    onRemove = {}
                )
            }
            .show()
    }

    // ---------- Saving ----------

    private fun saveMonthlyBudget(amount: Double) {
        db.collection("users").document(userId)
            .set(mapOf("monthlyBudget" to amount), SetOptions.merge())
            .addOnSuccessListener {
                loadBudget()
                askNotificationPermission()
                BudgetAlertChecker.check(applicationContext)
            }
            .addOnFailureListener { e -> showSaveError(e.message) }
    }



    private fun removeMonthlyBudget() {
        db.collection("users").document(userId)
            .update("monthlyBudget", FieldValue.delete())
            .addOnSuccessListener { loadBudget() }
            .addOnFailureListener { e -> showSaveError(e.message) }
    }

    private fun saveCategoryBudget(category: String, amount: Double) {
        db.collection("users").document(userId)
            .set(mapOf("categoryBudgets" to mapOf(category to amount)), SetOptions.merge())
            .addOnSuccessListener {
                loadBudget()
                askNotificationPermission()
                BudgetAlertChecker.check(applicationContext)
            }
            .addOnFailureListener { e -> showSaveError(e.message) }
    }

    private fun removeCategoryBudget(category: String) {
        db.collection("users").document(userId)
            .update(FieldPath.of("categoryBudgets", category), FieldValue.delete())
            .addOnSuccessListener { loadBudget() }
            .addOnFailureListener { e -> showSaveError(e.message) }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private fun showSaveError(message: String?) {
        Toast.makeText(this, "Could not save: $message", Toast.LENGTH_SHORT).show()
    }
}