package com.example.madproject

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VersionRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.documentDao()

    /**
     * Saves a new snapshot version of the document.
     * Stored as a delta patch relative to the previous version state.
     */
    suspend fun saveVersion(file: File, currentContent: String, versionName: String = "Auto Snapshot"): VersionEntity = withContext(Dispatchers.IO) {
        var doc = dao.getDocumentByPath(file.absolutePath)

        // 1. If document is not tracked yet, create DocumentEntity
        if (doc == null) {
            val newDoc = DocumentEntity(
                fileName = file.name,
                baseFilePath = file.absolutePath
            )
            val docId = dao.insertDocument(newDoc)
            doc = dao.getDocumentById(docId)!!

            // Initial base version has empty patch (base file serves as V1 content)
            val initialVersion = VersionEntity(
                documentId = doc.id,
                versionNumber = 1,
                versionName = "Initial Version",
                patchData = ""
            )
            dao.insertVersion(initialVersion)

            // Save the base file text directly to disk
            FileManager.writeFile(file, currentContent)
            return@withContext initialVersion
        }

        // 2. Fetch all previous versions to reconstruct the latest version text
        val existingVersions = dao.getVersionsForDocument(doc.id)
        val baseContent = FileManager.readFile(file)

        val nonInitialPatches = existingVersions.drop(1).map { it.patchData }
        val latestText = DiffUtilsManager.applyPatches(baseContent, nonInitialPatches)

        // 3. Compute the patch/delta between latest text and current new content
        val patchString = DiffUtilsManager.createPatch(latestText, currentContent)

        val nextVersionNumber = existingVersions.size + 1
        val newVersion = VersionEntity(
            documentId = doc.id,
            versionNumber = nextVersionNumber,
            versionName = "$versionName #$nextVersionNumber",
            patchData = patchString
        )

        dao.insertVersion(newVersion)
        return@withContext newVersion
    }

    /**
     * Reconstructs the full content of a specific version number.
     */
    suspend fun getVersionContent(file: File, targetVersionNumber: Int): String = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentByPath(file.absolutePath) ?: return@withContext ""
        val allVersions = dao.getVersionsForDocument(doc.id)

        val baseContent = FileManager.readFile(file)
        val patchesToApply = allVersions
            .filter { it.versionNumber in 2..targetVersionNumber }
            .map { it.patchData }

        return@withContext DiffUtilsManager.applyPatches(baseContent, patchesToApply)
    }

    suspend fun getVersionHistory(file: File): List<VersionEntity> = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentByPath(file.absolutePath) ?: return@withContext emptyList()
        return@withContext dao.getVersionsForDocument(doc.id)
    }
    suspend fun deleteHistoryForFile(file: File) {
        dao.deleteDocumentByPath(file.absolutePath)
    }

    suspend fun setReadOnly(file: File, isReadOnly: Boolean) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentByPath(file.absolutePath)
        doc?.let {
            dao.updateReadOnlyStatus(it.id, isReadOnly)
        }
    }

    suspend fun isReadOnly(file: File): Boolean = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentByPath(file.absolutePath)
        return@withContext doc?.isReadOnly ?: false
    }
}