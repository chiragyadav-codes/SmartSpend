package com.chirag.smart_spend.home

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chirag.smart_spend.R
import com.chirag.smart_spend.profile.ProfilePhotoHelper
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object DrawerHeaderBinder {

    fun bind(context: Context, navView: NavigationView) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val header = navView.getHeaderView(0)
        val tvAvatar = header.findViewById<TextView>(R.id.tvAvatar)
        val ivPhoto = header.findViewById<ImageView>(R.id.ivDrawerPhoto)
        val tvName = header.findViewById<TextView>(R.id.tvDrawerName)
        val tvEmail = header.findViewById<TextView>(R.id.tvDrawerEmail)

        tvEmail.text = user.email ?: ""

        val photo = ProfilePhotoHelper.load(context, user.uid)
        if (photo != null) {
            ivPhoto.setImageBitmap(photo)
            ivPhoto.visibility = View.VISIBLE
            tvAvatar.visibility = View.GONE
        } else {
            ivPhoto.visibility = View.GONE
            tvAvatar.visibility = View.VISIBLE
        }

        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "User"
                tvName.text = name
                tvAvatar.text = name.take(1).uppercase()
            }
    }
}