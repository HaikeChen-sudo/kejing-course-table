# 课境 CourseTable

一个现代 Android 课程表骨架项目，采用 Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel、Flow 的主流原生架构。

## 当前框架

- 底部双模块：首页为「今日课程」，第二页为「本周课表」。
- 应用名为「课境」，已配置 adaptive icon、round icon 和 Android 13 themed icon。
- 「本周课表」支持切换当前学期，并通过右上角进入二级「学期管理」页面。
- 「学期管理」展示每个学期独立文件夹，支持切换当前学期。
- 「学期管理」支持删除课表文件夹，并会同步删除对应本地数据。
- 导入新课表时提供「覆盖当前」和「新建文件夹」两种策略。
- 预留教务系统同步接口 `AcademicPortalClient`，并拆出 `AcademicPortalAdapter` 适配层；当前由 `DemoAcademicPortalAdapter` 模拟远程拉取。
- 已加入华北理工大学手动登录入口，默认使用电脑版 WebView 打开 `http://xjw1.ncst.edu.cn/`，用户自行登录后可读取当前页面，后续接课表解析。
- 「读取并导入」会保存最近一次教务页面快照到应用私有目录，并尝试解析课表后自动新建课表文件夹。
- 已实现本地文件夹存储入口 `LocalScheduleFileStore`：每个学期一个文件夹，内部保存 `schedule.json`，并持久化当前选中学期。

## 主要目录

- `app/src/main/java/com/example/coursetable/domain/model`：课程、学期、导入模式等领域模型。
- `app/src/main/java/com/example/coursetable/data`：仓库、教务系统客户端、本地文件夹存储。
- `app/src/main/java/com/example/coursetable/ui`：导航、ViewModel、Compose 页面。
- `app/src/main/java/com/example/coursetable/core/design`：课程卡片配色等设计令牌。

## 下一步扩展建议

- 当前使用轻量 JSON 文件持久化；数据量变大后可替换为 Room，同时保留 `ScheduleRepository` 对 UI 的稳定接口。
- 用 Retrofit/Ktor 实现真实 `AcademicPortalClient`，处理登录、验证码、Cookie、课表解析。
- 增加学期周数切换、课节网格、冲突检测、课程详情页。
- 增加学校适配器层，例如 `AcademicPortalAdapter`，让不同学校的解析规则独立维护。
