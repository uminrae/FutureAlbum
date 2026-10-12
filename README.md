# 未来的相册 (FutureAlbum)

基于现代 Jetpack Compose 与 Material Design 3 构建的高性能、离线隐私优先的智能手机相册管理应用。

## 核心特性

- **极致性能与时间轴预览**：针对海量本地照片优化，支持高帧率平滑滚动、毫秒级缩略图预加载与两级缓存（内存 + 磁盘）。
- **智能标签与分类管理**：支持自定义分类、多标签标记、批量打标、拼音快速检索与多维筛选。
- **全能图片与胶片编辑**：
  - 复古点阵液晶时间戳渲染（Retro Dot-Matrix Timestamp）
  - 经典胶片色彩风格与滤镜引擎
  - 定制相机水印与边框样式
- **EXIF 元数据与信息保护**：深度解析拍摄参数、相机型号、光圈快门、地理位置等，并支持 EXIF 隐私伪装与擦除。
- **离线 OCR 文字识别**：集成 Google ML Kit 离线文字识别，安全快速识别照片中的中英文文本。
- **媒体增强播放**：基于 AndroidX Media3 ExoPlayer，支持超高性能视频硬解播放与悬浮预览。
- **隐私与回收站**：安全删除与摇一摇撤销机制，支持自动后台维护（WorkManager）。

## 技术栈

- **Language**: Kotlin 2.0+ (JVM 21)
- **UI Framework**: Jetpack Compose, Material 3
- **Architecture**: MVVM, Jetpack Navigation, Coroutines & Flow
- **Local DB**: Room + KSP
- **Image Loading**: Coil 3 (支持 GIF, Video Frame)
- **Video Playback**: AndroidX Media3 ExoPlayer
- **Machine Learning**: Google ML Kit (Text Recognition)
- **System**: compileSdk 35 / minSdk 26 / targetSdk 35

## 编译与运行

1. 克隆代码库：
   ```bash
   git clone <REPO_URL>
   ```
2. 使用 Android Studio 打开项目。
3. 确保本地配置好 JDK 21 与 Android SDK (API 35)。
4. 构建 Release / Debug APK：
   ```bash
   ./gradlew assembleDebug
   ```
