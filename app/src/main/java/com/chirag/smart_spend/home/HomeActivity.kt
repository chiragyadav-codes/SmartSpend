package com.chirag.smart_spend.home

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import com.chirag.smart_spend.budget.BudgetPlannerActivity
import com.chirag.smart_spend.investments.InvestFragment
import com.chirag.smart_spend.profile.ProfileFragment
import com.chirag.smart_spend.reports.ChartsFragment
import com.chirag.smart_spend.transactions.AddTransactionActivity
import com.chirag.smart_spend.transactions.AllTransactionsActivity
import com.chirag.smart_spend.transactions.TransactionsFragment

import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

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

        val bottomNav =
            findViewById<BottomNavigationView>(R.id.bottomNav)

        val navView =
            findViewById<NavigationView>(R.id.navView)

        val fab =
            findViewById<FloatingActionButton>(R.id.fabAdd)

        setupToolbar()
        setupDrawer(navView)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    showTab(
                        "SmartSpend",
                        TransactionsFragment(),
                        R.menu.menu_toolbar
                    )
                }

                R.id.nav_charts -> {
                    showTab(
                        "Charts",
                        ChartsFragment(),
                        R.menu.menu_charts
                    )
                }

                R.id.nav_invest -> {
                    showTab(
                        "Invest",
                        InvestFragment()
                    )
                }

                R.id.nav_profile -> {
                    showTab(
                        "Profile",
                        ProfileFragment()
                    )
                }

                else -> return@setOnItemSelectedListener false
            }

            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        } else {
            savedInstanceState.getCharSequence("title")?.let {
                toolbar.title = it
            }

            val savedMenu = savedInstanceState.getInt("menu", 0)

            if (savedMenu != 0) {
                toolbar.menu.clear()
                toolbar.inflateMenu(savedMenu)
                currentMenu = savedMenu
            }
        }

        fab.setOnClickListener {
            startActivity(
                Intent(this@HomeActivity, AddTransactionActivity::class.java)
            )
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    // Toolbar setup
    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        toolbar.setOnMenuItemClickListener { item ->
            val currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            (currentFragment as? ToolbarActionHandler)
                ?.onToolbarAction(item.itemId) ?: false
        }
    }

    // Navigation drawer setup - only ONE listener
    private fun setupDrawer(navView: NavigationView) {
        DrawerHeaderBinder.bind(this, navView)

        navView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.drawer_all_transactions -> {
                    startActivity(
                        Intent(
                            this@HomeActivity,
                            AllTransactionsActivity::class.java
                        )
                    )
                }

                R.id.drawer_budget -> {
                    try {
                        android.util.Log.d(
                            "BudgetDebug",
                            "Budget Planner clicked"
                        )

                        startActivity(
                            Intent(
                                this@HomeActivity,
                                BudgetPlannerActivity::class.java
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "BudgetDebug",
                            "Failed to open BudgetPlannerActivity",
                            e
                        )

                        Toast.makeText(
                            this@HomeActivity,
                            "Unable to open Budget Planner: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                R.id.drawer_notification_access -> {
                    startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    )
                }

                R.id.drawer_dark_mode -> {
                    showThemeDialog()
                }

                R.id.drawer_about -> {
                    showAboutDialog()
                }

                R.id.drawer_logout -> {
                    auth.signOut()

                    startActivity(
                        Intent(
                            this@HomeActivity,
                            LoginActivity::class.java
                        )
                    )

                    finish()
                }

                else -> return@setNavigationItemSelectedListener false
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // Dark mode dialog
    private fun showThemeDialog() {
        val options = arrayOf(
            "System default",
            "Light",
            "Dark"
        )

        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
        )

        val checked =
            modes.indexOf(ThemeManager.getSavedMode(this)).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Dark mode")
            .setSingleChoiceItems(options, checked) { dialog, which ->
                ThemeManager.saveAndApply(this, modes[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // About dialog
    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }

        AlertDialog.Builder(this)
            .setTitle("SmartSpend")
            .setMessage(
                "Version $versionName\n\n" +
                        "Track your income and expenses, view monthly reports " +
                        "and charts, and get simple savings tips."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // Switch bottom navigation tabs
    private fun showTab(
        title: String,
        fragment: Fragment,
        menuRes: Int = 0
    ) {
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

    // Save toolbar state
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putCharSequence("title", toolbar.title)
        outState.putInt("menu", currentMenu)

        super.onSaveInstanceState(outState)
    }
}
