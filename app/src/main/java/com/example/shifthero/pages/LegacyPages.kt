package com.example.shifthero.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shifthero.components.addemployeecard.AddEmployeeCard
import com.example.shifthero.components.addshiftrequirementcard.AddShiftRequirementCard
import com.example.shifthero.components.compactaction.CompactAction
import com.example.shifthero.components.emptyhint.EmptyHint
import com.example.shifthero.components.heatmapstrip.HeatmapStrip
import com.example.shifthero.components.helpers.employeeName
import com.example.shifthero.components.helpers.formatHours
import com.example.shifthero.components.inforow.InfoRow
import com.example.shifthero.components.metriccard.MetricCard
import com.example.shifthero.components.progressbar.ProgressBar
import com.example.shifthero.components.screenframe.ScreenFrame
import com.example.shifthero.components.sectioncard.SectionCard
import com.example.shifthero.components.selectablepill.SelectablePill
import com.example.shifthero.components.shiftline.ShiftLine
import com.example.shifthero.core.model.AppState
import com.example.shifthero.core.model.HeatStatus
import com.example.shifthero.core.model.PayrollSummary
import com.example.shifthero.core.model.ShiftBlock
import com.example.shifthero.core.model.ShiftRequirement
import com.example.shifthero.core.model.SwapRequest
import com.example.shifthero.core.model.SwapStatus
import com.example.shifthero.domain.payroll.PayrollCalculator
import com.example.shifthero.domain.scheduling.SchedulingEngine
import com.example.shifthero.ui.theme.ShiftHeroThemeTokens


@Composable
internal fun AvailabilityScreen(
    appState: AppState,
    contentPadding: PaddingValues,
    onAddEmployee: (String, Int, Double) -> Unit,
    onToggle: (String, ShiftBlock) -> Unit,
) {
    ScreenFrame(
        title = "提交有空排班",
        subtitle = "提交自己的下週可上班時段，資料由後端排班 API 保存。",
        contentPadding = contentPadding,
    ) {
        if (appState.employees.isEmpty()) {
            AddEmployeeCard(onAddEmployee)
        } else {
            SectionCard(title = "下週可上班時段") {
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
    }
}

@Composable
internal fun ScheduleScreen(
    appState: AppState,
    contentPadding: PaddingValues,
    onCreateSwap: (ShiftRequirement) -> Unit,
) {
    val assignedByShift = appState.assignments.groupBy { it.shiftId }
    ScreenFrame(
        title = "班表瀏覽",
        subtitle = "目前有效班表與排班資訊。",
        contentPadding = contentPadding,
    ) {
        if (appState.shiftRequirements.isEmpty()) {
            EmptyHint("尚未建立班別需求，請到店長頁新增。")
        }
        appState.days.forEach { day ->
            val shifts = appState.shiftRequirements.filter { it.day == day }
            if (shifts.isNotEmpty()) {
                SectionCard(title = day) {
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
}

@Composable
internal fun PayrollScreen(appState: AppState, contentPadding: PaddingValues) {
    val viewerEmployee = appState.employees.firstOrNull { it.id == appState.viewerUserId }
    val payroll = viewerEmployee?.let { employee ->
        PayrollCalculator.calculate(employee, appState.shiftRequirements, appState.assignments, appState.swapRequests, appState.wageRule)
    } ?: PayrollSummary(0.0, 0, 0, 0, appState.wageRule.monthlyTargetIncome)
    val progress = (payroll.estimatedPay.toFloat() / payroll.targetIncome).coerceIn(0f, 1f)

    ScreenFrame(
        title = "薪資視覺化預報",
        subtitle = viewerEmployee?.let { "${it.name} · 時薪 $${it.hourlyRate}" } ?: "尚未取得你的員工資料。",
        contentPadding = contentPadding,
    ) {
        SectionCard(title = "本月預估") {
            Text("$${payroll.estimatedPay}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text("已排 ${payroll.totalHours.formatHours()} 小時 · 班表薪資 $${payroll.basePay} · Shift 補貼 $${payroll.bonusPay}")
            ProgressBar(progress)
            Text("距離目標 $${payroll.targetIncome} 還差 $${(payroll.targetIncome - payroll.estimatedPay).coerceAtLeast(0)}")
        }
        SectionCard(title = "薪資規則") {
            InfoRow("平日時薪", "$${viewerEmployee?.hourlyRate ?: appState.wageRule.baseHourlyRate}")
            InfoRow("假日倍率", "${appState.wageRule.holidayMultiplier}x")
            InfoRow("深夜倍率", "${appState.wageRule.nightMultiplier}x")
            InfoRow("加班倍率", "${appState.wageRule.overtimeMultiplier}x")
        }
    }
}

@Composable
internal fun SwapScreen(
    appState: AppState,
    contentPadding: PaddingValues,
    onClaim: (SwapRequest) -> Unit,
    onApprove: (SwapRequest) -> Unit,
    onCancel: (SwapRequest) -> Unit,
) {
    ScreenFrame(
        title = "Shift 互助市場",
        subtitle = "員工可以發布 Shift 請求，其他同事接班後由店長核准。",
        contentPadding = contentPadding,
    ) {
        if (appState.swapRequests.isEmpty()) {
            EmptyHint("目前沒有 Shift 請求。可到班表頁從自己的班次發布。")
        }
        appState.swapRequests.forEach { request ->
            val shift = appState.shiftRequirements.firstOrNull { it.id == request.shiftId }
            SectionCard(title = "${appState.employeeName(request.requesterEmployeeId)} 的請求") {
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

@Composable
internal fun ManagerScreen(
    appState: AppState,
    messages: List<String>,
    contentPadding: PaddingValues,
    onAddShift: (String, ShiftBlock, String, Int, Boolean) -> Unit,
    onGenerate: () -> Unit,
) {
    val heatCells = SchedulingEngine.heatmap(appState.shiftRequirements, appState.assignments)
    val shortage = SchedulingEngine.totalShortage(appState.shiftRequirements, appState.assignments)

    ScreenFrame(
        title = "店長管理",
        subtitle = "建立班別需求、查看全店提交結果、產生公平推薦班表。",
        contentPadding = contentPadding,
    ) {
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
    }
}
