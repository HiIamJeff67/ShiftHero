package com.example.shifthero.pages

import com.example.shifthero.components.emptyhint.EmptyHint
import com.example.shifthero.components.helpers.formatHours
import com.example.shifthero.components.metriccard.MetricCard
import com.example.shifthero.components.screenframe.ScreenFrame
import com.example.shifthero.components.sectioncard.SectionCard
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


internal data class TutorialStep(
    val title: String,
    val description: String,
    val highlight: String,
)

internal enum class ManagementTab(val label: String) {
    Team("Team"),
    Requests("Requests"),
    Schedule("Schedule"),
    Insights("Insights"),
}

internal enum class SwapTab(val label: String) {
    Manage("My Shift"),
    Overview("Overview"),
    Market("Market"),
}

internal enum class ShiftTimePickingTarget {
    Start,
    End,
}

internal val TUTORIAL_STEPS = listOf(
    TutorialStep(
        title = "歡迎使用 ShiftHero",
        description = "這是你的即時營運儀表板，會顯示缺班、薪資預估與排班健康度。",
        highlight = "先看 Dashboard 指標，快速掌握今天狀態。",
    ),
    TutorialStep(
        title = "第一步：建立員工",
        description = "前往 Management 頁新增第一位員工，並設定時薪與希望工時。",
        highlight = "建立後即可由後端依登入者與公司角色同步排班資料。",
    ),
    TutorialStep(
        title = "第二步：建立班別需求",
        description = "新增每天需要的人數與時段，系統會開始產生缺班統計與熱點。",
        highlight = "完成後可一鍵產生推薦班表與發布。",
    ),
    TutorialStep(
        title = "第三步：提交有空與 Shift",
        description = "員工提交可上班時段後，班表與 Shift 市場就會有完整流程資料。",
        highlight = "建立資料後，教學浮層會自動不再顯示。",
    ),
)

@Composable
internal fun DashboardScreen(
    appState: AppState,
    contentPadding: PaddingValues,
    onOpenManagement: () -> Unit,
) {
    val viewerEmployee = appState.employees.firstOrNull { it.id == appState.viewerUserId }
    val payroll = viewerEmployee?.let { employee ->
        PayrollCalculator.calculate(employee, appState.shiftRequirements, appState.assignments, appState.swapRequests, appState.wageRule)
    } ?: PayrollSummary(0.0, 0, 0, 0, appState.wageRule.monthlyTargetIncome)
    val shortageCount = SchedulingEngine.totalShortage(appState.shiftRequirements, appState.assignments)
    val nextShift = viewerEmployee?.let { employee ->
        appState.assignments
            .firstOrNull { it.employeeId == employee.id }
            ?.let { assignment -> appState.shiftRequirements.firstOrNull { it.id == assignment.shiftId } }
    }
    val assignedByShift = appState.assignments.groupBy { it.shiftId }
    val dailyAssignedHours = appState.days.map { day ->
        val shifts = appState.shiftRequirements.filter { it.day == day }
        shifts.sumOf { shift ->
            val assignedCount = assignedByShift[shift.id].orEmpty().size
            shift.durationHours * assignedCount
        }
    }
    val roleDistribution = appState.shiftRequirements
        .groupBy { it.role.ifBlank { "未分類" } }
        .mapValues { (_, shifts) -> shifts.sumOf { it.requiredCount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    val openSwaps = appState.swapRequests.count { it.status == SwapStatus.Open }
    ScreenFrame(
        title = appState.storeName,
        subtitle = viewerEmployee?.let { "營運總覽 · ${it.name}" } ?: "營運總覽",
        contentPadding = contentPadding,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("預估薪資", "$${payroll.estimatedPay}", "本月已排 ${payroll.totalHours.formatHours()} 小時", Modifier.weight(1f))
            MetricCard("缺口人數", shortageCount.toString(), "依目前班表計算", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("員工數", appState.employees.size.toString(), "活躍人力", Modifier.weight(1f))
            MetricCard("待處理 Shift", openSwaps.toString(), "開放中的員工請求", Modifier.weight(1f))
        }
        SectionCard(title = "重點班次", action = "前往管理頁", onAction = onOpenManagement) {
            if (nextShift == null) {
                EmptyHint("尚未排入班表，先到 Management 建立員工與班別需求。")
            } else {
                ShiftLine(shift = nextShift, note = "已指派班次")
            }
        }
        SectionCard(title = "人力投入趨勢", action = "Analytics") {
            if (dailyAssignedHours.all { it <= 0.0 }) {
                EmptyHint("尚無排班資料，建立班別並產生推薦班表後會顯示趨勢圖。")
            } else {
                TrendLineChart(
                    labels = appState.days.map { it.substringAfter(" ") },
                    values = dailyAssignedHours,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                )
            }
        }
        SectionCard(title = "職位需求分布", action = "Role Mix") {
            if (roleDistribution.isEmpty()) {
                EmptyHint("尚無班別需求資料。")
            } else {
                RolePieChart(
                    entries = roleDistribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }
        }
    }
}

@Composable
internal fun DashboardTutorialOverlay(
    stepIndex: Int,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val safeIndex = stepIndex.coerceIn(0, TUTORIAL_STEPS.lastIndex)
    val step = TUTORIAL_STEPS[safeIndex]
    val isLastStep = safeIndex == TUTORIAL_STEPS.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(ShiftHeroThemeTokens.colors.scrimStrong),
        contentAlignment = Alignment.TopCenter,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = ShiftHeroThemeTokens.colors.card,
                contentColor = ShiftHeroThemeTokens.colors.onCard,
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "新手教學 ${safeIndex + 1}/${TUTORIAL_STEPS.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (!isLastStep) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ShiftHeroThemeTokens.colors.cardMuted,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(onClick = onClose),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, contentDescription = "關閉教學", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Text(step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(step.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ShiftHeroThemeTokens.colors.cardMuted,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(step.highlight, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPrevious,
                        enabled = safeIndex > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("上一步")
                    }
                    Button(
                        onClick = onNext,
                        enabled = !isLastStep,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("下一步")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }

                if (isLastStep) {
                    Button(
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("完成並關閉")
                    }
                }
            }
        }
    }
}

@Composable
internal fun TrendLineChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    val points = values.map { it.toFloat() }
    val maxValue = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val minValue = points.minOrNull() ?: 0f
    val range = (maxValue - minValue).coerceAtLeast(1f)
    val lineColor = MaterialTheme.colorScheme.primary
    val dividerColor = ShiftHeroThemeTokens.colors.divider

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val left = 8.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 10.dp.toPx()

            drawLine(
                color = dividerColor,
                start = Offset(left, bottom),
                end = Offset(right, bottom),
                strokeWidth = 2.dp.toPx(),
            )

            if (points.size > 1) {
                val stepX = (right - left) / (points.size - 1)
                val path = Path()
                points.forEachIndexed { index, value ->
                    val x = left + (index * stepX)
                    val normalized = (value - minValue) / range
                    val y = bottom - normalized * (bottom - top)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            labels.take(values.size).forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun RolePieChart(
    entries: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    val total = entries.sumOf { it.second }.coerceAtLeast(1)
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        ShiftHeroThemeTokens.colors.success,
        ShiftHeroThemeTokens.colors.swap,
        MaterialTheme.colorScheme.tertiary,
        ShiftHeroThemeTokens.colors.destructive,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            val diameter = minOf(size.width, size.height)
            val stroke = diameter * 0.24f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            var start = -90f
            entries.forEachIndexed { index, (_, value) ->
                val sweep = 360f * value / total
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEachIndexed { index, (role, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors[index % colors.size])
                        )
                        Text(role, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${(value * 100 / total)}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
