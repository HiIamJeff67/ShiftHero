package com.example.shifthero.feature

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shifthero.core.database.AppDatabaseHelper
import com.example.shifthero.core.model.AppState
import com.example.shifthero.core.model.AppThemeMode
import com.example.shifthero.core.model.AvailabilitySlot
import com.example.shifthero.core.model.CompanyJoinRequest
import com.example.shifthero.core.model.CompanyJoinRequestStatus
import com.example.shifthero.core.model.CompanySummary
import com.example.shifthero.core.model.Employee
import com.example.shifthero.core.model.EmployeeRole
import com.example.shifthero.core.model.NavigationItem
import com.example.shifthero.core.model.ScheduleSettings
import com.example.shifthero.core.model.ShiftAssignment
import com.example.shifthero.core.model.ShiftBlock
import com.example.shifthero.core.model.ShiftRequirement
import com.example.shifthero.core.model.SwapRequest
import com.example.shifthero.core.model.SwapStatus
import com.example.shifthero.core.model.timeRangeLabel
import com.example.shifthero.core.network.ApiAvailabilitySlot
import com.example.shifthero.core.network.ApiAuthResult
import com.example.shifthero.core.network.ApiCompanyMember
import com.example.shifthero.core.network.ApiCompanyJoinRequest
import com.example.shifthero.core.network.ApiEmployeeRole
import com.example.shifthero.core.network.ApiMe
import com.example.shifthero.core.network.ApiScheduleInsight
import com.example.shifthero.core.network.ApiScheduleInsightStreamEvent
import com.example.shifthero.core.network.ApiScheduleSettings
import com.example.shifthero.core.network.ApiSessionTokens
import com.example.shifthero.core.network.ApiShiftRequirement
import com.example.shifthero.core.network.BackendApiClient
import com.example.shifthero.core.repository.SqliteSettingsRepository
import com.example.shifthero.core.repository.SqliteUserSessionRepository
import com.example.shifthero.core.repository.StoreSettings
import com.example.shifthero.core.repository.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UserProfileUiState(
    val publicId: String = "",
    val name: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "",
    val plan: String = "",
    val status: String = "",
)

data class ScheduleInsightUiState(
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val stage: String = "",
    val streamedSummary: String = "",
    val insight: ApiScheduleInsight? = null,
    val errorMessage: String? = null,
)

data class SchedulePlannerUiState(
    val screen: NavigationItem = NavigationItem.Dashboard,
    val appState: AppState,
    val apiBaseUrl: String,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val isDataHydrated: Boolean = false,
    val userProfile: UserProfileUiState? = null,
    val isUserProfileLoading: Boolean = false,
    val userProfileErrorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val companyRequestMessage: String? = null,
    val scheduleInsight: ScheduleInsightUiState = ScheduleInsightUiState(),
    val showTutorialOverlay: Boolean = false,
    val tutorialStepIndex: Int = 0,
    val latestSuggestionMessages: List<String> = emptyList(),
)

class SchedulePlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabaseHelper(application).writableDatabase
    private val settingsRepository = SqliteSettingsRepository(database)
    private val userSessionRepository = SqliteUserSessionRepository(database)

    private var currentCompanyId: String = ""

    var uiState by mutableStateOf(
        settingsRepository.getSettings().let { settings ->
            SchedulePlannerUiState(
                appState = emptyAppState(settings),
                apiBaseUrl = settings.apiBaseUrl,
                themeMode = settings.themeMode,
                showTutorialOverlay = true,
            )
        }
    )
        private set

    init {
        refreshState()
    }

    fun selectScreen(screen: NavigationItem) {
        uiState = uiState.copy(screen = screen)
        if (uiState.isAuthenticated && !uiState.isDataHydrated && screen != NavigationItem.Setting) {
            launchApi {
                refreshStateInternal(lightweight = false)
            }
        }
        if (uiState.isAuthenticated && screen == NavigationItem.User) {
            loadUserProfile(forceRefresh = uiState.userProfile == null)
        }
    }

    fun login(account: String, password: String) {
        val normalizedAccount = account.trim()
        if (normalizedAccount.isBlank() || password.isBlank()) {
            uiState = uiState.copy(authErrorMessage = "請輸入帳號與密碼")
            return
        }
        launchAuth {
            val settings = settingsRepository.getSettings()
            val authResult = backendClient(settings).login(normalizedAccount, password)
            saveSessionFromAuthResult(authResult)
            refreshStateInternal(clearAuthError = true, lightweight = true)
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: EmployeeRole,
        companyName: String,
    ) {
        val normalizedName = name.trim()
        val normalizedEmail = email.trim()
        val normalizedCompanyName = companyName.trim()
        if (normalizedName.length < 6 || normalizedEmail.isBlank() || password.length < 8) {
            uiState = uiState.copy(authErrorMessage = "註冊資料格式不正確")
            return
        }
        if (role == EmployeeRole.Manager && normalizedCompanyName.isBlank()) {
            uiState = uiState.copy(authErrorMessage = "Manager 註冊時必須填寫公司名稱")
            return
        }
        launchAuth {
            val settings = settingsRepository.getSettings()
            val client = backendClient(settings)
            val authResult = client.register(normalizedName, normalizedEmail, password)
            saveSessionFromAuthResult(authResult)
            if (role == EmployeeRole.Manager) {
                val tokenPair = ApiSessionTokens(authResult.user.accessToken, authResult.refreshToken.orEmpty())
                val (_, refreshedToken) = client.createCompany(
                    name = normalizedCompanyName,
                    email = normalizedEmail,
                    description = "Created via Android client",
                    tokens = tokenPair,
                )
                persistRefreshedAccessToken(refreshedToken)
            }
            refreshStateInternal(clearAuthError = true, lightweight = true)
        }
    }

    fun loginViaGoogle(authorizationCode: String) {
        val code = authorizationCode.trim()
        if (code.isBlank()) {
            uiState = uiState.copy(authErrorMessage = "請輸入 Google authorization code")
            return
        }
        launchAuth {
            val settings = settingsRepository.getSettings()
            val authResult = backendClient(settings).loginViaGoogle(code)
            saveSessionFromAuthResult(authResult)
            refreshStateInternal(clearAuthError = true, lightweight = true)
        }
    }

    fun registerViaGoogle(authorizationCode: String) {
        val code = authorizationCode.trim()
        if (code.isBlank()) {
            uiState = uiState.copy(authErrorMessage = "請輸入 Google authorization code")
            return
        }
        launchAuth {
            val settings = settingsRepository.getSettings()
            val authResult = backendClient(settings).registerViaGoogle(code)
            saveSessionFromAuthResult(authResult)
            refreshStateInternal(clearAuthError = true, lightweight = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            val session = userSessionRepository.getSession()
            if (session != null) {
                val settings = settingsRepository.getSettings()
                val tokens = ApiSessionTokens(session.accessToken, session.refreshToken)
                runCatching { backendClient(settings).logout(tokens) }
            }
            clearLocalSession()
        }
    }

    fun loadUserProfile(forceRefresh: Boolean = false) {
        if (!forceRefresh && uiState.userProfile != null) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            uiState = uiState.copy(isUserProfileLoading = true, userProfileErrorMessage = null)
            try {
                val (me, refreshed) = backendClient(settings).getMe(tokens)
                persistRefreshedAccessToken(refreshed)
                val session = userSessionRepository.getSession()
                if (session != null) {
                    userSessionRepository.upsertSession(
                        session.copy(
                            name = me.name.ifBlank { session.name },
                            displayName = me.displayName.ifBlank { session.displayName },
                            email = me.email.ifBlank { session.email },
                        ),
                    )
                }
                uiState = uiState.copy(
                    isUserProfileLoading = false,
                    userProfile = me.toUserProfileUiState(),
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isUserProfileLoading = false,
                    userProfileErrorMessage = e.message ?: "無法取得使用者資料",
                )
            }
        }
    }

    fun updateMyDisplayName(displayName: String) {
        val normalized = displayName.trim()
        if (normalized.length < 6) {
            uiState = uiState.copy(userProfileErrorMessage = "displayName 至少 6 個字元")
            return
        }
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            uiState = uiState.copy(isUserProfileLoading = true, userProfileErrorMessage = null)
            try {
                val refreshed = backendClient(settings).updateMeDisplayName(normalized, tokens)
                persistRefreshedAccessToken(refreshed)
                val session = userSessionRepository.getSession()
                if (session != null) {
                    userSessionRepository.upsertSession(session.copy(displayName = normalized))
                }
                val existing = uiState.userProfile
                uiState = uiState.copy(
                    isUserProfileLoading = false,
                    userProfile = existing?.copy(displayName = normalized),
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isUserProfileLoading = false,
                    userProfileErrorMessage = e.message ?: "更新失敗",
                )
            }
        }
    }

    fun selectCompany(companyId: String) {
        val normalized = companyId.trim()
        if (normalized.isBlank() || normalized == currentCompanyId) return
        settingsRepository.setSelectedCompanyId(normalized)
        currentCompanyId = normalized
        uiState = uiState.copy(scheduleInsight = ScheduleInsightUiState())
        refreshCompanyRequests()
    }

    fun submitJoinCompanyRequest(
        companyId: String,
        companyName: String,
        note: String,
        requestedRole: EmployeeRole,
    ) {
        val normalizedCompanyId = companyId.trim()
        if (normalizedCompanyId.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (request, refreshed) = backendClient(settings).createCompanyJoinRequest(
                companyId = normalizedCompanyId,
                requestedRole = requestedRole.toApiRole(),
                note = note.trim(),
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            val localRequest = request.toLocalJoinRequest()
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    myJoinRequests = upsertById(uiState.appState.myJoinRequests, localRequest) { it.id },
                ),
                companyRequestMessage = "已送出加入公司申請",
            )
        }
    }

    fun approveJoinCompanyRequest(requestId: String) {
        if (currentCompanyId.isBlank() || uiState.appState.viewerRoleInCompany != EmployeeRole.Manager) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (request, refreshed) = backendClient(settings).approveCompanyJoinRequest(
                companyId = currentCompanyId,
                joinRequestId = requestId,
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            val localRequest = request.toLocalJoinRequest()
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    companyJoinRequests = uiState.appState.companyJoinRequests.filterNot { it.id == requestId },
                    myJoinRequests = upsertById(uiState.appState.myJoinRequests, localRequest) { it.id },
                ),
                companyRequestMessage = "已核准加入申請",
            )
        }
    }

    fun rejectJoinCompanyRequest(requestId: String) {
        if (currentCompanyId.isBlank() || uiState.appState.viewerRoleInCompany != EmployeeRole.Manager) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (request, refreshed) = backendClient(settings).rejectCompanyJoinRequest(
                companyId = currentCompanyId,
                joinRequestId = requestId,
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            val localRequest = request.toLocalJoinRequest()
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    companyJoinRequests = uiState.appState.companyJoinRequests.filterNot { it.id == requestId },
                    myJoinRequests = upsertById(uiState.appState.myJoinRequests, localRequest) { it.id },
                ),
                companyRequestMessage = "已婉拒加入申請",
            )
        }
    }

    fun cancelJoinCompanyRequest(companyId: String, requestId: String) {
        if (companyId.isBlank() || requestId.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (request, refreshed) = backendClient(settings).cancelCompanyJoinRequest(
                companyId = companyId,
                joinRequestId = requestId,
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            val localRequest = request.toLocalJoinRequest()
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    myJoinRequests = upsertById(uiState.appState.myJoinRequests, localRequest) { it.id },
                ),
                companyRequestMessage = "已撤銷加入申請",
            )
        }
    }

    fun refreshCompanyRequests() {
        if (!uiState.isAuthenticated) return
        launchApi {
            refreshStateInternal(lightweight = true)
        }
    }

    fun leaveCurrentCompany() {
        if (currentCompanyId.isBlank()) return
        val leavingCompanyId = currentCompanyId
        launchApi {
            val session = userSessionRepository.getSession() ?: return@launchApi
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val refreshed = backendClient(settings).removeCompanyMember(
                companyId = leavingCompanyId,
                userId = session.userId,
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            settingsRepository.setSelectedCompanyId("")
            currentCompanyId = ""
            val remainingCompanies = uiState.appState.availableCompanies.filterNot { it.id == leavingCompanyId }
            uiState = uiState.copy(
                appState = emptyAppState(settings, session).copy(availableCompanies = remainingCompanies),
            )
        }
    }

    fun updateCurrentCompanyProfile(name: String, email: String, description: String) {
        if (currentCompanyId.isBlank() || uiState.appState.viewerRoleInCompany != EmployeeRole.Manager) return
        val normalizedName = name.trim()
        val normalizedEmail = email.trim()
        val normalizedDescription = description.trim()
        if (normalizedName.isBlank() || normalizedEmail.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (company, refreshed) = backendClient(settings).updateCompany(
                companyId = currentCompanyId,
                name = normalizedName,
                email = normalizedEmail,
                description = normalizedDescription,
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    storeName = company.name,
                    currentCompanyEmail = company.email,
                    currentCompanyDescription = company.description,
                    availableCompanies = upsertById(
                        uiState.appState.availableCompanies,
                        CompanySummary(company.id, company.name),
                    ) { it.id },
                ),
            )
        }
    }

    fun addShiftRequirement(day: String, block: ShiftBlock, role: String, requiredCount: Int, isHoliday: Boolean) {
        if (currentCompanyId.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (shift, refreshed) = backendClient(settings).createShiftRequirement(
                companyId = currentCompanyId,
                employeeRole = role.toApiRole(),
                startAt = toIsoDateTime(day, block.defaultStartMinute),
                endAt = toIsoDateTime(day, block.defaultEndMinute),
                requiredCount = requiredCount.coerceAtLeast(1),
                note = if (isHoliday) "holiday" else "",
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            upsertLocalShiftRequirement(shift)
        }
    }

    fun addMinuteShiftRequirement(day: String, startMinute: Int, endMinute: Int, employeeRole: EmployeeRole, requiredCount: Int) {
        if (currentCompanyId.isBlank() || endMinute <= startMinute) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (shift, refreshed) = backendClient(settings).createShiftRequirement(
                companyId = currentCompanyId,
                employeeRole = employeeRole.toApiRole(),
                startAt = toIsoDateTime(day, startMinute),
                endAt = toIsoDateTime(day, endMinute),
                requiredCount = requiredCount.coerceAtLeast(1),
                note = "",
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            upsertLocalShiftRequirement(shift)
        }
    }

    fun updateTimelineShiftTime(shiftId: String, startMinute: Int, endMinute: Int) {
        if (currentCompanyId.isBlank()) return
        val existingShift = uiState.appState.shiftRequirements.firstOrNull { it.id == shiftId } ?: return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val refreshed = backendClient(settings).updateShiftRequirement(
                companyId = currentCompanyId,
                shiftRequirementId = shiftId,
                employeeRole = existingShift.role.toApiRole(),
                startAt = toIsoDateTime(existingShift.day, startMinute),
                endAt = toIsoDateTime(existingShift.day, endMinute),
                requiredCount = existingShift.requiredCount,
                note = if (existingShift.isHoliday) "holiday" else "",
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            val updatedShift = existingShift.copy(
                startMinute = startMinute,
                endMinute = endMinute,
                block = ShiftBlock.fromMinuteRange(startMinute, endMinute),
            )
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    shiftRequirements = replaceById(uiState.appState.shiftRequirements, updatedShift.id, updatedShift) { it.id },
                ),
            )
        }
    }

    fun toggleAvailability(day: String, block: ShiftBlock) {
        val viewerUserId = uiState.appState.viewerUserId
        if (viewerUserId.isBlank() || currentCompanyId.isBlank()) return

        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val client = backendClient(settings)
            val currentSlots = uiState.appState.availabilitySlots
                .filter { it.employeeId == viewerUserId }
                .toMutableList()
            val existingIndex = currentSlots.indexOfFirst { it.day == day && it.block == block }
            if (existingIndex >= 0) {
                val existing = currentSlots.removeAt(existingIndex)
                if (existing.id.isNotBlank()) {
                    val refreshed = client.deleteAvailabilitySlot(currentCompanyId, existing.id, tokens)
                    persistRefreshedAccessToken(refreshed)
                    removeLocalAvailabilitySlot(existing.id)
                    return@launchApi
                }
            } else {
                currentSlots += AvailabilitySlot(employeeId = viewerUserId, day = day, block = block)
            }
            val isoSlots = currentSlots.map { slot ->
                toIsoDateTime(slot.day, slot.block.defaultStartMinute) to toIsoDateTime(slot.day, slot.block.defaultEndMinute)
            }
            val (slots, refreshed) = client.upsertAvailabilitySlots(currentCompanyId, isoSlots, tokens)
            persistRefreshedAccessToken(refreshed)
            val returnedSlots = slots.map { it.toLocalAvailabilitySlot() }
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    availabilitySlots = uiState.appState.availabilitySlots
                        .filterNot { it.employeeId == viewerUserId }
                        .plus(returnedSlots),
                ),
            )
        }
    }

    fun addAvailabilitySlot(day: String, startMinute: Int, endMinute: Int) {
        val viewerUserId = uiState.appState.viewerUserId
        if (viewerUserId.isBlank() || currentCompanyId.isBlank() || endMinute <= startMinute) return
        val optimisticSlotId = "local-availability-${System.currentTimeMillis()}"
        val optimisticSlot = AvailabilitySlot(
            id = optimisticSlotId,
            employeeId = viewerUserId,
            day = day,
            block = ShiftBlock.fromMinuteRange(startMinute, endMinute),
            startMinute = startMinute,
            endMinute = endMinute,
        )
        upsertLocalAvailabilitySlot(optimisticSlot)

        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (slots, refreshed) = backendClient(settings).upsertAvailabilitySlots(
                companyId = currentCompanyId,
                slots = listOf(toIsoDateTime(day, startMinute) to toIsoDateTime(day, endMinute)),
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            val returnedSlot = slots.firstOrNull()
            if (returnedSlot != null) {
                replaceLocalAvailabilitySlot(optimisticSlotId, returnedSlot.toLocalAvailabilitySlot(fallback = optimisticSlot))
            }
        }
    }

    fun deleteAvailabilitySlot(slot: AvailabilitySlot) {
        if (currentCompanyId.isBlank() || slot.id.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val refreshed = backendClient(settings).deleteAvailabilitySlot(currentCompanyId, slot.id, tokens)
            persistRefreshedAccessToken(refreshed)
            removeLocalAvailabilitySlot(slot.id)
        }
    }

    fun generateSchedule() {
        if (currentCompanyId.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (startAt, endAt) = currentWeekRangeIso()
            val (assignments, refreshed) = backendClient(settings).generateAssignments(currentCompanyId, startAt = startAt, endAt = endAt, tokens = tokens)
            persistRefreshedAccessToken(refreshed)
            uiState = uiState.copy(
                appState = uiState.appState.copy(
                    assignments = assignments.map { assignment ->
                        ShiftAssignment(
                            id = assignment.id,
                            shiftId = assignment.shiftRequirementId,
                            employeeId = assignment.userId,
                            reason = "後端分配",
                        )
                    },
                ),
                latestSuggestionMessages = listOf("已使用後端 API 產生推薦班表"),
            )
        }
    }

    fun generateScheduleInsights(
        startDate: String,
        endDate: String,
        locale: String,
        focus: String,
    ) {
        val range = validateScheduleInsightRange(startDate, endDate) ?: return
        if (!canGenerateScheduleInsights()) return
        viewModelScope.launch {
            uiState = uiState.copy(
                scheduleInsight = ScheduleInsightUiState(
                    isLoading = true,
                    stage = "正在準備班表分析",
                ),
            )
            try {
                val (tokens, settings) = requireValidSessionAndSettings() ?: return@launch
                val (insight, refreshed) = backendClient(settings).generateScheduleInsights(
                    companyId = currentCompanyId,
                    startAt = range.first,
                    endAt = range.second,
                    locale = locale,
                    focus = focus.trim(),
                    tokens = tokens,
                )
                persistRefreshedAccessToken(refreshed)
                uiState = uiState.copy(
                    scheduleInsight = ScheduleInsightUiState(
                        insight = insight,
                        streamedSummary = insight.summary,
                    ),
                )
            } catch (e: Exception) {
                handleScheduleInsightError(e)
            }
        }
    }

    fun streamScheduleInsights(
        startDate: String,
        endDate: String,
        locale: String,
        focus: String,
    ) {
        val range = validateScheduleInsightRange(startDate, endDate) ?: return
        if (!canGenerateScheduleInsights()) return
        viewModelScope.launch {
            uiState = uiState.copy(
                scheduleInsight = ScheduleInsightUiState(
                    isLoading = true,
                    isStreaming = true,
                    stage = "正在連線至 AI 分析流程",
                ),
            )
            try {
                val (tokens, settings) = requireValidSessionAndSettings() ?: return@launch
                backendClient(settings).streamScheduleInsights(
                    companyId = currentCompanyId,
                    startAt = range.first,
                    endAt = range.second,
                    locale = locale,
                    focus = focus.trim(),
                    tokens = tokens,
                ) { event ->
                    withContext(Dispatchers.Main.immediate) {
                        when (event) {
                            is ApiScheduleInsightStreamEvent.Stage -> {
                                uiState = uiState.copy(
                                    scheduleInsight = uiState.scheduleInsight.copy(stage = event.value),
                                )
                            }
                            is ApiScheduleInsightStreamEvent.Token -> {
                                uiState = uiState.copy(
                                    scheduleInsight = uiState.scheduleInsight.copy(
                                        streamedSummary = uiState.scheduleInsight.streamedSummary + event.value,
                                    ),
                                )
                            }
                            is ApiScheduleInsightStreamEvent.Done -> {
                                persistRefreshedAccessToken(event.newAccessToken)
                                uiState = uiState.copy(
                                    scheduleInsight = uiState.scheduleInsight.copy(
                                        isLoading = false,
                                        isStreaming = false,
                                        stage = "分析完成",
                                        insight = event.insight,
                                        streamedSummary = event.insight.summary.ifBlank {
                                            uiState.scheduleInsight.streamedSummary
                                        },
                                    ),
                                )
                            }
                            is ApiScheduleInsightStreamEvent.Error -> {
                                uiState = uiState.copy(
                                    scheduleInsight = uiState.scheduleInsight.copy(
                                        isLoading = false,
                                        isStreaming = false,
                                        errorMessage = event.message,
                                    ),
                                )
                            }
                        }
                    }
                }
                if (uiState.scheduleInsight.isLoading) {
                    uiState = uiState.copy(
                        scheduleInsight = uiState.scheduleInsight.copy(
                            isLoading = false,
                            isStreaming = false,
                            errorMessage = "串流已結束，但後端未回傳完成事件。",
                        ),
                    )
                }
            } catch (e: Exception) {
                handleScheduleInsightError(e)
            }
        }
    }

    private fun canGenerateScheduleInsights(): Boolean {
        val error = when {
            currentCompanyId.isBlank() -> "請先選擇公司。"
            uiState.appState.viewerRoleInCompany != EmployeeRole.Manager -> "只有 Manager 可以產生 AI 班表洞察。"
            else -> null
        }
        if (error != null) {
            uiState = uiState.copy(
                scheduleInsight = uiState.scheduleInsight.copy(errorMessage = error),
            )
            return false
        }
        return true
    }

    private fun validateScheduleInsightRange(startDate: String, endDate: String): Pair<String, String>? {
        val start = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull()
        val end = runCatching { LocalDate.parse(endDate.trim()) }.getOrNull()
        val error = when {
            start == null || end == null -> "日期格式必須為 YYYY-MM-DD。"
            !end.isAfter(start) -> "結束日必須晚於開始日。"
            java.time.temporal.ChronoUnit.DAYS.between(start, end) > 31 -> "分析範圍不可超過 31 天。"
            else -> null
        }
        if (error != null || start == null || end == null) {
            uiState = uiState.copy(
                scheduleInsight = uiState.scheduleInsight.copy(errorMessage = error),
            )
            return null
        }
        val zone = runCatching { ZoneId.of(uiState.appState.scheduleSettings.timezone) }
            .getOrDefault(ZoneId.of(DEFAULT_TIMEZONE))
        return start.atStartOfDay(zone).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) to
            end.atStartOfDay(zone).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun handleScheduleInsightError(error: Exception) {
        val message = error.message ?: "AI 班表洞察產生失敗。"
        if (message.contains("Unauthorized", ignoreCase = true) || message.contains("token", ignoreCase = true)) {
            logout()
            return
        }
        uiState = uiState.copy(
            scheduleInsight = uiState.scheduleInsight.copy(
                isLoading = false,
                isStreaming = false,
                errorMessage = message,
            ),
        )
        Log.e("ShiftHeroInsights", "AI schedule insight error", error)
    }

    fun claimShiftRequirement(shift: ShiftRequirement) {
        val viewerUserId = uiState.appState.viewerUserId
        if (viewerUserId.isBlank() || currentCompanyId.isBlank()) return
        if (uiState.appState.assignments.any { it.shiftId == shift.id && it.employeeId == viewerUserId }) return
        val assignedCount = uiState.appState.assignments.count { it.shiftId == shift.id }
        if (assignedCount >= shift.requiredCount) return

        val optimisticAssignmentId = "local-assignment-${System.currentTimeMillis()}"
        val optimisticAssignment = ShiftAssignment(
            id = optimisticAssignmentId,
            shiftId = shift.id,
            employeeId = viewerUserId,
            reason = "自行接受 Shift",
        )
        upsertLocalAssignment(optimisticAssignment)

        launchApi {
            try {
                val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
                val (assignment, refreshed) = backendClient(settings).claimAssignment(
                    companyId = currentCompanyId,
                    shiftRequirementId = shift.id,
                    tokens = tokens,
                )
                persistRefreshedAccessToken(refreshed)
                replaceLocalAssignment(
                    optimisticAssignmentId,
                    ShiftAssignment(
                        id = assignment.id,
                        shiftId = assignment.shiftRequirementId,
                        employeeId = assignment.userId,
                        reason = "自行接受 Shift",
                    ),
                )
            } catch (e: Exception) {
                removeLocalAssignment(optimisticAssignmentId)
                throw e
            }
        }
    }

    fun createSwapRequest(shift: ShiftRequirement) {
        val viewerUserId = uiState.appState.viewerUserId
        if (viewerUserId.isBlank() || currentCompanyId.isBlank()) return

        val assignmentId = uiState.appState.assignments
            .firstOrNull { it.shiftId == shift.id && it.employeeId == viewerUserId }
            ?.id
            ?.takeIf { it.isNotBlank() }
            ?: return

        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (swap, refreshed) = backendClient(settings).createSwapRequest(
                companyId = currentCompanyId,
                shiftAssignmentId = assignmentId,
                reason = "臨時有事，希望同事幫忙 ${shift.timeRangeLabel()}",
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            upsertLocalSwapRequest(
                SwapRequest(
                    id = swap.id,
                    shiftId = shift.id,
                    requesterEmployeeId = swap.requesterUserId,
                    accepterEmployeeId = swap.claimedByUserId,
                    subsidy = 0,
                    reason = swap.reason,
                    status = swap.status.toSwapStatus(),
                ),
            )
            uiState = uiState.copy(screen = NavigationItem.Management)
        }
    }

    fun claimSwap(request: SwapRequest) {
        if (currentCompanyId.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (swap, refreshed) = backendClient(settings).claimSwapRequest(currentCompanyId, request.id, tokens)
            persistRefreshedAccessToken(refreshed)
            upsertLocalSwapRequest(request.copy(accepterEmployeeId = swap.claimedByUserId, status = swap.status.toSwapStatus()))
            if (swap.status.equals("Approved", ignoreCase = true)) {
                refreshStateInternal(lightweight = false)
            }
        }
    }

    fun approveSwap(request: SwapRequest) {
        if (currentCompanyId.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (swap, refreshed) = backendClient(settings).approveSwapRequest(currentCompanyId, request.id, tokens)
            persistRefreshedAccessToken(refreshed)
            upsertLocalSwapRequest(request.copy(accepterEmployeeId = swap.claimedByUserId, status = swap.status.toSwapStatus()))
        }
    }

    fun cancelSwap(request: SwapRequest) {
        if (currentCompanyId.isBlank()) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (swap, refreshed) = backendClient(settings).cancelSwapRequest(currentCompanyId, request.id, tokens)
            persistRefreshedAccessToken(refreshed)
            upsertLocalSwapRequest(request.copy(accepterEmployeeId = swap.claimedByUserId, status = swap.status.toSwapStatus()))
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
        uiState = uiState.copy(themeMode = mode)
    }

    fun setApiBaseUrl(baseUrl: String) {
        settingsRepository.setApiBaseUrl(baseUrl)
        clearLocalSession()
        val settings = settingsRepository.getSettings()
        uiState = uiState.copy(apiBaseUrl = settings.apiBaseUrl)
    }

    fun updateScheduleSettings(autoApproveSwaps: Boolean, maxWeeklyHours: Int, minRestHours: Int, timezone: String) {
        if (currentCompanyId.isBlank() || uiState.appState.viewerRoleInCompany != EmployeeRole.Manager) return
        launchApi {
            val (tokens, settings) = requireValidSessionAndSettings() ?: return@launchApi
            val (scheduleSettings, refreshed) = backendClient(settings).updateScheduleSettings(
                companyId = currentCompanyId,
                autoApproveSwaps = autoApproveSwaps,
                maxWeeklyHours = maxWeeklyHours.coerceAtLeast(1),
                minRestHours = minRestHours.coerceAtLeast(0),
                timezone = timezone.ifBlank { DEFAULT_TIMEZONE },
                tokens = tokens,
            )
            persistRefreshedAccessToken(refreshed)
            uiState = uiState.copy(
                appState = uiState.appState.copy(scheduleSettings = scheduleSettings.toLocalScheduleSettings()),
                latestSuggestionMessages = listOf("已更新排班設定"),
            )
        }
    }

    fun dismissTutorialOverlay() {
        uiState = uiState.copy(showTutorialOverlay = false)
    }

    fun showPreviousTutorialStep() {
        uiState = uiState.copy(tutorialStepIndex = (uiState.tutorialStepIndex - 1).coerceAtLeast(0))
    }

    fun showNextTutorialStep(maxStepExclusive: Int) {
        if (maxStepExclusive <= 0) return
        val maxIndex = maxStepExclusive - 1
        uiState = uiState.copy(tutorialStepIndex = (uiState.tutorialStepIndex + 1).coerceAtMost(maxIndex))
    }

    fun resetAllData() {
        refreshState()
    }

    private fun refreshState() {
        launchApi {
            refreshStateInternal(lightweight = false)
        }
    }

    private suspend fun refreshStateInternal(
        messages: List<String> = emptyList(),
        clearAuthError: Boolean = false,
        lightweight: Boolean = false,
    ) {
        val settings = settingsRepository.getSettings()
        val session = userSessionRepository.getSession()
        val now = System.currentTimeMillis()
        if (session == null || session.refreshTokenExpiresAtEpochMs <= now) {
            userSessionRepository.clearSession()
            currentCompanyId = ""
            uiState = uiState.copy(
                appState = emptyAppState(settings),
                apiBaseUrl = settings.apiBaseUrl,
                themeMode = settings.themeMode,
                isDataHydrated = false,
                userProfile = null,
                isUserProfileLoading = false,
                userProfileErrorMessage = null,
                isAuthenticated = false,
                isAuthLoading = false,
                authErrorMessage = if (clearAuthError) null else uiState.authErrorMessage,
                companyRequestMessage = null,
                latestSuggestionMessages = messages,
                showTutorialOverlay = true,
            )
            return
        }

        val previousAppState = uiState.appState
        val wasDataHydrated = uiState.isDataHydrated
        val appState = withContext(Dispatchers.IO) { loadAppStateFromApi(settings, session, lightweight, previousAppState) }
        val hasData = hasCoreData(appState)
        uiState = uiState.copy(
            appState = appState,
            themeMode = settings.themeMode,
            apiBaseUrl = settings.apiBaseUrl,
            isDataHydrated = if (lightweight) wasDataHydrated else true,
            isAuthenticated = true,
            isAuthLoading = false,
            authErrorMessage = if (clearAuthError) null else uiState.authErrorMessage,
            latestSuggestionMessages = messages,
            showTutorialOverlay = if (hasData) false else uiState.showTutorialOverlay,
            tutorialStepIndex = if (hasData) 0 else uiState.tutorialStepIndex,
        )
    }

    private suspend fun loadAppStateFromApi(
        settings: StoreSettings,
        session: UserSession,
        lightweight: Boolean,
        previousAppState: AppState,
    ): AppState {
        val client = backendClient(settings)
        var tokens = ApiSessionTokens(session.accessToken, session.refreshToken)

        val (companies, refresh1) = client.getMyCompanies(tokens)
        tokens = effectiveTokens(tokens, refresh1)
        val (myApiJoinRequests, refreshMyJoinRequests) = client.getMyCompanyJoinRequests(tokens)
        tokens = effectiveTokens(tokens, refreshMyJoinRequests)
        val myJoinRequests = myApiJoinRequests.map { it.toLocalJoinRequest() }

        val selectedCompanyId = settings.selectedCompanyId.takeIf { id -> companies.any { it.id == id } }
            ?: companies.firstOrNull()?.id
            ?: ""

        currentCompanyId = selectedCompanyId
        if (selectedCompanyId != settings.selectedCompanyId) settingsRepository.setSelectedCompanyId(selectedCompanyId)

        if (selectedCompanyId.isBlank()) {
            persistRefreshedAccessToken(tokens.accessToken.takeIf { it != session.accessToken })
            return emptyAppState(settings, session).copy(
                availableCompanies = companies.map { CompanySummary(id = it.id, name = it.name) },
                myJoinRequests = myJoinRequests,
            )
        }

        val company = companies.first { it.id == selectedCompanyId }

        val (members, refresh2) = client.getCompanyMembers(selectedCompanyId, tokens)
        tokens = effectiveTokens(tokens, refresh2)

        val apiShifts = mutableListOf<com.example.shifthero.core.network.ApiShiftRequirement>()
        val apiAvailability = mutableListOf<com.example.shifthero.core.network.ApiAvailabilitySlot>()
        val apiAssignments = mutableListOf<com.example.shifthero.core.network.ApiAssignment>()
        val apiSwaps = mutableListOf<com.example.shifthero.core.network.ApiSwapRequest>()
        var apiScheduleSettings: ApiScheduleSettings? = null
        if (!lightweight) {
            val (scheduleSettings, refreshScheduleSettings) = client.getScheduleSettings(selectedCompanyId, tokens)
            tokens = effectiveTokens(tokens, refreshScheduleSettings)
            apiScheduleSettings = scheduleSettings
            val (weekStartIso, nextWeekStartIso) = currentWeekRangeIso()

            val (shifts, refresh3) = client.getShiftRequirements(
                companyId = selectedCompanyId,
                startAt = weekStartIso,
                endAt = nextWeekStartIso,
                tokens = tokens,
            )
            tokens = effectiveTokens(tokens, refresh3)
            apiShifts += shifts

            val (availability, refresh4) = client.getAvailabilitySlots(
                companyId = selectedCompanyId,
                startAt = weekStartIso,
                endAt = nextWeekStartIso,
                tokens = tokens,
            )
            tokens = effectiveTokens(tokens, refresh4)
            apiAvailability += availability

            val (assignments, refresh5) = client.getAssignments(
                companyId = selectedCompanyId,
                startAt = weekStartIso,
                endAt = nextWeekStartIso,
                tokens = tokens,
            )
            tokens = effectiveTokens(tokens, refresh5)
            apiAssignments += assignments

            val (swaps, refresh6) = client.getSwapRequests(selectedCompanyId, tokens)
            tokens = effectiveTokens(tokens, refresh6)
            apiSwaps += swaps
        }

        persistRefreshedAccessToken(tokens.accessToken.takeIf { it != session.accessToken })

        val employees = members
            .map { member ->
                Employee(
                    id = member.userId,
                    name = if (member.displayName.isNotBlank()) member.displayName else member.name,
                    role = member.employeeRole.toLocalRole(),
                    hourlyRate = settings.wageRule.baseHourlyRate,
                    targetHours = 40.0,
                    reliability = 0.9,
                    skillTags = listOf(member.employeeRole.name),
                )
            }
            .sortedBy { it.name }
        val viewerMember = members.firstOrNull { it.matchesSession(session) }
        val viewerRoleInCompany = viewerMember?.employeeRole?.toLocalRole()
        val companyJoinRequests = if (viewerRoleInCompany == EmployeeRole.Manager) {
            val (requests, refreshJoinRequests) = client.getCompanyJoinRequests(selectedCompanyId, tokens)
            tokens = effectiveTokens(tokens, refreshJoinRequests)
            requests.map { it.toLocalJoinRequest() }
        } else {
            emptyList()
        }

        val shiftRequirements = apiShifts.map { shift ->
            val start = parseDateTime(shift.startAt)
            val end = parseDateTime(shift.endAt)
            val day = dayLabel(start.toLocalDate())
            ShiftRequirement(
                id = shift.id,
                day = day,
                block = ShiftBlock.fromMinuteRange(minuteOf(start), minuteOf(end)),
                role = if (shift.employeeRole == ApiEmployeeRole.Manager) EmployeeRole.Manager.label else EmployeeRole.Staff.label,
                requiredCount = shift.requiredCount.coerceAtLeast(1),
                isHoliday = shift.note.contains("holiday", ignoreCase = true),
                startMinute = minuteOf(start),
                endMinute = minuteOf(end),
            )
        }

        val availabilitySlots = apiAvailability
            .filter { it.isAvailable }
            .map { slot ->
                val start = parseDateTime(slot.startAt)
                val end = parseDateTime(slot.endAt)
                AvailabilitySlot(
                    id = slot.id,
                    employeeId = slot.userId,
                    day = dayLabel(start.toLocalDate()),
                    block = ShiftBlock.fromMinuteRange(minuteOf(start), minuteOf(end)),
                    startMinute = minuteOf(start),
                    endMinute = minuteOf(end),
                    note = "",
                )
            }

        val assignments = apiAssignments.map { assignment ->
            ShiftAssignment(
                shiftId = assignment.shiftRequirementId,
                employeeId = assignment.userId,
                reason = "後端分配",
                id = assignment.id,
            )
        }

        val assignmentById = apiAssignments.associateBy { it.id }
        val swaps = apiSwaps.map { swap ->
            SwapRequest(
                id = swap.id,
                shiftId = assignmentById[swap.shiftAssignmentId]?.shiftRequirementId.orEmpty(),
                requesterEmployeeId = swap.requesterUserId,
                accepterEmployeeId = swap.claimedByUserId,
                subsidy = 0,
                reason = swap.reason,
                status = swap.status.toSwapStatus(),
            )
        }

        val canPreserveScheduleState = lightweight && previousAppState.currentCompanyId == selectedCompanyId
        val effectiveShiftRequirements = if (canPreserveScheduleState) previousAppState.shiftRequirements else shiftRequirements
        val effectiveAvailabilitySlots = if (canPreserveScheduleState) previousAppState.availabilitySlots else availabilitySlots
        val effectiveAssignments = if (canPreserveScheduleState) previousAppState.assignments else assignments
        val effectiveSwapRequests = if (canPreserveScheduleState) previousAppState.swapRequests else swaps
        val effectiveScheduleSettings = if (canPreserveScheduleState) previousAppState.scheduleSettings else apiScheduleSettings.toLocalScheduleSettings()

        val defaultDays = nextSevenDays()
        val extraDays = effectiveShiftRequirements.map { it.day } + effectiveAvailabilitySlots.map { it.day }
        val days = (defaultDays + extraDays).distinct()

        return AppState(
            storeName = company.name,
            currentCompanyId = selectedCompanyId,
            currentCompanyEmail = company.email,
            currentCompanyDescription = company.description,
            availableCompanies = companies.map { CompanySummary(id = it.id, name = it.name) },
            days = days,
            employees = employees,
            viewerUserId = viewerMember?.userId.orEmpty(),
            viewerDisplayName = session.displayName,
            viewerEmail = session.email,
            viewerRoleInCompany = viewerRoleInCompany,
            wageRule = settings.wageRule,
            shiftRequirements = effectiveShiftRequirements,
            availabilitySlots = effectiveAvailabilitySlots,
            assignments = effectiveAssignments,
            swapRequests = effectiveSwapRequests,
            companyJoinRequests = companyJoinRequests,
            myJoinRequests = myJoinRequests,
            scheduleSettings = effectiveScheduleSettings,
        )
    }

    private fun upsertLocalShiftRequirement(shift: ApiShiftRequirement) {
        val localShift = shift.toLocalShiftRequirement()
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                shiftRequirements = upsertById(uiState.appState.shiftRequirements, localShift) { it.id },
                days = (uiState.appState.days + localShift.day).distinct(),
            ),
        )
    }

    private fun upsertLocalAvailabilitySlot(slot: ApiAvailabilitySlot) {
        upsertLocalAvailabilitySlot(slot.toLocalAvailabilitySlot())
    }

    private fun upsertLocalAvailabilitySlot(localSlot: AvailabilitySlot) {
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                availabilitySlots = upsertById(uiState.appState.availabilitySlots, localSlot) { it.id },
                days = (uiState.appState.days + localSlot.day).distinct(),
            ),
        )
    }

    private fun removeLocalAvailabilitySlot(slotId: String) {
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                availabilitySlots = uiState.appState.availabilitySlots.filterNot { it.id == slotId },
            ),
        )
    }

    private fun replaceLocalAvailabilitySlot(slotId: String, slot: AvailabilitySlot) {
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                availabilitySlots = replaceById(uiState.appState.availabilitySlots, slotId, slot) { it.id },
                days = (uiState.appState.days + slot.day).distinct(),
            ),
        )
    }

    private fun upsertLocalSwapRequest(request: SwapRequest) {
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                swapRequests = upsertById(uiState.appState.swapRequests, request) { it.id },
            ),
        )
    }

    private fun upsertLocalAssignment(assignment: ShiftAssignment) {
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                assignments = upsertById(uiState.appState.assignments, assignment) { it.id },
            ),
        )
    }

    private fun replaceLocalAssignment(assignmentId: String, assignment: ShiftAssignment) {
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                assignments = replaceById(uiState.appState.assignments, assignmentId, assignment) { it.id },
            ),
        )
    }

    private fun removeLocalAssignment(assignmentId: String) {
        uiState = uiState.copy(
            appState = uiState.appState.copy(
                assignments = uiState.appState.assignments.filterNot { it.id == assignmentId },
            ),
        )
    }

    private fun ApiShiftRequirement.toLocalShiftRequirement(): ShiftRequirement {
        val start = parseDateTime(startAt)
        val end = parseDateTime(endAt)
        return ShiftRequirement(
            id = id,
            day = dayLabel(start.toLocalDate()),
            block = ShiftBlock.fromMinuteRange(minuteOf(start), minuteOf(end)),
            role = if (employeeRole == ApiEmployeeRole.Manager) EmployeeRole.Manager.label else EmployeeRole.Staff.label,
            requiredCount = requiredCount.coerceAtLeast(1),
            isHoliday = note.contains("holiday", ignoreCase = true),
            startMinute = minuteOf(start),
            endMinute = minuteOf(end),
        )
    }

    private fun ApiAvailabilitySlot.toLocalAvailabilitySlot(fallback: AvailabilitySlot? = null): AvailabilitySlot {
        val start = startAt.takeIf { it.isNotBlank() }?.let(::parseDateTime)
        val end = endAt.takeIf { it.isNotBlank() }?.let(::parseDateTime)
        val startMinute = start?.let(::minuteOf) ?: fallback?.startMinute ?: 9 * 60
        val endMinute = end?.let(::minuteOf) ?: fallback?.endMinute ?: 17 * 60
        return AvailabilitySlot(
            id = id.ifBlank { fallback?.id.orEmpty() },
            employeeId = userId.ifBlank { fallback?.employeeId.orEmpty() },
            day = start?.toLocalDate()?.let(::dayLabel) ?: fallback?.day.orEmpty(),
            block = ShiftBlock.fromMinuteRange(startMinute, endMinute),
            startMinute = startMinute,
            endMinute = endMinute,
            note = "",
        )
    }

    private fun <T> upsertById(items: List<T>, item: T, idOf: (T) -> String): List<T> {
        val id = idOf(item)
        return if (items.any { idOf(it) == id }) {
            items.map { existing -> if (idOf(existing) == id) item else existing }
        } else {
            items + item
        }
    }

    private fun <T> replaceById(items: List<T>, id: String, item: T, idOf: (T) -> String): List<T> {
        return items.map { existing -> if (idOf(existing) == id) item else existing }
    }

    private fun saveSessionFromAuthResult(result: ApiAuthResult) {
        val refreshToken = result.refreshToken.orEmpty()
        if (refreshToken.isBlank()) {
            throw IllegalStateException("後端未回傳 refresh token cookie（支援 refreshToken / refresh_token）")
        }
        val now = System.currentTimeMillis()
        userSessionRepository.upsertSession(
            UserSession(
                userId = result.user.publicId,
                publicId = result.user.publicId,
                name = result.user.name,
                displayName = result.user.displayName,
                email = result.user.email,
                accessToken = result.user.accessToken,
                refreshToken = refreshToken,
                csrfToken = result.user.csrfToken,
                accessTokenExpiresAtEpochMs = now + ACCESS_TOKEN_EXPIRES_MS,
                refreshTokenExpiresAtEpochMs = now + REFRESH_TOKEN_EXPIRES_MS,
            ),
        )
    }

    private fun persistRefreshedAccessToken(newAccessToken: String?) {
        if (newAccessToken.isNullOrBlank()) return
        val session = userSessionRepository.getSession() ?: return
        userSessionRepository.upsertSession(
            session.copy(
                accessToken = newAccessToken,
                accessTokenExpiresAtEpochMs = System.currentTimeMillis() + ACCESS_TOKEN_EXPIRES_MS,
            ),
        )
    }

    private fun effectiveTokens(previous: ApiSessionTokens, newAccessToken: String?): ApiSessionTokens {
        return if (newAccessToken.isNullOrBlank()) previous else previous.copy(accessToken = newAccessToken)
    }

    private fun requireValidSessionAndSettings(): Pair<ApiSessionTokens, StoreSettings>? {
        val settings = settingsRepository.getSettings()
        val session = userSessionRepository.getSession()
        val now = System.currentTimeMillis()
        if (session == null || session.refreshTokenExpiresAtEpochMs <= now) {
            logout()
            return null
        }
        return ApiSessionTokens(session.accessToken, session.refreshToken) to settings
    }

    private fun backendClient(settings: StoreSettings): BackendApiClient {
        return BackendApiClient(baseUrl = settings.apiBaseUrl)
    }

    private fun emptyAppState(settings: StoreSettings, session: UserSession? = null): AppState {
        return AppState(
            storeName = settings.storeName,
            currentCompanyId = settings.selectedCompanyId,
            currentCompanyEmail = "",
            currentCompanyDescription = "",
            availableCompanies = emptyList(),
            days = nextSevenDays(),
            employees = emptyList(),
            viewerUserId = session?.userId.orEmpty(),
            viewerDisplayName = session?.displayName.orEmpty(),
            viewerEmail = session?.email.orEmpty(),
            viewerRoleInCompany = null,
            wageRule = settings.wageRule,
            shiftRequirements = emptyList(),
            availabilitySlots = emptyList(),
            assignments = emptyList(),
            swapRequests = emptyList(),
            companyJoinRequests = emptyList(),
            myJoinRequests = emptyList(),
            scheduleSettings = ScheduleSettings(),
        )
    }

    private fun ApiCompanyJoinRequest.toLocalJoinRequest(): CompanyJoinRequest {
        return CompanyJoinRequest(
            id = id,
            companyId = companyId,
            companyName = companyName,
            requesterUserId = requesterUserId,
            requesterName = requesterName,
            requesterEmail = requesterEmail,
            requestedRole = requestedRole.toLocalRole(),
            note = note,
            status = status.toJoinRequestStatus(),
            createdAtEpochMs = parseEpochMillis(createdAt),
            updatedAtEpochMs = parseEpochMillis(updatedAt),
            reviewedByUserId = reviewedByUserId,
        )
    }

    private fun ApiScheduleSettings?.toLocalScheduleSettings(): ScheduleSettings {
        return ScheduleSettings(
            autoApproveSwaps = this?.autoApproveSwaps ?: false,
            maxWeeklyHours = this?.maxWeeklyHours ?: 40,
            minRestHours = this?.minRestHours ?: 8,
            timezone = this?.timezone?.ifBlank { DEFAULT_TIMEZONE } ?: DEFAULT_TIMEZONE,
        )
    }

    private fun String.toJoinRequestStatus(): CompanyJoinRequestStatus {
        return when (this) {
            CompanyJoinRequestStatus.Approved.name -> CompanyJoinRequestStatus.Approved
            CompanyJoinRequestStatus.Rejected.name -> CompanyJoinRequestStatus.Rejected
            CompanyJoinRequestStatus.Cancelled.name -> CompanyJoinRequestStatus.Cancelled
            else -> CompanyJoinRequestStatus.Pending
        }
    }

    private fun parseEpochMillis(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        return runCatching {
            OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        }.recoverCatching {
            LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    }

    private fun parseDateTime(raw: String): LocalDateTime {
        if (raw.isBlank()) return LocalDateTime.now()
        return try {
            OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        } catch (_: Exception) {
            LocalDateTime.parse(raw)
        }
    }

    private fun minuteOf(dateTime: LocalDateTime): Int = dateTime.hour * 60 + dateTime.minute

    private fun dayLabel(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("E M/d", Locale.TAIWAN))
    }

    private fun resolveDate(day: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("E M/d", Locale.TAIWAN)
        val today = LocalDate.now()
        return (0..30)
            .map { today.plusDays(it.toLong()) }
            .firstOrNull { it.format(formatter) == day }
            ?: today
    }

    private fun toIsoDateTime(day: String, minuteOfDay: Int): String {
        val date = resolveDate(day)
        val normalized = minuteOfDay.coerceIn(0, 24 * 60)
        val dateTime = LocalDateTime.of(date, LocalTime.MIDNIGHT).plusMinutes(normalized.toLong())
        return dateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime().format(SCHEDULE_API_DATE_TIME_FORMATTER)
    }

    private fun nextSevenDays(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("E M/d", Locale.TAIWAN)
        val today = LocalDate.now()
        return (0..6).map { today.plusDays(it.toLong()).format(formatter) }
    }

    private fun currentWeekStartDate(): LocalDate {
        val today = LocalDate.now()
        return today.minusDays((today.dayOfWeek.value - 1).toLong())
    }

    private fun currentWeekStart(): String {
        return currentWeekStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun currentWeekRangeIso(): Pair<String, String> {
        val start = currentWeekStartDate()
        return start.toStartIso() to start.plusDays(7).toStartIso()
    }

    private fun LocalDate.toStartIso(): String {
        return atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime().format(SCHEDULE_API_DATE_TIME_FORMATTER)
    }

    private fun hasCoreData(appState: AppState): Boolean {
        return appState.employees.isNotEmpty() ||
            appState.shiftRequirements.isNotEmpty() ||
            appState.availabilitySlots.isNotEmpty() ||
            appState.assignments.isNotEmpty() ||
            appState.swapRequests.isNotEmpty()
    }

    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(isAuthLoading = true, authErrorMessage = null)
            try {
                block()
            } catch (e: Exception) {
                uiState = uiState.copy(isAuthLoading = false, authErrorMessage = e.message ?: "登入失敗")
                Log.e("ShiftHeroAuth", "Auth error", e)
            }
        }
    }

    private fun launchApi(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                val message = e.message.orEmpty()
                if (message.contains("Unauthorized", ignoreCase = true) || message.contains("token", ignoreCase = true)) {
                    logout()
                }
                uiState = uiState.copy(companyRequestMessage = e.message ?: "操作失敗")
                Log.e("ShiftHeroAPI", "API error", e)
            }
        }
    }

    private fun clearLocalSession() {
        userSessionRepository.clearSession()
        settingsRepository.setSelectedCompanyId("")
        val settings = settingsRepository.getSettings()
        currentCompanyId = ""
        uiState = uiState.copy(
            screen = NavigationItem.Dashboard,
            appState = emptyAppState(settings),
            apiBaseUrl = settings.apiBaseUrl,
            isAuthenticated = false,
            isDataHydrated = false,
            userProfile = null,
            isUserProfileLoading = false,
            userProfileErrorMessage = null,
            authErrorMessage = null,
            companyRequestMessage = null,
            scheduleInsight = ScheduleInsightUiState(),
            latestSuggestionMessages = emptyList(),
            showTutorialOverlay = true,
            tutorialStepIndex = 0,
        )
    }

    private fun ApiMe.toUserProfileUiState(): UserProfileUiState {
        return UserProfileUiState(
            publicId = publicId,
            name = name,
            displayName = displayName,
            email = email,
            role = role,
            plan = plan,
            status = status,
        )
    }

    private fun ApiCompanyMember.matchesSession(session: UserSession): Boolean {
        return userId.equals(session.userId, ignoreCase = true) ||
            email.equals(session.email, ignoreCase = true)
    }

    private fun String.toApiRole(): ApiEmployeeRole {
        return if (equals("Manager", ignoreCase = true) || contains("店長")) ApiEmployeeRole.Manager else ApiEmployeeRole.Staff
    }

    private fun EmployeeRole.toApiRole(): ApiEmployeeRole {
        return if (this == EmployeeRole.Manager) ApiEmployeeRole.Manager else ApiEmployeeRole.Staff
    }

    private fun ApiEmployeeRole.toLocalRole(): EmployeeRole {
        return if (this == ApiEmployeeRole.Manager) EmployeeRole.Manager else EmployeeRole.Staff
    }

    private fun String.toSwapStatus(): SwapStatus {
        return when (this) {
            SwapStatus.Claimed.name -> SwapStatus.Claimed
            SwapStatus.Approved.name -> SwapStatus.Approved
            SwapStatus.Cancelled.name -> SwapStatus.Cancelled
            else -> SwapStatus.Open
        }
    }

    private companion object {
        const val ACCESS_TOKEN_EXPIRES_MS: Long = 30L * 60L * 1000L
        const val REFRESH_TOKEN_EXPIRES_MS: Long = 14L * 24L * 60L * 60L * 1000L
        const val DEFAULT_TIMEZONE = "Asia/Taipei"
        val SCHEDULE_API_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    }
}
