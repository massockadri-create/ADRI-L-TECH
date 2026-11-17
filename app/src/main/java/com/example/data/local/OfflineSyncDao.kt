package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineSyncDao {

    @Query("SELECT * FROM pending_sync_ops WHERE schoolId = :schoolId AND syncStatus != 'SYNCED' ORDER BY createdAtMillis DESC")
    fun observePendingOps(schoolId: String): Flow<List<PendingSyncOpEntity>>

    @Query("SELECT * FROM pending_sync_ops WHERE schoolId = :schoolId AND syncStatus = 'PENDING' ORDER BY createdAtMillis ASC")
    suspend fun getPendingOpsOnce(schoolId: String): List<PendingSyncOpEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOp(op: PendingSyncOpEntity): Long

    @Query("UPDATE pending_sync_ops SET syncStatus = :status WHERE localId = :localId")
    suspend fun updatePendingOpStatus(localId: Long, status: String)

    @Query("DELETE FROM pending_sync_ops WHERE localId = :localId")
    suspend fun deletePendingOp(localId: Long)

    @Query("SELECT * FROM sync_conflicts WHERE schoolId = :schoolId AND resolutionStatus = 'UNRESOLVED' ORDER BY detectedAtMillis DESC")
    fun observeUnresolvedConflicts(schoolId: String): Flow<List<SyncConflictEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflict(conflict: SyncConflictEntity): Long

    @Query("UPDATE sync_conflicts SET resolutionStatus = :status WHERE conflictId = :conflictId")
    suspend fun updateConflictStatus(conflictId: Long, status: String)
}
