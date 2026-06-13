package com.example.shifthero.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

enum class ApiEmployeeRole { Manager, Staff }

data class ApiCompany(
    val id: String,
    val name: String,
    val description: String,
    val email: String,
)

data class ApiCompanyMember(
    val userId: String,
    val name: String,
    val displayName: String,
    val email: String,
    val employeeRole: ApiEmployeeRole,
)

data class ApiShiftRequirement(
    val id: String,
    val companyId: String,
    val employeeRole: ApiEmployeeRole,
    val startAt: String,
    val endAt: String,
    val requiredCount: Int,
    val note: String,
)

data class ApiAvailabilitySlot(
    val id: String,
    val companyId: String,
    val userId: String,
    val startAt: String,
    val endAt: String,
    val isAvailable: Boolean,
)

data class ApiAssignment(
    val id: String,
    val companyId: String,
    val shiftRequirementId: String,
    val userId: String,
    val startAt: String,
    val endAt: String,
)

data class ApiScheduleSettings(
    val companyId: String,
    val autoApproveSwaps: Boolean,
    val maxWeeklyHours: Int,
    val minRestHours: Int,
    val timezone: String,
)

data class ApiScheduleInsightEmployee(
    val userId: String,
    val displayName: String,
    val employeeRole: String,
    val shiftCount: Int,
    val totalHours: Double,
    val longestShiftHours: Double,
    val nightShiftCount: Int,
    val weekendShiftCount: Int,
    val shortRestCount: Int,
    val availabilityConflicts: Int,
    val openSwapRequestCount: Int,
    val maxConsecutiveWorkDays: Int,
    val overtimeWeekCount: Int,
    val riskScore: Int,
    val riskLevel: String,
)

data class ApiScheduleInsightMetrics(
    val requiredHeadcount: Int,
    val assignedHeadcount: Int,
    val unfilledHeadcount: Int,
    val coverageRate: Double,
    val openSwapRequestCount: Int,
    val averageHours: Double,
    val workloadSpreadHours: Double,
    val employeesAtRisk: Int,
    val availabilityConflicts: Int,
    val employees: List<ApiScheduleInsightEmployee>,
)

data class ApiAiUsage(
    val used: Int,
    val limit: Int,
    val remaining: Int,
    val resetAt: String,
)

data class ApiScheduleInsight(
    val companyId: String,
    val companyName: String,
    val startAt: String,
    val endAt: String,
    val timezone: String,
    val locale: String,
    val model: String,
    val workflow: List<String>,
    val metrics: ApiScheduleInsightMetrics,
    val aiUsage: ApiAiUsage,
    val summary: String,
    val generatedAt: String,
)

sealed interface ApiScheduleInsightStreamEvent {
    data class Stage(val value: String) : ApiScheduleInsightStreamEvent
    data class Token(val value: String) : ApiScheduleInsightStreamEvent
    data class Done(
        val insight: ApiScheduleInsight,
        val newAccessToken: String?,
    ) : ApiScheduleInsightStreamEvent

    data class Error(val message: String) : ApiScheduleInsightStreamEvent
}

data class ApiSwapRequest(
    val id: String,
    val companyId: String,
    val shiftAssignmentId: String,
    val requesterUserId: String,
    val claimedByUserId: String?,
    val status: String,
    val reason: String,
)

data class ApiCompanyJoinRequest(
    val id: String,
    val companyId: String,
    val companyName: String,
    val requesterUserId: String,
    val requesterName: String,
    val requesterEmail: String,
    val requestedRole: ApiEmployeeRole,
    val note: String,
    val status: String,
    val reviewedByUserId: String?,
    val reviewedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class ApiAuthUser(
    val publicId: String,
    val name: String,
    val displayName: String,
    val email: String,
    val accessToken: String,
    val csrfToken: String,
)

data class ApiMe(
    val publicId: String,
    val name: String,
    val displayName: String,
    val email: String,
    val role: String,
    val plan: String,
    val status: String,
)

data class ApiAuthResult(
    val user: ApiAuthUser,
    val refreshToken: String?,
)

data class ApiSessionTokens(
    val accessToken: String,
    val refreshToken: String,
)

data class ApiEnvelope(
    val data: Any?,
    val newAccessToken: String?,
    val setCookies: List<String>,
)

private data class ApiAuthCookies(
    val accessToken: String?,
    val refreshToken: String?,
    val csrfToken: String?,
)

class BackendApiClient(
    private val baseUrl: String,
) {
    suspend fun login(account: String, password: String): ApiAuthResult {
        val envelope = request(
            method = "POST",
            path = "/auth/login",
            body = JSONObject().apply {
                put("account", account)
                put("password", password)
            },
        )
        val userJson = envelope.data as? JSONObject ?: JSONObject()
        val cookies = extractAuthCookies(envelope.setCookies)
        return ApiAuthResult(
            user = userJson.toAuthUser(
                accessTokenFallback = cookies.accessToken,
                csrfTokenFallback = cookies.csrfToken,
            ),
            refreshToken = cookies.refreshToken,
        )
    }

    suspend fun register(name: String, email: String, password: String): ApiAuthResult {
        val envelope = request(
            method = "POST",
            path = "/auth/register",
            body = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("password", password)
            },
        )
        val userJson = envelope.data as? JSONObject ?: JSONObject()
        val cookies = extractAuthCookies(envelope.setCookies)
        return ApiAuthResult(
            user = userJson.toAuthUser(
                accessTokenFallback = cookies.accessToken,
                csrfTokenFallback = cookies.csrfToken,
            ),
            refreshToken = cookies.refreshToken,
        )
    }

    suspend fun loginViaGoogle(authorizationCode: String): ApiAuthResult {
        val envelope = request(
            method = "POST",
            path = "/auth/loginViaGoogle",
            body = JSONObject().apply {
                put("authorizationCode", authorizationCode)
            },
        )
        val userJson = envelope.data as? JSONObject ?: JSONObject()
        val cookies = extractAuthCookies(envelope.setCookies)
        return ApiAuthResult(
            user = userJson.toAuthUser(
                accessTokenFallback = cookies.accessToken,
                csrfTokenFallback = cookies.csrfToken,
            ),
            refreshToken = cookies.refreshToken,
        )
    }

    suspend fun registerViaGoogle(authorizationCode: String): ApiAuthResult {
        val envelope = request(
            method = "POST",
            path = "/auth/registerViaGoogle",
            body = JSONObject().apply {
                put("authorizationCode", authorizationCode)
            },
        )
        val userJson = envelope.data as? JSONObject ?: JSONObject()
        val cookies = extractAuthCookies(envelope.setCookies)
        return ApiAuthResult(
            user = userJson.toAuthUser(
                accessTokenFallback = cookies.accessToken,
                csrfTokenFallback = cookies.csrfToken,
            ),
            refreshToken = cookies.refreshToken,
        )
    }

    suspend fun logout(tokens: ApiSessionTokens): String? {
        val envelope = request(
            method = "POST",
            path = "/auth/logout",
            tokens = tokens,
        )
        return envelope.newAccessToken
    }

    suspend fun getMe(tokens: ApiSessionTokens): Pair<ApiMe, String?> {
        val envelope = request(method = "GET", path = "/user/getMe", tokens = tokens)
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toMe() to envelope.newAccessToken
    }

    suspend fun updateMeDisplayName(displayName: String, tokens: ApiSessionTokens): String? {
        val envelope = request(
            method = "PUT",
            path = "/user/updateMe",
            body = JSONObject().apply {
                put(
                    "values",
                    JSONObject().apply {
                        put("displayName", displayName)
                    },
                )
            },
            tokens = tokens,
        )
        return envelope.newAccessToken
    }

    suspend fun createCompany(
        name: String,
        email: String,
        description: String,
        tokens: ApiSessionTokens,
    ): Pair<ApiCompany, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies",
            body = JSONObject().apply {
                put("name", name)
                put("email", email)
                if (description.isNotBlank()) put("description", description)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toCompany() to envelope.newAccessToken
    }

    suspend fun getMyCompanies(tokens: ApiSessionTokens): Pair<List<ApiCompany>, String?> {
        val envelope = request(method = "GET", path = "/companies/me", tokens = tokens)
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toCompany() } to envelope.newAccessToken
    }

    suspend fun getCompany(companyId: String, tokens: ApiSessionTokens): Pair<ApiCompany, String?> {
        val envelope = request(method = "GET", path = "/companies/$companyId", tokens = tokens)
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toCompany() to envelope.newAccessToken
    }

    suspend fun getCompanyMembers(companyId: String, tokens: ApiSessionTokens): Pair<List<ApiCompanyMember>, String?> {
        val envelope = request(method = "GET", path = "/companies/$companyId/members", tokens = tokens)
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toCompanyMember() } to envelope.newAccessToken
    }

    suspend fun addCompanyMember(companyId: String, userId: String, role: ApiEmployeeRole, tokens: ApiSessionTokens): String? {
        val envelope = request(
            method = "POST",
            path = "/companies/members",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("userId", userId)
                put("employeeRole", role.name)
            },
            tokens = tokens,
        )
        return envelope.newAccessToken
    }

    suspend fun removeCompanyMember(companyId: String, userId: String, tokens: ApiSessionTokens): String? {
        val envelope = request(
            method = "DELETE",
            path = "/companies/members",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("userId", userId)
            },
            tokens = tokens,
        )
        return envelope.newAccessToken
    }

    suspend fun updateCompany(
        companyId: String,
        name: String,
        email: String,
        description: String,
        tokens: ApiSessionTokens,
    ): Pair<ApiCompany, String?> {
        val envelope = request(
            method = "PATCH",
            path = "/companies",
            body = JSONObject().apply {
                put("companyId", companyId)
                put(
                    "values",
                    JSONObject().apply {
                        put("name", name)
                        put("email", email)
                        put("description", description)
                    },
                )
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toCompany() to envelope.newAccessToken
    }

    suspend fun createCompanyJoinRequest(
        companyId: String,
        requestedRole: ApiEmployeeRole,
        note: String,
        tokens: ApiSessionTokens,
    ): Pair<ApiCompanyJoinRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/joinRequests",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("requestedRole", requestedRole.name)
                if (note.isNotBlank()) put("note", note)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toCompanyJoinRequest() to envelope.newAccessToken
    }

    suspend fun getCompanyJoinRequests(companyId: String, tokens: ApiSessionTokens): Pair<List<ApiCompanyJoinRequest>, String?> {
        val envelope = request(
            method = "GET",
            path = "/companies/$companyId/joinRequests",
            tokens = tokens,
        )
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toCompanyJoinRequest() } to envelope.newAccessToken
    }

    suspend fun getMyCompanyJoinRequests(tokens: ApiSessionTokens): Pair<List<ApiCompanyJoinRequest>, String?> {
        val envelope = request(method = "GET", path = "/companies/joinRequests/me", tokens = tokens)
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toCompanyJoinRequest() } to envelope.newAccessToken
    }

    suspend fun approveCompanyJoinRequest(companyId: String, joinRequestId: String, tokens: ApiSessionTokens): Pair<ApiCompanyJoinRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/joinRequests/approve",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("joinRequestId", joinRequestId)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toCompanyJoinRequest() to envelope.newAccessToken
    }

    suspend fun rejectCompanyJoinRequest(companyId: String, joinRequestId: String, tokens: ApiSessionTokens): Pair<ApiCompanyJoinRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/joinRequests/reject",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("joinRequestId", joinRequestId)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toCompanyJoinRequest() to envelope.newAccessToken
    }

    suspend fun cancelCompanyJoinRequest(companyId: String, joinRequestId: String, tokens: ApiSessionTokens): Pair<ApiCompanyJoinRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/joinRequests/cancel",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("joinRequestId", joinRequestId)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toCompanyJoinRequest() to envelope.newAccessToken
    }

    suspend fun getScheduleSettings(companyId: String, tokens: ApiSessionTokens): Pair<ApiScheduleSettings, String?> {
        val envelope = request(method = "GET", path = "/companies/$companyId/scheduleSettings", tokens = tokens)
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toScheduleSettings() to envelope.newAccessToken
    }

    suspend fun updateScheduleSettings(
        companyId: String,
        autoApproveSwaps: Boolean,
        maxWeeklyHours: Int,
        minRestHours: Int,
        timezone: String,
        tokens: ApiSessionTokens,
    ): Pair<ApiScheduleSettings, String?> {
        val envelope = request(
            method = "PATCH",
            path = "/companies/scheduleSettings",
            body = JSONObject().apply {
                put("companyId", companyId)
                put(
                    "values",
                    JSONObject().apply {
                        put("autoApproveSwaps", autoApproveSwaps)
                        put("maxWeeklyHours", maxWeeklyHours)
                        put("minRestHours", minRestHours)
                        put("timezone", timezone)
                    },
                )
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toScheduleSettings() to envelope.newAccessToken
    }

    suspend fun getShiftRequirements(
        companyId: String,
        startAt: String? = null,
        endAt: String? = null,
        tokens: ApiSessionTokens,
    ): Pair<List<ApiShiftRequirement>, String?> {
        val envelope = request(
            method = "GET",
            path = "/companies/$companyId/shiftRequirements",
            query = mapOf("startAt" to startAt, "endAt" to endAt),
            tokens = tokens,
        )
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toShiftRequirement() } to envelope.newAccessToken
    }

    suspend fun createShiftRequirement(
        companyId: String,
        employeeRole: ApiEmployeeRole,
        startAt: String,
        endAt: String,
        requiredCount: Int,
        note: String,
        tokens: ApiSessionTokens,
    ): Pair<ApiShiftRequirement, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/shiftRequirements",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("employeeRole", employeeRole.name)
                put("startAt", startAt)
                put("endAt", endAt)
                put("requiredCount", requiredCount)
                put("note", note)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toShiftRequirement() to envelope.newAccessToken
    }

    suspend fun updateShiftRequirement(
        companyId: String,
        shiftRequirementId: String,
        employeeRole: ApiEmployeeRole,
        startAt: String,
        endAt: String,
        requiredCount: Int,
        note: String,
        tokens: ApiSessionTokens,
    ): String? {
        val envelope = request(
            method = "PATCH",
            path = "/companies/shiftRequirements",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("shiftRequirementId", shiftRequirementId)
                put(
                    "values",
                    JSONObject().apply {
                        put("employeeRole", employeeRole.name)
                        put("startAt", startAt)
                        put("endAt", endAt)
                        put("requiredCount", requiredCount)
                        put("note", note)
                    },
                )
            },
            tokens = tokens,
        )
        return envelope.newAccessToken
    }

    suspend fun getAvailabilitySlots(
        companyId: String,
        userId: String? = null,
        startAt: String? = null,
        endAt: String? = null,
        tokens: ApiSessionTokens,
    ): Pair<List<ApiAvailabilitySlot>, String?> {
        val envelope = request(
            method = "GET",
            path = "/companies/$companyId/availabilitySlots",
            query = mapOf("userId" to userId, "startAt" to startAt, "endAt" to endAt),
            tokens = tokens,
        )
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toAvailabilitySlot() } to envelope.newAccessToken
    }

    suspend fun upsertAvailabilitySlots(
        companyId: String,
        slots: List<Pair<String, String>>,
        tokens: ApiSessionTokens,
    ): Pair<List<ApiAvailabilitySlot>, String?> {
        val envelope = request(
            method = "PUT",
            path = "/companies/availabilitySlots",
            body = JSONObject().apply {
                put("companyId", companyId)
                put(
                    "slots",
                    JSONArray().apply {
                        slots.forEach { (startAt, endAt) ->
                            put(
                                JSONObject().apply {
                                    put("startAt", startAt)
                                    put("endAt", endAt)
                                    put("isAvailable", true)
                                },
                            )
                        }
                    },
                )
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toAvailabilitySlot() } to envelope.newAccessToken
    }

    suspend fun deleteAvailabilitySlot(companyId: String, availabilitySlotId: String, tokens: ApiSessionTokens): String? {
        val envelope = request(
            method = "DELETE",
            path = "/companies/availabilitySlots",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("availabilitySlotId", availabilitySlotId)
            },
            tokens = tokens,
        )
        return envelope.newAccessToken
    }

    suspend fun getAssignments(
        companyId: String,
        userId: String? = null,
        startAt: String? = null,
        endAt: String? = null,
        tokens: ApiSessionTokens,
    ): Pair<List<ApiAssignment>, String?> {
        val envelope = request(
            method = "GET",
            path = "/companies/$companyId/assignments",
            query = mapOf("userId" to userId, "startAt" to startAt, "endAt" to endAt),
            tokens = tokens,
        )
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toAssignment() } to envelope.newAccessToken
    }

    suspend fun generateAssignments(companyId: String, startAt: String? = null, endAt: String? = null, tokens: ApiSessionTokens): Pair<List<ApiAssignment>, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/assignments/generate",
            body = JSONObject().apply {
                put("companyId", companyId)
                if (!startAt.isNullOrBlank()) put("startAt", startAt)
                if (!endAt.isNullOrBlank()) put("endAt", endAt)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toAssignment() } to envelope.newAccessToken
    }

    suspend fun replaceAssignments(companyId: String, assignments: List<ApiAssignment>, tokens: ApiSessionTokens): String? {
        val envelope = request(
            method = "PUT",
            path = "/companies/assignments",
            body = JSONObject().apply {
                put("companyId", companyId)
                put(
                    "assignments",
                    JSONArray().apply {
                        assignments.forEach { assignment ->
                            put(
                                JSONObject().apply {
                                    put("shiftRequirementId", assignment.shiftRequirementId)
                                    put("userId", assignment.userId)
                                    put("startAt", assignment.startAt)
                                    put("endAt", assignment.endAt)
                                },
                            )
                        }
                    },
                )
            },
            tokens = tokens,
        )
        return envelope.newAccessToken
    }

    suspend fun claimAssignment(companyId: String, shiftRequirementId: String, tokens: ApiSessionTokens): Pair<ApiAssignment, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/assignments/claim",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("shiftRequirementId", shiftRequirementId)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toAssignment() to envelope.newAccessToken
    }

    suspend fun getSwapRequests(companyId: String, tokens: ApiSessionTokens): Pair<List<ApiSwapRequest>, String?> {
        val envelope = request(method = "GET", path = "/companies/$companyId/swapRequests", tokens = tokens)
        val data = envelope.data as? JSONArray ?: JSONArray()
        return (0 until data.length()).map { data.getJSONObject(it).toSwapRequest() } to envelope.newAccessToken
    }

    suspend fun createSwapRequest(companyId: String, shiftAssignmentId: String, reason: String, tokens: ApiSessionTokens): Pair<ApiSwapRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/swapRequests",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("shiftAssignmentId", shiftAssignmentId)
                put("reason", reason)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toSwapRequest() to envelope.newAccessToken
    }

    suspend fun claimSwapRequest(companyId: String, swapRequestId: String, tokens: ApiSessionTokens): Pair<ApiSwapRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/swapRequests/claim",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("swapRequestId", swapRequestId)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toSwapRequest() to envelope.newAccessToken
    }

    suspend fun approveSwapRequest(companyId: String, swapRequestId: String, tokens: ApiSessionTokens): Pair<ApiSwapRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/swapRequests/approve",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("swapRequestId", swapRequestId)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toSwapRequest() to envelope.newAccessToken
    }

    suspend fun cancelSwapRequest(companyId: String, swapRequestId: String, tokens: ApiSessionTokens): Pair<ApiSwapRequest, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/swapRequests/cancel",
            body = JSONObject().apply {
                put("companyId", companyId)
                put("swapRequestId", swapRequestId)
            },
            tokens = tokens,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toSwapRequest() to envelope.newAccessToken
    }

    suspend fun generateScheduleInsights(
        companyId: String,
        startAt: String,
        endAt: String,
        locale: String,
        focus: String,
        tokens: ApiSessionTokens,
    ): Pair<ApiScheduleInsight, String?> {
        val envelope = request(
            method = "POST",
            path = "/companies/$companyId/ai/scheduleInsights",
            body = scheduleInsightsBody(startAt, endAt, locale, focus),
            tokens = tokens,
            readTimeoutMs = STREAM_READ_TIMEOUT_MS,
        )
        val data = envelope.data as? JSONObject ?: JSONObject()
        return data.toScheduleInsight() to envelope.newAccessToken
    }

    suspend fun streamScheduleInsights(
        companyId: String,
        startAt: String,
        endAt: String,
        locale: String,
        focus: String,
        tokens: ApiSessionTokens,
        onEvent: suspend (ApiScheduleInsightStreamEvent) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val fullUrl = "${baseUrl.trimEnd('/')}/companies/$companyId/ai/scheduleInsights/stream"
        val payload = scheduleInsightsBody(startAt, endAt, locale, focus)
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = STREAM_READ_TIMEOUT_MS
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("User-Agent", "ShiftHero-Android/1.0")
            if (tokens.accessToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${tokens.accessToken}")
            }
            if (tokens.refreshToken.isNotBlank()) {
                setRequestProperty(
                    "Cookie",
                    "refreshToken=${tokens.refreshToken}; refresh_token=${tokens.refreshToken}",
                )
            }
            setFixedLengthStreamingMode(payload.size)
            doOutput = true
            outputStream.use { stream ->
                stream.write(payload)
                stream.flush()
            }
        }

        try {
            val statusCode = conn.responseCode
            if (statusCode !in 200..299) {
                val raw = conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
                val root = raw.takeIf { it.isNotBlank() }?.let(::JSONObject)
                throw IllegalStateException(root.apiErrorMessage("AI briefing stream failed ($statusCode)"))
            }

            BufferedReader(conn.inputStream.reader(StandardCharsets.UTF_8)).use { reader ->
                var eventName = "message"
                val dataLines = mutableListOf<String>()

                suspend fun dispatchEvent() {
                    if (dataLines.isEmpty()) return
                    parseScheduleInsightStreamEvent(eventName, dataLines.joinToString("\n"))?.let { event ->
                        onEvent(event)
                    }
                    eventName = "message"
                    dataLines.clear()
                }

                while (true) {
                    val line = reader.readLine() ?: break
                    when {
                        line.isBlank() -> dispatchEvent()
                        line.startsWith(":") -> Unit
                        line.startsWith("event:") -> eventName = line.substringAfter(':').trim()
                        line.startsWith("data:") -> dataLines += line.substringAfter(':').trimStart()
                    }
                }
                dispatchEvent()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun scheduleInsightsBody(
        startAt: String,
        endAt: String,
        locale: String,
        focus: String,
    ) = JSONObject().apply {
        put("startAt", startAt)
        put("endAt", endAt)
        put("locale", locale)
        if (focus.isNotBlank()) put("focus", focus)
    }

    private fun parseScheduleInsightStreamEvent(
        eventName: String,
        rawData: String,
    ): ApiScheduleInsightStreamEvent? {
        val json = runCatching { JSONObject(rawData) }.getOrNull()
        val scalar = runCatching { JSONArray("[$rawData]").optString(0) }.getOrNull().orEmpty()
        val fallbackText = scalar.ifBlank { rawData }
        return when (eventName.lowercase()) {
            "stage" -> ApiScheduleInsightStreamEvent.Stage(
                json.stringValue("stage", "value", "message").ifBlank { fallbackText },
            )
            "token" -> ApiScheduleInsightStreamEvent.Token(
                json.stringValue("token", "text", "delta", "value").ifBlank { fallbackText },
            )
            "done" -> ApiScheduleInsightStreamEvent.Done(
                insight = json.unwrapDataObject().toScheduleInsight(),
                newAccessToken = json.findNewAccessToken(),
            )
            "error" -> ApiScheduleInsightStreamEvent.Error(
                json.apiErrorMessage(fallbackText.ifBlank { "AI briefing stream failed" }),
            )
            else -> null
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        query: Map<String, String?> = emptyMap(),
        body: JSONObject? = null,
        tokens: ApiSessionTokens? = null,
        readTimeoutMs: Int = READ_TIMEOUT_MS,
    ): ApiEnvelope = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trimEnd('/')
        val encodedQuery = query.entries
            .filter { !it.value.isNullOrBlank() }
            .joinToString("&") { (key, value) ->
                key + "=" + URLEncoder.encode(value ?: "", StandardCharsets.UTF_8.name())
            }
        val fullUrl = buildString {
            append(cleanBase)
            append(path)
            if (encodedQuery.isNotBlank()) {
                append("?")
                append(encodedQuery)
            }
        }

        val payload = if (method != "GET" && body != null) {
            body.toString().toByteArray(StandardCharsets.UTF_8)
        } else {
            null
        }

        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ShiftHero-Android/1.0")
            if (!tokens?.accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer ${tokens?.accessToken}")
            }
            if (!tokens?.refreshToken.isNullOrBlank()) {
                setRequestProperty(
                    "Cookie",
                    "refreshToken=${tokens?.refreshToken}; refresh_token=${tokens?.refreshToken}",
                )
            }
            if (payload != null) {
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setFixedLengthStreamingMode(payload.size)
                doOutput = true
                outputStream.use { stream ->
                    stream.write(payload)
                    stream.flush()
                }
            }
        }

        val statusCode = conn.responseCode
        val raw = try {
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            BufferedReader(stream.reader(StandardCharsets.UTF_8)).use { it.readText() }
        } catch (_: Exception) {
            ""
        }

        val root = if (raw.isNotBlank()) JSONObject(raw) else JSONObject()
        val success = root.optBoolean("success", statusCode in 200..299)
        if (!success || statusCode !in 200..299) {
            val message = root.apiErrorMessage("Backend request failed: $method $path ($statusCode)")
            throw IllegalStateException(message)
        }

        val refreshableTokens = root.optJSONObject("refreshableTokens")
        val newAccessToken = refreshableTokens?.optString("newAccessToken")?.takeIf { it.isNotBlank() }
        val cookieHeaders = conn.headerFields
            .filterKeys { key -> key != null && key.equals("Set-Cookie", ignoreCase = true) }
            .values
            .flatten()

        val envelope = ApiEnvelope(
            data = root.opt("data"),
            newAccessToken = newAccessToken,
            setCookies = cookieHeaders,
        )
        conn.disconnect()
        envelope
    }

    private fun extractAuthCookies(setCookieHeaders: List<String>): ApiAuthCookies {
        return ApiAuthCookies(
            accessToken = extractCookieValue(setCookieHeaders, listOf("accessToken", "access_token", "access-token")),
            refreshToken = extractCookieValue(setCookieHeaders, listOf("refreshToken", "refresh_token", "refresh-token")),
            csrfToken = extractCookieValue(setCookieHeaders, listOf("csrfToken", "csrf_token", "csrf-token")),
        )
    }

    private fun extractCookieValue(setCookieHeaders: List<String>, candidateNames: List<String>): String? {
        val normalizedNames = candidateNames.map { it.trim().lowercase() }.toSet()
        var latestNonBlankValue: String? = null
        for (header in setCookieHeaders) {
            val firstSegment = header.substringBefore(';')
            val parts = firstSegment.split('=', limit = 2)
            if (parts.size != 2) continue

            val cookieName = parts[0].trim().lowercase()
            if (cookieName in normalizedNames) {
                val rawValue = parts[1].trim().trim('"')
                val decoded = try {
                    URLDecoder.decode(rawValue, StandardCharsets.UTF_8.name())
                } catch (_: Exception) {
                    rawValue
                }
                if (decoded.isNotBlank()) {
                    latestNonBlankValue = decoded
                }
            }
        }
        return latestNonBlankValue
    }

    private fun JSONObject.toAuthUser(
        accessTokenFallback: String? = null,
        csrfTokenFallback: String? = null,
    ) = ApiAuthUser(
        publicId = optString("publicId"),
        name = optString("name"),
        displayName = optString("displayName"),
        email = optString("email"),
        accessToken = optString("accessToken").ifBlank { accessTokenFallback.orEmpty() },
        csrfToken = optString("csrfToken").ifBlank { csrfTokenFallback.orEmpty() },
    )

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 60_000
        const val STREAM_READ_TIMEOUT_MS = 5 * 60_000
    }

    private fun JSONObject.toCompany() = ApiCompany(
        id = optString("id"),
        name = optString("name"),
        description = optString("description"),
        email = optString("email"),
    )

    private fun JSONObject.toCompanyMember() = ApiCompanyMember(
        userId = optString("userId"),
        name = optString("name"),
        displayName = optString("displayName"),
        email = optString("email"),
        employeeRole = parseEmployeeRole(optString("employeeRole")),
    )

    private fun JSONObject.toShiftRequirement() = ApiShiftRequirement(
        id = optString("id"),
        companyId = optString("companyId"),
        employeeRole = parseEmployeeRole(optString("employeeRole")),
        startAt = optString("startAt"),
        endAt = optString("endAt"),
        requiredCount = optInt("requiredCount", 1),
        note = optString("note"),
    )

    private fun JSONObject.toAvailabilitySlot() = ApiAvailabilitySlot(
        id = optString("id"),
        companyId = optString("companyId"),
        userId = optString("userId"),
        startAt = optString("startAt"),
        endAt = optString("endAt"),
        isAvailable = optBoolean("isAvailable", true),
    )

    private fun JSONObject.toAssignment() = ApiAssignment(
        id = optString("id"),
        companyId = optString("companyId"),
        shiftRequirementId = optString("shiftRequirementId"),
        userId = optString("userId"),
        startAt = optString("startAt"),
        endAt = optString("endAt"),
    )

    private fun JSONObject.toScheduleSettings() = ApiScheduleSettings(
        companyId = optString("companyId"),
        autoApproveSwaps = optBoolean("autoApproveSwaps", false),
        maxWeeklyHours = optInt("maxWeeklyHours", 40),
        minRestHours = optInt("minRestHours", 8),
        timezone = optString("timezone", "Asia/Taipei"),
    )

    private fun JSONObject.toScheduleInsight() = ApiScheduleInsight(
        companyId = optString("companyId"),
        companyName = optString("companyName"),
        startAt = optString("startAt"),
        endAt = optString("endAt"),
        timezone = optString("timezone"),
        locale = optString("locale"),
        model = optString("model"),
        workflow = optJSONArray("workflow").toStringList(),
        metrics = optJSONObject("metrics").toScheduleInsightMetrics(),
        aiUsage = optJSONObject("aiUsage").toAiUsage(),
        summary = optString("summary"),
        generatedAt = optString("generatedAt"),
    )

    private fun JSONObject?.toScheduleInsightMetrics(): ApiScheduleInsightMetrics {
        val json = this ?: JSONObject()
        return ApiScheduleInsightMetrics(
            requiredHeadcount = json.optInt("requiredHeadcount"),
            assignedHeadcount = json.optInt("assignedHeadcount"),
            unfilledHeadcount = json.optInt("unfilledHeadcount"),
            coverageRate = json.optDouble("coverageRate"),
            openSwapRequestCount = json.optInt("openSwapRequestCount"),
            averageHours = json.optDouble("averageHours"),
            workloadSpreadHours = json.optDouble("workloadSpreadHours"),
            employeesAtRisk = json.optInt("employeesAtRisk"),
            availabilityConflicts = json.optInt("availabilityConflicts"),
            employees = json.optJSONArray("employees").toScheduleInsightEmployees(),
        )
    }

    private fun JSONArray?.toScheduleInsightEmployees(): List<ApiScheduleInsightEmployee> {
        val array = this ?: JSONArray()
        return (0 until array.length()).map { index ->
            val json = array.optJSONObject(index) ?: JSONObject()
            ApiScheduleInsightEmployee(
                userId = json.optString("userId"),
                displayName = json.optString("displayName"),
                employeeRole = json.optString("employeeRole"),
                shiftCount = json.optInt("shiftCount"),
                totalHours = json.optDouble("totalHours"),
                longestShiftHours = json.optDouble("longestShiftHours"),
                nightShiftCount = json.optInt("nightShiftCount"),
                weekendShiftCount = json.optInt("weekendShiftCount"),
                shortRestCount = json.optInt("shortRestCount"),
                availabilityConflicts = json.optInt("availabilityConflicts"),
                openSwapRequestCount = json.optInt("openSwapRequestCount"),
                maxConsecutiveWorkDays = json.optInt("maxConsecutiveWorkDays"),
                overtimeWeekCount = json.optInt("overtimeWeekCount"),
                riskScore = json.optInt("riskScore"),
                riskLevel = json.optString("riskLevel"),
            )
        }
    }

    private fun JSONObject?.toAiUsage(): ApiAiUsage {
        val json = this ?: JSONObject()
        return ApiAiUsage(
            used = json.optInt("used"),
            limit = json.optInt("limit"),
            remaining = json.optInt("remaining"),
            resetAt = json.optString("resetAt"),
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        val array = this ?: JSONArray()
        return (0 until array.length()).map { array.optString(it) }
    }

    private fun JSONObject?.stringValue(vararg keys: String): String {
        val json = this ?: return ""
        keys.forEach { key ->
            val value = json.optString(key)
            if (value.isNotBlank()) return value
        }
        val nested = json.opt("data")
        if (nested is String) return nested
        if (nested is JSONObject) return nested.stringValue(*keys)
        return ""
    }

    private fun JSONObject?.unwrapDataObject(): JSONObject {
        var current = this ?: JSONObject()
        repeat(3) {
            if (current.has("metrics") || current.has("summary")) return current
            val nested = current.optJSONObject("data") ?: return current
            current = nested
        }
        return current
    }

    private fun JSONObject?.findNewAccessToken(): String? {
        val json = this ?: return null
        return json.optJSONObject("refreshableTokens")
            ?.optString("newAccessToken")
            ?.takeIf { it.isNotBlank() }
            ?: json.optJSONObject("data").findNewAccessToken()
    }

    private fun JSONObject?.apiErrorMessage(fallback: String): String {
        val json = this ?: return fallback
        val exception = json.optJSONObject("exception")
            ?: json.optJSONObject("data")?.optJSONObject("exception")
        return exception?.optString("message")?.takeIf { it.isNotBlank() }
            ?: exception?.optString("reason")?.takeIf { it.isNotBlank() }
            ?: json.optString("message").takeIf { it.isNotBlank() }
            ?: fallback
    }

    private fun JSONObject.toSwapRequest() = ApiSwapRequest(
        id = optString("id"),
        companyId = optString("companyId"),
        shiftAssignmentId = optString("shiftAssignmentId"),
        requesterUserId = optString("requesterUserId"),
        claimedByUserId = if (isNull("claimedByUserId")) null else optString("claimedByUserId"),
        status = optString("status"),
        reason = optString("reason"),
    )

    private fun JSONObject.toCompanyJoinRequest() = ApiCompanyJoinRequest(
        id = optString("id"),
        companyId = optString("companyId"),
        companyName = optString("companyName"),
        requesterUserId = optString("requesterUserId"),
        requesterName = optString("requesterName"),
        requesterEmail = optString("requesterEmail"),
        requestedRole = parseEmployeeRole(optString("requestedRole")),
        note = optString("note"),
        status = optString("status"),
        reviewedByUserId = if (isNull("reviewedByUserId")) null else optString("reviewedByUserId"),
        reviewedAt = if (isNull("reviewedAt")) null else optString("reviewedAt"),
        createdAt = optString("createdAt"),
        updatedAt = optString("updatedAt"),
    )

    private fun parseEmployeeRole(raw: String): ApiEmployeeRole {
        return if (raw.equals(ApiEmployeeRole.Manager.name, ignoreCase = true)) ApiEmployeeRole.Manager else ApiEmployeeRole.Staff
    }

    private fun JSONObject.toMe() = ApiMe(
        publicId = optString("publicId"),
        name = optString("name"),
        displayName = optString("displayName"),
        email = optString("email"),
        role = optString("role"),
        plan = optString("plan"),
        status = optString("status"),
    )
}
