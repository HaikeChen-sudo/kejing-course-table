# Coursia CourseTable

Coursia 是一个原生 Android 课程表应用，采用 Kotlin、Jetpack Compose、Material 3、Navigation Compose、ViewModel 和 Flow 构建。当前版本已经从骨架推进到可本地管理课表、手动添加课程、网页登录教务系统并解析导入课程表。

## 当前能力

- 首页「今日课程」按当前日期和教学周展示当天课程。
- 「本周课表」支持按周前后切换、回到当前周、切换星期、进入课程详情。
- 支持手动新增课程，填写星期、时间、地点、教师和周次范围。
- 「学期管理」按文件夹管理多个课表，支持选择、删除、覆盖导入和新建文件夹导入。
- 本地数据使用 `LocalScheduleFileStore` 保存，每个课表文件夹内维护一份 `schedule.json`，并持久化当前选中学期。
- 已内置华北理工大学和徐州工业职业技术学院入口，通过 WebView 登录教务系统后使用「读取并导入」抓取当前页面。
- 页面抓取会保存最近一次快照，并把 DOM 表格、页面文本和 HTML 一起交给解析器。
- 华北理工大学适配 `class_div` 课表块，支持跨节课程、当前周、教师、地点和多段周次解析。
- 徐州工业职业技术学院适配正方教务系统，支持 `kbgrid_table` 网格、`kblist_table` 列表、多课程同格、单双周、调课标记、校区/教室/教师提取。
- 正方 DOM 网格解析会优先使用页面表格中的真实节次时间，缺失时回退到默认节次时间。

## 主要目录

- `app/src/main/java/com/example/coursetable/domain/model`：课程、学期、导入模式等领域模型。
- `app/src/main/java/com/example/coursetable/data/remote`：学校配置、教务适配器、网页登录课表解析器。
- `app/src/main/java/com/example/coursetable/data/local`：本地课表文件夹和页面快照存储。
- `app/src/main/java/com/example/coursetable/data/repository`：课表仓库与导入/删除/切换逻辑。
- `app/src/main/java/com/example/coursetable/ui`：导航、ViewModel 和 Compose 页面。
- `app/src/main/java/com/example/coursetable/core/design`：颜色、形状和课程卡片配色。
- `app/src/test/java/com/example/coursetable/data/remote`：教务页面解析回归测试。

## 本地验证

默认系统 Java 如果过新，Gradle/Kotlin 可能无法解析版本号。建议使用 Android Studio 内置 JBR 运行：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew testDebugUnitTest
```

构建调试包：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew assembleDebug
```

生成的 APK 名称会包含版本号，例如 `kejing-v0.1.11-debug.apk`。

## 下一步扩展建议

- 将真实教务系统同步从 WebView 页面抓取逐步迁移到稳定 API 适配层。
- 增加冲突检测、节次网格视图和课程导出。
- 本地数据量变大后可替换为 Room，同时保留 `ScheduleRepository` 对 UI 的稳定接口。
- 为更多学校拆独立 `SchoolScheduleParser`，继续用真实页面样本补回归测试。
