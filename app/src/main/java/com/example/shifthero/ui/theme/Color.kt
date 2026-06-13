package com.example.shifthero.ui.theme

import androidx.compose.ui.graphics.Color

val ShiftYellow = Color(0xFFFFC400)
val ShiftYellowDeep = Color(0xFFE2A900)
val ShiftYellowSoft = Color(0xFFFFE49A)
val IndustrialOrange = Color(0xFFFF8A1E)

val Carbon950 = Color(0xFF090A0C)
val Carbon900 = Color(0xFF101114)
val Carbon850 = Color(0xFF17191D)
val Carbon800 = Color(0xFF1F2227)
val Carbon700 = Color(0xFF2B3038)
val Carbon600 = Color(0xFF3C434D)
val Carbon500 = Color(0xFF5C6674)

val Steel200 = Color(0xFFD8DEE8)
val Steel300 = Color(0xFFC1C9D6)
val Steel500 = Color(0xFF7C8796)
val Steel700 = Color(0xFF48515F)

val Paper50 = Color(0xFFFFFCF3)
val Paper100 = Color(0xFFFFF5D8)
val Paper200 = Color(0xFFF4E5B6)

val DangerRed = Color(0xFFE5484D)
val DangerRedSoft = Color(0xFFFFD7D9)
val DangerRedDark = Color(0xFFFF8F93)

val SuccessGreen = Color(0xFF26A269)
val SuccessGreenSoft = Color(0xFFDFF6EA)
val SignalBlue = Color(0xFF3F7DE8)
val SignalBlueSoft = Color(0xFFDCE8FF)

data class ShiftHeroExtendedColors(
    val card: Color,
    val onCard: Color,
    val cardMuted: Color,
    val onCardMuted: Color,
    val elevated: Color,
    val onElevated: Color,
    val muted: Color,
    val onMuted: Color,
    val divider: Color,
    val scheduleLine: Color,
    val money: Color,
    val onMoney: Color,
    val swap: Color,
    val onSwap: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val destructive: Color,
    val onDestructive: Color,
    val destructiveContainer: Color,
    val onDestructiveContainer: Color,
    val heatShortage: Color,
    val heatBalanced: Color,
    val heatSurplus: Color,
    val scrimStrong: Color,
)

val LightShiftHeroExtendedColors = ShiftHeroExtendedColors(
    card = Color.White,
    onCard = Carbon950,
    cardMuted = Paper100,
    onCardMuted = Carbon700,
    elevated = Paper50,
    onElevated = Carbon950,
    muted = Paper200,
    onMuted = Steel700,
    divider = Color(0xFFE8D8A4),
    scheduleLine = ShiftYellowDeep,
    money = SuccessGreenSoft,
    onMoney = Color(0xFF0C4D31),
    swap = SignalBlueSoft,
    onSwap = Color(0xFF174A9E),
    success = SuccessGreen,
    onSuccess = Color.White,
    successContainer = SuccessGreenSoft,
    onSuccessContainer = Color(0xFF0C4D31),
    destructive = DangerRed,
    onDestructive = Color.White,
    destructiveContainer = DangerRedSoft,
    onDestructiveContainer = Color(0xFF8F1D23),
    heatShortage = DangerRed,
    heatBalanced = ShiftYellow,
    heatSurplus = SuccessGreen,
    scrimStrong = Color(0xB3000000),
)

val DarkShiftHeroExtendedColors = ShiftHeroExtendedColors(
    card = Carbon850,
    onCard = Paper50,
    cardMuted = Carbon800,
    onCardMuted = Steel300,
    elevated = Carbon700,
    onElevated = Paper50,
    muted = Carbon700,
    onMuted = Steel300,
    divider = Carbon600,
    scheduleLine = ShiftYellow,
    money = Color(0xFF123D2A),
    onMoney = Color(0xFFA8F0C7),
    swap = Color(0xFF17345F),
    onSwap = Color(0xFFB8D2FF),
    success = Color(0xFF5AD18F),
    onSuccess = Carbon950,
    successContainer = Color(0xFF123D2A),
    onSuccessContainer = Color(0xFFA8F0C7),
    destructive = DangerRedDark,
    onDestructive = Carbon950,
    destructiveContainer = Color(0xFF5E171B),
    onDestructiveContainer = Color(0xFFFFBFC2),
    heatShortage = DangerRedDark,
    heatBalanced = ShiftYellow,
    heatSurplus = Color(0xFF5AD18F),
    scrimStrong = Color(0xCC000000),
)
