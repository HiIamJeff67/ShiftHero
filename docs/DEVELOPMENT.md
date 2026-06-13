# ShiftHero Development Guide

這份文件給接手開發的組員使用。ShiftHero 目前定位為正式本地 Android App，不是 demo-only prototype。所有功能都應以可持久化、可維護、可擴充為前提開發。

## 專案目標

ShiftHero 是給單店兼職團隊使用的智慧排班 App。第一階段採用「單店共用端點」模式：員工與店長使用同一台裝置或同一份本地 SQLite 資料，不做登入、不做雲端同步。

核心流程：

1. 店長建立員工與班別需求。
2. 員工選擇自己的名字後提交可上班時段。
3. 店長查看人力熱點圖並產生推薦班表。
4. 員工查看班表、預估薪資，必要時發布代班請求。
5. 同事接班後由店長核准。

## 技術架構

目前採用 MVVM + table-specific Repository + SQLiteOpenHelper。

```text
MainActivity / Compose UI
    ↓
SchedulePlannerViewModel
    ↓
Domain services
    ├── PayrollCalculator
    ├── SchedulingEngine
    └── SwapWorkflow
    ↓
Table-specific repositories
    ├── EmployeeRepository
    ├── ShiftRequirementRepository
    ├── AvailabilityRepository
    ├── ShiftAssignmentRepository
    ├── SwapRequestRepository
    └── SettingsRepository
    ↓
AppDatabaseHelper / shifthero.db
```

目前資料存在 Android 本地 SQLite。不要新增假資料或 seed data 來假裝流程可用；App 必須能從空資料庫開始建立員工、班別、可上班時段與班表。

## 目錄說明

```text
app/src/main/java/com/example/shifthero
├── MainActivity.kt
├── core
│   ├── database
│   │   ├── AppDatabaseHelper.kt
│   │   └── DatabaseCursor.kt
│   ├── model
│   │   ├── AppState.kt
│   │   ├── Employee.kt
│   │   ├── NavigationItem.kt
│   │   ├── Payroll.kt
│   │   ├── Schedule.kt
│   │   └── Swap.kt
│   └── repository
│       ├── AvailabilityRepository.kt
│       ├── EmployeeRepository.kt
│       ├── SettingsRepository.kt
│       ├── ShiftAssignmentRepository.kt
│       ├── ShiftRequirementRepository.kt
│       └── SwapRequestRepository.kt
├── domain
│   ├── payroll
│   ├── scheduling
│   └── swap
├── feature
│   └── SchedulePlannerViewModel.kt
└── ui/theme
```

## 命名與分層規範

- 不要新增 `ShiftHeroModel`、`ShiftHeroRepository`、`AppRepository` 這種總包式命名。
- Model 依資料或 domain 概念拆分，例如 `Employee`、`ShiftRequirement`、`SwapRequest`。
- Repository 依資料表或資料邊界拆分，例如 `EmployeeRepository`、`SwapRequestRepository`。
- ViewModel 負責聚合 UI state 與處理使用者 action，不直接在 Composable 裡寫資料庫操作。
- Domain service 必須是純 Kotlin 邏輯，不依賴 Android UI 或 SQLite。
- UI Composable 只接收 state 與 callback，不直接建立 repository 或 database helper。

## SQLite 注意事項

資料庫入口是 `AppDatabaseHelper`，目前資料庫名稱是 `shifthero.db`。

目前 schema：

- `settings`：店名、目前選取員工、薪資規則、班表發布狀態
- `employees`：員工資料
- `availability`：員工可上班時段
- `shifts`：店長建立的班別需求
- `assignments`：排班結果
- `swaps`：代班請求

修改 schema 時：

1. 必須調整 `DATABASE_VERSION`。
2. 不要只改 `CREATE TABLE`，也要設計 `onUpgrade`。
3. 開發時如果遇到舊 schema 啟動錯誤，可先清除 App storage 或 uninstall/reinstall。
4. 正式資料遷移不可直接 drop table；目前 `onUpgrade` 還是開發期簡化版本，之後要改成 migration。

## UI 與 Theme 規範

- Theme 使用 `ui/theme` 內的黃黑工業風 token。
- 新 UI 優先使用 `MaterialTheme.colorScheme` 與 `ShiftHeroThemeTokens.colors`。
- 刪除、取消、危險操作使用 destructive red。
- 卡片圓角維持 8dp，不要做過度圓角或行銷 landing page 風格。
- 這是工作型工具 App，畫面應該密集但清楚，優先讓店長與員工快速完成操作。

## 開發流程

常用指令：

```bash
./gradlew build
```

每次重要修改後至少跑一次 build。若改 domain service，應補 unit test。若改 SQLite schema，必須手動測試新安裝與舊資料庫升級兩種情境。

建議開發順序：

1. 先補完整 CRUD：編輯/刪除員工、編輯/刪除班別需求。
2. 將 `SchedulePlannerViewModel` 的 Compose state 改成 `StateFlow`。
3. 補操作成功/錯誤提示與確認對話框。
4. 補 PDF 匯出週班表。
5. 補設定頁：店名、薪資規則、月薪目標。
6. 視時間再考慮 Navigation Compose、Room、DataStore 或後端 API。

## 已知限制

- 目前沒有登入與權限系統。
- 目前沒有雲端同步。
- 目前 repository 是同步 SQLite 操作；資料量小可以接受，之後可移到 coroutine dispatcher。
- 目前 `onUpgrade` 是開發期簡化作法，正式發佈前要改成保留資料的 migration。
- 目前 UI 多數仍在 `MainActivity.kt`，後續應拆成 feature package，例如 `feature/manager`、`feature/schedule`、`feature/availability`。

## 接手建議

接手時先跑：

```bash
./gradlew build
```

然後在模擬器上從空資料庫測一次：

1. 新增員工。
2. 到店長頁新增班別需求。
3. 到有空頁提交可上班時段。
4. 回店長頁產生推薦班表。
5. 到班表頁確認排班。
6. 從自己的班發布代班請求。
7. 到代班頁接班並核准。

這條流程不能壞；後續所有功能都應圍繞它擴充。
