package com.example.madproject

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val baseFilePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isReadOnly: Boolean = false
)

@Entity(
    tableName = "versions",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VersionEntity(
    @PrimaryKey(autoGenerate = true)
    val versionId: Long = 0,
    val documentId: Long,
    val versionNumber: Int,
    val versionName: String,
    val patchData: String, // Unified diff delta text string
    val timestamp: Long = System.currentTimeMillis()
)