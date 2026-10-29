package com.chirag.smart_spend.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.chirag.smart_spend.transactions.BillImageHelper
import java.io.File

object ProfilePhotoHelper {

    private fun photoFile(context: Context, uid: String): File {
        return File(File(context.filesDir, "profile"), "profile_$uid.jpg")
    }

    fun hasPhoto(context: Context, uid: String): Boolean {
        return photoFile(context, uid).exists()
    }

    fun load(context: Context, uid: String): Bitmap? {
        val file = photoFile(context, uid)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    // Saves a new profile picture, replacing the old one. Returns true on success.
    fun save(context: Context, uid: String, uri: Uri): Boolean {
        val path = BillImageHelper.saveFromUri(
            context,
            uri,
            folder = "profile",
            maxSize = 512,
            fileName = "profile_$uid.jpg"
        )
        return path != null
    }

    fun remove(context: Context, uid: String) {
        photoFile(context, uid).delete()
    }
}