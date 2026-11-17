package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.OfflineSyncDao
import com.example.data.local.PendingSyncOpEntity
import com.example.data.local.SyncConflictEntity
import com.example.model.School
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

data class SyncRunReport(
    val syncedCount: Int,
    val conflictCount: Int,
    val failedCount: Int
)

class OfflineSyncManager(
    private val dao: OfflineSyncDao,
    private val db: FirebaseFirestore
) {
    constructor(context: Context, dao: OfflineSyncDao) : this(
        dao = dao,
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observePendingOps(schoolId: String): Flow<List<PendingSyncOpEntity>> =
        dao.observePendingOps(schoolId)

    fun observeConflicts(schoolId: String): Flow<List<SyncConflictEntity>> =
        dao.observeUnresolvedConflicts(schoolId)

    suspend fun enqueueOperation(
        school: School,
        collectionName: String,
        documentId: String,
        opType: String,
        fields: Map<String, Any>,
        summaryLabel: String,
        baseServerUpdatedAtMillis: Long = 0L
    ): Long {
        val uid = Firebase.auth.currentUser?.uid ?: school.ownerUid
        val owner = school.ownerUid.ifBlank { uid }
        val members = (school.memberUids + owner + uid).distinct().take(50)
        val json = JSONObject().apply {
            put("schoolId", school.id)
            put("ownerUid", owner)
            put("memberUids", org.json.JSONArray(members))
            fields.forEach { (k, v) -> put(k, v) }
        }.toString()

        return dao.insertPendingOp(
            PendingSyncOpEntity(
                schoolId = school.id,
                collectionName = collectionName,
                documentId = documentId.ifBlank { db.collection("schools").document(school.id).collection(collectionName).document().id },
                opType = opType,
                payloadJson = json,
                summaryLabel = summaryLabel,
                baseServerUpdatedAtMillis = baseServerUpdatedAtMillis
            )
        )
    }

    suspend fun syncPendingOperations(schoolId: String): Result<SyncRunReport> {
        return try {
            val pending = dao.getPendingOpsOnce(schoolId)
            var synced = 0
            var conflicts = 0
            var failed = 0

            for (op in pending) {
                val docRef = db.collection("schools")
                    .document(schoolId)
                    .collection(op.collectionName)
                    .document(op.documentId)

                try {
                    val serverSnap = docRef.get().await()
                    val serverUpdatedMillis = serverSnap.getTimestamp("updatedAt")?.toDate()?.time ?: 0L

                    // Detect conflict if server document exists and was modified after our offline snapshot
                    if (serverSnap.exists() && op.baseServerUpdatedAtMillis > 0L && serverUpdatedMillis > op.baseServerUpdatedAtMillis) {
                        dao.updatePendingOpStatus(op.localId, "CONFLICT")
                        dao.insertConflict(
                            SyncConflictEntity(
                                pendingOpId = op.localId,
                                schoolId = schoolId,
                                collectionName = op.collectionName,
                                documentId = op.documentId,
                                summaryLabel = op.summaryLabel,
                                localPayloadJson = op.payloadJson,
                                serverUpdatedAtMillis = serverUpdatedMillis
                            )
                        )
                        conflicts++
                    } else {
                        val payloadMap = jsonToFirestoreMap(op.payloadJson, isCreate = !serverSnap.exists())
                        if (serverSnap.exists()) {
                            docRef.update(payloadMap).await()
                        } else {
                            docRef.set(payloadMap).await()
                        }
                        dao.updatePendingOpStatus(op.localId, "SYNCED")
                        synced++
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, docRef.path)
                    failed++
                }
            }
            Result.success(SyncRunReport(synced, conflicts, failed))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveConflictKeepServer(conflict: SyncConflictEntity) {
        dao.updateConflictStatus(conflict.conflictId, "RESOLVED_SERVER")
        dao.deletePendingOp(conflict.pendingOpId)
    }

    suspend fun resolveConflictForceLocal(conflict: SyncConflictEntity): Result<Unit> {
        val docRef = db.collection("schools")
            .document(conflict.schoolId)
            .collection(conflict.collectionName)
            .document(conflict.documentId)
        return try {
            val snap = docRef.get().await()
            val payload = jsonToFirestoreMap(conflict.localPayloadJson, isCreate = !snap.exists())
            if (snap.exists()) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            dao.updateConflictStatus(conflict.conflictId, "RESOLVED_LOCAL")
            dao.deletePendingOp(conflict.pendingOpId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun detectTimestampConflict(baseMillis: Long, serverMillis: Long): Boolean {
            return baseMillis > 0L && serverMillis > baseMillis
        }

        private fun jsonToFirestoreMap(rawJson: String, isCreate: Boolean): Map<String, Any> {
            val obj = JSONObject(rawJson)
            val result = mutableMapOf<String, Any>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = obj.get(key)
                if (value is org.json.JSONArray) {
                    val list = mutableListOf<String>()
                    for (i in 0 until value.length()) {
                        list.add(value.getString(i))
                    }
                    result[key] = list
                } else if (value != JSONObject.NULL) {
                    result[key] = value
                }
            }
            if (isCreate) {
                result["createdAt"] = FieldValue.serverTimestamp()
            } else {
                result.remove("schoolId")
                result.remove("ownerUid")
                result.remove("createdAt")
            }
            result["updatedAt"] = FieldValue.serverTimestamp()
            return result
        }
    }
}
