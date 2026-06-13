package com.example.shifthero.pages

import com.example.shifthero.components.compactaction.CompactAction
import com.example.shifthero.components.emptyhint.EmptyHint
import com.example.shifthero.components.helpers.employeeName
import com.example.shifthero.components.inforow.InfoRow
import com.example.shifthero.components.metriccard.MetricCard
import com.example.shifthero.components.screenframe.ScreenFrame
import com.example.shifthero.components.sectioncard.SectionCard
import com.example.shifthero.components.selectablepill.SelectablePill
import com.example.shifthero.components.shiftline.ShiftLine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.shifthero.core.model.AppThemeMode
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
import com.example.shifthero.domain.payroll.PayrollCalculator
import com.example.shifthero.domain.scheduling.SchedulingEngine
import com.example.shifthero.feature.SchedulePlannerViewModel
import com.example.shifthero.feature.UserProfileUiState
import com.example.shifthero.ui.theme.ShiftHeroTheme
import com.example.shifthero.ui.theme.ShiftHeroThemeTokens
import kotlin.math.roundToInt


@Composable
internal fun SwapCenterScreen(
    appState: AppState,
    contentPadding: PaddingValues,
    onAddShiftRequirement: (String, Int, Int, EmployeeRole, Int) -> Unit,
    onClaimShift: (ShiftRequirement) -> Unit,
    onCreateSwap: (ShiftRequirement) -> Unit,
    onClaim: (SwapRequest) -> Unit,
    onApprove: (SwapRequest) -> Unit,
    onCancel: (SwapRequest) -> Unit,
) {
    var tab by remember { mutableStateOf(SwapTab.Manage) }
    val shiftsById = remember(appState.shiftRequirements) { appState.shiftRequirements.associateBy { it.id } }
    val assignmentsByShift = remember(appState.assignments) { appState.assignments.groupBy { it.shiftId } }
    val assignedShiftIds = remember(appState.assignments, appState.viewerUserId) {
        appState.assignments.filter { it.employeeId == appState.viewerUserId }.map { it.shiftId }.toSet()
    }
    val companyShiftNeeds = remember(appState.shiftRequirements) {
        appState.shiftRequirements.sortedWith(compareBy<ShiftRequirement> { it.day }.thenBy { it.startMinute })
    }
    val myAssignedShifts = remember(companyShiftNeeds, assignedShiftIds) {
        companyShiftNeeds.filter { it.id in assignedShiftIds }
    }
    val myShiftRequests = remember(appState.swapRequests, appState.viewerUserId) {
        appState.swapRequests.filter { it.requesterEmployeeId == appState.viewerUserId }
    }

    ScreenFrame(
        title = "Shift",
        subtitle = "公司班表需求與員工 Shift 請求集中管理。",
        contentPadding = contentPadding,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SwapTab.entries.forEach { item ->
                SelectablePill(
                    label = item.label,
                    selected = tab == item,
                    modifier = Modifier.weight(1f),
                    onClick = { tab = item },
                )
            }
        }

        when (tab) {
            SwapTab.Manage -> {
                MinuteShiftRequirementCard(
                    days = appState.days,
                    onCreate = onAddShiftRequirement,
                )
                SectionCard(title = "我的 Shift") {
                    if (myAssignedShifts.isEmpty() && myShiftRequests.isEmpty()) {
                        EmptyHint("你目前尚未分配班次，也沒有發布中的 Shift 請求。")
                    } else {
                        if (myAssignedShifts.isNotEmpty()) {
                            Text("已分配班次", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            myAssignedShifts.forEach { shift ->
                                ShiftLine(shift = shift, note = "${shift.day} ${shift.timeRangeLabel()}")
                                OutlinedButton(onClick = { onCreateSwap(shift) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("發布這班的 Shift 請求")
                                }
                            }
                        }
                        if (myShiftRequests.isNotEmpty()) {
                            Text("我發布的 Shift 請求", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            myShiftRequests.forEach { request ->
                                ShiftRequestCard(
                                    appState = appState,
                                    request = request,
                                    shift = shiftsById[request.shiftId],
                                    onClaim = onClaim,
                                    onApprove = onApprove,
                                    onCancel = onCancel,
                                )
                            }
                        }
                    }
                }
            }

            SwapTab.Overview -> {
                val openCount = appState.swapRequests.count { it.status == SwapStatus.Open }
                val shortageCount = SchedulingEngine.totalShortage(appState.shiftRequirements, appState.assignments)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Shift", appState.shiftRequirements.size.toString(), "公司需求", Modifier.weight(1f))
                    MetricCard("Open", openCount.toString(), "員工請求", Modifier.weight(1f))
                    MetricCard("Short", shortageCount.toString(), "尚缺人力", Modifier.weight(1f))
                }
                SectionCard(title = "公司 Shift 總覽") {
                    if (companyShiftNeeds.isEmpty()) {
                        EmptyHint("目前沒有公司 Shift 需求。")
                    } else {
                        companyShiftNeeds.forEach { shift ->
                            val assigned = assignmentsByShift[shift.id].orEmpty()
                            val shortage = (shift.requiredCount - assigned.size).coerceAtLeast(0)
                            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ShiftLine(
                                        shift = shift,
                                        note = "${shift.day} ${shift.timeRangeLabel()} · ${shift.role}",
                                        status = if (shortage > 0) HeatStatus.Shortage else HeatStatus.Balanced,
                                    )
                                    InfoRow("需求人數", shift.requiredCount.toString())
                                    InfoRow("已排人數", assigned.size.toString())
                                    InfoRow("尚缺人數", shortage.toString())
                                }
                            }
                        }
                    }
                }
                SectionCard(title = "員工 Shift 請求") {
                    if (appState.swapRequests.isEmpty()) {
                        EmptyHint("目前沒有員工 Shift 請求。")
                    } else {
                        appState.swapRequests.forEach { request ->
                            ShiftRequestCard(
                                appState = appState,
                                request = request,
                                shift = shiftsById[request.shiftId],
                                onClaim = onClaim,
                                onApprove = onApprove,
                                onCancel = onCancel,
                            )
                        }
                    }
                }
            }

            SwapTab.Market -> {
                SectionCard(title = "公司 Shift 需求") {
                    if (companyShiftNeeds.isEmpty()) {
                        EmptyHint("目前沒有公司 Shift 需求。")
                    } else {
                        companyShiftNeeds.forEach { shift ->
                            val assigned = assignmentsByShift[shift.id].orEmpty()
                            val assignedNames = assigned.map { appState.employeeName(it.employeeId) }
                            val shortage = (shift.requiredCount - assigned.size).coerceAtLeast(0)
                            OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShiftLine(
                                        shift = shift,
                                        note = "${shift.day} ${shift.timeRangeLabel()} · ${shift.role}",
                                        status = if (shortage > 0) HeatStatus.Shortage else HeatStatus.Balanced,
                                    )
                                    InfoRow("需求人數", shift.requiredCount.toString())
                                    InfoRow("已排人數", assigned.size.toString())
                                    InfoRow("尚缺人數", shortage.toString())
                                    InfoRow("已排員工", if (assignedNames.isEmpty()) "尚未分配" else assignedNames.joinToString("、"))
                                    when {
                                        shift.id in assignedShiftIds -> {
                                            OutlinedButton(onClick = { onCreateSwap(shift) }, modifier = Modifier.fillMaxWidth()) {
                                                Text("發布這班的 Shift 請求")
                                            }
                                        }
                                        shortage > 0 -> {
                                            Button(onClick = { onClaimShift(shift) }, modifier = Modifier.fillMaxWidth()) {
                                                Text("接受 Shift")
                                            }
                                        }
                                        else -> {
                                            EmptyHint("這個 Shift 需求已滿。")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                SectionCard(title = "員工 Shift 請求") {
                    val requests = appState.swapRequests.filter { it.requesterEmployeeId != appState.viewerUserId }
                    if (requests.isEmpty()) {
                        EmptyHint("目前沒有其他員工發布的 Shift 請求。")
                    } else {
                        requests.forEach { request ->
                            ShiftRequestCard(
                                appState = appState,
                                request = request,
                                shift = shiftsById[request.shiftId],
                                onClaim = onClaim,
                                onApprove = onApprove,
                                onCancel = onCancel,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftRequestCard(
    appState: AppState,
    request: SwapRequest,
    shift: ShiftRequirement?,
    onClaim: (SwapRequest) -> Unit,
    onApprove: (SwapRequest) -> Unit,
    onCancel: (SwapRequest) -> Unit,
) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${appState.employeeName(request.requesterEmployeeId)} 的 Shift 請求", style = MaterialTheme.typography.titleSmall)
            if (shift != null) {
                ShiftLine(shift = shift, note = "${shift.day} ${shift.timeRangeLabel()} · ${request.reason}")
            } else {
                Text(request.reason, style = MaterialTheme.typography.bodyMedium)
            }
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
