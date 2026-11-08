package com.wisdomtower.academy.data.content

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/** One picture ready to send: [dataUrl] goes to the server, [thumbUrl] to the chat page, [thumb] to the composer strip. */
class TutorPicture(val dataUrl: String, val thumbUrl: String, val thumb: Bitmap)

/**
 * Picture helper of the AI tutor (owner request 2026-11-08: attach pictures from the gallery to a tutor question).
 *
 * ADMIN ONLY for now: [isAllowed] is true only for the admin e-mail below (the same one the website route uses for the
 * NVIDIA tryout, `src/lib/aiTutorAdminModel.ts`). The server also ignores pictures from anyone else. To open the
 * feature to every student later, change [isAllowed] here AND the server check; nothing else.
 *
 * [encode] reads a gallery picture, fixes its rotation, shrinks it (longest side 1280 px, lower if needed) and saves it
 * as a JPEG of about 170 KB at most, so a question never gets near the server's 4.5 MB request limit and stays cheap
 * on mobile data. Returns null when the picture cannot be read. Call it off the main thread.
 */
object TutorImageUtil {
    const val MAX_IMAGES = 3
    private const val TARGET_BYTES = 170_000
    private const val FULL_SIDE = 1280
    private const val THUMB_SIDE = 320
    private val ADMIN_EMAILS = setOf("hiyabteklu720@gmail.com")

    /** (longest side, JPEG quality) tried in order until the file is small enough. */
    private val ATTEMPTS = listOf(1280 to 75, 1280 to 60, 1024 to 65, 1024 to 50, 800 to 55)

    fun isAllowed(email: String?): Boolean =
        !email.isNullOrBlank() && ADMIN_EMAILS.contains(email.trim().lowercase())

    fun encode(context: Context, uri: Uri): TutorPicture? {
        return try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= FULL_SIDE) sample *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
                ?: return null

            val rotation = readRotation(context, uri)
            var bmp = if (rotation != 0f) {
                val m = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
            } else {
                decoded
            }
            bmp = flatten(bmp)

            var bytes = ByteArray(0)
            for ((side, quality) in ATTEMPTS) {
                bytes = jpeg(bmp, side, quality)
                if (bytes.size <= TARGET_BYTES) break
            }
            val thumbBytes = jpeg(bmp, THUMB_SIDE, 70)
            val thumbBitmap = BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size) ?: return null

            TutorPicture(
                dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP),
                thumbUrl = "data:image/jpeg;base64," + Base64.encodeToString(thumbBytes, Base64.NO_WRAP),
                thumb = thumbBitmap
            )
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun readRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    /** A transparent picture (PNG) would turn black as a JPEG: paint it on white first. */
    private fun flatten(src: Bitmap): Bitmap {
        if (!src.hasAlpha()) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(src, 0f, 0f, null)
        return out
    }

    private fun jpeg(src: Bitmap, maxSide: Int, quality: Int): ByteArray {
        val longest = maxOf(src.width, src.height)
        val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                src,
                maxOf(1, (src.width * scale).toInt()),
                maxOf(1, (src.height * scale).toInt()),
                true
            )
        } else {
            src
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== src) scaled.recycle()
        return out.toByteArray()
    }
}
