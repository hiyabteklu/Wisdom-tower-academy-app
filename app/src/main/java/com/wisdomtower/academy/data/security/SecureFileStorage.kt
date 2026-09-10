package com.wisdomtower.academy.data.security

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Manages private app storage for educational resources (PDFs, notes).
 * Files are isolated in app-private sandbox storage and are inaccessible to other apps.
 * No file provider or external intents are used to share the raw unlocked files.
 */
class SecureFileStorage(private val context: Context) {

    private val secureDir: File by lazy {
        File(context.filesDir, SECURE_CONTENT_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun saveResourceFile(resourceId: String, data: ByteArray, extension: String = "bin"): File {
        val safeName = "${sanitizeFilename(resourceId)}.$extension"
        val targetFile = File(secureDir, safeName)
        FileOutputStream(targetFile).use { output ->
            output.write(data)
            output.flush()
        }
        return targetFile
    }

    fun getResourceFile(resourceId: String, extension: String = "bin"): File? {
        val safeName = "${sanitizeFilename(resourceId)}.$extension"
        val file = File(secureDir, safeName)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun deleteResourceFile(resourceId: String, extension: String = "bin"): Boolean {
        val safeName = "${sanitizeFilename(resourceId)}.$extension"
        val file = File(secureDir, safeName)
        return if (file.exists()) file.delete() else true
    }

    fun wipeAllSecureFiles(): Boolean {
        return try {
            if (secureDir.exists()) {
                secureDir.listFiles()?.forEach { it.delete() }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUsedStorageBytes(): Long {
        if (!secureDir.exists()) return 0L
        return secureDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun sanitizeFilename(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    companion object {
        private const val SECURE_CONTENT_DIR = "secure_academic_vault"
    }
}
