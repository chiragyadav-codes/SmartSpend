package com.chirag.smart_spend.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chirag.smart_spend.R
import com.chirag.smart_spend.auth.LoginActivity
import com.chirag.smart_spend.categorization.CategoryIcons
import com.chirag.smart_spend.home.DrawerHeaderBinder
import com.chirag.smart_spend.reports.ComparisonReport
import com.chirag.smart_spend.transactions.BillImageHelper
import com.chirag.smart_spend.transactions.model.Transaction
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var layoutAvatar: FrameLayout
    private lateinit var tvAvatar: TextView
    private lateinit var ivPhoto: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvTransactionCount: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var tvTopCategory: TextView
    private lateinit var tvBiggestExpense: TextView

    private var pendingCameraFile: File? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (file != null) {
            if (success) {
                saveProfilePhoto(Uri.fromFile(file), file)
            } else {
                file.delete()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            saveProfilePhoto(uri, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString("pendingCamera")?.let { pendingCameraFile = File(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingCameraFile?.let { outState.putString("pendingCamera", it.absolutePath) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutAvatar = view.findViewById(R.id.layoutAvatar)
        tvAvatar = view.findViewById(R.id.tvProfileAvatar)
        ivPhoto = view.findViewById(R.id.ivProfilePhoto)
        tvName = view.findViewById(R.id.tvProfileName)
        tvEmail = view.findViewById(R.id.tvProfileEmail)
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvMemberSince = view.findViewById(R.id.tvMemberSince)
        tvNetBalance = view.findViewById(R.id.tvNetBalance)
        tvTopCategory = view.findViewById(R.id.tvTopCategory)
        tvBiggestExpense = view.findViewById(R.id.tvBiggestExpense)

        layoutAvatar.setOnClickListener { showPhotoOptions() }
        tvName.setOnClickListener { showEditNameDialog() }
        view.findViewById<Button>(R.id.btnEditName).setOnClickListener { showEditNameDialog() }
        view.findViewById<Button>(R.id.btnResetPassword).setOnClickListener { sendPasswordReset() }
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener { confirmLogout() }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    // ---------- Loading ----------

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        tvEmail.text = user.email ?: ""
        showAvatar()

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                showName(document.getString("name") ?: "User")

                val createdAt = document.getLong("createdAt")
                tvMemberSince.text = if (createdAt != null) {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdAt))
                } else {
                    "-"
                }
            }

        db.collection("transactions")
            .whereEqualTo("userId", user.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener
                val all = result.map { doc ->
                    doc.toObject(Transaction::class.java).also { it.id = doc.id }
                }
                showStats(all)
            }
    }

    private fun showName(name: String) {
        tvName.text = name
        tvAvatar.text = name.take(1).uppercase()
    }

    private fun showAvatar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val photo = ProfilePhotoHelper.load(requireContext(), uid)

        if (photo != null) {
            ivPhoto.setImageBitmap(photo)
            ivPhoto.visibility = View.VISIBLE
            tvAvatar.visibility = View.GONE
        } else {
            ivPhoto.visibility = View.GONE
            tvAvatar.visibility = View.VISIBLE
        }
    }

    private fun showStats(all: List<Transaction>) {
        val income = all.filter { it.type == "income" }.sumOf { it.amount }
        val expenses = all.filter { it.type == "expense" }
        val expense = expenses.sumOf { it.amount }
        val net = income - expense

        tvTransactionCount.text = all.size.toString()
        tvTotalIncome.text = "₹${ComparisonReport.money(income)}"
        tvTotalExpense.text = "₹${ComparisonReport.money(expense)}"

        tvNetBalance.text = if (net >= 0) {
            "₹${ComparisonReport.money(net)}"
        } else {
            "-₹${ComparisonReport.money(-net)}"
        }
        val netColor = if (net >= 0) R.color.sm_income else R.color.sm_expense
        tvNetBalance.setTextColor(ContextCompat.getColor(requireContext(), netColor))

        val top = expenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .maxByOrNull { it.value }

        tvTopCategory.text = if (top == null) {
            "-"
        } else {
            val icon = CategoryIcons.iconFor(top.key)
            val prefix = if (icon != null) "$icon  " else ""
            "$prefix${top.key} · ₹${ComparisonReport.money(top.value)}"
        }

        val biggest = expenses.maxByOrNull { it.amount }
        tvBiggestExpense.text = if (biggest == null) {
            "-"
        } else {
            "₹${ComparisonReport.money(biggest.amount)} · ${biggest.category}"
        }
    }

    // ---------- Profile picture ----------

    private fun showPhotoOptions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hasPhoto = ProfilePhotoHelper.hasPhoto(requireContext(), uid)

        val options = mutableListOf("Take photo", "Choose from gallery")
        if (hasPhoto) options.add("Remove photo")

        AlertDialog.Builder(requireContext())
            .setTitle("Profile picture")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    2 -> {
                        ProfilePhotoHelper.remove(requireContext(), uid)
                        showAvatar()
                        refreshDrawer()
                    }
                }
            }
            .show()
    }

    private fun takePhoto() {
        try {
            val file = BillImageHelper.newCameraFile(requireContext())
            pendingCameraFile = file
            takePictureLauncher.launch(BillImageHelper.uriFor(requireContext(), file))
        } catch (e: Exception) {
            pendingCameraFile = null
            Toast.makeText(requireContext(), "Could not open the camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfilePhoto(uri: Uri, sourceFile: File?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            sourceFile?.delete()
            return
        }

        val appContext = requireContext().applicationContext

        Thread {
            val ok = ProfilePhotoHelper.save(appContext, uid, uri)
            sourceFile?.delete()

            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread

                if (ok) {
                    showAvatar()
                    refreshDrawer()
                    Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Could not read that image", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ---------- Name ----------

    private fun showEditNameDialog() {
        val padding = (20 * resources.displayMetrics.density).toInt()

        val input = EditText(requireContext())
        input.hint = "Your name"
        input.setText(tvName.text)
        input.setSelection(input.text.length)

        val container = FrameLayout(requireContext())
        container.setPadding(padding, padding / 2, padding, 0)
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit name")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    saveName(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveName(newName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("name" to newName), SetOptions.merge())
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                showName(newName)
                refreshDrawer()
                Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------- Other actions ----------

    private fun refreshDrawer() {
        val navView = requireActivity().findViewById<NavigationView>(R.id.navView) ?: return
        DrawerHeaderBinder.bind(requireContext(), navView)
    }

    private fun sendPasswordReset() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No email found for this account", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Password reset email sent to $email", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Do you want to log out of SmartSpend?")
            .setPositiveButton("Log out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}