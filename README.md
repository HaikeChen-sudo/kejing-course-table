# 课境 Coursia

一个原生 Android 课程表应用。它把今日课程、本周课表和随手备忘录放在同一处，适合上课前快速确认时间、地点和下一节课。

## 功能

- 「今日」按日期和教学周显示当天课程；课程卡片会显示距上课的剩余小时和分钟，课程进行中不显示提示，结束后显示「已结束」。
- 「本周」以时间网格查看整周课程，支持前后切周、回到当前周、进入课程详情，以及调整课程字体与卡片长度。
- 支持手动新增课程，填写星期、时间、地点、教师和周次范围。
- 「学期管理」可管理多个课表文件夹，支持选择、删除、覆盖导入和新建文件夹导入。
- 导入完成后会直接进入本周课表，并显示导入结果。
- 内置华北理工大学和徐州工业职业技术学院入口，可在 WebView 登录教务系统并读取当前页面导入课程。
- 提供备忘录，用于查看课程时随手记录待办和重要信息。
- 课表与备忘录均存储在本地，不依赖账号。

## 界面

应用采用冷哑光银、石墨黑和橙色点缀。今日、本周、学期管理与备忘录共用同一套背景和图标色调。

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

生成的 APK 名称会包含版本号，例如 `coursia-v0.1.34-debug.apk`。
