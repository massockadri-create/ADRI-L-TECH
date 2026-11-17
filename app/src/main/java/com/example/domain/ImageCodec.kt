package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCodec {

    fun encodeUriToBase64(context: Context, uri: Uri, maxDimensionPx: Int = 220): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val original = stream.use { BitmapFactory.decodeStream(it) } ?: return null
            val scale = max(original.width, original.height).toFloat() / maxDimensionPx.toFloat()
            val resized = if (scale > 1f) {
                val w = (original.width / scale).toInt().coerceAtLeast(1)
                val h = (original.height / scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(original, w, h, true)
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.PNG, 90, out)
            if (resized !== original) resized.recycle()
            original.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
