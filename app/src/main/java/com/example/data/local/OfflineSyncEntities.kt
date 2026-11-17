package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_ops")
data class PendingSyncOpEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
    val schoolId: String,
    val collectionName: String,
    val documentId: String,
    val opType: String, // CREATE or UPDATE
    val payloadJson: String,
    val summaryLabel: String,
    val baseServerUpdatedAtMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING" // PENDING, CONFLICT, SYNCED
)

@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true) val conflictId: Long = 0L,
    val pendingOpId: Long,
    val schoolId: String,
    val collectionName: String,
    val documentId: String,
    val summaryLabel: String,
    val localPayloadJson: String,
    val serverUpdatedAtMillis: Long,
    val detectedAtMillis: Long = System.currentTimeMillis(),
    val resolutionStatus: String = "UNRESOLVED" // UNRESOLVED, RESOLVED_SERVER, RESOLVED_LOCAL
)
