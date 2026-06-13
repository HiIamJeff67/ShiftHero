package com.example.shifthero.pages

import com.example.shifthero.components.addemployeecard.AddEmployeeCard
import com.example.shifthero.components.addshiftrequirementcard.AddShiftRequirementCard
import com.example.shifthero.components.compactaction.CompactAction
import com.example.shifthero.components.emptyhint.EmptyHint
import com.example.shifthero.components.helpers.employeeName
import com.example.shifthero.components.heatmapstrip.HeatmapStrip
import com.example.shifthero.components.inforow.InfoRow
import com.example.shifthero.components.metriccard.MetricCard
import com.example.shifthero.components.screenframe.ScreenFrame
import com.example.shifthero.components.sectioncard.SectionCard
import com.example.shifthero.components.selectablepill.SelectablePill
import com.example.shifthero.components.shiftline.ShiftLine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shifthero.core.model.AppThemeMode
import com.example.shifthero.core.model.AvailabilitySlot
import com.example.shifthero.core.model.NavigationItem
import com.example.shifthero.core.model.CompanyJoinRequest
import com.example.shifthero.core.model.CompanyJoinRequestStatus
import com.example.shifthero.core.model.Employee
import com.example.shifthero.core.model.EmployeeRole
import com.example.shifthero.core.model.HeatCell
import com.example.shifthero.core.model.HeatStatus
import com.example.shifthero.core.model.PayrollSummary
import com.example.shifthero.core.model.ShiftBlock
import com.example.shifthero.core.model.AppState
import com.example.shifthero.core.model.ShiftRequirement
import com.example.shifthero.core.model.SwapRequest
import com.example.shifthero.core.model.SwapStatus
import com.example.shifthero.core.model.durationHours
import com.example.shifthero.core.model.timeRangeLabel
import com.example.shifthero.core.network.ApiConfig
import com.example.shifthero.core.network.ApiScheduleInsight
import com.example.shifthero.domain.payroll.PayrollCalculator
import com.example.shifthero.domain.scheduling.SchedulingEngine
import com.example.shifthero.feature.ScheduleInsightUiState
import com.example.shifthero.feature.SchedulePlannerViewModel
import com.example.shifthero.feature.UserProfileUiState
import com.example.shifthero.ui.theme.ShiftHeroTheme
import com.example.shifthero.ui.theme.ShiftHeroThemeTokens
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt


@Composable
internal fun ManagementSplitScreen(
    appState: AppState,
    messages: List<String>,
    companyRequestMessage: String?,
    scheduleInsight: ScheduleInsightUiState,
    contentPadding: PaddingValues,
    onCompanySelected: (String) -> Unit,
    onSubmitJoinRequest: (String, String, String, EmployeeRole) -> Unit,
    onCancelJoinRequest: (String, String) -> Unit,
    onRefreshJoinRequests: () -> Unit,
    onLeaveCompany: () -> Unit,
    onUpdateCompanyProfile: (String, String, String) -> Unit,
    onApproveJoinRequest: (String) -> Unit,
    onRejectJoinRequest: (String) -> Unit,
    onUpdateShiftTime: (String, Int, Int) -> Unit,
    onAddShiftRequirement: (String, Int, Int, EmployeeRole, Int) -> Unit,
    onAddAvailabilitySlot: (String, Int, Int) -> Unit,
    onDeleteAvailabilitySlot: (AvailabilitySlot) -> Unit,
    onGenerate: () -> Unit,
    onGenerateScheduleInsights: (String, String, String, String) -> Unit,
    onStreamScheduleInsights: (String, String, String, String) -> Unit,
    onCreateSwap: (ShiftRequirement) -> Unit,
    onClaimShift: (ShiftRequirement) -> Unit,
    onOpenSwapPage: () -> Unit,
) {
    var tab by remember { mutableStateOf(ManagementTab.Team) }

    ScreenFrame(
        title = "Management",
        subtitle = "拆分管理：人員、排班時間軸、營運分析。",
        contentPadding = contentPadding,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ManagementTab.entries.forEach { item ->
                SelectablePill(
                    label = item.label,
                    selected = tab == item,
                    modifier = Modifier.weight(1f),
                    onClick = { tab = item },
                )
            }
        }

        when (tab) {
            ManagementTab.Team -> {
                val isManager = appState.viewerRoleInCompany == EmployeeRole.Manager
                val hasCompany = appState.currentCompanyId.isNotBlank()

                if (isManager) {
                    CompanyAdminSection(
                        appState = appState,
                        onUpdateCompanyProfile = onUpdateCompanyProfile,
                    )
                } else if (hasCompany) {
                    CompanyMemberSection(
                        appState = appState,
                        onLeaveCompany = onLeaveCompany,
                    )
                } else {
                    SectionCard(title = "申請加入公司") {
                        JoinCompanyRequestPopoverButton(
                            defaultCompanyId = "",
                            defaultCompanyName = "",
                            onSubmitRequest = onSubmitJoinRequest,
                        )
                    }
                }

                SectionCard(title = "員工管理") {
                    if (appState.employees.isEmpty()) {
                        EmptyHint("目前尚無員工。")
                    } else {
                        appState.employees.forEach { employee ->
                            InfoRow(employee.name, employee.role.label)
                        }
                    }
                }
            }

            ManagementTab.Requests -> {
                CompanyRequestsSection(
                    appState = appState,
                    message = companyRequestMessage,
                    onCompanySelected = onCompanySelected,
                    onSubmitJoinRequest = onSubmitJoinRequest,
                    onCancelJoinRequest = onCancelJoinRequest,
                    onRefresh = onRefreshJoinRequests,
                    onApproveJoinRequest = onApproveJoinRequest,
                    onRejectJoinRequest = onRejectJoinRequest,
                )
            }

            ManagementTab.Schedule -> {
                ScheduleTimelineSection(
                    appState = appState,
                    onUpdateShiftTime = onUpdateShiftTime,
                    onAddShiftRequirement = onAddShiftRequirement,
                    onAddAvailabilitySlot = onAddAvailabilitySlot,
                    onDeleteAvailabilitySlot = onDeleteAvailabilitySlot,
                    onGenerate = onGenerate,
                    onCreateSwap = onCreateSwap,
                    onClaimShift = onClaimShift,
                    onOpenSwapPage = onOpenSwapPage,
                )
            }

            ManagementTab.Insights -> {
                val heatCells = SchedulingEngine.heatmap(appState.shiftRequirements, appState.assignments)
                val shortage = SchedulingEngine.totalShortage(appState.shiftRequirements, appState.assignments)
                var selectedDay by remember(appState.days) { mutableStateOf(appState.days.firstOrNull().orEmpty()) }
                var showDayMenu by remember { mutableStateOf(false) }
                val context = LocalContext.current
                var scheduleExportPayload by remember { mutableStateOf("") }
                var exportMessage by remember { mutableStateOf<String?>(null) }
                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv"),
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(scheduleExportPayload.toByteArray())
                        } ?: error("無法建立匯出檔案")
                    }.onSuccess {
                        exportMessage = "已匯出班表 CSV。"
                    }.onFailure { error ->
                        exportMessage = error.message ?: "匯出失敗"
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("缺人", shortage.toString(), "尚未填補人數", Modifier.weight(1f))
                    MetricCard("已排班", appState.assignments.size.toString(), "目前有效指派", Modifier.weight(1f))
                }

                ScheduleInsightSection(
                    appState = appState,
                    state = scheduleInsight,
                    onGenerate = onGenerateScheduleInsights,
                    onStream = onStreamScheduleInsights,
                )

                SectionCard(title = "Schedule Export") {
                    Text(
                        "匯出檔會依日期與開始時間排序，Excel、Numbers 或 Google Sheets 都可以直接開啟。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            scheduleExportPayload = buildScheduleExportCsv(appState, heatCells)
                            exportLauncher.launch("shifthero-schedule-${System.currentTimeMillis()}.csv")
                        },
                        enabled = appState.shiftRequirements.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Export CSV")
                    }
                    exportMessage?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (appState.shiftRequirements.isEmpty()) {
                        EmptyHint("尚無班表需求可匯出。")
                    }
                }

                SectionCard(title = "當日每小時熱力圖") {
                    OutlinedButton(onClick = { showDayMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedDay.isBlank()) "選擇日期" else selectedDay)
                    }
                    androidx.compose.material3.DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
                        appState.days.forEach { day ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = day
                                    showDayMenu = false
                                },
                            )
                        }
                    }
                    HourlyCoverageHeatmap(appState = appState, selectedDay = selectedDay)
                }

                SectionCard(title = "本週每日熱力圖") {
                    WeeklyCoverageHeatmap(appState = appState)
                }

                SectionCard(title = "班次缺口總覽") {
                    if (heatCells.isEmpty()) EmptyHint("新增班別需求後會顯示。")
                    appState.days.forEach { day ->
                        val dayCells = heatCells.filter { it.shift.day == day }
                        if (dayCells.isNotEmpty()) {
                            Text(day, style = MaterialTheme.typography.labelLarge)
                            HeatmapStrip(heatCells = dayCells)
                        }
                    }
                }
                if (messages.isNotEmpty()) {
                    SectionCard(title = "推薦原因") {
                        messages.forEach { message ->
                            Text("• $message", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScheduleInsightSection(
    appState: AppState,
    state: ScheduleInsightUiState,
    onGenerate: (String, String, String, String) -> Unit,
    onStream: (String, String, String, String) -> Unit,
) {
    var startDate by remember(appState.currentCompanyId) {
        mutableStateOf(LocalDate.now().toString())
    }
    var endDate by remember(appState.currentCompanyId) {
        mutableStateOf(LocalDate.now().plusDays(7).toString())
    }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var locale by remember(appState.currentCompanyId) { mutableStateOf("zh-TW") }
    var focus by remember(appState.currentCompanyId) { mutableStateOf("") }
    val canGenerate = appState.currentCompanyId.isNotBlank() &&
        appState.viewerRoleInCompany == EmployeeRole.Manager &&
        !state.isLoading

    SectionCard(title = "AI Schedule Insights") {
        Text(
            "使用後端的 deterministic metrics 與 LLM workflow，產生覆蓋率、疲勞、可上班衝突及換班風險簡報。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showStartDatePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("開始日 $startDate")
            }
            OutlinedButton(
                onClick = { showEndDatePicker = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("結束日 $endDate")
            }
        }
        Text(
            "結束日不包含在分析範圍內，最多 31 天。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (showStartDatePicker) {
            ScheduleInsightDatePickerDialog(
                initialDate = startDate,
                onDismiss = { showStartDatePicker = false },
                onDateSelected = {
                    startDate = it.toString()
                    showStartDatePicker = false
                },
            )
        }
        if (showEndDatePicker) {
            ScheduleInsightDatePickerDialog(
                initialDate = endDate,
                onDismiss = { showEndDatePicker = false },
                onDateSelected = {
                    endDate = it.toString()
                    showEndDatePicker = false
                },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SelectablePill(
                label = "繁體中文",
                selected = locale == "zh-TW",
                modifier = Modifier.weight(1f),
                onClick = { locale = "zh-TW" },
            )
            SelectablePill(
                label = "English",
                selected = locale == "en",
                modifier = Modifier.weight(1f),
                onClick = { locale = "en" },
            )
        }

        OutlinedTextField(
            value = focus,
            onValueChange = { focus = it.take(500) },
            label = { Text("分析重點（選填）") },
            placeholder = { Text("例如：優先檢查週末缺人與員工疲勞風險") },
            supportingText = { Text("${focus.length}/500") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onGenerate(startDate, endDate, locale, focus) },
                enabled = canGenerate,
                modifier = Modifier.weight(1f),
            ) {
                Text("完整產生")
            }
            Button(
                onClick = { onStream(startDate, endDate, locale, focus) },
                enabled = canGenerate,
                modifier = Modifier.weight(1f),
            ) {
                Text("串流產生")
            }
        }

        if (appState.viewerRoleInCompany != EmployeeRole.Manager) {
            EmptyHint("此功能僅限公司 Manager 使用。")
        }

        if (state.isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    state.stage.ifBlank { "正在產生 AI 班表洞察" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (state.stage.isNotBlank()) {
            Text(
                state.stage,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        state.errorMessage?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        val summary = state.streamedSummary.ifBlank { state.insight?.summary.orEmpty() }
        if (summary.isNotBlank()) {
            Text(
                cleanInsightSummary(summary),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.insight?.let { insight ->
            ScheduleInsightResult(insight)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScheduleInsightDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val initialMillis = runCatching {
        LocalDate.parse(initialDate)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) {
                Text("確認")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun ScheduleInsightResult(insight: ApiScheduleInsight) {
    val metrics = insight.metrics
    val coveragePercent = (metrics.coverageRate * 100).roundToInt()
    InfoRow("覆蓋率", "$coveragePercent%")
    InfoRow("需求 / 已排 / 缺口", "${metrics.requiredHeadcount} / ${metrics.assignedHeadcount} / ${metrics.unfilledHeadcount}")
    InfoRow("平均工時", "${formatDecimal(metrics.averageHours)} 小時")
    InfoRow("工時差距", "${formatDecimal(metrics.workloadSpreadHours)} 小時")
    InfoRow("風險員工", metrics.employeesAtRisk.toString())
    InfoRow("可上班衝突", metrics.availabilityConflicts.toString())
    InfoRow("待處理換班", metrics.openSwapRequestCount.toString())

    val usage = insight.aiUsage
    InfoRow("本月 AI 額度", "${usage.used} / ${usage.limit}（剩餘 ${usage.remaining}）")
    if (usage.resetAt.isNotBlank()) InfoRow("額度重置", usage.resetAt)
    if (insight.model.isNotBlank()) InfoRow("模型", insight.model)

    val riskEmployees = metrics.employees
        .filter { it.riskLevel.equals("high", true) || it.riskLevel.equals("critical", true) }
        .sortedByDescending { it.riskScore }
    if (riskEmployees.isNotEmpty()) {
        Text("高風險員工", style = MaterialTheme.typography.labelLarge)
        riskEmployees.take(5).forEach { employee ->
            InfoRow(
                employee.displayName.ifBlank { employee.userId },
                "${employee.riskLevel} · ${formatDecimal(employee.totalHours)}h · score ${employee.riskScore}",
            )
        }
    }
}

private fun cleanInsightSummary(summary: String): String {
    return summary
        .lineSequence()
        .joinToString("\n") { line -> line.trimStart().trimStart('#').trimStart() }
        .replace("**", "")
        .trim()
}

private fun formatDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}

private fun buildScheduleExportCsv(appState: AppState, heatCells: List<HeatCell>): String {
    val dayOrder = appState.days.withIndex().associate { it.value to it.index }
    val assignmentsByShift = appState.assignments.groupBy { it.shiftId }
    val heatByShiftId = heatCells.associateBy { it.shift.id }
    val rows = mutableListOf(
        listOf("Date", "Start", "End", "Role", "Required", "Assigned", "Shortage", "Status", "Assigned Employees"),
    )
    appState.shiftRequirements
        .sortedWith(
            compareBy<ShiftRequirement> { dayOrder[it.day] ?: Int.MAX_VALUE }
                .thenBy { it.startMinute }
                .thenBy { it.endMinute }
                .thenBy { it.role },
        )
        .forEach { shift ->
            val assignments = assignmentsByShift[shift.id].orEmpty()
            val assignedNames = assignments.map { appState.employeeName(it.employeeId) }
            val shortage = (shift.requiredCount - assignments.size).coerceAtLeast(0)
            rows += listOf(
                shift.day,
                minuteToLabel(shift.startMinute),
                minuteToLabel(shift.endMinute),
                shift.role,
                shift.requiredCount.toString(),
                assignments.size.toString(),
                shortage.toString(),
                heatByShiftId[shift.id]?.status?.label ?: if (shortage > 0) HeatStatus.Shortage.label else HeatStatus.Balanced.label,
                if (assignedNames.isEmpty()) "尚未分配" else assignedNames.joinToString(" / "),
            )
        }
    return rows.joinToString(separator = "\n", prefix = "\uFEFF") { row ->
        row.joinToString(",") { value -> value.toCsvCell() }
    }
}

private fun String.toCsvCell(): String {
    val escaped = replace("\"", "\"\"")
    return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"$escaped\""
    } else {
        escaped
    }
}

@Composable
internal fun ScheduleTimelineSection(
    appState: AppState,
    onUpdateShiftTime: (String, Int, Int) -> Unit,
    onAddShiftRequirement: (String, Int, Int, EmployeeRole, Int) -> Unit,
    onAddAvailabilitySlot: (String, Int, Int) -> Unit,
    onDeleteAvailabilitySlot: (AvailabilitySlot) -> Unit,
    onGenerate: () -> Unit,
    onCreateSwap: (ShiftRequirement) -> Unit,
    onClaimShift: (ShiftRequirement) -> Unit,
    onOpenSwapPage: () -> Unit,
) {
    var selectedDay by remember(appState.days) { mutableStateOf(appState.days.firstOrNull().orEmpty()) }
    var showDayMenu by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftRequirement?>(null) }
    val isManager = appState.viewerRoleInCompany == EmployeeRole.Manager
    val hasCompany = appState.currentCompanyId.isNotBlank()
    val assignedByShift = appState.assignments.groupBy { it.shiftId }

    SectionCard(title = "公司班表狀態") {
        InfoRow("時區", appState.scheduleSettings.timezone)
        InfoRow("班別需求", appState.shiftRequirements.size.toString())
        InfoRow("已排班", appState.assignments.size.toString())
        InfoRow("可上班時段", appState.availabilitySlots.size.toString())
    }

    if (!hasCompany) {
        SectionCard(title = "建立班表") {
            EmptyHint("請先加入或建立公司後，再建立班表。")
        }
        return
    }

    if (isManager) {
        MinuteShiftRequirementCard(
            days = appState.days,
            onCreate = onAddShiftRequirement,
        )
        CompactAction(
            "產生推薦班表",
            Modifier.fillMaxWidth(),
            enabled = appState.employees.isNotEmpty() && appState.shiftRequirements.isNotEmpty(),
            onClick = onGenerate,
        )
    } else {
        SectionCard(title = "建立公司班表") {
            EmptyHint("Staff 可提交自己的可上班時段；公司班別需求與推薦排班由 Manager 建立。")
        }
    }

    SectionCard(title = "我的可上班時段") {
        MinuteAvailabilityCard(
            days = appState.days,
            slots = appState.availabilitySlots.filter { it.employeeId == appState.viewerUserId },
            onCreate = onAddAvailabilitySlot,
            onDelete = onDeleteAvailabilitySlot,
        )
        if (appState.availabilitySlots.none { it.employeeId == appState.viewerUserId }) {
            Text(
                "提交後，Manager 產生班表時會用這些分鐘級可上班時段。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionCard(title = "我的已指派班表 Timeline") {
        val myAssignedShiftIds = appState.assignments
            .filter { it.employeeId == appState.viewerUserId }
            .map { it.shiftId }
            .toSet()
        val hasMyShiftOnSelectedDay = appState.shiftRequirements.any {
            it.day == selectedDay && it.id in myAssignedShiftIds
        }
        if (selectedDay.isBlank() || appState.viewerUserId.isBlank()) {
            EmptyHint("請先加入公司，並選擇日期。")
        } else if (!hasMyShiftOnSelectedDay) {
            EmptyHint("此日期尚未有指派給你的班次。可上班時段只供排班參考，不會直接建立班表。")
        } else {
            EmployeeTimelineEditor(
                appState = appState,
                selectedDay = selectedDay,
                employeeId = appState.viewerUserId,
                editable = isManager,
                onUpdateShiftTime = onUpdateShiftTime,
                onRequestEdit = { editingShift = it },
            )
        }
    }

    SectionCard(title = "選擇瀏覽日期") {
        OutlinedButton(
            onClick = { showDayMenu = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (selectedDay.isBlank()) "選擇日期" else selectedDay)
        }
        androidx.compose.material3.DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
            appState.days.forEach { day ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(day) },
                    onClick = {
                        selectedDay = day
                        showDayMenu = false
                    },
                )
            }
        }
    }

    SectionCard(title = "公司班表瀏覽") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CompactAction("前往 Shift", Modifier.fillMaxWidth(), enabled = appState.shiftRequirements.isNotEmpty(), onClick = onOpenSwapPage)
        }
        val shifts = appState.shiftRequirements.filter { it.day == selectedDay }
        if (shifts.isEmpty()) {
            EmptyHint("當日尚無班次。")
        } else {
            shifts.forEach { shift ->
                val assignedNames = assignedByShift[shift.id].orEmpty().map { appState.employeeName(it.employeeId) }
                val assignments = assignedByShift[shift.id].orEmpty()
                val shortage = (shift.requiredCount - assignments.size).coerceAtLeast(0)
                ShiftLine(
                    shift = shift,
                    note = "${shift.timeRangeLabel()} · ${if (assignedNames.isEmpty()) "尚未分配" else assignedNames.joinToString("、")}",
                    status = if (shortage > 0) HeatStatus.Shortage else HeatStatus.Balanced,
                )
                if (isManager) {
                    OutlinedButton(onClick = { editingShift = shift }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("編輯時間")
                    }
                }
                if (assignments.any { it.employeeId == appState.viewerUserId }) {
                    OutlinedButton(onClick = { onCreateSwap(shift) }, modifier = Modifier.fillMaxWidth()) {
                        Text("發布這班的 Shift 請求")
                    }
                } else if (shortage > 0) {
                    Button(onClick = { onClaimShift(shift) }, modifier = Modifier.fillMaxWidth()) {
                        Text("接受 Shift")
                    }
                }
            }
        }
    }

    editingShift?.let { shift ->
        ShiftTimePopover(
            shift = shift,
            onDismiss = { editingShift = null },
            onSave = { startMinute, endMinute ->
                onUpdateShiftTime(shift.id, startMinute, endMinute)
                editingShift = null
            },
        )
    }
}

@Composable
internal fun MinuteShiftRequirementCard(
    days: List<String>,
    onCreate: (String, Int, Int, EmployeeRole, Int) -> Unit,
) {
    var selectedDay by remember(days) { mutableStateOf(days.firstOrNull().orEmpty()) }
    var startMinute by remember { mutableStateOf(9 * 60) }
    var endMinute by remember { mutableStateOf(13 * 60) }
    var employeeRole by remember { mutableStateOf(EmployeeRole.Staff) }
    var requiredCount by remember { mutableStateOf("1") }

    SectionCard(title = "建立班表需求") {
        DayAndMinuteRangeControls(
            days = days,
            selectedDay = selectedDay,
            startMinute = startMinute,
            endMinute = endMinute,
            onDaySelected = { selectedDay = it },
            onStartMinuteChanged = { startMinute = it.coerceAtMost(endMinute - 1) },
            onEndMinuteChanged = { endMinute = it.coerceAtLeast(startMinute + 1) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SelectablePill(
                label = "Staff",
                selected = employeeRole == EmployeeRole.Staff,
                modifier = Modifier.weight(1f),
                onClick = { employeeRole = EmployeeRole.Staff },
            )
            SelectablePill(
                label = "Manager",
                selected = employeeRole == EmployeeRole.Manager,
                modifier = Modifier.weight(1f),
                onClick = { employeeRole = EmployeeRole.Manager },
            )
        }
        OutlinedTextField(
            value = requiredCount,
            onValueChange = { requiredCount = it },
            label = { Text("需求人數") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        CompactAction(
            label = "建立 ${minuteToLabel(startMinute)}-${minuteToLabel(endMinute)} 班表需求",
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDay.isNotBlank() && endMinute > startMinute,
            onClick = {
                onCreate(selectedDay, startMinute, endMinute, employeeRole, requiredCount.toIntOrNull() ?: 1)
                selectedDay = days.firstOrNull().orEmpty()
                startMinute = 9 * 60
                endMinute = 13 * 60
                employeeRole = EmployeeRole.Staff
                requiredCount = "1"
            },
        )
    }
}

@Composable
private fun MinuteAvailabilityCard(
    days: List<String>,
    slots: List<AvailabilitySlot>,
    onCreate: (String, Int, Int) -> Unit,
    onDelete: (AvailabilitySlot) -> Unit,
) {
    var selectedDay by remember(days) { mutableStateOf(days.firstOrNull().orEmpty()) }
    var startMinute by remember { mutableStateOf(9 * 60) }
    var endMinute by remember { mutableStateOf(17 * 60) }

    DayAndMinuteRangeControls(
        days = days,
        selectedDay = selectedDay,
        startMinute = startMinute,
        endMinute = endMinute,
        onDaySelected = { selectedDay = it },
        onStartMinuteChanged = { startMinute = it.coerceAtMost(endMinute - 1) },
        onEndMinuteChanged = { endMinute = it.coerceAtLeast(startMinute + 1) },
    )
    CompactAction(
        label = "新增可上班 ${minuteToLabel(startMinute)}-${minuteToLabel(endMinute)}",
        modifier = Modifier.fillMaxWidth(),
        enabled = selectedDay.isNotBlank() && endMinute > startMinute,
        onClick = {
            onCreate(selectedDay, startMinute, endMinute)
            startMinute = 9 * 60
            endMinute = 17 * 60
        },
    )

    val selectedDaySlots = slots
        .filter { it.day == selectedDay }
        .sortedBy { it.startMinute }
    if (selectedDaySlots.isEmpty()) {
        EmptyHint("這一天尚未提交可上班時段。")
    } else {
        selectedDaySlots.forEach { slot ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${minuteToLabel(slot.startMinute)}-${minuteToLabel(slot.endMinute)}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedButton(
                    onClick = { onDelete(slot) },
                    enabled = slot.id.isNotBlank(),
                ) {
                    Text("刪除")
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DayAndMinuteRangeControls(
    days: List<String>,
    selectedDay: String,
    startMinute: Int,
    endMinute: Int,
    onDaySelected: (String) -> Unit,
    onStartMinuteChanged: (Int) -> Unit,
    onEndMinuteChanged: (Int) -> Unit,
) {
    var showDayMenu by remember { mutableStateOf(false) }
    var pickingTarget by remember { mutableStateOf(ShiftTimePickingTarget.Start) }
    var showTimePicker by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showDayMenu = true }, modifier = Modifier.fillMaxWidth()) {
        Text(if (selectedDay.isBlank()) "選擇日期" else selectedDay)
    }
    androidx.compose.material3.DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
        days.forEach { day ->
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(day) },
                onClick = {
                    onDaySelected(day)
                    showDayMenu = false
                },
            )
        }
    }

    MinuteRangePreview(startMinute = startMinute, endMinute = endMinute)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                pickingTarget = ShiftTimePickingTarget.Start
                showTimePicker = true
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("開始 ${minuteToLabel(startMinute)}")
        }
        OutlinedButton(
            onClick = {
                pickingTarget = ShiftTimePickingTarget.End
                showTimePicker = true
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("結束 ${minuteToLabel(endMinute)}")
        }
    }

    if (showTimePicker) {
        val initialMinute = if (pickingTarget == ShiftTimePickingTarget.Start) startMinute else endMinute
        val timePickerState = rememberTimePickerState(
            initialHour = (initialMinute / 60).coerceIn(0, 23),
            initialMinute = (initialMinute % 60).coerceIn(0, 59),
            is24Hour = true,
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (pickingTarget == ShiftTimePickingTarget.Start) "選擇開始時間" else "選擇結束時間") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = timePickerState.hour * 60 + timePickerState.minute
                        if (pickingTarget == ShiftTimePickingTarget.Start) {
                            onStartMinuteChanged(value)
                        } else {
                            onEndMinuteChanged(value)
                        }
                        showTimePicker = false
                    },
                ) { Text("套用") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun MinuteRangePreview(startMinute: Int, endMinute: Int) {
    val normalizedStart = startMinute.coerceIn(0, 24 * 60)
    val normalizedEnd = endMinute.coerceIn(normalizedStart, 24 * 60)
    val before = normalizedStart.coerceAtLeast(0)
    val duration = (normalizedEnd - normalizedStart).coerceAtLeast(1)
    val after = (24 * 60 - normalizedEnd).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            shape = RoundedCornerShape(8.dp),
            color = ShiftHeroThemeTokens.colors.cardMuted,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (before > 0) Spacer(modifier = Modifier.weight(before.toFloat()))
                Box(
                    modifier = Modifier
                        .weight(duration.toFloat())
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
                if (after > 0) Spacer(modifier = Modifier.weight(after.toFloat()))
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("00:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${minuteToLabel(startMinute)}-${minuteToLabel(endMinute)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text("24:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun minuteToLabel(value: Int): String {
    val minute = value.coerceIn(0, 24 * 60)
    return "%02d:%02d".format(minute / 60, minute % 60)
}

@Composable
internal fun EmployeeTimelineEditor(
    appState: AppState,
    selectedDay: String,
    employeeId: String,
    editable: Boolean,
    onUpdateShiftTime: (String, Int, Int) -> Unit,
    onRequestEdit: (ShiftRequirement) -> Unit,
) {
    val assignedShiftIds = appState.assignments
        .filter { it.employeeId == employeeId }
        .map { it.shiftId }
        .toSet()
    val shifts = appState.shiftRequirements
        .filter { it.day == selectedDay && it.id in assignedShiftIds }
        .sortedBy { it.startMinute }
    var zoom by remember { mutableStateOf(1f) }
    val scrollState = rememberScrollState()
    val transformState = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, _, _ ->
        zoom = (zoom * zoomChange).coerceIn(0.7f, 3.2f)
    }
    val hourWidth = 72.dp * zoom

    val colors = ShiftHeroThemeTokens.colors
    val timelineWidth = hourWidth * 24
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hourWidthPx = with(density) { hourWidth.toPx() }
    val minutePerPx = 60f / hourWidthPx
    val leadingPaddingPx = with(density) { 24.dp.toPx() }

    LaunchedEffect(selectedDay, shifts.firstOrNull()?.id, scrollState.maxValue) {
        val firstShift = shifts.firstOrNull() ?: return@LaunchedEffect
        if (scrollState.maxValue <= 0) return@LaunchedEffect
        val target = ((firstShift.startMinute / 60f) * hourWidthPx - leadingPaddingPx)
            .roundToInt()
            .coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(target)
    }

    Text("縮放：${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardMuted)
            .horizontalScroll(scrollState)
            .then(Modifier.transformable(transformState)),
    ) {
        Canvas(
            modifier = Modifier
                .width(timelineWidth)
                .fillMaxSize(),
        ) {
            val col = colors.divider
            for (hour in 0..24) {
                val x = hour * hourWidth.toPx()
                drawLine(
                    color = col,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (hour % 6 == 0) 2.6f else 1.3f,
                )
            }
        }

        for (hour in 0 until 24) {
            Text(
                text = "%02d:00".format(hour),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(x = hourWidth * hour)
                    .padding(start = 4.dp, top = 8.dp),
            )
        }

        shifts.forEach { shift ->
            var draftStart by remember(shift.id, shift.startMinute, shift.endMinute) { mutableStateOf(shift.startMinute) }
            var draftEnd by remember(shift.id, shift.startMinute, shift.endMinute) { mutableStateOf(shift.endMinute) }
            val minDuration = 15
            val xOffset = ((draftStart / 60f) * hourWidth.value).dp
            val eventWidth = (((draftEnd - draftStart).coerceAtLeast(minDuration) / 60f) * hourWidth.value).dp

            Box(
                modifier = Modifier
                    .padding(top = 38.dp)
                    .offset(x = xOffset, y = 0.dp)
                    .width(eventWidth)
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                    .then(
                        if (editable) {
                            Modifier
                                .clickable { onRequestEdit(shift) }
                                .pointerInput(shift.id) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val deltaMinute = (dragAmount.x * minutePerPx).toInt()
                                            val duration = (draftEnd - draftStart).coerceAtLeast(minDuration)
                                            draftStart = (draftStart + deltaMinute).coerceIn(0, 24 * 60 - duration)
                                            draftEnd = draftStart + duration
                                        },
                                        onDragEnd = { onUpdateShiftTime(shift.id, draftStart, draftEnd) },
                                    )
                                }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(shift.role, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(shift.timeRangeLabel(), style = MaterialTheme.typography.bodySmall)
                }

                if (editable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            .pointerInput("${shift.id}_start") {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaMinute = (dragAmount.x * minutePerPx).toInt()
                                        draftStart = (draftStart + deltaMinute).coerceIn(0, draftEnd - minDuration)
                                    },
                                    onDragEnd = { onUpdateShiftTime(shift.id, draftStart, draftEnd) },
                                )
                            },
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(10.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f))
                            .pointerInput("${shift.id}_end") {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaMinute = (dragAmount.x * minutePerPx).toInt()
                                        draftEnd = (draftEnd + deltaMinute).coerceIn(draftStart + minDuration, 24 * 60)
                                    },
                                    onDragEnd = { onUpdateShiftTime(shift.id, draftStart, draftEnd) },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
internal fun HourlyCoverageHeatmap(
    appState: AppState,
    selectedDay: String,
) {
    val fractions = remember(appState.shiftRequirements, appState.assignments, selectedDay) {
        val assignedCountByShift = appState.assignments.groupBy { it.shiftId }.mapValues { it.value.size }
        val minutes = DoubleArray(24)
        appState.shiftRequirements.filter { it.day == selectedDay }.forEach { shift ->
            val people = assignedCountByShift[shift.id] ?: 0
            if (people <= 0) return@forEach
            for (hour in 0 until 24) {
                val hourStart = hour * 60
                val hourEnd = (hour + 1) * 60
                val overlap = (minOf(shift.endMinute, hourEnd) - maxOf(shift.startMinute, hourStart)).coerceAtLeast(0)
                minutes[hour] += overlap * people
            }
        }
        minutes.map { (it / 60.0).coerceIn(0.0, 1.0).toFloat() }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        fractions.forEach { fraction ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                color = ShiftHeroThemeTokens.colors.cardMuted,
                shape = RoundedCornerShape(4.dp),
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("00:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("12:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("24:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun WeeklyCoverageHeatmap(appState: AppState) {
    val assignedCountByShift = remember(appState.assignments) {
        appState.assignments.groupBy { it.shiftId }.mapValues { it.value.size }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        appState.days.forEach { day ->
            val dayShifts = appState.shiftRequirements.filter { it.day == day }
            val requiredMinutes = dayShifts.sumOf { (it.endMinute - it.startMinute).coerceAtLeast(0) * it.requiredCount }
            val staffedMinutes = dayShifts.sumOf { shift ->
                (shift.endMinute - shift.startMinute).coerceAtLeast(0) * (assignedCountByShift[shift.id] ?: 0)
            }
            val fraction = if (requiredMinutes <= 0) 0f else (staffedMinutes.toFloat() / requiredMinutes).coerceIn(0f, 1f)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                color = ShiftHeroThemeTokens.colors.cardMuted,
            ) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                    )
                    Text(
                        text = day.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ShiftTimePopover(
    shift: ShiftRequirement,
    onDismiss: () -> Unit,
    onSave: (startMinute: Int, endMinute: Int) -> Unit,
) {
    fun minuteToLabel(value: Int): String {
        val minute = value.coerceIn(0, 24 * 60)
        return "%02d:%02d".format(minute / 60, minute % 60)
    }

    val startState = rememberTimePickerState(
        initialHour = (shift.startMinute / 60).coerceIn(0, 23),
        initialMinute = (shift.startMinute % 60).coerceIn(0, 59),
        is24Hour = true,
    )
    val endState = rememberTimePickerState(
        initialHour = (shift.endMinute / 60).coerceIn(0, 23),
        initialMinute = (shift.endMinute % 60).coerceIn(0, 59),
        is24Hour = true,
    )
    var pickingTarget by remember(shift.id) { mutableStateOf(ShiftTimePickingTarget.Start) }
    var hasError by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯班表時間") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${shift.day} · ${shift.role}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SelectablePill(
                        label = "開始 ${minuteToLabel(startState.hour * 60 + startState.minute)}",
                        selected = pickingTarget == ShiftTimePickingTarget.Start,
                        modifier = Modifier.weight(1f),
                        onClick = { pickingTarget = ShiftTimePickingTarget.Start; hasError = false },
                    )
                    SelectablePill(
                        label = "結束 ${minuteToLabel(endState.hour * 60 + endState.minute)}",
                        selected = pickingTarget == ShiftTimePickingTarget.End,
                        modifier = Modifier.weight(1f),
                        onClick = { pickingTarget = ShiftTimePickingTarget.End; hasError = false },
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ShiftHeroThemeTokens.colors.cardMuted,
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        when (pickingTarget) {
                            ShiftTimePickingTarget.Start -> TimePicker(state = startState)
                            ShiftTimePickingTarget.End -> TimePicker(state = endState)
                        }
                    }
                }
                if (hasError) {
                    Text(
                        "結束時間必須晚於開始時間",
                        color = ShiftHeroThemeTokens.colors.destructive,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = startState.hour * 60 + startState.minute
                    val end = endState.hour * 60 + endState.minute
                    if (end <= start) {
                        hasError = true
                    } else {
                        onSave(start, end)
                    }
                },
            ) { Text("儲存") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
internal fun CompanyAdminSection(
    appState: AppState,
    onUpdateCompanyProfile: (String, String, String) -> Unit,
) {
    var companyName by remember(appState.storeName) { mutableStateOf(appState.storeName) }
    var companyEmail by remember(appState.currentCompanyEmail) { mutableStateOf(appState.currentCompanyEmail) }
    var companyDescription by remember(appState.currentCompanyDescription) { mutableStateOf(appState.currentCompanyDescription) }
    val canSave = companyName.isNotBlank() && companyEmail.isNotBlank()

    SectionCard(title = "公司管理") {
        Text(
            "可修改公司名稱、聯絡 Email、描述。公司 ID 為固定值不可變更。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CompanyIdCopyRow(companyId = appState.currentCompanyId)
        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("公司名稱") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = companyEmail,
            onValueChange = { companyEmail = it },
            label = { Text("公司 Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = companyDescription,
            onValueChange = { companyDescription = it },
            label = { Text("公司描述") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )
        Button(
            onClick = { onUpdateCompanyProfile(companyName, companyEmail, companyDescription) },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("儲存公司資料")
        }
    }
}

@Composable
internal fun CompanyMemberSection(
    appState: AppState,
    onLeaveCompany: () -> Unit,
) {
    SectionCard(title = "公司成員") {
        InfoRow("目前公司", appState.storeName)
        CompanyIdCopyRow(companyId = appState.currentCompanyId)
        if (appState.currentCompanyEmail.isNotBlank()) {
            InfoRow("聯絡 Email", appState.currentCompanyEmail)
        }
        if (appState.currentCompanyDescription.isNotBlank()) {
            Text(appState.currentCompanyDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = onLeaveCompany,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftHeroThemeTokens.colors.destructive),
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("退出公司")
        }
    }
}

@Composable
internal fun CompanyIdCopyRow(companyId: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember(companyId) { mutableStateOf(false) }

    InfoRow("公司 ID", companyId)
    OutlinedButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(companyId))
            copied = true
        },
        enabled = companyId.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.ContentCopy, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (copied) "已複製公司 ID" else "複製公司 ID")
    }
}

@Composable
internal fun CompanyRequestsSection(
    appState: AppState,
    message: String?,
    onCompanySelected: (String) -> Unit,
    onSubmitJoinRequest: (String, String, String, EmployeeRole) -> Unit,
    onCancelJoinRequest: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onApproveJoinRequest: (String) -> Unit,
    onRejectJoinRequest: (String) -> Unit,
) {
    val isManager = appState.viewerRoleInCompany == EmployeeRole.Manager
    CompanySwitcher(appState = appState, onCompanySelected = onCompanySelected)

    if (!message.isNullOrBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (isManager) {
        val pendingRequests = appState.companyJoinRequests.filter { it.status == CompanyJoinRequestStatus.Pending }

        SectionCard(title = "申請加入本公司的員工") {
            CompanyIdCopyRow(companyId = appState.currentCompanyId)
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("重新整理申請")
            }
            if (pendingRequests.isEmpty()) {
                EmptyHint("目前沒有其他人申請加入這家公司。")
            } else {
                pendingRequests.forEach { request ->
                    JoinRequestRow(
                        request = request,
                        onApprove = { onApproveJoinRequest(request.id) },
                        onReject = { onRejectJoinRequest(request.id) },
                        onCancel = null,
                    )
                }
            }
        }
    } else {
        val hasCompany = appState.currentCompanyId.isNotBlank()
        if (!hasCompany) {
            SectionCard(title = "申請加入公司") {
                JoinCompanyRequestPopoverButton(
                    defaultCompanyId = "",
                    defaultCompanyName = "",
                    onSubmitRequest = onSubmitJoinRequest,
                )
            }
        }

        SectionCard(title = "我的加入申請") {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("重新整理申請")
            }
            if (appState.myJoinRequests.isEmpty()) {
                EmptyHint("尚未送出任何加入公司申請。")
            } else {
                appState.myJoinRequests.forEach { request ->
                    JoinRequestRow(
                        request = request,
                        onApprove = null,
                        onReject = null,
                        onCancel = if (request.status == CompanyJoinRequestStatus.Pending) {
                            { onCancelJoinRequest(request.companyId, request.id) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        if (hasCompany) {
            SectionCard(title = "目前公司") {
                InfoRow("公司", appState.storeName)
                CompanyIdCopyRow(companyId = appState.currentCompanyId)
            }
        }
    }
}

@Composable
internal fun CompanySwitcher(
    appState: AppState,
    onCompanySelected: (String) -> Unit,
) {
    if (appState.availableCompanies.size <= 1) return

    var expanded by remember { mutableStateOf(false) }
    val selected = appState.availableCompanies.firstOrNull { it.id == appState.currentCompanyId }

    SectionCard(title = "切換公司") {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Business, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(selected?.name ?: "選擇公司", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            appState.availableCompanies.forEach { company ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(company.name) },
                    onClick = {
                        expanded = false
                        onCompanySelected(company.id)
                    },
                )
            }
        }
    }
}

@Composable
internal fun JoinCompanyRequestPopoverButton(
    defaultCompanyId: String,
    defaultCompanyName: String,
    onSubmitRequest: (String, String, String, EmployeeRole) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    var companyId by remember(defaultCompanyId) { mutableStateOf(defaultCompanyId) }
    var companyName by remember(defaultCompanyName) { mutableStateOf(defaultCompanyName) }
    var note by remember { mutableStateOf("") }
    var requestedRole by remember { mutableStateOf(EmployeeRole.Staff) }

    OutlinedButton(
        onClick = { show = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.PersonAddAlt1, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("發送加入公司申請")
    }

    if (show) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("加入公司申請") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = companyId,
                        onValueChange = { companyId = it },
                        label = { Text("公司 ID（必填）") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("公司名稱（選填）") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("備註（選填）") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SelectablePill(
                            label = "Staff",
                            selected = requestedRole == EmployeeRole.Staff,
                            modifier = Modifier.weight(1f),
                            onClick = { requestedRole = EmployeeRole.Staff },
                        )
                        SelectablePill(
                            label = "Manager",
                            selected = requestedRole == EmployeeRole.Manager,
                            modifier = Modifier.weight(1f),
                            onClick = { requestedRole = EmployeeRole.Manager },
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (companyId.isNotBlank()) {
                            onSubmitRequest(companyId, companyName, note, requestedRole)
                            show = false
                            note = ""
                        }
                    },
                    enabled = companyId.isNotBlank(),
                ) { Text("送出申請") }
            },
            dismissButton = { OutlinedButton(onClick = { show = false }) { Text("取消") } },
        )
    }
}

@Composable
internal fun JoinRequestRow(
    request: CompanyJoinRequest,
    onApprove: (() -> Unit)?,
    onReject: (() -> Unit)?,
    onCancel: (() -> Unit)?,
) {
    val statusColor = when (request.status) {
        CompanyJoinRequestStatus.Pending -> MaterialTheme.colorScheme.primary
        CompanyJoinRequestStatus.Approved -> ShiftHeroThemeTokens.colors.heatBalanced
        CompanyJoinRequestStatus.Rejected -> ShiftHeroThemeTokens.colors.destructive
        CompanyJoinRequestStatus.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ShiftHeroThemeTokens.colors.cardMuted,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HowToReg, contentDescription = null, tint = statusColor)
                    Text(request.requesterName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    request.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
            InfoRow("Email", request.requesterEmail)
            InfoRow("公司 ID", request.companyId)
            InfoRow("申請角色", request.requestedRole.label)
            if (request.note.isNotBlank()) {
                Text(request.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onApprove != null && onReject != null && request.status == CompanyJoinRequestStatus.Pending) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompactAction("核准", Modifier.weight(1f), onClick = onApprove)
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftHeroThemeTokens.colors.destructive),
                    ) {
                        Text("婉拒")
                    }
                }
            } else if (onCancel != null && request.status == CompanyJoinRequestStatus.Pending) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftHeroThemeTokens.colors.destructive),
                ) {
                    Text("撤銷申請")
                }
            }
        }
    }
}

@Composable
internal fun LegacyManagementHubScreen(
    appState: AppState,
    messages: List<String>,
    contentPadding: PaddingValues,
    onAddEmployee: (String, Int, Double) -> Unit,
    onToggle: (String, ShiftBlock) -> Unit,
    onAddShift: (String, ShiftBlock, String, Int, Boolean) -> Unit,
    onGenerate: () -> Unit,
    onCreateSwap: (ShiftRequirement) -> Unit,
    onClaim: (SwapRequest) -> Unit,
    onApprove: (SwapRequest) -> Unit,
    onCancel: (SwapRequest) -> Unit,
) {
    val heatCells = SchedulingEngine.heatmap(appState.shiftRequirements, appState.assignments)
    val shortage = SchedulingEngine.totalShortage(appState.shiftRequirements, appState.assignments)
    val assignedByShift = appState.assignments.groupBy { it.shiftId }

    ScreenFrame(
        title = "Management",
        subtitle = "員工、可上班時段、班別需求、排班與 Shift 整合管理。",
        contentPadding = contentPadding,
    ) {
        if (appState.employees.isEmpty()) {
            AddEmployeeCard(onAddEmployee)
        } else {
            AddEmployeeCard(onAddEmployee)
            SectionCard(title = "可上班時段管理") {
                appState.days.forEach { day ->
                    Text(day, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ShiftBlock.entries.forEach { block ->
                            val selected = appState.availabilitySlots.any {
                                it.employeeId == appState.viewerUserId && it.day == day && it.block == block
                            }
                            SelectablePill(
                                label = block.label,
                                selected = selected,
                                modifier = Modifier.weight(1f),
                                onClick = { onToggle(day, block) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        AddShiftRequirementCard(days = appState.days, onAddShift = onAddShift)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("缺人", shortage.toString(), "尚未填補人數", Modifier.weight(1f))
            MetricCard("已排班", appState.assignments.size.toString(), "目前有效指派", Modifier.weight(1f))
        }

        SectionCard(title = "人力熱點圖") {
            if (heatCells.isEmpty()) EmptyHint("新增班別需求後，這裡會顯示缺人/剛好/充足狀態。")
            appState.days.forEach { day ->
                val dayCells = heatCells.filter { it.shift.day == day }
                if (dayCells.isNotEmpty()) {
                    Text(day, style = MaterialTheme.typography.labelLarge)
                    HeatmapStrip(heatCells = dayCells)
                }
            }
        }

        CompactAction(
            "產生推薦班表",
            Modifier.fillMaxWidth(),
            enabled = appState.employees.isNotEmpty() && appState.shiftRequirements.isNotEmpty(),
            onClick = onGenerate,
        )
        if (messages.isNotEmpty()) {
            SectionCard(title = "推薦原因") {
                messages.forEach { message ->
                    Text("• $message", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        SectionCard(title = "班表總覽") {
            if (appState.shiftRequirements.isEmpty()) {
                EmptyHint("尚未建立班別需求。")
            } else {
                appState.days.forEach { day ->
                    val shifts = appState.shiftRequirements.filter { it.day == day }
                    if (shifts.isNotEmpty()) {
                        Text(day, style = MaterialTheme.typography.titleMedium)
                        shifts.forEach { shift ->
                            val assignments = assignedByShift[shift.id].orEmpty()
                            val assignedNames = assignments.map { appState.employeeName(it.employeeId) }
                            ShiftLine(
                                shift = shift,
                                note = if (assignedNames.isEmpty()) "尚缺 ${shift.requiredCount} 人" else assignedNames.joinToString("、"),
                                status = if (assignments.size < shift.requiredCount) HeatStatus.Shortage else HeatStatus.Balanced,
                            )
                            if (assignments.any { it.employeeId == appState.viewerUserId }) {
                                OutlinedButton(onClick = { onCreateSwap(shift) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("發布這班的 Shift 請求")
                                }
                            }
                        }
                    }
                }
            }
        }

        SectionCard(title = "Shift 市場") {
            Text("公司 Shift 需求", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (appState.shiftRequirements.isEmpty()) {
                EmptyHint("目前沒有公司 Shift 需求。")
            } else {
                appState.shiftRequirements.forEach { shift ->
                    val assignments = assignedByShift[shift.id].orEmpty()
                    val assignedNames = assignments.map { appState.employeeName(it.employeeId) }
                    val shortage = (shift.requiredCount - assignments.size).coerceAtLeast(0)
                    OutlinedCard(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ShiftLine(
                                shift = shift,
                                note = "${shift.day} ${shift.timeRangeLabel()} · ${shift.role}",
                                status = if (shortage > 0) HeatStatus.Shortage else HeatStatus.Balanced,
                            )
                            InfoRow("需求人數", shift.requiredCount.toString())
                            InfoRow("已排人數", assignments.size.toString())
                            InfoRow("尚缺人數", shortage.toString())
                            InfoRow("已排員工", if (assignedNames.isEmpty()) "尚未分配" else assignedNames.joinToString("、"))
                            if (assignments.any { it.employeeId == appState.viewerUserId }) {
                                OutlinedButton(onClick = { onCreateSwap(shift) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("發布這班的 Shift 請求")
                                }
                            } else {
                                EmptyHint(if (shortage > 0) "可到 Shift 頁接受這個需求。" else "這個 Shift 需求已滿。")
                            }
                        }
                    }
                }
            }

            Text("員工 Shift 請求", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (appState.swapRequests.isEmpty()) {
                EmptyHint("目前沒有員工 Shift 請求。")
            } else {
                appState.swapRequests.forEach { request ->
                    val shift = appState.shiftRequirements.firstOrNull { it.id == request.shiftId }
                    OutlinedCard(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("${appState.employeeName(request.requesterEmployeeId)} 的 Shift 請求", style = MaterialTheme.typography.titleSmall)
                            if (shift != null) ShiftLine(shift = shift, note = request.reason)
                            InfoRow("補貼", "$${request.subsidy}")
                            InfoRow("狀態", request.status.label)
                            InfoRow("接班者", request.accepterEmployeeId?.let { appState.employeeName(it) } ?: "尚未有人接")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                CompactAction("我要接班", Modifier.weight(1f), enabled = request.status == SwapStatus.Open) { onClaim(request) }
                                CompactAction("店長核准", Modifier.weight(1f), enabled = request.status == SwapStatus.Claimed) { onApprove(request) }
                            }
                            OutlinedButton(
                                onClick = { onCancel(request) },
                                enabled = request.status == SwapStatus.Open || request.status == SwapStatus.Claimed,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftHeroThemeTokens.colors.destructive),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("取消請求")
                            }
                        }
                    }
                }
            }
        }
    }
}
