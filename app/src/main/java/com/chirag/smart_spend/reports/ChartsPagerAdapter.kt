package com.chirag.smart_spend.reports

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ChartsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MonthPieFragment()
            1 -> DayPieFragment()
            2 -> TrendsFragment()
            else -> CompareFragment()
        }
    }
}