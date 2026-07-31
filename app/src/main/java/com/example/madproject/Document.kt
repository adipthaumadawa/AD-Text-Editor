package com.example.madproject



import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DocumentDao {

    @Insert
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Query("SELECT * FROM documents WHERE baseFilePath = :path LIMIT 1")
    suspend fun getDocumentByPath(path: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :docId")
    suspend fun getDocumentById(docId: Long): DocumentEntity?

    @Insert
    suspend fun insertVersion(version: VersionEntity): Long

    @Query("SELECT * FROM versions WHERE documentId = :docId ORDER BY versionNumber ASC")
    suspend fun getVersionsForDocument(docId: Long): List<VersionEntity>

    @Query("SELECT COUNT(*) FROM versions WHERE documentId = :docId")
    suspend fun getVersionCount(docId: Long): Int

    @Query("UPDATE documents SET isReadOnly = :isReadOnly WHERE id = :docId")
    suspend fun updateReadOnlyStatus(docId: Long, isReadOnly: Boolean)

    @Query("DELETE FROM documents WHERE baseFilePath = :filePath")
    suspend fun deleteDocumentByPath(filePath: String)
}