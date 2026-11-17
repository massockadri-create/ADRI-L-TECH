package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.model.InterventionLog
import com.example.model.PlatformAdmin
import com.example.model.PlatformConfig
import com.example.model.School
import com.example.model.Subscription
import com.example.model.SupportTicket
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PlatformRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observePlatformConfig(): Flow<PlatformConfig> = flow {
        requireUserId()
        val path = "platform_config/global"
        emitAll(
            db.collection("platform_config").document("global")
                .snapshots()
                .map { snap ->
                    snap.toObject(PlatformConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: PlatformConfig()
                }
                .catch { e -> handleStreamError(e, OperationType.GET, path) }
        )
    }

    fun observeAdmins(): Flow<List<PlatformAdmin>> = flow {
        requireUserId()
        val path = "admins"
        emitAll(
            db.collection("admins")
                .snapshots()
                .map { it.toObjects(PlatformAdmin::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e -> handleStreamError(e, OperationType.LIST, path) }
        )
    }

    fun observeSchools(isSuperAdmin: Boolean): Flow<List<School>> = flow {
        val uid = requireUserId()
        val path = "schools"
        val query = if (isSuperAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereArrayContains("memberUids", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(School::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e -> handleStreamError(e, OperationType.LIST, path) }
        )
    }

    fun observeSchool(schoolId: String): Flow<School?> = flow {
        requireUserId()
        val path = "schools/$schoolId"
        emitAll(
            db.collection("schools").document(schoolId)
                .snapshots()
                .map { it.toObject(School::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e -> handleStreamError(e, OperationType.GET, path) }
        )
    }

    fun observeSubscriptions(isSuperAdmin: Boolean): Flow<List<Subscription>> = flow {
        val uid = requireUserId()
        val path = "subscriptions"
        val query = if (isSuperAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("ownerUid", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(Subscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e -> handleStreamError(e, OperationType.LIST, path) }
        )
    }

    fun observeTickets(isSuperAdmin: Boolean): Flow<List<SupportTicket>> = flow {
        val uid = requireUserId()
        val path = "support_tickets"
        val query = if (isSuperAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("requesterUid", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e -> handleStreamError(e, OperationType.LIST, path) }
        )
    }

    fun observeInterventions(isSuperAdmin: Boolean): Flow<List<InterventionLog>> = flow {
        val uid = requireUserId()
        val path = "intervention_logs"
        val query = if (isSuperAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("authorizedByOwnerUid", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(InterventionLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { e -> handleStreamError(e, OperationType.LIST, path) }
        )
    }

    suspend fun createSchool(school: School): Result<String> {
        var path = "schools"
        return try {
            val uid = requireUserId()
            val ownerUid = school.ownerUid.ifBlank { uid }
            val members = (school.memberUids + ownerUid).distinct().take(50)
            val docRef = db.collection("schools").document()
            path = docRef.path
            val payload = mutableMapOf<String, Any>(
                "ownerUid" to ownerUid,
                "memberUids" to members,
                "name" to school.name.trim(),
                "code" to school.code.trim().uppercase(),
                "schoolType" to school.schoolType.trim().ifBlank { "Établissement" },
                "city" to school.city.trim(),
                "country" to school.country.trim().ifBlank { "Cameroun" },
                "subscriptionStatus" to school.subscriptionStatus.ifBlank { "TRIAL" },
                "subscriptionEndMillis" to school.subscriptionEndMillis,
                "monthlyFeeFcfa" to school.monthlyFeeFcfa,
                "supportAccessEnabled" to school.supportAccessEnabled,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (school.motto.isNotBlank()) payload["motto"] = school.motto.trim()
            if (school.address.isNotBlank()) payload["address"] = school.address.trim()
            if (school.phone.isNotBlank()) payload["phone"] = school.phone.trim()
            if (school.email.isNotBlank()) payload["email"] = school.email.trim()
            if (school.directorName.isNotBlank()) payload["directorName"] = school.directorName.trim()
            if (school.registrationNumber.isNotBlank()) payload["registrationNumber"] = school.registrationNumber.trim()
            if (school.primaryColorHex.isNotBlank()) payload["primaryColorHex"] = school.primaryColorHex
            if (school.secondaryColorHex.isNotBlank()) payload["secondaryColorHex"] = school.secondaryColorHex
            if (school.accentColorHex.isNotBlank()) payload["accentColorHex"] = school.accentColorHex
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSchool(schoolId: String): Result<Unit> {
        val path = "schools/$schoolId"
        return try {
            requireUserId()
            require(schoolId.isNotBlank())
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSchoolBranding(school: School): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            require(school.id.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "name" to school.name.trim(),
                "code" to school.code.trim().uppercase(),
                "schoolType" to school.schoolType.trim(),
                "city" to school.city.trim(),
                "country" to school.country.trim(),
                "motto" to school.motto.trim(),
                "address" to school.address.trim(),
                "phone" to school.phone.trim(),
                "email" to school.email.trim(),
                "directorName" to school.directorName.trim(),
                "registrationNumber" to school.registrationNumber.trim(),
                "primaryColorHex" to school.primaryColorHex,
                "secondaryColorHex" to school.secondaryColorHex,
                "accentColorHex" to school.accentColorHex,
                "supportAccessEnabled" to school.supportAccessEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (school.logoPrimaryBase64.isNotBlank()) updates["logoPrimaryBase64"] = school.logoPrimaryBase64
            if (school.logoSecondaryBase64.isNotBlank()) updates["logoSecondaryBase64"] = school.logoSecondaryBase64
            if (school.stampBase64.isNotBlank()) updates["stampBase64"] = school.stampBase64
            if (school.signatureBase64.isNotBlank()) updates["signatureBase64"] = school.signatureBase64
            db.document(path).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSchoolSubscriptionStatus(
        schoolId: String,
        status: String,
        endMillis: Long,
        monthlyFeeFcfa: Long
    ): Result<Unit> {
        val path = "schools/$schoolId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "subscriptionStatus" to status,
                    "subscriptionEndMillis" to endMillis,
                    "monthlyFeeFcfa" to monthlyFeeFcfa,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createSubscription(sub: Subscription): Result<Unit> {
        var path = "subscriptions"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("subscriptions").document()
            path = docRef.path
            val payload = mutableMapOf<String, Any>(
                "schoolId" to sub.schoolId,
                "schoolName" to sub.schoolName,
                "ownerUid" to sub.ownerUid.ifBlank { uid },
                "planName" to sub.planName,
                "monthlyFeeFcfa" to sub.monthlyFeeFcfa,
                "monthsCount" to sub.monthsCount,
                "totalAmountFcfa" to sub.totalAmountFcfa,
                "paymentMethod" to sub.paymentMethod,
                "referenceCode" to sub.referenceCode,
                "status" to sub.status,
                "startDateMillis" to sub.startDateMillis,
                "endDateMillis" to sub.endDateMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (sub.proofNote.isNotBlank()) payload["proofNote"] = sub.proofNote
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun approveSubscription(sub: Subscription): Result<Unit> {
        val path = "subscriptions/${sub.id}"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            updateSchoolSubscriptionStatus(
                schoolId = sub.schoolId,
                status = "ACTIVE",
                endMillis = sub.endDateMillis,
                monthlyFeeFcfa = sub.monthlyFeeFcfa
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createSupportTicket(ticket: SupportTicket): Result<Unit> {
        var path = "support_tickets"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("support_tickets").document()
            path = docRef.path
            val payload = mutableMapOf<String, Any>(
                "schoolId" to ticket.schoolId,
                "schoolName" to ticket.schoolName,
                "requesterUid" to uid,
                "requesterName" to ticket.requesterName.ifBlank { auth.currentUser?.displayName ?: "Utilisateur" },
                "subject" to ticket.subject.trim(),
                "description" to ticket.description.trim(),
                "priority" to ticket.priority,
                "status" to "OPEN",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSupportTicketStatus(
        ticketId: String,
        status: String,
        resolutionNote: String
    ): Result<Unit> {
        val path = "support_tickets/$ticketId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "status" to status,
                    "resolutionNote" to resolutionNote.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun logIntervention(
        school: School,
        reason: String,
        actionSummary: String
    ): Result<Unit> {
        var path = "intervention_logs"
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email ?: "admin@adrieltech.cm"
            val docRef = db.collection("intervention_logs").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "schoolId" to school.id,
                    "schoolName" to school.name,
                    "adminUid" to uid,
                    "adminEmail" to email,
                    "reason" to reason.trim(),
                    "actionSummary" to actionSummary.trim(),
                    "authorizedByOwnerUid" to school.ownerUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun savePlatformConfig(config: PlatformConfig): Result<Unit> {
        val path = "platform_config/global"
        return try {
            requireUserId()
            val docRef = db.document(path)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "appVersion" to config.appVersion.trim(),
                        "minSupportedVersion" to config.minSupportedVersion.trim(),
                        "maintenanceMode" to config.maintenanceMode,
                        "standardMonthlyFeeFcfa" to config.standardMonthlyFeeFcfa,
                        "supportEmail" to config.supportEmail.trim(),
                        "supportPhone" to config.supportPhone.trim(),
                        "releaseNotes" to config.releaseNotes.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "appVersion" to config.appVersion.trim(),
                        "minSupportedVersion" to config.minSupportedVersion.trim(),
                        "maintenanceMode" to config.maintenanceMode,
                        "standardMonthlyFeeFcfa" to config.standardMonthlyFeeFcfa,
                        "supportEmail" to config.supportEmail.trim(),
                        "supportPhone" to config.supportPhone.trim(),
                        "releaseNotes" to config.releaseNotes.trim(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun savePlatformAdmin(admin: PlatformAdmin): Result<Unit> {
        val path = "admins/${admin.uid}"
        return try {
            requireUserId()
            val docRef = db.document(path)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "email" to admin.email.trim(),
                        "displayName" to admin.displayName.trim(),
                        "role" to admin.role,
                        "active" to admin.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "uid" to admin.uid.trim(),
                        "email" to admin.email.trim(),
                        "displayName" to admin.displayName.trim(),
                        "role" to admin.role,
                        "active" to admin.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun handleStreamError(error: Throwable, op: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
