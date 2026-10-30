package com.chirag.smart_spend.transactions

import com.chirag.smart_spend.budget.BudgetAlertChecker
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.chirag.smart_spend.R
import com.chirag.smart_spend.categorization.CategoryIcons
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvScreenTitle: TextView
    private lateinit var rgType: RadioGroup
    private lateinit var etAmount: EditText
    private lateinit var etCategory: EditText
    private lateinit var etNote: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var layoutBill: LinearLayout
    private lateinit var layoutBillPreview: FrameLayout
    private lateinit var ivBillPreview: ImageView
    private lateinit var btnSave: Button

    private var editingTransactionId: String? = null
    private var updatingCategory = false
    private var saved = false

    // The bill photo stored with the transaction (edit mode only).
    private var originalBillPath = ""

    // The bill photo currently shown on this screen.
    private var currentBillPath = ""

    private var pendingCameraFile: File? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (file != null) {
            if (success) {
                attachImage(Uri.fromFile(file), file)
            } else {
                file.delete()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachImage(uri, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvScreenTitle = findViewById(R.id.tvScreenTitle)
        rgType = findViewById(R.id.rgType)
        etAmount = findViewById(R.id.etAmount)
        etCategory = findViewById(R.id.etCategory)
        etNote = findViewById(R.id.etNote)
        chipGroup = findViewById(R.id.chipGroupCategories)
        layoutBill = findViewById(R.id.layoutBill)
        layoutBillPreview = findViewById(R.id.layoutBillPreview)
        ivBillPreview = findViewById(R.id.ivBillPreview)
        btnSave = findViewById(R.id.btnSaveTransaction)

        buildCategoryChips()

        rgType.setOnCheckedChangeListener { _, _ ->
            buildCategoryChips()
            updateBillSectionVisibility()
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (updatingCategory || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(checkedIds[0]) ?: return@setOnCheckedStateChangeListener

            updatingCategory = true
            etCategory.setText(chip.tag as String)
            etCategory.setSelection(etCategory.text.length)
            updatingCategory = false
        }

        etCategory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!updatingCategory) syncChipWithText()
            }
        })

        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener { takePhoto() }

        findViewById<Button>(R.id.btnPickPhoto).setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        findViewById<TextView>(R.id.tvRemoveBill).setOnClickListener {
            discardSessionBill()
            currentBillPath = ""
            showBillPreview()
        }

        ivBillPreview.setOnClickListener { openBillViewer() }

        editingTransactionId = intent.getStringExtra("transactionId")
        val transactionId = editingTransactionId
        if (transactionId != null) {
            tvScreenTitle.text = "Edit Transaction"
            btnSave.text = "Update Transaction"
            loadTransaction(transactionId)
        }

        btnSave.setOnClickListener { saveTransaction() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !saved) {
            discardSessionBill()
        }
    }

    // ---------- Category icons ----------

    private fun buildCategoryChips() {
        val isIncome = rgType.checkedRadioButtonId == R.id.rbIncome
        val options = if (isIncome) CategoryIcons.income else CategoryIcons.expense

        updatingCategory = true
        chipGroup.removeAllViews()
        for ((name, icon) in options) {
            val chip = Chip(this)
            chip.text = "$icon  $name"
            chip.tag = name
            chip.isCheckable = true
            chip.id = View.generateViewId()
            chipGroup.addView(chip)
        }
        updatingCategory = false

        syncChipWithText()
    }

    // Highlights the icon that matches what is typed in the Category box.
    private fun syncChipWithText() {
        val text = etCategory.text.toString().trim()

        updatingCategory = true
        chipGroup.clearCheck()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if ((chip.tag as String).equals(text, ignoreCase = true)) {
                chip.isChecked = true
                break
            }
        }
        updatingCategory = false
    }

    private fun updateBillSectionVisibility() {
        val isIncome = rgType.checkedRadioButtonId == R.id.rbIncome
        layoutBill.visibility = if (isIncome) View.GONE else View.VISIBLE
    }

    // ---------- Bill photo ----------

    private fun takePhoto() {
        try {
            val file = BillImageHelper.newCameraFile(this)
            pendingCameraFile = file
            takePictureLauncher.launch(BillImageHelper.uriFor(this, file))
        } catch (e: Exception) {
            pendingCameraFile = null
            Toast.makeText(this, "Could not open the camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attachImage(uri: Uri, sourceFile: File?) {
        Thread {
            val path = BillImageHelper.saveFromUri(this, uri)
            sourceFile?.delete()

            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    path?.let { BillImageHelper.delete(it) }
                    return@runOnUiThread
                }

                if (path == null) {
                    Toast.makeText(this, "Could not read that image", Toast.LENGTH_SHORT).show()
                } else {
                    discardSessionBill()
                    currentBillPath = path
                    showBillPreview()
                }
            }
        }.start()
    }

    // Deletes the shown photo if it was added during this visit to the screen.
    private fun discardSessionBill() {
        if (currentBillPath.isNotEmpty() && currentBillPath != originalBillPath) {
            BillImageHelper.delete(currentBillPath)
        }
    }

    private fun showBillPreview() {
        if (currentBillPath.isEmpty()) {
            layoutBillPreview.visibility = View.GONE
            return
        }

        val file = File(currentBillPath)
        if (!file.exists()) {
            currentBillPath = ""
            layoutBillPreview.visibility = View.GONE
            return
        }

        ivBillPreview.setImageURI(Uri.fromFile(file))
        layoutBillPreview.visibility = View.VISIBLE
    }

    private fun openBillViewer() {
        if (currentBillPath.isEmpty()) return

        val uri = BillImageHelper.uriFor(this, File(currentBillPath))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open the image", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Load and save ----------

    private fun loadTransaction(id: String) {
        db.collection("transactions").document(id)
            .get()
            .addOnSuccessListener { document ->
                val transaction = document.toObject(Transaction::class.java)
                    ?: return@addOnSuccessListener

                if (transaction.type == "income") {
                    rgType.check(R.id.rbIncome)
                } else {
                    rgType.check(R.id.rbExpense)
                }

                etAmount.setText(transaction.amount.toString())
                etCategory.setText(transaction.category)
                etNote.setText(transaction.note)

                originalBillPath = transaction.billImagePath
                currentBillPath = transaction.billImagePath
                showBillPreview()
            }
    }

    private fun saveTransaction() {
        val amountText = etAmount.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val note = etNote.text.toString().trim()

        if (amountText.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please enter amount and category", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "income" else "expense"
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Only expenses keep a bill photo.
        val billToSave = if (type == "expense") currentBillPath else ""

        val transactionId = editingTransactionId
        if (transactionId != null) {
            val updates = hashMapOf<String, Any>(
                "amount" to amount,
                "type" to type,
                "category" to category,
                "note" to note,
                "billImagePath" to billToSave
            )
            db.collection("transactions").document(transactionId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Transaction updated!", Toast.LENGTH_SHORT).show()
                    finishAfterSave(billToSave)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            val transaction = Transaction(
                userId = userId,
                amount = amount,
                type = type,
                category = category,
                note = note,
                date = System.currentTimeMillis(),
                source = "manual",
                billImagePath = billToSave
            )
            db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener {
                    Toast.makeText(this, "Transaction saved!", Toast.LENGTH_SHORT).show()
                    finishAfterSave(billToSave)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Removes photo files that are no longer used, then closes the screen.
    private fun finishAfterSave(savedBillPath: String) {
        saved = true

        if (originalBillPath.isNotEmpty() && originalBillPath != savedBillPath) {
            BillImageHelper.delete(originalBillPath)
        }
        if (currentBillPath.isNotEmpty() &&
            currentBillPath != savedBillPath &&
            currentBillPath != originalBillPath
        ) {
            BillImageHelper.delete(currentBillPath)
        }
        BudgetAlertChecker.check(applicationContext)
        finish()
    }
}