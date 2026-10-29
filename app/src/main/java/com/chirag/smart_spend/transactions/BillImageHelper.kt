package com.chirag.smart_spend.transactions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object BillImageHelper {

    private fun folderDir(context: Context, folder: String): File {
        val dir = File(context.filesDir, folder)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newCameraFile(context: Context): File {
        val dir = File(context.cacheDir, "camera")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun delete(path: String) {
        if (path.isNotEmpty()) File(path).delete()
    }

    // Reads a photo from the camera or gallery, shrinks it, fixes its rotation,
    // and saves it privately. Returns the saved file's path, or null on failure.
    fun saveFromUri(
        context: Context,
        uri: Uri,
        folder: String = "bills",
        maxSize: Int = 1280,
        fileName: String? = null
    ): String? {
        return try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > maxSize * 2 || bounds.outHeight / sample > maxSize * 2) {
                sample *= 2
            }

            val options = BitmapFactory.Options()
            options.inSampleSize = sample
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val matrix = Matrix()
            when (readOrientation(context, uri)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val scale = min(1f, maxSize.toFloat() / max(decoded.width, decoded.height))
            matrix.postScale(scale, scale)

            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

            val file = File(
                folderDir(context, folder),
                fileName ?: "bill_${System.currentTimeMillis()}.jpg"
            )
            FileOutputStream(file).use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            if (result !== decoded) decoded.recycle()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun readOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }
}