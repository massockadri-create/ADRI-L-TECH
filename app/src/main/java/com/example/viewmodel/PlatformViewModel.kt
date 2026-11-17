package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.AuthManager
import com.example.data.remote.PlatformRepository
import com.example.domain.FinanceCalculator
import com.example.domain.MatriculeGenerator
import com.example.model.InterventionLog
import com.example.model.PlatformAdmin
import com.example.model.PlatformConfig
import com.example.model.School
import com.example.model.Subscription
import com.example.model.SupportTicket
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlatformViewModel(
    private val repository: PlatformRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String
) : ViewModel() {

    val isSuperAdmin: Boolean =
        currentUserEmail.equals(AuthManager.SUPER_ADMIN_EMAIL, ignoreCase = true)

    val platformConfig: StateFlow<UiState<PlatformConfig>> = repository.observePlatformConfig()
        .asUiState("PlatformConfig")

    val schoolsState: StateFlow<UiState<List<School>>> = repository.observeSchools(isSuperAdmin)
        .asUiState("Schools")

    val subscriptionsState: StateFlow<UiState<List<Subscription>>> = repository.observeSubscriptions(isSuperAdmin)
        .asUiState("Subscriptions")

    val ticketsState: StateFlow<UiState<List<SupportTicket>>> = repository.observeTickets(isSuperAdmin)
        .asUiState("SupportTickets")

    val interventionsState: StateFlow<UiState<List<InterventionLog>>> = repository.observeInterventions(isSuperAdmin)
        .asUiState("Interventions")

    val adminsState: StateFlow<UiState<List<PlatformAdmin>>> = if (isSuperAdmin) {
        repository.observeAdmins().asUiState("Admins")
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun createSchool(
        name: String,
        code: String,
        schoolType: String,
        city: String,
        phone: String,
        email: String,
        directorName: String,
        motto: String,
        onCreated: (String) -> Unit = {}
    ) {
        if (name.trim().length < 2) {
            _actionMessage.value = "Le nom de l'établissement est requis (min 2 caractères)."
            return
        }
        val cleanCode = code.trim().ifBlank { MatriculeGenerator.generateSchoolCode(name) }
        val trialEnd = FinanceCalculator.computeSubscriptionEndMillis(System.currentTimeMillis(), 1L)
        val school = School(
            ownerUid = currentUserId,
            memberUids = listOf(currentUserId),
            name = name.trim(),
            code = cleanCode,
            schoolType = schoolType.ifBlank { "Lycée / Collège" },
            city = city.trim(),
            country = "Cameroun",
            subscriptionStatus = if (isSuperAdmin) "ACTIVE" else "TRIAL",
            subscriptionEndMillis = trialEnd,
            monthlyFeeFcfa = FinanceCalculator.STANDARD_MONTHLY_SUBSCRIPTION_FCFA,
            supportAccessEnabled = false,
            motto = motto.trim(),
            phone = phone.trim(),
            email = email.trim(),
            directorName = directorName.trim().ifBlank { currentUserName }
        )
        viewModelScope.launch {
            repository.createSchool(school)
                .onSuccess { id ->
                    _actionMessage.value = "Établissement créé : ${school.name} ($cleanCode)"
                    onCreated(id)
                }
                .onFailure { e ->
                    _actionMessage.value = e.localizedMessage ?: "Erreur création établissement"
                }
        }
    }

    fun deleteSchool(school: School) {
        viewModelScope.launch {
            repository.deleteSchool(school.id)
                .onSuccess {
                    _actionMessage.value = "Établissement supprimé : ${school.name}"
                }
                .onFailure { e ->
                    _actionMessage.value = e.localizedMessage ?: "Erreur suppression établissement"
                }
        }
    }

    fun updateSubscriptionStatus(school: School, status: String, monthsToExtend: Long = 1L) {
        val now = System.currentTimeMillis()
        val base = maxOf(now, school.subscriptionEndMillis)
        val newEnd = if (status == "ACTIVE") {
            FinanceCalculator.computeSubscriptionEndMillis(base, monthsToExtend)
        } else {
            school.subscriptionEndMillis
        }
        viewModelScope.launch {
            repository.updateSchoolSubscriptionStatus(
                schoolId = school.id,
                status = status,
                endMillis = newEnd,
                monthlyFeeFcfa = school.monthlyFeeFcfa
            ).onSuccess {
                _actionMessage.value = "Statut mis à jour : $status"
            }.onFailure { e ->
                _actionMessage.value = e.localizedMessage ?: "Erreur mise à jour"
            }
        }
    }

    fun submitSubscriptionPayment(
        school: School,
        monthsCount: Long,
        paymentMethod: String,
        referenceCode: String,
        proofNote: String
    ) {
        val now = System.currentTimeMillis()
        val total = FinanceCalculator.computeSubscriptionTotal(monthsCount, school.monthlyFeeFcfa)
        val end = FinanceCalculator.computeSubscriptionEndMillis(maxOf(now, school.subscriptionEndMillis), monthsCount)
        val sub = Subscription(
            schoolId = school.id,
            schoolName = school.name,
            ownerUid = school.ownerUid,
            planName = "ADRIÈL SCHOOL SaaS",
            monthlyFeeFcfa = school.monthlyFeeFcfa,
            monthsCount = monthsCount,
            totalAmountFcfa = total,
            paymentMethod = paymentMethod,
            referenceCode = referenceCode.trim(),
            proofNote = proofNote.trim(),
            status = if (isSuperAdmin) "ACTIVE" else "PENDING",
            startDateMillis = now,
            endDateMillis = end
        )
        viewModelScope.launch {
            repository.createSubscription(sub)
                .onSuccess {
                    if (isSuperAdmin) {
                        repository.updateSchoolSubscriptionStatus(school.id, "ACTIVE", end, school.monthlyFeeFcfa)
                    }
                    _actionMessage.value = "Abonnement enregistré (${FinanceCalculator.formatFcfa(total)})"
                }
                .onFailure { e -> _actionMessage.value = e.localizedMessage ?: "Erreur abonnement" }
        }
    }

    fun approveSubscription(sub: Subscription) {
        viewModelScope.launch {
            repository.approveSubscription(sub)
                .onSuccess { _actionMessage.value = "Abonnement validé pour ${sub.schoolName}" }
                .onFailure { e -> _actionMessage.value = e.localizedMessage ?: "Erreur validation" }
        }
    }

    fun createSupportTicket(school: School, subject: String, description: String, priority: String) {
        if (subject.trim().length < 2 || description.trim().length < 2) {
            _actionMessage.value = "Veuillez renseigner l'objet et la description."
            return
        }
        val ticket = SupportTicket(
            schoolId = school.id,
            schoolName = school.name,
            requesterUid = currentUserId,
            requesterName = currentUserName.ifBlank { currentUserEmail },
            subject = subject,
            description = description,
            priority = priority
        )
        viewModelScope.launch {
            repository.createSupportTicket(ticket)
                .onSuccess { _actionMessage.value = "Ticket support envoyé à ADRIÈL TECH" }
                .onFailure { e -> _actionMessage.value = e.localizedMessage ?: "Erreur envoi ticket" }
        }
    }

    fun updateTicket(ticketId: String, status: String, resolutionNote: String) {
        viewModelScope.launch {
            repository.updateSupportTicketStatus(ticketId, status, resolutionNote)
                .onSuccess { _actionMessage.value = "Ticket mis à jour ($status)" }
                .onFailure { e -> _actionMessage.value = e.localizedMessage ?: "Erreur ticket" }
        }
    }

    fun logSupportIntervention(school: School, reason: String, actionSummary: String, onLogged: () -> Unit) {
        if (reason.trim().length < 2 || actionSummary.trim().length < 2) {
            _actionMessage.value = "Motif et action obligatoires pour le journal d'intervention."
            return
        }
        viewModelScope.launch {
            repository.logIntervention(school, reason, actionSummary)
                .onSuccess {
                    _actionMessage.value = "Intervention journalisée"
                    onLogged()
                }
                .onFailure { e -> _actionMessage.value = e.localizedMessage ?: "Erreur journalisation" }
        }
    }

    fun savePlatformConfig(config: PlatformConfig) {
        viewModelScope.launch {
            repository.savePlatformConfig(config)
                .onSuccess { _actionMessage.value = "Configuration ADRIÈL TECH enregistrée" }
                .onFailure { e -> _actionMessage.value = e.localizedMessage ?: "Erreur config" }
        }
    }

    fun saveAdmin(uid: String, email: String, displayName: String, role: String) {
        if (uid.isBlank() || email.isBlank() || displayName.isBlank()) {
            _actionMessage.value = "UID, email et nom requis."
            return
        }
        viewModelScope.launch {
            repository.savePlatformAdmin(
                PlatformAdmin(uid = uid, email = email, displayName = displayName, role = role, active = true)
            )
                .onSuccess { _actionMessage.value = "Compte administrateur enregistré" }
                .onFailure { e -> _actionMessage.value = e.localizedMessage ?: "Erreur admin" }
        }
    }

    private fun <T> Flow<T>.asUiState(tag: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("PlatformVM-$tag", "Stream error", rootError)
                emit(UiState.Error(rootError.message ?: "Erreur de chargement"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
}
