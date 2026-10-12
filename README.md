# 未来的相册 (FutureAlbum)

<p align="center">
  <strong>基于 Jetpack Compose 与 Material Design 3 构建的高性能、离线隐私优先的下一代智能手机相册</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.0+-purple.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg" alt="Compose">
  <img src="https://img.shields.io/badge/Target%20SDK-35-orange.svg" alt="Target SDK">
  <img src="https://img.shields.io/badge/Release-v8.12.0-brightgreen.svg" alt="Latest Release">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License">
</p>

---

## 📥 下载安装

最新正式版已发布，可直接下载体验：

- 🚀 **GitHub Releases 页面**：[前往 Releases 下载最新版本](https://github.com/uminrae/FutureAlbum/releases/latest)
- 📲 **APK 直链下载**：👉 [FutureAlbum-v8.12.0-Release.apk (194.56 MB)](https://github.com/uminrae/FutureAlbum/releases/download/v8.12.0/FutureAlbum-v8.12.0-Release.apk)

---

## 💡 设计理念与初衷

在智能手机时代，相册是记录个人生活最私密的数字空间。然而，许多商业相册软件日益臃肿、充斥开屏广告、过度索取权限，甚至频繁向云端上传用户的人脸与相册数据。

**「未来的相册」** 由此诞生 —— 我们坚持 **「离线优先、极致性能、纯粹无扰、绝对隐私」** 的核心理念：
1. **零云端回传**：所有数据（照片、标签、OCR 识别文字、修改记录）100% 保存在本机沙盒，不设任何远程用户追踪服务器。
2. **满帧流畅**：深入底层的针对性优化，彻底解决海量照片（数万张）滑动掉帧与内存抖动难题。
3. **智能且自控**：AI 赋能搜索与标签管理，但计算全部在端侧静默完成，将相册的掌控权完全交还给用户。

---

## ✨ 核心特性

### 1. 🚀 极致性能与满帧时间轴
- **双层缓存架构**：深度结合 LRU 内存缓存与高效本地缩略图磁盘缓存，实现毫秒级异步加载与图像预热。
- **超高性能时间轴**：针对 10,000+ 本地媒体深度调优，支持 60Hz/120Hz 高刷新率丝滑滚动。
- **沉浸式交互手势**：
  - 自由网格缩放（Pinch-to-Zoom）实时切换视图跨度；
  - 极速滑动条（FastScroller）支持按年/月快速时间定位；
  - 类似 Instagram 的长按窥视预览（Peek Overlay）与无缝拖拽多选（Drag Select）。

### 2. 🔍 离线 OCR 智能图文检索（以图搜字）
- **Google ML Kit 端侧离线模型**：集成离线中英文文字识别管线，APK 内置 C++ 离线识别动态库，无需连网即可识别。
- **后台静默低功耗调度**：基于 AndroidX WorkManager 分批次索引照片文本，防发热降频机制确保丝毫不影响日常操作。
- **搜索栏图文穿透检索**：在搜索框直接输入聊天截图中的句子、发票/收据上的商户名称、路牌或招牌字符，即使图片未经标记也能秒级定位！
- **拼音与多级标签检索**：支持标签全拼、拼音首字母检索，以及未打标照片一键过滤。

### 3. 🎞️ 复古胶片工坊与照片编辑
- **复古点阵液晶时间戳**：精准还原经典胶片机戳印（提供 LCD Dot、VCR OSD Mono、VHS 复古样式，支持绿字、橙字、红字及倾斜质感）。
- **专业 3D LUT 色彩引擎**：内置精细调校的胶片调色配置（包含 CPM35、经典旁轴底片色调等），即时预览无损色彩渲染。
- **相机边框与摄影水印**：自定义定制版相机边框、徕卡/哈苏风白底参数卡，一键导出质感大片。

### 4. 🛡️ EXIF 深度解析与隐私伪装保护
- **全量拍摄参数探查**：精确读取相机/镜头型号、焦段、等效 35mm、光圈快门、ISO、白平衡、曝光补偿及高精度 GPS 经纬度。
- **地理位置反查与地图导航**：内置离线/系统地理编码解析，直观展示拍摄城市与方位。
- **EXIF 隐私伪装与擦除**：支持一键随机伪装拍摄机型参数，或彻底擦除敏感地理位置与拍摄日期，安全分享社交平台防社工挖掘。

### 5. 🎬 影音增强与安全机制
- **AndroidX Media3 ExoPlayer**：高效率硬件解码播放，支持悬浮预览、进度快速拖拽与原画播放。
- **安全防误触回收站**：彻底分离物理删除与软删除；内置“摇一摇撤销”（Shake to Undo）机制，误删操作秒级挽回。

---

## 🛠️ 技术栈与工程架构

本项目基于现代 Android 官方推荐的最佳实践规范开发：

| 模块 / 功能 | 选型与技术实现 |
| :--- | :--- |
| **语言规范** | Kotlin 2.0+ (JVM 21) |
| **UI 框架** | Jetpack Compose (BOM), Material Design 3, Compose Foundation |
| **架构模式** | MVVM + 单向数据流 (UDF), 响应式 Flow / Coroutines |
| **持久层** | Room Database (KSP 代码生成), 复杂跨表查询与索引加速 |
| **图片加载** | Coil 3 (支持动态 GIF、视频抽帧解码与自定义磁盘缓存层) |
| **媒体播放** | AndroidX Media3 ExoPlayer 1.5.1 |
| **端侧 AI / OCR** | Google ML Kit (`text-recognition-chinese` 16.0.1 离线管线) |
| **后台调度** | AndroidX WorkManager (静默增量索引) |
| **中文拼音** | Pinyin4j 2.5.1 (拼音多音字及首字母轻量索引) |
| **编译标准** | CompileSdk: 35 / MinSdk: 26 / TargetSdk: 35 (适配 Android 15 Edge-to-Edge) |

---

## 🏗️ 源码结构

```tree
app/src/main/java/com/example/phototag/
├── data/
│   ├── dao/             # Room 数据库访问对象 (PhotoDao, TagDao)
│   ├── entity/          # 数据实体 (PhotoEntity, TagEntity, CrossRef)
│   ├── ocr/             # Google ML Kit 离线文字识别封装 (OcrManager)
│   ├── repository/      # 数据仓库层 (PhotoRepository, 统一封装 MediaStore & DB)
│   └── worker/          # 后台定时与异步处理任务 (MediaIndexWorker)
├── ui/
│   ├── components/      # 高度解耦的 Compose UI 组件 (时间轴、胶片栏、底栏、弹窗)
│   ├── screen/          # 核心页面 (PhotoListScreen, PhotoViewerScreen, ImageEditScreen...)
│   └── theme/           # Material 3 主题系统、配色与字体梯度
└── util/
    ├── editor/          # 图像滤镜渲染引擎、复古点阵与胶片调色逻辑
    ├── exif/            # EXIF 解析、元数据修改与隐私伪装引擎
    └── ...              # 拼音检索、震动反馈、缩略图预加载等工具类
```

---

## 🚀 编译与本地运行

如果你希望自行从源代码构建安装包：

1. **环境准备**：
   - 安装最新版 [Android Studio](https://developer.android.com/studio) (Koala 或 Ladybug 以上版本)
   - 配置 **JDK 21**
   - 在 Android SDK Manager 中安装 **Android SDK Platform 35**

2. **克隆代码库**：
   ```bash
   git clone https://github.com/uminrae/FutureAlbum.git
   cd FutureAlbum
   ```

3. **构建与调试**：
   ```bash
   # 编译 Debug 版 APK
   ./gradlew assembleDebug

   # 编译正式 Release 优化版 APK
   ./gradlew assembleRelease
   ```
   *编译生成的 APK 位于 `app/build/outputs/apk/` 目录下。*

---

## 📋 声明与条款 (Declarations & Disclaimers)

### 1. 🔒 隐私安全与数据保护承诺 (Privacy Declaration)
- **100% 本地离线执行**：本项目坚决贯彻用户隐私保护原则，**不包含任何形式的第三方追踪、广告推广 SDK 或云端数据上报代码**。
- **端侧处理**：相册内的所有扫描、OCR 字符提取、元数据解析、标签分类等行为全部在用户设备本地芯片上计算，任何照片与个人数据均不会离开设备。

### 2. ⚖️ 版权与法律免责声明 (Legal Disclaimer)
- **技术学习与研究**：本项目为开源技术探索项目，旨在分享 Jetpack Compose 复杂界面调优、本地图片渲染及端侧 AI 架构经验。
- **色彩与滤镜风格**：项目中包含的胶片色彩风格与滤镜算法，属于对经典胶卷美学与色彩科学的数字化模拟研究；提及的相关相机品牌或型号仅作美学风格区分指代，其版权与商标归各自合法权利人所有，与本项目无商业关联。
- **合规使用提示**：项目中提供的 EXIF 元数据擦除与伪装功能旨在协助用户规避社交网络中潜在的隐私泄露风险（如经纬度或拍摄设备追踪）。请使用者在遵守适用的法律法规与平台守则的前提下合规使用，切勿用于违法欺诈或侵害他人权益的行为。使用者因非正常或违规操作引发的任何纠纷均由使用者自行承担。

### 3. 📄 开源许可证 (License)
本项目基于 [MIT License (麻省理工学院开源许可证)](LICENSE) 协议开源。在保留原作者版权声明的前提下，允许任何人自由免费使用、修改、分发、整合及用于商业项目。

---

<p align="center">
  Made with ❤️ by minqeoleu & FutureAlbum Contributors
</p>
