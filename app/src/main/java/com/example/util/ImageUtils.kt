package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtils {
    fun uriToCompressedBase64(context: Context, uri: Uri, maxDimension: Int = 256): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return null

            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width <= maxDimension && height <= maxDimension) {
                1.0f
            } else {
                maxDimension.toFloat() / max(width, height).toFloat()
            }

            val targetWidth = max(1, (width * scale).toInt())
            val targetHeight = max(1, (height * scale).toInt())
            val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            if (scaled != bitmap) {
                bitmap.recycle()
            }

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            scaled.recycle()
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
