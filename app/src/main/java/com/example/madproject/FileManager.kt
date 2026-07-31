package com.example.madproject // Ensure this matches your project's package name

import android.content.Context
import java.io.File
import java.nio.charset.Charset

object FileManager {

    // Read file content using UTF-8 encoding by default
    fun readFile(file: File, charset: Charset = Charsets.UTF_8): String {
        return file.readText(charset)
    }

    // Write text content to file using UTF-8 encoding
    fun writeFile(file: File, content: String, charset: Charset = Charsets.UTF_8) {
        file.writeText(content, charset)
    }


    // Create a new file in local app-specific storage
    fun createNewFile(context: Context, fileName: String): File {
        val storageDir = context.filesDir
        val newFile = File(storageDir, fileName)
        if (!newFile.exists()) {
            newFile.createNewFile()
        }

        return newFile
    }
    fun deleteFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }
}