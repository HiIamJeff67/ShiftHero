package com.example.shifthero.pages

import com.example.shifthero.components.inforow.InfoRow
import com.example.shifthero.components.screenframe.ScreenFrame
import com.example.shifthero.components.sectioncard.SectionCard
import com.example.shifthero.components.selectablepill.SelectablePill
import com.example.shifthero.components.shiftherologo.ShiftHeroLogo

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
internal fun SettingScreen(
    appState: AppState,
    contentPadding: PaddingValues,
    themeMode: AppThemeMode,
    apiBaseUrl: String,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onApiBaseUrlChanged: (String) -> Unit,
    onScheduleSettingsChanged: (Boolean, Int, Int, String) -> Unit,
    onResetData: () -> Unit,
    onLogout: () -> Unit,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var autoApproveSwaps by remember(appState.scheduleSettings.autoApproveSwaps) {
        mutableStateOf(appState.scheduleSettings.autoApproveSwaps)
    }
    var maxWeeklyHours by remember(appState.scheduleSettings.maxWeeklyHours) {
        mutableStateOf(appState.scheduleSettings.maxWeeklyHours.toString())
    }
    var minRestHours by remember(appState.scheduleSettings.minRestHours) {
        mutableStateOf(appState.scheduleSettings.minRestHours.toString())
    }
    var scheduleTimezone by remember(appState.scheduleSettings.timezone) {
        mutableStateOf(appState.scheduleSettings.timezone)
    }
    val canManageScheduleSettings = appState.viewerRoleInCompany == EmployeeRole.Manager

    ScreenFrame(
        title = "Setting",
        subtitle = "App 資訊、主題模式與資料維護設定。",
        contentPadding = contentPadding,
    ) {
        SectionCard(title = "軟體資訊") {
            ShiftHeroLogo(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            )
            InfoRow("App", "ShiftHero")
            InfoRow("資料來源", "Golang API + Local Session Cache")
            InfoRow("架構", "MVVM + Compose + API Client")
            InfoRow("店名", appState.storeName)
        }

        SectionCard(title = "主題模式") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AppThemeMode.entries.forEach { mode ->
                    SelectablePill(
                        label = when (mode) {
                            AppThemeMode.System -> "系統"
                            AppThemeMode.Light -> "淺色"
                            AppThemeMode.Dark -> "深色"
                        },
                        selected = themeMode == mode,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeModeChanged(mode) },
                    )
                }
            }
        }

        SectionCard(title = "排班設定") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = autoApproveSwaps,
                    onCheckedChange = { autoApproveSwaps = it },
                    enabled = canManageScheduleSettings,
                )
                Text("自動核准 Shift 請求")
            }
            Text(
                "設定會同步至後端。啟用後，同事接受換班請求時會立即轉移班次；工時與休息限制主要供 AI 排班分析使用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = maxWeeklyHours,
                    onValueChange = { maxWeeklyHours = it },
                    label = { Text("每週工時上限") },
                    enabled = canManageScheduleSettings,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = minRestHours,
                    onValueChange = { minRestHours = it },
                    label = { Text("最短休息時數") },
                    enabled = canManageScheduleSettings,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            OutlinedTextField(
                value = scheduleTimezone,
                onValueChange = { scheduleTimezone = it },
                label = { Text("時區") },
                enabled = canManageScheduleSettings,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (canManageScheduleSettings) {
                Button(
                    onClick = {
                        onScheduleSettingsChanged(
                            autoApproveSwaps,
                            maxWeeklyHours.toIntOrNull() ?: appState.scheduleSettings.maxWeeklyHours,
                            minRestHours.toIntOrNull() ?: appState.scheduleSettings.minRestHours,
                            scheduleTimezone,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("儲存排班設定")
                }
            } else {
                Text(
                    "只有 Manager 可以更新排班設定。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionCard(title = "API 連線") {
            InfoRow("目前 URL", apiBaseUrl)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SelectablePill(
                    label = "Docker",
                    selected = apiBaseUrl == ApiConfig.LOCAL_DOCKER_API_BASE_URL,
                    modifier = Modifier.weight(1f),
                    onClick = { onApiBaseUrlChanged(ApiConfig.LOCAL_DOCKER_API_BASE_URL) },
                )
                SelectablePill(
                    label = "Render",
                    selected = apiBaseUrl == ApiConfig.RENDER_API_BASE_URL,
                    modifier = Modifier.weight(1f),
                    onClick = { onApiBaseUrlChanged(ApiConfig.RENDER_API_BASE_URL) },
                )
            }
            Text(
                "Docker 指向 Android emulator 可連到宿主機 nginx 的 10.0.2.2；切換環境會清除本機登入狀態。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "資料維護") {
            Text(
                "重置會清空本機設定與快取，遠端資料由後端維護。",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftHeroThemeTokens.colors.destructive),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("重置本地資料")
            }
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftHeroThemeTokens.colors.destructive),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("登出")
            }
        }
    }

    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("確認重置資料") },
            text = { Text("這個操作無法復原。是否要刪除本地 SQLite 資料並重設 app？") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showResetDialog = false
                        onResetData()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShiftHeroThemeTokens.colors.destructive),
                ) { Text("確認重置") }
            },
            dismissButton = { OutlinedButton(onClick = { showResetDialog = false }) { Text("取消") } },
        )
    }
}
