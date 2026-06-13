package com.example.shifthero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.shifthero.components.bottomtabs.BottomTabs
import com.example.shifthero.core.model.AppThemeMode
import com.example.shifthero.core.model.NavigationItem
import com.example.shifthero.feature.SchedulePlannerViewModel
import com.example.shifthero.pages.AuthEntryScreen
import com.example.shifthero.pages.DashboardScreen
import com.example.shifthero.pages.DashboardTutorialOverlay
import com.example.shifthero.pages.ManagementSplitScreen
import com.example.shifthero.pages.SettingScreen
import com.example.shifthero.pages.SwapCenterScreen
import com.example.shifthero.pages.TUTORIAL_STEPS
import com.example.shifthero.pages.UserProfileScreen
import com.example.shifthero.ui.theme.ShiftHeroTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SchedulePlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode = viewModel.uiState.themeMode
            val darkTheme = when (themeMode) {
                AppThemeMode.System -> isSystemInDarkTheme()
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }
            ShiftHeroTheme(darkTheme = darkTheme) {
                SchedulePlannerApp(viewModel)
            }
        }
    }
}


@Composable
private fun SchedulePlannerApp(viewModel: SchedulePlannerViewModel) {
    val uiState = viewModel.uiState
    val appState = uiState.appState

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (uiState.isAuthenticated) {
                BottomTabs(selected = uiState.screen, onSelected = viewModel::selectScreen)
            }
        },
    ) { innerPadding ->
        if (!uiState.isAuthenticated) {
            AuthEntryScreen(
                contentPadding = innerPadding,
                isLoading = uiState.isAuthLoading,
                errorMessage = uiState.authErrorMessage,
                themeMode = uiState.themeMode,
                apiBaseUrl = uiState.apiBaseUrl,
                onLogin = viewModel::login,
                onRegister = viewModel::register,
                onThemeModeChanged = viewModel::setThemeMode,
                onApiBaseUrlChanged = viewModel::setApiBaseUrl,
            )
            return@Scaffold
        }

        when (uiState.screen) {
            NavigationItem.Dashboard -> Box {
                DashboardScreen(
                    appState = appState,
                    contentPadding = innerPadding,
                    onOpenManagement = { viewModel.selectScreen(NavigationItem.Management) },
                )
                if (uiState.showTutorialOverlay) {
                    DashboardTutorialOverlay(
                        stepIndex = uiState.tutorialStepIndex,
                        contentPadding = innerPadding,
                        onClose = viewModel::dismissTutorialOverlay,
                        onNext = { viewModel.showNextTutorialStep(TUTORIAL_STEPS.size) },
                        onPrevious = viewModel::showPreviousTutorialStep,
                    )
                }
            }

            NavigationItem.Management -> ManagementSplitScreen(
                appState = appState,
                messages = uiState.latestSuggestionMessages,
                companyRequestMessage = uiState.companyRequestMessage,
                scheduleInsight = uiState.scheduleInsight,
                contentPadding = innerPadding,
                onCompanySelected = viewModel::selectCompany,
                onSubmitJoinRequest = viewModel::submitJoinCompanyRequest,
                onCancelJoinRequest = viewModel::cancelJoinCompanyRequest,
                onRefreshJoinRequests = viewModel::refreshCompanyRequests,
                onLeaveCompany = viewModel::leaveCurrentCompany,
                onUpdateCompanyProfile = viewModel::updateCurrentCompanyProfile,
                onApproveJoinRequest = viewModel::approveJoinCompanyRequest,
                onRejectJoinRequest = viewModel::rejectJoinCompanyRequest,
                onUpdateShiftTime = viewModel::updateTimelineShiftTime,
                onAddShiftRequirement = viewModel::addMinuteShiftRequirement,
                onAddAvailabilitySlot = viewModel::addAvailabilitySlot,
                onDeleteAvailabilitySlot = viewModel::deleteAvailabilitySlot,
                onGenerate = viewModel::generateSchedule,
                onGenerateScheduleInsights = viewModel::generateScheduleInsights,
                onStreamScheduleInsights = viewModel::streamScheduleInsights,
                onCreateSwap = viewModel::createSwapRequest,
                onClaimShift = viewModel::claimShiftRequirement,
                onOpenSwapPage = { viewModel.selectScreen(NavigationItem.Swap) },
            )

            NavigationItem.Swap -> SwapCenterScreen(
                appState = appState,
                contentPadding = innerPadding,
                onAddShiftRequirement = viewModel::addMinuteShiftRequirement,
                onClaimShift = viewModel::claimShiftRequirement,
                onCreateSwap = viewModel::createSwapRequest,
                onClaim = viewModel::claimSwap,
                onApprove = viewModel::approveSwap,
                onCancel = viewModel::cancelSwap,
            )

            NavigationItem.User -> UserProfileScreen(
                contentPadding = innerPadding,
                profile = uiState.userProfile,
                isLoading = uiState.isUserProfileLoading,
                errorMessage = uiState.userProfileErrorMessage,
                onRefresh = { viewModel.loadUserProfile(forceRefresh = true) },
                onUpdateDisplayName = viewModel::updateMyDisplayName,
            )

            NavigationItem.Setting -> SettingScreen(
                appState = appState,
                contentPadding = innerPadding,
                themeMode = uiState.themeMode,
                apiBaseUrl = uiState.apiBaseUrl,
                onThemeModeChanged = viewModel::setThemeMode,
                onApiBaseUrlChanged = viewModel::setApiBaseUrl,
                onScheduleSettingsChanged = viewModel::updateScheduleSettings,
                onResetData = viewModel::resetAllData,
                onLogout = viewModel::logout,
            )
        }
    }
}
