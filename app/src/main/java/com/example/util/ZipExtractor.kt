package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipExtractor {

    fun extractZipFromUri(context: Context, zipUri: Uri, targetDirName: String): List<File> {
        val outputFiles = mutableListOf<File>()
        val targetDir = File(context.filesDir, targetDirName)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        } else {
            // Clean previous files in directory
            targetDir.listFiles()?.forEach { it.delete() }
        }

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(zipUri)
            inputStream?.use { stream ->
                ZipInputStream(stream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    val buffer = ByteArray(8192)
                    while (entry != null) {
                        if (!entry.isDirectory && !entry.name.startsWith("__MACOSX") && !entry.name.startsWith(".")) {
                            val cleanFileName = File(entry.name).name
                            if (cleanFileName.isNotBlank()) {
                                val destinationFile = File(targetDir, cleanFileName)
                                FileOutputStream(destinationFile).use { fos ->
                                    var len: Int
                                    while (zipStream.read(buffer).also { len = it } > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                                outputFiles.add(destinationFile)
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return outputFiles.sortedBy { it.name }
    }

    fun getExtractedFiles(context: Context, targetDirName: String): List<File> {
        val targetDir = File(context.filesDir, targetDirName)
        if (!targetDir.exists()) return emptyList()
        return targetDir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }?.sortedBy { it.name } ?: emptyList()
    }
}
