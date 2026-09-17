package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileStorageHelper {

    /**
     * Copies a user-selected content URI (from SAF or GetContent) into internal storage
     * so that the app has permanent, unrestricted access across app restarts, reboots,
     * and background services without any SecurityException or permission loss.
     *
     * @return Absolute path to the saved file in internal storage, or the original URI string if copy fails.
     */
    fun saveUriToInternalStorage(
        context: Context,
        uri: Uri,
        folderName: String = "custom_audio",
        filePrefix: String = "audio"
    ): String {
        return try {
            // Attempt to persist URI permission if applicable
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {
                // Not all ContentProviders support persistable permissions, that's okay
            }

            val storageDir = File(context.filesDir, folderName)
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val originalFileName = queryFileName(context, uri)
            val extension = originalFileName?.substringAfterLast('.', "")
                ?.takeIf { it.isNotBlank() }
                ?: (context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "mp3")

            val safeExtension = when (extension.lowercase()) {
                "mpeg", "mpga" -> "mp3"
                "wav", "ogg", "m4a", "aac", "mp4", "mkv", "webm" -> extension.lowercase()
                else -> "mp3"
            }

            val destFileName = "${filePrefix}_${System.currentTimeMillis()}.$safeExtension"
            val destFile = File(storageDir, destFileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                destFile.absolutePath
            } else {
                uri.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return uri.lastPathSegment
    }
}
