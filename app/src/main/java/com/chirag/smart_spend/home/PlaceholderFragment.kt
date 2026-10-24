package com.chirag.smart_spend.home

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.chirag.smart_spend.R

class PlaceholderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val textView = TextView(requireContext())
        textView.text = arguments?.getString("label") ?: ""
        textView.gravity = Gravity.CENTER
        textView.textSize = 18f
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.sm_text_secondary))
        return textView
    }

    companion object {
        fun newInstance(label: String): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val args = Bundle()
            args.putString("label", label)
            fragment.arguments = args
            return fragment
        }

    }
}