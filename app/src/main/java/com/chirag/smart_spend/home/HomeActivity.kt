package com.chirag.smart_spend.home

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.chirag.smart_spend.R
import com.chirag.smart_spend.ThemeManager
import com.chirag.smart_spend.auth.LoginActivity
import com.chirag.smart_spend.investments.InvestFragment
import com.chirag.smart_spend.profile.ProfileFragment
import com.chirag.smart_spend.reports.ChartsFragment
import com.chirag.smart_spend.transactions.AddTransactionActivity
import com.chirag.smart_spend.transactions.TransactionsFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.chirag.smart_spend.transactions.AllTransactionsActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private var currentMenu = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val navView = findViewById<NavigationView>(R.id.navView)
        val fab = findViewById<FloatingActionButton>(R.id.fabAdd)

        setupToolbar()
        setupDrawer(navView)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showTab("SmartSpend", TransactionsFragment(), R.menu.menu_toolbar)
                R.id.nav_charts -> showTab("Charts", ChartsFragment(), R.menu.menu_charts)
                R.id.nav_invest -> showTab("Invest", InvestFragment())
                R.id.nav_profile -> showTab("Profile", ProfileFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            savedInstanceState.getCharSequence("title")?.let { toolbar.title = it }
            val savedMenu = savedInstanceState.getInt("menu", 0)
            if (savedMenu != 0) {
                toolbar.inflateMenu(savedMenu)
                currentMenu = savedMenu
            }
        }

        fab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        toolbar.setOnMenuItemClickListener { item ->
            val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            (current as? ToolbarActionHandler)?.onToolbarAction(item.itemId) ?: false
        }
    }

    private fun setupDrawer(navView: NavigationView) {
        val header = navView.getHeaderView(0)
        val tvAvatar = header.findViewById<TextView>(R.id.tvAvatar)
        val tvName = header.findViewById<TextView>(R.id.tvDrawerName)
        val tvEmail = header.findViewById<TextView>(R.id.tvDrawerEmail)

        val user = auth.currentUser
        tvEmail.text = user?.email ?: ""

        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "User"
                    tvName.text = name
                    tvAvatar.text = name.take(1).uppercase()
                }
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.drawer_all_transactions ->
                    startActivity(Intent(this, AllTransactionsActivity::class.java))
                R.id.drawer_notification_access ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                R.id.drawer_dark_mode -> showThemeDialog()
                R.id.drawer_about -> showAboutDialog()
                R.id.drawer_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun showThemeDialog() {
        val options = arrayOf("System default", "Light", "Dark")
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
        )
        val checked = modes.indexOf(ThemeManager.getSavedMode(this)).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Dark mode")
            .setSingleChoiceItems(options, checked) { dialog, which ->
                ThemeManager.saveAndApply(this, modes[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }

        AlertDialog.Builder(this)
            .setTitle("SmartSpend")
            .setMessage(
                "Version $versionName\n\n" +
                        "Track your income and expenses automatically from transaction " +
                        "notifications, see monthly reports and charts, and get simple savings tips."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTab(title: String, fragment: Fragment, menuRes: Int = 0) {
        toolbar.title = title
        toolbar.menu.clear()
        if (menuRes != 0) {
            toolbar.inflateMenu(menuRes)
        }
        currentMenu = menuRes
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("title", toolbar.title)
        outState.putInt("menu", currentMenu)
    }
}