# 蓝牙投屏 · Bluetooth Screen Share

Kotlin + Jetpack Compose 写的安卓小工具：**用蓝牙把「被控手机」的屏幕画面传到「控制手机」**。
一个 APK 同时包含两个角色，两台手机各装一份即可。

```
控制端 (Controller)                被控端 (Host)
┌──────────────────┐   RFCOMM   ┌──────────────────┐
│ 选择被控设备      │ ─────────► │ 屏幕共享服务      │
│ 胶囊：连接成功    │  CAPTURE   │ MediaProjection  │
│ 点按钮 → 收一帧   │ ◄───────── │ 截一帧 JPEG       │
└──────────────────┘  长度+JPEG  └──────────────────┘
```

## 控制端（默认进入的界面）

1. 首次进入申请蓝牙权限（Android 12+ 为 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`）。
2. 列出本机**已配对**设备，同时每 12 秒循环搜索一次**附近**设备，列表持续刷新。
3. 点选设备后：
   - 未配对则先自动配对（`createBond`），再建立 RFCOMM 连接；
   - 成功：顶部弹出**绿色胶囊**「某某设备连接成功」，停留 **3 秒**，然后向上滑出屏外；
   - 失败：**红色胶囊**「某某设备连接失败，请重新连接」，随后自动重新弹出设备选择面板，直到连接成功。
4. 连接成功后页面中间有一个圆形「传输屏幕」按钮：
   - 点一下 → 通过蓝牙向被控端请求一帧屏幕；
   - 按钮随即**动画移动到屏幕底部**（仍留在屏内），之后每点一次传输一帧；
   - 下方实时显示收到的画面和「已传输 N 次 · 最近 xx KB」。

## 被控端（入口：设备选择页底部「本机作为被控端 →」）

1. 点「开启共享」→ 授权录屏 → 前台服务启动，通知栏常驻。
2. 点「可被发现」让本机 300 秒内可被控制端搜到。
3. 收到 `CAPTURE` 请求时抓取当前屏幕，缩放到宽 720px、JPEG 质量 70 后回传。

> 说明：屏幕内容只能由**被控手机自己**采集（Android 不允许远程读别人的屏幕），
> 所以被控手机必须也装这个 APK 并开启「被控端模式」。控制端负责选择、连接、请求和显示。

## 协议

蓝牙 RFCOMM / SPP（UUID `00001101-0000-1000-8000-00805F9B34FB`）：

| 方向 | 内容 |
| --- | --- |
| 控制端 → 被控端 | 一行文本 `CAPTURE\n` |
| 被控端 → 控制端 | 4 字节大端长度 + JPEG 数据 |

## 构建（在 GitHub 上自动完成）

代码推到 `main` 分支后，GitHub Actions 会自动编译（配置见 `.github/workflows/android.yml`）：

1. 打开仓库的 **Actions** 页面，点最新一次「构建 APK」运行；
2. 等它变绿（约 3–6 分钟），页面底部 **Artifacts** 里可以下载 `btshare-apk.zip`，解压得到 APK；
3. 或者直接去 **Releases** 页面，每次构建都会生成一个 `v1.0.N`，里面挂着同一个 APK，手机点开就能下。

也可以在 Actions 页面点 **Run workflow** 手动触发一次。

### 本地构建（可选）

```bash
# 需要 JDK 17+、Android SDK（platform 34、build-tools 34.0.0）
echo "sdk.dir=/path/to/android-sdk" > local.properties
gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

仓库里带了一个演示用签名密钥 `keystore/btshare.jks`
（storePassword/keyPassword 均为 `btshare2024`，alias `btshare`），
正式发布请换成自己的密钥。

## 已知限制

- 蓝牙 SPP 带宽约 100–200 KB/s，一帧 720p JPEG 通常 60–200 KB，传输约 1–2 秒。
- 两台手机必须先**配对**（控制端会自动发起配对）。
- 被控端每次开启共享都需要用户授权录屏（Android 系统限制）。
- 屏幕静止时被控端不会产生新帧，控制端拿到的是最近一帧画面。
