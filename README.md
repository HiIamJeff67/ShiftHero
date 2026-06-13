<p align="center">
  <img src="app/src/main/res/drawable-nodpi/shift_hero_dark_logo.png" alt="ShiftHero" width="560">
</p>

<h1 align="center">ShiftHero</h1>

<p align="center">
  給門市團隊使用的 Android 智慧排班與代班管理工具
</p>

## 專案簡介

ShiftHero 是以單一門市與兼職團隊為主要情境的 Android App。系統將員工可上班時間、門市人力需求、班表、代班流程與公司成員管理集中在同一個操作介面，降低店長人工整理班表與協調代班的成本。

本 repository 是 **Android client**。帳號、公司、排班與代班等跨裝置資料由後端 API 管理；PostgreSQL 與 Redis 等基礎設施只能由後端服務存取，不應將資料庫連線字串或密碼放入 Android App。

## 核心功能

- 帳號註冊、登入、登出與本機 session 保存
- 使用者資料查看與顯示名稱更新
- 公司切換、公司資料管理與成員角色區分
- 員工申請加入公司，店長審核或拒絕申請
- 建立門市班別需求與調整班別時段
- 員工提交及刪除自己的可上班時段
- 依人力需求產生排班結果
- 員工主動認領開放班別
- 發布、認領、核准與取消代班請求
- 取得一般或串流式排班分析建議
- 調整換班規則、工時限制、休息時間與時區
- 支援系統、淺色及深色主題
- 在本機 Docker API 與 Render API 之間切換

## 使用角色

| 角色 | 主要操作 |
| --- | --- |
| Manager | 管理公司資料與成員、審核加入申請、建立人力需求、產生班表、核准代班、調整排班設定 |
| Staff | 提交可上班時間、查看與認領班別、建立或認領代班請求、查看個人資料 |

實際權限仍由後端依 access token 與公司成員角色驗證，前端畫面限制不能取代後端授權。

## 主要流程

```text
註冊或登入
    |
    +-- 選擇已加入的公司
    |       |
    |       +-- Manager 建立班別需求
    |       +-- Staff 提交可上班時段
    |       +-- Manager 產生或調整班表
    |       +-- 成員查看、認領班別或提出代班
    |       `-- Manager 核准代班結果
    |
    `-- 尚未加入公司
            |
            +-- 提交公司加入申請
            `-- 等待 Manager 審核
```

## 技術架構

- Kotlin
- Jetpack Compose + Material 3
- MVVM
- Android `ViewModel`
- Kotlin Coroutines
- `SQLiteOpenHelper`
- 自製 HTTP API client
- JUnit 4
- Gradle Version Catalog

目前畫面狀態與操作主要由 `SchedulePlannerViewModel` 統整，網路請求經由 `BackendApiClient` 傳送。SQLite 僅保存本機設定、登入 session 與部分加入公司申請快取；排班業務資料的 source of truth 是後端。

```text
Compose Pages / Components
          |
          v
SchedulePlannerViewModel
          |
          +-------------------------+
          |                         |
          v                         v
BackendApiClient              Local Repositories
          |                         |
          v                         v
ShiftHero Backend API         AppDatabaseHelper
          |                         |
          v                         v
Backend-owned database        shifthero.db
```

## 專案結構

```text
ShiftHero/
|-- README.md                         # 專案總覽與啟動說明
|-- build.gradle.kts                  # Root Gradle 設定
|-- settings.gradle.kts               # Module 與 repository 設定
|-- gradle.properties                 # Gradle 專案參數
|-- gradle/
|   |-- libs.versions.toml            # 套件與 plugin 版本
|   `-- wrapper/                      # Gradle Wrapper
|-- app/
|   |-- build.gradle.kts              # Android app module 設定
|   |-- proguard-rules.pro
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/example/shifthero/
|       |   |   |-- MainActivity.kt  # Compose app 入口與主畫面切換
|       |   |   |-- components/      # 可重用 UI 元件
|       |   |   |-- core/
|       |   |   |   |-- database/    # SQLiteOpenHelper 與 cursor helper
|       |   |   |   |-- model/       # App、排班、薪資與代班資料模型
|       |   |   |   |-- network/     # API base URL 與 HTTP client
|       |   |   |   `-- repository/  # 本機設定、session 與快取存取
|       |   |   |-- domain/
|       |   |   |   |-- payroll/     # 薪資計算規則
|       |   |   |   |-- scheduling/  # 排班與缺工計算邏輯
|       |   |   |   `-- swap/        # 代班狀態流程
|       |   |   |-- feature/
|       |   |   |   `-- SchedulePlannerViewModel.kt
|       |   |   |-- pages/           # 登入、首頁、管理、代班、使用者與設定頁
|       |   |   `-- ui/theme/        # 色彩、字型與 Compose theme
|       |   `-- res/
|       |       |-- drawable-nodpi/   # ShiftHero logo
|       |       |-- mipmap-*/         # App launcher icons
|       |       `-- values/           # Theme、顏色與字串資源
|       |-- test/                     # JVM unit tests
|       `-- androidTest/              # Android instrumentation tests
`-- docs/
    |-- DEVELOPMENT.md                # 開發與分層規範
    `-- contracts/
        |-- company-join-requests/    # 公司加入申請 API contract
        `-- schedule/                 # 排班 API、OpenAPI、範例與 Postman collection
```

## 畫面分區

登入後的底部導覽包含：

- **Dashboard**：公司與排班狀態摘要、新手操作提示。
- **Management**：公司、成員、班別需求、可上班時間及排班管理。
- **Shift**：可認領班別與代班中心。
- **User**：使用者資料與顯示名稱。
- **Setting**：主題、API 環境與公司排班規則。

## 環境需求

- Android Studio
- Android SDK 36
- Android 12 / API 31 以上的模擬器或實機
- 專案內附的 Gradle Wrapper
- 可連線的 ShiftHero backend

## 開始使用

1. Clone repository：

   ```bash
   git clone <repository-url>
   cd ShiftHero
   ```

2. 使用 Android Studio 開啟專案並等待 Gradle sync 完成。

3. 建立或啟動 API：

   - Android Emulator 連接本機 Docker backend 時，預設使用：
     `http://10.0.2.2/api/development/v1`
   - Render 環境使用：
     `https://shift-hero-backend.onrender.com/api/development/v1`

4. Build 專案：

   ```bash
   ./gradlew build
   ```

5. 執行 JVM unit tests：

   ```bash
   ./gradlew test
   ```

6. 從 Android Studio 將 `app` 執行到模擬器或實機。

## API 與資料責任

後端 API 負責：

- 身分驗證與 token 更新
- 使用者與公司資料
- 公司成員與加入申請
- 班別需求
- 員工可上班時段
- 排班結果
- 代班請求
- 排班設定與分析建議

Android SQLite 目前保存：

- `settings`：API URL、主題與本機偏好
- `user_session`：登入 session 與 token
- `company_join_requests`：加入公司申請的本機快取

排班資料不可只寫入本機 SQLite，否則其他裝置與公司成員無法看到一致結果。

## API 文件

- [排班前端整合指南](docs/contracts/schedule/frontend-integration-guide.md)
- [排班 API contract](docs/contracts/schedule/contract.md)
- [排班 OpenAPI](docs/contracts/schedule/openapi.v1.yaml)
- [公司加入申請 contract](docs/contracts/company-join-requests/contract.md)
- [公司加入申請 OpenAPI](docs/contracts/company-join-requests/openapi.v1.yaml)
- [開發指南](docs/DEVELOPMENT.md)

## 安全注意事項

- 不要在 Kotlin、README、測試資料或匯出的對話紀錄中加入資料庫 URI、密碼、API token 或私鑰。
- Android App 只需要後端 API base URL，不需要 PostgreSQL 或 Redis 連線資訊。
- `.env`、`.env.*` 與 Codex 對話匯出目錄已由 root `.gitignore` 排除。
- `.env.example` 只能保留變數名稱與無敏感性的範例值。
- 已經 push 到 GitHub 的 secret 必須立即撤銷或重設；刪除檔案本身不能使舊憑證恢復安全。

## 開發注意事項

- UI 應透過 `SchedulePlannerViewModel` 呼叫資料操作，不要在 Composable 直接存取 SQLite 或 API。
- 新增 domain 規則時，優先保持純 Kotlin，並補上 JVM unit test。
- API 欄位與權限應以 `docs/contracts/` 內的文件為準。
- 修改 SQLite schema 時，需同步提高 `DATABASE_VERSION` 並設計 migration。
- 目前 `AppDatabaseHelper.onUpgrade()` 會重建本機資料表，正式發佈前應改為保留資料的 migration。
- `android:usesCleartextTraffic="true"` 是為了本機 HTTP 開發環境，正式版本應評估改用 Network Security Config 限縮範圍。

## 目前限制

- 此 repository 不包含 backend 原始碼或資料庫部署設定。
- Google OAuth client 方法已存在，但完整登入介面與平台設定仍需另外整合。
- 自動化測試目前以排班 domain unit test 與基礎 Android 測試骨架為主，覆蓋率仍有限。
- `SchedulePlannerViewModel` 負責的功能較多，後續可依 authentication、company、schedule 與 swap 拆分。
- 正式 release 尚需補強 token 儲存、資料庫 migration、網路錯誤處理與 release minification。

## License

目前尚未指定開源授權。除非 repository owner 另行聲明，否則不代表允許複製、修改或散布。
